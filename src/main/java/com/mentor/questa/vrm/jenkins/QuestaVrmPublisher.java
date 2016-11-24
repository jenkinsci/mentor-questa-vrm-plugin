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

import hudson.FilePath;
import hudson.model.Descriptor;
import com.mentor.questa.ucdb.jenkins.QuestaCoverageTestDataPublisher;
import hudson.AbortException;
import hudson.DescriptorExtensionList;
import hudson.Extension;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.Proc;
import hudson.Util;
import hudson.model.Run;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Saveable;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.tasks.junit.TestDataPublisher;
import hudson.tasks.junit.TestResultAction;
import hudson.util.DescribableList;
import hudson.util.FormValidation;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import net.sf.json.JSONObject;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Publish VRM Results
 *
 *
 */
public class QuestaVrmPublisher extends Recorder implements SimpleBuildStep {

    static final String TMP_DIRECTORY = "questaVrm-temporary",
            HTML_ARCHIVE_DIR = "questavrmhtmlreport",
            COV_ARCHIVE_DIR = "covhtmlreport";

    private final String vrmdata;

    private boolean htmlReport = false;
    private boolean collectCoverage = false;

    private String vrunExec;

    private String vrmhtmldir;

    private String extraArgs = "";

    private DescribableList<TestDataPublisher, Descriptor<TestDataPublisher>> testDataPublishers;

    private Double healthScaleFactor = 1.0;

    @DataBoundConstructor
    public QuestaVrmPublisher(String vrmdata) {
        this.vrmdata = vrmdata;
    }

    public Double getHealthScaleFactor() {
        return healthScaleFactor;
    }

    @DataBoundSetter
    public final void setHealthScaleFactor(Double healthScaleFactor) {
        this.healthScaleFactor = healthScaleFactor;
    }

    /**
     *
     * @return
     */
    public String getVrmdata() {
        return vrmdata;
    }

    @DataBoundSetter
    public final void setTestDataPublishers(@Nonnull List<? extends TestDataPublisher> testDataPublishers) {
        this.testDataPublishers = new DescribableList<TestDataPublisher, Descriptor<TestDataPublisher>>(Saveable.NOOP);
        this.testDataPublishers.addAll(testDataPublishers);
    }

    public @Nonnull
    List<? extends TestDataPublisher> getTestDataPublishers() {
        return testDataPublishers == null ? Collections.<TestDataPublisher>emptyList() : testDataPublishers;
    }

    private FilePath getWorkspace(AbstractBuild build) {
        FilePath workspace = build.getWorkspace();
        if (workspace == null) {
            workspace = build.getProject().getSomeWorkspace();
        }
        return workspace;
    }

    private void processTestDataPublishers(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener, QuestaVrmRegressionResult regressionResult) throws IOException, InterruptedException {
        TestResultAction action = build.getAction(TestResultAction.class);
        if (action == null) {
            return;
        }
        List<TestResultAction.Data> testData = new ArrayList<TestResultAction.Data>();

        if (isCollectCoverage()) {
            QuestaCoverageTestDataPublisher.Data d = (QuestaCoverageTestDataPublisher.Data) QuestaCoverageTestDataPublisher.getTestData(regressionResult.getMergeFiles(), resolveParametersInString(build, listener, getVrunExec()), build, workspace, launcher, listener, action.getResult());
            if (d != null) {
                testData.add(d);
            }

        }

        for (TestDataPublisher testPub : getTestDataPublishers()) {
            TestResultAction.Data d = testPub.contributeTestData(build, workspace, launcher, listener, action.getResult());

            if (d != null) {
                testData.add(d);
            }

        }
        action.setData(testData);

    }

    private String constructCmdString(Run build, TaskListener listener) {
        String expandedVrmData = resolveParametersInString(build, listener, vrmdata);

        // Workaround for vrm windows path bug
        if (System.getProperty("file.separator").equals("\\")) {
            expandedVrmData = expandedVrmData.replace('\\', '/');
        }

        String cmd = resolveParametersInString(build, listener, getVrunExec()) + " -vrmdata " + expandedVrmData + " -status " + resolveParametersInString(build, listener, getExtraArgs()) + " -json ";

        if (isHtmlReport()) {
            cmd += " -html -htmldir " + resolveParametersInString(build, listener, getVrmhtmldir());
            if (isCollectCoverage()) {
                cmd += " -covreport";
            }
        }
        return cmd;
    }

