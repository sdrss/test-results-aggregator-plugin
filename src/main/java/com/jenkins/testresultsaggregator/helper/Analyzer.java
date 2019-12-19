package com.jenkins.testresultsaggregator.helper;

import java.io.PrintStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

import com.google.common.base.Strings;
import com.jenkins.testresultsaggregator.TestResultsAggregator;
import com.jenkins.testresultsaggregator.data.Aggregated;
import com.jenkins.testresultsaggregator.data.Data;
import com.jenkins.testresultsaggregator.data.Job;
import com.jenkins.testresultsaggregator.data.JobStatus;
import com.jenkins.testresultsaggregator.data.ReportGroup;
import com.jenkins.testresultsaggregator.data.ReportJob;
import com.jenkins.testresultsaggregator.data.Results;

public class Analyzer {
	
	private PrintStream logger;
	
	public Analyzer(PrintStream logger) {
		this.logger = logger;
	}
	
	public Aggregated analyze(List<Data> listData, Properties properties) throws Exception {
		// Resolve
		String outOfDateResults = properties.getProperty(TestResultsAggregator.AggregatorProperties.OUT_OF_DATE_RESULTS_ARG.name());
		// Check if Groups/Names are used
		boolean foundAtLeastOneGroupName = false;
		for (Data data : listData) {
			if (!Strings.isNullOrEmpty(data.getGroupName())) {
				foundAtLeastOneGroupName = true;
			}
		}
		// Order List per Group Name
		if (foundAtLeastOneGroupName) {
			Collections.sort(listData, new Comparator<Data>() {
				@Override
				public int compare(Data dataDTO1, Data dataDTO2) {
					return dataDTO1.getGroupName().compareTo(dataDTO2.getGroupName());
				}
			});
		}
		Aggregated aggregated = new Aggregated();
		// Calculate Aggregated Results for Reporting
		Results totalResults = new Results();
		for (Data data : listData) {
			boolean foundFailure = false;
			boolean foundRunning = false;
			boolean foundSkip = false;
			Results resultsPerGroup = new Results();
			int jobFailed = 0;
			int jobSkipped = 0;
			int jobUnstable = 0;
			int jobAborted = 0;
			int jobSuccess = 0;
			int jobRunning = 0;
			data.setReportGroup(new ReportGroup());
			for (Job job : data.getJobs()) {
				job.setReport(new ReportJob());
				if (job.getResults() != null) {
					// Calculate Job Status
					job.getReport().calculateStatus(job.getResults());
					// Calculate Total
					job.getReport().calculateTotal(job.getResults());
					// Calculate Pass
					job.getReport().calculatePass(job.getResults());
					// Calculate Fail
					job.getReport().calculateFailedColor(job.getResults());
					// Calculate Skipped
					job.getReport().calculateSkipped(job.getResults());
					// Calculate timestamp
					job.getReport().calculateTimestamp(job.getResults(), outOfDateResults);
					// Calculate Changes
					job.getReport().calculateChanges(job.getResults());
					// Calculate Report
					job.getReport().calculateReport(job.getResults());
					// Calculate Sonar Url
					job.getReport().calculateSonar(job.getResults());
					// Calculate Coverage Packages
					job.getReport().calculateCCPackages(job.getResults());
					job.getReport().calculateCCFiles(job.getResults());
					job.getReport().calculateCCClasses(job.getResults());
					job.getReport().calculateCCMethods(job.getResults());
					job.getReport().calculateCCLines(job.getResults());
					job.getReport().calculateCCConditions(job.getResults());
					
					// Calculate Duration
					if (job.getBuildInfo() != null) {
						job.getReport().calculateDuration(job.getBuildInfo().getDuration());
						// Total Duration
						aggregated.setTotalDuration(aggregated.getTotalDuration() + job.getBuildInfo().getDuration());
						// Total Changes
						aggregated.setTotalNumberOfChanges(aggregated.getTotalNumberOfChanges() + job.getResults().getNumberOfChanges());
						// Calculate Description
						job.getReport().calculateDescription(job.getBuildInfo().getDescription());
					}
					// Calculate Percentage
					job.getReport().calculatePercentage(job.getResults());
					// Calculate Group
					if (JobStatus.SUCCESS.name().equals(job.getReport().getStatus())) {
						data.getReportGroup().setJobSuccess(data.getReportGroup().getJobSuccess() + 1);
						aggregated.setSuccessJobs(aggregated.getSuccessJobs() + 1);
						jobSuccess++;
					} else if (JobStatus.FIXED.name().equals(job.getReport().getStatus())) {
						data.getReportGroup().setJobSuccess(data.getReportGroup().getJobSuccess() + 1);
						aggregated.setFixedJobs(aggregated.getFixedJobs() + 1);
						jobSuccess++;
					} else if (JobStatus.RUNNING.name().equals(job.getReport().getStatus())) {
						foundRunning = true;
						data.getReportGroup().setJobRunning(data.getReportGroup().getJobRunning() + 1);
						aggregated.setRunningJobs(aggregated.getRunningJobs() + 1);
						jobRunning++;
					} else if (JobStatus.FAILURE.name().equals(job.getReport().getStatus())) {
						foundFailure = true;
						data.getReportGroup().setJobFailed(data.getReportGroup().getJobFailed() + 1);
						aggregated.setFailedJobs(aggregated.getFailedJobs() + 1);
						jobFailed++;
						// Do not Check total Test
						job.getReport().calculateTotal(null);
						job.getReport().calculatePass(null);
						job.getReport().calculateFailed(null);
						job.getReport().calculateFailedColor(null);
						job.getReport().calculateSkipped(null);
					} else if (JobStatus.STILL_FAILING.name().equals(job.getReport().getStatus())) {
						foundFailure = true;
						data.getReportGroup().setJobFailed(data.getReportGroup().getJobFailed() + 1);
						aggregated.setKeepFailJobs(aggregated.getKeepFailJobs() + 1);
						jobFailed++;
						// Do not Check total Test
						job.getReport().calculateTotal(null);
						job.getReport().calculatePass(null);
						job.getReport().calculateFailed(null);
						job.getReport().calculateFailedColor(null);
						job.getReport().calculateSkipped(null);
					} else if (JobStatus.UNSTABLE.name().equals(job.getReport().getStatus())) {
						foundSkip = true;
						data.getReportGroup().setJobUnstable(data.getReportGroup().getJobUnstable() + 1);
						aggregated.setUnstableJobs(aggregated.getUnstableJobs() + 1);
						jobUnstable++;
					} else if (JobStatus.STILL_UNSTABLE.name().equals(job.getReport().getStatus())) {
						foundSkip = true;
						data.getReportGroup().setJobUnstable(data.getReportGroup().getJobUnstable() + 1);
						aggregated.setKeepUnstableJobs(aggregated.getKeepUnstableJobs() + 1);
						jobUnstable++;
					} else if (JobStatus.ABORTED.name().equals(job.getReport().getStatus())) {
						foundSkip = true;
						data.getReportGroup().setJobAborted(data.getReportGroup().getJobAborted() + 1);
						aggregated.setAbortedJobs(aggregated.getAbortedJobs() + 1);
						jobAborted++;
					}
					// Calculate Total Per Group
					resultsPerGroup.setPass(resultsPerGroup.getPass() + job.getResults().getPass());
					resultsPerGroup.setSkip(resultsPerGroup.getSkip() + job.getResults().getSkip());
					resultsPerGroup.setTotal(resultsPerGroup.getTotal() + job.getResults().getTotal());
					// Calculate Total for Summary Column
					totalResults.addResults(job.getResults());
				} else {
					logger.println("Not found results for " + job.getJobName());
				}
			}
			// Set Results Per Group
			data.getReportGroup().setResults(resultsPerGroup);
			// Calculate Group Status
			if (foundFailure) {
				data.getReportGroup().setStatus(JobStatus.FAILURE.name());
			} else if (foundRunning) {
				data.getReportGroup().setStatus(JobStatus.RUNNING.name());
			} else if (foundSkip) {
				data.getReportGroup().setStatus(JobStatus.UNSTABLE.name());
			} else {
				data.getReportGroup().setStatus(JobStatus.SUCCESS.name());
			}
			// Calculate Percentage Per Group based on Jobs
			data.getReportGroup().setPercentage(Helper.countPercentageD(jobSuccess, jobSuccess + jobRunning + jobAborted + jobUnstable + jobSkipped + jobFailed).toString());
		}
		// Order Jobs per
		final String orderBy = (String) properties.get(TestResultsAggregator.AggregatorProperties.SORT_JOBS_BY.name());
		for (Data data : listData) {
			Collections.sort(data.getJobs(), new Comparator<Job>() {
				@Override
				public int compare(Job dataJobDTO1, Job dataJobDTO2) {
					try {
						if (TestResultsAggregator.SortResultsBy.NAME.name().equalsIgnoreCase(orderBy)) {
							return dataJobDTO1.getJobNameFromFriendlyName().compareTo(dataJobDTO2.getJobNameFromFriendlyName());
						} else if (TestResultsAggregator.SortResultsBy.STATUS.name().equalsIgnoreCase(orderBy)) {
							return dataJobDTO1.getReport().getStatusFromEnum().getPriority() - dataJobDTO2.getReport().getStatusFromEnum().getPriority();
						} else if (TestResultsAggregator.SortResultsBy.TOTAL_TEST.name().equalsIgnoreCase(orderBy)) {
							return dataJobDTO2.getResults().getTotal() - dataJobDTO1.getResults().getTotal();
						} else if (TestResultsAggregator.SortResultsBy.PASS.name().equalsIgnoreCase(orderBy)) {
							return dataJobDTO2.getResults().getPass() - dataJobDTO1.getResults().getPass();
						} else if (TestResultsAggregator.SortResultsBy.FAIL.name().equalsIgnoreCase(orderBy)) {
							return dataJobDTO2.getResults().getFail() - dataJobDTO1.getResults().getFail();
						} else if (TestResultsAggregator.SortResultsBy.SKIP.name().equalsIgnoreCase(orderBy)) {
							return dataJobDTO2.getResults().getSkip() - dataJobDTO1.getResults().getSkip();
						} else if (TestResultsAggregator.SortResultsBy.LAST_RUN.name().equalsIgnoreCase(orderBy)) {
							return dataJobDTO1.getResults().getTimestamp().compareTo(dataJobDTO2.getResults().getTimestamp());
						} else if (TestResultsAggregator.SortResultsBy.COMMITS.name().equalsIgnoreCase(orderBy)) {
							return dataJobDTO1.getResults().getNumberOfChanges() - dataJobDTO2.getResults().getNumberOfChanges();
						} else if (TestResultsAggregator.SortResultsBy.DURATION.name().equalsIgnoreCase(orderBy)) {
							return dataJobDTO1.getResults().getDuration().compareTo(dataJobDTO2.getResults().getDuration());
						} else if (TestResultsAggregator.SortResultsBy.PERCENTAGE.name().equalsIgnoreCase(orderBy)) {
							return dataJobDTO1.getResults().getPercentage().compareTo(dataJobDTO2.getResults().getPercentage());
						} else {
							// Default
							return dataJobDTO1.getJobNameFromFriendlyName().compareTo(dataJobDTO2.getJobNameFromFriendlyName());
						}
					} catch (NullPointerException ex) {
						return -1;
					}
				}
			});
		}
		// Set
		aggregated.setData(listData);
		aggregated.setResults(totalResults);
		logger.println(LocalMessages.ANALYZE.toString() + " " + LocalMessages.FINISHED.toString());
		return aggregated;
	}
}
