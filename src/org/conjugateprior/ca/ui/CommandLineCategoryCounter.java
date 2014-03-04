package org.conjugateprior.ca.ui;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.util.Locale;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.conjugateprior.ca.CategoryDictionary;
import org.conjugateprior.ca.CategoryReporter;
import org.conjugateprior.ca.IYoshikoderDocument;
import org.conjugateprior.ca.SimpleDocumentTokenizer;
import org.conjugateprior.ca.SimpleYoshikoderDocument;

public class CommandLineCategoryCounter extends CommandLineApplication {

	protected Locale tLocale = Locale.getDefault();
	protected Charset tEncoding = Charset.defaultCharset();
	protected File tOutputfile = null;
	protected boolean oldMatchStrategy = false;
	protected File[] filesToProcess;
	protected CategoryDictionary dict;
		
	@Override
	protected String getUsageString() {
		return "ykreporter [options] -dictionary <file.ykd> -output <file.csv> [doc1.txt doc2.txt folder1]";
	}
	
	public CommandLineCategoryCounter() {
		super();

		Option help = new Option("help", "Show this message, then exit");
		Option encoding = new Option("encoding", true, "Encoding for input files e.g. UTF8");
		encoding.setArgName("encoding name");
		Option oldMatching = new Option("oldmatching", "Use old-style Yoshikoder pattern matching");
		Option locale = new Option("locale",  true, "A locale for input files e.g. en_US");
		locale.setArgName("locale name");				
		// required
		Option dictionary = new Option("dictionary", true, "Content analysis dictionary from Yoshikoder");
		dictionary.setArgName("file");
		dictionary.setRequired(true);
		Option outputfile = new Option("output", true, "Specify a name for the output file e.g. output.csv");
		outputfile.setRequired(true);
		outputfile.setArgName("file");
		
		addCommandLineOption(help);
		addCommandLineOption(encoding);
		addCommandLineOption(locale);
		addCommandLineOption(dictionary);
		addCommandLineOption(oldMatching);
		addCommandLineOption(outputfile);		
	}
	
	
	@Override
	protected void processLine(CommandLine line) throws Exception {	
		if (line.hasOption("help")) {
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
		tOutputfile = new File(line.getOptionValue("output"));
		
		if (line.hasOption("dictionary")) {
			String fname = line.getOptionValue("dictionary");
			File sf = new File(fname);
			if (!sf.exists()){
				throw new Exception("Dictionary file cannot be found. Check path?");
			}
			try {
				dict = CategoryDictionary.readCategoryDictionaryFromFile(sf);
				
			} catch (Exception ex){
				throw new Exception("Dictionary file could not be parsed. Check this is" +
					"a valid Yoshikoder dictionary format?" + 
					"(Normal filename suffix is '.ykd')" +
					"Error message follows:" + ex.getMessage());
			}
		}	
		oldMatchStrategy = line.hasOption("oldmatching");
		
		tOutputfile = new File(line.getOptionValue("output"));
	
		String[] files = line.getArgs();
		if (files.length == 0)
			throw new Exception("No documents or folders of documents to process!");
		filesToProcess = getRecursiveDepthOneFileArray(files);	
		
		// all little verbosity never hurts
		System.err.println("Settings:");
		System.err.println("  Locale of input files: " + tLocale + " ie '" + 
				tLocale.getDisplayName() + "'");
		System.err.println("  Encoding of input files: " + tEncoding.name() + " ie '" +
				tEncoding.displayName() + "'");
		System.out.println("  Yoshikoder dictionary file: " + 
				line.getOptionValue("dictionary"));
		System.err.println("  Using old pattern matching strategy? " + oldMatchStrategy);
		System.err.println("  Output CSV file encoding: " + 
				(onWindows() ? "windows-1252 ('Latin 1')" : "UTF8"));
		System.err.println("  Output file line endings: " + 
				(onWindows() ? "\\r\\n (Windows style)" : "\\n (Unix style)"));
		System.err.println("And here come the files...");

		// and we're off
		FileOutputStream fout = new FileOutputStream(tOutputfile);
		CategoryReporter reporter = new CategoryReporter(dict);
		if (onWindows())
			reporter.setWindowsOutput(); // \r\n and Latin 1 (FFS...)

		SimpleDocumentTokenizer tok = new SimpleDocumentTokenizer(tLocale);
		
		IYoshikoderDocument doc = null;
		reporter.openStreamingReport(fout); // write out first line
		for (File file : filesToProcess) {
			try {
				String txt = SimpleYoshikoderDocument.getTextFromFile(file, tEncoding);
				String docTitle = file.getName();
				System.err.println("Processing " + docTitle);
				doc = new SimpleYoshikoderDocument(docTitle, txt, null, tok); // null date
				if (!oldMatchStrategy)
					reporter.streamReportLine(docTitle, reporter.reportOnDocument(doc));
				else 
					reporter.streamReportLine(docTitle, reporter.reportOnDocumentOldStyle(doc));
				
			} catch (Exception exc){
				System.err.println("Problem with " + doc.getTitle());
				System.err.println("Error message follows:");
				System.err.println(exc.getMessage());
				System.err.println("Carrying on without processing this document...");
			}
		}
		reporter.closeStreamingReport();
	}
	
	public static void main(String[] args) {
		CommandLineCategoryCounter rep = new CommandLineCategoryCounter();
		try {
			rep.process(args);
		} catch (Exception ex){
			System.err.println(ex.getMessage());
			rep.printUsageAndOptions();
			System.exit(0);
		}
	}	
	
}
