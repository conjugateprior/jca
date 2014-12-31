package org.conjugateprior.ca.app;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.conjugateprior.ca.AbstractYoshikoderDocument;
import org.conjugateprior.ca.IYoshikoderDocument;
import org.conjugateprior.ca.SimpleDocumentTokenizer;
import org.conjugateprior.ca.SimpleYoshikoderDocument;
import org.conjugateprior.ca.reports.VocabularyFilterer;

public class WordCounter extends AbstractCounter {

	protected int idIndex = 0;
	protected Map<String,Integer> wordToId;
	
	protected int mtxDocumentLineCounter = 0; // this starts at 1
	protected int mtxMaxColIndex = -1;
	protected int tripleCount = 0; // how many entries in all
	
	protected VocabularyFilterer filterer = new VocabularyFilterer();
	
	public WordCounter() {
		super();
	}

	protected void dumpDocumentFile(File f) throws Exception {
		try (
				OutputStreamWriter docs = new OutputStreamWriter(
						new FileOutputStream(f), outputEncoding);
				BufferedWriter docWriter = new BufferedWriter(docs);
				){
			for (File doc : files) {
				docWriter.write(doc.getName() + "\n"); // TODO platform specific?
			}
		} 
	}
		
	
	protected void dumpVocabularyFile(File f) throws Exception {
	try (
			OutputStreamWriter words = new OutputStreamWriter(
					new FileOutputStream(f), outputEncoding);
			BufferedWriter wordsWriter = new BufferedWriter(words);
		){
			String[] wdsInOrder = new String[wordToId.size()]; // sort
			for (String wd : wordToId.keySet()) {
				int ind = wordToId.get(wd);
				wdsInOrder[ind] = wd;
			}
			for (String entry : wdsInOrder)
				wordsWriter.write(entry + "\n"); // TODO platform specific?			
		} 
	}
	
	public void dumpMetadata() throws Exception {
		
	}
	
	public String makeMTXHeader(){
		return null;
	}
	
	public String makeLDACLineFromDocument(IYoshikoderDocument doc){
		Map<String,Integer> map = filterer.getWordCountMapFromDocument(doc);
		
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
		return sb.toString();
	}
	
	public String makeMTXLineFromDocument(IYoshikoderDocument doc){
		Map<String,Integer> map = filterer.getWordCountMapFromDocument(doc);
		mtxDocumentLineCounter++;
		tripleCount += map.keySet().size();
		StringBuilder sb = new StringBuilder();			 
		for (String wd : map.keySet()) {
			Integer id = wordToId.get(wd);
			if (id == null){
				id = idIndex;
				wordToId.put(wd, idIndex);
				idIndex++;
			}
			if (id > mtxMaxColIndex)
				mtxMaxColIndex = id; // update highest word index so far this document

			sb.append(mtxDocumentLineCounter + " ");
			sb.append((id + 1) + " "); // 1 based indexing
			sb.append(map.get(wd) + "\n"); // TODO platform specific ?
		}
		return sb.toString();
	}
	
	public void processFiles() throws Exception {
		if (outputFolder != null)
			FileUtils.forceMkdir(outputFolder);

		// now the long part
		BufferedWriter writer = null;
		try {
			if (outputFolder != null){
				if (format.equals(OutputFormat.MTX))
					writer = getBufferedWriter(new File(outputFolder, "data.mtx"));
				else 
					writer = getBufferedWriter(new File(outputFolder, "data.ldac"));
			} else {
				writer = getBufferedWriter();
			}
			if (format.equals(OutputFormat.MTX))
				writer.write(makeMTXHeader()); 			

			SimpleDocumentTokenizer tok = 
					new SimpleDocumentTokenizer(locale);
			for (File f : files) {
				IYoshikoderDocument idoc = 
						new SimpleYoshikoderDocument(f.getName(), 
								AbstractYoshikoderDocument.getTextFromFile(f, encoding),
								null, tok);	
				if (format.equals(OutputFormat.MTX))	
					writer.write(makeMTXLineFromDocument(idoc));
				else 
					writer.write(makeLDACLineFromDocument(idoc));

				writer.newLine();
				writer.flush(); // do we need this really?
			}
			writer.flush(); // do we need this really?

			if (outputFolder != null){
				dumpVocabularyFile(new File(outputFolder, "vocab.csv"));
				dumpDocumentFile(new File(outputFolder, "docs.csv"));
			}
			
		} finally {
			if (writer != null)
				writer.close();
		}
		
	}

}
