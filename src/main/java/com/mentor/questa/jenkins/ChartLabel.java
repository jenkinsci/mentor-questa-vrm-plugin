package com.mentor.questa.jenkins;

import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.Build;
import hudson.model.Run;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.TestResult;
import jenkins.model.Jenkins;

import java.awt.*;
import java.text.SimpleDateFormat;

/*
 * The ChartLabel class is used to represent the X-axis data of the different trend charts
 * This is used by the test result, coverage and attributes charts
 */
public class ChartLabel implements Comparable<ChartLabel> {
	final SimpleDateFormat dateMonthFirstFormat = new SimpleDateFormat("MM/dd");
	final SimpleDateFormat dateDayFirstFormat = new SimpleDateFormat("dd/MM");
	
    TestResult o;

    String url;

    public ChartLabel(TestResult o) {
        this.o = o;
        this.url = null;

    }

    public TestResult getO() {
        return o;
    }
    private Class getTestResultClass(){
        try{
            return Class.forName("com.mentor.questa.vrm.jenkins.QuestaVrmRegressionBuildAction");
        }catch (ClassNotFoundException e){
            return AbstractTestResultAction.class;
        }
        
    }
    public String getActionUrl() {
        String actionUrl = o.getTestResultAction().getUrlName(); 
        Action questaAction = o.getRun().getAction(getTestResultClass());
        if (questaAction!=null) {
            actionUrl = questaAction.getUrlName();
        }
        return actionUrl;
    }
    public String getUrl() {
        if (this.url == null) {
            generateUrl();
        }
        return url;
    }

    private void generateUrl() {
        Run<?, ?> build = o.getRun();
        String buildLink = build.getUrl();
        this.url= "";
        Jenkins inst = Util.getActiveInstance();
        this.url = inst.getRootUrlFromRequest() ;
        this.url += buildLink + getActionUrl() + o.getUrl();
    }
    
    @Override
    public int compareTo(ChartLabel that) {
        return this.o.getRun().number - that.o.getRun().number;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ChartLabel)) {
            return false;
        }
        ChartLabel that = (ChartLabel) o;
        return this.o == that.o;
    }

    public Color getColor() {
      return null;
    }

    @Override
    public int hashCode() {
        return o.hashCode();
    }

    @Override
    public String toString() {
        Run run = o.getRun();
        GraphsByBuildDates flag = Util.getGraphXAxisDisplay(run.getParent());
    	String l = null;
    	if (flag == null) {
    		l = run.getDisplayName();
            String s = run instanceof AbstractBuild ? ((AbstractBuild) run).getBuiltOnStr() : null;
            if (s != null) {
                l += ' ' + s;
            }
        }
    	else {
    		if (flag.getDateMonthFormat() == true)
    			l = dateMonthFirstFormat.format(((Build) run).getTime());
    		else 
    			l = dateDayFirstFormat.format(((Build) run).getTime());
    	}
        return l;
    }

}
