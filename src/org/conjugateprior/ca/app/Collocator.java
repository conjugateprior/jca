package org.conjugateprior.ca.app;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.linear.OpenMapRealMatrix;
import org.conjugateprior.ca.AbstractYoshikoderDocument;
import org.conjugateprior.ca.DocumentTokenizer;
import org.conjugateprior.ca.RegexpDocumentTokenizer;
import org.conjugateprior.ca.YoshikoderDocument;
import org.conjugateprior.ca.SimpleDocumentTokenizer;
import org.conjugateprior.ca.SimpleYoshikoderDocument;

public class Collocator extends AbstractCounter {

	boolean lowercase = true;
	
	protected Map<String,Integer> wordIndex = new HashMap<String,Integer>();
	
	public Collocator() {
		super();
	}

	// sentence breaks first, then accurate statistics 
	// of surprise and coincidence
	
	@Override
	public void processFiles() throws Exception {
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
			for (String wd : idoc.getWordTypes())
				wordIndex.put(wd, 0); // for now
			
			if (!getSilent())
				System.err.print(".");
		}
		int index = 0; // index th rows/columns
		for (String wd : wordIndex.keySet()){
			wordIndex.put(wd, index); 
			index++;
		}
		int V = wordIndex.size();
		OpenMapRealMatrix mat = new OpenMapRealMatrix(V, V);
		
		for (File f : files) {
			YoshikoderDocument idoc = 
					new SimpleYoshikoderDocument(f.getName(), 
							AbstractYoshikoderDocument.getTextFromFile(f, encoding),
							null, tok);
			for (int ii = 0; ii < idoc.getSentenceCount(); ii++) {
				int[] offs = idoc.getCharacterOffsetsForSentenceIndex(index);
				String offsent = idoc.getText().substring(offs[0], offs[1]).trim();
				if (offsent.length() < 3)
					continue; // heuristic
				int[][] wdo = tok.getWordOffsets(offsent);
				if (wdo.length < 2)
					continue; // sentences must be at least two words long
				for (int jj = 1; jj < wdo.length; jj++) {
					String wordbefore = offsent.substring(wdo[jj-1][0], wdo[jj-1][1]);
					String word = offsent.substring(wdo[jj][0], wdo[jj][1]);
					
				}
				
			}	
				
			for (String wd : idoc.getWordTypes())
				wordIndex.put(wd, 0); // for now
			
			if (!getSilent())
				System.err.print(".");
		}
		
		
		
		
	}
	
	public static void main(String[] args) {
		OpenMapRealMatrix mat = new OpenMapRealMatrix(5, 3);
		mat.addToEntry(1, 2, 3);
		System.err.println(mat);
	}
}
