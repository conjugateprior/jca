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
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.lang3.StringEscapeUtils;
import org.conjugateprior.ca.OldCategoryDictionary;
import org.conjugateprior.ca.OldCategoryDictionary.DictionaryCategory;
import org.conjugateprior.ca.OldCategoryDictionary.DictionaryPattern;
import org.conjugateprior.ca.IYoshikoderDocument;


/**
 * @deprecated
 * @author will
 *
 */
public class OldCSVCategoryCountPrinter extends CountPrinter {

    protected String wordCountHeader = "WordCount";
    protected String dictfilename = "dict.ykd";
	
	protected OldCategoryDictionary hdict; // instead of the WordCounter
	protected DictionaryCategory[] categoryNodesInPrintOrder;
	
	protected String fieldSeparator = ",";
	protected String categorySeparator = ">";
	
	public void setWindowsOutput(){
		outputCharset = Charset.forName("windows-1252");
		newline = "\r\n";
	}

	@Override
	protected String getDataHeader() {
		StringBuffer sb = new StringBuffer();
		for (DictionaryCategory cat : categoryNodesInPrintOrder) {
			sb.append(fieldSeparator);
			sb.append(excelEscape(cat.getPathAsString(categorySeparator)));
		}
		sb.append(fieldSeparator);
		sb.append(excelEscape(wordCountHeader));
		sb.append(newline);
		return sb.toString();
	}
	
	private void recurseCategories(List<DictionaryCategory> sb, DictionaryCategory n){
		sb.add(n);
		@SuppressWarnings("unchecked")
		Enumeration<DictionaryCategory> enumeration=n.children();
		while (enumeration.hasMoreElements()){
			DictionaryCategory child = enumeration.nextElement();
			recurseCategories(sb, child);
		}
	}
	
	protected DictionaryCategory[] getCategoryNodesInPrintOrder(OldCategoryDictionary d){
		DictionaryCategory n = d.getCategoryRoot();
		List<DictionaryCategory> list = new ArrayList<DictionaryCategory>();
		recurseCategories(list, n);
		return list.toArray(new DictionaryCategory[list.size()]);
	}	
	
	public OldCSVCategoryCountPrinter(OldCategoryDictionary dict, 
			File folder, Charset c, Locale l, File[] f) {
		super(folder, "data.csv", f, c, l);

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
		for (DictionaryCategory cn : categoryNodesInPrintOrder) {
			sb.append(fieldSeparator);
			sb.append(cn.getMatchedIndices().size());
		}
		sb.append(fieldSeparator);
		sb.append(doc.getWordCount());
		sb.append(newline);
		return sb.toString();
	}

	protected void writeRowsFile() throws Exception {
		BufferedWriter docsWriter = null;
		try {
			OutputStreamWriter docs = new OutputStreamWriter(
					new FileOutputStream(new File(folder, rowfilename)), outputCharset);
			docsWriter = new BufferedWriter(docs);
		
			for (File file : files) {
				docsWriter.write(file.getName() + newline);
			}
		} finally {
			if (docsWriter != null)
				docsWriter.close();
		}
	}
	
	protected void writeReadmeFile() throws Exception {
		Date d = new Date();
		String user = System.getProperty("user.name");
		BufferedWriter writer = null;
		try {
			OutputStreamWriter out = new OutputStreamWriter(
					new FileOutputStream(new File(folder, readmefilename)), outputCharset);
			writer = new BufferedWriter(out);
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
		} finally {
			if (writer != null)
				writer.close();
		}
	}
	
	// write out the dictionary too...
	@Override
	protected void postProcess() throws Exception {
		String s = hdict.toXml(true); // construct with a header
		
		BufferedWriter writer = null;
		try {
			OutputStreamWriter out = new OutputStreamWriter(
					new FileOutputStream(new File(folder, dictfilename)), outputCharset);
			writer = new BufferedWriter(out);
			writer.write(s);
		} finally {
			if (writer != null)
				writer.close();
		}
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
		for (DictionaryCategory node : categoryNodesInPrintOrder){
			//System.err.println("filling " + node);

			Set<Integer> indexMatches = new HashSet<Integer>();
			Set<DictionaryPattern> pats = node.getPatterns();
			for (DictionaryPattern pat : pats) {
				Set<Integer> indices = 
						doc.getWordIndexesForPattern(pat.getRegexps());
				//System.err.println(pat.getName() + " - indices matched: " + indices);
				indexMatches.addAll(indices);
			}
			node.setMatchedIndices(indexMatches);
		}
		// testing
		
		// percolate
		for (DictionaryCategory node : categoryNodesInPrintOrder){
			if (node.isLeaf()){
				DictionaryCategory current = node;
				DictionaryCategory parent = (DictionaryCategory)node.getParent();
				while (parent != null){
					parent.getMatchedIndices().addAll(current.getMatchedIndices());
					current = parent;
					parent = (DictionaryCategory)current.getParent();
				}
			}
		}
	}

	public static void main(String[] args) throws Exception {
		OldCategoryDictionary dict = new OldCategoryDictionary();
		dict.getCategoryRoot().setName("Foo");
		DictionaryCategory ct1 = 
				dict.addCategoryToParentCategory("Cat1", dict.getCategoryRoot());
		dict.addPatternToCategory("mar*", ct1);
		dict.addPatternToCategory("mar* had", ct1);
		dict.addPatternToCategory("lamb", ct1);
		DictionaryCategory ct2 = 
				dict.addCategoryToParentCategory("Cat2", dict.getCategoryRoot());
		dict.addPatternToCategory("every*", ct2);
		
		File outf = new File("/Users/will/Desktop/jfreq-folder");
		OldCSVCategoryCountPrinter wp = new OldCSVCategoryCountPrinter(dict, 
				outf, 
				Charset.forName("UTF8"), Locale.ENGLISH, 
				new File[]{new File("/Users/will/Desktop/jfreqing", "d1.txt"), 
			new File("/Users/will/Desktop/jfreqing", "d2.txt")});

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
