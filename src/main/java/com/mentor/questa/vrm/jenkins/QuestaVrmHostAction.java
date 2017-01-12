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

import com.mentor.questa.ucdb.jenkins.QuestaUCDBResult;
import hudson.Functions;
import hudson.model.Api;
import hudson.model.Run;
import hudson.tasks.test.TestResult;
import hudson.util.Area;
import hudson.util.ChartUtil;
import hudson.util.DataSetBuilder;
import hudson.util.ShiftedCategoryAxis;
import hudson.util.StackedAreaRenderer2;
import java.awt.Color;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import jenkins.model.RunAction2;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
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
import org.kohsuke.stapler.Ancestor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.bind.JavaScriptMethod;

/**
 *
 * 
 */
public class QuestaVrmHostAction implements RunAction2 {

    public transient Run<?, ?> run;

    private transient WeakReference<QuestaVrmRegressionResult> questaVrmResultRef;

    public QuestaVrmHostAction(Run run) {
        this.run = run;
    }

    public Api getApi() {
        return new Api(this);
    }

    @Override
    public String getIconFileName() {
        return "/plugin/mentor-questa-vrm/icons/bars.png";
    }

    @Override
    public String getDisplayName() {
        return "Questa Host Utilization";
    }

    @Override
    public String getUrlName() {
        return "questavrmhostinfo";
    }
    
    @Override
    public void onAttached(Run<?, ?> run) {
        this.run = run;
    }

    @Override
    public void onLoad(Run<?, ?> run) {
        this.run = run;
    }

    public Object getDynamic(String token, StaplerRequest req, StaplerResponse rsp) {
        return getRegressionResult().getDynamic(token, req, rsp);
    }
    
    public synchronized QuestaVrmRegressionResult getRegressionResult() {
        QuestaVrmRegressionResult r = new QuestaVrmRegressionResult("");
        if (run == null) {
            return r;
        }
        if (questaVrmResultRef == null) {
            QuestaVrmRegressionBuildAction action = run.getAction(QuestaVrmRegressionBuildAction.class);
            if (action == null) {
                return r;
            }
            r = action.getResult();
            questaVrmResultRef = new WeakReference<QuestaVrmRegressionResult>(r);
        } else {
            r = questaVrmResultRef.get();
        }
        return r;

    }

    @JavaScriptMethod
    public JSONArray getActions(long time) {
        JSONArray result = new JSONArray();
        
        // Add time string as a first entry
        result.add(new Date(time).toString());
        
        for (TestResult temp : getRegressionResult().getActions()) {
            QuestaVrmAbstractResult action = (QuestaVrmAbstractResult) temp;
            
            // Add only actions that are running at that time.. 
            if (action.getStartTime() != -1 && action.getDoneTime() != -1 && time >= action.getStartTime() && time < action.getDoneTime()) {
            
                JSONObject obj = new JSONObject();
                obj.element("action", action.getAction());
                obj.element("url", action.getRelativeUrl());
                obj.element("status", action.getStatus());
                obj.element("age", (time - action.getStartTime()) / 1000);
                obj.element("duration", action.getDuration());
                obj.element("host", action.getHost());
                
                if (action instanceof QuestaVrmTestResult) {
                    obj.element("testname", action.getTestname());
                    obj.element("testurl", ((QuestaVrmTestResult) action).getTestUrl());
                }
              
                result.add(obj);
            }
        }
        return result;
    }

    private CategoryDataset buildDataSet(StaplerRequest req) {
        boolean showAction = Boolean.valueOf(req.getParameter("showActions")) || getActionCookie(req);
        DataSetBuilder<String, Long> dsb = new DataSetBuilder<String, Long>();
        
        PriorityQueue<Pair> pq = new PriorityQueue<Pair>();
        HashMap<String, Integer> hostCount = new HashMap<String, Integer>();
        for (TestResult temp : getRegressionResult().getActions()) {
            QuestaVrmAbstractResult action = (QuestaVrmAbstractResult) temp;
            if (showAction || action instanceof QuestaVrmTestResult) {
                if (action.getStartTime() == -1 || action.getDoneTime() == -1) {
                    continue;
                }
                pq.add(new Pair(action.getStartTime(), action.getHost(), 1));
                pq.add(new Pair(action.getDoneTime(), action.getHost(), -1));
                hostCount.put(action.getHost(), 0);
            }
        }
        
        if (pq.isEmpty()) {
            return dsb.build();
        }

        long offset = getRegressionResult().getRegressionBegin().getTime();
        int noOfTests;
        HashSet<String> visited = new HashSet<String>();
        
        while (!pq.isEmpty()) {
            long currentKey = pq.peek().date;

            while (!pq.isEmpty() && pq.peek().date == currentKey) {
                Pair current = pq.peek();
                noOfTests = hostCount.get(current.host);
                while (!pq.isEmpty() && pq.peek().compareTo(current) == 0) {
                    noOfTests += pq.poll().diff;
                }
                dsb.add(noOfTests, current.host, (current.date - offset) / 1000);
                hostCount.put(current.host, noOfTests);
                visited.add(current.host);

            }
            for (Map.Entry<String, Integer> host : hostCount.entrySet()) {
                if (!visited.contains(host.getKey())) {
                    dsb.add(host.getValue(), host.getKey(), (currentKey - offset) / 1000);
                }
            }
            visited.clear();

        }
        return dsb.build();

    }

