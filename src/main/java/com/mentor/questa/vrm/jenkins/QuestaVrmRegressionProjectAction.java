/*
 * The MIT License
 *
 * Copyright 2016 Mentor Graphics.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.mentor.questa.vrm.jenkins;

import java.util.ArrayList;
import java.util.List;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import com.mentor.questa.jenkins.BuildTimeGraph;
import com.mentor.questa.jenkins.Util;
import com.mentor.questa.ucdb.jenkins.CoverageUtil;

import hudson.model.Job;
import hudson.model.ProminentProjectAction;
import hudson.tasks.junit.TestResult;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.util.Area;
import net.sf.json.JSONArray;

/**
 *
 * 
 */
public class QuestaVrmRegressionProjectAction implements ProminentProjectAction {
	public final Job job;

	private transient AbstractTestResultAction currentTestResultAction;

	private transient BuildTimeGraph buildTimeGraph;

	public QuestaVrmRegressionProjectAction(Job job, AbstractTestResultAction testResultAction) {
		this.job = job;
		currentTestResultAction = testResultAction;
		buildTimeGraph = new BuildTimeGraph(job);
	}

	@Override
	public String getDisplayName() {
		return "Questa VRM Project Action";
	}

	@Override
	public String getUrlName() {
		return "questavrmprojectaction";
	}

	@Override
	public String getIconFileName() {
		return null;
	}

	public QuestaVrmRegressionBuildAction getBuildAction() {
		return job.getLastCompletedBuild().getAction(QuestaVrmRegressionBuildAction.class);

	}

	public BuildTimeGraph getBuildTimeGraph() {
		return buildTimeGraph;
	}

	public Area calcDefaultSize() {
		return Util.getProjectGraphArea();
	}

	public Object getDynamic(String name, StaplerRequest req, StaplerResponse rsp) {
		return job.getDynamic(name, req, rsp);
	}

	public List<String> getCoverageColumnHeaders() {

		return CoverageUtil.getCoverageSummaryHeaders();
	}

	public synchronized List<List<CoverageUtil.RowItem>> getRows(int n) {

		ArrayList<List<CoverageUtil.RowItem>> rows = new ArrayList<>();
		int i = 0;

		for (; currentTestResultAction != null
				&& i < n; currentTestResultAction = currentTestResultAction.getPreviousResult(), i++) {
			if (!(currentTestResultAction.getResult() instanceof TestResult)) {
				continue;
			}
			ArrayList<CoverageUtil.RowItem> row = new ArrayList<>();
			CoverageUtil.RowItem build = new CoverageUtil.RowItem(currentTestResultAction.run.getDisplayName());
			build.url = currentTestResultAction.run.getUrl();
			build.imgSrc = currentTestResultAction.run.getBuildStatusUrl();
			row.add(build);
			row.addAll(CoverageUtil.getCoverageSummaryRow(currentTestResultAction));
			rows.add(row);
		}
		return rows;
	}

	public List<List<CoverageUtil.RowItem>> getRows() {
		return getRows(5);
	}

	@JavaScriptMethod
	public JSONArray getJSONData() {
		JSONArray result = new JSONArray();

		for (List<CoverageUtil.RowItem> row : getRows()) {
			JSONArray jsonRow = new JSONArray();
			for (CoverageUtil.RowItem rowItem : row) {
				jsonRow.add(rowItem.getJSONObject());
			}
			result.add(jsonRow);
		}
		return result;

	}

	public JSONArray getJSONRow() {
		JSONArray jsonRow = new JSONArray();
		List<List<CoverageUtil.RowItem>> rows = getRows(1);
		if (!rows.isEmpty()) {
			List<CoverageUtil.RowItem> row = rows.get(0);
			for (CoverageUtil.RowItem rowItem : row) {
				jsonRow.add(rowItem.getJSONObject());
			}
		}
		return jsonRow;
	}

	@JavaScriptMethod
	public boolean hasMore() {
		return currentTestResultAction != null;
	}

}
