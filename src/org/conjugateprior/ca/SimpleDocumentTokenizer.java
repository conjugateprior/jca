package org.conjugateprior.ca;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;


public class SimpleDocumentTokenizer implements IDocumentTokenizer {
    
    private static Logger log = Logger.getLogger(SimpleDocumentTokenizer.class.getName());
    
    // this is a fixed feature of the tokenizer (the text may change)
    protected Locale locale;
    
    public SimpleDocumentTokenizer(Locale loc){
    	locale = loc;
    }
    
    /**
     * Generates a list of spans for the document that delimit word tokens.
     * 
     * @param doc document
     * @return list of token spans
     * @throws TokenizationException document's text could not be retrieved
     */
    public int[][] getWordOffsets(String txt) throws Exception {
    	BreakIterator wordIterator = BreakIterator.getWordInstance(locale);
    	wordIterator.setText(txt);
        List<int[]> list = new ArrayList<int[]>();
        
        int start = wordIterator.first();
        int end = wordIterator.next();
        int tokennumber = 0;
        while (end != BreakIterator.DONE) {
            char c = txt.charAt(start);
            if (Character.isLetterOrDigit(c) || Character.getType(c)==Character.CURRENCY_SYMBOL)
                list.add(new int[]{start, end, tokennumber++});            
            
            start = end;
            try {
                end = wordIterator.next();
            } catch (Exception e) { // but keep on trucking
                log.log(Level.WARNING, "word tokenization error after char " + end, e);
            }
        }
        
        // construct the token ranges
        int[][] ranges = new int[list.size()][2];
        int index = 0;
        for (Iterator<int[]> iter = list.iterator(); iter.hasNext();) {
            int[] range = iter.next();
            ranges[index++] = range;
        }
        
        return ranges;
    }

    public int[][] getSentenceOffsets(String txt) throws Exception {
    	BreakIterator sentenceIterator = BreakIterator.getSentenceInstance(locale);
    	sentenceIterator.setText(txt);
        List<int[]> list = new ArrayList<int[]>();
        
        int start = sentenceIterator.first();
        int end = sentenceIterator.next();
        int tokennumber = 0;
        while (end != BreakIterator.DONE) {
            // remove the trailing whitespace - there must be a more elegant way...
        	String s = txt.substring(start, end);
        	int ii = s.length()-1;
        	while (!Character.isLetterOrDigit(s.charAt(ii)))
        		ii--;
        	int diff = (end-start) - (ii + 1);
        	
        	list.add(new int[]{start, end-diff, tokennumber++});            
            start = end;
            try {
                end = sentenceIterator.next();
            } catch (Exception e) { // but keep on trucking
                log.log(Level.WARNING, "sentence tokenization error after char " + end, e);
            }
        }
        
        int[][] ranges = new int[list.size()][2];
        int index = 0;
        for (Iterator<int[]> iter = list.iterator(); iter.hasNext();) {
            int[] range = iter.next();
            ranges[index++] = range;
        }    
        return ranges;
    }
    
    public Locale getLocale(){
        return locale;
    }
    
    public static void main(String[] args) throws Exception {
		String mary = "Mary had a little lamb.  It's fleece was white as snow. ";
		String offs = "01234567890123456789012345678901234567890123456789012345";
		String upp =  "00000000001111111111222222222233333333334444444444555555";
		SimpleDocumentTokenizer tok = new SimpleDocumentTokenizer(Locale.ENGLISH);
		int[][] wds = tok.getWordOffsets(mary);
		int[][] sents = tok.getSentenceOffsets(mary);
		System.out.println(mary);
		System.out.println(upp);
		System.out.println(offs);
		System.out.println("Words");
		for (int[] is : wds) {
			System.out.println(Arrays.toString(is));
		}
		System.out.println("Sentences");
		for (int[] is : sents) {
			System.out.println(Arrays.toString(is));
		}
    }
    
}
