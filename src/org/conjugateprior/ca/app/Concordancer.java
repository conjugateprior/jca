package org.conjugateprior.ca.app;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.conjugateprior.ca.AbstractYoshikoderDocument;
import org.conjugateprior.ca.DocumentTokenizer;
import org.conjugateprior.ca.RegexpDocumentTokenizer;
import org.conjugateprior.ca.SimpleDocumentTokenizer;
import org.conjugateprior.ca.SimpleYoshikoderDocument;
import org.conjugateprior.ca.Tokenization;
import org.conjugateprior.ca.YoshikoderDocument;

public class Concordancer extends AbstractCounter {
	
	// compares the character indexes of the target starts
	private Comparator<int[]> indexComparator = new Comparator<int[]>() {
		@Override
		public int compare(int[] o1, int[] o2) {
			int diff = o1[2]-o2[2];
			if (diff > 0) 
				return 0;
			else if (diff < 0) 
				return -1;
			return diff;
		}
	};
	
	public Concordancer() {
		super();
	}

	protected String makeHTMLHeader(){
		return "<!DOCTYPE html>\n<html>\n<head>\n<meta charset=\"UTF-8\">\n" + 
				"<style>\n#conctable .rightalign { text-align: right }\n" + 
				"#conctable .leftalign { text-align: left }\n</style>\n" + 
				"</head>\n<body>\n  <table id=\"conctable\">\n" +
			    "<tr><th>Document</th><th></th><th class=\"leftalign\">Pattern</th></tr>";
	}
	
	protected static String collapseWhitespace(String s){
		return s.replace("\n", " ").replace("\r",  " ");
	}
	
	protected String makeHTMLLinesFromDocument(YoshikoderDocument doc) throws Exception {
		List<int[]> concs = new ArrayList<int[]>();
		for (Pattern[] pat : patterns) 
			concs.addAll( doc.getConcordanceCharacterOffsetsForPattern(pat, window) );
		concs.sort(indexComparator);
		String txt = doc.getText();
		StringBuffer str = new StringBuffer();
		boolean firstline = true;
		if (concs.size()==0){
			str.append("    <tr><td>" +
					StringEscapeUtils.escapeHtml4(doc.getTitle()) + 
					"</td><td></td><td></td></tr>");
		} else {
			for (int[] is : concs) {
				str.append("    <tr><td>" + (firstline ? StringEscapeUtils.escapeHtml4(doc.getTitle()) : "") + "</td>");	
				firstline = false;
				String s;
				if (is[0] != -1)
					s = collapseWhitespace(txt.substring(is[0], is[1]));
				else 
					s = "";
				str.append("<td class=\"rightalign\">" + 
						StringEscapeUtils.escapeHtml4(s) + "</td>");
				String targ = collapseWhitespace(txt.substring(is[2], is[3]));
				int restart = (is[3]<txt.length() ? is[3] : is[4]);
				if (is[5] != -1)
					s = collapseWhitespace(txt.substring(restart, is[5]));
				else 
					s = "";
				str.append("<td><strong>" + StringEscapeUtils.escapeHtml4(targ) + "</strong>" +
						StringEscapeUtils.escapeHtml4(s) + "</td></tr>");
				str.append(SystemUtils.LINE_SEPARATOR); // oohh
			}
		}
		return str.toString();
	}
	
	public static YoshikoderDocument makeConcordanceDocument(List<Pattern[]> pats, int window, 
			YoshikoderDocument doc, DocumentTokenizer tok)
	throws Exception {
		// FIXME First cut: ignore double counting words that match multiple patterns
		//StringBuffer sb = new StringBuffer();
		//int n = doc.getWordCount();
		System.err.println("words -> " + doc.getWordCount());
		String txt = doc.getText();
		//System.err.println(txt);
		
		StringBuffer str = new StringBuffer();
		for (Pattern[] patterns : pats) {
			System.err.println(patterns[0].pattern());
		}
		
		
		for (Pattern[] pat : pats) {
			List<int[]> concs = doc.getConcordanceCharacterOffsetsForPattern(pat, window);
		
			for (int[] is : concs) {
				String s;
				if (is[0] != -1)
					s = collapseWhitespace(txt.substring(is[0], is[1]));
				else 
					s = "";
				str.append(s);

				//s = " [" + txt.substring(is[2], is[3]) + "]";			
				//str.append(s);
				str.append(" -- "); //indicate absence without being caught in the tokenizer
				
				// catch trailing punctuation etc. by restarting straight after match
				int restart = (is[3]<txt.length() ? is[3] : is[4]);

				if (is[5] != -1)
					s = collapseWhitespace(txt.substring(restart, is[5]));
				else
					s = "";

				str.append(s);
				str.append(SystemUtils.LINE_SEPARATOR);
			}
		}
		
		return new SimpleYoshikoderDocument("Pre-concordanced ".concat(doc.getTitle()), 
				str.toString(), doc.getDate(), tok);
	}
	
