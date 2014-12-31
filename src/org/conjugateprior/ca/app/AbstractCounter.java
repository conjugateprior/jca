package org.conjugateprior.ca.app;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import javafx.scene.control.TreeItem;

import org.conjugateprior.ca.DCat;
import org.conjugateprior.ca.DPat;
import org.conjugateprior.ca.FXCategoryDictionary;
import org.conjugateprior.ca.IPatternEngine;
import org.conjugateprior.ca.IYoshikoderDocument;
import org.conjugateprior.ca.SubstringPatternEngine;

public class AbstractCounter {

	protected enum OutputFormat {
		TEXT, LATEX, HTML, CSV, LDAC, MTX
	};
	
	public static String WORDCOUNT = "WordCount";
	
	// see outputEncoding for what this will be encoded as
	protected OutputFormat format = OutputFormat.TEXT;

	public OutputFormat getFormat() {
		return format;
	}

	// sets format (SIDE EFFECT: if HTML 
	// also sets the output encoding to UTF-8)
	public void setFormat(OutputFormat format) {
		this.format = format;
		if (format.equals(OutputFormat.HTML))
			setOutputEncoding(Charset.forName("UTF-8"));
	}
	
	// sets format by calling setFormat(OutputFormat o)
	public void setFormat(String format) throws Exception {
		String s = format.toLowerCase();
		if (s.equals("html"))
			setFormat(OutputFormat.HTML);
		else if (s.equals("latex") || s.equals("tex"))
			setFormat(OutputFormat.LATEX);
		else 
			setFormat(OutputFormat.TEXT);
	}
	
	// get a suitably encoded stream for f 
	// if it needs an outputFolder construct that yourself
	public BufferedWriter getBufferedWriter(File f) throws Exception {
		OutputStreamWriter out = 
				new OutputStreamWriter(new FileOutputStream(f), 
				outputEncoding);
		return new BufferedWriter(out);
	}
	
	// get a suitably encoded stream wrapped around std-out
	public BufferedWriter getBufferedWriter() throws Exception {
		OutputStreamWriter out = 
				new OutputStreamWriter(System.out, 
				outputEncoding);
		return new BufferedWriter(out);
	}
	
	// window
	protected int window = 5;
	
	public int getWindow(){
		return window;
	}

	public void setWindow(int win){
		window = win;
	}
	
	protected File outputFolder = null;
	
	public File getOutputFolder() {
		return outputFolder;
	}

	public void setOutputFolder(File outputFolder) {
		this.outputFolder = outputFolder;
	}

	public void setOutputFolder(String outputFolder) {
		this.outputFolder = new File(outputFolder);
	}
	
	// charset / encoding
	protected Charset outputEncoding = Charset.defaultCharset();
	
	public Charset getOutputEncoding() {
		return outputEncoding;
	}

	public void setOutputEncoding(Charset outputCharset) {
		this.outputEncoding = outputCharset;
	}

	public void setOutputEncoding(String outputCharset) {
		this.outputEncoding = Charset.forName(outputCharset);
	}

	// files stuff
	protected File[] files = null;

	public File[] getFiles() {
		return files;
	}

	public void setFiles(String[] files) throws Exception {
		this.files = getRecursiveDepthOneFileArray(files);
	}

	public void setFiles(File[] files) throws Exception {
		this.files = getRecursiveDepthOneFileArray(files);
	}
	
	// dictionary stuff
	
	protected File dictionaryFile = null; // set if it comes in as a string/file
	protected FXCategoryDictionary dictionary;

	public File getDictionaryFile() {
		return dictionaryFile;
	}
	
	public FXCategoryDictionary getDictionary() {
		return dictionary;
	}

	// does not set dictionary file
	public void setDictionary(FXCategoryDictionary dict) {
		this.dictionary = dict;
	}
	
	// sets dictionary file
	public void setDictionary(String dictionaryFilename) throws Exception {
		dictionaryFile = new File(dictionaryFilename);
		setDictionary(dictionaryFile);
	}
	
	// SIDE EFFECT: sets dictionary file
	public void setDictionary(File dictionaryFile) throws Exception {
		if (!dictionaryFile.exists()){
			throw new Exception("Dictionary file cannot be found at"
					+ dictionaryFile.getAbsolutePath());
		}
		this.dictionaryFile = dictionaryFile;
		
		FXCategoryDictionary dict = null;
		String dn = dictionaryFile.getName();
		if (dn.toLowerCase().endsWith(".ykd") || 
				dn.toLowerCase().endsWith(".lcd")){
			dict = FXCategoryDictionary.readXmlCategoryDictionaryFromFile(dictionaryFile); 	
		
		} else if (dn.toLowerCase().endsWith(".vbpro")){
			dict = FXCategoryDictionary.importCategoryDictionaryFromFileVBPRO(dictionaryFile); 
		
		} else if (dn.toLowerCase().endsWith(".cat")){
			dict = FXCategoryDictionary.importCategoryDictionaryFromFileWordstat(dictionaryFile);
		
		} else if (dn.toLowerCase().endsWith(".dic")){
			dict = FXCategoryDictionary.importCategoryDictionaryFromFileLIWC(dictionaryFile);
		
		} else if (dn.toLowerCase().endsWith(".xml")) {
			// windows or server .xml addition?
			dict = FXCategoryDictionary.readXmlCategoryDictionaryFromFile(dictionaryFile); 
				
		} else {
			throw new Exception(
					"Dictionary file format could not be identified.\n" +
				    "It must be a Yoshikoder ('.ykd'), Lexicoder ('.lcd'), " +
				    "Wordstat ('.CAT'), LIWC (.dic), or VBPro ('.vbpro') file\n");
		}		
		setDictionary(dict);
	}
		
