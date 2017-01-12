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

import hudson.tasks.junit.SuiteResult;
import hudson.tasks.test.TestResult;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;


/**
 *
 * 
 */
public class QuestaUCDBResult extends QuestaCoverageResult {


    private final HashMap<String, QuestaCoverageResult> testsMap;
    private final HashMap<String, Double> trendableAttributes;
    private transient int passCount, failCount, skipped;
    private transient hudson.tasks.junit.TestResult testResult;
    private transient double cputime = 0;

    public QuestaUCDBResult(String coverageId) {
        this(coverageId, false);
    }
    public QuestaUCDBResult(String coverageId, boolean shadow) {
        super(shadow);
        this.addAttributes("Filename", coverageId);
        this.testsMap = new HashMap<>();
        trendableAttributes = new HashMap<>();
    }

    public void addTest(QuestaCoverageResult testResult) {
        testsMap.put(testResult.getTestName(), testResult);
    }

    public void addTrendableAttribute(String name, String value) {
        if (!value.equals("-")) {
            trendableAttributes.put(name, Double.parseDouble(value));
        }

    }

    public void addTrendableAttribute(String name, Double value) {
        trendableAttributes.put(name, value);
    }

    @Override
    public String getAttributeValue(String attrKey) {
        if (trendableAttributes.containsKey(attrKey)) {
            return trendableAttributes.get(attrKey) + "";
        }
        return super.getAttributeValue(attrKey);
    }

    @Override
    public double getAttributeDoubleValue(String attrKey) {
        if (trendableAttributes.containsKey(attrKey)) {
            return trendableAttributes.get(attrKey);
        }
        return super.getAttributeDoubleValue(attrKey);
    }

    @Override
    public ArrayList<String> getTrendableAttributes() {

        ArrayList<String> result = new ArrayList<>();
        if (trendableAttributes != null) {
            result.addAll(trendableAttributes.keySet());
        }
        for (String attr : attributesValues.keySet()) {
            if (isNumericAttribute(attr)) {
                result.add(attr);
            }
        }
        return result;

    }

    public ArrayList<String> getGlobalTrendableAttributes() {

        ArrayList<String> result = new ArrayList<>();
        if (trendableAttributes != null) {
            result.addAll(trendableAttributes.keySet());
        }

        return result;

    }

    public Collection<QuestaCoverageResult> getTests() {
        return testsMap.values();
    }

    @Override
    public double getCpuTime() {
        return cputime;
    }

    @Override
    public int getPassCount() {
        return passCount;
    }

    @Override
    public int getFailCount() {
        return failCount;
    }

    @Override
    public int getSkipCount() {
        return skipped;
    }

    @Override
    public int getTotalCount() {
        return skipped + failCount + passCount;
    }

    @Override
    public boolean containsTest(String otherTest) {
        return testsMap.containsKey(otherTest);
    }

    @Override
    public QuestaCoverageResult getTest(String testname) {
        if (containsTest(testname)) {
            QuestaCoverageResult covResult = testsMap.get(testname);
            covResult.setUcdbResult(this);
            return covResult;
        }
        return null;

    }
    
    @Override
    public void tally(TestResult testResult) {
        if (this.testResult != null || !(testResult instanceof hudson.tasks.junit.TestResult)) {
            return;
        }

        this.testResult = (hudson.tasks.junit.TestResult) testResult;
        cputime = 0;

        for (QuestaCoverageResult test : testsMap.values()) {
            test.setUcdbResult(this);
            for (SuiteResult suite : this.testResult.getSuites()) {
                hudson.tasks.test.TestResult temp = suite.getCase(test.getTestName());
                if (temp != null) {
                    test.tally(temp);
                    cputime += test.getCpuTime();
                    int teststatus = test.getTestStatus();
                    if (teststatus <= 1) {
                        passCount++;
                    } else if (teststatus == 4) {
                        skipped++;
                    } else {
                        failCount++;
                    }
                }
            }

        }
    }


    @Override
    public QuestaUCDBResult createEmptyCopy() {
        QuestaUCDBResult copy = new QuestaUCDBResult(this.getCoverageId(),true);
		
        for (QuestaCoverageResult covRes : testsMap.values()) {
            copy.addTest(covRes.createEmptyCopy());
        }
        return copy;
    }

    public void copyEmptyTests(QuestaCoverageResult previous) {

        if (previous instanceof QuestaUCDBResult) {
            for (QuestaCoverageResult covRes : ((QuestaUCDBResult) previous).getTests()) {
                if (!testsMap.containsKey(covRes.getTestName())) {
                    addTest(covRes.createEmptyCopy());
                }
            }
        } else {
            if (!testsMap.containsKey(previous.getTestName())) {
                addTest(previous.createEmptyCopy());
            }
        }
    }

    @Override
    public void setUcdbResult(QuestaUCDBResult ucdbResult) {
        // Do nothing
    }

    @Override
    public QuestaUCDBResult getUcdbResult() {
        return this;
    } 
    private static final long serialVersionUID = 1L;
}
