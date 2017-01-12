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

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;;
import hudson.tasks.Builder;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.TestResult;
import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;

public class QuestaVrmResultsParserTest {
    @Rule 
    public final JenkinsRule rule= new JenkinsRule();
    private FreeStyleProject project; 
    private final String projectName ="vrm_parser_test";
    static QuestaVrmRegressionResult regressionResult;
    
    
    public static final class QuestaVrmResultsParserBuilder extends Builder implements Serializable{

        private final String vrmdata="VRMDATA";
        private final String jsonfile= "json.js";
        
        @Override
        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        
            QuestaVrmResultsParser instance = new QuestaVrmResultsParser();
            
            FilePath testWs = build.getWorkspace();
			// touch the json file so that it is recent
            testWs.child(jsonfile).touch(System.currentTimeMillis());
            try{
            regressionResult =(QuestaVrmRegressionResult) instance.parseResult(vrmdata, build, testWs, launcher, listener);
            } catch (Exception e){
                System.out.println(e.toString());
                System.out.println(e.getStackTrace());
            }
            new QuestaVrmJunitProcessor().perform(build, testWs, listener, listener.getLogger());
            return true; 
        }
        
        
  
    }
    
    public QuestaVrmResultsParserTest() {
    }
    

    
    @Before
    public void setUp() throws Exception {
                project = rule.createFreeStyleProject(projectName);
        project.getBuildersList().add(new  QuestaVrmResultsParserBuilder());
        regressionResult = null;
        
    }

    /**
     * Test of parseResult method, of class QuestaVrmResultsParser.
     */
    @LocalData
    @Test
    public void testParseResult() throws Exception {
        System.out.println("parseResult");

        FreeStyleBuild build = project.scheduleBuild2(0).get(100, TimeUnit.MINUTES);
        assertNotNull(build);
        
        assertNotNull("Regression result not null",regressionResult);
        assertEquals("Total action script count is 37 ", 37, regressionResult.getTotalActionCount());
        assertEquals("Total action script failures count is 1 ", 1, regressionResult.getFailedActionCount());
        assertEquals("Total action script passing count is 33 ", 33,regressionResult.getPassActionCount());
        assertEquals("Total action script skipped count is 3 ", 3,regressionResult.getSkipActionCount());
        
        assertEquals("Total test count is 26 ",  26,regressionResult.getTotalCount());
        assertEquals("Total test failures count is 1 ", 1, regressionResult.getFailCount());
        assertEquals("Total test passing count is 25 ", 25, regressionResult.getPassCount());
        assertEquals("Total test skipped count is 0 ", 0, regressionResult.getSkipCount());

	assertTrue("There is a single mergefile", regressionResult.getMergeFiles().size()==1);
        assertTrue("There is a single mergefile coverage report", regressionResult.getCovHTMLReports().size()==1);
        
        // check junit equivalence
        TestResult testResult =  (TestResult)((AbstractTestResultAction)build.getAction(AbstractTestResultAction.class)).getResult();
        assertEquals("Total junit test count check ",  regressionResult.getTotalCount(), testResult.getTotalCount());
        assertEquals("Total junit test failures count check ", regressionResult.getFailCount(), testResult.getFailCount());
        assertEquals("Total junit tests passing count check ", regressionResult.getPassCount(), testResult.getPassCount());
        assertEquals("Total junit test skipped count check ", regressionResult.getSkipCount(), testResult.getSkipCount());
        
        // Check the existance of logs of the failed test only! 
        assertTrue(testResult.getFailedTests().size()==1);
        TestResult failedtest = (TestResult)testResult.getFailedTests().toArray()[0];
        assertTrue("Standard out of failed test is saved", failedtest.getStdout().length()!=0);
        
    }
    
}
