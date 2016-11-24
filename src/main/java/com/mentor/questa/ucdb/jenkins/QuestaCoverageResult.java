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

import hudson.tasks.test.TestResult;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kohsuke.stapler.export.ExportedBean;
import org.kohsuke.stapler.export.Exported;

/**
 *
 * 
 */
@ExportedBean
public class QuestaCoverageResult implements Serializable {

    protected final HashMap<String, Double> coverageValues;
    protected final HashMap<String, String> attributesValues;

    private double totalCoverage;
    private double testplanCov;
    private String fileName;
	private boolean shadow;

    private transient TestResult testResult;
    private transient QuestaCoverageAction coverageAction;
    private transient QuestaUCDBResult ucdbResult;


    public QuestaCoverageResult() {
        this(false);
    }

    public QuestaCoverageResult(boolean shadow) {
                
        this.shadow= shadow;
        this.coverageValues = new HashMap<String, Double>();
        this.attributesValues = new HashMap<String, String>();
		
    }

    public void add(String coverageType, String value) {
        if (!value.equals("-") && !value.equals("na")) {
            double doubleValue = Double.parseDouble(value);
            if (doubleValue > 0) {
                coverageValues.put(coverageType, Double.parseDouble(value));
            }
        }
    }

    public void addAttributes(String name, String value) {
        if (name.equals("Filename") && fileName == null) {
            fileName = value;

        } else if (!value.equals("-")) {
            attributesValues.put(name, value);

        }
    }

    public boolean isTest() {
        return fileName != null && attributesValues.containsKey("TESTNAME");
    }

    public boolean isNumericAttribute(String attrKey) {
        String value = attributesValues.get(attrKey);
        Pattern doubleValue = Pattern.compile("\\d+(\\.\\d+)?");
        Matcher m = doubleValue.matcher(getAttributeValue(attrKey));
        if (m.find()) {
            return value.replace(m.group(0), "").trim().isEmpty();
        }
        return false;
    }

    public void setTestplanCov(String value) {
        if (!value.equals("-")) {
            this.testplanCov = Double.parseDouble(value);
        }

    }

    public void setCoverageAction(QuestaCoverageAction coverageAction) {
        this.coverageAction = coverageAction;
    }

    public void setTotalCoverage(String value) {
        if (!value.equals("-")) {
            this.totalCoverage = Double.parseDouble(value);
        }
    }

    public QuestaCoverageAction getCoverageAction() {
        return coverageAction;
    }

    public double getTotalCoverage() {
        return totalCoverage;
    }

    public double getTestplanCov() {
        return testplanCov;
    }

    public String getCoverageId() {
        return fileName;
    }

    public String getUrlName() {
        if (isTest()) {
            return attributesValues.get("TESTNAME");
        }
        return "coverage";

    }

    public int getPassCount() {
        return testResult.getPassCount();
    }

    public int getFailCount() {
        return testResult.getFailCount();
    }

    public int getSkipCount() {
        return testResult.getSkipCount();
    }

    public int getTotalCount() {
        return testResult.getTotalCount();
    }

    public double getCpuTime() {
        if (attributesValues.containsKey("CPUTIME")) {
            return getAttributeDoubleValue("CPUTIME");
        }
        return testResult.getDuration();
    }

    @Exported
    public HashMap<String, Double> getCoverageValues() {
        return coverageValues;
    }

    @Exported
    public HashMap<String, String> getAttributesValues() {
        return attributesValues;
    }

    @Exported
    public String getAttributeValue(String attrKey) {
        if (attributesValues.containsKey(attrKey)) {
            return attributesValues.get(attrKey);
        }

        return "";

    }

    public String getTestName() {
        return getAttributeValue("TESTNAME");
    }

    public String getAttributeUnit(String attrKey) {
        if (attrKey.equals("SIMTIME")) {
            return getAttributeValue("TIMEUNIT");
        }
        if (attributesValues.containsKey(attrKey)) {
            String value = attributesValues.get(attrKey);
            Pattern doubleValue = Pattern.compile("\\d+(\\.\\d+)?");
            Matcher m = doubleValue.matcher(getAttributeValue(attrKey));
            if (m.find()) {
                return value.replace(m.group(0), "").trim();
            }
        }
        return "";

    }

