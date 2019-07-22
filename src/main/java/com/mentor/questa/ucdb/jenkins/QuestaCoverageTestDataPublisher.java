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

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.TaskListener;
import hudson.tasks.junit.TestAction;
import hudson.tasks.junit.TestDataPublisher;
import hudson.tasks.junit.TestObject;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.TestResultAction;
import hudson.util.FormValidation;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

/**
 *
 * 
 */
public class QuestaCoverageTestDataPublisher extends TestDataPublisher {
    public static final String VRUN_EXEC="vrun";
    public static final String VCOVER_EXEC="vcover";
    
    public String coverageResults;
    public String vcoverExec;

    @DataBoundConstructor
    public QuestaCoverageTestDataPublisher(String coverageResults) {
        this.coverageResults = coverageResults;
    }

    private static String convertVruntoVcover(String vrunExec) {
        int vrunIndex = vrunExec.lastIndexOf(VRUN_EXEC);
        return new StringBuilder(vrunExec).replace(vrunIndex, vrunIndex + VRUN_EXEC.length(), VCOVER_EXEC).toString();
    }

    @DataBoundSetter
    public void setVcoverExec(String vcoverExec) {
        this.vcoverExec = vcoverExec;
    }

    private static void captureHistory(HashMap<String, QuestaUCDBResult> coverageResult, TestResult testResult) {
        // if first occurance, create the initial attributesSettingMap
        if (testResult.getPreviousResult() == null) {
            QuestaAttributesSettingsMap graphMap = new QuestaAttributesSettingsMap();
            for(QuestaUCDBResult covResult: coverageResult.values()) {
                graphMap.addGraphSetting(covResult);
            }
            CoverageUtil.saveAttributeSettingsMap(testResult.getRun().getParent(), graphMap);
            return;
        }
        
        // Update list of avaliableAttributes available attributes...
        QuestaAttributesSettingsMap graphMap = CoverageUtil.loadAttributeSettingsMap(testResult.getRun().getParent(), null);
        
        for(QuestaUCDBResult covResult: coverageResult.values()){
            QuestaAttributesGraphSetting settings = graphMap.getGraphSetting(covResult);
            settings.updateMergeLevelAttributes(covResult.getTrendableAttributes());
            
                for (QuestaCoverageResult test: covResult.getTests()){
                    settings.updateTestLevelAttributes(test.getTrendableAttributes());
                }
            
        }
        CoverageUtil.saveAttributeSettingsMap(testResult.getRun().getParent(), graphMap);
        
        // Insert dummy coverageResult for every previous result
        for (QuestaCoverageResult prevcov : CoverageUtil.getCoverageResult(testResult.getPreviousResult())) {
            boolean found = false;
            
            for (Map.Entry<String, QuestaUCDBResult> coverageEntry : coverageResult.entrySet()) {

                if (prevcov.getCoverageId().equals(coverageEntry.getKey())) {
                    coverageEntry.getValue().copyEmptyTests(prevcov);
                    found = true;

                }
            }
            if (!found && prevcov instanceof QuestaUCDBResult) {
                coverageResult.put(prevcov.getCoverageId(), ((QuestaUCDBResult)prevcov).createEmptyCopy());
            } 
        }

    }

    public static TestResultAction.Data getRegressionTestData(List<String> mergefiles, String vrunExec, String vcoverExec, Date regressionBegin, Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener, TestResult testResult, String vrmData) throws IOException, InterruptedException {
        HashMap<String, QuestaUCDBResult> coverageResult = new HashMap<>();
        if (vcoverExec == null || vcoverExec.trim().isEmpty())
            vcoverExec = convertVruntoVcover(vrunExec);

        if (mergefiles != null) {

            for (String mergefile : mergefiles) {
                 new QuestaCoverageTCLParser().parseResult(coverageResult, mergefile, vcoverExec, build, workspace, launcher, listener, regressionBegin, vrmData);
            }

        }

        captureHistory(coverageResult, testResult);
        return new Data(coverageResult);
    }



    @Override
    public TestResultAction.Data getTestData(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener, TestResult testResult) throws IOException, InterruptedException {
        HashMap<String, QuestaUCDBResult> coverageResult = new QuestaCoverageTCLParser().parseResult(coverageResults, vcoverExec, build, build.getProject().getWorkspace(), launcher, listener);
        captureHistory(coverageResult, testResult);
        return new Data(coverageResult);

    }

    public static class Data extends TestResultAction.Data {

        private final HashMap<String, QuestaUCDBResult> coverageResults;

        public Data(HashMap<String, QuestaUCDBResult> coverageResults) {
            this.coverageResults = coverageResults;

        }

        @Override
        public List<? extends TestAction> getTestAction(TestObject testObject) {
            if (!(testObject instanceof hudson.tasks.test.TestResult)) {
                return Collections.EMPTY_LIST;
            }
            hudson.tasks.test.TestResult testResult = (hudson.tasks.test.TestResult) testObject;

            if (testObject.getTotalCount() == 1) {
                for (QuestaUCDBResult cov : coverageResults.values()) {
                    cov.tally(testResult);
                    if (cov.containsTest(testObject.getName())) {
                        return Collections.singletonList(new QuestaCoverageAction(testResult, cov.getTest(testObject.getName())));
                    }
                }
               
            }

            ArrayList<QuestaCoverageAction> coverageActions = new ArrayList<>();
            int index = coverageResults.values().size() < 2 ? 0 : 1;
            for (QuestaCoverageResult cov : coverageResults.values()) {
                cov.tally(testResult);
                coverageActions.add(new QuestaCoverageAction(testResult, cov, index++));
            }
            return coverageActions;

        }

        HashMap<String, QuestaUCDBResult> getCoverageResults() {
            return coverageResults;
        }

    }


    public static class DescriptorImpl extends Descriptor<TestDataPublisher> {

        @Override
        public String getDisplayName() {
            return "Questa Coverage Test Data Publisher";
        }

        public FormValidation doCheckCoverageResults(@QueryParameter String value) throws IOException, ServletException {
            if (value.length() == 0) {
                return FormValidation.error("Required!");
            }

            return FormValidation.ok();

        }

    }
}
