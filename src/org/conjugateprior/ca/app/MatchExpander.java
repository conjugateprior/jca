package org.conjugateprior.ca.app;

import java.io.BufferedWriter;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.conjugateprior.ca.AbstractYoshikoderDocument;
import org.conjugateprior.ca.DPat;
import org.conjugateprior.ca.FXCategoryDictionary;
import org.conjugateprior.ca.IYoshikoderDocument;
import org.conjugateprior.ca.SimpleDocumentTokenizer;
import org.conjugateprior.ca.SimpleYoshikoderDocument;

// For the Oli and Stuart project

public class MatchExpander extends AbstractCounter {

	// pattern[match1,match2,match3]
	
	public MatchExpander(FXCategoryDictionary dict, File[] docs) throws Exception {
		super();
		setDictionary(dict);
		setFiles(docs);
	}

	
	public MatchExpander(File dictFile, File[] docs) throws Exception {
		super();
		setDictionary(dictFile);
		setFiles(docs);
	}

	@Override
	public void processFiles() throws Exception {
		FXCategoryDictionary dict = getDictionary();
		Set<DPat> pats = dict.getPatternsInSubtree(dict.getCategoryRoot());
		//System.err.println(pats);
		Map<DPat,Set<String>> map = new HashMap<DPat,Set<String>>(pats.size());
		for (DPat dPat : pats)
			map.put(dPat, new HashSet<String>());
		
		SimpleDocumentTokenizer tok = new SimpleDocumentTokenizer(locale);
		for (File f : files) {
			IYoshikoderDocument idoc = 
					new SimpleYoshikoderDocument(f.getName(), 
							AbstractYoshikoderDocument.getTextFromFile(f, encoding),
							null, tok);	
			System.err.println("Processing " + idoc.getTitle());
			for (DPat dPat : pats) {
				List<int[]> lst = 
					idoc.getCharacterOffsetsForPattern(dPat.getRegexps());
				if (lst.size()>0){
					Set<String> matches = map.get(dPat);
					for (int[] is : lst) {
						String str = idoc.getText().substring(is[0], is[1]);
						matches.add(str.toLowerCase());
					}
					map.put(dPat, matches);
				}
			}
		}
		
		// no control over location
		try (BufferedWriter writer = 
				getBufferedWriter(outputFolder)){ // make sure it's really a file
			for (DPat dPat : pats) {
				writer.write(dPat.getName());
				Set<String> st = map.get(dPat);
				if (st.size()>0){
					writer.write("\t");
					writer.write(StringUtils.join(st, "\t"));
				}//writer.write("]");
				writer.newLine();
			}
			writer.flush();
		}
	}
	
	public static void main(String[] args) throws Exception {
		String dir = "/Users/will/Desktop/ep-split-up/spli/"; //"/Users/will/Dropbox/teaching/mainz/labs/data/abortion-debate-by-para/";
		File[] fls = (new File(dir)).listFiles();
		//String dictfname = "/Users/will/Dropbox/teaching/mainz/labs/data/bara-et-al.ykd";
		/*
		FXCategoryDictionary dict = new FXCategoryDictionary("test");
		TreeItem<DCat> cat = 
				dict.addCategoryToParentCategory("first", dict.getCategoryRoot());
		dict.addPatternToCategory("the*", cat);
		dict.addPatternToCategory("honourab*", cat);
		*/
		String dictfname = "/Users/will/Desktop/ep-split-up/here/dpos.ykd";		
		File d = new File(dictfname);		
		MatchExpander ex = new MatchExpander(d, fls);
		ex.setOutputFolder("matched_positive.txt");
		ex.processFiles();
		
		dictfname = "/Users/will/Desktop/ep-split-up/here/dneg.ykd";		
		d = new File(dictfname);		
		ex = new MatchExpander(d, fls);
		ex.setOutputFolder("matched_negative.txt");
		ex.processFiles();
	}
	

}
