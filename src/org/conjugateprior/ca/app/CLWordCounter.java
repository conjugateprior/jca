package org.conjugateprior.ca.app;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Locale;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.conjugateprior.ca.reports.VocabularyFilterer;

public class CLWordCounter {

	protected WordCounter counter;
	
	protected Options options;
	protected HelpFormatter helpFormatter;
	
	protected String llist = "danish dutch english finnish french german hungarian " + 
			"italian norwegian portuguese romanian russian spanish swedish turkish";
	
	public static String USAGE = "ykwords [-encoding <encoding>] [-locale <locale>] " + 
	           "[-no_currency] [-no_numbers] [-stopwords <file>] " +
			   "[-stemmer <language>] [-format <format>] -output <folder> " +
	           "[doc1.txt doc2.txt folder1]";
	
	public void printUsageAndOptions(){
		helpFormatter.printHelp(USAGE, options);
	}
	
	public CLWordCounter(WordCounter c) {
		counter = c;
		
		helpFormatter = new HelpFormatter();
		options = new Options();
		
		Option oHelp = new Option("help", "Show this message, then exit");
		Option oEncoding = new Option("encoding", true, 
				"Input file character encoding (default: " + 
				Charset.defaultCharset().displayName() + ")");
		oEncoding.setArgName("encoding");
		
		Option oLocale = new Option("locale",  true, 
				"Locale for input files (default: " + 
				Locale.getDefault().toString() + ")");
		oLocale.setArgName("locale");				
		
		Option oStopwords = new Option("stopwords", true, "File of words not to be counted");
		oStopwords.setArgName("file");
		
		Option oCurrency = new Option("no_currency", 
				"Remove strings beginning with currency signs");
		Option oNumbers = new Option("no_numbers", 
				"Remove strings beginning with digits");
		Option oStemmer = new Option("stemmer",  true, 
				"Stem files before counting (happens last). One of: " + llist);
		oStemmer.setArgName("language");	

		Option oOutputfile = new Option("output", true, 
				"Name for an output folder");
		oOutputfile.setRequired(true);
		oOutputfile.setArgName("folder");
		
		Option oFormat = new Option("format", true, 
				"One of: utf8, text (default: text, which here means " + Charset.defaultCharset().name() + ")");
		oFormat.setArgName("format");
		
		options.addOption(oHelp);
		options.addOption(oEncoding);
		options.addOption(oLocale);
		options.addOption(oOutputfile);
		options.addOption(oStopwords);
		options.addOption(oFormat);
		options.addOption(oCurrency);
		options.addOption(oNumbers);
		options.addOption(oStemmer);
	}
	
	public void processLine(String[] args) throws Exception {	
		CommandLineParser parser = new GnuParser();
		CommandLine line = parser.parse(options, args);
		
		if (line.hasOption("help")) {
			printUsageAndOptions();
			System.exit(0);
		}
		if (line.hasOption("locale")){ // if not we get the default locale
			try {
				counter.setLocale(line.getOptionValue("locale"));
			} catch (Exception ex){
				throw new Exception(
						"Could not parse locale argument.\n" + 
				        "A valid locale consists of a two letter language codes from ISO 639\n" +
						"optionally connected by an underscore to a two letter country code\n" +
				        "from ISO 3166.  See also http://en.wikipedia.org/wiki/BCP_47");
			}
		} 
		if (line.hasOption("encoding")){ // if not we get the default charset
			try {
				counter.setEncoding(line.getOptionValue("encoding"));
			} catch (Exception ex){
				throw new Exception("Could not parse file encoding. Error message follows:\n" +
						ex.getMessage());
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

		if (line.hasOption("format")){
			String opt = line.getOptionValue("format").toLowerCase();
			counter.setFormat(opt);
			
		}
		
		counter.setOutputFolder(line.getOptionValue("output"));
				
		counter.processFiles();
	}
	
	public static void main(String[] args) throws Exception {
		//ykcats -dictionary ~/Dropbox/blogposts/littledict.vbpro -output thing ~/Dropbox/blogposts/speeches
		/*
		String[] as = new String[]{
									"-output", "/Users/will/Dropbox/blogposts/ykwordsoutputfolder", 
									"-format", "ldac", 
									"-no_numbers",
									"-no_currency",
									"-stemmer", "english", 
									"/Users/will/Dropbox/blogposts/speeches"};
		*/
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
