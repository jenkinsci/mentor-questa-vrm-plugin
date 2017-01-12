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

import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.TestAction;
import hudson.tasks.test.TestResult;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.commons.lang.StringEscapeUtils;
import com.mentor.questa.jenkins.Util;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;

/**
 *
 * 
 */
public class QuestaVrmTestResult extends QuestaVrmAbstractResult {

    private transient CaseResult mirrorTest;

    public QuestaVrmTestResult(QuestaVrmRegressionResult regressionResult) {
        super(regressionResult);
    }

    @Exported(visibility = 999)
    @Override
    public String getStderr() {
        return mirrorTest != null ? mirrorTest.getStderr() : "";
    }

    @Exported(visibility = 999)
    @Override
    public String getStdout() {
        return mirrorTest != null ? mirrorTest.getStdout() : "";
    }

    @Override
    public String getName() {
        return mirrorTest != null ? getTestname() : getAction();
    }

    @Exported(visibility = 999)
    @Override
    public String getDisplayName() {
        return getTestname();
    }

    public void setMirrorTest(CaseResult mirrorTest) {
        this.mirrorTest = mirrorTest;
    }

    @Override
    public String getFullDisplayName() {
        return getTestname();
    }

    private String getClassName() {
        return getAction().substring(0, getAction().lastIndexOf('/')).replace("/", ".");
    }

    @Override
    public String getTestUrl() {
        return mirrorTest == null ? super.getRelativeUrl() : mirrorTest.getRelativePathFrom(getParent().getJunitTestResult()); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isTest() {
        return mirrorTest != null;
    }

    @Override
    public TestResult getMirrorTest() {
        return mirrorTest;
    }


    @Override
    public List<TestAction> getTestActions() {
        if (mirrorTest != null) {
            return mirrorTest.getTestActions();
        }
        return super.getTestActions();
    }
    
    @Override
    public Object getDynamic(String token, StaplerRequest req, StaplerResponse rsp) {

        if (mirrorTest == null) {
            return null;
        }
        return mirrorTest.getDynamic(token, req, rsp);

    }
    
    @Override
    String getXmlSnippet(File ws) throws IOException {
        StringBuilder sb = new StringBuilder("<testcase classname=\"");
        sb.append(getClassName());
        sb.append("\" name=\"");
        sb.append(StringEscapeUtils.escapeXml(getTestname()));
        sb.append("\" time=\"");
        sb.append(getDuration());
        sb.append("\">");
        if (isFailed()) {
            sb.append(System.getProperty("line.separator"));
            sb.append("<failure message=\"");
            sb.append(StringEscapeUtils.escapeXml(getReason()));
            sb.append("\" />");
            sb.append(System.getProperty("line.separator"));
            sb.append("<system-err>");
            sb.append(StringEscapeUtils.escapeXml(Util.possiblyTrimStdio(false, ws, getStderrPattern())));
            sb.append("</system-err>");
            sb.append("<system-out>");
            sb.append(StringEscapeUtils.escapeXml(Util.possiblyTrimStdio(false, ws, getLogPattern())));
            sb.append("</system-out>");
        } else {
            sb.append("<system-out/>");

        }
        if (isSkipped()) {
            sb.append("<skipped/>");
        }

        sb.append("</testcase>");
        sb.append(System.getProperty("line.separator"));
        return sb.toString();
    }
    private static final long serialVersionUID = 1L;
}
