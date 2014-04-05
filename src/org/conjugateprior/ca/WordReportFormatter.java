package org.conjugateprior.ca;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.swing.SwingWorker;

public class WordReportFormatter {

	public enum OutputFormat { LDAC, MTX }
	
	abstract public class Printer extends SwingWorker<File, Void>{
		int idIndex = 0;
		Map<String,Integer> wordToId = new HashMap<String,Integer>();
		
		File folder;
		String datafilename;
		BufferedWriter writer;
		BufferedWriter docsWriter;
		BufferedWriter wordsWriter;
		
		Charset charset;
		Locale locale;
		SimpleDocumentTokenizer tokenizer;
		File[] files;
		
		Printer(File f, String df, Charset cs, Locale loc, File[] fs){
			folder = f;
			datafilename = df;
			charset = cs;
			locale = loc;
			files = fs;
			
			tokenizer = new SimpleDocumentTokenizer(loc);
		}
		
		@Override
		protected File doInBackground() throws Exception {
			setProgress(0);
			openFileWriters();
		
			IYoshikoderDocument doc = null;
			float counter = 0;
			for (File file : files) {
				if (isCancelled()){
					writer.close();
					docsWriter.close();
					wordsWriter.close();
					return folder; // return the half filled folder
				}				
				try {
					String txt = SimpleYoshikoderDocument.getTextFromFile(file, charset);
					String docTitle = file.getName();
					//System.err.println("Processing " + docTitle); // TODO put me elsewhere
					doc = new SimpleYoshikoderDocument(docTitle, txt, null, tokenizer); // null date
					String s = makeLineFromDocument(doc); // reporter invoked here
					docsWriter.write(docTitle + newline);
					writer.write(s);
					System.err.println(getProgress());
					counter++;
					setProgress( Math.round(100*(counter/files.length)) );
				
				} catch (Exception ex){
					ex.printStackTrace();
					// do something sensible with this
					System.err.println("Problem with " + doc.getTitle() +
							" [" + ex.getMessage() + "] Skipping this document");
				}
			}
			writer.close();
			docsWriter.close();			
			// sort out the words
			String[] wdsInOrder = new String[wordToId.size()];
			for (String wd : wordToId.keySet()) {
				int ind = wordToId.get(wd);
				wdsInOrder[ind] = wd;
			}
			for (String entry : wdsInOrder)
				wordsWriter.write(entry + newline);
			wordsWriter.close();
			// tidy up
			postProcess();
			setProgress(100);
			
			return folder;
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
						new FileOutputStream(new File(folder, "documents.csv")), outputCharset);
				docsWriter = new BufferedWriter(docs);
			} catch (Exception ex){
				throw new Exception("Could not create all the files needed inside " +
						folder.getAbsolutePath());
			}
		}
		
		
		abstract String makeLineFromDocument(IYoshikoderDocument doc);
		abstract void postProcess() throws Exception;
		
		public void extractREADMEFileAndSaveToFolder(String readmeResourceName) throws Exception {
	    	InputStream in = getClass().getResourceAsStream("resources/" + readmeResourceName);
	    	if (in == null) // for when we're developing
	    		in = new FileInputStream(new File("resources/" + 
	    				readmeResourceName));
	    	FileWriter out = null;
	    	try {
	    		out = new FileWriter(new File(folder, "README.md"));
	    		int c;
	    		while ((c = in.read()) != -1) { out.write(c); }
	    	} finally {
	    		if (in != null) { in.close(); }
	    		if (out != null) { out.close(); }
	    	}
	    }
	}
	
	public class LDACPrinter extends Printer {		

		public LDACPrinter(File folder, Charset c, Locale l, File[] f) {
			super(folder, "data.ldac", c, l, f);
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
	
	public class MTXPrinter extends Printer {				
		
		String helpFileContents;
		
		int mtxDocumentLineCounter = 0; // this starts at 1
		int mtxMaxColIndex = -1;
		int tripleCount = 0; // how many entries in all

		public MTXPrinter(File folder, Charset c, Locale l, File[] f) {
			super(folder, "data.mtx", c, l, f);
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
			String header = getHeader();
			real.write(header);
			real.close();
			
			File tmpfile = new File(folder, datafilename);
			FileChannel inputChannel = null;
			FileChannel outputChannel = null;
			try {
				inputChannel = new FileInputStream(tmpfile).getChannel();
				outputChannel = new FileOutputStream(new File(folder, "data.mtx"), true).getChannel();
				outputChannel.transferFrom(inputChannel, header.getBytes().length, inputChannel.size());
			} finally {
				inputChannel.close();
				outputChannel.close();
			}
			boolean b = tmpfile.delete();
			if (!b)
				throw new Exception("Could not delete temporary file " + tmpfile.getAbsolutePath());
			datafilename = "data.mtx";

			extractREADMEFileAndSaveToFolder("README-mtx");
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
	
	protected Charset outputCharset = Charset.forName("UTF8");
	protected String newline = "\n";
	
	protected WordReporter reporter;
	protected Printer printer;
		
	public Printer getPrinter(){
		return printer;
	}
	
	public WordReportFormatter(WordReporter rep, OutputFormat out, 
			File folder, Charset c, Locale l, File[] fs){
		reporter = rep;
		if (out == OutputFormat.LDAC)
			printer = new LDACPrinter(folder, c, l, fs);
		else if (out == OutputFormat.MTX)
			printer = new MTXPrinter(folder, c, l, fs);
		else {
			System.err.println("Should never get here");
		}
	}
	
	// returns the output folder
	public Printer getReportPrinter() throws Exception {
		return printer;		
	}

	public static void main(String[] args) throws Exception {
		WordReporter reporter = new WordReporter();
		//File f1 = new File("/Users/will/test/mtxtest");
		//File f2 = new File("/Users/will/test/ldactest");
		//if (f1.exists()) System.err.println( f1.delete() );
		//if (f2.exists()) System.err.println( f2.delete() );
		File la = new File("/Users/will/Dropbox/NicoleWillUncertain/LA_articles");
		File[] fls = la.listFiles();
		
		/*
		WordReportFormatter formatter = new WordReportFormatter(reporter, 
				WordReportFormatter.OutputFormat.MTX, 
				new File("/Users/will/test/mtxtest"),
				Charset.forName("UTF-8"), 
				Locale.ENGLISH,
				new File[]{new File("/Users/will/test/", "m1.txt"), 
			               new File("/Users/will/test/", "m2.txt")});
		*/
		
		WordReportFormatter formatter = new WordReportFormatter(reporter, 
				WordReportFormatter.OutputFormat.MTX, 
				new File("/Users/will/test/mtxtest"),
				Charset.forName("UTF-8"), 
				Locale.ENGLISH, fls);
		
		Printer worker = formatter.getPrinter();
		worker.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if ("progress" == evt.getPropertyName()) {
					int progress = (Integer) evt.getNewValue();
					System.err.println(progress + "%");
				} 
			}
		});
		worker.execute();
	}
}
