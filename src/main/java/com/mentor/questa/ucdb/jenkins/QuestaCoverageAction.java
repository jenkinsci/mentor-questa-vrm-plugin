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

import com.mentor.questa.jenkins.Util;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.tasks.junit.TestAction;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.MetaTabulatedResult;
import hudson.tasks.test.TestResult;
import hudson.util.ChartUtil;
import java.awt.Color;
import java.awt.Paint;
import java.io.IOException;

import java.util.Calendar;
import java.util.HashMap;

import java.util.Map;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.RectangleInsets;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 *
 * 
 */
public class QuestaCoverageAction extends TestAction {

    private static final int DEFAULT_GRAPH_WIDTH = 500;
    private static final int DEFAULT_GRAPH_HEIGHT = 200;
    private final QuestaCoverageResult coverageResult;
    private final TestResult testObject;

    private transient AbstractTestResultAction buildAction;
    private transient int index = 0;

    public QuestaCoverageAction(TestResult testObject, QuestaCoverageResult coverageResult) {

        this.buildAction = testObject.getTestResultAction();
        this.testObject = testObject;
        this.coverageResult = coverageResult;
        this.coverageResult.setCoverageAction(this);

    }

    public QuestaCoverageAction(TestResult testObject, QuestaCoverageResult coverageResult, int index) {
        this(testObject, coverageResult);
        this.index = index;

    }

    private String getName(){
        return ((index != 0) ? coverageResult.getDisplayID()+" ": "");
    }
    public String getGraphName() {
        return "Questa "+getName()+"Coverage Results" ;
    }

    @Override
    public String getDisplayName() {
        return "Questa "+getName()+"Coverage History";
    }

    @Override
    public String getIconFileName() {
        return "/plugin/mentor-questa-vrm/icons/report.png";
    }

    @Override
    public String getUrlName() {
        return coverageResult.getUrlName() + ((index != 0) ? index : "");
    }

    public QuestaCoverageResult getCoverageResult() {
        return coverageResult;
    }

    public TestResult getTestObject() {
        return testObject;
    }

    public HashMap<String, String> getAttributesValues() {
        return coverageResult.getAttributesValues();
    }

    public String getAttributeValue(String key) {
        return coverageResult.getAttributesValues().get(key);
    }

    public boolean isLeafLevel() {
        return !(testObject instanceof MetaTabulatedResult);
    }

    public QuestaCoverageResult getPreviousResult() {
        QuestaCoverageResult result = null;
        for (TestResult current = testObject.getPreviousResult(); result == null && current != null; current = current.getPreviousResult()) {
            result = CoverageUtil.getCoverageResult(current.getTestResultAction(), coverageResult.getCoverageId());

        }
        return result;
    }

    public QuestaCoverageResult getCoverageResultFromTest(TestResult testResult) {
        return CoverageUtil.getCoverageResult(testResult, coverageResult.getCoverageId());
    }

    public QuestaCoverageHistory getHistory() {
        return new QuestaCoverageHistory(testObject, coverageResult, getUrlName());
    }

    public int asInt(double value) {
        return (int) value;
    }

    public String getDoubleDiffString(double d) {
        return Util.getDoubleDiffString(d, 4);
    }

    public Object getDynamic(String name, StaplerRequest req, StaplerResponse rsp) {
        //System.out.println(name);
        if (name.equals(getUrlName())) {
            return this;
        } else {
            return (Object) testObject.getDynamic(name, req, rsp);
        }

    }

    /**
     * If the last build is the same, no need to regenerate the graph. Browser
     * should reuse it's cached image.
     *
     * @param req request
     * @param rsp response
     * @return true, if new image does NOT need to be generated, false
     * otherwise.
     */
    @SuppressFBWarnings(value ="NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE",justification = "Null value not possible"  )
    protected boolean newGraphNotNeeded(StaplerRequest req, StaplerResponse rsp) {
        Calendar t = buildAction.owner.getProject().getLastBuild().getTimestamp();
        return req.checkIfModified(t, rsp);
    }

    /**
     * Get the default graph width
     *
     * @return width of graph in pixels
     */
    public int getGraphWidth() {
        return DEFAULT_GRAPH_WIDTH;
    }

    /**
     * Get the default graph height
     *
     * @return height of graph in pixels
     */
    public int getGraphHeight() {
        return DEFAULT_GRAPH_HEIGHT;
    }
    
    private JFreeChart createBarChart(final StaplerRequest req, final CategoryDataset dataset, final String title) {
        JFreeChart chart = ChartFactory.createBarChart(
                title, //title
                null, //categoryaxislabel, 
                null, //valueaxislabel    
                dataset, //dataset
                PlotOrientation.HORIZONTAL, //orientation
                false, //legend
                true, //tooltips
                false //urls
        );

        // NOW DO SOME OPTIONAL CUSTOMISATION OF THE CHART...
        chart.setBackgroundPaint(Color.white);

        final CategoryPlot plot = chart.getCategoryPlot();

        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlinePaint(null);
        plot.setForegroundAlpha(0.8f);

        final BarRenderer cir = new BarRenderer() {
            private final Paint[] colors = {Color.red, Color.blue, Color.green,
                Color.yellow, Color.orange, Color.cyan,
                Color.magenta, Color.blue};

            /**
             * Returns the paint for an item. Overrides the default behavior
             * inherited from AbstractSeriesRenderer.
             *
             * @param row the series.
             * @param column the category.
             *
             * @return The item color.
             */
            @Override
            public Paint getItemPaint(final int row, final int column) {
                return colors[column % colors.length];
            }

            @Override
            public boolean equals(Object obj) {
                return super.equals(obj);
            }

            @Override
            public int hashCode() {
                return super.hashCode(); //To change body of generated methods, choose Tools | Templates.
            }
            
            
        };
        cir.setMaximumBarWidth(0.35);
        plot.setRenderer(cir);

        // crop extra space around the graph
        plot.setInsets(new RectangleInsets(0, 0, 0, 5.0));

        return chart;
    }

    private CategoryDataset populateBarChart() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        for (Map.Entry<String, Double> coverageEntry : coverageResult.getCoverageValues().entrySet()) {
            dataset.addValue(coverageEntry.getValue(), "a", coverageEntry.getKey());
        }

        return dataset;
    }

    public void doBar(final StaplerRequest req, StaplerResponse rsp) throws IOException {
        if (ChartUtil.awtProblemCause != null) {
            rsp.sendRedirect2(req.getContextPath() + "/images/headless.png");
            return;
        }

        if (newGraphNotNeeded(req, rsp)) {
            return;
        }

        final CategoryDataset dataset = populateBarChart();

        new hudson.util.Graph(-1, getGraphWidth(), getGraphHeight()) {
            @Override
            protected JFreeChart createGraph() {
                return createBarChart(req, dataset, getGraphName());
            }
        }.doPng(req, rsp);

    }
    
    public void doBarMap(final StaplerRequest req, StaplerResponse rsp) throws IOException {
        if (newGraphNotNeeded(req, rsp)) {
            return;
        }

        final CategoryDataset dataset = populateBarChart();

        new hudson.util.Graph(-1, getGraphWidth(), getGraphHeight()) {
            @Override
            protected JFreeChart createGraph() {
                return createBarChart(req, dataset, getGraphName());
            }
        }.doMap(req, rsp);

    }

}
