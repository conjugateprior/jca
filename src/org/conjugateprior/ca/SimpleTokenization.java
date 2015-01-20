package org.conjugateprior.ca;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// TODO 
// rewrite xml out code
// reconstruct the GUI

public class SimpleTokenization implements ITokenization {
	
	protected int[][]             wordOffsets;     // each row = [start-offset, end-offset+1]
	protected int[][]             sentenceOffsets; // each row = [start-offset, end-offset+1]
	protected Map<String,List<Integer>> wordTypeToTokenNumber; // vocab words to token indexes
	protected Map<Integer,String>       tokenNumberToWordType;
	
	public SimpleTokenization(String txt, IDocumentTokenizer tok) throws Exception {
		
		wordOffsets = tok.getWordOffsets(txt);
		if (wordOffsets.length == 0)
			throw new Exception("There seem to be no word tokens in this document!");
		
		sentenceOffsets = tok.getSentenceOffsets(txt);
		wordTypeToTokenNumber = new HashMap<String,List<Integer>>();
		tokenNumberToWordType = new HashMap<Integer,String>();
		
		Locale loc = tok.getLocale();
		for (int ii = 0; ii < wordOffsets.length; ii++) {
			String wd = txt.substring(wordOffsets[ii][0], wordOffsets[ii][1])
				.toLowerCase(loc).intern();
			tokenNumberToWordType.put(ii, wd);
			if (wordTypeToTokenNumber.containsKey(wd))
				wordTypeToTokenNumber.get(wd).add(ii);
			else {
				List<Integer> lst = new ArrayList<Integer>();
				lst.add(ii);
				wordTypeToTokenNumber.put(wd, lst);
			}
		}
	}
	
	@Override
	public Set<String> getWordTypes(){
		return wordTypeToTokenNumber.keySet();
	}
	
	@Override
	public int getDocumentLength(){
		return wordOffsets.length;
	}
	
	@Override
	public String getTokenAtIndex(int index){
		if (index >= 0 && index < wordOffsets.length)
			return tokenNumberToWordType.get(index);
		return null;
	}
	
	// unordered, for reporting purposes
	@Override
	public Set<Integer> getAllMatchingTokenIndexesForWordType(String word){
		List<Integer> lst = wordTypeToTokenNumber.get(word);
		return new HashSet<Integer>(lst);
	}

	@Override
	public List<int[]> getCharacterOffsetsForWordType(String word){
		List<Integer> lst = wordTypeToTokenNumber.get(word);
		List<int[]> newlist = new ArrayList<int[]>(lst.size());
		for (Iterator<Integer> iterator = lst.iterator(); iterator.hasNext();) {
			Integer index = iterator.next();
			newlist.add(new int[]{wordOffsets[index][0], wordOffsets[index][1]});			
		}
		return newlist;
	}
	
	@Override
	public List<int[]> getConcordanceCharacterOffsetsForWordType(String wd, int window){
		int N = getDocumentLength() - 1; // index of last token
		
		List<Integer> lst = wordTypeToTokenNumber.get(wd);
		List<int[]> charoffs = new ArrayList<int[]>(lst.size());
		for (Integer startIndex : lst) {
			int endIndex = startIndex;
			int endRhs = Math.min(N, endIndex + window);
			int startRhs = Math.min(N, endIndex + 1);
			int startLhs = Math.max(0, startIndex - window);
			int endLhs = Math.max(0, startIndex - 1);
			
			charoffs.add(new int[]{
					wordOffsets[startLhs][0],
					wordOffsets[endLhs][1],
					wordOffsets[startIndex][0],
					wordOffsets[endIndex][1],
					wordOffsets[startRhs][0],
					wordOffsets[endRhs][1]}
			);
		}
		return charoffs;
	}	
	
	@Override
	public int[] getOffsetsForTokenIndex(int index){
		if (index > 0 && index < wordOffsets.length-1)
			return new int[]{wordOffsets[index][0], wordOffsets[index][1]};
		return new int[]{};
	}
	
	@Override
	public int[] getOffsetsForSentenceIndex(int index){
		if (index > 0 && index < sentenceOffsets.length-1)
			return new int[]{sentenceOffsets[index][0], sentenceOffsets[index][1]};
		return new int[]{};
	}
	
	public int getSentenceCount(){
		return sentenceOffsets.length;
	}

	// start new code

	// window is in words, offsets are in characters indexes as a 4-tuple
	@Override
	public List<int[]> getConcordanceCharacterOffsetsForPattern(Pattern[] pat, int window){
		int N = getDocumentLength() - 1; // index of last token
		
		List<Integer> lst = getStartingTokenIndexesForPattern(pat);
		List<int[]> charoffs = new ArrayList<int[]>(lst.size());
		for (Integer startIndex : lst) {
			int endIndex = startIndex + pat.length - 1;
			int endRhs = Math.min(N, endIndex + window);
			int startRhs = Math.min(N, endIndex + 1);
			int startLhs = Math.max(0, startIndex - window);
			int endLhs = Math.max(0, startIndex - 1);
			
			charoffs.add(new int[]{
					wordOffsets[startLhs][0],
					wordOffsets[endLhs][1],
					wordOffsets[startIndex][0],
					wordOffsets[endIndex][1],
					wordOffsets[startRhs][0],
					wordOffsets[endRhs][1]}
			);
		}
		return charoffs;
	}	
	
	@Override
	public List<int[]> getCharacterOffsetsForPattern(Pattern[] pat){
		List<Integer> lst = getStartingTokenIndexesForPattern(pat);
		List<int[]> offs = new ArrayList<int[]>();
		for (Integer tokIndex : lst) {
			int[] off = new int[]{wordOffsets[tokIndex][0], 
					              wordOffsets[tokIndex+pat.length-1][1]};
			offs.add(off);
		}
		return offs;
	}

