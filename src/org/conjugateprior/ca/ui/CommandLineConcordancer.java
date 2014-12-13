package org.conjugateprior.ca.ui;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.lang3.StringEscapeUtils;
import org.conjugateprior.ca.AbstractYoshikoderDocument;
import org.conjugateprior.ca.IPatternEngine;
import org.conjugateprior.ca.IYoshikoderDocument;
import org.conjugateprior.ca.SimpleDocumentTokenizer;
import org.conjugateprior.ca.SimpleYoshikoderDocument;
import org.conjugateprior.ca.SubstringPatternEngine;

// TODO add formats for output
public class CommandLineConcordancer extends CommandLineApplication {

	protected Locale tLocale = Locale.getDefault();
	protected Charset tEncoding = Charset.defaultCharset();
	protected File tOutputfile = null;	
	protected File[] filesToProcess;
	protected Pattern[] regexp;
	protected int window = 5;
	
	protected enum OutputFormat {
		TEXT, UTF8, LATEX, HTML
	};
	protected OutputFormat outputFormat;
	
	public CommandLineConcordancer() {
		super();

		Option help = new Option("help", "Show this message, then exit");
		Option encoding = new Option("encoding", true, 
				"Input file character encoding (default: " + 
				Charset.defaultCharset().name() + ")");
		encoding.setArgName("encoding");
		
		Option locale = new Option("locale",  true, 
				"Locale for input files (default: " + 
		Locale.getDefault().toString() + ")");
		locale.setArgName("locale");				
		
		Option pattern = new Option("pattern", true, 
				"Word or phrase to match (wildcards allowed)");
		pattern.setRequired(true);
		pattern.setArgName("pattern");
		
		Option format = new Option("format", true, 
				"One of: text (the default), utf8, html, latex");
		format.setArgName("format");
		
		Option window = new Option("window", true, 
				"Number of words either side (default: 5)");
		window.setArgName("number of words");
		
		Option outputfile = new Option("output", true, 
				"Name of output file (default: stdout)");
		outputfile.setArgName("output");
		
		addCommandLineOption(help);
		addCommandLineOption(encoding);
		addCommandLineOption(locale);
		addCommandLineOption(pattern);
		addCommandLineOption(window);
		addCommandLineOption(format);
		addCommandLineOption(outputfile);
	}
	
	
	@Override
	protected String getUsageString() {
		return "ykconcordancer -pattern <pattern> [-locale <locale>] " +
	           "[-encoding <encoding>] [-window <number>] [-output <file>] " +
			   "[-format <format>] [file1 file2 | folder1]";
	}
	
	private String padString(String str, int len){
		StringBuilder sb = new StringBuilder();
		for (int toPrepend=len-str.length(); toPrepend>0; toPrepend--) {
		    sb.append(' ');
		}
		sb.append(str);
		return sb.toString();
	}
	
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

		if (line.hasOption("window")){
			try {
				window = Integer.parseInt(line.getOptionValue("window"));
			} catch (Exception ex){
				//default to 10
			}
		}
		
		String[] spl = line.getOptionValue("pattern").split("[ ]+");
		
		IPatternEngine patternEngine = new SubstringPatternEngine();
		regexp = patternEngine.makeRegexp(spl);
		
		String[] files = line.getArgs();
		if (files.length == 0)
			throw new Exception("No documents or folders of documents to process!");
		filesToProcess = getRecursiveDepthOneFileArray(files);	
		//System.err.println(filesToProcess.length);
		
		if (line.hasOption("format")){
			String s = line.getOptionValue("format").toLowerCase();
			if (s.equals("default"))
				outputFormat = OutputFormat.TEXT;
			else if (s.equals("latex"))
				outputFormat = OutputFormat.LATEX;
			else if (s.equals("html"))
				outputFormat = OutputFormat.HTML;
			else if (s.equals("utf8") || s.equals("utf-8"))
				outputFormat = OutputFormat.UTF8;
			else {
				System.err.println("Unrecognized format. Setting format to: default");
				System.err.println("That is, plain text encoded in your machine's default local character encoding");
				System.err.println("(which is " + Charset.defaultCharset().displayName() + ")");
				outputFormat = OutputFormat.TEXT;
			}
		} else {
			outputFormat = OutputFormat.TEXT;
		}

		BufferedWriter writer = null;
		if (line.hasOption("output")){
			String f = line.getOptionValue("output");
			OutputStreamWriter out = null;
			if (outputFormat == OutputFormat.HTML || outputFormat == OutputFormat.UTF8){
				out = new OutputStreamWriter(new FileOutputStream(new File(f)), 
						Charset.forName("UTF-8"));
			} else {
				out = new OutputStreamWriter(new FileOutputStream(new File(f)), 
						Charset.defaultCharset());				
			}
			writer = new BufferedWriter(out);
		} else {
			OutputStreamWriter out = null;
			if (outputFormat == OutputFormat.HTML || outputFormat == OutputFormat.UTF8){
				out = new OutputStreamWriter(System.out, 
						Charset.forName("UTF-8"));
			} else {
				out = new OutputStreamWriter(System.out, 
						Charset.defaultCharset());				
			}
			writer = new BufferedWriter(out);
		}
				
		SimpleDocumentTokenizer tokenizer = new SimpleDocumentTokenizer(tLocale);
		