	protected String makeTextLinesFromDocument(YoshikoderDocument doc) throws Exception {
		List<int[]> concs = new ArrayList<int[]>();
		
		for (Pattern[] pat : patterns) 
			concs.addAll( doc.getConcordanceCharacterOffsetsForPattern(pat, window) );
		concs.sort(indexComparator);
		String txt = doc.getText();
		StringBuffer str = new StringBuffer();
		
		int maxlen = 0;
		for (int[] is : concs)
			maxlen = Math.max(maxlen, is[1]-is[0]);
		for (int[] is : concs) {
			String s;
			if (is[0] != -1)
				s = collapseWhitespace(txt.substring(is[0], is[1]));
			else 
				s = "";
			
			str.append(StringUtils.leftPad(s, maxlen));
			
			s = " [" + txt.substring(is[2], is[3]) + "]";
						
			str.append(s);
		    
		    // catch trailing punctuation etc. by restarting straight after match
		    int restart = (is[3]<txt.length() ? is[3] : is[4]);
			
		    if (is[5] != -1)
		    	s = collapseWhitespace(txt.substring(restart, is[5]));
		    else
		    	s = "";
		    
			str.append(s);
			str.append(SystemUtils.LINE_SEPARATOR); // oohh
		}
		return str.toString();
	}

	protected String makeHTMLFooter(){
		return "</table>\n</body>\n</html>\n";
	}
	
	public void processFiles() throws Exception {
		// figure out where we're firing this stuff out of
		BufferedWriter writer = null;
		try {
			if (outputFolder != null){
				FileUtils.forceMkdir(outputFolder);
				if (format.equals(OutputFormat.HTML))
					writer = getBufferedWriter(new File(outputFolder, "concordance.html"));
				else
					writer = getBufferedWriter(new File(outputFolder, "concordance.txt"));
			} else {
				if (format.equals(OutputFormat.HTML))
					writer = getBufferedWriter();
				else
					writer = getBufferedWriter();
			}
			if (format.equals(OutputFormat.HTML))
				writer.write(makeHTMLHeader()); 			
			
			DocumentTokenizer tok = null;
			if (usingRegexpTokenizer)
				if (regexp == null)
					throw new Exception("No regexp set");
				else 
					tok = new RegexpDocumentTokenizer(locale, regexp);
			else
				tok = new SimpleDocumentTokenizer(locale);
			
			for (File f : files) {
				YoshikoderDocument idoc = 
						new SimpleYoshikoderDocument(f.getName(), 
								AbstractYoshikoderDocument.getTextFromFile(f, encoding),
								null, tok);	
				if (format.equals(OutputFormat.HTML))	
					writer.write(makeHTMLLinesFromDocument(idoc));
				else {
					writer.write("---- " + idoc.getTitle());
					writer.newLine();
					writer.write(makeTextLinesFromDocument(idoc));
				}
				writer.newLine();
				writer.flush(); // do we need this really?
			}
			if (format.equals(OutputFormat.HTML))
				writer.write(makeHTMLFooter());
			writer.flush(); // do we need this really?
		} finally {
			if (writer != null)
				writer.close();
		}
	}
	
	public static void main(String[] args) throws Exception {
		String s = "An mary had a little lamb and then she had some more lamb.";
		DocumentTokenizer simp = new SimpleDocumentTokenizer(Locale.ENGLISH);
		YoshikoderDocument doc = new SimpleYoshikoderDocument("mary", s, null, 
				simp);
		List<Pattern[]> pats = new ArrayList<>();
		pats.add(new Pattern[]{Pattern.compile("lamb")});
		pats.add(new Pattern[]{Pattern.compile("little"), Pattern.compile("lamb")});
		YoshikoderDocument dd = Concordancer.makeConcordanceDocument(pats, 2, doc, simp);
		System.out.println(dd.getTitle());
		System.out.println(dd.getText());
	}
	
	
}
