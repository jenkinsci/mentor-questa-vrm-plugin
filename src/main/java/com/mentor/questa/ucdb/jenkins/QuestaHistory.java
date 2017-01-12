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

import hudson.model.AbstractBuild;
import hudson.model.Run;

import hudson.tasks.test.TestObject;
import jenkins.model.Jenkins;
import hudson.tasks.test.TestResult;
import hudson.util.Area;
import hudson.util.ChartUtil;
import hudson.util.ColorPalette;
import hudson.util.DataSetBuilder;
import hudson.util.Graph;
import hudson.util.ShiftedCategoryAxis;
import hudson.util.StackedAreaRenderer2;
import java.awt.Color;
import java.awt.Paint;
import hudson.model.Action;
import hudson.tasks.junit.Helper;
import hudson.tasks.test.AbstractTestResultAction;
import java.util.List;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.StackedAreaRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.ui.RectangleInsets;
import org.kohsuke.stapler.Stapler;


/**
 *
 * 
 */
public  class QuestaHistory extends hudson.tasks.junit.History {

    private final String actionUrl;

    public QuestaHistory(TestObject testObject, String actionUrl) {
        super(testObject);
        this.actionUrl = actionUrl;
    }

    public QuestaHistory(TestObject testObject) {
        super(testObject);
        this.actionUrl= "";
    }


    public String getActionUrl() {
        return actionUrl;
    }
    
    public String getUrl() {
        Jenkins  instance =Jenkins.getInstance();
        if (instance!=null){
            return  instance.getRootUrlFromRequest()+getTestObject().getRun().getUrl() + "/" + getUrlFromRun();
        } else {
            return getTestObject().getRun().getUrl() + "/" + getUrlFromRun();
        }
    }

    public String getUrlFromRun() {
        return getTestObject().getTestResultAction().getUrlName() + "/" + getTestObject().getRelativePathFrom(getTestObject().getTopLevelTestResult()) + "/" + actionUrl;
    }

   
    
    public int getStart() {
        try {
            return Integer.parseInt(Stapler.getCurrentRequest().getParameter("start"));
        } catch (NumberFormatException e) {
            int index = getTestObject().getRun().getParent().getBuilds().indexOf(getTestObject().getRun());
            // place the current build in the middle of the history window... 
            return Math.max(0, index - 12);
        }
    }

    public int getEnd() {

        try {
            return Integer.parseInt(Stapler.getCurrentRequest().getParameter("end"));
        } catch (NumberFormatException e) {
            return getStart() + 25;
        }
    }
    
    public boolean hasNewer() {
        return getStart() != 0;
    }


    public boolean hasOlder() {
        return getEnd() < getTestObject().getRun().getParent().getBuilds().size();
    }
    
    @Override
    public List<TestResult> getList() {
        List<TestResult> list;
        try {

            list =  getList(
                    getStart(),
                    getEnd());
        } catch (NumberFormatException e) {
            list = super.getList();
        }

        return list;
    }
    
    public Area calcDefaultSize() {
        // This is the default history dimension on the testResult history page 
        return new Area(600, 300);
    }

    protected abstract class GraphImpl extends Graph {

        protected final String yLabel;

        protected GraphImpl(String yLabel, int width, int height) {
            super(-1, width, height); // cannot use timestamp, since ranges may change
            this.yLabel = yLabel;

        }

        protected abstract DataSetBuilder<String, ChartLabel> createDataSet();

