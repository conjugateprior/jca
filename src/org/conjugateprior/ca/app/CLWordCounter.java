package org.conjugateprior.ca.app;

import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.conjugateprior.ca.reports.VocabularyFilterer;

public class CLWordCounter extends CLApplication {

	protected WordCounter counter;
	
	protected String llist = "danish dutch english finnish french german hungarian " + 
			"italian norwegian portuguese romanian russian spanish swedish turkish";
	
	public String getUsage(){
		return "word [-encoding <encoding>] [-locale <locale>] " + 
				"[-regexp <regexp>]" +   
	           "[-no_currency] [-no_numbers] [-stopwords <file>] " +
			   "[-stemmer <language>] [-format <format>]  [-silent] -output <folder> " +
	           "[doc1.txt doc2.txt folder1]";
	}
	
	public CLWordCounter(WordCounter c) {
		super();
		counter = c;
				
		addOption(getHelpOption(false));
		addOption(getEncodingOption(false));
		addOption(getLocaleOption(false));
		addOption(getRegexpOption(false));
		addOption(getOutputFolderOption(true)); // not file
		addOption(getSilentOption());	
		
		Option form = new Option("format", true, 
				"One of: ldac (default), mtx, csv (only for small corpora...)");
		form.setArgName("format");
		addOption(form);
		
		Option stopwords = new Option("file", true,
				"File of words not to be counted");
		stopwords.setArgName("stopwords");
		addOption(stopwords);
		
		Option currency = new Option("no_currency", false,
				"Remove strings beginning with currency signs");
		addOption(currency);
		
		Option numbers = new Option("no_numbers", false,
				"Remove strings beginning with digits");
		addOption(numbers);

		Option stemmer = new Option("stemmer", true,
		"Stem files before counting. (This happens last). One of: " + llist);
		stemmer.setArgName("language");
		addOption(stemmer);
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
		
		if (line.hasOption("regexp")){
			counter.setUsingRegexpTokenizer(true);
			String d = line.getOptionValue("regexp");
			if (d == null)
				throw new Exception("No regexp provided");
			counter.setRegexp(d);
		}
		
		// filter business
		VocabularyFilterer filterer = counter.getFilterer();
		if (line.hasOption("no_numbers"))
			filterer.addNoNumberFilter();
		if (line.hasOption("no_currency"))
			filterer.addNoCurrencyFilter();
		if (line.hasOption("stopwords")){
			File f = new File(line.getOptionValue("stopwords"));
			if (!f.exists())
				throw new Exception("Could not find stopword file " +
						f.getAbsolutePath());
			filterer.addStopwordFilter(f);
		}
		if (line.hasOption("stemmer")){
			String langname = line.getOptionValue("stemmer").toLowerCase();
			String[] langs = llist.split(" ");
			String stemmerLanguage = null;
			for (String l : langs) {
				if (langname.equals(l)){
					stemmerLanguage = l;
					break;
				}
			}
			if (stemmerLanguage == null)
				throw new Exception("Unrecognized language for stemmer. Must be one of: " 
							+ llist);
			filterer.addStemmingFilter(stemmerLanguage);
		}

		counter.setSilent(line.hasOption("silent"));

		// and files
		String[] files = line.getArgs();
		counter.setFiles(files);
		if (files.length == 0)
			throw new Exception("No documents or folders of documents to process!");
		if (line.hasOption("format"))
			counter.setFormat(line.getOptionValue("format").toLowerCase());
		
		counter.setOutputFolder(line.getOptionValue("output"));
				
		counter.processFiles();
		
		System.exit(0);
	}
	
	/*
	public static void main(String[] args) {
		WordCounter cc = new WordCounter();
		CLWordCounter c = new CLWordCounter(cc);
		try {
			c.processLine(args);
		} catch (Exception ex){
			System.err.println(ex.getMessage());
			c.printUsageAndOptions();
		}
	}
	*/
	
}
