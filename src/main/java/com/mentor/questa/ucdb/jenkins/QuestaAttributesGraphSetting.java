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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Arrays;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.export.ExportedBean;

/**
 *
 * 
 */
@ExportedBean
public class QuestaAttributesGraphSetting {

    private final List<QuestaAttributeGraphTab> testLevelGraphs;
    private final List<QuestaAttributeGraphTab> mergeLevelGraphs;

    /** Keep a list of test/merge level attributes seen for that project
     For population of multi-select lists
    **/
    private final HashSet<String> testLevelAttributes;
    private final HashSet<String> mergeLevelAttributes;

    public QuestaAttributesGraphSetting( List<String> trendableAttributes) {
        testLevelAttributes = new HashSet<>(Arrays.asList(new String[]{"SIMTIME", "CPUTIME", "MEMUSAGE", "CVGPEAKMEM", "CVGTOTALTIME", "CVGTOTALMEM"}));
        mergeLevelAttributes = new HashSet<>(trendableAttributes);
        
        testLevelGraphs = new ArrayList<>();
        
        testLevelGraphs.add(new QuestaAttributeGraphTab("CPU Time", "CPUTIME"));
        testLevelGraphs.add(new QuestaAttributeGraphTab("Simulation Time", "SIMTIME"));
        testLevelGraphs.add(new QuestaAttributeGraphTab("Memory Usage", "MEMUSAGE"));
        testLevelGraphs.add(new QuestaAttributeGraphTab("CVG Total Mem", "CVGTOTALMEM"));
        testLevelGraphs.add(new QuestaAttributeGraphTab("CVG Peak Mem", "CVGPEAKMEM"));
        testLevelGraphs.add(new QuestaAttributeGraphTab("CVG Peak Time", "CVGPEAKTIME"));
        
        mergeLevelGraphs = new ArrayList<>();
        for (String attr: trendableAttributes){
            mergeLevelGraphs.add(new QuestaAttributeGraphTab(attr, attr));
        }
    }

    public List<QuestaAttributeGraphTab> getTestLevelGraphs() {
        return testLevelGraphs;
    }

    public List<QuestaAttributeGraphTab> getGraphs(boolean test) {
        return test ? testLevelGraphs : mergeLevelGraphs;
    }

    public HashSet<String> getTestLevelAttributes() {
        return testLevelAttributes;
    }

    public HashSet<String> getMergeLevelAttributes() {
        return mergeLevelAttributes;
    }

    public HashSet<String> getAvailableAttributes(boolean test) {
        return test ? testLevelAttributes : mergeLevelAttributes;
    }

    public List<QuestaAttributeGraphTab> getMergeLevelGraphs() {
        return mergeLevelGraphs;
    }

    private void updateAvaliableAttributes(HashSet<String> originalAttrs, List<String> newAttrs) {
        originalAttrs.addAll(newAttrs);
    }

    void updateTestLevelAttributes(List<String> testLevelAttributes) {
        updateAvaliableAttributes(this.testLevelAttributes, testLevelAttributes);
    }

    void updateMergeLevelAttributes(List<String> mergeLevelAttributes) {
        updateAvaliableAttributes(this.mergeLevelAttributes, mergeLevelAttributes);
    }

    private synchronized void updateGraphs(List<QuestaAttributeGraphTab> graphs, JSONObject jsonData) {
        graphs.clear();
        if (jsonData.containsKey("attributePublishers")) {

            if (jsonData.get("attributePublishers") instanceof JSONArray) {
                JSONArray array = jsonData
                        .getJSONArray("attributePublishers");

                for (int i = 0; i < array.size(); i++) {
                    JSONObject graphConfig = array.getJSONObject(i);

                    if (!graphConfig.getString("graphName").trim().isEmpty() && !graphConfig.getJSONArray("attributes").isEmpty()) {
                        graphs.add(new QuestaAttributeGraphTab(graphConfig.getString("graphName"), graphConfig.getJSONArray("attributes")));
                    }

                }
            } else {
                JSONObject graphConfig = jsonData.getJSONObject("attributePublishers");
                if (!graphConfig.getString("graphName").trim().isEmpty() && !graphConfig.getJSONArray("attributes").isEmpty()) {
                    graphs.add(new QuestaAttributeGraphTab(graphConfig.getString("graphName"), graphConfig.getJSONArray("attributes")));
                }

            }
        }
    }

    void updateGraphs(boolean test, JSONObject jsonData) {
        updateGraphs(test ? testLevelGraphs : mergeLevelGraphs, jsonData);
    }


}
