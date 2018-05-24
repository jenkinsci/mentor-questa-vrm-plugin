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


import com.mentor.questa.ucdb.jenkins.CoverageUtil;
import com.mentor.questa.ucdb.jenkins.QuestaCoverageHistory;
import com.mentor.questa.ucdb.jenkins.QuestaCoverageProjectAction;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Job;
import hudson.model.TopLevelItem;
import hudson.plugins.view.dashboard.DashboardPortlet;
import java.util.ArrayList;
import java.util.List;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import hudson.util.ListBoxModel;

/**
 *
 * 
 */
public class QuestaVrmTrendGraphsPortlet extends DashboardPortlet {
	private String ID;
	public boolean buildTimeTrend;
	public boolean testResultsTrend;
	public boolean coverageResultsTrend;
	public boolean attributesTrend;
	
    @DataBoundConstructor
    public QuestaVrmTrendGraphsPortlet(String name, String ID) {
        super(name);
        this.ID = ID;
    }
    
    public List<QuestaCoverageHistory> getHistory(Job job) {
    	List<QuestaCoverageProjectAction> myActions = job.getActions(QuestaCoverageProjectAction.class);
    	List<QuestaCoverageHistory> result = new ArrayList<>();
    	for (QuestaCoverageProjectAction action : myActions) {
	    	QuestaCoverageHistory myHistory = action.getHistory();
	    	myHistory.setActionUrl("job/" + job.getName() + "/" + myHistory.getActionUrl() + "/");
	    	myHistory.setShowAttributes(false);
	    	result.add(myHistory);
    	}
    	return result;
    }
    
    @DataBoundSetter
    public void setBuildTimeTrend(boolean buildTimeTrend) {
    	this.buildTimeTrend = buildTimeTrend;
    }

	@DataBoundSetter
    public void setTestResultsTrend(boolean testResultsTrend) {
    	this.testResultsTrend = testResultsTrend;
    }
    
    @DataBoundSetter
    public void setCoverageResultsTrend(boolean coverageResultsTrend) {
    	this.coverageResultsTrend = coverageResultsTrend;
    }
    
    @DataBoundSetter
    public void setAttributesTrend(boolean attributesTrend) {
    	this.attributesTrend = attributesTrend;
    }
    
    public List<String> getCoverageColumnHeaders() {     
        return CoverageUtil.getCoverageSummaryHeaders();
    }
    
    public List<CoverageUtil.RowItem> getRow(TopLevelItem job) {
        return CoverageUtil.getLastCoverageResult(job);
    }


    @Override
    public String getDisplayName() {
        return super.getDisplayName(); 
    }
    
    @Extension(optional = true)
    public static class DescriptorImpl extends Descriptor<DashboardPortlet> {

        @Override
        public String getDisplayName() {
            return "Questa Coverage Trend Graphs Portlet";
        }
        
    }

}
