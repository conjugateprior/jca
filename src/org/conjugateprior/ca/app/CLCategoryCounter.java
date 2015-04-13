package org.conjugateprior.ca.app;

import java.nio.charset.Charset;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;

public class CLCategoryCounter extends CLApplication {

	protected CategoryCounter counter;
		
	public String getUsage() {
		return "cat [-encoding <encoding>] [-locale <locale>] " +
				   "[-oldmatching] [-output <folder>] [-format <format>] [-silent] " +
				   "-dictionary <file> " +
				   "[doc1.txt doc2.txt folder1]";
	}
	
	public CLCategoryCounter(CategoryCounter c) {
		super();	
		
		counter = c;
		
		addOption(getHelpOption(false));
		addOption(getEncodingOption(false));
		addOption(getLocaleOption(false));
		addOption(getOuputFolderOption(true)); // not file
		addOption(getDictionaryOption(true));
		addOption(getSilentOption());
		
		CLOption oldmatching = new CLOption("oldmatching", 
				"Use old-style pattern matching");
		addOption(oldmatching);
		
		Option format = new Option("format", true, 
			"One of: utf8, text (default: text, which here means " + 
			Charset.defaultCharset().name() + ")");
		format.setArgName("format");
		addOption(format);
	}
	
	@Override
	public void processLine(String[] args) throws Exception {	
		CommandLineParser parser = new GnuParser();
		CommandLine line = parser.parse(options, args);
		
		if (line.hasOption("help")) {
			printUsageAndOptions();
		}
		if (line.hasOption("locale")){
			try {
				counter.setLocale(line.getOptionValue("locale"));
			} catch (Exception ex){
				throw new Exception(getOptionErrorMessage("locale"));
			}
		}
		if (line.hasOption("encoding")){ // if not we get the default charset
			try {
				counter.setEncoding(line.getOptionValue("encoding"));
			} catch (Exception ex){
				throw new Exception(getOptionErrorMessage("encoding"));
			}
		}
		
		counter.setDictionary(line.getOptionValue("dictionary"));
		counter.setUsingOldMatchStrategy(line.hasOption("oldmatching"));
		counter.setSilent(line.hasOption("silent"));
		
		// and files
		String[] files = line.getArgs();
		counter.setFiles(files);
		if (files.length == 0)
			throw new Exception("No documents or folders of documents to process!");

		if (line.hasOption("format")){
			String opt = line.getOptionValue("format").toLowerCase();
			counter.setFormat(opt);
			if (opt.equals("utf-8") || opt.equals("utf8") || opt.equals("html"))
				counter.setOutputEncoding(Charset.forName("UTF-8"));
		}
		
		if (line.hasOption("output"))
			counter.setOutputFolder(line.getOptionValue("output"));
				
		counter.processFiles();
		
		System.exit(0);
	}
	
	/*
	public static void main(String[] args) throws Exception {
		CategoryCounter cc = new CategoryCounter();
		CLCategoryCounter c = new CLCategoryCounter(cc);
		try {
			c.processLine(args);
		} catch (Exception ex){
			System.err.println(ex.getMessage());
			c.printUsageAndOptions();
		}
	}
	*/
}
