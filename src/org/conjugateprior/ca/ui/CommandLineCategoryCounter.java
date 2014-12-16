package org.conjugateprior.ca.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javafx.scene.control.TreeItem;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.lang3.StringEscapeUtils;
import org.conjugateprior.ca.AbstractYoshikoderDocument;
import org.conjugateprior.ca.DCat;
import org.conjugateprior.ca.DPat;
import org.conjugateprior.ca.FXCategoryDictionary;
import org.conjugateprior.ca.IYoshikoderDocument;
import org.conjugateprior.ca.SimpleDocumentTokenizer;
import org.conjugateprior.ca.SimpleYoshikoderDocument;
import org.conjugateprior.ca.reports.CSVFXCategoryDictionaryCountPrinter;
import org.conjugateprior.ca.reports.CSVOldStyleCategoryDictionaryCountPrinter;
import org.conjugateprior.ca.reports.CountPrinter;

public class CommandLineCategoryCounter extends CommandLineApplication {

	protected Locale tLocale;
	protected Charset tEncoding;
	protected File tOutputfile = null;
	protected boolean oldMatchStrategy = false;
	protected File[] filesToProcess;
	
	protected FXCategoryDictionary dict;
	protected List<TreeItem<DCat>> categoryNodesInPrintOrder;	
	
	@Override
	protected String getUsageString() {
		return "ykcats [-encoding <encoding>] [-locale <locale>] " +
			   "[-oldmatching] [-output <folder>] -dictionary <file> " +
			   "[doc1.txt doc2.txt folder1]";
	}
	
	public CommandLineCategoryCounter() {
		super();

		Option help = new Option("help", "Show this message, then exit");
		Option encoding = new Option("encoding", true, 
				"Input file character encoding (default: " + 
				Charset.defaultCharset().displayName() + ")");
		encoding.setArgName("encoding");
		Option oldMatching = new Option("oldmatching", 
				"Use old-style pattern matching");
		Option locale = new Option("locale",  true, 
				"Locale for input files (default: " + 
				Locale.getDefault().toString() + ")");
		locale.setArgName("locale");				
		// required
		Option dictionary = new Option("dictionary", true, 
				"Content analysis dictionary in Yoshikoder ('.ykd'), " +
		        "Lexicoder ('.lcd'), Wordstat ('.CAT'), LIWC ('.dic'), or VBPro " + 
				"('.vbpro') format");
		dictionary.setArgName("file");
		dictionary.setRequired(true);
		
		Option outputfile = new Option("output", true, 
				"Name for an output folder");
		outputfile.setArgName("folder");
		
		addCommandLineOption(help);
		addCommandLineOption(encoding);
		addCommandLineOption(locale);
		addCommandLineOption(dictionary);
		addCommandLineOption(oldMatching);
		
		addCommandLineOption(outputfile);		
	}
	
	private void recurseCategories(List<TreeItem<DCat>> sb, TreeItem<DCat> n){
		sb.add(n);
		for (TreeItem<DCat> treeItem : n.getChildren()) {
			recurseCategories(sb, treeItem);
		}
	}
	
	protected List<TreeItem<DCat>> getCategoryNodesInPrintOrder(FXCategoryDictionary d){
		TreeItem<DCat> n = d.getCategoryRoot();
		List<TreeItem<DCat>> list = new ArrayList<TreeItem<DCat>>();
		recurseCategories(list, n);
		return list;
	}	

	// this time with match counts not the indices
	protected void fillTreeWithMatchCounts(IYoshikoderDocument doc){
		// optimise later
		//Set<String> vocab = doc.getWordTypes();
		for (TreeItem<DCat> node : categoryNodesInPrintOrder){
			Set<Integer> indexMatches = new HashSet<Integer>();
			Set<DPat> pats = node.getValue().getPatterns();
			int rawCount = 0; // including double counted items!
			for (DPat pat : pats) {
				Set<Integer> indices = 
						doc.getAllMatchingTokenIndexesForPattern(pat.getRegexps());
				rawCount += indices.size();
			}
			indexMatches.add(new Integer(rawCount)); // here one number, the total count
			node.getValue().setMatchedIndices(indexMatches);
		}
		// percolate
		for (TreeItem<DCat> node : categoryNodesInPrintOrder){
			if (node.isLeaf()){
				TreeItem<DCat> current = node;
				TreeItem<DCat> parent = node.getParent();
				while (parent != null){
					// (mis)use the indices to hold and update single counts 
					Set<Integer> count = current.getValue().getMatchedIndices();
					Integer co = count.iterator().next();
					Set<Integer> pcount = parent.getValue().getMatchedIndices();
					Integer pco = pcount.iterator().next();
					pcount.clear();
					pcount.add(new Integer(pco + co));

					current = parent;
					parent = current.getParent();
				}
			}
		}
	}

	// new style and old style matching
	protected String makeLineFromDocument(FXCategoryDictionary dict, IYoshikoderDocument doc, boolean oldStyle){
		if (!oldStyle)
			fillTreeWithIndices(doc);
		else
			fillTreeWithMatchCounts(doc);
		
		StringBuilder sb = new StringBuilder();
		sb.append(StringEscapeUtils.escapeCsv(doc.getTitle()));
		for (TreeItem<DCat> cn : categoryNodesInPrintOrder) {
			sb.append(",");
			sb.append(cn.getValue().getMatchedIndices().size());
		}
		sb.append(",");
		sb.append(doc.getDocumentLength()); // no newline
		return sb.toString();
	}

	// this is the new style match counting where we never double count
	// multiple patterns that match (overlapping slices of) the same tokens
	protected void fillTreeWithIndices(IYoshikoderDocument doc){
		// optimise later
		//Set<String> vocab = doc.getWordTypes();
		for (TreeItem<DCat> node : categoryNodesInPrintOrder){
			//System.err.println("filling " + node);

			Set<Integer> indexMatches = new HashSet<Integer>();
			Set<DPat> pats = node.getValue().getPatterns();
			for (DPat pat : pats) {
				Set<Integer> indices = 
						doc.getAllMatchingTokenIndexesForPattern(pat.getRegexps());
				//System.err.println(pat.getName() + " - indices matched: " + indices);
				indexMatches.addAll(indices);
			}
			node.getValue().setMatchedIndices(indexMatches);
		}
		// percolate
		for (TreeItem<DCat> node : categoryNodesInPrintOrder){
			if (node.isLeaf()){
				TreeItem<DCat> current = node;
				TreeItem<DCat> parent = node.getParent();
				while (parent != null){
					parent.getValue().getMatchedIndices().addAll(current.getValue().getMatchedIndices());
					current = parent;
					parent = current.getParent();
				}
			}
		}
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
		
		if (line.hasOption("output"))
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
			// for printing out
			categoryNodesInPrintOrder = getCategoryNodesInPrintOrder(dict);
		}
		oldMatchStrategy = line.hasOption("oldmatching");
	
		String[] files = line.getArgs();
		if (files.length == 0)
			throw new Exception("No documents or folders of documents to process!");
		filesToProcess = getRecursiveDepthOneFileArray(files);	
		
		if (tOutputfile == null){			
			// TODO check the dictionary does not already specify one of these
			SimpleDocumentTokenizer tokenizer = new SimpleDocumentTokenizer(tLocale);
			// push out in default local encoding
			for (TreeItem<DCat> titem : categoryNodesInPrintOrder) 
				System.out.print("," + StringEscapeUtils.escapeCsv(titem.getValue().getName()));
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
