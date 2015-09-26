package org.conjugateprior.ca;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import javafx.scene.paint.Color;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.lang3.StringEscapeUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class FXCategoryDictionary {
	
	public static PatternEngine patternEngine;
	
	public enum XmlDictionaryType {
	    LEXICODER, YOSHIKODER_050805 
	}
	
	public static class FXCatDictXMLPrinter {
		
		static String xmlHeader = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
	
		private FXCategoryDictionary dict;
		
		public FXCatDictXMLPrinter(FXCategoryDictionary d) {
			dict = d;
		}
		
		// always add a header when we print to file
		public void printToFile050805(File f) throws Exception {			
			String s = printToString050805(true);
			try (
				OutputStreamWriter osw = new OutputStreamWriter(
					new FileOutputStream(f), Charset.forName("UTF-8"));
			){
				osw.write(s);
			}			
		}
		
		public String printToString050805(boolean addHeader){
			StringBuffer sb = new StringBuffer();
			if (addHeader)
				sb.append(xmlHeader);
			sb.append("<dictionary style=\"050805\" patternengine=\"substring\">\n");
			toXmlRecurse(sb, dict.getCategoryRoot());
			sb.append("</dictionary>");
			return sb.toString();
		}
		
		private static String toXml(TreeItem<DCat> n, boolean close) {
			if (close)
				return "</cnode>\n";
			
			StringBuilder str = new StringBuilder("<cnode");
			str.append(" name=\"" + StringEscapeUtils.escapeXml10(n.getValue().getName()) + '"');
			Color cc = n.getValue().getColor();
			if (cc != null)
				str.append(" color=\"" + 
						cc.getRed() + " " + cc.getGreen() + " " + cc.getBlue() + '"');
			str.append(">\n");
			return str.toString();
		}

		private static String patternsToXml(TreeItem<DCat> n) {
			Set<DPat> pats = n.getValue().getPatterns();
			StringBuilder str = new StringBuilder();
			for (DPat dictionaryPattern : pats) {
				str.append("<pnode name=\"" + 
						StringEscapeUtils.escapeXml10(dictionaryPattern.getName()) + 
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
		SAXParser parser = factory.newSAXParser();
		XMLDictionaryTypeIdentifier identifier = new XMLDictionaryTypeIdentifier();
		
		try (
			InputStream stream = new FileInputStream(f);
		){
			parser.parse(stream, identifier);
		}
		return identifier.getDictionaryType();
	}
	
	public static class LexicoderHandler extends DefaultHandler {
		private Stack<TreeItem<DCat>> stack = new Stack<TreeItem<DCat>>();
		private FXCategoryDictionary dict;

		public void startElement(String uri, String localName, String qName,
				Attributes attributes) throws SAXException{
			if (qName.equals("dictionary")){ 
				dict = new FXCategoryDictionary("New Dictionary"); // substring pattern engine by default
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
					// trim because Lexicoder seems to add a leading space
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
		
		public FXCategoryDictionary getCategoryDictionary(){
			return dict;
		}
	}	
	
	public static class YKDHandler050805 extends DefaultHandler {
		private Stack<TreeItem<DCat>> stack = new Stack<TreeItem<DCat>>();
		private FXCategoryDictionary dict;

		static private Color parseColor(String s) throws Exception {
			String[] vv = s.split(" ");
			return new Color(Double.parseDouble(vv[0]), 
					         Double.parseDouble(vv[1]),
							 Double.parseDouble(vv[2]),
							 1); // opacity 1
		}
		
		public void startElement(String uri, String localName, String qName,
				Attributes attributes) throws SAXException{
			if (qName.equals("dictionary")){ 
				dict = new FXCategoryDictionary("New Dictionary"); // substring pattern engine by default
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
					newcat = dict.addCategoryToParentCategory(name, stack.peek());
					if (col != null){
						try {
							Color c = parseColor(col);
							newcat.getValue().setColor(c);
						} catch (Exception ex){
							/*  */
						}
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
		
		public FXCategoryDictionary getCategoryDictionary(){
			return dict;
		}
	}
	
	// the .dic files that LIWC uses
	public static FXCategoryDictionary importCategoryDictionaryFromFileLIWC(File f) throws Exception {
		
		Matcher pm = Pattern.compile("(\\d+)\\t(\\w+)\\t*").matcher("");
		Matcher en = Pattern.compile("([\\w*]+)([\\t\\d+]+)").matcher("");

		FXCategoryDictionary dict = new FXCategoryDictionary(f.getName());
		try (
				InputStreamReader osw = new InputStreamReader(
						new FileInputStream(f), Charset.forName("UTF8"));
				BufferedReader reader = new BufferedReader(osw);
			){

			Map<Integer,TreeItem<DCat>> id2cat = 
					new HashMap<Integer,TreeItem<DCat>>();
			String line = null;
			while ((line = reader.readLine()) != null){
				pm.reset(line);
				en.reset(line);

				if (pm.matches()){
					TreeItem<DCat> cat = new TreeItem<DCat>(new DCat(pm.group(2), null));
					id2cat.put(Integer.parseInt(pm.group(1)), cat);
					dict.addCategoryToParentCategory(cat,
							dict.getCategoryRoot());
				} else if (en.matches()){					
					String wd = en.group(1);
					String[] ids = en.group(2).split("\t");
					for (String str : ids) {
						if (!str.equals("")){
							TreeItem<DCat> cat = id2cat.get(Integer.parseInt(str));
							dict.addPatternToCategory(wd, cat);
						}
					}
				} else {
					//
				}
			}
		} 
		return dict;
	}
	
	// do the renaming in the load function
	public static FXCategoryDictionary deDuplicateCategoryNames(FXCategoryDictionary dict) {
		System.out.println( dict.getCategoryNodesInPrintOrder());
		for (TreeItem<DCat> cat : dict.getCategoryNodesInPrintOrder()) {
			int counter = 1;
			Set<String> usedNames = new HashSet<String>();
			for (TreeItem<DCat> item : cat.getChildren()) {
				String itemName = item.getValue().getName();
				if (usedNames.contains(itemName)){
					String nname = itemName + "(" + counter + ")";
					item.getValue().setName(nname);
					counter++;
					usedNames.add(nname);
				} else { 
					usedNames.add(itemName);
				}
			}
		}
		return dict;
	}
	
	public static FXCategoryDictionary importCategoryDictionaryFromFileVBPRO(File f) throws Exception {
		
		FXCategoryDictionary d = null;
		try (
			InputStreamReader osw = new InputStreamReader(
					new FileInputStream(f), Charset.forName("UTF8"));
			BufferedReader reader = new BufferedReader(osw);
		){
			d = new FXCategoryDictionary(f.getName());
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
		}
		return d;
	}

	public static FXCategoryDictionary importCategoryDictionaryFromFileWordstat(File f) throws Exception {
		
		FXCategoryDictionary d = null;
		try (
			InputStreamReader osw = new InputStreamReader(
					new FileInputStream(f), Charset.forName("UTF8"));
			BufferedReader reader = new BufferedReader(osw);
		){
			reader.read(); // BOM
			d = new FXCategoryDictionary(f.getName());
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
		}
		return d;
	}
	
	public static FXCategoryDictionary readXmlCategoryDictionaryFromFile(File f) throws Exception {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser parser = factory.newSAXParser();
		XmlDictionaryType type = identifyDictionaryXmlFormat(f);
		
		FXCategoryDictionary d = null;
		try (
			InputStream stream = new FileInputStream(f)
		){	
			if (type != null){
				if (type.equals(XmlDictionaryType.YOSHIKODER_050805)){
					YKDHandler050805 h = new YKDHandler050805();					
					parser.parse(stream, h);
					d = h.getCategoryDictionary();
					
				} else if (type.equals(XmlDictionaryType.LEXICODER)){
					LexicoderHandler h = new LexicoderHandler();
					parser.parse(stream, h);
					d = h.getCategoryDictionary();
				
				} else {
					throw new Exception("Could not identify the dictionary format");
				}
			} else {
				throw new Exception("Could not identify the dictionary format");
			}
		} 
		return d;
	}
		
	/////////////////////////////////////////////////////////////////////
	
	class DuplicateCategoryException extends Exception {
		private static final long serialVersionUID = 1L;

		public DuplicateCategoryException() {
			super();
		}
		
		@Override
		public String getMessage() {
			return "There is already a category with that name under this parent category"; 
		}
	}
	
	protected TreeItem<DCat> root; // need listeners most likely
	
	public FXCategoryDictionary(String dictName) {
		DCat dc = new DCat(dictName, null);
		root = new TreeItem<DCat>(dc);
		patternEngine = new SubstringPatternEngine();
	}
	
	// 
	boolean checkNoDuplicates(TreeItem<DCat> parent, TreeItem<DCat> cat){
		boolean isDuplicate = true;
		for (TreeItem<DCat> item : parent.getChildren()) {
			if (cat.getValue().getName().equals( item.getValue().getName() )){
				isDuplicate = false;
				break;
			}
		}
		return isDuplicate;
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
	// patterns already present are implicitly dropped
	public DPat addPatternToCategory(String el, TreeItem<DCat> cat)
			throws Exception {
		DPat pat = new DPat(el);
		Set<DPat> pats = cat.getValue().getPatterns();
		pats.add(pat);
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

	protected boolean hasChildNamed(ObservableList<TreeItem<DCat>> ch, String name){
		for (TreeItem<DCat> treeItem : ch) {
			if (treeItem.getValue().getName().equals(name))
				return true;
		}
		return false;
	}
	
	protected void insertNodeAlphabeticallyInto(TreeItem<DCat> cat, 
			TreeItem<DCat> parent) {
		
		String name = cat.getValue().getName();
		if (hasChildNamed(parent.getChildren(), name)){
			int counter = 1;
			while (hasChildNamed(parent.getChildren(), name + "(" + counter + ")"))
				counter++;
			cat.getValue().setName(name + "(" + counter + ")");
		}
		int ind = findIndexFor(cat, parent);
		parent.getChildren().add(ind, cat);
	}
	
	public void addCategoryToParentCategory(TreeItem<DCat> cat, 
			TreeItem<DCat> parentCat) {
		insertNodeAlphabeticallyInto(cat, parentCat);
	}

	// use this one 
	public TreeItem<DCat> addCategoryToParentCategory(String catname, 
			TreeItem<DCat> parentCat) {
		DCat dc = new DCat(catname, null);
		TreeItem<DCat> cat = new TreeItem<DCat>(dc);
		insertNodeAlphabeticallyInto(cat, parentCat);
		return cat;
	}

	public TreeItem<DCat> addCategoryToParentCategory(String catname, 
			Color c, TreeItem<DCat> parentCat) {
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
	
	public PatternEngine getPatternEngine() {
		return patternEngine;
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		toStringRecurse(sb, getCategoryRoot(), ">");
		return sb.toString();
	}
	
	private void recurseCategories(List<TreeItem<DCat>> sb, TreeItem<DCat> n){
		sb.add(n);
		for (TreeItem<DCat> treeItem : n.getChildren()) 
			recurseCategories(sb, treeItem);
	}
	
	public List<TreeItem<DCat>> getCategoryNodesInPrintOrder(){
		TreeItem<DCat> n = getCategoryRoot();
		List<TreeItem<DCat>> list = new ArrayList<TreeItem<DCat>>();
		recurseCategories(list, n);
		return list;
	}	
	
	public static void main(String[] args) throws Exception {
		//File f = new File("/Users/will/Documents/scratch/2007_abortion_dictionary.ykd");
		//FXCategoryDictionary dict = FXCategoryDictionary.readXmlCategoryDictionaryFromFile(f);
        //System.out.println(dict);
        
		//File f = new File("/Users/will/Dropbox/shared/SOP/Paper/repl/Corrected Translations with Inflections/dict-fr-from-ra.ykd");
		//FXCategoryDictionary dict = FXCategoryDictionary.readXmlCategoryDictionaryFromFile(f);
        //System.out.println(dict);
		
        //f = new File("/Applications/LIWC2007/Dictionaries/LIWC2001_German.dic");
		//dict = FXCategoryDictionary.importCategoryDictionaryFromFileLIWC(f);
        //System.out.println(dict);
        
        FXCategoryDictionary d = new FXCategoryDictionary("fg");
        d.addCategoryToParentCategory("thing", d.getCategoryRoot());
        d.addCategoryToParentCategory("thingy", d.getCategoryRoot());
        TreeItem<DCat> item = d.addCategoryToParentCategory("thing", d.getCategoryRoot());
        d.addPatternToCategory("pattern thing", item);
        d.addCategoryToParentCategory("thing", d.getCategoryRoot());
        System.out.println(d);
        
        d = deDuplicateCategoryNames(d);
        System.out.println(d);
        
	}

}
	

