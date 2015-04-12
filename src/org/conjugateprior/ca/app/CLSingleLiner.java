package org.conjugateprior.ca.app;

import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;

public class CLSingleLiner extends CLApplication {

	protected SingleLiner liner;

	@Override
	public String getUsage() {
		return "line [-encoding <encoding>] [-locale <locale>] " +
				"[-output <file>] [doc1.txt doc2.txt folder1]";
	}
	
	public CLSingleLiner(SingleLiner c) {
		super();
		liner = c;

		addOption(getHelpOption(false));
		addOption(getEncodingOption(false));
		addOption(getLocaleOption(false));
		addOption(getOuputFileOption(false)); // not folder
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
				liner.setLocale(line.getOptionValue("locale"));
			} catch (Exception ex){
				throw new Exception(getOptionErrorMessage("locale"));
			}
		} 
		if (line.hasOption("encoding")){ 
			try {
				liner.setEncoding(line.getOptionValue("encoding"));
			} catch (Exception ex){
				throw new Exception(getOptionErrorMessage("encoding"));
			}
		} 		
		if (line.hasOption("output")) 
			liner.setOutputFile(new File(line.getOptionValue("output")));
		else 
			liner.setOutputFile(new File("singleline.txt"));
		
		// and files
		String[] files = line.getArgs();
		liner.setFiles(files);
		if (files.length == 0)
			throw new Exception("No documents or folders of documents to process!");

		liner.processFiles();
		System.exit(0);
	}

	/*
	public static void main(String[] args) throws Exception {
		SingleLiner cc = new SingleLiner();
		CLSingleLiner c = new CLSingleLiner(cc);
		try {
			c.processLine(args);
		} catch (Exception ex){
			System.err.println(ex.getMessage());
			c.printUsageAndOptions();
		}
	}
	*/
}
