package org.conjugateprior.ca.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.util.Locale;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.conjugateprior.ca.WordReportFormatter;
import org.conjugateprior.ca.WordReportFormatter.Printer;
import org.conjugateprior.ca.WordReporter;

public class CommandLineWordCounter extends CommandLineApplication {
	
	protected Locale tLocale = Locale.getDefault();
	protected Charset tEncoding = Charset.defaultCharset();
	protected File tOutputfile = null;
	
	protected boolean removeCurrency = false;
	protected boolean removeNumbers = false;
	protected File[] filesToProcess;
	protected File stopwordFile;
	protected boolean stem = false;
	
	protected boolean mtx = false;
	protected boolean ldac = true;
	
	protected FileOutputStream streamData;
	protected FileOutputStream streamWords;
	protected FileOutputStream streamDocs;
	
	@Override
	protected String getUsageString(){
		return "ykwordcounter [options] -output <folder name> [doc1.txt doc2.txt folder1]";
	}
	
	public CommandLineWordCounter() {
		super();
		
		Option help = new Option("help", "Show this message, then exit");
		Option encoding = new Option("encoding", true, "Character encoding for input files e.g. UTF8");
		encoding.setArgName("encoding name");
		Option removeStopwords = new Option("stopwords", true, "Stopwords (words not to count)");
		removeStopwords.setArgName("file");
		Option locale = new Option("locale",  true, "Locale for input files e.g. en_US");
		locale.setArgName("locale name");		
		Option noCurrency = new Option("no_currency", "Remove currency amounts");
		Option noNumbers = new Option("no_numbers", "Remove numerical quantities");

		Option outputfile = new Option("output", true, "Name for the output folder");
		outputfile.setRequired(true);
		outputfile.setArgName("folder name");

		Option stemmer = new Option("stemmer",  true, "Stem the input files (happens last)");
		stemmer.setArgName("language name");	

		Option format = new Option("format", true, "Matrix format for output");
		outputfile.setArgName("matrix format");
		
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
		}
		if (line.hasOption("encoding")){
			try {
				tEncoding = Charset.forName(line.getOptionValue("encoding"));
			} catch (Exception ex){
				throw new Exception("Could not parse file encoding. Error message follows:\n" +
						ex.getMessage());
			}
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
			String llist = "danish dutch english finnish french german hungarian " + 
					"italian norwegian portuguese romanian russian spanish swedish turkish";
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
		if (line.hasOption("format")){
			form = line.getOptionValue("format");
			if (form.equals("ldac"))
				ldac = true;
			else if (form.equals("mtx"))
				mtx = true;
			else
				throw new Exception("Unrecognized matrix format argument. Must be one of: ldac mtx");
		} else {
			form = "ldac";
			ldac = true;
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
		
		WordReporter rep = new WordReporter();
		if (removeNumbers)
			rep.addFilter(rep.new NoNumberFilter());
		if (removeCurrency)
			rep.addFilter(rep.new NoCurrencyFilter());
		if (stopwordFile != null)
			rep.addFilter(rep.new StopwordFilter(stopwordFile));
		if (stem)
			rep.addFilter(rep.getStemmerByName(stemmerLanguage));
		
		WordReportFormatter formatter = null;
		Printer worker = null;
		if (ldac){ 
			formatter = new WordReportFormatter(rep, 
					WordReportFormatter.OutputFormat.LDAC, tOutputfile, 
					tEncoding, tLocale, filesToProcess);
			worker = formatter.getReportPrinter();
		} else if (mtx){
			formatter = new WordReportFormatter(rep, 
					WordReportFormatter.OutputFormat.MTX, tOutputfile, 
					tEncoding, tLocale, filesToProcess);
			worker = formatter.getReportPrinter();
		} else {
			System.err.println("Should never get here!");
		}
		
		worker.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if ("progress" == evt.getPropertyName()) {
					int progress = (Integer) evt.getNewValue();
					System.err.println(progress + "%");
				} 
			}
		});
		
		worker.execute();
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
