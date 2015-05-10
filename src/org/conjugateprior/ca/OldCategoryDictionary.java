package org.conjugateprior.ca;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.lang3.StringEscapeUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

// this functionality is duplicated (hopefully) by FXCatDict

/**
 * The old non TreeItem version of a yoshikoder category dictionary
 * @author will
 * @deprecated
 */
public class OldCategoryDictionary extends DefaultTreeModel {

	private static final long serialVersionUID = -6071338642400002382L;

	protected String duplicateMessage = 
			"There is already a category with that name under this parent category";
	protected PatternEngine patternEngine;
	
	// this is not any kind of tree node
	public class DictionaryPattern implements Comparable<DictionaryPattern>{
		
		private String[] elements;
		private String name;
		private Pattern[] regexps;
	
		public DictionaryPattern(String s){
			elements = s.split("[ ]+");
			regexps = patternEngine.makeRegexp(elements);
			fixName();
		}
		
		public String[] getElements() {
			return elements;
		}
		
		public Pattern[] getRegexps() {
			return regexps;
		}
		
		protected void fixName(){
			if (elements.length > 1){
				StringBuffer sb = new StringBuffer();
				for (String s: elements) {
					sb.append(s);
					sb.append(" ");
				}
				name = sb.subSequence(0, sb.length()-1).toString();
			} else {
				name = elements[0];
			}			
		}
		
		public String getName() {
			return name;
		}
		
		public String toString() {
			return name;
		}

		public int compareTo(DictionaryPattern o) {
			return this.toString().compareTo(o.toString());
		}
		
	}
	
	public class DictionaryCategory extends DefaultMutableTreeNode 
	                         implements Comparable<DictionaryCategory>{
		
		private static final long serialVersionUID = -7535185037037073347L;
		
		protected String name;
		protected Set<DictionaryPattern> patterns;
		protected Color color = null;
		
		// for computing stuff
		private Set<Integer> matchedIndices;
		
		public DictionaryCategory(String s) {
			super();
			name = s;
			patterns = new HashSet<DictionaryPattern>();
		
			matchedIndices = new HashSet<Integer>();
		}
		
		public Set<Integer> getMatchedIndices(){
			return matchedIndices;
		}
		
		public DictionaryCategory(String s, Color c){
			this(s);
			color = c;
		}
		
		public Color getColor(){
			return color;
		}
		public void setColor(Color newcol){
			color = newcol;
		}
		
		public Set<DictionaryPattern> getPatterns() {
			return patterns;
		}

		public void setPatterns(Set<DictionaryPattern> pats) {
			patterns = pats;
		}

		public String getName() {
			return name;
		}
		
		public void setName(String n) {
			name = n;
		}
		
		public String toString() {
			return name;
		}
		
		@Override
		public int compareTo(DictionaryCategory o) {
			return this.name.compareTo(o.getName());
		}
		
		public void setMatchedIndices(Set<Integer> s){
			matchedIndices = s;
		}
		
		public void addMatchedIndices(Set<Integer> s){
			matchedIndices.addAll(s);
		}
		
		public String getPathAsString(String sep){
	    	TreeNode[] ns = getPath();
	        StringBuffer sb = new StringBuffer();
	        for (TreeNode treeNode : ns) {
	        	DictionaryCategory cat = (DictionaryCategory)treeNode;
	        	sb.append(cat.toString());
	        	sb.append(sep);
			}
	        String s = sb.toString();
	    	return s.substring(0, s.length()-sep.length());
	    }
	}
	
	public enum XmlDictionaryType {
	    LEXICODER, YOSHIKODER_050805 
	}
	
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

	public static class LexicoderHandler extends DefaultHandler {
		private Stack<DictionaryCategory> stack = new Stack<DictionaryCategory>();
		private OldCategoryDictionary dict;

