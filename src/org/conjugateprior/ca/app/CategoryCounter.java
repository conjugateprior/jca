package org.conjugateprior.ca.app;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Date;

import javafx.scene.control.TreeItem;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.conjugateprior.ca.AbstractYoshikoderDocument;
import org.conjugateprior.ca.DCat;
import org.conjugateprior.ca.FXCategoryDictionary;
import org.conjugateprior.ca.FileBasedYoshikoderDocument;
import org.conjugateprior.ca.IYoshikoderDocument;
import org.conjugateprior.ca.SimpleDocumentTokenizer;
import org.conjugateprior.ca.SimpleYoshikoderDocument;

public class CategoryCounter extends AbstractCounter {
	
	public CategoryCounter() {
		super();
		setFormat(OutputFormat.CSV); 
	}
	
	public void dumpMetadata() throws Exception {
		// push out the translated dictionary
		File dict = new File(outputFolder, "dictionary.ykd");
		
		FXCategoryDictionary.FXCatDictXMLPrinter dprinter = 
				new FXCategoryDictionary.FXCatDictXMLPrinter(dictionary);
		dprinter.printToFile050805(dict);
		// plus original file
		boolean orig = false;
		if (dictionaryFile != null){
			if (!dictionaryFile.getName().toLowerCase().endsWith(".ykd")){
				FileUtils.copyFile(dictionaryFile, new File(outputFolder, dictionaryFile.getName()));
				orig = true;
			}
		}
		// and a readme
		File readme = new File(outputFolder, "README.txt");
		
		try (
				OutputStreamWriter write = new OutputStreamWriter(
						new FileOutputStream(readme), outputEncoding);
				BufferedWriter writer = new BufferedWriter(write);
			){
			String dd = DateFormatUtils.format(DateUtils.toCalendar(new Date()), 
					"yyyy-MM-dd HH:mm:ss");
			writer.write("Date: " + dd);
			writer.newLine();
			writer.write("Input file encoding: " + encoding);
			writer.newLine();
			String fname = null;
			if (format.equals(OutputFormat.HTML))
				fname = "data.html";
			else
				fname = "data.csv"; // TODO check this hard coding doesn't break anythng
			writer.write("Output data file: " + fname);
			writer.newLine();
			writer.write("Output file encoding: " + outputEncoding);
			writer.newLine();
			writer.write("Dictionary file: " + "dictionary.ykd");
			writer.newLine();
			if (orig){
				writer.write("Original dictionary file: " + dictionaryFile.getName());
				writer.newLine();
			}
			writer.write("Matching strategy: " + (usingOldMatchStrategy ? "old" : "new"));
			writer.newLine();
		}
	}
	
	public void processFiles() throws Exception {
		// figure out where we're firing this stuff out of
		if (outputFolder != null){
			FileUtils.forceMkdir(outputFolder);
			dumpMetadata();
		}
		
		// now the long part
		BufferedWriter writer = null;
		try {
			if (outputFolder != null){
				if (format.equals(OutputFormat.HTML))
					writer = getBufferedWriter(new File(outputFolder, "data.html"));
				else
					writer = getBufferedWriter(new File(outputFolder, "data.csv"));
			} else {
				if (format.equals(OutputFormat.HTML))
					writer = getBufferedWriter();
				else
					writer = getBufferedWriter();
			}
			if (format.equals(OutputFormat.HTML))
				writer.write(makeHTMLHeader()); 			
			else {
				writer.write(makeCSVHeader());
				writer.newLine();
			}
			SimpleDocumentTokenizer tok = 
					new SimpleDocumentTokenizer(locale);
			Charset cs = getEncoding();
			for (File f : files) {
				IYoshikoderDocument idoc = 
						new FileBasedYoshikoderDocument(f.getName(), 
								AbstractYoshikoderDocument.getTextFromFile(f, encoding),
								null, tok, f, cs);	
				if (format.equals(OutputFormat.HTML))	
					writer.write(makeHTMLLineFromDocument(idoc));
				else 
					writer.write(makeCSVLineFromDocument(idoc));
				
				writer.newLine();
				
				if (!getSilent())
					System.err.print(".");	
			}
			if (!getSilent())
				System.err.println();
			
			if (format.equals(OutputFormat.HTML))
				writer.write(makeHTMLFooter());
			writer.flush(); // do we need this really?

		} finally {
			if (writer != null)
				writer.close();
		}
	}
	
	public String makeHTMLHeader() {
		StringBuilder sb = new StringBuilder();
		//sb.append(StringEscapeUtils.escapeCsv(doc.getTitle()));
		sb.append("<!DOCTYPE html>\n<html>\n<head>\n<meta charset=\"UTF-8\">\n" + 
				"<style>\n#cattable { text-align: right }\n" + 
				"#cattable .leftalign { text-align: left }\n</style>\n" + 
				"</head>\n<body>\n  <table id=\"cattable\">\n");
		sb.append("    <tr><th id=\"leftalign\">Document</th>");
		for (TreeItem<DCat> cn : dictionary.getCategoryNodesInPrintOrder()) {
			String name = DCat.getPathAsString(cn, ">");
			sb.append("<th>");
			sb.append(StringEscapeUtils.escapeHtml4(name));
			sb.append("</th>");
		}
		sb.append("<th>" + StringEscapeUtils.escapeHtml4(WORDCOUNT) + "</th>");
		sb.append("</tr>\n");
		return sb.toString();
	}		

	public String makeHTMLFooter(){
		return "  </table>\n</body>\n</html>\n";
	}
	
	// new style and old style matching
	public String makeHTMLLineFromDocument(IYoshikoderDocument doc){
		if (!usingOldMatchStrategy)
			fillTreeWithIndices(doc);
		else
			fillTreeWithMatchCounts(doc);
		
		StringBuilder sb = new StringBuilder();
		sb.append("<tr><td id=\"leftalign\">");
		sb.append(StringEscapeUtils.escapeCsv(doc.getTitle()));
		sb.append("</td>");
		for (TreeItem<DCat> cn : dictionary.getCategoryNodesInPrintOrder()) {
			sb.append("<td>");
			sb.append(cn.getValue().getMatchedIndices().size());
			sb.append("</td>");
		}
		sb.append("<td>");
		sb.append(doc.getDocumentLength()); 
		sb.append("</td></tr>");
		
		return sb.toString();
	}
	
	public String makeCSVHeader() {
		StringBuilder sb = new StringBuilder();
		//sb.append(StringEscapeUtils.escapeCsv(doc.getTitle()));
		for (TreeItem<DCat> cn : dictionary.getCategoryNodesInPrintOrder()) {
			sb.append(",");
			String name = DCat.getPathAsString(cn, ">");
			sb.append(StringEscapeUtils.escapeCsv(name));
		}
		sb.append("," + StringEscapeUtils.escapeCsv(WORDCOUNT));
		return sb.toString();
	}
	
	public String makeCSVLineFromDocument(IYoshikoderDocument doc){
		if (!usingOldMatchStrategy)
			fillTreeWithIndices(doc);
		else
			fillTreeWithMatchCounts(doc);
		
		StringBuilder sb = new StringBuilder();
		sb.append(StringEscapeUtils.escapeCsv(doc.getTitle()));
		for (TreeItem<DCat> cn : dictionary.getCategoryNodesInPrintOrder()) {
			sb.append(",");
			sb.append(cn.getValue().getMatchedIndices().size());
		}
		sb.append(",");
		sb.append(doc.getDocumentLength()); // no newline
		return sb.toString();
	}

	// to a file
	/*
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
     */
}