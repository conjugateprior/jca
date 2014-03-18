package org.conjugateprior.ca;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

public class WordReportFormatter {

	public enum OutputFormatType { LDAC, MTX }
	
	abstract class Printer {
		File folder;
		String datafilename;
		BufferedWriter writer;
		BufferedWriter docsWriter;
		BufferedWriter wordsWriter;
		
		Printer(File f, String df){
			folder = f;
			datafilename = df;
		}
		
		public void run(File[] fs, Charset cs, Locale l) throws Exception {
			openFileWriters();
			processDocuments(fs, cs, l);
			postProcess();
		}
		
		protected void openFileWriters() throws Exception{
			if (folder.exists())
				throw new Exception("Folder " + folder.getAbsolutePath() + " already exists.");
			boolean b = folder.mkdirs();
			if (!b) throw new Exception("Could not create all the folder elements in " + 
					folder.getAbsolutePath());
			try {
				OutputStreamWriter osw = new OutputStreamWriter(
						new FileOutputStream(new File(folder, datafilename)), outputCharset);
				writer = new BufferedWriter(osw);
				OutputStreamWriter words = new OutputStreamWriter(
						new FileOutputStream(new File(folder, "words.csv")), outputCharset);
				wordsWriter = new BufferedWriter(words);
				OutputStreamWriter docs = new OutputStreamWriter(
						new FileOutputStream(new File(folder, "docs.csv")), outputCharset);
				docsWriter = new BufferedWriter(docs);
			} catch (Exception ex){
				throw new Exception("Could not create all the files needed inside " +
						folder.getAbsolutePath());
			}
		}
		
		protected void processDocuments(File[] files, Charset encoding, Locale loc) throws Exception {
			IYoshikoderDocument doc = null;
			SimpleDocumentTokenizer tok = new SimpleDocumentTokenizer(loc);
			for (File file : files) {
				try {
					String txt = SimpleYoshikoderDocument.getTextFromFile(file, encoding);
					String docTitle = file.getName();
					System.err.println("Processing " + docTitle); // TODO put me elsewhere
					doc = new SimpleYoshikoderDocument(docTitle, txt, null, tok); // null date
					String s = makeLineFromDocument(doc); // reporter invoked here
					docsWriter.write(docTitle + newline);
					writer.write(s);
				} catch (Exception ex){
					System.err.println("Problem with " + doc.getTitle() +
							" [" + ex.getMessage() + "] Skipping this document");
				}
			}
			writer.close();
			docsWriter.close();			
			// push out the words one per line, 
			// sorted by identifier so the row numbers are the feature id numbers.
			List<Entry<String,Integer>> lst = 
					new ArrayList<Entry<String,Integer>>(wordToId.entrySet());
			Collections.sort(lst, new Comparator<Entry<String, Integer>>() {
				public int compare(Entry<String, Integer> o1,
						Entry<String, Integer> o2) {
					return o1.getValue().compareTo(o2.getValue());
				}
			});
			for (Entry<String, Integer> entry : lst)
				wordsWriter.write(entry.getKey() + newline);
			wordsWriter.close();
		}
		
		abstract String makeLineFromDocument(IYoshikoderDocument doc);
		abstract void postProcess() throws Exception;
		
		public File getOutputFolder(){
			return folder;
		}
	}
	
	class LDACPrinter extends Printer {		

		public LDACPrinter(File folder) {
			super(folder, "data.ldac");
		}
				
		@Override
		protected String makeLineFromDocument(IYoshikoderDocument doc){
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
		void postProcess() throws Exception { /* nothing */ }
	}
	
	class MTXPrinter extends Printer {				
		
		int mtxDocumentLineCounter = 0; // this starts at 1
		int mtxMaxColIndex = -1;
		int tripleCount = 0; // how many entries in all

		public MTXPrinter(File folder) {
			super(folder, "data.mtx-tmp");
		}
				
		@Override
		public String makeLineFromDocument(IYoshikoderDocument doc){
			Map<String,Integer> map = reporter.getWordCountMapFromDocument(doc);
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
				sb.append(map.get(wd) + newline);
			}
			return sb.toString();
		}
		
		@Override
		void postProcess() throws Exception {
			OutputStreamWriter real = new OutputStreamWriter(
					new FileOutputStream(new File(folder, "data.mtx")));
			real.write(getHeader());
			real.close();
			
			File tmpfile = new File(folder, datafilename);
			FileChannel inputChannel = null;
			FileChannel outputChannel = null;
			try {
				inputChannel = new FileInputStream(tmpfile).getChannel();
				outputChannel = new FileOutputStream(new File(folder, "data.mtx"), true).getChannel();
				outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
			} finally {
				inputChannel.close();
				outputChannel.close();
			}
			boolean b = tmpfile.delete();
			if (!b)
				throw new Exception("Could not delete temporary file " + tmpfile.getAbsolutePath());
			datafilename = "data.mtx";
		}
		
		protected String getHeader(){
			StringBuilder sb = new StringBuilder();
			sb.append("%%MatrixMarket matrix coordinate integer general" + newline);
			sb.append(mtxDocumentLineCounter + " ");
			sb.append((mtxMaxColIndex+1) + " ");
			sb.append(tripleCount + newline);
			return sb.toString();
		}
	}
	
	protected int idIndex = 0;
	protected Map<String,Integer> wordToId = new HashMap<String,Integer>();
	protected Charset outputCharset = Charset.forName("UTF8");
	protected String newline = "\n";
	
	protected WordReporter reporter;
	protected Printer printer;
		
	public WordReportFormatter(WordReporter rep, OutputFormatType out, File folder){
		reporter = rep;
		if (out == OutputFormatType.LDAC)
			printer = new LDACPrinter(folder);
		else if (out == OutputFormatType.MTX)
			printer = new MTXPrinter(folder);
		else {
			System.err.println("Should never get here");
		}
	}
	
	// returns the output folder
	public File makeReport(File[] fs, Charset cs, Locale l) throws Exception {
		printer.run(fs, cs, l);
		return printer.getOutputFolder();
	}
}
