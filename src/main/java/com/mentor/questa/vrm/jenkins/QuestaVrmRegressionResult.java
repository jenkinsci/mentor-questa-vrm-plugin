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

import com.mentor.questa.jenkins.Util;
import com.mentor.questa.ucdb.jenkins.CoverageUtil;
import com.mentor.questa.ucdb.jenkins.QuestaCoverageResult;
import com.mentor.questa.ucdb.jenkins.QuestaUCDBResult;
import hudson.Functions;
import hudson.model.Api;
import hudson.model.Item;
import hudson.model.Run;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.SuiteResult;
import hudson.tasks.test.MetaTabulatedResult;
import hudson.tasks.test.TestObject;
import hudson.tasks.test.TestResult;
import org.apache.commons.lang.StringEscapeUtils;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 *
 * 
 */
public class QuestaVrmRegressionResult extends MetaTabulatedResult {

    public static final String QUESTA_REGRESSION_RESULT = "questavrmRegr";

    private float duration;
    private float totalTime;
    private String timestamp;
    private final String vrmdata;
    private String logfile;
    private List<String> mergeFiles, covHTMLReports;

    private transient int passedActionCount, failedActionCount, skippedActionCount;
    private transient int passedTestsCount, failedTestsCount, skippedTestsCount;

    private transient Run<?, ?> owner;
    private transient TestResult testResult;
    
    private transient Map<String, Map.Entry<Double, Double>> totalCovMap;
    
    /**
     * All the actions are stored together to keep the order in which they are
     * retrieved
     */

    private final ArrayList<QuestaVrmAbstractResult> allActions;

    /**
     * Those lists are created upon de-serialization to ease the toggle between
     * test/action mode.
     */
    private transient HashMap<String, QuestaVrmAbstractResult> tests;
    private transient ArrayList<QuestaVrmAbstractResult> failedActions;
    private transient ArrayList<QuestaVrmAbstractResult> failedNonTests;
    private transient ArrayList<QuestaVrmAbstractResult> failedTests;
    private transient HashMap<String, QuestaVrmAbstractResult> actions;

