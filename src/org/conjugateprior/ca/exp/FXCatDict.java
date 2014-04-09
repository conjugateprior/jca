package org.conjugateprior.ca.exp;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import javafx.scene.control.TreeItem;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.conjugateprior.ca.IPatternEngine;
import org.conjugateprior.ca.SubstringPatternEngine;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class FXCatDict {

	protected static String duplicateMessage = 
			"There is already a category with that name under this parent category";
	
	// TODO check this makes sense as a static thing
	public static IPatternEngine patternEngine;
	
	public static class YKDHandler050805 extends DefaultHandler {
		private Stack<TreeItem<DCat>> stack = new Stack<TreeItem<DCat>>();
		private FXCatDict dict;

		public void startElement(String uri, String localName, String qName,
				Attributes attributes) throws SAXException{
			if (qName.equals("dictionary")){ 
				dict = new FXCatDict("New Dictionary"); // substring pattern engine by default
			} else if (qName.equals("cnode")){ 
				String name = attributes.getValue("name");             
				TreeItem<DCat> newcat = null;
				if (stack.isEmpty()){
					try { 
						dict.getCategoryRoot().getValue().name = name; // adjust in place
					} catch (Exception ex) { /* will not happen actually */ }
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
		
		public FXCatDict getCategoryDictionary(){
			return dict;
		}
	}
	
	public static FXCatDict readCategoryDictionaryFromFile(File f) throws Exception {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		InputStream stream = null;
		FXCatDict d = null;
		try {
			SAXParser parser = factory.newSAXParser();
			YKDHandler050805 h = new YKDHandler050805();
			stream = new FileInputStream(f);
			parser.parse(stream, h);
			d = h.getCategoryDictionary();
			return d;
		} catch (Exception e){
			throw e; // re-throw
		} finally {
			if (stream != null)
				stream.close();
		}
	}
	
	protected TreeItem<DCat> root; // need listeners most likely
	
	public FXCatDict(String dictName) {
		DCat dc = new DCat();
		dc.name = dictName;
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
		cat.getValue().patterns.addAll(patterns);
	}

	public void addPatternToCategory(DPat pattern, TreeItem<DCat> cat){
		cat.getValue().patterns.add(pattern);
	}
	
	// string will be split on [ ]+ and a patternengine applied to the elements
	public DPat addPatternToCategory(String el, TreeItem<DCat> cat)
			throws Exception {
		DPat pat = new DPat(el);
		cat.getValue().patterns.add(pat);
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
		DCat dc = new DCat();
		dc.name = catname;
		TreeItem<DCat> cat = new TreeItem<DCat>(dc);
		insertNodeAlphabeticallyInto(cat, parentCat);
		return cat;
	}

	public TreeItem<DCat> addCategoryToParentCategory(String catname, 
			Color c, TreeItem<DCat> parentCat) throws Exception {
		DCat dc = new DCat();
		dc.name = catname;
		dc.color = c;
		TreeItem<DCat> cat = new TreeItem<DCat>(dc);
		insertNodeAlphabeticallyInto(cat, parentCat);
		return cat;
	}
	
	public void removeCategory(TreeItem<DCat> cat){
		cat.getParent().getChildren().remove(cat); // will that work?
	}
	
	protected void recursiveRake(TreeItem<DCat> node, Set<DPat> leaves){
		Set<DPat> s = node.getValue().patterns;
		leaves.addAll(s);
		for (int ii = 0; ii < node.getChildren().size(); ii++)
			recursiveRake(node.getChildren().get(ii), leaves);
	}

	public Set<DPat> getPatterns(TreeItem<DCat> node){
		return node.getValue().patterns;
	}
	
	public List<DPat> getSortedPatterns(TreeItem<DCat> node){
		Set<DPat> s = node.getValue().patterns;		
		ArrayList<DPat> arr = new ArrayList<DPat>(s);
		Collections.sort(arr);
		return arr;
	}
	
	public TreeItem<DCat> getCategoryRoot(){
		return root;
	}
	
	public String getNodePathAsString(TreeItem<DCat> node, String sep){
		List<String> arr = new ArrayList<String>();
		TreeItem<DCat> thisNode = node;
		arr.add(thisNode.getValue().name);
		while ((thisNode = thisNode.getParent()) != null){
			arr.add(sep);
			arr.add(thisNode.getValue().name);
		}
		Collections.reverse(arr);
		StringBuilder sb = new StringBuilder();
		for (String string : arr) 
			sb.append(string);
		return sb.toString();
		
	}
	
	private void toStringRecurse(StringBuffer sb, TreeItem<DCat> node){
		sb.append(getNodePathAsString(node, ":"));
		sb.append("\n");
		for (DPat pat : node.getValue().patterns)
			sb.append("\t[" + pat.toString() + "]\n");
		for (TreeItem<DCat> titem : node.getChildren()) 
			toStringRecurse(sb, titem);			
	}
	
	public IPatternEngine getPatternEngine() {
		return patternEngine;
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		toStringRecurse(sb, getCategoryRoot());
		return sb.toString();
	}
	
	public static void main(String[] args) throws Exception {
		File f = new File("/Users/will/Documents/scratch/2007_abortion_dictionary.ykd");
		FXCatDict dict = FXCatDict.readCategoryDictionaryFromFile(f);
        System.out.println(dict);
	}

}
	

