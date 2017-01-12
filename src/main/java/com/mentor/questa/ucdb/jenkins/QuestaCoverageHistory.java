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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Functions;

import hudson.tasks.test.TestObject;

import hudson.tasks.test.TestResult;
import hudson.util.Area;

import hudson.util.ColorPalette;
import hudson.util.DataSetBuilder;

import hudson.util.Graph;
import hudson.util.ShiftedCategoryAxis;
import hudson.util.StackedAreaRenderer2;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletException;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.category.StackedAreaRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.category.CategoryDataset;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 *
 * 
 */
public class QuestaCoverageHistory extends QuestaHistory {

    private final QuestaCoverageResult coverageResult;
  

    private Map<String, QuestaAttributeGraphTab> attributePublisherMap;

    public QuestaCoverageHistory(TestObject testObject, QuestaCoverageResult coverageResult,  String actionUrl) {
        super(testObject, actionUrl);
        this.coverageResult = coverageResult;
       

    }

    protected void redirectSubmitAttributes(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        rsp.sendRedirect(Functions.isAutoRefresh(req) ? "../.." : "..");
    }

    public void doSubmitAttributes(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        QuestaCoverageProjectAction action = getTestObject().getRun().getParent().getAction(QuestaCoverageProjectAction.class);
        if (action == null) {
            return;
        }
        QuestaAttributesSettingsMap graphMap = action.getAttributesSetting();
        getGraphSetting(action).updateGraphs(coverageResult.isTest(), req.getSubmittedForm());
        action.setAttributesSetting(graphMap);
        redirectSubmitAttributes(req, rsp);
    }
    private QuestaAttributesGraphSetting getGraphSetting(QuestaAttributesSettingsMap graphMap){
        return graphMap.getGraphSetting(coverageResult.getUcdbResult().getCoverageId());
    }
    private QuestaAttributesGraphSetting getGraphSetting(QuestaCoverageProjectAction action){
        return getGraphSetting(action.getAttributesSetting());
    }
    public QuestaCoverageResult getCoverageResult() {
        return coverageResult;
    }

    public boolean isNonZeroCoverage(){
          Area area = calcDefaultSize();
          CoverageGraphImpl impl = new CoverageGraphImpl("Coverage %", area.width, area.height, null);
          impl.createDataSet();
          return  impl.isNonZeroCoverage();
    }
    public Set<String> getAvailableAttributes() {
        QuestaCoverageProjectAction action = getTestObject().getRun().getParent().getAction(QuestaCoverageProjectAction.class);
        if (action == null) {
            return Collections.<String>emptySet();
        }
        return getGraphSetting(action).getAvailableAttributes(coverageResult.isTest());

    }

   
    public Set<String> getTrendableAttributes() {
        HashSet<String> attrSet = new HashSet<>();
        for (QuestaAttributeGraphTab pub : getAttributesPublishers()) {
            attrSet.addAll(pub.getAttributes());
        }
        return attrSet;
    }

    public Object getProxy() {        
        return getTestObject();
    }

    public ChartLabel getLabel(hudson.tasks.test.TestResult o, final List<String> metrics) {
        return new ChartLabel(o){
                          @Override
                    public Color getColor() {
                        if (metrics==null || metrics.size()!=1){
                            return super.getColor();
                        }
                         if (getO().getFailCount() > 0) {
                            return ColorPalette.RED;
                        } else if (getO().getSkipCount() > 0) {
                            return ColorPalette.YELLOW;
                        } else {
                            return ColorPalette.BLUE;
                        }
                    }
                    
        };
    }

    public List< QuestaAttributeGraphTab> getAttributesPublishers() {
        QuestaCoverageProjectAction action = getTestObject().getRun().getParent().getAction(QuestaCoverageProjectAction.class);
        if (action == null) {
            return Collections.<QuestaAttributeGraphTab>emptyList();
        }
        return getGraphSetting(action).getGraphs(coverageResult.isTest());
    }

    private QuestaAttributeGraphTab getAttributePublisherMap(String metric) {
        if (attributePublisherMap == null || !attributePublisherMap.containsKey(metric)) {
            attributePublisherMap = new HashMap<>();
            for (QuestaAttributeGraphTab pub : getAttributesPublishers()) {
                attributePublisherMap.put(pub.getSafeName(), pub);
            }
        }

        return attributePublisherMap.get(metric);
    }

