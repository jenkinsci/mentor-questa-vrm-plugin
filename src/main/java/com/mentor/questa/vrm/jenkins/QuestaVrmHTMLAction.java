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

import hudson.Util;
import hudson.model.ProminentProjectAction;
import hudson.util.HttpResponses;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.ServletException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;


public class QuestaVrmHTMLAction implements ProminentProjectAction {

    private Map<String, String> fileChecksums = null;

    private final File rootDir;

    private final String urlName;

    private final String indexFile;

    private final String iconName;

    private final String title;

 
    public QuestaVrmHTMLAction(File rootDir, String urlName, String indexFile, String iconName, String title, String... safeExtensions) {
        this.rootDir = rootDir;
        this.urlName = urlName;
        this.indexFile = indexFile;
        this.iconName = iconName;
        this.title = title; 
    }

    private void addFile(String relativePath, String checksum) {
        this.fileChecksums.put(relativePath, checksum);
    }

    private String getChecksum(String file) {
        if (file == null || !fileChecksums.containsKey(file)) {
            throw new IllegalArgumentException(file + " has no checksum recorded");
        }
        return fileChecksums.get(file);
    }

    private String calculateChecksum(@Nonnull File file) throws NoSuchAlgorithmException, IOException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);

            byte[] bytes = new byte[1024];
            while (-1 != (fis.read(bytes))) {
                sha1.update(bytes);
            }
        }  finally {
            if (fis != null) {
                fis.close();
            }
        }

        return Util.toHexString(sha1.digest());
    }

    private void processDirectory(@Nonnull File directory, @Nullable String path)  {
       File[] files = directory.listFiles();
       if(files==null) return;
       try {
        for (File file : files) {

            String relativePath = file.getName();
            if (path != null) {
                relativePath = path + "/" + relativePath;
            }

            if (file.isDirectory()) {
                processDirectory(file, relativePath);
            }
            if (file.isFile()) {
                addFile(relativePath, calculateChecksum(file));
            }
        }} catch (IOException e){
        
        } catch(NoSuchAlgorithmException e){
        
        }
    }

    @Override
    public String getIconFileName() {
        return iconName;
    }

    @Override
    public String getDisplayName() {
        return title;
    }

    @Override
    public String getUrlName() {
        return urlName;
    }

    public File getRootDir() {
        return rootDir;
    }

    public void doDynamic(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        // Initialize the file checksums 
        if (fileChecksums==null) {
            fileChecksums = new HashMap<String, String>();
            processDirectory(rootDir,null);
        }
        
        if (req.getRestOfPath().equals("")) {
            // Redirect to the index page
            throw HttpResponses.redirectTo(indexFile);
        }

        String fileName = req.getRestOfPath();
        if (fileName.startsWith("/")) {
            fileName = fileName.substring(1);
        }

        File file = new File(getRootDir(), fileName);

        if (!new File(getRootDir(), fileName).exists()) {
            throw HttpResponses.notFound();
        }

        // file not in the marked as safe, abort 
        if (!fileChecksums.containsKey(fileName)) {
            throw HttpResponses.notFound();
        }

        // Check that the file is inside the archive rootDir
        if (!file.getAbsolutePath().startsWith(this.getRootDir().getAbsolutePath())) {
            throw HttpResponses.notFound();
        }

       
        String actualChecksum;
        try {
            actualChecksum = calculateChecksum(file);
        } catch (NoSuchAlgorithmException nse) {
           
            throw new IllegalStateException(nse);
        }

        String expectedChecksum = getChecksum(fileName);
        
        // check checksum values
        if (!expectedChecksum.equals(actualChecksum)) {
            throw HttpResponses.forbidden();
        }

        serveSafeFile(file, req, rsp);

    }

    private void serveSafeFile(File file, StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        // serve the file without Content-Security-Policy
        long lastModified = file.lastModified();
        long length = file.length();
        InputStream in = null;
        try {
            in = new FileInputStream(file);
            rsp.serveFile(req, in, lastModified, -1, length, file.getName());
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }
   
}
