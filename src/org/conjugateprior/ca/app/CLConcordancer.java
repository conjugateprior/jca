package org.conjugateprior.ca.app;

import java.nio.charset.Charset;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;

public class CLConcordancer extends CLApplication{

	protected Concordancer concordancer;
	
	public String getUsage(){
		return "conc -pattern <pattern> | -dictionary <file> -category <category> " + 
				"[-locale <locale>] " +
				"[-encoding <encoding>] [-window <number>] [-output <file>] " +
				"[-format <format>] [file1 file2 | folder1]";
	}
			
	public CLConcordancer(Concordancer c) {
		super();
		
		concordancer = c;
		
		addOption(getHelpOption(false));
		addOption(getEncodingOption(false));
		addOption(getLocaleOption(false));
		addOption(getPatternOption(false));
		addOption(getWindowOption(false));
		addOption(getOuputFileOption(false)); // not folder
		addOption(getDictionaryOption(false));
		addOption(getCategoryOption(false));
	
		CLOption format = new CLOption("format", true, 
				"One of: text (the default), utf8, html");
		format.setArgName("format");
		// required == false
		addOption(format);
	}
	
	@Override
	public void processLine(String[] args) throws Exception {	
		CommandLineParser parser = new GnuParser();
		CommandLine line = parser.parse(options, args);
		
		if (line.hasOption("help")){	
			printUsageAndOptions();	
			System.exit(0);
		}
		if (line.hasOption("locale")){
			try {
				concordancer.setLocale(line.getOptionValue("locale"));
			} catch (Exception ex){
				throw new Exception(getOptionErrorMessage("locale"));
			}
		}
		if (line.hasOption("encoding")){ // if not we get the default charset
			try {
				concordancer.setEncoding(line.getOptionValue("encoding"));
			} catch (Exception ex){
				throw new Exception(getOptionErrorMessage("encoding"));
			}
		}
		if (line.hasOption("window")){
			int window = Integer.parseInt(line.getOptionValue("window"));
			System.err.println(line.getOptionValue("window"));
			if (window < 1)
				throw new Exception(getOptionErrorMessage("window"));
			concordancer.setWindow(window);
		} else {
			concordancer.setWindow(5);
		}
		if (line.hasOption("dictionary") && line.hasOption("pattern"))
			throw new Exception("Use dictionary and category arguments or use pattern argument, but not both");
		if (!(line.hasOption("dictionary") || line.hasOption("pattern")))
			throw new Exception("Missing either dictionary and category arguments, or pattern argument");
		if (line.hasOption("dictionary")) {	
			concordancer.setDictionary(line.getOptionValue("dictionary"));				
			String cat = null;
			if (line.hasOption("category")){
				cat = line.getOptionValue("category");
				concordancer.setCategory(cat);
				
			} else { 
				// TODO get every pattern?
				concordancer.setCategory(concordancer.getDictionary().getCategoryRoot());
				//throw new Exception("No category specified for the dictionary ");
			}
		} else if (line.hasOption("pattern"))
			concordancer.addPattern(line.getOptionValue("pattern"));

		// the CL interface conflates style and encoding - here we deconflate
		if (line.hasOption("format")){
			String ff = line.getOptionValue("format");
			if (ff.toLowerCase().equals("utf-8") || ff.toLowerCase().equals("utf8"))
				concordancer.setOutputEncoding(Charset.forName("UTF-8"));
			else 
				concordancer.setFormat(ff);
		}
		
		if (line.hasOption("output"))
			concordancer.setOutputFolder(line.getOptionValue("output"));
		
		String[] files = line.getArgs();
		concordancer.setFiles(files);
		if (files.length == 0)
			throw new Exception("No documents or folders of documents to process!");
	
		concordancer.processFiles();	
		System.exit(0);
	}
	
	/*
	public static void main(String[] args) {	
		Concordancer cc = new Concordancer();
		CLConcordancer c = new CLConcordancer(cc);
		try { 
			c.processLine(args);
		} catch (Exception ex){
			ex.printStackTrace();
			c.printUsageAndOptions();
		}
	}
	*/
	
}

// //ykcats -dictionary ~/Dropbox/blogposts/littledict.vbpro -output thing ~/Dropbox/blogposts/speeches
// String[] as = new String[]{
//		/*"-dictionary", "/Users/will/Dropbox/blogposts/littledict.vbpro",
//		"-category", "mine", */ "-pattern", "low income families", "-window", "0",
//							/*"-output", "/Users/will/Dropbox/blogposts/ykoncsoutputfolder",*/
//							"-format", "text", "/Users/will/Dropbox/blogposts/speeches"};
