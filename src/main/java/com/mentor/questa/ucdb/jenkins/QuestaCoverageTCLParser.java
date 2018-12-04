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
import hudson.AbortException;
import java.io.File;

import java.io.FileNotFoundException;
import java.io.PrintStream;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.Proc;
import hudson.Util;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import java.io.ByteArrayOutputStream;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import jenkins.MasterToSlaveFileCallable;
/**
 * Parse Coverage XML Results
 *
 *
 */
public class QuestaCoverageTCLParser implements Serializable {

	public QuestaCoverageTCLParser() {

	}

	private String getRelativePath(FilePath workspace, String path) {

		String wsPath = workspace.getRemote();
		if (wsPath != null && path.startsWith(wsPath)) {
			return path.substring(wsPath.length() + 1);
		}
		return path;

	}

	public HashMap<String, QuestaUCDBResult> parseResult(String coverageResults, String vcoverExec, Run<?, ?> run,
			FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
		return parseResult(new HashMap<String, QuestaUCDBResult>(), coverageResults, vcoverExec, run, workspace,
				launcher, listener, null);
	}

	public HashMap<String, QuestaUCDBResult> parseResult(HashMap<String, QuestaUCDBResult> results,
			String coverageResults, String vcoverExec, Run<?, ?> run, FilePath workspace, Launcher launcher,
			TaskListener listener) throws InterruptedException, IOException {
		return parseResult(results, coverageResults, vcoverExec, run, workspace, launcher, listener, null);
	}

	public HashMap<String, QuestaUCDBResult> parseResult(HashMap<String, QuestaUCDBResult> results,
			final String coverageResults, String vcoverExec, Run<?, ?> run, FilePath workspace, Launcher launcher,
			TaskListener listener, Date regressionBegin) throws InterruptedException, IOException {
		final long buildTime = regressionBegin == null ? run.getTimestamp().getTimeInMillis()
				: regressionBegin.getTime();
		PrintStream logger = listener.getLogger();

		// get the output of this build
		logger.println("Trying to locate coverage results \'" + coverageResults + "\' from this run...");

		FilePath[] reports = workspace.act(new MasterToSlaveFileCallable<FilePath[]>() {
                    @Override
                    public FilePath[] invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
                        return findReport(coverageResults, new FilePath(f));
                    }
                });

		if (reports == null || reports.length == 0) {
			logger.println("***[Error]: Could not find any UCDB files."
					+ " Coverage results for this run will not be recorded. ***");
			return results;
		}

		// If multiple runs have happened in the same workspace, we need to ensure we're
		// reporting on the most recent run
		ArrayList<FilePath> recentReports = mostRecent(buildTime, workspace, reports);
		// ensure we have a valid file path
		if (recentReports.isEmpty()) {
			logger.println("[ERROR]: No recent coverage reports.  Returning empty coverage results.");
			return results;
		}

		for (FilePath report : recentReports) {
			QuestaUCDBResult mergeResult = parseResultFromFile(report.getRemote(), vcoverExec, workspace, launcher,
					run.getEnvironment(listener), logger);
			results.put(mergeResult.getCoverageId(), mergeResult);
		}

