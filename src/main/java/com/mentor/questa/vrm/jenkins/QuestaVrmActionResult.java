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

import java.io.File;
import java.io.IOException;
import com.mentor.questa.jenkins.Util;
import org.kohsuke.stapler.export.Exported;

/**
 *
 * 
 */
public class QuestaVrmActionResult extends QuestaVrmAbstractResult {
    
    private String stdout, stderr;
     
    public QuestaVrmActionResult(QuestaVrmRegressionResult regressionResult) {
        super(regressionResult);
    }
    
  
    @Exported(visibility = 999)
    @Override
    public String getStderr() {
        return stderr;
    }

    @Exported(visibility = 999)
    @Override
    public String getStdout() {
        return stdout;
    }
    @Override
    public String getName() {
        return getAction(); 
    }

    @Exported(visibility = 999)
    @Override
    public String getDisplayName() {
        return getAction();
    }

    @Override
    public String getFullDisplayName() {
        return getAction(); 
    }

    void initStderr(File ws) throws IOException {
        this.stderr = Util.possiblyTrimStdio(false, ws, getStderrPattern());
    }
    
     void initStdout(File ws) throws IOException {
        this.stdout = Util.possiblyTrimStdio(false,ws, getLogPattern());
    }

}
