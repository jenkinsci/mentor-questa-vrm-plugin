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


import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlDefinitionList;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTable;
import com.mentor.questa.jenkins.AbstractViews;

import hudson.model.FreeStyleProject;


import org.junit.Test;
import static org.junit.Assert.*;

import org.jvnet.hudson.test.JenkinsRule.WebClient;
import org.jvnet.hudson.test.recipes.LocalData;


public class QuestaVrmViewsTest extends AbstractViews {

    public QuestaVrmViewsTest() {
    }
    

    @LocalData
    @Test
    public void testProjectPage() throws Exception {
        System.out.println("Testing project page");
        FreeStyleProject proj = (FreeStyleProject) rule.jenkins.getItem("Demo");
        assertNotNull("We should have a project named Demo", proj);

        System.out.println(proj.getBuilds().size());

        WebClient wc = rule.createWebClient();

        HtmlPage projectPage = wc.getPage(proj);
        
        // Checking summaries links 
        assertSummaryLink(projectPage, "lastCompletedBuild/testReport/","Latest Test Result");
        assertSummaryLink(projectPage, "lastCompletedBuild/questavrmreport/", "Latest Regression Result");
        assertSummaryLink(projectPage, "/jenkins/job/Demo/questavrmhtmlreport", "Latest Questa VRM Report");
        assertSummaryLink(projectPage, "/jenkins/job/Demo/covhtmlreport", "Latest Questa Coverage Report");
        
        // checking displayed failures 
        rule.assertXPathResultsContainText(projectPage, "//td", "(3 failures  / -2)");
        rule.assertXPathResultsContainText(projectPage, "//td", "(1 non-test failures  / ±0)");
        
        // check summary table
        assertHtmlTable("Summary", projectPage, "coverageSum", 6);
      
        
        // simulate clicking on the More... link 
        rule.assertXPath(projectPage, "//div[@id='more-link']");
       /**This causes a JavaScript exception 
        * projectPage.executeJavaScript("getData()");
        * assertHtmlTable("Summary", projectPage, "coverageSum", 7);
      **/  
        //check images
        assertClickableImageBySrc("Test trend graph", wc, projectPage, "test/trend");
        assertClickableImage("Build time trend", wc, projectPage, "//img[@alt='[Build time graph]']");
        assertClickableImageById("Coverage Result trend", wc, projectPage, "VRMDATA/merge.ucdb");
       
    }
    
    @LocalData
    @Test
    public void testBuildPage() throws Exception {
        System.out.println("Testing Build page");
        FreeStyleProject proj = (FreeStyleProject) rule.jenkins.getItem("Demo");
        assertNotNull("We should have a project named Demo" , proj);

        WebClient wc = rule.createWebClient();

        HtmlPage buildPage = wc.getPage(proj.getBuildByNumber(5));
       
        // Checking summaries links 
        assertSummaryLink(buildPage, "testReport/","Test Result");
        assertSummaryLink(buildPage, "questavrmreport/", "Questa VRM - Regression Result");
       
        // checking displayed failures 
        rule.assertXPathResultsContainText(buildPage, "//td", "(1 non-test failures  / ±0)");
        assertSummaryLink(buildPage, "questavrmreport/nightly_pawn~2_trendScript","nightly/pawn~2/trendScript" );
        
        assertSummaryLink(buildPage, "/jenkins/job/Demo/5/questavrmhostinfo",  "Questa Host Utilization");
    }
    
    @LocalData
    @Test
    public void testRegressionPage() throws Exception {
        System.out.println("Testing Regression page");
        FreeStyleProject proj = (FreeStyleProject) rule.jenkins.getItem("Demo");
        assertNotNull("We should have a project named Demo", proj);

        WebClient wc = rule.createWebClient();

        HtmlPage regressionPage = wc.getPage(proj.getBuildByNumber(5), "questavrmreport");
        assertNotNull("Regression page not null", regressionPage );
        assertClickableImageByAlt("Coverage Bar Chart", wc, regressionPage, "[Coverage Results Chart]");
        // Checking failed table 
        HtmlTable failedTable = assertHtmlTable("Failures", regressionPage,"failed-table", 7);
      
        assertEquals("1 failed action rows", 1, failedTable.getByXPath("tbody/tr[@class='action-row']").size() );
        assertEquals("Action  rows are invisible", 1, failedTable.getByXPath("tbody/tr[@class='action-row' and contains(@style,'display: none')]").size() );
        // checking regression table
        HtmlTable regrTable = assertHtmlTable("Regression", regressionPage,"vrmregr", 24);
        assertEquals("5 action rows", 5, regrTable.getByXPath("tbody/tr[@class='action-row']").size() );
        assertEquals("Action  rows are invisible", 5, regrTable.getByXPath("tbody/tr[@class='action-row' and @style='display: none;']").size() );
        // execute show actions 
       /** regressionPage.executeJavaScript("flipView()");
        assertEquals("No action rows are invisible", 0, regressionPage.getByXPath("//tr[@class='action-row' and @style='display: none;']").size() );
        regressionPage.executeJavaScript("flipView()");
        assertEquals("Back to invisible action rows", 6, regressionPage.getByXPath("//tr[@class='action-row' and @style='display: none;']").size() );
        **/
        // Checking sidepanel links 
        assertSummaryLink(regressionPage, "coverage",  "Questa Coverage History");
    }
    