	protected TreeItem<DCat> category = null;
	
	public TreeItem<DCat> getCategory(){
		return category;
	}
	
	// replaces patterns with the subtree of category
	// assumes that a dictionary is in place
	public void setCategory(TreeItem<DCat> cat){
		category = cat;
		Set<DPat> s = dictionary.getPatternsInSubtree(category);
		patterns.clear();
		for (DPat dPat : s) {
			patterns.add(dPat.getRegexps());
		}
	}
	
	// find it by name and set it
	// replaces patterns with the subtree of category
	// assumes that a dictionary is in place
	public void setCategory(String cat){
		List<TreeItem<DCat>> ll = dictionary.getCategoryNodesInPrintOrder();
		TreeItem<DCat> item = null;
		for (TreeItem<DCat> treeItem : ll) {
			if (treeItem.getValue().getName().equals(cat)){
				item = treeItem;
				break;
			}
		}
		setCategory(item);
	}
	
	// default substring
	protected IPatternEngine patternEngine = new SubstringPatternEngine();
	
	// default empty not null
	protected List<Pattern[]> patterns = new ArrayList<Pattern[]>();
	
	public List<Pattern[]> getPatterns() {
		return patterns;
	}

	// replaces patterns
	public void setPatterns(List<Pattern[]> patterns) {
		this.patterns = patterns;
	}

	public void addPatterns(List<Pattern[]> patterns) {
		this.patterns.addAll(patterns);
	}

	public void addPattern(Pattern[] pattern) {
		this.patterns.add(pattern);
	}
	
	// make one with a substring pattern engine 
	public void addPattern(String pattern) {
		String[] spl = pattern.split("[ ]+");
		if (patternEngine == null)
			patternEngine = new SubstringPatternEngine();
		addPattern( patternEngine.makeRegexp(spl) );
	}

	protected boolean usingOldMatchStrategy = false;
	
	public boolean isUsingOldMatchStrategy() {
		return usingOldMatchStrategy;
	}

	public void setUsingOldMatchStrategy(boolean useOldMatchStrategy) {
		this.usingOldMatchStrategy = useOldMatchStrategy;
	}

	protected void fillTreeWithIndices(IYoshikoderDocument doc){
		// optimise later
		//Set<String> vocab = doc.getWordTypes();
		List<TreeItem<DCat>> categoryNodesInPrintOrder = dictionary.getCategoryNodesInPrintOrder();
		
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

	// this time with match counts not the indices
	protected void fillTreeWithMatchCounts(IYoshikoderDocument doc){
		// optimise later
		List<TreeItem<DCat>> categoryNodesInPrintOrder = dictionary.getCategoryNodesInPrintOrder();
		
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

	// locale stuff
	
	protected Locale locale = Locale.getDefault();
	
	public Locale getLocale() {
		return locale;
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	public void setLocale(String locale) throws Exception {
		this.locale = translateLocale(locale);
	}
	
	// charset stuff
	protected Charset encoding = Charset.defaultCharset();
	
	public Charset getEncoding() {
		return encoding;
	}

	public void setEncoding(Charset encoding) {
		this.encoding = encoding;
	}
	
	public void setEncoding(String encoding) throws Exception {
		this.encoding = Charset.forName(encoding);
	}
	
	public Locale translateLocale(String s) throws Exception {
		String[] bits = s.split("_");
		int bl = bits.length;
		Locale l = null;
		if (bl == 1)
			l = new Locale(bits[0]); // language
		else if (bl == 2)
			l = new Locale(bits[0], bits[1]); // language country
		else if (bl == 3)
			l = new Locale(bits[0], bits[1], bits[2]); // lang country var
		else {
			throw new Exception("Could not parse locale");
		}
		return l;
	}
	
	
	public File[] getRecursiveDepthOneFileArray(String[] files) throws Exception {
		List<File> filelist = new ArrayList<File>();
		for (int ii = 0; ii < files.length; ii++) {
			try {
				File g = new File(files[ii]);
				if (!g.exists())
					throw new Exception(g.getName() + " does not exist");
				filelist.add(g);
			} catch (Exception ex){
				System.err.println(ex.getMessage());
			}
		}
		return getRecursiveDepthOneFileArray(filelist.toArray(new File[filelist.size()]));
	}

	/**
	 * Get files from a folder and all files not beginning with period one folder down
	 * from it
	 * @param array of filenames
	 * @return array of files
	 * @throws Exception one of the filenames does not exist
	 */
	public File[] getRecursiveDepthOneFileArray(File[] files) throws Exception {
		List<File> filelist = new ArrayList<File>();
		File fail = null;
		for (int ii = 0;  ii < files.length; ii++) {
			File f = files[ii];
			if (!f.exists()){
				fail = f;
				break;
			} if (f.isDirectory()){
				File[] contents = f.listFiles();
				for (int jj = 0; jj < contents.length; jj++) {
					if (!contents[jj].isDirectory() && !contents[jj].getName().startsWith("."))
						if (contents[jj].length() > 0)
							filelist.add(contents[jj]); // an imperfect filter but...
				}
			} else {
				if (f.length() > 0)
					filelist.add(f);
			}
		}
		if (fail != null)
			throw new Exception("File " + fail.getAbsolutePath() + " does not exist.");
		
		return filelist.toArray(new File[filelist.size()]);
	}

}