    private void addVrmBuildActions(Run  build, TaskListener listener, QuestaVrmRegressionResult regressionResult) {
        QuestaVrmRegressionBuildAction regressionAction = new QuestaVrmRegressionBuildAction(build, isHtmlReport());
        regressionAction.setResult(regressionResult, listener);
        build.addAction(regressionAction);
        build.addAction(new QuestaVrmHostAction(build));
    }

    private File getOrCreateDir(File dir, String directoryName) {
        File targetDir = new File(dir, directoryName);
        if (!targetDir.exists()) {
            targetDir.mkdir();
        }
        return targetDir;
    }
   
    private void archiveHTMLReport( Run<?, ?> build, TaskListener listener, FilePath fromDir, String src,  String target) throws IOException, InterruptedException {
        listener.getLogger().println("Archiving  VRM HTML report...");

        FilePath archiveDir = new FilePath(getOrCreateDir(build.getParent().getRootDir(), target));

        // check whether the directory that contains the HTML report exists
        if (!fromDir.exists()) {
            listener.getLogger().println("[ERROR]: HTML report \'" + src + "\' not found. Skipping archiving HTML Report for build #" + build.getNumber() + ".");
            return;
        } else {
            archiveDir.deleteContents();
        }

        fromDir.copyRecursiveTo("**/*", archiveDir);

    }

    @Override
    public void perform(Run<?,?> build, FilePath workspace ,Launcher launcher, TaskListener listener) throws IOException, InterruptedException {


        listener.getLogger().println("Recording VRM results...");

        synchronized (build) {

            String cmd = constructCmdString(build, listener);
            try {
                ProcStarter ps = launcher.launch();
                ps.cmds(Util.tokenize(cmd)).envs(build.getEnvironment(listener)).stdin(null).stdout(new ByteArrayOutputStream()).pwd(workspace);
                ps.quiet(true);
                Proc proc = launcher.launch(ps);
                proc.join();

            } catch (IOException e) {
                listener.getLogger().println("[ERROR]: Vrun executable \'" + getVrunExec() + "\' not found. Aborting storing VRM results. ");
                return ;
            }
            QuestaVrmRegressionResult regressionResult;
            try {
                regressionResult = (QuestaVrmRegressionResult) new QuestaVrmResultsParser().parseResult(getVrmdata(), build, workspace,  launcher, listener);
            } catch (AbortException a) {
                listener.getLogger().println("[ERROR]: No report found from command \'" + cmd + "\', please recheck your configuration. Aborting storing VRM results.");
                return ;
            }

            QuestaVrmJunitProcessor vrmProcessor = new QuestaVrmJunitProcessor();

            vrmProcessor.perform(build, workspace, listener, listener.getLogger());

            // Add VRM build action(s)
            addVrmBuildActions(build, listener, regressionResult);

            if (isHtmlReport()) {
                FilePath vrmHtmlDir = workspace.child(getVrmhtmldir());
                archiveHTMLReport(build,listener, vrmHtmlDir, getVrmhtmldir(), HTML_ARCHIVE_DIR);
                
                // Process Coverage HTML reports if necessary 
                // if a single mergefile exists then the user might have changed the htmldir 
                if (regressionResult.getCovHTMLReports().size() == 1){
                   String covHtmlDir = regressionResult.getCovHTMLReports().get(0);
                   
                   FilePath covHtmlDirPath = vrmHtmlDir.child(covHtmlDir);
                    if (!covHtmlDirPath.exists()) {
                         archiveHTMLReport(build,listener, workspace.child(covHtmlDir), covHtmlDir, COV_ARCHIVE_DIR);
                    }
                }
            }
            
            // process data publishers
            processTestDataPublishers(build, workspace, launcher, listener, regressionResult);

            // Adjust build result if any of the regression actions failed
            if (build.getResult() == null || (build.getResult().isBetterThan(Result.UNSTABLE)
                    && regressionResult.getFailedActionCount() > 0)) {
                build.setResult(Result.UNSTABLE);
            }

            listener.getLogger().println("Recording finished.");
            processDeletion(workspace);

        }

        
    }

