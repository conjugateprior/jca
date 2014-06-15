package org.conjugateprior.ca.reports;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javafx.scene.control.TreeItem;

import org.apache.commons.lang3.StringEscapeUtils;
import org.conjugateprior.ca.DCat;
import org.conjugateprior.ca.DPat;
import org.conjugateprior.ca.FXCategoryDictionary;
import org.conjugateprior.ca.IYoshikoderDocument;

public class CSVFXCategoryDictionaryCountPrinter extends CountPrinter {

    protected String wordCountHeader = "WordCount";
    protected String dictfilename = "dict.ykd";
	
	protected FXCategoryDictionary hdict; // instead of the WordCounter
	protected List<TreeItem<DCat>> categoryNodesInPrintOrder;
	
	protected String fieldSeparator = ",";
	protected String categorySeparator = ">";
	
	public void setWindowsOutput(){
		outputCharset = Charset.forName("windows-1252");
		newline = "\r\n";
	}

	@Override
	protected String getDataHeader() {
		StringBuffer sb = new StringBuffer();
		for (TreeItem<DCat> cat : categoryNodesInPrintOrder) {
			sb.append(fieldSeparator);
			sb.append(excelEscape(FXCategoryDictionary.getNodePathAsString(cat, categorySeparator)));
		}
		sb.append(fieldSeparator);
		sb.append(excelEscape(wordCountHeader));
		sb.append(newline);
		return sb.toString();
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

	public CSVFXCategoryDictionaryCountPrinter(FXCategoryDictionary dict,
			File f, String df, File[] fs,
			Charset chset, Locale loc) {
		super(f, "data.csv", fs, chset, loc);

		hdict = dict;		
		categoryNodesInPrintOrder = getCategoryNodesInPrintOrder(dict);
		//System.err.println("the order");
		//System.err.println(Arrays.toString(categoryNodesInPrintOrder));
		
	}

	public void setFieldSeparator(String fSep){
		fieldSeparator = fSep;
	}
	
	public String getFieldSeparator() {
		return fieldSeparator;
	}
	
	public String getCategorySeparator() {
		return categorySeparator;
	}
	
	public void setCategorySeparator(String cSep){
		categorySeparator = cSep;
	}

	public String makeLineFromDocument(IYoshikoderDocument doc){
		fillTreeWithIndices(doc);

		StringBuilder sb = new StringBuilder();
		sb.append(excelEscape(doc.getTitle()));
		for (TreeItem<DCat> cn : categoryNodesInPrintOrder) {
			sb.append(fieldSeparator);
			sb.append(cn.getValue().getMatchedIndices().size());
		}
		sb.append(fieldSeparator);
		sb.append(doc.getDocumentLength());
		sb.append(newline);
		return sb.toString();
	}
	
	protected void writeReadmeFile() throws Exception {
		Date d = new Date();
		String user = System.getProperty("user.name");
		//BufferedWriter writer = null;
		try (
			OutputStreamWriter out = new OutputStreamWriter(
				new FileOutputStream(new File(folder, readmefilename)), outputCharset);
			BufferedWriter writer = new BufferedWriter(out);
		){
			writer.write("Dictionary-based content analysis");
			writer.write(newline);
			writer.write(newline);
			writer.write("User:\t" + user);
			writer.write(newline);
			writer.write("Time:\t" + d.toString());
			writer.write(newline);
			writer.write("Dict:\tdict.ykd (copy of the original)");
			writer.write(newline);
			writer.write("Docs:\tdocuments.csv");
			writer.write(newline);
		}
	}
	
	// write out the dictionary too...
	@Override
	protected void postProcess() throws Exception {
		super.postProcess();
		
		FXCategoryDictionary.FXCatDictXMLPrinter printer = 
			new FXCategoryDictionary.FXCatDictXMLPrinter(hdict);
		File loc = new File(folder, dictfilename);
		printer.printToFile050805(loc);
	}
	
	// Excel escaping: double quotes get doubled, commas trigger double quotes
	protected String excelEscape(String s){
		return StringEscapeUtils.escapeCsv(s);
		/*
		String ss = s;
		if (s.contains("\""))
			ss = s.replaceAll("\"", "\"\"");
		if (ss.contains(",")) // should also check for newlines
			ss = "\"" + ss + "\"";
		return ss;
		*/
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
		// testing
		
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

	public static void main(String[] args) throws Exception {
		FXCategoryDictionary dict = new FXCategoryDictionary("New dict");
		dict.getCategoryRoot().getValue().setName("Foo");
		TreeItem<DCat> ct1 = 
				dict.addCategoryToParentCategory("Cat1", dict.getCategoryRoot());
		dict.addPatternToCategory("mar*", ct1);
		dict.addPatternToCategory("mar* had", ct1);
		dict.addPatternToCategory("lamb", ct1);
		TreeItem<DCat> ct2 = 
				dict.addCategoryToParentCategory("Cat2", dict.getCategoryRoot());
		dict.addPatternToCategory("every*", ct2);
		
		File outf = new File("/Users/will/Desktop/jfreq-folder");
		CSVFXCategoryDictionaryCountPrinter wp = 
				new CSVFXCategoryDictionaryCountPrinter(dict, 
				outf, "data.csv",
				new File[]{new File("/Users/will/Desktop/jfreqing", "d1.txt"), 
						   new File("/Users/will/Desktop/jfreqing", "d2.txt")},
			    Charset.forName("UTF8"), 
			    Locale.ENGLISH);

		final int maxProg = wp.getMaxProgress();
		PropertyChangeListener listener = new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if ("progress".equals(evt.getPropertyName())){
					System.err.println(evt.getNewValue() + " of " + maxProg);
				}
			}
		};
		wp.addPropertyChangeListener(listener);
		wp.processFiles(false);		
	}
	
}
