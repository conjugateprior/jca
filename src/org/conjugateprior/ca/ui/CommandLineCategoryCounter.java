package org.conjugateprior.ca.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.Locale;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.conjugateprior.ca.FXCategoryDictionary;
import org.conjugateprior.ca.reports.CSVFXCategoryDictionaryCountPrinter;
import org.conjugateprior.ca.reports.CSVOldStyleCategoryDictionaryCountPrinter;
import org.conjugateprior.ca.reports.CountPrinter;

public class CommandLineCategoryCounter extends CommandLineApplication {

	protected Locale tLocale = Locale.getDefault();
	protected Charset tEncoding = Charset.defaultCharset();
	protected File tOutputfile = null;
	protected boolean oldMatchStrategy = false;
	protected File[] filesToProcess;
	
	protected FXCategoryDictionary dict;
		
	@Override
	protected String getUsageString() {
		return "ykreporter [options] -dictionary <dictfile> -output <folder> [doc1.txt doc2.txt folder1]";
	}
	
	public CommandLineCategoryCounter() {
		super();

		Option help = new Option("help", "Show this message, then exit");
		Option encoding = new Option("encoding", true, "Encoding for input files (default: " + 
				Charset.defaultCharset().displayName() + ")");
		encoding.setArgName("encoding name");
		Option oldMatching = new Option("oldmatching", "Use old-style Yoshikoder pattern matching");
		Option locale = new Option("locale",  true, "A locale for input files (default: " + 
				Locale.getDefault().toString() + ")");
		locale.setArgName("locale name");				
		// required
		Option dictionary = new Option("dictionary", true, "Content analysis dictionary in Yoshikoder, Lexicoder, or VBPro format");
		dictionary.setArgName("file");
		dictionary.setRequired(true);
		
		Option outputfile = new Option("output", true, "Specify a name for the output folder");
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
				throw new Exception("Dictionary file cannot be found at"
						+ sf.getAbsolutePath());
			}
			
			if (fname.toLowerCase().endsWith(".ykd") || 
				fname.toLowerCase().endsWith(".lcd")){
				dict = FXCategoryDictionary.readXmlCategoryDictionaryFromFile(sf); 	
			
			} else if (fname.toLowerCase().endsWith(".vbpro")){
				dict = FXCategoryDictionary.importCategoryDictionaryFromFileVBPRO(sf); 
			
			} else if (fname.toLowerCase().endsWith(".cat")){
				dict = FXCategoryDictionary.importCategoryDictionaryFromFileWordstat(sf);
			
			} else if (fname.toLowerCase().endsWith(".dic")){
				dict = FXCategoryDictionary.importCategoryDictionaryFromFileLIWC(sf);
			
			} else if (fname.toLowerCase().endsWith(".xml")) {
				// windows or server .xml addition?
				dict = FXCategoryDictionary.readXmlCategoryDictionaryFromFile(sf); 
					
			} else {
				throw new Exception(
						"Dictionary file format could not be identified.\n" +
					    "It must be a Yoshikoder ('.ykd'), Lexicoder ('.lcd'), " +
					    "Wordstat ('.CAT'), LIWC (.dic), or VBPro ('.vbpro') file\n");
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
		System.out.println("  Dictionary file: " + 
				line.getOptionValue("dictionary"));
		System.err.println("  Using old pattern matching strategy? " + oldMatchStrategy);
		//System.err.println("  Output CSV file encoding: " + 
		//		(onWindows() ? "windows-1252 ('Latin 1')" : "UTF8"));
		//System.err.println("  Output file line endings: " + 
		//		(onWindows() ? "\\r\\n (Windows style)" : "\\n (Unix style)"));

		CountPrinter printer = null;
		if (oldMatchStrategy){
			printer = new CSVOldStyleCategoryDictionaryCountPrinter(dict, 
				tOutputfile, "data.csv", filesToProcess, tEncoding, tLocale);
		} else {
			printer = new CSVFXCategoryDictionaryCountPrinter(dict, 
				tOutputfile, "data.csv", filesToProcess, tEncoding, tLocale);
		}
		/*
		if (onWindows())
			printer.setWindowsOutput(); // \r\n and Latin 1 (FFS...)
		*/
		final float maxProg = (float)printer.getMaxProgress();
		final DecimalFormat df = new DecimalFormat("#.##");
		PropertyChangeListener listener = new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if ("progress".equals(evt.getPropertyName())){
					int prog = (Integer)evt.getNewValue();
					System.err.println(df.format((prog / maxProg) * 100) + "% complete");
				}
			}
		};
		printer.addPropertyChangeListener(listener);		
		printer.processFiles(false);
		
		String newl = printer.getNewline();
		File rme = new File(tOutputfile, printer.getReadmefilename());
		try (
				OutputStreamWriter out = new OutputStreamWriter(
					new FileOutputStream(rme, true), /* appending */
						printer.getOutputCharset());
				BufferedWriter writer = new BufferedWriter(out);
			){
				writer.write(newl);
			writer.write("Settings:");
			writer.write(newl + newl);
			writer.write("File enc:\t" + printer.getOutputCharset());
			writer.write(newl);
			//writer.write("Output enc:\t" + 
			//		(onWindows() ? "windows-1252 ('Latin 1')" : "UTF-8"));
			//writer.write(newl);
			writer.write("Dict:\t" + line.getOptionValue("dictionary") + " (source file)");
			writer.write(newl);
			writer.write("Matching:\t" + (oldMatchStrategy ? "old" : "new"));
			writer.write(newl);
			writer.write("Line endings:\t \\n (Unix style)");
			//writer.write(newl);
			
		}
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
