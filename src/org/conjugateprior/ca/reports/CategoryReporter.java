package org.conjugateprior.ca.reports;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.conjugateprior.ca.CategoryDictionary;
import org.conjugateprior.ca.CategoryDictionary.DictionaryCategory;
import org.conjugateprior.ca.CategoryDictionary.DictionaryPattern;
import org.conjugateprior.ca.IYoshikoderDocument;
import org.conjugateprior.ca.SimpleDocumentTokenizer;
import org.conjugateprior.ca.SimpleYoshikoderDocument;
/**
 * @deprecated
 * @author will
 *
 */
public class CategoryReporter {

	class ReportLine {
		Integer[] count;
		Integer wordCount;

		public ReportLine(Integer[] counts, Integer wCount) {
			count = counts;
			wordCount = wCount;
		}		
		@Override
		public String toString() {
			return Arrays.toString(count) + " (" + wordCount + ")";
		}
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

	// this is a static constructed report that toStrings to CSV
	// for many documents it's probably better to stream the report lines 
	// into a file
	/*
	class Report {
		String[] documentNames;
		ReportLine[] reportLines;
		String sep;
		String[] header;
	
		public Report(String[] dNames, String[] colnames, ReportLine[] rLines, String separator) {
			documentNames = dNames;
			reportLines = rLines;
			sep = separator;
			header = colnames;
		}
		
		@Override
		public String toString() {
			if (reportLines.length == 0)
				return "";
			StringBuffer sb = new StringBuffer();
			for (int ii = 0; ii < header.length; ii++) {
				sb.append(",");
				sb.append(escape(header[ii]));
			}
			sb.append("," + escape(DictionaryReporter.WORD_COUNT_HEADER));
			sb.append("\n");
			for (int ii = 0; ii < reportLines.length; ii++) {
				Integer[] counts = reportLines[ii].count;
				sb.append(escape(documentNames[ii]));
				for (int jj = 0; jj < counts.length; jj++) {
					sb.append(",");
					sb.append(counts[jj].toString());
				}
				sb.append(",");
				sb.append(reportLines[ii].wordCount);
				sb.append("\n");
			}
			return sb.toString();
		}
	}	
	*/	

	protected String wordCountHeader = "WordCount";
	
	protected CategoryDictionary hdict;
	protected DictionaryCategory[] categoryNodesInPrintOrder;
	
	protected BufferedWriter writer;
	protected String fieldSeparator = ",";
	protected String categorySeparator = ">";
	protected String newline = "\n";
	protected Charset outputCharset = Charset.forName("UTF-8");
	