		public void startElement(String uri, String localName, String qName,
				Attributes attributes) throws SAXException{
			if (qName.equals("dictionary")){ 
				dict = new OldCategoryDictionary(); // substring pattern engine by default
				String name = attributes.getValue("name");
				dict.getCategoryRoot().setName(name);
				stack.push(dict.getCategoryRoot());
				
			} else if (qName.equals("cnode")){ 
				String name = attributes.getValue("name");             
				DictionaryCategory newcat = null;
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
		
		public OldCategoryDictionary getCategoryDictionary(){
			return dict;
		}
	}	
	
	public static class YKDHandler050805 extends DefaultHandler {
		private Stack<DictionaryCategory> stack = new Stack<DictionaryCategory>();
		private OldCategoryDictionary dict;

		public void startElement(String uri, String localName, String qName,
				Attributes attributes) throws SAXException{
			if (qName.equals("dictionary")){ 
				dict = new OldCategoryDictionary(); // substring pattern engine by default
			} else if (qName.equals("cnode")){ 
				String name = attributes.getValue("name");             
				DictionaryCategory newcat = null;
				if (stack.isEmpty()){
					dict.getCategoryRoot().setName(name); // adjust in place
					newcat = dict.getCategoryRoot();
				} else {
					try {
						newcat = dict.addCategoryToParentCategory(name, stack.peek());
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
		
		public OldCategoryDictionary getCategoryDictionary(){
			return dict;
		}
	}
		
	public static OldCategoryDictionary importCategoryDictionaryFromFileVBPRO(File f) throws Exception {
		
		BufferedReader reader = null;
		OldCategoryDictionary d = null;
		try {
			InputStreamReader osw = new InputStreamReader(
					new FileInputStream(f), Charset.forName("UTF8"));
			reader = new BufferedReader(osw);
			d = new OldCategoryDictionary();
			String line = null;
			d.getCategoryRoot().setName("Dictionary");
			DictionaryCategory currentCat = d.getCategoryRoot();
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

	public static OldCategoryDictionary importCategoryDictionaryFromFileWordstat(File f) throws Exception {
		
		BufferedReader reader = null;
		OldCategoryDictionary d = null;
		try {
			InputStreamReader osw = new InputStreamReader(
					new FileInputStream(f), Charset.forName("UTF8"));
			reader = new BufferedReader(osw);
			reader.read(); // BOM
			d = new OldCategoryDictionary();
			String line = null;
			d.getCategoryRoot().setName("Dictionary");
			DictionaryCategory currentCat = d.getCategoryRoot();
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
	
	// return 
	public static OldCategoryDictionary readXmlCategoryDictionaryFromFile(File f) throws Exception {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		InputStream stream = null;
		OldCategoryDictionary d = null;
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
	
	public OldCategoryDictionary(DictionaryCategory root) {
		super(root);
		patternEngine = new SubstringPatternEngine();
	}

	public OldCategoryDictionary() {
		super(new DefaultMutableTreeNode());
		setRoot(new DictionaryCategory("Dict"));
		patternEngine = new SubstringPatternEngine();
	}
	
	public Set<DictionaryPattern> getPatternsInSubtree(DictionaryCategory node){
		Set<DictionaryPattern> leaves = new HashSet<DictionaryPattern>();
		recursiveRake(node, leaves);
		return leaves;
	}
	
	public List<DictionaryPattern> getSortedPatternsInSubtree(DictionaryCategory node){
		Set<DictionaryPattern> leaves = new HashSet<DictionaryPattern>();
		recursiveRake(node, leaves);
		ArrayList<DictionaryPattern> arr = new ArrayList<DictionaryPattern>(leaves);
		Collections.sort(arr);
		return arr;
	}
	
	public void addPatternsToCategory(Set<DictionaryPattern> patterns, DictionaryCategory cat) {
		cat.getPatterns().addAll(patterns);
	}

	public void addPatternToCategory(DictionaryPattern pattern, DictionaryCategory cat){
		cat.getPatterns().add(pattern);
	}
	
	// string will be split on [ ]+ and a patternengine applied to the elements
	public DictionaryPattern addPatternToCategory(String el, DictionaryCategory cat)
			throws Exception {
		DictionaryPattern pat = new DictionaryPattern(el);
		cat.getPatterns().add(pat);
		return pat;
	}

	protected int findIndexFor(DictionaryCategory child, DictionaryCategory parent){
		int cc = parent.getChildCount();
		if (cc==0){
			return 0;
		} 
		if (cc==1){
			return child.compareTo((DictionaryCategory)parent.getChildAt(0)) 
				<= 0 ? 0 :1; 
		}
		return findIndexFor(child, parent, 0, cc-1); // first and last
	}
	
	protected int findIndexFor(DictionaryCategory child, DictionaryCategory parent, int i1, int i2){
		if (i1==i2){
			return child.compareTo((DictionaryCategory)parent.getChildAt(i1))
				<= 0 ? i1 : i1+1;
		}
		int half = (i1 + i2) / 2;
		if (child.compareTo((DictionaryCategory)parent.getChildAt(half)) <= 0){
			return findIndexFor(child, parent, i1, half);
		}
		return findIndexFor(child, parent, half+1, i2);
	}

	protected void insertNodeAlphabeticallyInto(DictionaryCategory cat, 
			DictionaryCategory parent) throws Exception {
		int ind = findIndexFor(cat, parent);
		insertNodeInto(cat, parent, ind);
		/*
		@SuppressWarnings("unchecked")
		Enumeration<DictionaryCategory> en = parent.children();
		while (en.hasMoreElements()){
			DictionaryCategory child = en.nextElement();
			int comp = cat.compareTo(child);
			if (comp < 0){
				insertNodeInto(cat, parent, 0);
				return; // bail
			} else if (comp == 0){
				throw new Exception(duplicateMessage);
			} else if (comp > 0){
				int ind = getIndexOfChild(parent, child)+1;
				insertNodeInto(cat, parent, ind);
				return; // bail
			}
		}
		insertNodeInto(cat, parent, getChildCount(parent));
		*/
	}
	
	protected void addCategoryToParentCategory(DictionaryCategory cat, 
			DictionaryCategory parentCat) throws Exception {
		insertNodeAlphabeticallyInto(cat, parentCat);
	}

	// use this one 
	public DictionaryCategory addCategoryToParentCategory(String catname, 
			DictionaryCategory parentCat) throws Exception {
		DictionaryCategory cat = new DictionaryCategory(catname);
		insertNodeAlphabeticallyInto(cat, parentCat);
		return cat;
	}

	public DictionaryCategory addCategoryToParentCategory(String catname, 
			Color c, DictionaryCategory parentCat) throws Exception {
		DictionaryCategory cat = new DictionaryCategory(catname, c);
		insertNodeAlphabeticallyInto(cat, parentCat);
		return cat;
	}
	
	public void removeCategory(DictionaryCategory cat){
		removeNodeFromParent(cat);
	}
	
	protected void recursiveRake(DictionaryCategory node, Set<DictionaryPattern> leaves){
		Set<DictionaryPattern> s = node.getPatterns();
		leaves.addAll(s);
		for (int ii = 0; ii < node.getChildCount(); ii++)
			recursiveRake((DictionaryCategory)node.getChildAt(ii), leaves);
	}

	public Set<DictionaryPattern> getPatterns(DictionaryCategory node){
		return node.getPatterns();
	}
	
	public List<DictionaryPattern> getSortedPatterns(DictionaryCategory node){
		Set<DictionaryPattern> s = node.getPatterns();		
		ArrayList<DictionaryPattern> arr = new ArrayList<DictionaryPattern>(s);
		Collections.sort(arr);
		return arr;
	}
	
	public DictionaryCategory getCategoryRoot(){
		return (DictionaryCategory)getRoot();
	}
	
	private void toStringRecurse(StringBuffer sb, DictionaryCategory node){
		sb.append(node.getPathAsString(":"));
		sb.append("\n");
		for (DictionaryPattern pat : node.getPatterns()) {
			sb.append("\t[" + pat.toString() + "]\n");
		}
		@SuppressWarnings("unchecked")
		Enumeration<DictionaryCategory> en = node.children();
		while (en.hasMoreElements()) {
			OldCategoryDictionary.DictionaryCategory dc = en.nextElement();
			toStringRecurse(sb, dc);
		}
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
	
	private String toXml(DictionaryCategory n, boolean close) {
		if (close)
			return "</cnode>\n";
		
		StringBuilder str = new StringBuilder("<cnode");
		str.append(" name=\"" + escapeXML(n.getName()) + '"');
		Color cc = n.getColor();
		if (cc != null)
			str.append(" color=\"" + escapeColorRGB(cc) + '"');
		str.append(">\n");
		return str.toString();
	}

	private String patternsToXml(DictionaryCategory n) {
		Set<DictionaryPattern> pats = n.getPatterns();
		StringBuilder str = new StringBuilder();
		for (DictionaryPattern dictionaryPattern : pats) {
			str.append("<pnode name=\"" + 
					escapeXML(dictionaryPattern.getName()) + 
					"\"/>\n");
		}
		return str.toString();
	}
	
	private void toXmlRecurse(StringBuffer sb, DictionaryCategory node){
		sb.append(toXml(node, false));
		if (node.getPatterns().size() > 0)
			sb.append(patternsToXml(node));
	
		@SuppressWarnings("unchecked")
		Enumeration<DictionaryCategory> en = node.children();
		while (en.hasMoreElements()) {
			OldCategoryDictionary.DictionaryCategory dc = en.nextElement();
			toXmlRecurse(sb, dc);
		}
		
		sb.append(toXml(node, true));
	}
	
	public PatternEngine getPatternEngine() {
		return patternEngine;
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
		toStringRecurse(sb, getCategoryRoot());
		return sb.toString();
	}
	
	public static void main(String[] args) throws Exception {
		// File f = new File("/Users/will/Desktop/2007_abortion_dictionary.ykd");
		// CategoryDictionary dict = CategoryDictionary.readCategoryDictionaryFromFile(f);
		
		//File f2 = new File("/Users/will/Desktop/thing.vbpro");
		//CategoryDictionary d = CategoryDictionary.importCategoryDictionaryFromFileVBPRO(f2);
		//System.out.println(d.toXml(true));

		File f3 = new File("/Users/will/Dropbox/shared/SOP/LSD/LSD2011/LSD2011.CAT");
		OldCategoryDictionary d = OldCategoryDictionary.importCategoryDictionaryFromFileWordstat(f3);
		System.out.println(d.toXml(true).substring(0, 800));

	}

}