		if (outputFormat == OutputFormat.HTML) {
			
			writer.write("<!DOCTYPE html>\n<html>\n<head>\n<meta charset=\"UTF-8\">\n" + 
					"<style>\n#conctable .rightalign { text-align: right }\n" + 
					"#conctable .leftalign { text-align: left }\n</style>\n" + 
					"</head>\n<body>\n");
			writer.write("  <table id=\"conctable\">\n");
			writer.write("    <tr><th>Document</th><th></th><th class=\"leftalign\">Pattern</th></tr>\n");
			for (File f : filesToProcess) {
				IYoshikoderDocument idoc = 
						new SimpleYoshikoderDocument(f.getName(), 
							AbstractYoshikoderDocument.getTextFromFile(f, tEncoding),
							null, tokenizer);
				List<int[]> concs = 
					idoc.getConcordanceCharacterOffsetsForPattern(regexp, window);	
				dumpConcLinesHTML(writer, concs, idoc.getText(), idoc.getTitle());
			}
			writer.write("  </table>\n</body>\n</html>\n");
		} else if (outputFormat == OutputFormat.LATEX){
			
			writer.write("#\\usepackage{ctable}\n");
			writer.write("\\begin{tabular}{rr@{\\hskip 0.3em}l} \\toprule");
			writer.newLine();
			writer.write("Document & & Pattern \\\\ \\midrule");
			writer.newLine();
			for (File f : filesToProcess) {
				IYoshikoderDocument idoc = 
						new SimpleYoshikoderDocument(f.getName(), 
							AbstractYoshikoderDocument.getTextFromFile(f, tEncoding),
							null, tokenizer);
				List<int[]> concs = 
					idoc.getConcordanceCharacterOffsetsForPattern(regexp, window);	
				dumpConcLinesLATEX(writer, concs, idoc.getText(), idoc.getTitle());
			}
			writer.write("\\bottomrule");
			writer.newLine();
			writer.write("\\end{tabular}");
			writer.newLine();
		} else {
			// TEXT or UTF8 (encoding is set in the writer already)
			for (File f : filesToProcess) {
				IYoshikoderDocument idoc = 
						new SimpleYoshikoderDocument(f.getName(), 
							AbstractYoshikoderDocument.getTextFromFile(f, tEncoding),
							null, tokenizer);
				List<int[]> concs = 
					idoc.getConcordanceCharacterOffsetsForPattern(regexp, window);	
				writer.write("---- " + idoc.getTitle());
				writer.newLine();
				dumpConcLinesTEXT(writer, concs, idoc.getText(), idoc.getTitle());
			}
		}
		
		writer.flush();
	}
	
	protected void dumpConcLinesTEXT(BufferedWriter str, 
			List<int[]> indices, String txt, String fname)
		throws IOException {
		
		int maxlen = 0;
		for (int[] is : indices)
			maxlen = Math.max(maxlen, is[1]-is[0]);
		for (int[] is : indices) {
			String s = collapseWhitespace(txt.substring(is[0], is[1]));
			str.write(padString(s, maxlen));
			s = " [" + txt.substring(is[2], is[3]) + "]";
		    str.write(s);
		    // catch trailing punctuation etc. by restarting straight after match
		    int restart = (is[3]<txt.length() ? is[3] : is[4]);
			s = collapseWhitespace(txt.substring(restart, is[5]));
			str.write(s);
			str.newLine();
		}
	}
	
	protected String collapseWhitespace(String s){
		return s.replace("\n", " ").replace("\r",  " ");
	}
	
	// \# \$ \% \^{} \& \_ \{ \} \~{} \textbackslash{}
	// TODO make me complete!
	protected String escapeLatexSpecials(String s){
		return s.replace("\\", "\\textbackslash{}")
				.replace("%", "\\%").replace("$", "\\$")
				.replace("&", "\\&").replace("^", "\\^{}")
				.replace("{", "\\{").replace("}", "\\}")
				.replace("_",  "\\_");	
	}
	
	protected void dumpConcLinesLATEX(BufferedWriter str, 
			List<int[]> indices, String txt, String fname)
		throws IOException {
		
		boolean firstline = true;
		for (int[] is : indices) {
			str.write((firstline ? escapeLatexSpecials(fname) : "")); // the docment title
			firstline = false;
			String s = escapeLatexSpecials(collapseWhitespace(txt.substring(is[0], is[1])));
			str.write(" & " + s + " & ");
			String targ = escapeLatexSpecials(collapseWhitespace(txt.substring(is[2], is[3])));
			int restart = (is[3]<txt.length() ? is[3] : is[4]);
			s = escapeLatexSpecials(collapseWhitespace(txt.substring(restart, is[5])));
		    str.write("\\textbf{" + targ + "} " + s + "\\\\");
		    str.newLine();
		}
	}
	
	// push out UTF8
	protected void dumpConcLinesHTML(BufferedWriter str, 
			List<int[]> indices, String txt, String fname)
		throws IOException {
		boolean firstline = true;
		for (int[] is : indices) {
			str.write("    <tr><td>" + (firstline ? StringEscapeUtils.escapeHtml4(fname) : "") + "</td>");	
			firstline = false;
			String s = collapseWhitespace(txt.substring(is[0], is[1]));
			str.write("<td class=\"rightalign\">" + 
					StringEscapeUtils.escapeHtml4(s) + "</td>");
			String targ = collapseWhitespace(txt.substring(is[2], is[3]));
			int restart = (is[3]<txt.length() ? is[3] : is[4]);
			s = collapseWhitespace(txt.substring(restart, is[5]));
		    str.write("<td><strong>" + StringEscapeUtils.escapeHtml4(targ) + "</strong>" +
		    		StringEscapeUtils.escapeHtml4(s) + "</td></tr>\n");
		}
	}
	
	public static void main(String[] args) {
		CommandLineConcordancer rep = new CommandLineConcordancer();
		try {
			rep.process(args);
		} catch (Exception ex){
			System.err.println(ex.getMessage());
			rep.printUsageAndOptions();
			System.exit(0);
		}
	}
	
}
