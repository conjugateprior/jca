package org.conjugateprior.ca;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public interface ITokenization {

	public abstract Set<String> getWordTypes();

	public abstract int getWordCount();

	public abstract String getWordAtIndex(int index);

	// unordered, for reporting purposes
	public abstract Set<Integer> getWordIndexesForWordType(
			String word);

	public abstract List<int[]> getCharacterOffsetsForWordType(String word);

	public abstract List<int[]> getConcordanceCharacterOffsetsForWordType(
			String wd, int window);

	public abstract List<int[]> getConcordanceWordIndexOffsetsForWordType(
			String wd, int window);

	public abstract int[] getCharacterOffsetsForWordIndex(int index);

	public abstract int[] getCharacterOffsetsForSentenceIndex(int index);

	public abstract int getSentenceCount();
	
	// window is in words, offsets are in characters indexes as a 4-tuple
	public abstract List<int[]> getConcordanceCharacterOffsetsForPattern(
			Pattern[] pat, int window);
	
	// window is in words, offsets are in characters indexes as a 4-tuple
	public abstract List<int[]> getConcordanceWordIndexOffsetsForPattern(
				Pattern[] pat, int window);

	public abstract List<int[]> getCharacterOffsetsForPattern(Pattern[] pat);

	// unordered, for reporting purposes
	public abstract Set<Integer> getWordIndexesForPattern(
			Pattern[] pat);

	public abstract Map<String, Integer> getWordCountMap();

}