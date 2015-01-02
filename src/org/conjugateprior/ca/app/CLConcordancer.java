package org.conjugateprior.ca.app;

import java.io.File;
import java.nio.charset.Charset;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class CLConcordancer {

	protected Concordancer concordancer;
	
	protected Options options;
	protected HelpFormatter helpFormatter;
	
	public static String USAGE = 
			"ykconc -pattern <pattern> | -dictionary <file> -category <category> " + 
					"[-locale <locale>] " +
					"[-encoding <encoding>] [-window <number>] [-output <file>] " +
					"[-format <format>] [file1 file2 | folder1]";
	
	public void printUsageAndOptions(){
		helpFormatter.printHelp(USAGE, options);
	}
	
	public CLConcordancer(Concordancer c) {
		concordancer = c;
		
		helpFormatter = new HelpFormatter();
		options = new Options();
		
		Option help = new Option("help", "Show this message, then exit");
		Option encoding = new Option("encoding", true, 
				"Input file character encoding (default: " + 
				concordancer.getEncoding().name() + ")");
		encoding.setArgName("encoding");
		
		Option locale = new Option("locale",  true, 
				"Locale for input files (default: " + 
		concordancer.getLocale().toString() + ")");
		locale.setArgName("locale");				
		
		Option pattern = new Option("pattern", true, 
				"Word or phrase to match (wildcards allowed)");
		pattern.setRequired(false);
		pattern.setArgName("pattern");
		
		Option format = new Option("format", true, 
				"One of: text (the default), utf8, html");
		format.setArgName("format");
		
		Option window = new Option("window", true, 
				"Number of words either side (default: 5)");
		window.setArgName("number");
		
		Option outputfile = new Option("output", true, 
				"Name of output file (default: stdout)");
		outputfile.setArgName("output");
		
		Option dict = new Option("dictionary", true, 
				"Content analysis dictionary");
		dict.setArgName("file");
		
		Option dictcat = new Option("category", true, 
				"Category from content analysis dictionary");
		dictcat.setArgName("category");
				
		options.addOption(help);
		options.addOption(encoding);
		options.addOption(locale);
		options.addOption(pattern);
		options.addOption(window);
		options.addOption(format);
		options.addOption(outputfile);
		options.addOption(dict);
		options.addOption(dictcat);
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
				concordancer.setLocale(line.getOptionValue("locale"));
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
				concordancer.setEncoding(line.getOptionValue("encoding"));
			} catch (Exception ex){
				throw new Exception("Could not parse file encoding. Error message follows:\n" +
						ex.getMessage());
			}
		} 		

		if (line.hasOption("window")){
			int window = Integer.parseInt(line.getOptionValue("window"));
			if (window < 1)
				throw new Exception("Window size must be greater than 0");
			concordancer.setWindow(window);
		}
		
		if (line.hasOption("dictionary") && line.hasOption("pattern"))
			throw new Exception("Use dictionary and category arguments or use pattern argument, but not both");
		if (!(line.hasOption("dictionary") || line.hasOption("pattern")))
			throw new Exception("Missing either dictionary and category arguments, or pattern argument");
		
		if (line.hasOption("dictionary")) {
			File dictionaryFile = new File(line.getOptionValue("dictionary"));
			concordancer.setDictionary(dictionaryFile);
				
			String cat = null;
			if (line.hasOption("category")){
				cat = line.getOptionValue("category");
				concordancer.setCategory(cat);
			} else 
				throw new Exception("No category specified for the dictionary ");
		} else if (line.hasOption("pattern"))
			concordancer.addPattern(line.getOptionValue("pattern"));

		// the CL interface conflates style and encoding - here we deconflate
		if (line.hasOption("format")){
			String ff = line.getOptionValue("format");
			if (ff.toLowerCase().equals("utf-8") || ff.toLowerCase().equals("utf8"))
				concordancer.setOutputEncoding(Charset.forName("UTF-8"));
			else {
				concordancer.setFormat(ff);
			}
		}
		
		if (line.hasOption("output"))
			concordancer.setOutputFolder(line.getOptionValue("output"));
		
		String[] files = line.getArgs();
		concordancer.setFiles(files);
		if (files.length == 0)
			throw new Exception("No documents or folders of documents to process!");
		
		concordancer.processFiles();
	}
	
	public static void main(String[] args) {
		//ykcats -dictionary ~/Dropbox/blogposts/littledict.vbpro -output thing ~/Dropbox/blogposts/speeches
		String[] as = new String[]{
				/*"-dictionary", "/Users/will/Dropbox/blogposts/littledict.vbpro",
				"-category", "mine", */ "-pattern", "low income families", "-window", "0",
									/*"-output", "/Users/will/Dropbox/blogposts/ykoncsoutputfolder",*/
									"-format", "text", "/Users/will/Dropbox/blogposts/speeches"};
		
		Concordancer cc = new Concordancer();
		CLConcordancer c = new CLConcordancer(cc);
		try {
			c.processLine(as);
		} catch (Exception ex){
			System.err.println(ex.getMessage());
			c.printUsageAndOptions();
		}
	}
	
}
