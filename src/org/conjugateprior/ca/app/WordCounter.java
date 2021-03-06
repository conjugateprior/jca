package org.conjugateprior.ca.app;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.conjugateprior.ca.AbstractYoshikoderDocument;
import org.conjugateprior.ca.DocumentTokenizer;
import org.conjugateprior.ca.RegexpDocumentTokenizer;
import org.conjugateprior.ca.SimpleDocumentTokenizer;
import org.conjugateprior.ca.SimpleYoshikoderDocument;
import org.conjugateprior.ca.YoshikoderDocument;
import org.conjugateprior.ca.reports.VocabularyFilterer;

public class WordCounter extends AbstractCounter {

	protected int idIndex = 0;
	protected Map<String,Integer> wordToId = new HashMap<String,Integer>();
	
	protected int mtxDocumentLineCounter = 0; // this starts at 1
	protected int mtxMaxColIndex = -1;
	protected int tripleCount = 0; // how many entries in all
	
	protected String ldacFilename = "data.ldac";
	protected String mtxTempFilename = "data.mtx-tmp";
	protected String mtxFilename = "data.mtx";
	protected String csvFilename = "data.csv";
	protected String documentFilename = "docs.csv";
	protected String wordFilename = "words.csv";
	
	protected boolean LDACPairCount = false;
	
	protected VocabularyFilterer filterer = new VocabularyFilterer();
	
	public VocabularyFilterer getFilterer() {
		return filterer;
	}

	public WordCounter() {
		super();
	}
	
	public boolean getLDACPairCount(){
		return LDACPairCount;
	}
	
	public void setLDACPairCount(boolean b){
		LDACPairCount = b;
	}
	
