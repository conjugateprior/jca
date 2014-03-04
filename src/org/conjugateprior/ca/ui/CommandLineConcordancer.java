package org.conjugateprior.ca.ui;

import org.apache.commons.cli.CommandLine;

// TODO make me go
public class CommandLineConcordancer extends CommandLineApplication {

	@Override
	protected String getUsageString() {
		return "ykconcordancer -targets <file> -pattern <regexps> -window <number> -format [txt|html|csv] -output <folder name> [file1 file2 | folder1]";
	}
	
	protected void processLine(CommandLine line) throws Exception {
		//
	}
	
}