        @Override
        protected JFreeChart createGraph() {
            final CategoryDataset dataset = createDataSet().build();
            final JFreeChart chart = ChartFactory.createStackedAreaChart(null, // chart
                    // title
                    null, // unused
                    yLabel, // range axis label
                    dataset, // data
                    PlotOrientation.VERTICAL, // orientation
                    false, // include legend
                    true, // tooltips
                    false // urls
            );

            chart.setBackgroundPaint(Color.white);

            final CategoryPlot plot = chart.getCategoryPlot();
            plot.setBackgroundPaint(Color.WHITE);
            plot.setOutlinePaint(null);
            plot.setForegroundAlpha(0.8f);

            plot.setRangeGridlinesVisible(true);
            plot.setRangeGridlinePaint(Color.black);

            CategoryAxis domainAxis = new ShiftedCategoryAxis(null);
            plot.setDomainAxis(domainAxis);
            domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_90);
            domainAxis.setLowerMargin(0.0);
            domainAxis.setUpperMargin(0.0);
            domainAxis.setCategoryMargin(0.0);

            final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
            ChartUtil.adjustChebyshev(dataset, rangeAxis);
            rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
            rangeAxis.setAutoRange(true);

            StackedAreaRenderer ar = new StackedAreaRenderer2() {
                @Override
                public Paint getItemPaint(int row, int column) {
                    ChartLabel key = (ChartLabel) dataset.getColumnKey(column);
                    if (key.getColor() != null) {
                        return key.getColor();
                    }
                    return super.getItemPaint(row, column);
                }

                @Override
                public String generateURL(CategoryDataset dataset, int row,
                        int column) {
                    ChartLabel label = (ChartLabel) dataset.getColumnKey(column);
                    return label.getUrl();
                }

                @Override
                public String generateToolTip(CategoryDataset dataset, int row,
                        int column) {
                    ChartLabel label = (ChartLabel) dataset.getColumnKey(column);
                    return label.o.getRun().getDisplayName() + " : "
                            + dataset.getValue(row, column);
                }
            };
            plot.setRenderer(ar);
            ar.setSeriesPaint(0, ColorPalette.YELLOW); // Skips.
            ar.setSeriesPaint(1, ColorPalette.RED); // Failures.
            ar.setSeriesPaint(2, ColorPalette.BLUE); // Total.

            // crop extra space around the graph
            plot.setInsets(new RectangleInsets(0, 0, 0, 5.0));

            return chart;
        }
    }

    protected class ChartLabel implements Comparable<ChartLabel> {

        TestResult o;

        String url;

        public ChartLabel(TestResult o) {
            this.o = o;
            this.url = null;

        }

        public TestResult getO() {
            return o;
        }
        private Class getTestResultClass(){
            try{
                return Class.forName("com.mentor.questa.vrm.jenkins.QuestaVrmRegressionBuildAction");
            }catch (ClassNotFoundException e){
                return AbstractTestResultAction.class;
            }
            
        }
        public String getActionUrl() {
            String actionUrl = o.getTestResultAction().getUrlName(); 
            Action questaAction = o.getRun().getAction(getTestResultClass());
            if (questaAction!=null) {
                actionUrl = questaAction.getUrlName();
            }
            return actionUrl;
        }
        public String getUrl() {
            if (this.url == null) {
                generateUrl();
            }
            return url;
        }

        private void generateUrl() {
            Run<?, ?> build = o.getRun();
            String buildLink = build.getUrl();
            this.url= "";
            Jenkins inst = Helper.getActiveInstance();
            if (inst!=null ){
                this.url = inst.getRootUrlFromRequest() ;
            }
            this.url+= buildLink + getActionUrl() + o.getUrl();
        }
        
        @Override
        public int compareTo(ChartLabel that) {
            return this.o.getRun().number - that.o.getRun().number;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ChartLabel)) {
                return false;
            }
            ChartLabel that = (ChartLabel) o;
            return this.o == that.o;
        }

        public Color getColor() {
          return null;
        }

        @Override
        public int hashCode() {
            return o.hashCode();
        }

        @Override
        public String toString() {
            Run<?, ?> run = o.getRun();
            String l = run.getDisplayName();
            String s = run instanceof AbstractBuild ? ((AbstractBuild) run).getBuiltOnStr() : null;
            if (s != null) {
                l += ' ' + s;
            }
            return l;
        }

    }

}