	protected void dumpDocumentFile(File f) throws Exception {
		try (
				OutputStreamWriter docs = new OutputStreamWriter(
						new FileOutputStream(f), outputEncoding);
				BufferedWriter docWriter = new BufferedWriter(docs);
				){
			for (File doc : files) {
				docWriter.write(StringEscapeUtils.escapeCsv(doc.getName()) + "\n"); 
				// TODO platform specific?
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
				wordsWriter.write(StringEscapeUtils.escapeCsv(entry) + "\n"); // TODO platform specific?			
		} 
	}
	
	// make it *after* we've dumped the data (and computed the stats as side effects)
	protected String makeMTXHeader(){
		StringBuilder sb = new StringBuilder();
		sb.append("%%MatrixMarket matrix coordinate integer general" + "\n");
		sb.append(mtxDocumentLineCounter + " ");
		sb.append((mtxMaxColIndex+1) + " ");
		sb.append(tripleCount + "\n");
		return sb.toString();

	}

	protected void mtxClearup() throws Exception {		
		String header = makeMTXHeader();
		try (
				OutputStreamWriter real = new OutputStreamWriter(
				new FileOutputStream(new File(outputFolder, mtxFilename)))
			){
			real.write(header);
		}		
		File tmpfile = new File(outputFolder, mtxTempFilename);
		try (
				FileInputStream fic = new FileInputStream(tmpfile);
				FileChannel inputChannel = fic.getChannel();
				FileOutputStream foc = new FileOutputStream(new File(outputFolder, mtxFilename), true);
				FileChannel outputChannel = foc.getChannel();
			){
			outputChannel.transferFrom(inputChannel, header.getBytes().length, inputChannel.size());
			boolean b = tmpfile.delete();
			if (!b)
				throw new Exception("Could not delete temporary file " + 
						tmpfile.getAbsolutePath());
		} 
	}
	
	public String makeLDACLineFromDocument(YoshikoderDocument doc){
		Map<String,Integer> map = filterer.getWordCountMapFromDocument(doc);
		
		StringBuffer sb = new StringBuffer();
		//
		for (String wd : map.keySet()) {
			Integer id = wordToId.get(wd);
			if (id == null){
				id = idIndex;
				wordToId.put(wd, idIndex);
				idIndex++;
			}
			sb.append(" " + id + ":" + map.get(wd));
		}
		if (LDACPairCount)
			sb.insert(0, map.keySet().size()); // this many feature pairs
		else
			sb.delete(0, 1); // remove starting space
		
		return sb.toString();
	}
	
	public String makeCSVLineFromDocument(List<Map<Integer,Integer>> lil, int docIndex){
		StringBuffer sb = new StringBuffer();
		sb.append(StringEscapeUtils.escapeCsv(files[docIndex].getName()));
		int[] counts = new int[idIndex];
		Map<Integer,Integer> docmap = lil.get(docIndex);
		
		for (Map.Entry<Integer,Integer> entry : docmap.entrySet())
			counts[entry.getKey()] = entry.getValue(); 
		for (int c : counts) {
			sb.append(",");
			sb.append(c);
		}
		return sb.toString();
	}
	
	public String makeMTXLineFromDocument(YoshikoderDocument doc){
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

		File ff = null;
		if (format.equals(OutputFormat.CSV))
			ff = new File(outputFolder, csvFilename);	
		else if (format.equals(OutputFormat.MTX))
			ff = new File(outputFolder, mtxTempFilename);
		else
			ff = new File(outputFolder, ldacFilename); // default ldac
		
		try (BufferedWriter writer = getBufferedWriter(ff)){

			DocumentTokenizer tok = null;
			if (usingRegexpTokenizer)
				if (regexp == null)
					throw new Exception("No regexp set");
				else 
					tok = new RegexpDocumentTokenizer(locale, regexp);
			else	
				tok = new SimpleDocumentTokenizer(locale);

			List<Map<Integer,Integer>> lil = new ArrayList<>(); // only used by CSV
			if (format.equals(OutputFormat.CSV)){
				for (File f : files) {
					YoshikoderDocument idoc = 
							new SimpleYoshikoderDocument(f.getName(), 
									AbstractYoshikoderDocument.getTextFromFile(f, encoding),
									null, tok);	
					Map<String,Integer> docwordtocount = filterer.getWordCountMapFromDocument(idoc);
				
					Map<Integer,Integer> docindextocount = new HashMap<>();
					for (String wd : docwordtocount.keySet()) {
						int wdCount = docwordtocount.get(wd);
						Integer wordId = wordToId.get(wd);
						if (wordId == null){
							docindextocount.put(idIndex, wdCount);							
							wordToId.put(wd, idIndex++);
						} else {
							docindextocount.put(wordId.intValue(), wdCount);
						}
					}
					
					lil.add(docindextocount);
				}
				// write top line
				String[] toprow = new String[idIndex];
				for (Map.Entry<String,Integer> entry : wordToId.entrySet())
					toprow[entry.getValue()] = entry.getKey(); 
				for (String s : toprow) {
					writer.write(",");
					writer.write(StringEscapeUtils.escapeCsv(s));
				}
				writer.newLine();
			}
			
			int docIndex = 0;
			for (File f : files) {
				YoshikoderDocument idoc = 
						new SimpleYoshikoderDocument(f.getName(), 
								AbstractYoshikoderDocument.getTextFromFile(f, encoding),
								null, tok);	
				if (format.equals(OutputFormat.MTX))	
					writer.write(makeMTXLineFromDocument(idoc));
				else if (format.equals(OutputFormat.CSV))
					writer.write(makeCSVLineFromDocument(lil, docIndex++)); // use idIndex
				else
					writer.write(makeLDACLineFromDocument(idoc));
				writer.newLine();

				if (!getSilent())
					System.err.print(".");
			}
			writer.flush(); // do we need this really?
			if (!getSilent())
				System.err.println();

			if (format.equals(OutputFormat.MTX)){
				mtxClearup();
				extractResourceFileAndSaveToFolder("README-mtx", "README.txt");
			} else if (format.equals(OutputFormat.LDAC))
				if (getLDACPairCount())
					extractResourceFileAndSaveToFolder("README-ldac", "README.txt");
				else
					extractResourceFileAndSaveToFolder("README-ldac-nopaircount", "README.txt");
			
			if (!format.equals(OutputFormat.CSV)){
				dumpVocabularyFile(new File(outputFolder, wordFilename));
				dumpDocumentFile(new File(outputFolder, documentFilename));
			}
		}		
	}

	public static void main(String[] args) throws Exception {
		WordCounter counter = new WordCounter();
		counter.setOutputFolder("/Users/wlowe/data/out");
		counter.setFormat(OutputFormat.LDAC);
		counter.setLDACPairCount(true);
		/*
		File a1 = File.createTempFile("aaa", "b");
		File a2 = File.createTempFile("aaa", "b");
		FileOutputStream fout = new FileOutputStream(a1);
		fout.write("This is it".getBytes());
		fout.close();
		fout = new FileOutputStream(a2);
		fout.write("is it always".getBytes());
		fout.close();
		counter.setFiles(new File[]{a1, a2});
		*/
		counter.setFiles(new String[]{"/Users/wlowe/data/uk-debate-by-speaker"});
		counter.processFiles();
	}
}