    public QuestaVrmRegressionResult(String vrmdata) {
        this.vrmdata = vrmdata;
        allActions = new ArrayList<QuestaVrmAbstractResult>();
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public void setLogfile(String logfile) {
        this.logfile = logfile;
    }

    public void setDuration(float duration) {
        this.duration = duration;
    }

    public void setMergeFiles(List<String> mergeFiles) {
        this.mergeFiles = mergeFiles;
    }

    public void setCovHTMLReports(List<String> covHTMLReports) {
        this.covHTMLReports = covHTMLReports;
    }

    public void setOwner(Run run) {
        this.owner = run;
        for (QuestaVrmAbstractResult test : allActions) {
            test.setOwner(run);
            test.setRegressionResult(this);
        }
    }

    public void addAction(QuestaVrmAbstractResult testResult) {
        allActions.add(testResult);
        int test = (testResult instanceof QuestaVrmTestResult) ? 1 : 0;
        if (testResult.isFailed()) {
            failedActionCount++;
            failedTestsCount += test;
        } else if (testResult.isSkipped()) {
            skippedActionCount++;
            skippedTestsCount += test;

        } else {
            passedActionCount++;
            passedTestsCount += test;
        }

        totalTime += testResult.getDuration();

    }

    public String getVrmdata() {
        return vrmdata;
    }

    public List<String> getMergeFiles() {
        return mergeFiles;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public Date getRegressionBegin() {

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd\'T\'HH:mm:ss");
        try {
            return df.parse(timestamp);

        } catch (Exception e) {
        }
        return null;
    }

    @Exported(visibility = 999)
    @Override
    public int getTotalCount() {
        return getFailCount() + getPassCount() + getSkipCount();
    }

    @Override
    public String getName() {
        return "regression-" + timestamp;
    }

    @Exported(visibility = 999)
    public int getFailedActionCount() {
        return failedActionCount;

    }

    @Exported(visibility = 999)
    public int getSkipActionCount() {
        return skippedActionCount;

    }

    @Exported(visibility = 999)
    public int getPassActionCount() {
        return passedActionCount;

    }

    @Exported(visibility = 999)
    public int getTotalActionCount() {
        return skippedActionCount + passedActionCount + failedActionCount;

    }

    @Exported(visibility = 999)
    @Override
    public int getSkipCount() {
        return skippedTestsCount;

    }

    @Exported(visibility = 999)
    @Override
    public int getPassCount() {
        return passedTestsCount;
    }

    @Exported(visibility = 999)
    @Override
    public int getFailCount() {
        return failedTestsCount;
    }

    @Exported(visibility = 999)
    public boolean isEmpty() {
        return !allActions.isEmpty();
    }

    @Override
    public Collection<? extends TestResult> getFailedTests() {
        return failedTests;
    }

    public Collection<? extends TestResult> getFailedActions() {
        return failedActions;
    }

    public Collection<? extends TestResult> getNonTestFailures() {
        return failedNonTests;
    }

    public Collection<? extends TestResult> getActions() {
        return allActions;
    }

    @Override
    public Collection<? extends TestResult> getChildren() {
        return tests.values();
    }

    @Override
    public boolean hasChildren() {
        return !tests.isEmpty();
    }

    @Override
    public TestObject getParent() {
        return null;
    }

    @Exported(visibility = 999)
    @Override
    public float getDuration() {
        return duration;
    }
    
    public List<String> getCovHTMLReports() {
        return covHTMLReports;
    }
    

    @Override
    public String getDisplayName() {
        return "Questa VRM Regression Results";
    }

    @Override
    public Api getApi() {
        return new Api(this);
    }

    @Override
    public Run<?, ?> getRun() {
        return owner;
    }

   
    @Override
    public String getRelativePathFrom(TestObject it) {

        if (it == this) {
            return ".";
        }

        StringBuilder buf = new StringBuilder();
        TestObject next = this;
        TestObject cur = this;
        // Iterate over ancestors and construct relative url
        while (next != null && it != next) {
            cur = next;
            buf.insert(0, '/');
            buf.insert(0, cur.getSafeName());
            next = cur.getParent();
        }
        if (it == next) {
            return buf.toString();
        } else {
            QuestaVrmRegressionBuildAction action = getRun().getAction(QuestaVrmRegressionBuildAction.class);
            if (action == null) {

                return ""; // this won't take us to the right place, but it also won't 404.
            }
            buf.insert(0, '/');
            buf.insert(0, action.getUrlName());

            // Now the build
            Run<?, ?> myBuild = cur.getRun();
            if (myBuild == null) {

                return "";
            }
            buf.insert(0, '/');
            buf.insert(0, myBuild.getUrl());

            // If we're inside a stapler request, just delegate to Hudson.Util to get the relative path!
            StaplerRequest req = Stapler.getCurrentRequest();
            if (req != null && myBuild instanceof Item) {
                buf.insert(0, '/');

                Item myBuildAsItem = (Item) myBuild;
                buf.insert(0, Functions.getRelativeLinkTo(myBuildAsItem));
            } else {
                // We're not in a stapler request. Okay, give up.
                String hudsonRootUrl = Util.getActiveInstance().getRootUrl();
                if (hudsonRootUrl == null || hudsonRootUrl.length() == 0) {
                    return "";
                }
                buf.insert(0, '/');
                buf.insert(0, hudsonRootUrl);
            }

            return buf.toString();
        }

    }

    @Override
    public TestResult getPreviousResult() {
        Run<?, ?> b = getRun();
        if (b == null) {
            return null;
        }
        while (true) {
            b = b.getPreviousBuild();
            if (b == null) {
                return null;
            }
            QuestaVrmRegressionBuildAction r = b.getAction(QuestaVrmRegressionBuildAction.class);
            if (r != null) {
                return r.getResult();
            }
        }
    }

    public final String getFailureDiffString() {
        TestResult prev = getPreviousResult();
        if (prev == null || !(prev instanceof QuestaVrmRegressionResult)) {
            return "";  // no record
        }
        QuestaVrmRegressionResult prevRegr = (QuestaVrmRegressionResult) prev;
        return " / " + Functions.getDiffString(this.getFailedActionCount() - this.getFailCount() - prevRegr.getFailedActionCount() + prevRegr.getFailCount());
    }

    public TestResult getJunitTestResult() {
        return testResult;
    }

    public String getDoubleDiffString(double d) {
        return Util.getDoubleDiffString(d, 2);
    }

    @Override
    public TestResult getResultInRun(Run<?, ?> build) {
        QuestaVrmRegressionBuildAction tra = build.getAction(QuestaVrmRegressionBuildAction.class);
        return (tra == null) ? null : tra.getResult().findCorrespondingResult(this.getId());
    }
    @Override
    public Object getDynamic(String token, StaplerRequest req, StaplerResponse rsp) {

        if (actions.containsKey(token)) {
            return actions.get(token);
        }
        return testResult.getDynamic(token, req, rsp);

    }

    @Override
    public TestResult findCorrespondingResult(String id) {

        if (tests.containsKey(id)) {
            return tests.get(id);
        }

        if (actions.containsKey(id)) {
            return actions.get(id);
        }
        String safeId = safe(id);

        if (actions.containsKey(safeId)) {
            return actions.get(safeId);
        }

        return this;
    }

    public void freeze(Run run) {
        setOwner(run);
        testResult = (TestResult) getTestResultAction().getResult();
        tally();
    }

    @Override
    public void tally() {
        actions = new HashMap<>();
        failedActions = new ArrayList<>();
        tests = new HashMap<>();
        failedTests = new ArrayList<>();
        failedNonTests = new ArrayList<>();
        passedActionCount = 0;
        failedActionCount = 0;
        skippedActionCount = 0;
        passedTestsCount = 0;
        failedTestsCount = 0;
        skippedTestsCount = 0;

        hudson.tasks.junit.TestResult junitTestResult = null;

        if (testResult instanceof hudson.tasks.junit.TestResult) {
            junitTestResult = (hudson.tasks.junit.TestResult) testResult;
        }

        for (QuestaVrmAbstractResult action : allActions) {
            int test = (action instanceof QuestaVrmTestResult) ? 1 : 0;
            if (test == 1 && junitTestResult != null) {
                tests.put(action.getTestname(), action);
                for (SuiteResult suite : junitTestResult.getSuites()) {
                    CaseResult caseResult = suite.getCase(action.getTestname());
                    ((QuestaVrmTestResult) action).setMirrorTest(caseResult);
                }
            }

            actions.put(action.getSafeName(), action);

            if (action.isFailed()) {
                failedActionCount++;
                failedTestsCount += test;
                if (test == 1) {
                    failedTests.add(action);
                } else {
                    failedNonTests.add(action);
                }
                failedActions.add(action);
            } else if (action.isSkipped()) {
                skippedActionCount++;
                skippedTestsCount += test;

            } else {
                passedActionCount++;
                passedTestsCount += test;
            }
        }
    }

    String getXmlSnippet(File ws) throws IOException {
        StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
        sb.append(System.getProperty("line.separator"));
        sb.append("<testsuite errors=\"0\" failures=\"");
        sb.append(failedTestsCount);
        sb.append("\" timestamp=\"");
        sb.append(timestamp);
        sb.append("\" tests=\"");
        sb.append(getTotalCount());
        sb.append("\">");
        sb.append(System.getProperty("line.separator"));
        if (logfile != null) {

            sb.append("<system-out>");
            sb.append(StringEscapeUtils.escapeXml(Util.possiblyTrimStdio(false, ws, logfile)));
            sb.append("</system-out>");
            sb.append(System.getProperty("line.separator"));
        }

        for (QuestaVrmAbstractResult actionResult : allActions) {
            sb.append(actionResult.getXmlSnippet(ws));

        }
        sb.append("</testsuite>");
        return sb.toString();
    }

	public Map<String, Map.Entry<Double, Double>> getTotalCovMap() {
		if (totalCovMap == null) {
			totalCovMap = new HashMap<>();
			populateTotalCoverageResults();
		}
		return totalCovMap;
	}

	public void populateTotalCoverageResults() {
		List<QuestaCoverageResult> coverageResults = CoverageUtil.getCoverageResult(this);
		for (QuestaCoverageResult coverageResult : coverageResults) {
			QuestaUCDBResult ucdbResult = coverageResult.getUcdbResult();
			getTotalCovMap().put(ucdbResult.getCoverageId(), new AbstractMap.SimpleImmutableEntry<>(
					ucdbResult.getTotalCoverage(), ucdbResult.getTestplanCov()));

		}
	}
    
    private static final long serialVersionUID = 1L;
}
