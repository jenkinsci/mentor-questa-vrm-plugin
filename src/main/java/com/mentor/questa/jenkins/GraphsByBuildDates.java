package com.mentor.questa.jenkins;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import net.sf.json.JSONObject;

/*
 * This class adds a property to the project configuration page
 * to allow displaying charts with build dates on the X-axis
 */
public class GraphsByBuildDates extends JobProperty<AbstractProject<?, ?>> {

	private Boolean checked;
	private Boolean dateMonthFormat;

	@DataBoundConstructor
	public GraphsByBuildDates(Boolean checked) {
		this.checked = checked;
	}

	public Boolean getChecked() {
		return checked;
	}

	public void setDateMonthFormat(boolean dateMonthFormat) {
		this.dateMonthFormat = dateMonthFormat;
	}
	
	public Boolean getDateMonthFormat() {
		return dateMonthFormat;
	}
	
	@Extension
	public static class DescriptorImpl extends JobPropertyDescriptor {

		public DescriptorImpl() {
			super(GraphsByBuildDates.class);
			load();
		}

		@Override
		public String getDisplayName() {
			return "Display the trend charts by build dates (default format is dd/MM)";
		}

		@Override
		public boolean isApplicable(Class<? extends Job> jobType) {
			return true;
			// return AbstractProject.class.isAssignableFrom(jobType);
		}

		@Override
		public JobProperty<?> newInstance(StaplerRequest req, JSONObject formData) throws FormException {
			if (formData.optBoolean("checked")) {
				GraphsByBuildDates res = new GraphsByBuildDates(true);
				res.setDateMonthFormat(formData.getBoolean("dateFormatMonth"));
				return res;
			}
			return null;
		}
	}
}
