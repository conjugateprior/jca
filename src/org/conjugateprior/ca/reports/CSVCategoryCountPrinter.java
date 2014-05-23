package org.conjugateprior.ca.reports;

import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.conjugateprior.ca.CategoryDictionary;
import org.conjugateprior.ca.CategoryDictionary.DictionaryCategory;
import org.conjugateprior.ca.CategoryDictionary.DictionaryPattern;
import org.conjugateprior.ca.IYoshikoderDocument;

public class CSVCategoryCountPrinter extends CountPrinter {

    protected String wordCountHeader = "WordCount";
	
	protected CategoryDictionary hdict; // instead of the WordCounter
	protected DictionaryCategory[] categoryNodesInPrintOrder;
	
	protected BufferedWriter writer;
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
	
	protected DictionaryCategory[] getCategoryNodesInPrintOrder(CategoryDictionary d){
		DictionaryCategory n = d.getCategoryRoot();
		List<DictionaryCategory> list = new ArrayList<DictionaryCategory>();
		recurseCategories(list, n);
		return list.toArray(new DictionaryCategory[list.size()]);
	}	
	
	public CSVCategoryCountPrinter(CategoryDictionary dict, 
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
		sb.append(doc.getDocumentLength());
		sb.append(newline);
		return sb.toString();
	}

	// Excel escaping: double quotes get doubled, commas trigger double quotes
	protected String excelEscape(String s){
		String ss = s;
		if (s.contains("\""))
			ss = s.replaceAll("\"", "\"\"");
		if (ss.contains(",")) // should also check for newlines
			ss = "\"" + ss + "\"";
		return ss;
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
						doc.getAllMatchingTokenIndexesForPattern(pat.getRegexps());
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
		CategoryDictionary dict = new CategoryDictionary();
		dict.getCategoryRoot().setName("Foo");
		DictionaryCategory ct1 = 
				dict.addCategoryToParentCategory("Cat1", dict.getCategoryRoot());
		dict.addPatternToCategory("mar*", ct1);
		dict.addPatternToCategory("lamb", ct1);
		DictionaryCategory ct2 = 
				dict.addCategoryToParentCategory("Cat2", dict.getCategoryRoot());
		dict.addPatternToCategory("every*", ct2);
		
		File outf = new File("/Users/will/Desktop/jfreq-folder");
		CSVCategoryCountPrinter pr = new CSVCategoryCountPrinter(dict, 
				outf, 
				Charset.forName("UTF8"), Locale.ENGLISH, 
				new File[]{new File("/Users/will/Desktop/jfreqing", "d1.txt"), 
			new File("/Users/will/Desktop/jfreqing", "d2.txt")});

		pr.processFiles(true);
	}
	
}