		return results;
	}

	@SuppressFBWarnings(value = "DM_DEFAULT_ENCODING", justification = "Expected behavior")
	private QuestaUCDBResult parseResultFromFile(String inputfile, String vcoverExec, FilePath workspace,
			Launcher launcher, Map<String, String> env, PrintStream logger)
			throws FileNotFoundException, IOException, InterruptedException {

		logger.println("Processing coverage results from: " + inputfile);
		ByteArrayOutputStream outputstream = new ByteArrayOutputStream();

		QuestaUCDBResult mergeResult = new QuestaUCDBResult(getRelativePath(workspace, inputfile));

		// Processing vcover stats output
		String cmd = vcoverExec + " stats -stats=none -prec 4 -tcl " + inputfile;
		String vcoveroutput = "";
		try {
			Proc proc = launchQuiet(workspace, launcher, env, outputstream, cmd);
			proc.join();
			vcoveroutput = outputstream.toString();
                        // Check if a wrapper was being used instead of the executable.
                        vcoveroutput = getTclOutput(vcoveroutput);
			mergeResult = parseCoverage(mergeResult.getCoverageId(), vcoveroutput);
		} catch (AbortException e) {
			logger.println("[ERROR]: Error processing UCDB \'" + inputfile + "\', with command \'" + cmd + "\'.");
			logger.println("Command Output:" + vcoveroutput);
		} catch (IOException ie) {
			logger.println(
					"[ERROR]: Vcover executable \'" + vcoverExec + "\' not found. Returning empty coverage results.");
			return mergeResult;
		}

		// Processing UCDB Attributes
		cmd = vcoverExec + " attribute  -ucdb -tcl  -stats=none " + inputfile;
		outputstream.reset();

		try {
			Proc proc = launchQuiet(workspace, launcher, env, outputstream, cmd);
			proc.join();
			vcoveroutput = outputstream.toString();
                        // Check if a wrapper was being used instead of the executable.
                        vcoveroutput = getTclOutput(vcoveroutput);
			parseTrendableAttributes(mergeResult.attributesValues, vcoveroutput);
		} catch (AbortException e) {
			logger.println("[ERROR]: During processing global attributes of UCDB \'" + inputfile + "\', with command \'"
					+ cmd + "\'.  Ignoring attributes.");
			logger.println("Command Output:" + vcoveroutput);
		} catch (IOException ie) {
			logger.println(
					"[ERROR]: Vcover executable \'" + vcoverExec + "\' not found. Returning empty coverage results.");
			return mergeResult;
		}

		// Processing trendable attributes
		cmd = vcoverExec + " attribute  -ucdb -tcl -trendable -stats=none " + inputfile;
		outputstream.reset();
		try {
			Proc proc = launchQuiet(workspace, launcher, env, outputstream, cmd);
			proc.join();
			vcoveroutput = outputstream.toString();
                        // Check if a wrapper was being used instead of the executable.
                        vcoveroutput = getTclOutput(vcoveroutput);
			HashMap<String, String> attributes = new HashMap<>();
			parseTrendableAttributes(attributes, vcoveroutput);
			for (Map.Entry<String, String> attrEntry : attributes.entrySet()) {
				// trendable attributes are stored as double
				mergeResult.addTrendableAttribute(attrEntry.getKey(), attrEntry.getValue());
			}

		} catch (AbortException e) {
			logger.println("[ERROR]: During processing trendable global attributes of UCDB \'" + inputfile
					+ "\', with command \'" + cmd + "\'.  Ignoring trendable attributes.");
			logger.println("Command Output:" + vcoveroutput);
		} catch (IOException e) {
			logger.println(
					"[Warning]: Accessing UCDB trendable attributes require Questa version > 10.5a . Ignoring trendable attributes.");
		}

		return mergeResult;
	}

	private Proc launchQuiet(FilePath workspace, Launcher launcher, Map<String, String> env,
			ByteArrayOutputStream outputstream, String cmd) throws IOException {
		ProcStarter ps = launcher.launch();
		ps.cmds(Util.tokenize(cmd)).envs(env).stdin(null).stdout(outputstream).pwd(workspace);
		ps.quiet(true);
		Proc proc = launcher.launch(ps);
		return proc;
	}

	/**
	 *
	 * @param workspace
	 * @return FilePath[],
	 */
	private FilePath[] findReport(String coverageResults, FilePath workspace) throws InterruptedException, IOException {

		File file = new File(coverageResults);
		// workaround if an absolute path is specified that is not relative to the
		// workspace
		if (file.isAbsolute()) {
			FilePath filePath = new FilePath(file);
			if (filePath.exists()) {
				return new FilePath[] { filePath };
			}
			return null;
		}
		FilePath[] reports = workspace.list(coverageResults);

		if (reports.length > 0) {
			return reports;
		}
		return null;
	}

	private ArrayList<FilePath> mostRecent(final long buildTime, FilePath workspace, final FilePath[] reports)
			throws IOException, InterruptedException {
		final long nowMaster = System.currentTimeMillis();
		return workspace.act(new jenkins.SlaveToMasterFileCallable<ArrayList<FilePath>>() {

			@Override
			public ArrayList<FilePath> invoke(File file, VirtualChannel vc) throws IOException, InterruptedException {
				final long nowSlave = System.currentTimeMillis();
				ArrayList<FilePath> recentReports = new ArrayList<>();

				for (FilePath report : reports) {
					try {
						long lastmod = report.lastModified();
						if ((buildTime + (nowSlave - nowMaster) - 3000) <= lastmod) {
							recentReports.add(report);
						}
					} catch (IOException e) {

					} catch (InterruptedException e) {

					}
				}
				return recentReports;
			}

		});

	}

	private static TclList parseTcl(String in, String firstToken) throws AbortException {
		TclList root = genericParse(in);
		TclToken current = root;
		while (current instanceof TclList && current.size() > 0) {
			current = ((TclList) current).getChildren().getFirst();
		}
		if (!current.toString().startsWith(firstToken)) {
			throw new AbortException("Unexpected return value");
		}
		return root;
	}

	static void parseTrendableAttributes(HashMap attributes, String vcoverOutput) throws AbortException {
		TclList root = parseTcl(vcoverOutput, "ATTRIBUTE");
		attributes.clear();

		LinkedList<TclToken> list = root.getChildren();

		// first entry is ATTRIBUTE UCDB, neglect it..
		list.removeFirst();

		for (TclToken child : list) {
			if (child instanceof TclList) {
				TclList childList = (TclList) child;
				if (child.size() == 2 && childList.areLeafChildren()) {
					attributes.put(childList.getChildren().getFirst().toString(),
							childList.getChildren().getLast().toString());
				}
			} else {
				String[] key_value = child.toString().split(" ");
				if (key_value.length != 2) {
					continue;
				}
				attributes.put(key_value[0], key_value[1]);

			}

		}
	}

	static QuestaUCDBResult parseCoverage(String coverageID, String vcoveroutput) throws AbortException {
		TclList root = parseTcl(vcoveroutput, "Filename");
		QuestaUCDBResult mergeResult = new QuestaUCDBResult(coverageID);

		// First-level elements should contribute to the mergefile itself
		LinkedList<TclToken> children = root.getChildren();
		while (!children.isEmpty()) {
			TclToken child = children.remove(0); /* removing children after processing to save memory */
			constructCoverageResults(mergeResult, mergeResult, child);
		}
		return mergeResult;
	}

	private static void constructCoverageResults(QuestaUCDBResult mergeResult, QuestaCoverageResult coverage,
			TclToken current) {
		// leaf level of vcover stats correspond to coverage values
		if (!(current instanceof TclList)) {
			String[] key_value = current.toString().split(" ");
			if (key_value.length == 2) {
				try {
					CoverageTypes type = CoverageTypes.valueOf(key_value[0]);
					switch (type) {
					case TestplanCoverage:
						coverage.setTestplanCov(key_value[1]);
						break;
					case TotalCoverage:
						coverage.setTotalCoverage(key_value[1]);
						break;
					default:
						coverage.add(type.getDisplayName(), key_value[1]);

					}
				} catch (Exception e) {

				}

			}
			return;
		}

		TclList currentList = (TclList) current;
		// A pair corresponds to an attribute
		if (current.size() == 2 && currentList.areLeafChildren()) {
			coverage.addAttributes(currentList.getChildren().getFirst().toString(),
					currentList.getChildren().getLast().toString());
		} else {

			QuestaCoverageResult result = new QuestaCoverageResult();

			// recursively process all children
			LinkedList<TclToken> children = currentList.getChildren();
			while (!children.isEmpty()) {
				TclToken child = children.remove(0); /* removing children after processing to save memory */
				constructCoverageResults(mergeResult, result, child);
			}

			if (result.isTest()) {
				// filter out history records
				if (!result.getCoverageId().endsWith(mergeResult.getCoverageId())) {
					mergeResult.addTest(result);
				}
			} else {
				// flatten non-test scopes (USERATTR|CHILDREN...)
				for (Map.Entry<String, String> attrEntry : result.attributesValues.entrySet()) {
					coverage.addAttributes(attrEntry.getKey(), attrEntry.getValue());
				}
			}
		}
	}

	/**
	 * returns the parsed tcl list.
	 *
	 * @param line
	 * @return
	 */
	private static TclList genericParse(String line) throws AbortException {
		Map<String, TclToken> leafTokens = new HashMap<>();
		TclList root = new TclList();
		TclList temp;
		TclList current = root;
		line = line.trim();
		if (line.isEmpty() || line.charAt(0) != '{') {
			throw new AbortException("Expected tcl list not found");
		}
		// start from the next character
		for (int i = 0; i < line.length(); i++) {
			switch (line.charAt(i)) {
			case '{':
				temp = new TclList();
				temp.parent = (TclList) current;
				current = temp;
				break;
			case '}':
				if (current.parent == null) {
					throw new AbortException("Poorly formed tcl list");
				}
				switch (current.size()) {
				case 0:
					break;
				case 1:
					// flatten single lists
					current.parent.addChild(current.getChildren().getFirst());
					break;
				default:
					current.parent.addChild(current);
				}
				current = current.parent;
				break;
			case ' ':
				break;
			default:

				StringBuilder value = new StringBuilder();
				while (i < line.length() && (line.charAt(i) != '}' || line.charAt(i - 1) == '\\')
						&& (line.charAt(i) != '{' || line.charAt(i - 1) == '\\')) {
					value.append(line.charAt(i));
					i++;
				}
				i--;

				// add only non-empty values..
				if (value.toString().trim().length() > 0) {
					TclToken token = null;
					String valueString = value.toString().trim();
					if (!leafTokens.containsKey(valueString)) {
						try {
							token = new TclToken(valueString.getBytes("ASCII"));
						} catch (UnsupportedEncodingException e) {
							// TODO should log an error here
						}
						leafTokens.put(valueString, token);
					} else {
						token = leafTokens.get(valueString);
					}
					current.addChild(token);
				}
			}
		}
		if (current != root) {
			throw new AbortException("Poorly formed tcl list.");
		}

		if (root.getChildren().getFirst() instanceof TclList) {
			return (TclList) root.getChildren().getFirst();
		} 
		return root;
	}

        /**
	 * Checks if vcoverOutput is from a script or wrapper not directly from
         * running the executable.
	 *
	 * @param vcoverOutput
	 * @return String
	 */
        private String getTclOutput(String vcoverOutput) {
            int vcoverStart;
            vcoverStart = vcoverOutput.indexOf('{');
            if ((vcoverStart+1) >= 0 && (vcoverStart+1) < vcoverOutput.length() && (vcoverOutput.charAt(vcoverStart+1) == '{' || vcoverOutput.charAt(vcoverStart+1) == '}'))
               return vcoverOutput.substring(vcoverStart);
            return vcoverOutput;

        }

	private static class TclList extends TclToken {
		private LinkedList<TclToken> children;
		TclList parent;

		public TclList() {
			super(null);
			children = new LinkedList<>();

		}

		@Override
		public int size() {
			return children.size();
		}

		public LinkedList<TclToken> getChildren() {
			return children;
		}

		public void addChild(TclToken x) {
			children.add(x);
		}

		public boolean areLeafChildren() {

			for (TclToken child : children) {
				if ((child instanceof TclList)) {
					return false;
				}
			}
			return true;

		}

	}

	private static class TclToken {
		private final byte[] value;

		public TclToken(byte[] value) {
			this.value = value;
		}

		public int size() {
			return 1;
		}

		@Override
		public String toString() {
			try {
				return new String(value, "ASCII");
			} catch (UnsupportedEncodingException e) {
				// e.printStackTrace();
			}
			return "";
		}

	}

	private enum CoverageTypes {

		Covergroups("Covergroups"), CoverDirectives("Directives"), Statements("Statements"), Branches(
				"Branches"), UdpExpressions("UDP Expressions"), UdpConditions("UDP Conditions"), ToggleNodes(
						"Toggles"), States("FSMs States"), Transitions("FSMs Transitions"), FecExpressions(
								"FEC Expressions"), FecConditions("FEC Conditions"), AssertSuccesses(
										"Assertions"), TestplanCoverage, TotalCoverage;

		private String displayName;

		private CoverageTypes() {
		}

		private CoverageTypes(String displayName) {
			this.displayName = displayName;
		}

		public String getDisplayName() {
			return displayName;
		}

	};

}
