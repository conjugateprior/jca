package org.conjugateprior.ca;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

abstract public class AbstractYoshikoderDocument 
	implements YoshikoderDocument {

	protected String title;
	protected Locale locale;
	protected Tokenization tokenization;
	protected Date date;
	protected int wordCount;
	// subclasses get to keep the text
	
	public AbstractYoshikoderDocument(String docTitle, String txt, Date d, DocumentTokenizer tok)
		throws Exception {
		
		title = docTitle;
		tokenization = new SimpleTokenization(txt, tok);
		locale = tok.getLocale();
		date = d;
		wordCount = tokenization.getWordCount();
	}

	// triggers a retokenization
	public void setLocale(Locale loc, DocumentTokenizer tokenizerForLoc) throws Exception {
		if (loc.equals(locale))
			return;
		
		tokenization = new SimpleTokenization(getText(), tokenizerForLoc);
		locale = loc;
		wordCount = tokenization.getWordCount();
	}
	
	public int getWordCount(){
		return wordCount;
	}
	
	public int compareTo(YoshikoderDocument o) {
		return getTitle().compareTo(o.getTitle());
	}

	public String getText() throws IOException {
		return loadText();
	}

	abstract protected String loadText() throws IOException;

	public void setDate(Date d){
		date = d;
	}
	
	public Date getDate(){
		return date;
	}
	
	public Map<String,Integer> getWordCounts() {
		return tokenization.getWordCountMap();
	}
	
	/**
	 * Equality between documents depends only on the title.
	 */
	public boolean equals(Object o){
		try {
			YoshikoderDocument doc = (YoshikoderDocument)o;
			return title.equals(doc.getTitle());
		} catch (ClassCastException e){
			// falls through and returns false
		}
		return false;
	}

	/**
	 * Hash code of a document depends only on the title.
	 */
	public int hashCode(){
		return title.hashCode();
	}

	public String getTitle() {
		return title;
	}

	public Locale getLocale() {
		return locale;
	}

	public Set<String> getVocabulary(){
		return tokenization.getWordTypes();
	}
	
	public String getWordAt(int N){
		return tokenization.getWordAtIndex(N);
	}
	
	// new code 
	public Set<Integer> getWordIndexesForPattern(Pattern[] pat){
		return tokenization.getWordIndexesForPattern(pat);
	}
	
	public List<int[]> getCharacterOffsetsForPattern(Pattern[] pat) {
		return tokenization.getCharacterOffsetsForPattern(pat);
	}

	public List<int[]> getConcordanceCharacterOffsetsForPattern(Pattern[] pat, int window){
		return tokenization.getConcordanceCharacterOffsetsForPattern(pat, window);
	}
	
	public static String getTextFromFile(File f, Charset cs) 
			throws UnsupportedEncodingException, IOException {
		byte[] bytes = getBytes(f);
		return new String(bytes, cs.name());
	}
	
	protected static byte[] getBytes(File f) throws IOException {
		FileChannel in = null;
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(f);
			in = fis.getChannel();
			
			long size = in.size();
			if (size > Integer.MAX_VALUE) {
				throw new IOException("File : " + f
						+ " is too large for processing");
			}
			MappedByteBuffer buf = 
				in.map(FileChannel.MapMode.READ_ONLY, 0, size);
			byte[] bytes = new byte[(int) size];
			
			buf.get(bytes);
			in.close();
			
			return bytes;
		} finally {
			if (in != null) {
				fis.close();
				in.close();
			}
		}
	}
	
	public String toString(){
		DateFormat df = DateFormat.getDateInstance();
		StringBuffer sb = new StringBuffer();
		sb.append(title);
		if (date != null){
			sb.append(", ");
			sb.append(df.format(date));
		} 
		sb.append(" (");
		sb.append(tokenization.getWordCount());
		sb.append(" tokens, ");
		sb.append(tokenization.getWordTypes().size());
		sb.append(" types)");
		return sb.toString();
	}

}
