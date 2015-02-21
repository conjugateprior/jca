package org.conjugateprior.ca;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SimpleYoshikoderDocument 
	extends AbstractYoshikoderDocument {

	protected String text; // simple because it just keep the string in memory
	
	public SimpleYoshikoderDocument(String docTitle, String txt, Date d,
			IDocumentTokenizer tok) throws Exception {
		super(docTitle, txt, d, tok);
		text = txt;
	}

	@Override
	public Set<String> getWordTypes() {
		return tokenization.getWordTypes();
	}

	@Override
	public int getDocumentLength() {
		return tokenization.getDocumentLength();
	}

	@Override
	public String getTokenAtIndex(int index) {
		return tokenization.getTokenAtIndex(index);
	}

	@Override
	public Set<Integer> getAllMatchingTokenIndexesForWordType(String word) {
		return tokenization.getAllMatchingTokenIndexesForWordType(word);
	}

	@Override
	public List<int[]> getCharacterOffsetsForWordType(String word) {
		return tokenization.getCharacterOffsetsForWordType(word);
	}

	@Override
	public List<int[]> getConcordanceCharacterOffsetsForWordType(String wd,
			int window) {
		return tokenization.getConcordanceCharacterOffsetsForWordType(wd, window);
	}

	@Override
	public int[] getOffsetsForTokenIndex(int index) {
		return tokenization.getOffsetsForTokenIndex(index);
	}

	@Override
	public int[] getOffsetsForSentenceIndex(int index) {
		return tokenization.getOffsetsForSentenceIndex(index);
	}

	public int getSentenceCount(){
		return tokenization.getSentenceCount();
	}
	
	@Override
	public Map<String, Integer> getWordCountMap() {
		return tokenization.getWordCountMap();
	}

	@Override
	public void setTitle(String newTitle) {
		title = newTitle;
	}
	
	@Override
	protected String loadText() throws IOException {
		return text;
	}
	
	public static void main(String[] args) throws Exception {
		String doc = AbstractYoshikoderDocument.getTextFromFile(
				new File("/Users/will/Dropbox/shared/sop/European Parliament Corpus/EPcorpusEN_DE/europarl-v7.de-en.en"),
				Charset.forName("UTF8"));
		
	}
	
}
