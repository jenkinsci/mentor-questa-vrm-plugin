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

import com.mentor.questa.vrm.jenkins.QuestaVrmRegressionProjectAction;
import com.thoughtworks.xstream.XStream;
import hudson.Util;
import hudson.XmlFile;
import hudson.model.Job;
import hudson.model.TopLevelItem;
import hudson.tasks.junit.TestAction;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.TestObject;
import hudson.tasks.test.TestResult;
import hudson.util.HeapSpaceStringConverter;
import hudson.util.XStream2;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.sf.json.JSONObject;


/**
 *
 * 
 */
public class CoverageUtil {
    public static final int TOTAL_COVERAGE_COLS = 7, COVERAGE_NUMBER_COLS = 3;
    public static List<QuestaCoverageResult> getCoverageResult(TestResult testResult) {

        ArrayList< QuestaCoverageResult> coverageResults = new ArrayList<QuestaCoverageResult>();

        for (TestAction ta : testResult.getTestActions()) {
            if (ta instanceof QuestaCoverageAction) {
                QuestaCoverageAction coverageAction = (QuestaCoverageAction) ta;
                coverageResults.add(coverageAction.getCoverageResult());
            }

        }
        return coverageResults;

    }

    public static List<QuestaCoverageResult> getCoverageResult(AbstractTestResultAction testResultAction) {
        TestResult testResult;

        if (testResultAction.getResult() instanceof TestResult) {
            testResult = (TestResult) testResultAction.getResult();
        } else {
            return null;
        }
        return getCoverageResult(testResult);

    }

    public static List<String> getCoverageSummaryHeaders() {

        return Arrays.asList("Duration","Passed", "Failed", "Skipped", "Total", "CPU Time", "Total Coverage", "Testplan Coverage");
    }

    private static void insertCell(ArrayList<RowItem> row, RowItem insert){
        for (int i =0; i< COVERAGE_NUMBER_COLS; i++){
            row.add(insert);
        }
    
    }
    private static void completeEmptyRow(ArrayList<RowItem> row,RowItem insert) {
        for (int i =0; i< TOTAL_COVERAGE_COLS; i++){
            row.add(insert);
        }
    }
    
    /**
     * This method returns the coverage summary rows from a testresultAction if
     * it exists; The order of the rows are:Duration, Passed, Failed, Skipped, Total, CPU
     * Time, Total Coverage, Testplan Coverage (id, Passed, Failed, Skipped,
     * Total, CPU Time, Total Coverage, Testplan Coverage)*
     *
     * @param testResultAction
     * @return
     */
    public static List<RowItem> getCoverageSummaryRow(AbstractTestResultAction testResultAction) {
        TestResult testResult;
        ArrayList<RowItem> row = new ArrayList<RowItem>();
        RowItem emptyCell = new RowItem("--");
        RowItem nestedCell= new RowItem("...");
        row.add(new CoverageUtil.RowItem(testResultAction.run.getDurationString()));
        int defaultFailed = 0, defaultPassed = 0, defaultSkipped = 0;
        ArrayList< QuestaCoverageResult> coverageResults = new ArrayList<QuestaCoverageResult>();
        
        if (testResultAction.getResult() instanceof TestResult) {
            testResult = (TestResult) testResultAction.getResult();
        } else {
            completeEmptyRow(row, emptyCell);
            return row;
        }
        
        for (TestAction ta : testResult.getTestActions()) {
            if (ta instanceof QuestaCoverageAction) {
                QuestaCoverageResult coverageResult = ((QuestaCoverageAction) ta).getCoverageResult();
                // Filter out dummy tests inserted for having a history link...
                if(!coverageResult.isShadow()){
                    coverageResult.tally(testResult);
                    coverageResults.add(coverageResult);
                    defaultFailed +=  coverageResult.getFailCount();
                    defaultPassed +=  coverageResult.getPassCount();
                    defaultSkipped += coverageResult.getSkipCount();
                }
            }

        }

        DecimalFormat formatter = new DecimalFormat("#0.00");
        DecimalFormat covFormatter = new DecimalFormat("#0.0000");

        row.add(new RowItem(testResult.getPassCount() , testResult.getTotalCount(), formatter));
        row.add(new RowItem(testResult.getFailCount() , testResult.getTotalCount(), formatter));
        row.add(new RowItem(testResult.getSkipCount() , testResult.getTotalCount(), formatter));
        row.add(new RowItem(testResult.getTotalCount() ));

        if (coverageResults.isEmpty()) {
            insertCell(row, emptyCell);
        } else if (coverageResults.size() == 1) {
            if (!coverageResults.get(0).isShadow()) {
                row.add(new CoverageUtil.RowItem(Util.getTimeSpanString((long) coverageResults.get(0).getCpuTime() * 1000)));
                row.add(new RowItem(coverageResults.get(0).getTotalCoverage(),covFormatter));
                row.add(new RowItem(coverageResults.get(0).getTestplanCov(), covFormatter));
            } else {
                insertCell(row, emptyCell);
            }
        } else {
            insertCell(row, nestedCell);
        }

        if (defaultFailed + defaultPassed + defaultSkipped == 0) {
            return row;
        }
        
        // process nested rows... 
        
        if (coverageResults.size() > 1) {
            if (testResult.getTotalCount() != defaultFailed + defaultPassed + defaultSkipped) {
                defaultFailed = testResult.getFailCount() - defaultFailed;
                defaultPassed = testResult.getPassCount() - defaultPassed;
                defaultSkipped = testResult.getSkipCount() - defaultSkipped;
                row.add(new RowItem("(default)"));
                row.add(new RowItem(defaultPassed));
                row.add(new RowItem(defaultFailed));
                row.add(new RowItem(defaultSkipped));
                row.add(new RowItem(defaultFailed + defaultPassed + defaultSkipped));
                insertCell(row, emptyCell);

            }
            for (QuestaCoverageResult coverageResult : coverageResults) {
                if (coverageResult.getTotalCount() == 0 || coverageResult.isShadow()) {
                    continue;
                }
                row.add(new RowItem(coverageResult.getCoverageId()));
                row.add(new RowItem(coverageResult.getPassCount()));
                row.add(new RowItem(coverageResult.getFailCount()));
                row.add(new RowItem(coverageResult.getSkipCount()));
                row.add(new RowItem(coverageResult.getTotalCount()));
                row.add(new RowItem(Util.getTimeSpanString((long) coverageResult.getCpuTime() * 1000)));
                row.add(new RowItem(coverageResult.getTotalCoverage(), covFormatter));
                row.add(new RowItem(coverageResult.getTestplanCov(), covFormatter));
            }

        }
        return row;

    }

