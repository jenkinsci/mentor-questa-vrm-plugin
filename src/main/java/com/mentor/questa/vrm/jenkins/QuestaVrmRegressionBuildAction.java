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


import com.thoughtworks.xstream.XStream;
import hudson.model.Action;
import hudson.XmlFile;
import hudson.tasks.BuildStepMonitor;
import java.lang.ref.WeakReference;
import java.util.Collection;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.junit.TestResultAction;
import hudson.util.HeapSpaceStringConverter;
import hudson.util.XStream2;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import jenkins.model.RunAction2;
import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.stapler.StaplerProxy;


public class QuestaVrmRegressionBuildAction implements RunAction2, StaplerProxy, SimpleBuildStep.LastBuildAction {

    public transient Run<?, ?> run;
    public final boolean htmlReport;

    private transient WeakReference<QuestaVrmRegressionResult> questaVrmResultRef;

    public QuestaVrmRegressionBuildAction(Run owner, boolean htmlReport) {
        this.run = owner;
        this.htmlReport = htmlReport;
    }

    @Override
    public void onAttached(Run<?, ?> run) {
        this.run = run;
    }

    @Override
    public void onLoad(Run<?, ?> run) {
        this.run = run;
    }

    @Override
    public String getIconFileName() {
        return "/plugin/mentor-questa-vrm/icons/vrm.png";
    }

    @Override
    public String getDisplayName() {
        return "Questa VRM - Regression Result";
    }

    public Run<?, ?> getOwner() {
        return run;
    }

    @Override
    public String getUrlName() {
        return "questavrmreport";
    }

    private boolean addCovHTMLAction(Collection<Action> actions, File dir, String path, int index, String mergefile) {
        File archiveDir = new File(dir,path);
        String displayLink = "Latest Questa Coverage Report";
        if(index!=0){
            displayLink = "Latest "+com.mentor.questa.jenkins.Util.getFileName(mergefile)+ " Coverage Report";
        }
        if (archiveDir.exists()) {
            actions.add(new QuestaVrmHTMLAction(archiveDir, QuestaVrmHTMLArchiver.COV_ARCHIVE_DIR+(index==0?"":index), "index.html", "/plugin/mentor-questa-vrm/icons/coverage-report.png", displayLink));
            return true;
        } 
        return false;
    }

    @Override
    public Collection<? extends Action> getProjectActions() {
        Collection<Action> actions = new ArrayList<>();
        File vrmHtmlDir = new File(run.getParent().getRootDir(), QuestaVrmHTMLArchiver.HTML_ARCHIVE_DIR);

        if (htmlReport && vrmHtmlDir.exists()) {
            actions.add(new QuestaVrmHTMLAction(vrmHtmlDir, QuestaVrmHTMLArchiver.HTML_ARCHIVE_DIR, "index.html", "/plugin/mentor-questa-vrm/icons/HTML.png", "Latest Questa VRM Report"));
            QuestaVrmRegressionResult regressionResult = getResult();
            
            if (regressionResult.getCovHTMLReports().size() == 1) {
                    if (!addCovHTMLAction(actions, vrmHtmlDir, regressionResult.getCovHTMLReports().get(0) , 0, null)) {
                        // The coverage report is not nested within the VRM HTML directory.
                         addCovHTMLAction(actions,  run.getParent().getRootDir(), QuestaVrmHTMLArchiver.COV_ARCHIVE_DIR, 0, null);
                    }
            } else {
                int index = 1;
                for (String covHtmlReport : regressionResult.getCovHTMLReports()) {
                    addCovHTMLAction(actions, vrmHtmlDir, covHtmlReport, index++, regressionResult.getMergeFiles().get(index-2));
                }
            }
        }

        actions.add(new QuestaVrmRegressionProjectAction(run.getParent(), run.getAction(TestResultAction.class)));
        return actions;
    }

    @Override
    public Object getTarget() {
        return getResult();
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    public synchronized void setResult(QuestaVrmRegressionResult result, TaskListener listener) {
        result.freeze(run);

        if (run != null) {
            // persist the data
            try {
                getDataFile().write(result);
            } catch (IOException e) {
                e.printStackTrace(listener.fatalError("Failed to save the VRM regression result"));
            }
        }

        this.questaVrmResultRef = new WeakReference<>(result);
    }

    public synchronized QuestaVrmRegressionResult getResult() {
        QuestaVrmRegressionResult r;
        if (questaVrmResultRef == null) {
            r = load();
            questaVrmResultRef = new WeakReference<>(r);
        } else {
            r = questaVrmResultRef.get();
        }

        if (r == null) {
            r = load();
            questaVrmResultRef = new WeakReference<>(r);
        }

        return r;
    }

    private QuestaVrmRegressionResult load() {
        QuestaVrmRegressionResult r;
        try {
            r = (QuestaVrmRegressionResult) getDataFile().read();

        } catch (IOException e) {
            r = new QuestaVrmRegressionResult("");
        }
        r.freeze(run);
        return r;

    }

    private XmlFile getDataFile() {
        return new XmlFile(XSTREAM, new File(run.getRootDir(), "vrmresults.xml"));
    }

    private static final XStream XSTREAM = new XStream2();

    static {
        XSTREAM.alias("regression", QuestaVrmRegressionResult.class);
        XSTREAM.alias("test", QuestaVrmTestResult.class);
        XSTREAM.alias("action", QuestaVrmActionResult.class);
        XSTREAM.registerConverter(new HeapSpaceStringConverter(), 100);
    }

}