	// unordered, for reporting purposes
	@Override
	public Set<Integer> getAllMatchingTokenIndexesForPattern(Pattern[] pat){
		List<Integer> lst = getStartingTokenIndexesForPattern(pat);
		if (pat.length > 1){
			List<Integer> extras = new ArrayList<Integer>();
			for (Integer integer : lst) {	
				for (int ii = 1; ii < pat.length; ii++)
					extras.add(integer + ii); // guaranteed not to fall off the end				
			}
			lst.addAll(extras);
		}
		Set<Integer> set = new HashSet<Integer>(lst);
		//System.err.println("Matching token indexes: " + set);
		return set;
	}

	// just the index of the matching first tokens
	protected List<Integer> getStartingTokenIndexesForPattern(Pattern[] pat){
		// maybe we should hand in matchers in the first place?
		Matcher[] matcher = new Matcher[pat.length];
		for (int ii = 0; ii < matcher.length; ii++)
			matcher[ii] = pat[ii].matcher("");
		
		List <Integer> lst = new ArrayList<Integer>();
		for (String wdType : wordTypeToTokenNumber.keySet()) {
			List<Integer> tokNumbers = wordTypeToTokenNumber.get(wdType);
			for (Integer tokNumber : tokNumbers) {
				if (matcher[0].reset(wdType).matches()){
					boolean fits = true;
					// check subsequent patterns, if any
					for (int ii = 1; ii < pat.length; ii++){
						if (tokNumber + ii < getDocumentLength()){
							String nextwd = tokenNumberToWordType.get(tokNumber + ii);
							if (!matcher[ii].reset(nextwd).matches()){
								fits = false;
								break;
							}
						} else {
							fits = false;
							break;
						}
					}
					if (fits){
						lst.add(tokNumber);
					}
				}
			}
		}
		return lst;
	}
	
	// end new code
	
	@Override
	public Map<String,Integer> getWordCountMap() {
		Map<String,Integer> map = new HashMap<String,Integer>();
		for (Iterator<String> iterator = wordTypeToTokenNumber.keySet().iterator(); iterator.hasNext();) {
			String wd = iterator.next();
			List<Integer> lst = wordTypeToTokenNumber.get(wd);
			map.put(wd, lst.size());
		}
		return map;
	}
	
	public static void main(String[] args) throws Exception {
		String txt =  "Mary had a little lamb.  It's fleece was white as snow. ";
		String offs = "01234567890123456789012345678901234567890123456789012345";
		String upp =  "00000000001111111111222222222233333333334444444444555555";
		String title = "Mary";
		IDocumentTokenizer tok = new SimpleDocumentTokenizer(Locale.ENGLISH);
		//Tokenization tn = new SimpleTokenization(txt, tok);
		SimpleYoshikoderDocument doc = new SimpleYoshikoderDocument(title,
				txt, new Date(20000000), tok);
		System.out.println(doc);
		System.out.println(txt);
		System.out.println(upp);
		System.out.println(offs);
		
		System.out.println( doc.getWordCounts() );
		System.out.println(Arrays.toString(doc.getOffsetsForTokenIndex(2)));
		List<int[]> lst = doc.getCharacterOffsetsForPattern(new Pattern[]{Pattern.compile("had")});
		System.out.println(lst.size());
		for (int[] is : lst) 
			System.out.println( Arrays.toString(is) );
		lst = doc.getCharacterOffsetsForPattern(new Pattern[]{Pattern.compile("had"), 
				Pattern.compile("a"), Pattern.compile("lit.*")});
		for (int[] is : lst) 
			System.out.println( Arrays.toString(is) );
		System.out.println("word 2");
		lst = doc.getConcordanceCharacterOffsetsForPattern(
				new Pattern[]{Pattern.compile("had")}, 3);
		for (int[] is : lst) 
			System.out.println( Arrays.toString(is) );
		System.out.println("pattern");
		lst = doc.getConcordanceCharacterOffsetsForPattern(
				new Pattern[]{Pattern.compile("a.*")}, 3);
		for (int[] is : lst) 
			System.out.println( Arrays.toString(is) );
		System.out.println("snow");
		lst = doc.getConcordanceCharacterOffsetsForPattern(
				new Pattern[]{Pattern.compile("snow")}, 2);
		for (int[] is : lst) 
			System.out.println( Arrays.toString(is) );
		System.out.println("nothing there");
		lst = doc.getConcordanceCharacterOffsetsForPattern(
				new Pattern[]{Pattern.compile("kll")}, 2);
		for (int[] is : lst) 
			System.out.println( Arrays.toString(is) );
		/*
		System.out.println("word 11");
		lst = tn.getConcordanceOffsetsFor(Pattern.compile("snow"), 1);
		for (int[] is : lst) 
			System.out.println( Arrays.toString(is) );
		System.out.println("word middle window 2");
		lst = tn.getConcordanceOffsetsFor(Pattern.compile("fleece"), 2);
		for (int[] is : lst) 
			System.out.println( Arrays.toString(is) );
		System.out.println("word middle window too large");
		lst = tn.getConcordanceOffsetsFor(Pattern.compile("mary"), 100);
		for (int[] is : lst) 
			System.out.println( Arrays.toString(is) );
		System.out.println("sentence 100");
		int[] ofs = tn.getOffsetsForSentenceIndex(100);
		System.out.println( Arrays.toString(ofs) );
		*/
	}


}
