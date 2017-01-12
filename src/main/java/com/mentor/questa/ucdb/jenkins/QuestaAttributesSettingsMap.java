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

import java.util.HashMap;
import java.util.Map;

/**
 *
 * 
 */
public class QuestaAttributesSettingsMap {
    Map<String, QuestaAttributesGraphSetting> graphMap; 

    public QuestaAttributesSettingsMap() {
        graphMap = new HashMap<>();
    }

    public QuestaAttributesSettingsMap(QuestaUCDBResult ucdbResult) {
        this();
        if(ucdbResult!=null){
            initAttributeSetting(ucdbResult);
        }
    }
    private QuestaAttributesGraphSetting initAttributeSetting( QuestaUCDBResult ucdbResult){
            QuestaAttributesGraphSetting graphSetting = new QuestaAttributesGraphSetting(ucdbResult.getGlobalTrendableAttributes());
            graphMap.put(ucdbResult.getCoverageId(), graphSetting);
            return graphSetting;
    }
    
    public void addGraphSetting(String coverageId,QuestaAttributesGraphSetting graphSetting){
        graphMap.put(coverageId, graphSetting);
    }
    public void addGraphSetting(QuestaUCDBResult ucdbResult){
        initAttributeSetting(ucdbResult);
    }
    
    public boolean containsGraphSetting(String coverageId){
        return graphMap.containsKey(coverageId);
    }
    public QuestaAttributesGraphSetting getGraphSetting(String coverageId){
        if(containsGraphSetting(coverageId)){
            return graphMap.get(coverageId);
        }
        return null;
        
    }
     public QuestaAttributesGraphSetting getGraphSetting(QuestaUCDBResult ucdbResult){
        if( ucdbResult == null)
            return null;
        
        if(containsGraphSetting(ucdbResult.getCoverageId())){
            return graphMap.get(ucdbResult.getCoverageId());
        }
        
        return initAttributeSetting(ucdbResult);
        
    }
    
    
    
}