    private void processDeletion(FilePath workspace) {
        try {
            workspace.child("questaVrm-temporary").deleteRecursive();
            workspace.child("json.js").delete();
        } catch (IOException e) {

        } catch (InterruptedException e) {
        }

    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    /**
     * @return the vrmhtmldir
     */
    public String getVrmhtmldir() {
        if (vrmhtmldir == null) {
            vrmhtmldir = "vrmhtmldir";
        }
        return vrmhtmldir;
    }

    /**
     * @param vrmhtmldir the vrmhtmldir to set
     */
    @DataBoundSetter
    public final void setVrmhtmldir(String vrmhtmldir) {
        this.vrmhtmldir = vrmhtmldir;
    }

    @DataBoundSetter
    public final void setVrunExec(String vrunExec) {
        this.vrunExec = vrunExec;
    }

    /**
     * @return the htmlReport
     */
    public boolean isHtmlReport() {
        return htmlReport;
    }

    /**
     * @param htmlReport the htmlReport to set
     */
    @DataBoundSetter
    public final void setHtmlReport(boolean htmlReport) {
        this.htmlReport = htmlReport;
    }

    /**
     * @return the collectCoverage
     */
    public boolean isCollectCoverage() {
        return collectCoverage;
    }

    /**
     * @param collectCoverage the collectCoverage to set
     */
    @DataBoundSetter
    public final void setCollectCoverage(boolean collectCoverage) {
        this.collectCoverage = collectCoverage;
    }

    /**
     * @return the vrunExec
     */
    public String getVrunExec() {
        if (vrunExec == null) {
            vrunExec = getDescriptor().getVrunExec();
        }
        return vrunExec;
    }

    /**
     * @return the extraArgs
     */
    public String getExtraArgs() {
        if (extraArgs == null) {
            extraArgs = "";
        }
        return extraArgs;
    }

    /**
     * @param extraArgs the extraArgs to set
     */
    @DataBoundSetter
    public final void setExtraArgs(String extraArgs) {
        this.extraArgs = extraArgs;
    }

    protected static String resolveParametersInString(Run<?, ?> build, TaskListener listener, String input) {
        try {
            return build.getEnvironment(listener).expand(input);
        } catch (Exception e) {
            listener.getLogger().println("Failed to resolve parameters in string \""
                    + input + "\" due to following error:\n" + e.getMessage());
        }
        return input;
    }

    /**
     * {@inheritDoc}
     */
    @Extension @Symbol("questavrm")
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        private String vrunExec;

        public DescriptorImpl() {
            load();
        }

        public FormValidation doCheckVrmdata(
                @AncestorInPath AbstractProject project,
                @QueryParameter String value) throws IOException {

            if (project == null) {
                return FormValidation.ok();
            }
            if (value.length() == 0) {
                return FormValidation.error("Required!");
            }
            return FormValidation.ok();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Publish Questa VRM Regression Results";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            vrunExec = json.get("vrunExec").toString();
            save();
            super.configure(req, json);
            return true;
        }

        public String getVrunExec() {
            if (vrunExec == null) {
                return "vrun";
            }
            return vrunExec;
        }

        public DescriptorExtensionList<TestDataPublisher, Descriptor<TestDataPublisher>> getTestDataPublishers() {
            DescriptorExtensionList<TestDataPublisher, Descriptor<TestDataPublisher>> originalList = TestDataPublisher.all();
            DescriptorExtensionList<TestDataPublisher, Descriptor<TestDataPublisher>> newList = DescriptorExtensionList.createDescriptorList(Jenkins.getInstance(), TestDataPublisher.class);

            for (Descriptor d : originalList) {

                if ((d instanceof QuestaCoverageTestDataPublisher.DescriptorImpl)) {
                    newList.remove(d);
                }
            }
            return newList;
        }

    }

}
