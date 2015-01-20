package org.conjugateprior.ca.app;

import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.conjugateprior.ca.reports.VocabularyFilterer;

public class CLWordCounter extends CLApplication {

	protected WordCounter counter;
	
	protected String llist = "danish dutch english finnish french german hungarian " + 
			"italian norwegian portuguese romanian russian spanish swedish turkish";
	
	public String getUsage(){
		return "ykwords [-encoding <encoding>] [-locale <locale>] " + 
	           "[-no_currency] [-no_numbers] [-stopwords <file>] " +
			   "[-stemmer <language>] [-format <format>] -output <folder> " +
	           "[doc1.txt doc2.txt folder1]";
	}
	
	public CLWordCounter(WordCounter c) {
		super();
		counter = c;
				
		addOption(getHelpOption(false));
		addOption(getEncodingOption(false));
		addOption(getLocaleOption(false));
		addOption(getOuputFolderOption(true)); // not file
			
		Option format = OptionBuilder.withArgName("format").hasArg()
			.withDescription("One of: liwc, mtx (default: liwc)")
			.create("format");
		addOption(format);
		
		Option stopwords = OptionBuilder.withArgName("file").hasArg()
				.withDescription("File of words not to be counted")
				.create("stopwords");
		addOption(stopwords);
		
		Option currency = OptionBuilder
				.withDescription("Remove strings beginning with currency signs")
				.create("currency");
		addOption(currency);
		
		Option numbers = OptionBuilder
				.withDescription("Remove strings beginning with digits")
				.create("no_numbers");
		addOption(numbers);

		Option stemmer = OptionBuilder.hasArg().withArgName("language")
				.withDescription("Stem files before counting. (This happens last). One of: " + llist)
			   .create("stemmer");
		addOption(stemmer);
	}
	
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
	
	public static void main(String[] args) throws Exception {
		WordCounter cc = new WordCounter();
		CLWordCounter c = new CLWordCounter(cc);
		try {
			c.processLine(args);
		} catch (Exception ex){
			System.err.println(ex.getMessage());
			c.printUsageAndOptions();
		}
	}
	
}
