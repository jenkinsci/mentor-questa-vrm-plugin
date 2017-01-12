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


import java.io.File;

import java.io.IOException;
import java.net.URISyntaxException;

import java.util.HashMap;
import org.apache.commons.io.FileUtils;

import org.junit.Test;
import static org.junit.Assert.*;


public class QuestaCoverageTCLParserTest {
    
    public QuestaCoverageTCLParserTest() {
    }
    public String getFileContent(String name) throws URISyntaxException,IOException{
       
        return FileUtils.readFileToString(new File(QuestaCoverageTCLParserTest.class.getResource(name).toURI()));
    }

    /**
     * Test of parseTrendableAttributes method, of class QuestaCoverageTCLParser.
     */
    @Test
    public void testParseTrendableAttributes() throws Exception {
        System.out.println("parseTrendableAttributes");
        HashMap<String,String> attributes = new HashMap();
        String vcoverOutput = getFileContent("attribute-result.xml");
        QuestaCoverageTCLParser.parseTrendableAttributes(attributes, vcoverOutput);
        assertEquals("File contain 3 attributes", attributes.size(), 3);
    }

    /**
     * Test of parseCoverage method, of class QuestaCoverageTCLParser.
     */
    @Test
    public void testParseCoverage() throws Exception {
        System.out.println("parseCoverage");
        String coverageID = "merge_0.ucdb";
        String vcoveroutput = getFileContent("coverage-result.xml");
        QuestaUCDBResult result = QuestaCoverageTCLParser.parseCoverage(coverageID, vcoveroutput);
        assertNotNull("Coverage Result parsing not null",result);
        assertTrue("There are coverage values", result.containsCoverage());
        assertEquals("Total coverage is 48.1104",48.1104,result.getTotalCoverage(), 0.00001);
        assertEquals("Testplan coverage is 38.9318",38.9318,result.getTestplanCov(), 0.00001);
        assertEquals("There are 9 tests", 9, result.getTests().size());
       
    }
    
}
