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

public class CLCategoryCounter {

	protected CategoryCounter counter;
	
	protected Options options;
	protected HelpFormatter helpFormatter;
	
	public static String USAGE = "ykcats [-encoding <encoding>] [-locale <locale>] " +
	   "[-oldmatching] [-output <folder>] [-format <format>] -dictionary <file> " +
	   "[doc1.txt doc2.txt folder1]";
	
	public void printUsageAndOptions(){
		helpFormatter.printHelp(CLCategoryCounter.USAGE, options);
	}
	
	public CLCategoryCounter(CategoryCounter c) {
		counter = c;
		
		helpFormatter = new HelpFormatter();
		options = new Options();
		
		Option oHelp = new Option("help", "Show this message, then exit");
		Option oEncoding = new Option("encoding", true, 
				"Input file character encoding (default: " + 
				Charset.defaultCharset().displayName() + ")");
		oEncoding.setArgName("encoding");
		
		Option oOldMatching = new Option("oldmatching", 
				"Use old-style pattern matching");
		Option oLocale = new Option("locale",  true, 
				"Locale for input files (default: " + 
				Locale.getDefault().toString() + ")");
		oLocale.setArgName("locale");				
		// required
		Option oDictionary = new Option("dictionary", true, 
				"Content analysis dictionary in Yoshikoder ('.ykd'), " +
		        "Lexicoder ('.lcd'), Wordstat ('.CAT'), LIWC ('.dic'), or VBPro " + 
				"('.vbpro') format");
		oDictionary.setArgName("file");
		oDictionary.setRequired(true);
		
		Option oOutputfile = new Option("output", true, 
				"Name for an output file");
		oOutputfile.setArgName("file");
		
		Option oFormat = new Option("format", true, 
				"One of: utf8, text (default: text, which here means " + Charset.defaultCharset().name() + ")");
		oFormat.setArgName("format");
		
		options.addOption(oHelp);
		options.addOption(oEncoding);
		options.addOption(oOldMatching);
		options.addOption(oLocale);
		options.addOption(oDictionary);
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

		// we always have a dictionary 
		File dictionaryFile = new File(line.getOptionValue("dictionary"));
		counter.setDictionary(dictionaryFile);
		counter.setUsingOldMatchStrategy(line.hasOption("oldmatching"));

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
	}
	

	public static void main(String[] args) throws Exception {
		//ykcats -dictionary ~/Dropbox/blogposts/littledict.vbpro -output thing ~/Dropbox/blogposts/speeches
		String[] as = new String[]{"-dictionary", "/Users/will/Dropbox/blogposts/littledict.vbpro",
									/*"-output", "/Users/will/Dropbox/blogposts/ykcatesoutputfolder",*/ 
									"-format", "html", "/Users/will/Dropbox/blogposts/speeches"};
		
		CategoryCounter cc = new CategoryCounter();
		CLCategoryCounter c = new CLCategoryCounter(cc);
		try {
			c.processLine(as);
		} catch (Exception ex){
			System.err.println(ex.getMessage());
			c.printUsageAndOptions();
		}
	}
}

/*
if (tOutputfile == null){			
	SimpleDocumentTokenizer tokenizer = new SimpleDocumentTokenizer(tLocale);
	// push out in default local encoding
	for (TreeItem<DCat> titem : categoryNodesInPrintOrder) 
		System.out.print("," + StringEscapeUtils.escapeCsv(DCat.getPathAsString(titem, ">")));
	System.out.println("," + 
		CSVFXCategoryDictionaryCountPrinter.wordCountHeader);

	for (File f : filesToProcess) {
		IYoshikoderDocument idoc = 
				new SimpleYoshikoderDocument(f.getName(), 
					AbstractYoshikoderDocument.getTextFromFile(f, tEncoding),
					null, tokenizer);
		String docline = makeLineFromDocument(dict, idoc, oldMatchStrategy);
		System.out.println(docline);
	}
} else {
	CountPrinter printer = null;
	if (oldMatchStrategy){
		printer = new CSVOldStyleCategoryDictionaryCountPrinter(dict, 
				tOutputfile, "data.csv", filesToProcess, tEncoding, tLocale);
	} else {
		printer = new CSVFXCategoryDictionaryCountPrinter(dict, 
				tOutputfile, "data.csv", filesToProcess, tEncoding, tLocale);
	}
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
					new FileOutputStream(rme, true), 
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
*/