    public QuestaCoverageResult getCoverageResultFromTest(TestResult testResult) {
        if (testResult != null) {
            return CoverageUtil.getCoverageResult(testResult, coverageResult.getCoverageId());
        }
        return null;
    }

    public Graph getAttributeGraph(String metric) {
        Area area = calcDefaultSize();
        List<String> metrics = getAttributePublisherMap(metric).getAttributes();
        if (metrics.size() != 1) {
            return new CoverageGraphImpl(getAttributePublisherMap(metric).getyLabel(), area.width, area.height, metrics);
        } else {
            return new CoverageGraphMapImpl(getAttributePublisherMap(metric).getyLabel(), area.width, area.height, metrics);
        }
    }

    public Graph getAttributeGraphMap(String metric) {
        Area area = calcDefaultSize();
        return new CoverageGraphMapImpl(getAttributePublisherMap(metric).getyLabel(), area.width, area.height, getAttributePublisherMap(metric).getAttributes());
    }

    public void doTrendCoverageMap(final StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        Area area = calcDefaultSize();
        new CoverageGraphMapImpl("Coverage %", area.width, area.height, null).doMap(req, rsp);
    }

    public void doTrendCoverage(final StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        Area area = calcDefaultSize();
        new CoverageGraphImpl("Coverage %", area.width, area.height, null).doPng(req, rsp);

    }

    abstract class AbstractCoverageGraphImpl extends GraphImpl {

        final List<String> metrics;
        private boolean nonZeroCoverage = false;
        public AbstractCoverageGraphImpl(String yLabel, int width, int height, List<String> metrics) {
            super(yLabel, width, height);
            this.metrics = metrics;

        }

        private void addCoverage(Set<String> coverageKeys, DataSetBuilder<String, ChartLabel> data, ChartLabel label, QuestaCoverageResult currentCov) {

            for (Map.Entry<String, Double> coverageEntry : currentCov.getCoverageValues().entrySet()) {
                nonZeroCoverage|= coverageEntry.getValue()>0;
                data.add(coverageEntry.getValue(), coverageEntry.getKey(), label);
            }
            coverageKeys.removeAll(currentCov.getCoverageValues().keySet());
            for (String key : coverageKeys) {
                data.add(0, key, label);
            }
            coverageKeys.clear();
            coverageKeys.addAll(currentCov.getCoverageValues().keySet());

            data.add(currentCov.getTestplanCov(), "Testplan Coverage", label);
            data.add(currentCov.getTotalCoverage(), "Total Coverage", label);
        }

        private void addAttributes(DataSetBuilder<String, ChartLabel> data, ChartLabel label, QuestaCoverageResult currentCov) {

            for (String metric : metrics) {
                data.add(currentCov.getAttributeDoubleValue(metric), metric, label);
            }
          
        }

        @Override
        protected DataSetBuilder<String, ChartLabel> createDataSet() {
            DataSetBuilder<String, ChartLabel> data = new DataSetBuilder<>();

            List<TestResult> list = getList();
            Set<String> coverageKeys = new HashSet<>();

            for (hudson.tasks.test.TestResult o : list) {
                QuestaCoverageResult currentCov = CoverageUtil.getCoverageResult(o, coverageResult.getCoverageId());
                if (currentCov == null || currentCov.isShadow()) {
                    continue;
                }
                ChartLabel label = getLabel(o, metrics);
                if (metrics == null) {
                    addCoverage(coverageKeys, data, label, currentCov);
                } else {
                    addAttributes(data, label, currentCov);
                }

            }
            return data;
        }

        public boolean isNonZeroCoverage() {
            return nonZeroCoverage;
        }

    }
    @SuppressFBWarnings(value="SE_BAD_FIELD", justification="not applicable")
    class CoverageGraphMapImpl extends AbstractCoverageGraphImpl{

        public CoverageGraphMapImpl(String yLabel, int width, int height, List<String> metrics) {
            super(yLabel, width, height, metrics);
        }

