package com.mentor.questa.jenkins;

import java.awt.Color;
import java.awt.Paint;
import java.io.IOException;
import java.text.SimpleDateFormat;

import javax.servlet.ServletException;

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
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import hudson.model.Build;
import hudson.model.Job;
import hudson.model.Messages;
import hudson.model.Result;
import hudson.model.Run;
import hudson.util.ChartUtil;
import hudson.util.ColorPalette;
import hudson.util.DataSetBuilder;
import hudson.util.RunList;
import hudson.util.ShiftedCategoryAxis;
import hudson.util.StackedAreaRenderer2;

/*
 * This class overrides the implementation of the build time graph
 * The purpose is to allow displaying build dates on the chart X-axis
 */
public class BuildTimeGraph {
	Job job;
	
	public BuildTimeGraph(Job job) {
		this.job = job;
	}
    
    public void doBuildTimeGraph(final StaplerRequest req, StaplerResponse rsp) 
    		throws IOException, ServletException {

    	GraphsByBuildDates drawGraphByBuildDates = Util.getGraphXAxisDisplay(job);
    	ChartUtil.generateGraph(req, rsp, getBuildTimeChart(job, drawGraphByBuildDates), Util.getProjectGraphArea());
    }
    
    private JFreeChart getBuildTimeChart(Job currentJob, GraphsByBuildDates buildDatesFlag) {
		final GraphsByBuildDates drawGraphByBuildDates = buildDatesFlag;
		final SimpleDateFormat dateMonthFirstFormat = new SimpleDateFormat("MM/dd");
		final SimpleDateFormat dateDayFirstFormat = new SimpleDateFormat("dd/MM");
		class ChartLabel implements Comparable<ChartLabel> {
			final Run run;

			public ChartLabel(Run r) {
				this.run = r;
			}

			public int compareTo(ChartLabel that) {
				return this.run.number - that.run.number;
			}

			@Override
			public boolean equals(Object o) {
				// HUDSON-2682 workaround for Eclipse compilation bug
				// on (c instanceof ChartLabel)
				if (o == null || !ChartLabel.class.isAssignableFrom(o.getClass())) {
					return false;
				}
				ChartLabel that = (ChartLabel) o;
				return run == that.run;
			}

			public Color getColor() {
				// TODO: consider gradation. See
				// http://www.javadrive.jp/java2d/shape/index9.html
				Result r = run.getResult();
				if (r == Result.FAILURE)
					return ColorPalette.RED;
				else if (r == Result.UNSTABLE)
					return ColorPalette.YELLOW;
				else if (r == Result.ABORTED || r == Result.NOT_BUILT)
					return ColorPalette.GREY;
				else
					return ColorPalette.BLUE;
			}

			@Override
			public int hashCode() {
				return run.hashCode();
			}

			@Override
			public String toString() {
				String l = null;
				if (drawGraphByBuildDates == null) {
					l = run.getDisplayName();
					if (run instanceof Build) {
						String s = ((Build) run).getBuiltOnStr();
						if (s != null)
							l += ' ' + s;
					}
				} else {
					if (drawGraphByBuildDates.getDateMonthFormat() == true)
		    			l = dateMonthFirstFormat.format(((Build) run).getTime());
		    		else 
		    			l = dateDayFirstFormat.format(((Build) run).getTime());
				}
				return l;
			}

		}

		DataSetBuilder<String, ChartLabel> data = new DataSetBuilder<String, ChartLabel>();
		RunList<Run> runlist = currentJob.getNewBuilds();
		for (Run r : runlist) {
			if (r.isBuilding())
				continue;
			data.add(((double) r.getDuration()) / (1000 * 60), "min", new ChartLabel(r));
		}

		final CategoryDataset dataset = data.build();

		final JFreeChart chart = ChartFactory.createStackedAreaChart(null, // chart
																			// title
				null, // unused
				Messages.Job_minutes(), // range axis label
				dataset, // data
				PlotOrientation.VERTICAL, // orientation
				false, // include legend
				true, // tooltips
				false // urls
		);

		chart.setBackgroundPaint(Color.white);

		final CategoryPlot plot = chart.getCategoryPlot();

		// plot.setAxisOffset(new Spacer(Spacer.ABSOLUTE, 5.0, 5.0, 5.0, 5.0));
		plot.setBackgroundPaint(Color.WHITE);
		plot.setOutlinePaint(null);
		plot.setForegroundAlpha(0.8f);
		// plot.setDomainGridlinesVisible(true);
		// plot.setDomainGridlinePaint(Color.white);
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

		StackedAreaRenderer ar = new StackedAreaRenderer2() {
			@Override
			public Paint getItemPaint(int row, int column) {
				ChartLabel key = (ChartLabel) dataset.getColumnKey(column);
				return key.getColor();
			}

			@Override
			public String generateURL(CategoryDataset dataset, int row, int column) {
				ChartLabel label = (ChartLabel) dataset.getColumnKey(column);
				return String.valueOf(label.run.number);
			}

			@Override
			public String generateToolTip(CategoryDataset dataset, int row, int column) {
				ChartLabel label = (ChartLabel) dataset.getColumnKey(column);
				return label.run.getDisplayName() + " : " + label.run.getDurationString();
			}
		};
		plot.setRenderer(ar);

		// crop extra space around the graph
		plot.setInsets(new RectangleInsets(0, 0, 0, 5.0));

		return chart;

	}

}
