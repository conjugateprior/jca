package org.conjugateprior.ca;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

abstract public class AbstractTokenizer implements DocumentTokenizer {

	protected static Logger log = Logger.getLogger(AbstractTokenizer.class.getName());
	
    // this is a fixed feature of the tokenizer (the text may change)
    protected Locale locale;
    
    public AbstractTokenizer(Locale loc){
    	locale = loc;
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
        	if (s.trim().length()>1){
        		//int ii = s.length()-1;
        		//while (!Character.isLetterOrDigit(s.charAt(ii)))
        		//	ii--;
        		//int diff = (end-start) - (ii + 1);

        		list.add(new int[]{start, end, tokennumber++});            
        	}
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

    abstract public int[][] getWordOffsets(String txt) throws Exception;
    
	@Override
	public Locale getLocale() {
		return locale;
	}

}
