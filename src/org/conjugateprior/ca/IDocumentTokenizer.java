package org.conjugateprior.ca;

import java.util.Locale;

public interface IDocumentTokenizer {

	int[][] getWordOffsets(String txt) throws Exception;
	
	int[][] getSentenceOffsets(String txt) throws Exception;
	
	Locale getLocale();
	
}
