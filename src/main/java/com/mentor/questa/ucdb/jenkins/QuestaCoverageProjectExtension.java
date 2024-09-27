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

import hudson.Extension;

import hudson.model.Action;
import hudson.model.Job;
import hudson.model.Run;
import hudson.tasks.test.AbstractTestResultAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import jenkins.model.TransientActionFactory;

/**
 *
 * 
 */
@Extension
public class QuestaCoverageProjectExtension extends TransientActionFactory<Job> {

    @Override
    public Class<Job> type() {
        return Job.class;
    }

    @Override
    public Collection<? extends Action> createFor(Job job) {
        ArrayList<Action> actions = new ArrayList<Action>();
        if (job == null) {
            return actions;
        }
        Run lastBuild = job.getLastSuccessfulBuild();
        if (lastBuild == null) {
            return actions;
        }

        final List<AbstractTestResultAction> buildActions = lastBuild
                .getActions(AbstractTestResultAction.class);
        for (AbstractTestResultAction buildAction : buildActions) {
            List<QuestaCoverageResult> coverageResults = CoverageUtil.getCoverageResult(buildAction);
            if (coverageResults == null) {
                continue;
            }
            int index = (coverageResults.size() == 1) ? 0 : 1;
            for (QuestaCoverageResult coverageResult : coverageResults) {
                actions.add(new QuestaCoverageProjectAction(buildAction, coverageResult, index++));
            }
        }
        return actions;
    }

}
