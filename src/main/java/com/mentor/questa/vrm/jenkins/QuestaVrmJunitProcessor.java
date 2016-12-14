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

import hudson.AbortException;
import hudson.FilePath;
import hudson.Util;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.TestResultAction;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;

/**
 *
 *
 */
public class QuestaVrmJunitProcessor  implements Serializable {
   
    public void perform(Run build, FilePath workspace, TaskListener listener, PrintStream logger) throws IOException, InterruptedException{
        recordTestResult(build,workspace, listener, logger);
    }
    private TestResult recordTestResult(Run build, FilePath workspace, TaskListener listener, PrintStream logger) throws IOException, InterruptedException {
        TestResultAction existingAction = build.getAction(TestResultAction.class);
        final long buildTime = build.getTimestamp().getTimeInMillis();
        final long nowMaster = System.currentTimeMillis();

        TestResult existingTestResults = null;

        if (existingAction != null) {
            existingTestResults = existingAction.getResult();
        }

        TestResult result = getTestResult(workspace, "vm-jUnit.xml", existingTestResults, buildTime, nowMaster);
        if (result != null) {
            TestResultAction action;
            if (existingAction == null) {
                action = new TestResultAction(build, result, listener);
            } else {
                action = existingAction;
                action.setResult(result, listener);
            }

            if (result.getPassCount() == 0 && result.getFailCount() == 0) {
                logger.println("All test reports are empty.");
            }

            if (existingAction == null) {
                build.getActions().add(action);
            }
        }
        return result;
    }

    private TestResult getTestResult(final FilePath workspace,
            final String junitFilePattern,
            final TestResult existingTestResults,
            final long buildTime, final long nowMaster)
            throws IOException, InterruptedException {

        return workspace.act(new jenkins.SlaveToMasterFileCallable<TestResult>() {
            @Override
            public TestResult invoke(File ws, VirtualChannel channel) throws IOException {
                final long nowSlave = System.currentTimeMillis();
                FileSet fs = Util.createFileSet(ws, junitFilePattern);
                DirectoryScanner ds = fs.getDirectoryScanner();
                String[] files = ds.getIncludedFiles();

                if (files.length == 0) {
                    //no equivalent junit results.. 
                    throw new AbortException("Error parsing temporary junit file. The equivalent junit results are not generated.");
                }
                try {
                    if (existingTestResults == null) {
                        return new TestResult(buildTime + (nowSlave - nowMaster), ds, true);
                    } else {
                        existingTestResults.parse(buildTime + (nowSlave - nowMaster), ds);
                        return existingTestResults;
                    }
                } catch (IOException ioe) {
                    throw new IOException(ioe);
                }
            }

        });

    }

}

