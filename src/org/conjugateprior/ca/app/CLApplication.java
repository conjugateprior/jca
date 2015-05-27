package org.conjugateprior.ca.app;

import java.nio.charset.Charset;
import java.util.Locale;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class CLApplication {

	public class CLOption extends Option {

		private static final long serialVersionUID = 1L;
		String errorMessage;

		public CLOption(String opt, String description) {
			super(opt, description);
		}

		public CLOption(String opt, boolean hasArg, String description) {
			super(opt, hasArg, description);
		}

		public CLOption(String opt, String longOpt, boolean hasArg, String description) {
			super(opt, longOpt, hasArg, description);
		}

		public void setErrorMessage(String errorMessage) {
			this.errorMessage = errorMessage;
		}

		public String getErrorMessage(){
			return errorMessage;
		}

	}

	protected Options options;
	protected HelpFormatter helpFormatter;
	
	public CLApplication() {
		options = new Options();
		helpFormatter = new HelpFormatter();
	}

	public void processLine(String[] line) throws Exception { 
		System.err.println("Error. Unimplemented processLine in CLApplication");
		System.err.println("Arguments:");
		for (int ii = 0; ii < line.length; ii++)
			System.err.println("\t" +line[ii]);
	}
	
	public void printUsageAndOptions(){
		helpFormatter.printHelp(getUsage(), options);
	}
	
	public String getUsage(){
		return "";
	}
	
	public void addOption(Option o){
		options.addOption(o);
	}
	
	public Options getOptions(){
		return options;
	}
	
	public HelpFormatter getHelpFormatter(){
		return helpFormatter;
	}
	
	protected CLOption getHelpOption(boolean required){
		CLOption clOpt = new CLOption("help", "Show this message, then exit");
		clOpt.setRequired(required);
		return clOpt;
	}
	
	protected CLOption getRegexpOption(boolean required){
		CLOption encoding = new CLOption("regexp", true, 
				"Use regexp to tokenize (default: not used)");
		encoding.setArgName("regexp");
		encoding.setErrorMessage("Could not work with this regexp");
		encoding.setRequired(required);
		return encoding;
	}
	
	protected CLOption getEncodingOption(boolean required){
		CLOption encoding = new CLOption("encoding", true, 
				"Input file character encoding (default: " + 
				Charset.defaultCharset().name() + ")");
		encoding.setArgName("encoding");
		encoding.setErrorMessage("Could not parse file encoding name");
		encoding.setRequired(required);
		return encoding;
	}
	
	protected CLOption getLocaleOption(boolean required){
		CLOption locale = new CLOption("locale",  true, 
				"Locale for input files (default: " + 
		Locale.getDefault().toString() + ")");
		locale.setArgName("locale");	
		locale.setErrorMessage("Could not parse locale argument.\n" + 
        "A valid locale consists of a two letter language codes from ISO 639\n" +
		"optionally connected by an underscore to a two letter country code\n" +
        "from ISO 3166.  See also http://en.wikipedia.org/wiki/BCP_47");
		locale.setRequired(required);
		return locale;
		
	}
	
	protected CLOption getSilentOption(){
		CLOption clOpt = new CLOption("silent", 
				"Switch off progress dots. Without flag, dots are shown)");
		return clOpt;
	}
	
	
	protected CLOption getDictionaryOption(boolean required){
		CLOption dict = new CLOption("dictionary", true, 
				"Content analysis dictionary in Yoshikoder ('.ykd'), " +
		        "Lexicoder ('.lcd'), Wordstat ('.CAT'), LIWC ('.dic'), or VBPro " + 
				"('.vbpro') format");
		dict.setArgName("file");
		dict.setRequired(required);
		return dict;
	}
	
	protected CLOption getPatternOption(boolean required){
		CLOption pattern = new CLOption("pattern", true, 
				"Word or phrase to match (wildcards allowed)");
		pattern.setArgName("pattern");
		pattern.setRequired(required);
		return pattern;
	}
	
	protected CLOption getWindowOption(boolean required){
		CLOption window = new CLOption("window", true, 
				"Number of words either side (default: 5)");
		window.setArgName("number");
		window.setErrorMessage("Window argument must be an non-zero integer");
		window.setRequired(required);
		return window;
	}
	
	protected CLOption getOuputFileOption(boolean required){
		CLOption outputfile = new CLOption("output", true, 
				"Name of output file (default: stdout)");
		outputfile.setArgName("filename");
		outputfile.setRequired(required);
		return outputfile;
	}
	
	protected CLOption getOutputFolderOption(boolean required){
		CLOption outputfile = new CLOption("output", true, 
				"Name of output file (default: stdout)");
		outputfile.setArgName("foldername");
		outputfile.setRequired(required);
		return outputfile;
	}
	
	protected CLOption getCategoryOption(boolean required){
		CLOption dictcat = new CLOption("category", true, 
				"Category from content analysis dictionary");
		dictcat.setArgName("category");
		dictcat.setRequired(required);
		dictcat.setErrorMessage("Could not locate this category in the dictionary");
		return dictcat;
	}
	
	protected String getOptionErrorMessage(String optionName){
		return ((CLOption)options.getOption(optionName)).errorMessage;
	}

	/**
	 * args[0] determines which CL app to run, which parses and acts on the remaining
	 * @param args
	 */
	public static void main(String[] args) {
		String[] ar = null;
		if (args.length < 2){
			System.err.println("Usage: yk word|cat|conc|desc|line [options] [files]");
			System.exit(0);
		} else {
			ar = new String[args.length-1];
			for (int ii = 1; ii < args.length; ii++) 
				ar[ii-1] = args[ii];
		}
		
		if (args[0].equalsIgnoreCase("cat")){
			CategoryCounter cc = new CategoryCounter();
			CLCategoryCounter c = new CLCategoryCounter(cc);
			try {
				c.processLine(ar);
			} catch (Exception ex){
				System.err.println(ex.getMessage());
				c.printUsageAndOptions();
			}
		} else if (args[0].equalsIgnoreCase("conc")){
			Concordancer cc = new Concordancer();
			CLConcordancer c = new CLConcordancer(cc);
			try {
				c.processLine(ar);
			} catch (Exception ex){
				System.err.println(ex.getMessage());
				c.printUsageAndOptions();
			}
		} else if (args[0].equalsIgnoreCase("word")){
			WordCounter cc = new WordCounter();
			CLWordCounter c = new CLWordCounter(cc);
			try {
				c.processLine(ar);
			} catch (Exception ex){
				System.err.println(ex.getMessage());
				c.printUsageAndOptions();
			}
		} else if (args[0].equalsIgnoreCase("desc")){
			Description cc = new Description();
			CLDescription c = new CLDescription(cc);
			try {
				c.processLine(ar);
			} catch (Exception ex){
				System.err.println(ex.getMessage());
				c.printUsageAndOptions();
			}
		} else if (args[0].equalsIgnoreCase("line")){
			SingleLiner cc = new SingleLiner();
			CLSingleLiner c = new CLSingleLiner(cc);
			try {
				c.processLine(ar);
			} catch (Exception ex){
				System.err.println(ex.getMessage());
				c.printUsageAndOptions();
			}
		} else {
			System.err.println("Usage: jca word|cat|conc|desc|line [options] [files]");
		}
		System.exit(0);
	}
	
}
