/*
 * The MIT License
 *
 * Copyright 2017 Mentor Graphics.
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


import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTable;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.mentor.questa.jenkins.AbstractViews;
import hudson.model.FreeStyleProject;
import static org.junit.Assert.*;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;

public class QuestaCoverageViewsTest extends AbstractViews {

    public QuestaCoverageViewsTest() {
    }
        
    @LocalData
    @Test
    public void testNoCoveragePage() throws Exception {
        System.out.println("Testing no coverage project page");
        FreeStyleProject proj = (FreeStyleProject) rule.jenkins.getItem("NoCoverage+UCDB");
        assertNotNull("We should have a project named NoCoverage+UCDB " , proj);

        JenkinsRule.WebClient wc = rule.createWebClient();

        HtmlPage projectPage = wc.getPage(proj);
        assertSummaryLink(projectPage, "lastCompletedBuild/testReport/","Latest Test Result");

        // should check failure count 
       
        assertSummaryLink(projectPage, "lastCompletedBuild/questavrmreport/", "Latest Regression Result");
        
        //check images
        assertClickableImageBySrc("Test trend graph", wc, projectPage, "test/trend");
        assertTrue("There is no coverage graph", projectPage.getByXPath("//img[@alt='[Coverage graph]']").isEmpty());
      
    }
    
    @LocalData
    @Test
    public void testAlternateCoveragePage() throws Exception {
        System.out.println("Testing alternate merge project page");
        FreeStyleProject proj = (FreeStyleProject) rule.jenkins.getItem("Alternate_Merge");
        assertNotNull("We should have a project named  Alternate_Merge" , proj);

     
        JenkinsRule.WebClient wc = rule.createWebClient();
        
        HtmlPage projectPage = wc.getPage(proj);
        // there should be a single coverage result link.. 
        assertSummaryLink(projectPage, "covhtmlreport", "Latest Questa Coverage Report");
        assertTrue("There is no more link", projectPage.getByXPath("//div[@id='more-link']").isEmpty());

        assertClickableImageById("Coverage Result trend 1st Graph", wc, projectPage, "VRMDATA/merge_0.ucdb");
        assertClickableImageById("Coverage Result trend 2nd Graph", wc, projectPage, "VRMDATA/merge_1.ucdb");
        // Check that the regression result page contains a single graph and two histories
        HtmlPage regressionPage = wc.getPage(proj.getBuildByNumber(6), "questavrmreport");
        assertNotNull("Regression page not null", regressionPage );
        assertClickableImageByAlt("Coverage Bar Chart", wc, regressionPage, "[Coverage Results Chart]");
        assertEquals("A single coverage graph", 1,regressionPage.getByXPath("//img[@alt='[Coverage Results Chart]']").size());

        rule.assertXPathResultsContainText(regressionPage, "//a", "Questa merge_1 Coverage History");
        rule.assertXPathResultsContainText(regressionPage, "//a", "Questa merge_0 Coverage History");
        assertSummaryLink(regressionPage, "coverage1",  "Coverage History");
        assertSummaryLink(regressionPage, "coverage2",  "Coverage History");
       

    }
    
    @LocalData
    @Test
    public void testTwoMergePage() throws Exception {
        System.out.println("Testing alternate merge pages");
        FreeStyleProject proj = (FreeStyleProject) rule.jenkins.getItem("two-merge");
        assertNotNull("We should have a project named two-merge" , proj);

     
        JenkinsRule.WebClient wc = rule.createWebClient();

        HtmlPage projectPage = wc.getPage(proj);
       
        assertSummaryLink(projectPage, "covhtmlreport1", "Latest merge2 Coverage Report");
        assertSummaryLink(projectPage, "covhtmlreport2", "Latest merge Coverage Report");
        
        // Check the  summary table size 3*3+1 
        HtmlTable SummaryTable =assertHtmlTable("Summary", projectPage, "coverageSum", 10);
        assertEquals("Details in summary table are invisible", 3, SummaryTable.getByXPath("tbody[contains(@style,'display: none')]").size() );
        projectPage.executeJavaScript("showCoverageDetails('#3')");
        assertEquals("Details of #3 are shown", 0, SummaryTable.getByXPath("tbody[@id='#3' and contains(@style,'display: none')]").size() );
        assertEquals("Remaining Details in summary table are invisible", 2, SummaryTable.getByXPath("tbody[contains(@style,'display: none')]").size() );
        projectPage.executeJavaScript("hideCoverageDetails('#3')");
        assertEquals("Details in summary table are invisible again", 3, SummaryTable.getByXPath("tbody[contains(@style,'display: none')]").size());
        
        assertClickableImageById("Coverage Result trend 1st Graph", wc, projectPage, "VRMDATA2/merge.ucdb");
        assertClickableImageByAlt("Attribute Graph of first UCDB", wc, projectPage, "[Metrics]");
        assertClickableImageById("Coverage Result trend 2nd Graph", wc, projectPage, "VRMDATA2/merge2.ucdb");
        assertClickableImageByAlt("Attribute Graph of 2nd UCDB", wc, projectPage, "[Metric4]");
        
         HtmlPage regressionPage = wc.getPage(proj.getBuildByNumber(3), "questavrmreport");
        assertNotNull("Regression page not null", regressionPage );
        assertClickableImageByAlt("Coverage Bar Chart", wc, regressionPage, "[Coverage Results Chart]");
        assertEquals("Two coverage graphs", 2,regressionPage.getByXPath("//img[@alt='[Coverage Results Chart]']").size());
        rule.assertXPathResultsContainText(regressionPage, "//a", "Questa merge2 Coverage History");
        rule.assertXPathResultsContainText(regressionPage, "//a", "Questa merge Coverage History");
        assertSummaryLink(regressionPage, "coverage1",  "Coverage History");
        assertSummaryLink(regressionPage, "coverage2",  "Coverage History");
        
    }
    @LocalData
    @Test
    public void testCoverageHistoryPage() throws Exception {
        System.out.println("Testing alternate merge project page");
        FreeStyleProject proj = (FreeStyleProject) rule.jenkins.getItem("Alternate_Merge");
        assertNotNull("We should have a project named  Alternate_Merge" , proj);

        JenkinsRule.WebClient wc = rule.createWebClient();
      
        // Get the history page of merge_1 
        HtmlPage regressionPage = wc.getPage(proj.getBuildByNumber(6), "questavrmreport");
        HtmlAnchor anchor = regressionPage.getAnchorByText("Questa merge_1 Coverage History");
        HtmlPage historyPage= anchor.click(); 
        assertNotNull("Regression page not null", historyPage );
        
        assertClickableImageByAlt("Coverage Graph", wc, historyPage, "[Coverage graph]");
        assertClickableImageByAlt("Attributes Graph", wc, historyPage, "[Metrics]");
        
        HtmlTable histTable =assertHtmlTable("History Table", historyPage, "testresult", 3);
        assertEquals("Table columns equal 8", 8,histTable.getRow(0).getCells().size());
        assertEquals("Values of build #5","Alternate_Merge #5", histTable.getCellAt(1, 0).getTextContent());
        assertEquals("Values of build #3","Alternate_Merge #3", histTable.getCellAt(2, 0).getTextContent());
       

    }
    
    
}
