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
import hudson.Launcher;
import hudson.Util;

import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;

import hudson.tasks.test.TestResult;
import hudson.tasks.test.TestResultParser;
import hudson.util.IOUtils;
import java.io.File;
import java.io.FileOutputStream;

import java.io.Serializable;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.MasterToSlaveFileCallable;
import net.sf.json.JSONArray;

import net.sf.json.JSONObject;
import net.sf.json.util.JSONTokener;
import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;

/**
 *
 *
 */
public class QuestaVrmResultsParser extends TestResultParser implements Serializable {
    private static final Logger logger = Logger.getLogger(QuestaVrmResultsParser.class.getName());
    
    private static final String REGR_MERGEFILES = "mergefiles",
            REGR_HTMLREPORTS = "covhtmlreports",
            REGR_OPTNS = "options",
            REGR_DURATION = "duration",
            REGR_TIMESTAMP = "timestamp",
            REGR_LOGFILE = "logfile",
            REGR_COLS = "columns",
            ABORT_MSG = "No results found, check your configuration.",
            REGR_SUMMARY = "json.js";

    @Override
    public hudson.tasks.test.TestResult parseResult(String vrmdata, Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        final long buildTime = run.getTimestamp().getTimeInMillis();
        final long timeOnMaster = System.currentTimeMillis();
        return workspace.act(new ParseResultCallable(vrmdata, buildTime, timeOnMaster, false));
    }

    private static final class ParseResultCallable extends MasterToSlaveFileCallable<TestResult> {

        public enum ColumnIndex {

            ACTION(0),
            TESTNAME(1),
            SEED(2),
            STATUS(3),
            REASON(4),
            COVERAGE(5),
            TPLANCOV(6),
            USERNAME(7),
            HOSTNAME(8),
            QUEUED(9),
            ELAPSED(10),
            UCDBFILE(11),
            MERGEFILE(12),
            LAUNCHTIME(13),
            STARTTIME(14),
            DONETIME(15);

            private final int index;

            private ColumnIndex(int index) {
                this.index = index;
            }

            public int getIndex() {
                return index;
            }

        }

        private String getRelativePath(File wsPath, String path) {

            if (wsPath != null && path.startsWith(wsPath.getPath())) {
                return path.substring(wsPath.getPath().length() + 1);
            }
            return path;

        }
        
        private String processFilePath(File ws, String relativePath, String filename) {
            // Workaround for vrm windows path bug
            if (File.separator.equals("\\")){
                filename=filename.replace('/', '\\');
            }

            File file = new File(filename);
            if (file.isAbsolute()) {
                return getRelativePath(ws, filename);
            } else {
                return relativePath!=null? relativePath+ System.getProperty("file.separator") :""+ filename;
            }

        }
        
         private String processFilePath(File ws, String filename){
             return processFilePath(ws, vrmdata, filename);
         }
        
        private final long buildTime;
        private final String testResults;
        private final String vrmdata;
        private final long nowMaster;
        /**
         * This is currently not used, but can be latter when making which files
         * to archive configurable
         */
        private final boolean keepLongStdio;

        private ParseResultCallable(String vrmdata, long buildTime, long nowMaster, boolean keepLongStdio) {
            this.buildTime = buildTime;
            this.testResults = REGR_SUMMARY;
            this.vrmdata = vrmdata;
            this.nowMaster = nowMaster;
            this.keepLongStdio = keepLongStdio;
        }