    @Exported
    public double getAttributeDoubleValue(String attrKey) {
        if (attributesValues.containsKey(attrKey)) {
            Pattern doubleValue = Pattern.compile("\\d+(\\.\\d+)?");
            Matcher m = doubleValue.matcher(attributesValues.get(attrKey));
            if (m.find()) {
                return Double.parseDouble(m.group(0));
            }
        }
        return 0;

    }

    public int getTestStatus() {
        if (attributesValues.containsKey("TESTSTATUS")) {
            return attributesValues.get("TESTSTATUS").charAt(0) - '0';
        }
        return 4;
    }

    public String toolTipString() {
        StringBuilder str = new StringBuilder();
        str.append("Total Coverage:");
        str.append(totalCoverage);
        str.append("\n");
        str.append("Testplan Coverage:");
        str.append(testplanCov);
        str.append("\n");

        for (String key : coverageValues.keySet()) {
            str.append("\t");
            str.append(key);
            str.append(": ");
            str.append(coverageValues.get(key));
            str.append("\n");
        }
        return str.toString();
    }

    public String toolTipString(List<String> attr) {
        if (attr == null) {
            return toolTipString();
        }
        StringBuilder str = new StringBuilder();
        for (String key : attr) {
            str.append("\t");
            str.append(key);
            str.append(": ");
            str.append(getAttributeDoubleValue(key));
            str.append("\n");
        }
        return str.toString();
    }

    private ArrayList<String> getUserDefinedAttributes() {
        ArrayList<String> otherAttrs = new ArrayList<String>();
        String[] predefined = new String[]{
            "VSIMARGS",
            "TESTCMD",
            "TESTCOMMENT",
            "LOGNAME",
            "WLFNAME",
            "RUNCWD",
            "VRM_CONTEXT",
            "SEED",
            "ORIGFILENAME",
            "HOSTNAME",
            "HOSTOS",
            "BROWSER_FLAG",
            "MTIVERSION",
            "USERNAME",
            "SIMTIME",
            "TIMEUNIT",
            "DATE",
            "CPUTTIME",
            "MEMUSAGE",
            "CVGPEAKMEM",
            "CVGTOTALMEM",
            "CVGPEAKTIME",
            "TESTNAME",
            "TESTSTATUS",
            "MERGELEVEL"
        };
        HashSet<String> preSet = new HashSet<String>(Arrays.asList(predefined));

        for (String key : attributesValues.keySet()) {
            if (!preSet.contains(key)) {
                otherAttrs.add(key);
            }
        }
        
        return otherAttrs;
    }

    public ArrayList<String> getTrendableAttributes() {
        ArrayList<String> result = new ArrayList<String>();

        for (String attr : attributesValues.keySet()) {
            if (isNumericAttribute(attr)) {
                result.add(attr);
            }
        }
        return result;

    }

    public ArrayList<String> getOtherAttributes() {
        ArrayList<String> result = new ArrayList<String>();
        for (String key : getUserDefinedAttributes()) {
            result.add(key);
        }
        return result;
    }

    public String[] getSimulationKeys() {
        return new String[]{
            "VSIMARGS",
            "TESTCMD",
            "TESTCOMMENT",
            "LOGNAME",
            "WLFNAME",
            "RUNCWD",
            "VRM_CONTEXT",
            "SEED",
            "MTIVERSION"
        };
    }

    public boolean containsTest(String otherTest) {
        String test = getTestName();
        return (test.length() > 0 && test.equals(otherTest));
    }

    public QuestaCoverageResult getTest(String testname) {
        if (containsTest(testname)) {
            return this;
        }
        return null;

    }

    public QuestaCoverageResult createEmptyCopy() {
        QuestaCoverageResult copy = new QuestaCoverageResult(true);
        copy.addAttributes("Filename", fileName);
        copy.addAttributes("TESTNAME", getTestName());
        return copy;

    }

    @Exported
    public boolean containsCoverage() {
        return !coverageValues.isEmpty();
    }

    public boolean isShadow() {
        return shadow;
    }

    public synchronized void tally(TestResult testResult) {
        this.testResult = testResult;
    }

    public void setUcdbResult(QuestaUCDBResult ucdbResult) {
        this.ucdbResult = ucdbResult;
    }
    
    
    public QuestaUCDBResult getUcdbResult(){
        return ucdbResult;
    }
}
