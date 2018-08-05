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
package com.mentor.questa.ucdb.jenkins;

import com.mentor.questa.jenkins.Util;
import com.mentor.questa.jenkins.ChartLabel;
import hudson.Functions;

import hudson.model.Result;

import hudson.tasks.test.TestObject;
import hudson.util.Area;
import hudson.util.ColorPalette;
import java.awt.Color;
import java.io.IOException;
import java.util.List;
import javax.servlet.ServletException;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 *
 * 
 */
public class QuestaCoverageProjectHistory extends QuestaCoverageHistory {


    public QuestaCoverageProjectHistory(TestObject testObject, QuestaCoverageResult coverageResult,  String actionUrl) {
        super(testObject, coverageResult, actionUrl);
      

    }
    
    @Override
    protected void redirectSubmitAttributes(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        String redirect = "../..";
        rsp.sendRedirect(Functions.isAutoRefresh(req) ? redirect + "/.." : redirect);

    }

    @Override
    public int getStart() {
        return 0;
    }

    @Override
    public int getEnd() {
        return getTestObject().getRun().getParent().getBuilds().size();
    }

    @Override
    public Object getProxy() {
        return getTestObject().getRun().getParent();
    }

    @Override
    public ChartLabel getLabel(hudson.tasks.test.TestResult o, final List<String> metrics) {
        return new ChartLabel(o) {
            @Override
            public Color getColor() {
                if (metrics == null || metrics.size() != 1) {
                    return super.getColor();
                }

                if (getO().getBuildResult() == null || getO().getBuildResult() == Result.SUCCESS) {
                    return ColorPalette.BLUE;
                }
                if (getO().getBuildResult() == Result.UNSTABLE) {
                    return ColorPalette.YELLOW;
                }
                return ColorPalette.RED;
            }

            @Override
            public String getUrl() {
                String path = Stapler.getCurrentRequest().getParameter("rel");
                return (path == null ? "" : path) + this.getO().getRun().getNumber() + "/" + this.getActionUrl();
            }

        };
    }
    
    
    @Override
    public String getUrl() {
        return getActionUrl(); 
    }


    @Override
    public Area calcDefaultSize() {
        return Util.getProjectGraphArea();
    }
    
}
