package org.conjugateprior.ca.reports;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public abstract class WordCountPrinter extends CountPrinter {

	protected WordCounter reporter;
	
	protected int idIndex = 0;
	protected Map<String,Integer> wordToId;
	
	public WordCountPrinter(WordCounter rep, File f, String df, 
			Charset cs, Locale loc, File[] fs) {
		
		super(f, df, fs, cs, loc);
		reporter = rep;
		wordToId = new HashMap<String,Integer>();
	}
	
	protected void writeColumnsFile() throws Exception {

		try (
			OutputStreamWriter words = new OutputStreamWriter(
					new FileOutputStream(new File(folder, columnfilename)), outputCharset);
			BufferedWriter wordsWriter = new BufferedWriter(words);
		){
			// sort
			String[] wdsInOrder = new String[wordToId.size()];
			for (String wd : wordToId.keySet()) {
				int ind = wordToId.get(wd);
				wdsInOrder[ind] = wd;
			}

			for (String entry : wdsInOrder)
				wordsWriter.write(entry + newline);
			
		} 
	}
	
}