	public void setWindowsOutput(){
		outputCharset = Charset.forName("windows-1252");
		newline = "\r\n";
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
	
	public CategoryReporter(CategoryDictionary dict) {
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
	
	public void openStreamingReport(OutputStream out) throws IOException {
		OutputStreamWriter osw = new OutputStreamWriter(out, outputCharset);
		writer = new BufferedWriter(osw);

		for (DictionaryCategory cat : categoryNodesInPrintOrder)
			writer.write(fieldSeparator + excelEscape(cat.getPathAsString(categorySeparator)));
		writer.write(fieldSeparator + excelEscape(wordCountHeader) + newline);
	}
	
	public void openStreamingReport(File f) throws IOException {
		openStreamingReport(new FileOutputStream(f));
	}

	public void streamReportLine(String docname, ReportLine line) throws IOException {
		writer.write(excelEscape(docname));
		for (int ii = 0; ii < line.count.length; ii++)
			writer.append(fieldSeparator + line.count[ii].toString());
		writer.append(fieldSeparator + line.wordCount.toString() + newline);
	}

	public void closeStreamingReport() throws IOException {
		if (writer != null)
			writer.close();
	}

	public ReportLine reportOnDocumentOldStyle(IYoshikoderDocument doc){
		fillTreeWithMatchCounts(doc);

		ReportLine line = null;
		Integer[] counts = new Integer[categoryNodesInPrintOrder.length];
		int counter = 0;
		for (DictionaryCategory cn : categoryNodesInPrintOrder) {
			// get the single count out and stuff it in the array
			counts[counter] = cn.getMatchedIndices().iterator().next();
			counter++;
		}
		line = new ReportLine(counts, doc.getDocumentLength());
		return line;
	}
	
	public ReportLine reportOnDocument(IYoshikoderDocument doc){
		fillTreeWithIndices(doc);

		ReportLine line = null;
		Integer[] counts = new Integer[categoryNodesInPrintOrder.length];
		int counter = 0;
		for (DictionaryCategory cn : categoryNodesInPrintOrder) {
			// how many tokens did we manage to match?
			counts[counter] = cn.getMatchedIndices().size();
			counter++;
		}
		line = new ReportLine(counts, doc.getDocumentLength());
		return line;
	}

	public void setWordCountHeader(String wcHeader) {
		wordCountHeader = wcHeader;
	}
	
	public String getWordCountHeader() {
		return wordCountHeader;
	}
	
	// this is the old style match counting
	protected void fillTreeWithMatchCounts(IYoshikoderDocument doc){
		// optimise later
		//Set<String> vocab = doc.getWordTypes();
		for (DictionaryCategory node : categoryNodesInPrintOrder){
			Set<Integer> indexMatches = new HashSet<Integer>();
			Set<DictionaryPattern> pats = node.getPatterns();
			int rawCount = 0; // including double counted items!
			for (DictionaryPattern pat : pats) {
				Set<Integer> indices = 
						doc.getAllMatchingTokenIndexesForPattern(pat.getRegexps());
				rawCount += indices.size();
			}
			indexMatches.add(new Integer(rawCount)); // here one number, the total count
			node.setMatchedIndices(indexMatches);
		}
		// percolate
		for (DictionaryCategory node : categoryNodesInPrintOrder){
			if (node.isLeaf()){
				DictionaryCategory current = node;
				DictionaryCategory parent = (DictionaryCategory)node.getParent();
				while (parent != null){
					// (mis)use the indices to hold and update single counts 
					Set<Integer> count = current.getMatchedIndices();
					Integer co = count.iterator().next();
					Set<Integer> pcount = parent.getMatchedIndices();
					Integer pco = pcount.iterator().next();
					pcount.clear();
					pcount.add(new Integer(pco + co));
					
					current = parent;
					parent = (DictionaryCategory)current.getParent();
				}
			}
		}
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
				//System.err.println("Indices matched: " + indices.size());
				indexMatches.addAll(indices);
			}
			node.setMatchedIndices(indexMatches);
		}
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
		CategoryDictionary dd = new CategoryDictionary();
		dd.getCategoryRoot().setName("d");
		dd.addPatternToCategory("tomato", dd.getCategoryRoot());
		DictionaryCategory animal = dd.addCategoryToParentCategory("cat", dd.getCategoryRoot());
		
		try {
			DictionaryCategory foo = dd.addCategoryToParentCategory("cat", dd.getCategoryRoot());
		} catch (Exception ex){
			System.out.println( ex.toString() );
		}
		dd.addCategoryToParentCategory("Catt", dd.getCategoryRoot());
		dd.addCategoryToParentCategory("aca", dd.getCategoryRoot());
		dd.addCategoryToParentCategory("dbi", animal);
		
		DictionaryPattern pat = dd.addPatternToCategory("tomat* dog", animal);
		System.out.println( Arrays.toString(pat.getRegexps()) );
		
		pat = dd.addPatternToCategory("tomato dog cauliflower", animal);
		System.out.println( Arrays.toString(pat.getRegexps()) );
		
		CategoryReporter rep = new CategoryReporter(dd);
		rep.setCategorySeparator(":");
		rep.setFieldSeparator("\t");

		String[] doc = new String[]{"tomato", "tomato", "dog", 
				"cauliflower", "thing", "tomato", "dog"};
		StringBuffer buf = new StringBuffer();
		for (String string : doc) {
			buf.append(string);
			buf.append(" ");
		}
		System.out.println(buf.toString());
		IYoshikoderDocument ykdoc = new SimpleYoshikoderDocument("T\"as if\"", 
				buf.toString(), new Date(2000000), 
				new SimpleDocumentTokenizer(Locale.ENGLISH));
	
		IYoshikoderDocument yk2 = ykdoc;
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		rep.openStreamingReport(out);
		ReportLine line = rep.reportOnDocument(ykdoc);
		// ReportLine line = rep.reportOnDocumentOldStyle(ykdoc);
		rep.streamReportLine("mydoc thing", line);
		line = rep.reportOnDocument(yk2);
		rep.streamReportLine(ykdoc.getTitle(), line);
		rep.closeStreamingReport();
		System.out.println(out.toString());
	}
}