        @Override
        protected JFreeChart createGraph() {
            boolean includeLegend = metrics == null || metrics.size() > 1;
            final CategoryDataset dataset = createDataSet().build();
            final JFreeChart chart = ChartFactory.createStackedAreaChart(
                    null, // chart title
                    null, // unused
                    yLabel, // range axis label
                    dataset, // data
                    PlotOrientation.VERTICAL, // orientation
                    includeLegend, // include legend
                    true, // tooltips
                    false // urls
            );

            if (includeLegend) {
                final LegendTitle legend = chart.getLegend();
                legend.setPosition(RectangleEdge.BOTTOM);
            }
            chart.setBackgroundPaint(Color.white);

            final CategoryPlot plot = chart.getCategoryPlot();
            plot.setBackgroundPaint(Color.WHITE);
            plot.setOutlinePaint(null);
            plot.setForegroundAlpha(0.8f);
            plot.setDomainGridlinesVisible(true);
            plot.setDomainGridlinePaint(Color.white);
            plot.setRangeGridlinesVisible(true);
            plot.setRangeGridlinePaint(Color.black);

            CategoryAxis domainAxis = new ShiftedCategoryAxis(null);
            plot.setDomainAxis(domainAxis);
            domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_90);
            domainAxis.setLowerMargin(0.0);
            domainAxis.setUpperMargin(0.0);
            domainAxis.setCategoryMargin(0.0);

            final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
            rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

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
                public String generateURL(CategoryDataset dataset, int row, int column) {
                    ChartLabel label = (ChartLabel) dataset.getColumnKey(column);
                    return label.getUrl();
                }

                @Override
                public String generateToolTip(CategoryDataset dataset, int row, int column) {
                    ChartLabel label = (ChartLabel) dataset.getColumnKey(column);
                    QuestaCoverageResult currentCoverageResult = CoverageUtil.getCoverageResult(label.getO(), coverageResult.getCoverageId());
                    if (currentCoverageResult != null) {
                        return label.getO().getRun().getDisplayName() + ": " + currentCoverageResult.toolTipString(metrics);
                    }

                    return coverageResult.getCoverageId();

                }

            };

            plot.setRenderer(ar);

            // crop extra space around the graph
            plot.setInsets(new RectangleInsets(0, 0, 0, 5.0));

            return chart;
        }
    }

    class CoverageGraphImpl extends AbstractCoverageGraphImpl {
        
        public CoverageGraphImpl(String yLabel, int width, int height, List<String> metrics) {
            super(yLabel, width, height, metrics);
        }

        @Override
        protected JFreeChart createGraph() {
            CategoryDataset dataset = createDataSet().build();
            boolean includeLegend = metrics == null || metrics.size() > 1;
            final JFreeChart chart = ChartFactory.createLineChart(
                    null, // chart title
                    null, // unused
                    yLabel, // range axis label
                    dataset, // data
                    PlotOrientation.VERTICAL, // orientation
                    includeLegend, // include legend
                    true, // tooltips
                    false // urls
            );

            // NOW DO SOME OPTIONAL CUSTOMISATION OF THE CHART...
            chart.setBackgroundPaint(Color.white);
            if (includeLegend) {
                final LegendTitle legend = chart.getLegend();
                legend.setPosition(RectangleEdge.BOTTOM);
            }
            final CategoryPlot plot = chart.getCategoryPlot();

            plot.setBackgroundPaint(Color.WHITE);
            plot.setOutlinePaint(null);
            plot.setForegroundAlpha(0.8f);
            plot.setDomainGridlinesVisible(true);
            plot.setDomainGridlinePaint(Color.white);
            plot.setRangeGridlinesVisible(true);
            plot.setRangeGridlinePaint(Color.black);

            CategoryAxis domainAxis = new ShiftedCategoryAxis(null);
            plot.setDomainAxis(domainAxis);
            domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_90);
            domainAxis.setLowerMargin(0.0);
            domainAxis.setUpperMargin(0.0);
            domainAxis.setCategoryMargin(0.0);

            final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
            rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

            LineAndShapeRenderer ar = new LineAndShapeRenderer();
            BasicStroke stroke = new BasicStroke(2f);
            for (int i = 0; i < dataset.getRowCount(); i++) {
                ar.setSeriesShapesVisible(i, Boolean.FALSE);
                ar.setSeriesStroke(i, stroke);
            }

            plot.setRenderer(ar);
            // crop extra space around the graph
            plot.setInsets(new RectangleInsets(0, 0, 0, 5.0));

            return chart;
        }
    }
}
