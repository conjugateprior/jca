package org.conjugateprior.ca.exp;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.scene.control.TreeItem;
import javafx.scene.paint.Color;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.lang3.StringEscapeUtils;
import org.conjugateprior.ca.IPatternEngine;
import org.conjugateprior.ca.SubstringPatternEngine;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class FXCatDict {

	protected static String duplicateMessage = 
		"There is already a category with that name under this parent category";
	
	public static IPatternEngine patternEngine;
	
	public enum XmlDictionaryType {
	    LEXICODER, YOSHIKODER_050805 
	}
	
	// for telling which sort of xml format this is
	public static class XMLDictionaryTypeIdentifier extends DefaultHandler {
		
		XmlDictionaryType type = null;
		
		public void startElement(String uri, String localName, String qName,
				Attributes attributes) throws SAXException{
			if (qName.equals("dictionary")){ 
				String style = attributes.getValue("style");
				if (style.equals("Lexicoder"))
					type = XmlDictionaryType.LEXICODER;
				else if (style.equals("050805"))
					type = XmlDictionaryType.YOSHIKODER_050805;
			}
		}
		
		public XmlDictionaryType getDictionaryType(){
			return type;
		}
	}
	
	// get the type of xml dictionary or null if we can't tell
	public static XmlDictionaryType identifyDictionaryXmlFormat(File f) 
			throws Exception {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		InputStream stream = null;
		try {
			SAXParser parser = factory.newSAXParser();
			XMLDictionaryTypeIdentifier identifier = new XMLDictionaryTypeIdentifier();
			stream = new FileInputStream(f);
			parser.parse(stream, identifier);
			return identifier.getDictionaryType();

		} catch (Exception e){
			throw e; // re-throw
		} finally {
			if (stream != null)
				stream.close();
		}
	}
	
	public static class LexicoderHandler extends DefaultHandler {
		private Stack<TreeItem<DCat>> stack = new Stack<TreeItem<DCat>>();
		private FXCatDict dict;

		public void startElement(String uri, String localName, String qName,
				Attributes attributes) throws SAXException{
			if (qName.equals("dictionary")){ 
				dict = new FXCatDict("New Dictionary"); // substring pattern engine by default
				String name = attributes.getValue("name");
				
				dict.getCategoryRoot().getValue().setName(name); 
				stack.push(dict.getCategoryRoot());
				
			} else if (qName.equals("cnode")){ 
				String name = attributes.getValue("name");             
				TreeItem<DCat> newcat = null;
				// the dictionary root is always on the stack at this point
				try {
					newcat = dict.addCategoryToParentCategory(name, stack.peek());
				} catch (Exception ex){
					throw new SAXException(ex);
				}
				stack.push(newcat);
				
			} else if (qName.equals("pnode")){ 
				String name = attributes.getValue("name");
				try {
					// trim because we Lexicoder seems to add a leading space
					dict.addPatternToCategory(name.trim(), stack.peek());
				} catch (Exception ex){
					throw new SAXException(ex);
				}
			}
		}

		public void endElement(String uri, String localName, String qName) 
				throws SAXException{
			if (qName.equals("cnode"))
				stack.pop();
		}
		
		public FXCatDict getCategoryDictionary(){
			return dict;
		}
	}	
	
	public static class YKDHandler050805 extends DefaultHandler {
		private Stack<TreeItem<DCat>> stack = new Stack<TreeItem<DCat>>();
		private FXCatDict dict;

		private Color parseColor(String s) throws Exception {
			String[] vv = s.split(" ");
			return new Color(Double.parseDouble(vv[0]), 
					         Double.parseDouble(vv[1]),
							 Double.parseDouble(vv[2]),
							 1); // opacity 1
		}
		
		public void startElement(String uri, String localName, String qName,
				Attributes attributes) throws SAXException{
			if (qName.equals("dictionary")){ 
				dict = new FXCatDict("New Dictionary"); // substring pattern engine by default
			} else if (qName.equals("cnode")){ 
				String name = attributes.getValue("name");             
				String col = attributes.getValue("color");
				TreeItem<DCat> newcat = null;
				if (stack.isEmpty()){
					try { 
						dict.getCategoryRoot().getValue().setName(name); // adjust in place
					} catch (Exception ex) { /* will not happen actually */ }
					newcat = dict.getCategoryRoot();
				} else {
					try {
						newcat = dict.addCategoryToParentCategory(name, stack.peek());
						if (col != null){
							Color c = parseColor(col);
							newcat.getValue().setColor(c);
						}
					} catch (Exception ex){
						throw new SAXException(ex);
					}
				}
				stack.push(newcat);
			} else if (qName.equals("pnode")){ 
				String name = attributes.getValue("name");
				try {
					dict.addPatternToCategory(name, stack.peek());	
				} catch (Exception ex){
					throw new SAXException(ex);
				}
			}
		}

		public void endElement(String uri, String localName, String qName) 
				throws SAXException{
			if (qName.equals("cnode"))
				stack.pop();
		}
		
		public FXCatDict getCategoryDictionary(){
			return dict;
		}
	}
	
	public static FXCatDict importCategoryDictionaryFromFileVBPRO(File f) throws Exception {
		
		BufferedReader reader = null;
		FXCatDict d = null;
		try {
			InputStreamReader osw = new InputStreamReader(
					new FileInputStream(f), Charset.forName("UTF8"));
			reader = new BufferedReader(osw);
			d = new FXCatDict("New Dictionary");
			String line = null;
			d.getCategoryRoot().getValue().setName("Dictionary");
			TreeItem<DCat> currentCat = d.getCategoryRoot();
			Matcher catname = Pattern.compile(">>(.+)<<").matcher("");
			while((line = reader.readLine()) != null){
				catname.reset(line);
				if (catname.matches()){
					String newcatname = catname.group(1);
					currentCat = d.addCategoryToParentCategory(newcatname, d.getCategoryRoot());
				} else if (line.startsWith("#")){
					// pass
				} else {
					d.addPatternToCategory(line.trim(), currentCat);
				}
			}
		} finally {
			if (reader != null)
				reader.close();
		}
		return d;
	}

	public static FXCatDict importCategoryDictionaryFromFileWordstat(File f) throws Exception {
		
		BufferedReader reader = null;
		FXCatDict d = null;
		try {
			InputStreamReader osw = new InputStreamReader(
					new FileInputStream(f), Charset.forName("UTF8"));
			reader = new BufferedReader(osw);
			reader.read(); // BOM
			d = new FXCatDict("New Dictionary");
			String line = null;
			d.getCategoryRoot().getValue().setName("Dictionary");
			TreeItem<DCat> currentCat = d.getCategoryRoot();
			Matcher patname = Pattern.compile("(.+)\\(\\d+\\)").matcher("");
			while((line = reader.readLine()) != null){
				patname.reset(line);
				if (patname.matches()){
					String newpatname = patname.group(1);
					d.addPatternToCategory(newpatname.trim(), currentCat);
					
				} else if (line.startsWith("#")){
					// pass
				} else {
					String nme = line.trim();
					if (nme.startsWith("\uFEFF")) // remove BOM for UTF8
						nme = nme.substring(1);
					currentCat = d.addCategoryToParentCategory(nme, 
							d.getCategoryRoot());
				}
			}
		} finally {
			if (reader != null)
				reader.close();
		}
		return d;
	}
	
	public static FXCatDict readXmlCategoryDictionaryFromFile(File f) throws Exception {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		InputStream stream = null;
		FXCatDict d = null;
		try {
			SAXParser parser = factory.newSAXParser();
			XmlDictionaryType type = identifyDictionaryXmlFormat(f);
			if (type != null){
				if (type.equals(XmlDictionaryType.YOSHIKODER_050805)){
					YKDHandler050805 h = new YKDHandler050805();
					stream = new FileInputStream(f);
					parser.parse(stream, h);
					d = h.getCategoryDictionary();
					
				} else if (type.equals(XmlDictionaryType.LEXICODER)){
					LexicoderHandler h = new LexicoderHandler();
					stream = new FileInputStream(f);
					parser.parse(stream, h);
					d = h.getCategoryDictionary();
				
				} else {
					throw new Exception("Could not identify the dictionary format");
				}
			} else {
				throw new Exception("Could not identify the dictionary format");
			}
		} finally {
			if (stream != null)
				stream.close();
		}
		return d;
	}
		
	/////////////////////////////////////////////////////////////////////
	
	protected TreeItem<DCat> root; // need listeners most likely
	
	public FXCatDict(String dictName) {
		DCat dc = new DCat(dictName, null);
		root = new TreeItem<DCat>(dc);
		patternEngine = new SubstringPatternEngine();
	}
	
	public Set<DPat> getPatternsInSubtree(TreeItem<DCat> node){
		Set<DPat> leaves = new HashSet<DPat>();
		recursiveRake(node, leaves);
		return leaves;
	}

	public List<DPat> getSortedPatternsInSubtree(TreeItem<DCat> node){
		Set<DPat> leaves = new HashSet<DPat>();
		recursiveRake(node, leaves);
		ArrayList<DPat> arr = new ArrayList<DPat>(leaves);
		Collections.sort(arr);
		return arr;
	}
	
	public void addPatternsToCategory(Set<DPat> patterns, TreeItem<DCat> cat) {
		cat.getValue().getPatterns().addAll(patterns);
	}

	public void addPatternToCategory(DPat pattern, TreeItem<DCat> cat){
		cat.getValue().getPatterns().add(pattern);
	}
	
	// string will be split on [ ]+ and a patternengine applied to the elements
	public DPat addPatternToCategory(String el, TreeItem<DCat> cat)
			throws Exception {
		DPat pat = new DPat(el);
		cat.getValue().getPatterns().add(pat);
		return pat;
	}

	protected int findIndexFor(TreeItem<DCat> child, TreeItem<DCat> parent){
		int cc = parent.getChildren().size();
		if (cc==0){
			return 0;
		} 
		if (cc==1){ 
			return child.getValue().compareTo(parent.getChildren().get(0).getValue()) 
				<= 0 ? 0 :1; 
		}
		return findIndexFor(child, parent, 0, cc-1); // first and last
	}
	
	protected int findIndexFor(TreeItem<DCat> child, TreeItem<DCat> parent, int i1, int i2){
		if (i1==i2){
			return child.getValue().compareTo(parent.getChildren().get(i1).getValue())
				<= 0 ? i1 : i1+1;
		}
		int half = (i1 + i2) / 2;
		if (child.getValue().compareTo(parent.getChildren().get(half).getValue()) <= 0){
			return findIndexFor(child, parent, i1, half);
		}
		return findIndexFor(child, parent, half+1, i2);
	}

	protected void insertNodeAlphabeticallyInto(TreeItem<DCat> cat, 
			TreeItem<DCat> parent) throws Exception {
		for (TreeItem<DCat> item : parent.getChildren()) {
			if (cat.getValue().getName().equals( item.getValue().getName() ))
				throw new Exception(duplicateMessage);
		}
		int ind = findIndexFor(cat, parent);
		parent.getChildren().add(ind, cat);
	}
	
	protected void addCategoryToParentCategory(TreeItem<DCat> cat, 
			TreeItem<DCat> parentCat) throws Exception {
		insertNodeAlphabeticallyInto(cat, parentCat);
	}

	// use this one 
	public TreeItem<DCat> addCategoryToParentCategory(String catname, 
			TreeItem<DCat> parentCat) throws Exception {
		DCat dc = new DCat(catname, null);
		TreeItem<DCat> cat = new TreeItem<DCat>(dc);
		insertNodeAlphabeticallyInto(cat, parentCat);
		return cat;
	}

	public TreeItem<DCat> addCategoryToParentCategory(String catname, 
			Color c, TreeItem<DCat> parentCat) throws Exception {
		DCat dc = new DCat(catname, c);
		TreeItem<DCat> cat = new TreeItem<DCat>(dc);
		insertNodeAlphabeticallyInto(cat, parentCat);
		return cat;
	}
	
	public void removeCategory(TreeItem<DCat> cat){
		cat.getParent().getChildren().remove(cat); // will that work?
	}
	
	protected void recursiveRake(TreeItem<DCat> node, Set<DPat> leaves){
		Set<DPat> s = node.getValue().getPatterns();
		leaves.addAll(s);
		for (int ii = 0; ii < node.getChildren().size(); ii++)
			recursiveRake(node.getChildren().get(ii), leaves);
	}

	public Set<DPat> getPatterns(TreeItem<DCat> node){
		return node.getValue().getPatterns();
	}
	
	public List<DPat> getSortedPatterns(TreeItem<DCat> node){
		Set<DPat> s = node.getValue().getPatterns();		
		ArrayList<DPat> arr = new ArrayList<DPat>(s);
		Collections.sort(arr);
		return arr;
	}
	
	public TreeItem<DCat> getCategoryRoot(){
		return root;
	}
	
	public static String getNodePathAsString(TreeItem<DCat> node, String sep){
		List<String> arr = new ArrayList<String>();
		TreeItem<DCat> thisNode = node;
		arr.add(thisNode.getValue().getName());
		while ((thisNode = thisNode.getParent()) != null){
			arr.add(sep);
			arr.add(thisNode.getValue().getName());
		}
		Collections.reverse(arr);
		StringBuilder sb = new StringBuilder();
		for (String string : arr) 
			sb.append(string);
		return sb.toString();
		
	}
	
	private void toStringRecurse(StringBuffer sb, TreeItem<DCat> node, String sep){
		sb.append(getNodePathAsString(node, sep));
		sb.append("\n");
		List<DPat> pats = new ArrayList<DPat>(node.getValue().getPatterns());
		Collections.sort(pats);
		for (DPat pat : pats)
			sb.append("\t[" + pat.toString() + "]\n");
		for (TreeItem<DCat> titem : node.getChildren()) 
			toStringRecurse(sb, titem, sep);			
	}
	
	public IPatternEngine getPatternEngine() {
		return patternEngine;
	}
	
	private String getDictionaryXMLHeader(){
		return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
	}
	
	private String escapeXML(String s){
		return StringEscapeUtils.escapeXml10(s);
	}
	
	private String escapeColorRGB(Color cc){
		return cc.getRed() + " " + cc.getGreen() + " " + cc.getBlue();
	}

	private String toXml(TreeItem<DCat> n, boolean close) {
		if (close)
			return "</cnode>\n";
		
		StringBuilder str = new StringBuilder("<cnode");
		str.append(" name=\"" + escapeXML(n.getValue().getName()) + '"');
		Color cc = n.getValue().getColor();
		if (cc != null)
			str.append(" color=\"" + escapeColorRGB(cc) + '"');
		str.append(">\n");
		return str.toString();
	}

	private String patternsToXml(TreeItem<DCat> n) {
		Set<DPat> pats = n.getValue().getPatterns();
		StringBuilder str = new StringBuilder();
		for (DPat dictionaryPattern : pats) {
			str.append("<pnode name=\"" + 
					escapeXML(dictionaryPattern.getName()) + 
					"\"/>\n");
		}
		return str.toString();
	}
	
	private void toXmlRecurse(StringBuffer sb, TreeItem<DCat> node){
		sb.append(toXml(node, false));
		if (node.getValue().getPatterns().size() > 0)
			sb.append(patternsToXml(node));
	
		for (TreeItem<DCat> cat : node.getChildren()) {
			toXmlRecurse(sb, cat);
		}
		sb.append(toXml(node, true));
	}
	
	public String toXml(boolean addHeader){
		StringBuffer sb = new StringBuffer();
		
		if (addHeader)
			sb.append(getDictionaryXMLHeader());
		
		sb.append("<dictionary style=\"050805\" patternengine=\"substring\">\n");
		toXmlRecurse(sb, getCategoryRoot());
		sb.append("</dictionary>");
		
		return sb.toString();
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		toStringRecurse(sb, getCategoryRoot(), ">");
		return sb.toString();
	}
	
	public static void main(String[] args) throws Exception {
		File f = new File("/Users/will/Documents/scratch/2007_abortion_dictionary.ykd");
		FXCatDict dict = FXCatDict.readXmlCategoryDictionaryFromFile(f);
        System.out.println(dict);
	}

}
	

