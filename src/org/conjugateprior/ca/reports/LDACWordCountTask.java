package org.conjugateprior.ca.reports;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Locale;

import javafx.concurrent.Task;

import org.conjugateprior.ca.AbstractYoshikoderDocument;
import org.conjugateprior.ca.YoshikoderDocument;
import org.conjugateprior.ca.SimpleYoshikoderDocument;

public class LDACWordCountTask extends Task<File> {

	WordCountPrinter printer;
	
	public LDACWordCountTask(VocabularyFilterer reporter, 
			File folder, Charset c, Locale l, File[] f) {
		super();
		printer = new LDACWordCountPrinter(reporter, folder, c, l, f){			
			@Override
			public String makeLineFromDocument(YoshikoderDocument doc) {
				
				
				if (LDACWordCountTask.this.isCancelled()) { 
					updateMessage("Cancelled document processing");
					updateProgress(files.length + 1, getMaxProgress());
					//break; 
				}
				updateMessage("Processed " + getProgress() + " of " + getMaxProgress() + " documents");
				updateProgress(getProgress(), getMaxProgress());
				
				return super.makeLineFromDocument(doc);
			}
			
			@Override
			protected void writeDataFile(boolean showProgress) throws Exception {	
				BufferedWriter writer = null;
				try {
					OutputStreamWriter osw = new OutputStreamWriter(
							new FileOutputStream(new File(folder, datafilename)), outputCharset);
					writer = new BufferedWriter(osw);
					
					String dh = getDataHeader();
					if (dh != null)
						writer.write(dh); // nothing usually
					
					int counter = 0;
					for (File file : files) {
						
						if (isCancelled()) { 
							updateMessage("Cancelled document processing");
							// set progress as if we'd done all the files
							updateProgress(files.length + 1, getMaxProgress());
							break; 
						}
						updateMessage("Processed " + getProgress() + " of " + getMaxProgress() + " documents");
						updateProgress(getProgress(), getMaxProgress());
						
						YoshikoderDocument doc = null;
						try {
							doc = new SimpleYoshikoderDocument(file.getName(), 
								AbstractYoshikoderDocument.getTextFromFile(file, charset),
								null, tokenizer);		
							
							// subclasses override this
							String s = makeLineFromDocument(doc);
							
							writer.write(s);
							if (showProgress)
								System.err.println(getProgress());
							counter++;
							setProgress( counter+1 ); // preprocess + files to date
						
						} catch (Exception ex){
							ex.printStackTrace();
							// do something sensible with this
							throw new Exception("Problem with " + doc.getTitle() +
									" [" + ex.getMessage() + "] Skipping this document");
						}
					}
					
				} finally {
					if (writer != null)
						writer.close();
				}
			}
		};		
	}
	
	@Override
	protected File call() throws Exception {
		printer.processFiles(true);
		return printer.folder;
	}
	
	public static void main(String[] args) {

	}

}
