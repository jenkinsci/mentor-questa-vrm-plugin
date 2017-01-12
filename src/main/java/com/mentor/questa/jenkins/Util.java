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
package com.mentor.questa.jenkins;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Functions;
import hudson.util.Area;
import hudson.util.TextFile;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;

/**
 *
 *
 */
public class Util {

    /**
     * This method returns the possibly trimmed contents of the file. The file
     * can be relative to a workspace.
     * 
     * @param keepLongStdio Disable trimming long files 
     * @param workspace the directory that contains the file
     * @param filename input filename 
     * @return the possibly trimmed file contents 
     * @throws IOException 
     */
    
    public static String possiblyTrimStdio(boolean keepLongStdio, File workspace, String filename) throws IOException {
        File file = new File(filename);

        if (!file.isAbsolute()) {

            // A temporary workaround for VRM logfile bug
            String[] temp = filename.split(" ");

            FileSet fs = hudson.Util.createFileSet(workspace, temp[0]);
            DirectoryScanner ds = fs.getDirectoryScanner();

            String[] files = ds.getIncludedFiles();
            File baseDir = ds.getBasedir();
            // Ignore empty things for now... 
            if (files.length == 0) {
                return "";
            }

            file = new File(baseDir, files[0]);
        }

        return possiblyTrimStdio(keepLongStdio, file);
    }

    /**
     * The method returns the possibly trimmed file contents.
     *
     * @param keepLongStdio if true, the file is eligible for trimming
     * @param stdio
     * @return
     * @throws IOException
     */
    @SuppressFBWarnings(value = "DM_DEFAULT_ENCODING", justification = "Expected behavior")
    private static String possiblyTrimStdio(boolean keepLongStdio, File stdio) throws IOException {
        final long len = stdio.length();
        if (keepLongStdio && len < 1024 * 1024) {
            return FileUtils.readFileToString(stdio);
        }

        final int halfMaxSize = 5000;

        long middle = len - halfMaxSize * 2;
        String head = "", tail = "";
        if (middle > 0) {
            TextFile tx = new TextFile(stdio);
            head = tx.head(halfMaxSize);
            tail = tx.fastTail(halfMaxSize);

            int headBytes = head.getBytes().length;
            int tailBytes = tail.getBytes().length;

            middle = len - (headBytes + tailBytes);
        }

        if (middle <= 0) {
            return FileUtils.readFileToString(stdio);
        }

        return head + "\n...[truncated " + middle + " bytes]...\n" + tail;
    }

    /**
     * This method formats the difference as "+/- d". For example 5.342445, 2
     * would return "+5.34", and -0.2347,3 would return "-0.235".
     *
     * @param d
     * @param prec
     * @return
     */
    public static String getDoubleDiffString(double d, int prec) {

        if (Math.abs(d) * Math.pow(10, prec) <= 0) {
            return "";
        }
        String s = String.format("%." + prec + "f", d);
        if (d > 0) {
            return "+" + s;
        } else {
            return s;
        }
    }

    /**
     * Method to return the size of the graph on the project page. This is
     * similar to dimensions used for the test trend graph of junit results
     *
     * @return
     */
    public static Area getProjectGraphArea() {
        Area res = Functions.getScreenResolution();
        if (res != null && res.width <= 800) {
            return new Area(250, 100);
        } else {
            return new Area(500, 200);
        }
    }

    public static String getFileName(String fileName) {

        int startIndex = Math.max(0, Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'))+1);

        int endIndex = fileName.lastIndexOf(".");
        if (endIndex == -1) {
            endIndex = fileName.length();
        }
        return fileName.substring(startIndex, endIndex);

    }
}
