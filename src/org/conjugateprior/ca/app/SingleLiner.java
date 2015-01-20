package org.conjugateprior.ca.app;

import java.io.BufferedWriter;
import java.io.File;

import org.conjugateprior.ca.AbstractYoshikoderDocument;
import org.conjugateprior.ca.IYoshikoderDocument;
import org.conjugateprior.ca.SimpleDocumentTokenizer;
import org.conjugateprior.ca.SimpleYoshikoderDocument;

public class SingleLiner extends AbstractCounter {

	protected File outputFile;
	
	public SingleLiner() {
		super();
	}

	public void setOutputFile(File f){
		outputFile = f;
	}
	
	protected String makeTextLineFromDocument(IYoshikoderDocument doc) throws Exception {
		String title = doc.getTitle();
		return title + "\t" + title + "\t" + 
				doc.getText().replaceAll("[\\s]+", " ");
	}

	public void processFiles() throws Exception {
		try (BufferedWriter writer = getBufferedWriter(outputFile)){			
			SimpleDocumentTokenizer tok = 
					new SimpleDocumentTokenizer(locale);
			for (File f : files) {
				IYoshikoderDocument idoc = 
						new SimpleYoshikoderDocument(f.getName(), 
								AbstractYoshikoderDocument.getTextFromFile(f, encoding),
								null, tok);	
				writer.write(makeTextLineFromDocument(idoc));
				writer.newLine();
				writer.flush(); // do we need this really?
			}
		}
	}

	public static void main(String[] args) throws Exception {
		// /Users/will/Dropbox/blogposts/speeches
		//SingleLiner sl = new SingleLiner();
		//sl.setFiles(new String[]{"/Users/will/Dropbox/blogposts/speeches"});
		//sl.setOutputFile(new File("/Users/will/Dropbox/blogposts/speeches-as-one-file.txt"));
		//sl.processFiles();
	}
	
}
