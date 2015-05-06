package org.conjugateprior.ca.app;

import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;

public class CLDescription extends CLApplication {
	
	protected Description description;

	@Override
	public String getUsage() {
		return "desc [-encoding <encoding>] [-locale <locale>] " +
				"[-output <file>] [-silent] [doc1.txt doc2.txt folder1]";
	}
	
	public CLDescription(Description c) {
		super();
		description = c;

		addOption(getHelpOption(false));
		addOption(getEncodingOption(false));
		addOption(getLocaleOption(false));
		addOption(getOuputFileOption(false)); // not folder
		addOption(getSilentOption());
	}

	@Override
	public void processLine(String[] args) throws Exception {	
		CommandLineParser parser = new GnuParser();
		CommandLine line = parser.parse(options, args);
		
		if (line.hasOption("help")) {
			printUsageAndOptions();
			System.exit(0);
		}
		if (line.hasOption("locale")){ 
			try {
				description.setLocale(line.getOptionValue("locale"));
			} catch (Exception ex){
				throw new Exception(getOptionErrorMessage("locale"));
			}
		} 
		if (line.hasOption("encoding")){ 
			try {
				description.setEncoding(line.getOptionValue("encoding"));
			} catch (Exception ex){
				throw new Exception(getOptionErrorMessage("encoding"));
			}
		} 		
		if (line.hasOption("output")) 
			description.setOutputFile(new File(line.getOptionValue("output")));
		
		description.setSilent(line.hasOption("silent"));

		
		// and files
		String[] files = line.getArgs();
		description.setFiles(files);
		if (files.length == 0)
			throw new Exception("No documents or folders of documents to process!");

		description.processFiles();
		System.exit(0);
	}

	/*
	public static void main(String[] args) throws Exception {
		Description cc = new Description();
		CLDescription c = new CLDescription(cc);
		try {
			c.processLine(args);
			//c.processLine(args);
		} catch (Exception ex){
			System.err.println(ex.getMessage());
			c.printUsageAndOptions();
		}
	}
	*/

}
