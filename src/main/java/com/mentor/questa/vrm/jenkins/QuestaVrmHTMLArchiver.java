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

;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.List;


public class QuestaVrmHTMLArchiver implements Serializable {

    static final String HTML_ARCHIVE_DIR = "questavrmhtmlreport",
            COV_ARCHIVE_DIR = "covhtmlreport";
    private final String vrmHtmlReport;
    private final List<String> covHTMLReports;

    public QuestaVrmHTMLArchiver(String vrmHtmlReport, List<String> covHTMLReports) {
        this.vrmHtmlReport = vrmHtmlReport;
        this.covHTMLReports = covHTMLReports;
    }

    private File getOrCreateDir(PrintStream logger, File dir, String directoryName) {
        File targetDir = new File(dir, directoryName);
        if (!targetDir.exists()) {
            if(!targetDir.mkdir()){
                logger.println("[ERROR]: Unable to create HTML archive directory '"+directoryName+"'");
            }
                
        }
        return targetDir;
    }

    private void archiveHTMLReport(Run<?, ?> build, TaskListener listener, FilePath fromDir, String src, String target) throws IOException, InterruptedException {
        listener.getLogger().println("Archiving  VRM HTML report...");

        FilePath archiveDir = new FilePath(getOrCreateDir(listener.getLogger(), build.getParent().getRootDir(), target));

        // check whether the directory that contains the HTML report exists
        if (!fromDir.exists()) {
            listener.getLogger().println("[ERROR]: HTML report \'" + src + "\' not found. Skipping archiving HTML Report for build #" + build.getNumber() + ".");
            return;
        } else {
            archiveDir.deleteContents();
        }

        fromDir.copyRecursiveTo("**/*", archiveDir);

    }

    public void perform(Run build, FilePath workspace, TaskListener listener, PrintStream logger) throws IOException, InterruptedException {
        FilePath vrmHtmlDir = workspace.child(vrmHtmlReport);
        archiveHTMLReport(build, listener, vrmHtmlDir, vrmHtmlReport, HTML_ARCHIVE_DIR);

        // Process Coverage HTML reports if necessary 
        // if a single mergefile exists then the user might have changed the htmldir 
        if (covHTMLReports.size() == 1) {
            String covHtmlDir = covHTMLReports.get(0);

            FilePath covHtmlDirPath = vrmHtmlDir.child(covHtmlDir);
            if (!covHtmlDirPath.exists()) {
                archiveHTMLReport(build, listener, workspace.child(covHtmlDir), covHtmlDir, COV_ARCHIVE_DIR);
            }
        }

    }
}
