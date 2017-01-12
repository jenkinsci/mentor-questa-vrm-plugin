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
package com.mentor.questa.jenkins;

import org.jvnet.hudson.test.JenkinsRule.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.html.HtmlImage;
import com.gargoylesoftware.htmlunit.html.HtmlTable;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.jvnet.hudson.test.JenkinsRule;


public class AbstractViews {
    @Rule
    public final JenkinsRule rule = new JenkinsRule();

    protected void assertClickableImageById(String description, WebClient wc, HtmlPage page, String imgSrc) throws Exception {
        assertClickableImage(description, wc, page, "//img[@id='" + imgSrc + "']");
    }
    protected void assertClickableImageByAlt(String description, WebClient wc, HtmlPage page, String imgSrc) throws Exception {
        assertClickableImage(description, wc, page, "//img[@alt='" + imgSrc + "']");
    }
    protected void assertClickableImageBySrc(String description, WebClient wc, HtmlPage page, String imgSrc) throws Exception {
        assertClickableImage(description, wc, page, "//img[@src='" + imgSrc + "']");
    }
    protected void assertClickableImage(String description, WebClient wc, HtmlPage page, String imgXPath) throws Exception {
       
        // Check that the image exists on the page and returned with a good status
        rule.assertXPath(page, imgXPath);
        String url = page.getUrl().toString();
        DomElement element=   page.getFirstByXPath(imgXPath);
        url = url.substring(url.lastIndexOf("jenkins") + 8);
        String imgSrc = element.getAttribute("src");
        if(imgSrc.contains("jenkins")){
            url = imgSrc.substring(imgSrc.lastIndexOf("jenkins")+8);
        } else {
            url +='/'+imgSrc;
        }
        Page imagePage = wc.goTo(url, "image/png");
        rule.assertGoodStatus(imagePage);
        // Check that the image is clickable..
        Object imageNode = page.getFirstByXPath(imgXPath);

        assertNotNull(description + " clickable not null", imageNode);
        assertTrue(description + " click renders an HTMLPage", imageNode instanceof HtmlImage);
    }

    protected void assertSummaryLink(HtmlPage page, String url, String content){
        String aXPath = "//a[@href='" +url+ "']";
        rule.assertXPath(page, aXPath);
        rule.assertXPathResultsContainText(page, aXPath, content);
  
    }
    
    protected HtmlTable assertHtmlTable(String description,HtmlPage page, String id, int rowCount){
        rule.assertXPath(page, "//table[@id]='"+id+"'");
        DomElement domTable = page.getElementById(id);
        assertNotNull(description+" table not null", domTable);
        assertTrue(description+" table is a html Table", domTable instanceof HtmlTable);
        HtmlTable htmlTable = (HtmlTable) domTable;
        assertEquals(description+" table contains "+rowCount+" rows",rowCount, htmlTable.getRowCount());
        return htmlTable;
    }
    
}
