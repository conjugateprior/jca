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

public class MatchExpander extends AbstractCounter {

	// pattern[match1,match2,match3]
	
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
			for (DPat dPat : pats) {
				List<int[]> lst = 
					idoc.getCharacterOffsetsForPattern(dPat.getRegexps());
				if (lst.size()>0){
					Set<String> matches = map.get(dPat);
					for (int[] is : lst) {
						String str = idoc.getText().substring(is[0], is[1]);
						matches.add(str);
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
				writer.write("[");
				writer.write(StringUtils.join(st, ","));
				writer.write("]");
				writer.newLine();
				writer.flush();
			}
		}
	}
	
	public static void main(String[] args) throws Exception {
		String dir = "/Users/will/Dropbox/teaching/mainz/labs/data/abortion-debate-by-para/";
		File[] fls = (new File(dir)).listFiles();
		File d = new File("/Users/will/Dropbox/teaching/texas/dictionaries/bara-et-al.ykd");
		MatchExpander ex = new MatchExpander(d, fls);
		ex.setOutputFolder("matched_patterns.txt");
		ex.processFiles();
	}

}
