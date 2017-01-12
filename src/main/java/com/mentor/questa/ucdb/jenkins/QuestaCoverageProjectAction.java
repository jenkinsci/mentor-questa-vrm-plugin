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

import hudson.model.Job;
import hudson.model.ProminentProjectAction;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.TestObject;
import java.lang.ref.WeakReference;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;


/**
 *
 * 
 */
public class QuestaCoverageProjectAction implements ProminentProjectAction {
 
    public final Job job;
    private final QuestaCoverageHistory history;
    private WeakReference<QuestaAttributesSettingsMap> graphMapRef;
    
    private transient int index=0;
  
    
    public QuestaCoverageProjectAction(AbstractTestResultAction testResultAction,QuestaCoverageResult coverageResult, int index) {
        this.job= testResultAction.run.getParent();
        this.index=index;
        this.history = new QuestaCoverageProjectHistory((TestObject)testResultAction.getResult(), coverageResult, getUrlName());
    }
   
    public String getGraphName() {
        return (index==0?"": this.history.getCoverageResult().getDisplayID()+" ")+"Coverage Result Trend";
    }

    @Override
    public String getDisplayName() {
        return getGraphName();
    }

    @Override
    public String getUrlName() {
        return  "projectAction"+((index==0)?"":index);
    }

    @Override
    public String getIconFileName() {
        return null;
    }
    
     public synchronized void setAttributesSetting(QuestaAttributesSettingsMap result) {
        CoverageUtil.saveAttributeSettingsMap(job, result);
        this.graphMapRef = new WeakReference<>(result);
    }
    
    public synchronized QuestaAttributesSettingsMap getAttributesSetting() {
        QuestaAttributesSettingsMap s = null;
        
        if(graphMapRef!=null) {
            s = graphMapRef.get();
        }

        if(s==null) {
            s = CoverageUtil.loadAttributeSettingsMap(job,(QuestaUCDBResult)history.getCoverageResult());
            graphMapRef = new WeakReference(s);
        }
   
        return s;
    }
    
    /*
    This method is in order to account for the difference in url between project action page (index.jelly) 
    and the project main page(floatingBox.jelly)
    */
    public Object getDynamic(String name, StaplerRequest req, StaplerResponse rsp) {
    	if (name.equals(getUrlName())) {
            return this;
    	} 
        return null;
    }

    public QuestaCoverageHistory getHistory() {
        return history;
    }

   
}
