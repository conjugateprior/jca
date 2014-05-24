package org.conjugateprior.ca.reports;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Map;

import org.conjugateprior.ca.IYoshikoderDocument;

public class LDACWordCountPrinter extends WordCountPrinter {
	
	public LDACWordCountPrinter(WordCounter reporter, 
			File folder, Charset c, Locale l, File[] f) {
		super(reporter, folder, "data.ldac", c, l, f);
	}
			
	@Override
	public String makeLineFromDocument(IYoshikoderDocument doc){

		Map<String,Integer> map = reporter.getWordCountMapFromDocument(doc);
		
		StringBuffer sb = new StringBuffer();
		sb.append(map.keySet().size()); // this many feature pairs
		for (String wd : map.keySet()) {
			Integer id = wordToId.get(wd);
			if (id == null){
				id = idIndex;
				wordToId.put(wd, idIndex);
				idIndex++;
			}
			sb.append(" " + id + ":" + map.get(wd));
		}
		sb.append(newline);
		return sb.toString();
	}
	
	@Override
	protected void writeReadmeFile() throws Exception {
		extractREADMEFileAndSaveToFolder("README-ldac");
		
		super.writeReadmeFile(); // updates progress
	}
	
	public static void main(String[] args) throws Exception {
		WordCounter rep = new WordCounter();
		CountPrinter wp = CountPrinter.getWordCountPrinter(rep, 
				ICountPrinter.Format.MTX, new File("/Users/will/Desktop/fold"), 
				Charset.forName("UTF8"), Locale.ENGLISH, 
				new File[]{new File("/Users/will/Desktop/jfreqing/d1.txt"),
				new File("/Users/will/Desktop/jfreqing/d2.txt")});
		final int maxProg = wp.getMaxProgress();
		PropertyChangeListener listener = new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if ("progress".equals(evt.getPropertyName())){
					System.err.println(evt.getNewValue().getClass().getName() + " of " + maxProg);
				}
			}
		};
		wp.addPropertyChangeListener(listener);
		wp.processFiles(false);
	}
	
}