    private JFreeChart createChart(StaplerRequest req, CategoryDataset dataset) {

        final JFreeChart chart = ChartFactory.createStackedAreaChart(
                null, // chart title
                "Relative time", // unused
                "count", // range axis label
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
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        StackedAreaRenderer ar = new StackedAreaRenderer2() {
            private long getTime(CategoryDataset dataset, int column) {
                Long offset = (Long) dataset.getColumnKey(column);
                return getRegressionResult().getRegressionBegin().getTime() + offset * 1000;

            }

            @Override
            public String generateURL(CategoryDataset dataset, int row, int column) {
                return "javascript:getSummary(" + getTime(dataset, column) + ");";
            }

            @Override
            public String generateToolTip(CategoryDataset dataset, int row, int column) {
                String host = (String) dataset.getRowKey(row);
                Date date = new Date(getTime(dataset, column));
                int value = (Integer) dataset.getValue(row, column);
                return value +" on "+host + "@" + date.toString();
            }
        };
        plot.setRenderer(ar);

        // crop extra space around the graph
        plot.setInsets(new RectangleInsets(0, 0, 0, 5.0));

        return chart;
    }

    private Area calcDefaultSize() {
        Area res = Functions.getScreenResolution();
        if (res != null && res.width <= 800) {
            return new Area(250, 100);
        } else {
            return new Area(500, 200);
        }
    }
    
    public void doTrend(StaplerRequest req, StaplerResponse rsp) throws IOException {
        if (ChartUtil.awtProblemCause != null) {
            // not available. send out error message
            rsp.sendRedirect2(req.getContextPath() + "/images/headless.png");
            return;
        }

        ChartUtil.generateGraph(req, rsp, createChart(req, buildDataSet(req)), calcDefaultSize());
    }

    
    public void doTrendMap(StaplerRequest req, StaplerResponse rsp) throws IOException {

        ChartUtil.generateClickableMap(req, rsp, createChart(req, buildDataSet(req)), calcDefaultSize());
    }

   
    private boolean getActionCookie(StaplerRequest req) {
        boolean showActions = false;

        // check the current preference value
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("HostAction_showActions")) {
                    showActions = Boolean.parseBoolean(cookie.getValue());
                }
            }
        }
        return showActions;
    }

    public void doFlipMode(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {

        // flip!
        boolean showActions = !getActionCookie(req);

        // set the updated value
        Cookie cookie = new Cookie("HostAction_showActions", String.valueOf(showActions));
        List anc = req.getAncestors();
        Ancestor a = (Ancestor) anc.get(anc.size() - 2);
        cookie.setPath(a.getUrl()); // just for this project
        cookie.setMaxAge(60 * 60 * 24 * 365); // 1 year

        rsp.addCookie(cookie);

        // back to the same page
        rsp.sendRedirect(".");
    }

   
    
   private static class Pair implements Comparable<Pair> {

        public long date;
        public String host;

        public int diff;

        public Pair(long date, String host, int diff) {
            this.date = date;
            this.host = host;
            this.diff = diff;
        }

        @Override
        public int compareTo(Pair o) {
            if (date < o.date) {
                return -1;
            }
            if (date > o.date) {
                return 1;
            }
            if(!host.equals(o.host)){
               return host.compareTo(o.host);
            }
            if(diff!=o.diff){
                return diff - o.diff;
            }
            return System.identityHashCode(this)- System.identityHashCode(o); 
        }

        @Override
        public boolean equals(Object obj) {
            return (obj==this);
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(this);
        }
        
        
       
        

    }

}
