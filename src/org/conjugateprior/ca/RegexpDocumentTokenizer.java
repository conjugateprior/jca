package org.conjugateprior.ca;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class RegexpDocumentTokenizer extends AbstractTokenizer implements DocumentTokenizer  {

	private Pattern pattern;
	
	public RegexpDocumentTokenizer(Locale loc, String regexp) throws Exception {
    	super(loc);
		pattern = Pattern.compile(regexp);
    }

    public int[][] getWordOffsets(String txt) throws Exception {
		List<int[]> pairs = new ArrayList<int[]>();
		
    	Matcher m = pattern.matcher(txt);
		int st = 0;
		int en = 0;
		while (m.find(st)) {
			st = m.start();
			en = m.end();
			pairs.add(new int[]{st, en});
			st = en;
		}
		
        // construct the token ranges
        int[][] ranges = new int[pairs.size()][2];
        int index = 0;
        for (Iterator<int[]> iter = pairs.iterator(); iter.hasNext();) {
            int[] range = iter.next();
            ranges[index++] = range;
        }
        
        return ranges;
    }
	
	public static void main(String[] args) throws Exception {
		String others = "Other's dogs had seen the pup.\nIt wasn't any particular breed, though.";
		RegexpDocumentTokenizer tok = new RegexpDocumentTokenizer(Locale.ENGLISH, 
				"[\\p{L}\\p{M}]+");
		int[][] si = tok.getWordOffsets(others);
		for (int ii = 0; ii < si.length; ii++) {
			System.err.println(others.subSequence(si[ii][0], si[ii][1]) + 
					" (" + si[ii][0] + "," + si[ii][1] + ")");
		}
		
		tok = new RegexpDocumentTokenizer(Locale.ENGLISH, 
				"[\\p{L}\\p{P}]*\\p{L}");
		si = tok.getWordOffsets(others);
		for (int ii = 0; ii < si.length; ii++) {
			System.err.println(others.subSequence(si[ii][0], si[ii][1]) + 
					" (" + si[ii][0] + "," + si[ii][1] + ")");
		}
		
		
		
		
	}

}