    public static QuestaCoverageResult getCoverageResult(AbstractTestResultAction testResultAction, String coverageId) {
        TestResult testResult;
        if (testResultAction.getResult() instanceof TestResult) {
            testResult = (TestResult) testResultAction.getResult();
        } else {
            return null;
        }

        return getCoverageResult(testResult, coverageId);

    }

    public static QuestaCoverageAction getCoverageAction(AbstractTestResultAction testResultAction, String coverageId) {
        TestResult testResult;
        if (testResultAction.getResult() instanceof TestResult) {
            testResult = (TestResult) testResultAction.getResult();
        } else {
            return null;
        }

        return getCoverageAction(testResult, coverageId);

    }

    public static QuestaCoverageAction getCoverageAction(TestObject t, String coverageId) {

        QuestaCoverageResult covResult = getCoverageResult(t, coverageId);

        return covResult != null ? covResult.getCoverageAction() : null;

    }

    public static QuestaCoverageResult getCoverageResult(TestObject t, String coverageId) {

        for (TestAction ta : t.getTestActions()) {
            if (ta instanceof QuestaCoverageAction) {
                QuestaCoverageAction coverageAction = (QuestaCoverageAction) ta;
                
                if ((coverageAction.getCoverageResult().getCoverageId().endsWith(coverageId) || coverageId.endsWith(coverageAction.getCoverageResult().getCoverageId()) )) {
                    return coverageAction.getCoverageResult();
                } else if (coverageAction.getCoverageResult() instanceof QuestaUCDBResult) {
                    QuestaCoverageResult test = ((QuestaUCDBResult) coverageAction.getCoverageResult()).getTest(coverageId);
                    if (test != null) {
                        return test;
                    }
                }
            }

        }
        return null;

    }

    public static List<RowItem> getLastCoverageResult(TopLevelItem item) {

        if (!(item instanceof Job)) {
            return null;
        }
        Job job = (Job) item;
        QuestaVrmRegressionProjectAction summaryAction = job.getAction(QuestaVrmRegressionProjectAction.class);
        if (summaryAction == null) {
            return null;
        }
        return summaryAction.getRows(1).get(0);

    }


    public static class RowItem {

        public String url;
        public String value;
        public String imgSrc;
        public String toolTip;

        public RowItem(String value, String toolTip) {
            this.value = value;
            this.toolTip = toolTip;

        }

        public RowItem(String value) {
            this(value, "");

        }

        public RowItem(double value, double total, DecimalFormat format) {
            this(value);
            this.toolTip = format.format(value/total * 100)+"%"; 
        }
        
        public RowItem(int value, double total, DecimalFormat format) {
            this(value);
            this.toolTip = format.format(value/total * 100)+"%"; 
        }
        
        public RowItem(int value) {
            this.value = value+"";
        }
        public RowItem(double value) {
            this.value = value+"";
        }
       
        public RowItem(double value, DecimalFormat format){
            this(format.format(value));
        }

        public JSONObject getJSONObject() {
            JSONObject jsonObj = new JSONObject();
            if (url != null) {
                jsonObj.element("url", url);
            }
            if (imgSrc != null) {
                jsonObj.element("imgSrc", imgSrc);
            }
            jsonObj.element("content", value);
            jsonObj.element("tooltip", value);
            return jsonObj;
        }

    }

    public static class CoverageRowItem extends RowItem {

        public CoverageRowItem(String value, String toolTip) {
            super(value, toolTip);
        }

    }
    
    static  void saveAttributeSettingsMap( Job job , QuestaAttributesSettingsMap graphMap) {
         
       synchronized(job){
        try {
            getDataFile(job).write(graphMap);
         
        } catch (IOException e) {
                
        }
     }
       
     
    }
    

    
    static QuestaAttributesSettingsMap loadAttributeSettingsMap(Job job , QuestaUCDBResult ucdbResult) {
        QuestaAttributesSettingsMap s;
        try {
            s = (QuestaAttributesSettingsMap)getDataFile(job).read();
         
        } catch (IOException e) {
            s = new QuestaAttributesSettingsMap(ucdbResult);
        }
        
        return s;
        
    }

    static XmlFile getDataFile(Job job) {
        return new XmlFile(XSTREAM, new File(job.getRootDir(), "ucdbAttributes.xml"));
    }
     
    private static final XStream XSTREAM = new XStream2();

    
    static {

        XSTREAM.alias("graphtab", QuestaAttributeGraphTab.class);
        XSTREAM.registerConverter(new HeapSpaceStringConverter(), 100);
    }
}
