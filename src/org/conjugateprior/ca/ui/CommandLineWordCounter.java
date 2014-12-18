package org.conjugateprior.ca.ui;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.util.Locale;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.conjugateprior.ca.reports.CountPrinter;
import org.conjugateprior.ca.reports.ICountPrinter;
import org.conjugateprior.ca.reports.WordCounter;

public class CommandLineWordCounter extends CommandLineApplication {
	
	protected Locale tLocale;
	protected Charset tEncoding;
	protected File tOutputfile = null;
	
	protected boolean removeCurrency = false;
	protected boolean removeNumbers = false;
	protected File[] filesToProcess;
	protected File stopwordFile;
	protected boolean stem = false;
	
	protected ICountPrinter.Format outputFormat;
	
	protected FileOutputStream streamData;
	protected FileOutputStream streamWords;
	protected FileOutputStream streamDocs;
	
	protected String llist = "danish dutch english finnish french german hungarian " + 
			"italian norwegian portuguese romanian russian spanish swedish turkish";
	
	@Override
	protected String getUsageString(){
		return "ykwordcounter [-encoding <encoding>] [-locale <locale>] " + 
	           "[-no_currency] [-no_numbers] [-stopwords <file>] " +
			   "[-stemmer <language>] [-format <format>] -output <folder> " +
	           "[doc1.txt doc2.txt folder1]";
	}
	
	public CommandLineWordCounter() {
		super();
				
		Option help = new Option("help", "Show this message, then exit");
		Option encoding = new Option("encoding", true, 
			"Input file character encoding (default: " + 
		    Charset.defaultCharset().name() + ")");
		encoding.setArgName("encoding");
		Option removeStopwords = new Option("stopwords", true, "File of words not to be counted");
		removeStopwords.setArgName("file");
		Option locale = new Option("locale",  true, 
				"Locale for input files (default: " + 
		        Locale.getDefault().toString() + ")");
		locale.setArgName("locale");		
		Option noCurrency = new Option("no_currency", 
				"Remove strings beginning with currency signs");
		Option noNumbers = new Option("no_numbers", 
				"Remove strings beginning with digits");

		Option outputfile = new Option("output", true, 
				"Name for the output folder");
		outputfile.setRequired(true);
		outputfile.setArgName("folder");

		Option stemmer = new Option("stemmer",  true, 
				"Stem files before counting (happens last). One of: " + llist);
		stemmer.setArgName("language");	

		Option format = new Option("format", true, 
				"One of: ldac (the default), mtx");
		format.setArgName("format");
		
		addCommandLineOption(help);
		addCommandLineOption(encoding);
		addCommandLineOption(locale);
		addCommandLineOption(outputfile);
		addCommandLineOption(format);
		addCommandLineOption(noCurrency);
		addCommandLineOption(noNumbers);
		addCommandLineOption(removeStopwords);
		addCommandLineOption(stemmer);
	}

	@Override
	protected void processLine(CommandLine line) throws Exception {	
		
		if (line.hasOption("help")){
			printUsageAndOptions();
			System.exit(0);
		}
		if (line.hasOption("locale")){
			try {
				tLocale = translateLocale(line.getOptionValue("locale"));
			} catch (Exception ex){
				throw new Exception(
						"Could not parse locale argument.\n" + 
				        "A valid locale consists of a two letter language codes from ISO 639\n" +
						"optionally connected by an underscore to a two letter country code\n" +
				        "from ISO 3166.  See also http://en.wikipedia.org/wiki/BCP_47");
			}
		} else {
			tLocale = Locale.getDefault();
		}
		if (line.hasOption("encoding")){
			try {
				tEncoding = Charset.forName(line.getOptionValue("encoding"));
			} catch (Exception ex){
				throw new Exception("Could not parse file encoding. Error message follows:\n" +
						ex.getMessage());
			}
		} else {
			tEncoding = Charset.defaultCharset();
		}
		removeCurrency = line.hasOption("no_currency");
		removeNumbers = line.hasOption("no_numbers");
		tOutputfile = new File(line.getOptionValue("output"));
		
		if (line.hasOption("stopwords")){
			stopwordFile = new File(line.getOptionValue("stopwords"));
			if (!stopwordFile.exists())
				throw new Exception("Could not find stopword file " +
						stopwordFile.getAbsolutePath());
		}
		
		String stemmerLanguage = null;
		if (line.hasOption("stemmer")){
			stem = true;
			String langname = line.getOptionValue("stemmer").toLowerCase();
			
			String[] langs = llist.split(" ");
			for (String l : langs) {
				if (langname.equals(l)){
					stemmerLanguage = l;
					break;
				}
			}
			if (stemmerLanguage == null){
				throw new Exception("Unrecognized language for stemmer. Must be one of: " + llist);
			}
		}
		
		String form = null;
		// default is ldac
		if (line.hasOption("format")){
			form = line.getOptionValue("format");
			if (form.equals("mtx"))
				outputFormat = ICountPrinter.Format.MTX;
			else if (form.equals("ldac"))
				outputFormat = ICountPrinter.Format.LDAC;
			else
				throw new Exception("Unrecognized matrix format argument. Must be one of: ldac mtx");
		}
		
		String[] files = line.getArgs();
		if (files.length == 0)
			throw new Exception("No documents or folders of documents to process!");
		filesToProcess = getRecursiveDepthOneFileArray(files);
		
		// wiederholen
		System.err.println("Settings:");
		System.err.println("  Locale of input files: " + tLocale + " ie '" + 
				tLocale.getDisplayName() + "'");
		System.err.println("  Encoding of input files: " + tEncoding.name() + " ie '" +
				tEncoding.displayName() + "'");
		System.err.println("  Remove numbers? " + removeNumbers);
		System.err.println("  Remove currency? " + removeCurrency);
		System.err.println("  Remove stopwords? " + ((stopwordFile != null) ? 
				"yes, from " + stopwordFile.getAbsolutePath() : "no"));
		System.err.println("  Stem tokens? " + (stem ? ("True (using " + stemmerLanguage + " stemmer)") : "False"));
		System.err.println("  Matrix output format: " + form);
		
		System.err.println("And here come the files...");
		
		// here we go...
		if (tOutputfile.exists())
			throw new Exception(tOutputfile.getAbsolutePath() + " already exists. " + 
					"Halting to prevent data loss.");
		
		WordCounter rep = new WordCounter();
		if (removeNumbers)
			rep.addFilter(rep.new NoNumberFilter());
		if (removeCurrency)
			rep.addFilter(rep.new NoCurrencyFilter());
		if (stopwordFile != null)
			rep.addFilter(rep.new StopwordFilter(stopwordFile));
		if (stem)
			rep.addStemmingFilter(stemmerLanguage);
		
		ICountPrinter wp = CountPrinter.getWordCountPrinter(rep, 
					outputFormat, tOutputfile, 
					tEncoding, tLocale, filesToProcess);

		/*
		wp.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if ("progress" == evt.getPropertyName()) {
					int progress = (Integer) evt.getNewValue();
					System.err.println(progress + "%");
				} 
			}
		});
		*/
		wp.processFiles(true);
		//worker.execute();
	}
	
	public static void main(String[] args) {
		CommandLineWordCounter rep = new CommandLineWordCounter();
		try {
			rep.process(args);
		} catch (Exception ex){
			System.err.println(ex.getMessage());
			rep.printUsageAndOptions();
			System.exit(0);
		}
	}	
}