    @LocalData
    @Test
    public void testHostUtilizationPage() throws Exception {
        System.out.println("Testing Host Utilization page");
        FreeStyleProject proj = (FreeStyleProject) rule.jenkins.getItem("Demo");
        assertNotNull("We should have a project named Demo", proj);

        WebClient wc = rule.createWebClient();

        HtmlPage hostUtilizationPage = wc.getPage(proj.getBuildByNumber(5), "questavrmhostinfo");
        assertNotNull("Host Utilization page not null", hostUtilizationPage );
        // check image 
        assertClickableImageByAlt("Host Utilization", wc, hostUtilizationPage, "[Host Utilization Chart]");
        // Checking empty non-visible summary table
        assertHtmlTable("Summary", hostUtilizationPage,"summary-table", 1);
        rule.assertXPath(hostUtilizationPage, "//table[@id='summary-table' and contains(@style, 'display:none')]");
      
        // checking main table
        HtmlTable regrTable = assertHtmlTable("Main", hostUtilizationPage,"vrmregr", 19);
        assertEquals("All test rows", 18, regrTable.getByXPath("tbody/tr[@class='test-row']").size() );
        
        // click on the image: throws Java Script run time error
        //hostUtilizationPage.executeJavaScript("getSummary(1483348647000)");
        //assertHtmlTable("Summary", hostUtilizationPage,"summary-table", 18);
        
        // press on show Actions
        HtmlPage flippedhostUtilizationPage = wc.getPage(proj.getBuildByNumber(5), "questavrmhostinfo/flipMode");
        assertClickableImageByAlt("Flipped Host Utilization", wc, flippedhostUtilizationPage, "[Host Utilization Chart]");
        regrTable = assertHtmlTable("Flipped Main", flippedhostUtilizationPage,"vrmregr", 24);
        assertEquals("All action rows", 5, regrTable.getByXPath("tbody/tr[@class='action-row']").size() );
       // flippedhostUtilizationPage.executeJavaScript("getSummary(1483348642000)");
       // assertHtmlTable("Summary", flippedhostUtilizationPage,"summary-table", 2);
        
    }
    
    @LocalData
    @Test
    public void testTestPage() throws Exception {
        System.out.println("Testing test page");
        FreeStyleProject proj = (FreeStyleProject) rule.jenkins.getItem("Demo");
        assertNotNull("We should have a project named Demo", proj);

        WebClient wc = rule.createWebClient();

        HtmlPage testPage = wc.getPage(proj.getBuildByNumber(5), "questavrmreport/nightly/pawn~1/");
        assertNotNull("test page not null", testPage );
       
        // Checking test statistics table 
        assertHtmlTable("Test Statistics ", testPage,"ucdbstats1", 7);
        assertClickableImageByAlt("Coverage Bar Chart", wc, testPage, "[Coverage Results Chart]");
      
        // Verify "Test Record Attribute"
         
        rule.assertXPath(testPage, "//dl[@id='simulation']");
        DomElement simulationDom = testPage.getElementById("simulation", true);
        assertTrue("Simulation setting definition list exists" ,simulationDom instanceof HtmlDefinitionList);
        HtmlDefinitionList simulation = (HtmlDefinitionList) simulationDom;
        
        assertEquals("Simulation list contains 7 elements", 14,simulation.getChildElementCount());
        
         // Verify "other attributes"
         
        rule.assertXPath(testPage, "//dl[@id='otherAttributes']");
        DomElement otherAttr = testPage.getElementById("otherAttributes", true);
        assertEquals(" Other attributes list contains 6 elements ", 12,otherAttr.getChildElementCount());
        
        // Checking sidepanel links 
        
        assertSummaryLink(testPage, "pawn~1",  "Questa Coverage History");
    }
}
