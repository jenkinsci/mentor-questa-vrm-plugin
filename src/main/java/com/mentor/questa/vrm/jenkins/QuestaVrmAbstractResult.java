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
import hudson.Functions;
import hudson.model.Api;
import hudson.model.Item;
import hudson.model.Run;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.TestObject;
import hudson.tasks.test.TestResult;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * This abstract class represents the information 
 retrieved for each testAction that ran by VRM.
 * 
 * 
 */
@ExportedBean
public abstract class QuestaVrmAbstractResult extends TestResult {

    private String testname, action, seed, reason, hostname, ucdbfile, mergefile;
    private Date launchTime, startTime, doneTime;
    private float testplanCov, totalCov;
    private String status;

    private float duration, queued;

    private transient Run<?, ?> owner;
    private transient QuestaVrmRegressionResult regressionResult;
    private transient int failedSince;

    public QuestaVrmAbstractResult(QuestaVrmRegressionResult regressionResult) {
        this.regressionResult = regressionResult;
    }

    public boolean isTest() {
        return false;
    }

    @Override
    public QuestaVrmRegressionResult getParent() {
        return regressionResult;
    }


    public void setStatus(String status) {
        this.status = status;
    }

    public void setTestname(String testname) {
        this.testname = testname;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setSeed(String seed) {
        this.seed = seed;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setUcdbfile(String ucdbfile) {
        this.ucdbfile = ucdbfile;
    }

    public void setMergefile(String mergefile) {
        this.mergefile = mergefile;
    }

    public void setDuration(float duration) {
        this.duration = duration;
    }

    public void setOwner(Run run) {
        this.owner = run;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public void setLaunchTime(Date launchTime) {
        if(launchTime!=null){
            this.launchTime = new Date(launchTime.getTime());
        }
    }

    public void setStartTime(Date startTime) {
        if(startTime!=null){
            this.startTime = new Date(startTime.getTime());
        }
    }

    public void setDoneTime(Date doneTime) {
        if(doneTime !=null){
            this.doneTime =new Date(doneTime.getTime());
        }
    }

    public void setTestplanCov(float testplanCov) {
        this.testplanCov = testplanCov;
    }

    public void setCoverage(float value) {
        totalCov = value;

    }

    public void setQueued(float queued) {
        this.queued = queued;
    }

    public void setRegressionResult(QuestaVrmRegressionResult regressionResult) {
        this.regressionResult = regressionResult;
    }

    public boolean isFailed() {
        return status == null || status.startsWith("Failed");
    }

    @Override
    public boolean isPassed() {
        return status != null && status.startsWith("Passed");
    }

    public boolean isSkipped() {
        return !isPassed() && !isFailed();
    }

    @Exported(visibility = 999)
    @Override
    public int getSkipCount() {
        return isSkipped() ? 1 : 0;
    }

    @Exported(visibility = 999)
    @Override
    public int getFailCount() {
        return isFailed() ? 1 : 0;
    }

    @Exported(visibility = 999)
    @Override
    public int getPassCount() {
        return isPassed() ? 1 : 0;
    }

    @Exported(visibility = 999)
    @Override
    public float getDuration() {
        return duration;
    }

    @Override
    public String getTitle() {
        return testname;
    }

    @Exported(visibility = 999)
    @Override
    public int getTotalCount() {
        return 1;
    }

    @Exported(visibility = 999)
    public float getQueued() {
        return queued;
    }

    @Exported(visibility = 999)
    public String getTestname() {
        return testname;
    }

    @Exported(visibility = 999)
    public String getSeed() {
        return seed;
    }

    @Exported(visibility = 999)
    public String getStatus() {
        return status;
    }

    @Exported(visibility = 999)
    public float getTplanCov() {
        return testplanCov;
    }

    @Exported(visibility = 999)
    public String getHost() {
        return hostname;
    }

    @Exported(visibility = 999)
    public float getCoverage() {
        return totalCov;
    }

    @Exported(visibility = 999)
    public String getAction() {
        return action;
    }

    @Exported(visibility = 999)
    public long getStartTime() {
        if (startTime == null) {
            return -1;
        }
        return startTime.getTime();
    }

    @Exported(visibility = 999)
    public long getDoneTime() {
        if (doneTime == null) {
            return -1;
        }
        return doneTime.getTime();
    }

    @Exported(visibility = 999)
    public long getLaunchTime() {
        if (launchTime == null) {
            return -1;
        }
        return launchTime.getTime();
    }

    @Exported(visibility = 999)
    public String getStartTimeString() {
        if (startTime == null) {
            return "--";
        }
        return startTime.toString();
    }

    @Exported(visibility = 999)
    public String getDoneTimeString() {
        if (doneTime == null) {
            return "--";
        }
        return doneTime.toString();
    }

    @Exported(visibility = 999)
    public String getLaunchTimeString() {
        if (launchTime == null) {
            return "--";
        }
        return launchTime.toString();
    }

    @Exported(visibility = 999)
    public String getReason() {
        return reason;
    }

    @Exported(visibility = 999)
    public String getUcdbfile() {
        return ucdbfile;
    }

    @Exported(visibility = 999)
    public String getMergefile() {
        return mergefile;
    }

    @Exported(visibility = 999)
    public double getTestplanCov() {
        return testplanCov;
    }

    @Exported(visibility = 999)
    public String getHostname() {
        return hostname;
    }

    public String getRelativeUrl() {
        return getSafeName();
    }

    @Override
    public String getDisplayName() {
        return getName();
    }

    String getLogPattern() {
        return regressionResult.getVrmdata() + System.getProperty("file.separator") + getAction() + ".log";
    }

    String getStderrPattern() {
        return regressionResult.getVrmdata() + System.getProperty("file.separator") + getAction() + ".stderr";
    }

    String getXmlSnippet(File ws) throws IOException {
        return "";
    }

    private Collection<? extends hudson.tasks.test.TestResult> singletonListOfThisOrEmptyList(boolean f) {
        if (f) {
            return singletonList(this);
        } else {
            return emptyList();
        }
    }

    @Override
    public Collection<? extends TestResult> getFailedTests() {
        return singletonListOfThisOrEmptyList(isFailed());
    }

    @Override
    public Collection<? extends TestResult> getPassedTests() {
        return singletonListOfThisOrEmptyList(isPassed());
    }

    @Override
    public Collection<? extends TestResult> getSkippedTests() {
        return singletonListOfThisOrEmptyList(isSkipped());
    }

    public TestResult getMirrorTest() {
        return this;
    }

    public String getTestUrl() {
        return getRelativeUrl();
    }

    /**
     * If this test failed, then return the build number when this test started
     * failing.
     */
    @Override
    @Exported(visibility = 9)
    public int getFailedSince() {

        if (failedSince == 0 && getFailCount() == 1) {
            TestResult prevResult = getPreviousResult();
            if (prevResult == null || !(prevResult instanceof QuestaVrmAbstractResult)) {
                if (getRun() != null) {
                    this.failedSince = getRun().getNumber();
                }
                return failedSince;
            }
            QuestaVrmAbstractResult prev = (QuestaVrmAbstractResult) prevResult;
            if (!prev.isPassed()) {
                this.failedSince = prev.getFailedSince();
            } else if (getRun() != null) {
                this.failedSince = getRun().getNumber();
            } else {

            }
        }
        return failedSince;
    }
    @Override
    public Run<?, ?> getFailedSinceRun() {
        return getRun().getParent().getBuildByNumber(getFailedSince());
    }

    /**
     * Gets the number of consecutive builds (including this) that this test
     * case has been failing.
     *
     * @return the number of consecutive failing builds.
     */
    @Exported(visibility = 9)
    public int getAge() {
        if (isPassed()) {
            return 0;
        } else if (getRun() != null) {
            return getRun().getNumber() - getFailedSince() + 1;
        } else {

            return 0;
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
                TestResult result = r.getResult().findCorrespondingResult(getSafeName());
                if (result != null) {
                    return result;
                }
            }
        }
    }

    @Exported(name = "status", visibility = 9) // because stapler notices suffix 's' and remove it
    public CaseResult.Status getJunitCompatibleStatus() {
        if (isSkipped()) {
            return CaseResult.Status.SKIPPED;
        }
        TestResult pr = getPreviousResult();
        if (pr == null) {
            return isPassed() ? CaseResult.Status.PASSED : CaseResult.Status.FAILED;
        }

        if (pr.isPassed()) {
            return isPassed() ? CaseResult.Status.PASSED : CaseResult.Status.REGRESSION;
        } else {
            return isPassed() ? CaseResult.Status.FIXED : CaseResult.Status.FAILED;
        }
    }

    @Override
    public TestResult getResultInRun(Run<?, ?> build) {
        QuestaVrmRegressionBuildAction tra = build.getAction(QuestaVrmRegressionBuildAction.class);
        return (tra == null) ? null : tra.getResult().findCorrespondingResult(this.getRelativeUrl());
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
        // Walk up my ancestors from leaf to root, looking for "it"
        // and accumulating a relative url as I go
        while (next != null && it != next) {
            cur = next;
            buf.insert(0, '/');
            buf.insert(0, cur.getSafeName());
            next = cur.getParent();
        }
        if (it == next) {
            return buf.toString();
        } else {
            // Keep adding on to the string we've built so far

            // Start with the test result testAction
            AbstractTestResultAction testAction = getTestResultAction();
            if (testAction == null) {

                return ""; // this won't take us to the right place, but it also won't 404.
            }
            buf.insert(0, '/');
            buf.insert(0, testAction.getUrlName());

            // Now the build
            Run<?, ?> myBuild = cur.getRun();
            if (myBuild == null) {
                return ""; // this won't take us to the right place, but it also won't 404. 
            }
            buf.insert(0, '/');
            buf.insert(0, myBuild.getUrl());

            // If we're inside a stapler request, use  Hudson.Functions to get the relative path!
            StaplerRequest req = Stapler.getCurrentRequest();
            if (req != null && myBuild instanceof Item) {
                buf.insert(0, '/');
               
                Item myBuildAsItem = (Item) myBuild;
                buf.insert(0, Functions.getRelativeLinkTo(myBuildAsItem));
            } else {
                
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
    public Api getApi() {
        return new Api(this);
    }

    @Override
    public TestResult findCorrespondingResult(String id) {
        if (getId().equals(id) || (id == null)) {
            return this;
        }
        return null;
    }

    @Override
    public String toString() {
        return "Testname:" + testname + "\n" + "Status:" + status + "(" + isPassed() + "," + isFailed() + "," + isSkipped() + ")\n" + "Duration:" + duration;
    }

}