        @Override
        public TestResult invoke(File ws, VirtualChannel channel) throws IOException {
            final long nowSlave = System.currentTimeMillis();

            FileSet fs = Util.createFileSet(ws, testResults);
            DirectoryScanner ds = fs.getDirectoryScanner();

            String[] files = ds.getIncludedFiles();
            File baseDir = ds.getBasedir();
            if (files.length == 0) {
                // no json file found, mostly a configuration error.
                // error or fatal problem
                throw new AbortException(ABORT_MSG);
            }

            File reportFile = new File(baseDir, files[0]);

            if ((buildTime + (nowSlave - nowMaster) - 3000/*error margin*/ <= reportFile.lastModified())) {

                QuestaVrmRegressionResult regressionResult = new QuestaVrmRegressionResult(vrmdata);
                JSONObject jsonobject = JSONObject.fromObject(new JSONTokener(FileUtils.readFileToString(reportFile)));
                JSONObject options = jsonobject.getJSONObject(REGR_OPTNS);

                ArrayList<String> mrgfiles = new ArrayList<String>();
                if (jsonobject.containsKey(REGR_MERGEFILES)) {
                    
                    for (Object mrgfile : jsonobject.getJSONArray(REGR_MERGEFILES)) {
                        String mrgfilename = mrgfile.toString();
                        mrgfiles.add(processFilePath(ws, mrgfilename));
                        logger.log(Level.INFO, "Regression has mergefile: "+ mrgfiles.get(mrgfiles.size()-1));
                    }

                }
                
                regressionResult.setMergeFiles(mrgfiles);
                ArrayList<String> covhtmlreports = new ArrayList<String>();
                if (jsonobject.containsKey(REGR_HTMLREPORTS)) {
                    for (Object covhtmlreport : jsonobject.getJSONArray(REGR_HTMLREPORTS)) {
                        covhtmlreports.add(processFilePath(ws, null,covhtmlreport.toString()));
                    }
                }

                regressionResult.setCovHTMLReports(covhtmlreports);

                if (options.containsKey(REGR_DURATION)) {
                    regressionResult.setDuration((float) options.getDouble(REGR_DURATION));
                }

                if (options.containsKey(REGR_TIMESTAMP)) {
                    regressionResult.setTimestamp(options.getString(REGR_TIMESTAMP));
                }

                if (options.containsKey(REGR_LOGFILE)) {
                    regressionResult.setLogfile(options.getString(REGR_LOGFILE));
                }

                int[] colIndex = columnMapping(jsonobject.getJSONArray(REGR_COLS));

                int actionIndex = colIndex[ColumnIndex.ACTION.getIndex()];

                if (actionIndex == -1) {
                    logger.log(Level.SEVERE, "No actions found in the JSON file");
                    throw new AbortException(ABORT_MSG);

                }

                for (Object data : jsonobject.getJSONArray("results")) {
                    JSONArray dataarray = JSONArray.fromObject(data);
                    if (dataarray.getString(actionIndex).endsWith("execScript") && !dataarray.getString(colIndex[ColumnIndex.TESTNAME.getIndex()]).equals("--")) {
                        // Process test actions
                        QuestaVrmTestResult testResult = new QuestaVrmTestResult(regressionResult);
                        initResult(testResult, dataarray, colIndex);
                        regressionResult.addAction(testResult);

                    } else {
                        // Process non-test actions 
                        QuestaVrmActionResult actionResult = new QuestaVrmActionResult(regressionResult);
                        initResult(actionResult, dataarray, colIndex);
                        if (actionResult.isFailed()) {
                            actionResult.initStderr(ws);
                            actionResult.initStdout(ws);
                        }
                        regressionResult.addAction(actionResult);

                    }

                }
                logger.log(Level.INFO, "Total actions "+ regressionResult.getTotalActionCount()+", failed actions "+ regressionResult.getFailedActionCount());
                logger.log(Level.INFO, "Total tests "+ regressionResult.getTotalCount()+", failed tests "+ regressionResult.getFailCount());
                
                OutputStreamWriter out = null;
                try {
                    File outFile = new File(ws, "vm-jUnit.xml");
                    out = new OutputStreamWriter(new FileOutputStream(outFile), "UTF-8");
                    out.write(regressionResult.getXmlSnippet(ws));
                } catch(IOException e){
                     logger.log(Level.SEVERE, "Exception while writing junit temp file "+ e.toString());
                }finally {
                    IOUtils.closeQuietly(out);
                }
                return regressionResult;

            }
            return null;

        }



