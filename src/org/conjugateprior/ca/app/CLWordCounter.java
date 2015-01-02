package org.conjugateprior.ca.app;

import java.nio.charset.Charset;
import java.util.Locale;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class CLWordCounter {

	protected WordCounter counter;
	
	protected Options options;
	protected HelpFormatter helpFormatter;
	
	public static String USAGE = "ykwords [-encoding <encoding>] [-locale <locale>] " +
	   "[-format <format>] -output <folder>" +
	   "[doc1.txt doc2.txt folder1]";
	
	public void printUsageAndOptions(){
		helpFormatter.printHelp(CLCategoryCounter.USAGE, options);
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
		
		Option oOutputfile = new Option("output", true, 
				"Name for an output file");
		oOutputfile.setRequired(true);
		oOutputfile.setArgName("file");
		
		Option oFormat = new Option("format", true, 
				"One of: utf8, text (default: text, which here means " + Charset.defaultCharset().name() + ")");
		oFormat.setArgName("format");
		
		options.addOption(oHelp);
		options.addOption(oEncoding);
		options.addOption(oLocale);
		options.addOption(oOutputfile);
		options.addOption(oFormat);
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
		String[] as = new String[]{/* "-dictionary", "/Users/will/Dropbox/blogposts/littledict.vbpro", */
									"-output", "/Users/will/Dropbox/blogposts/ykwordsoutputfolder", 
									"-format", "mtx", "/Users/will/Dropbox/blogposts/speeches"};
		
		WordCounter cc = new WordCounter();
		CLWordCounter c = new CLWordCounter(cc);
		try {
			c.processLine(as);
		} catch (Exception ex){
			System.err.println(ex.getMessage());
			c.printUsageAndOptions();
		}
	}
	
}