        private int[] columnMapping(JSONArray cols) {

            int[] colIndex = new int[ColumnIndex.values().length];
            Arrays.fill(colIndex, -1);

            for (int i = 0; i < cols.size(); i++) {

                String col = cols.getString(i);

                if (col.equals("action")) {
                    colIndex[ColumnIndex.ACTION.getIndex()] = i;
                } else if (col.equals("testname")) {
                    colIndex[ColumnIndex.TESTNAME.getIndex()] = i;
                } else if (col.equals("seed")) {
                    colIndex[ColumnIndex.SEED.getIndex()] = i;
                } else if (col.equals("reason")) {
                    colIndex[ColumnIndex.REASON.getIndex()] = i;
                } else if (col.equals("status")) {
                    colIndex[ColumnIndex.STATUS.getIndex()] = i;
                } else if (col.equals("hostname")) {
                    colIndex[ColumnIndex.HOSTNAME.getIndex()] = i;
                } else if (col.equals("ucdbfile")) {
                    colIndex[ColumnIndex.UCDBFILE.getIndex()] = i;
                } else if (col.equals("mergefile")) {
                    colIndex[ColumnIndex.MERGEFILE.getIndex()] = i;
                } else if (col.equals("coverage")) {
                    colIndex[ColumnIndex.COVERAGE.getIndex()] = i;
                } else if (col.equals("tplancov")) {
                    colIndex[ColumnIndex.TPLANCOV.getIndex()] = i;
                } else if (col.equals("elapsed")) {
                    colIndex[ColumnIndex.ELAPSED.getIndex()] = i;
                } else if (col.equals("queued")) {
                    colIndex[ColumnIndex.QUEUED.getIndex()] = i;
                } else if (col.equals("starttime")) {
                    colIndex[ColumnIndex.STARTTIME.getIndex()] = i;
                } else if (col.equals("donetime")) {
                    colIndex[ColumnIndex.DONETIME.getIndex()] = i;
                } else if (col.equals("launchtime")) {
                    colIndex[ColumnIndex.LAUNCHTIME.getIndex()] = i;
                }
            }
            return colIndex;

        }

        private void initResult(QuestaVrmAbstractResult result, JSONArray dataarray, int[] colindex) {
            result.setAction(parseString(dataarray, colindex[ColumnIndex.ACTION.getIndex()]));
            result.setTestname(parseString(dataarray, colindex[ColumnIndex.TESTNAME.getIndex()]));
            result.setHostname(parseString(dataarray, colindex[ColumnIndex.HOSTNAME.getIndex()]));
            result.setReason(parseString(dataarray, colindex[ColumnIndex.REASON.getIndex()]));
            result.setSeed(parseString(dataarray, colindex[ColumnIndex.SEED.getIndex()]));
            result.setStatus(parseString(dataarray, colindex[ColumnIndex.STATUS.getIndex()]));
            result.setUcdbfile(parseString(dataarray, colindex[ColumnIndex.UCDBFILE.getIndex()]));
            result.setMergefile(parseString(dataarray, colindex[ColumnIndex.MERGEFILE.getIndex()]));

            result.setDuration(parseFloat(dataarray, colindex[ColumnIndex.ELAPSED.getIndex()]));
            result.setQueued(parseFloat(dataarray, colindex[ColumnIndex.QUEUED.getIndex()]));
            result.setCoverage(parseFloat(dataarray, colindex[ColumnIndex.COVERAGE.getIndex()]));
            result.setTestplanCov(parseFloat(dataarray, colindex[ColumnIndex.TPLANCOV.getIndex()]));

            result.setStartTime(parseDate(dataarray, colindex[ColumnIndex.STARTTIME.getIndex()]));
            result.setLaunchTime(parseDate(dataarray, colindex[ColumnIndex.LAUNCHTIME.getIndex()]));
            result.setDoneTime(parseDate(dataarray, colindex[ColumnIndex.DONETIME.getIndex()]));

        }

        private String parseString(JSONArray dataarray, int index) {
            if (index == -1) {
                return "--";
            }
            return dataarray.getString(index);
        }

        private float parseFloat(JSONArray dataarray, int index) {
            String value = parseString(dataarray, index);
            if (value.equals("--")) {
                return 0;
            }
            return Float.parseFloat(value);

        }

        private Date parseDate(JSONArray dataarray, int index) {
            String value = parseString(dataarray, index);
            DateFormat df = new SimpleDateFormat("E MMM dd HH:mm:ss z yyyy");
            try {
                return df.parse(value);

            } catch (Exception e) {
            }
            return null;

        }
    }

}

