package org.conjugateprior.ca.reports;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Locale;

import org.conjugateprior.ca.AbstractYoshikoderDocument;
import org.conjugateprior.ca.CategoryDictionary;
import org.conjugateprior.ca.IYoshikoderDocument;
import org.conjugateprior.ca.SimpleDocumentTokenizer;
import org.conjugateprior.ca.SimpleYoshikoderDocument;

/**
 *
 */
public abstract class CountPrinter implements ICountPrinter {
	
	protected int progress = 0;

	protected File[] files;
	protected Charset charset;
	protected Locale locale;
	protected SimpleDocumentTokenizer tokenizer;
	
	protected Charset outputCharset = Charset.forName("UTF8");
	protected String newline = "\n";
	
	protected File folder;
	protected String datafilename;
	
	protected String rowfilename = "documents.csv";
	protected String columnfilename = "words.csv";
	protected String readmefilename = "readme.txt";
	
	public Charset getOutputCharset() {
		return outputCharset;
	}

	public void setOutputCharset(Charset outputCharset) {
		this.outputCharset = outputCharset;
	}

	public String getNewline() {
		return newline;
	}

	public void setNewline(String newline) {
		this.newline = newline;
	}
	
	@Override
	public int getProgress(){
		return progress;
	}
	
	void setProgress(int ii){
		progress = ii;
	}
	
	public CountPrinter(File f, String df, File[] fs, Charset chset, Locale loc){
		folder = f;
		datafilename = df;
		files = fs;
		charset = chset;
		locale = loc;
		
		tokenizer = new SimpleDocumentTokenizer(locale);
	}
	/**
	 * The order of operations: preProcess, writeDataFile, writeColumnsfile,
	 * writeRowsFile, writeReadmeFile, postProcess
	 */
	public File processFiles(boolean showProgress) throws Exception {
		
		preProcess(); // make folder
		writeDataFile(showProgress);
		writeColumnsFile();
		writeRowsFile();
		writeReadmeFile();
		postProcess();
		
		return folder;
	}
	
	// makes folder
	protected void preProcess() throws Exception {
		if (folder.exists())
			throw new Exception("Folder " + folder.getAbsolutePath() + " already exists.");
		boolean b = folder.mkdirs();
		if (!b) throw new Exception("Could not create all the folder elements in " + 
				folder.getAbsolutePath());
	}
	
	// this should include the newline if implemented in a subclass
	protected String getDataHeader(){ return null; };
	
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
				IYoshikoderDocument doc = null;
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
					setProgress( Math.round(100*(counter/files.length)) );
				
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
	
	protected void writeRowsFile() throws Exception {}
	protected void writeColumnsFile() throws Exception {}
		
	// override in subclasses
	abstract public String makeLineFromDocument(IYoshikoderDocument doc);
	
	// file writers are now closed, any last minute movements
	protected void postProcess() throws Exception {/* do nothing */ };
	
	protected void writeReadmeFile() throws Exception {}
	
	protected void extractREADMEFileAndSaveToFolder(String readmeResourceName) 
			throws Exception {

		InputStream in = getClass().getResourceAsStream("resources/" + readmeResourceName);
		if (in == null) // for when we're developing
			in = new FileInputStream(new File("resources/" + 
					readmeResourceName));
		FileWriter out = null;
		try {
			out = new FileWriter(new File(folder, readmefilename));
			int c;
			while ((c = in.read()) != -1) { out.write(c); }
		} finally {
			if (in != null) { in.close(); }
			if (out != null) { out.close(); }
		}
	}
	
	public static ICountPrinter getCategoryCountPrinter(CategoryDictionary dict,
			Format out, File folder, Charset c, Locale l, File[] fs) 
					throws Exception {
		if (out.equals(Format.CSV))
			return new CSVCategoryCountPrinter(dict, folder, c, l, fs);
		else
			throw new Exception("No category counters are available for format " + out);
	}
	
	public static ICountPrinter getWordCountPrinter(WordCounter rep, 
			Format out, File folder, Charset c, Locale l, File[] fs)
					throws Exception {
		
		if (out == Format.LDAC)
			return new LDACWordCountPrinter(rep, folder, c, l, fs);
		else if (out == Format.MTX)
			return new MTXWordCountPrinter(rep, folder, c, l, fs);
		else
			throw new Exception("No word counters are available for format " + out);
	}
	
	/*
	public static void main(String[] args) {
		
		WordReportFormatter formatter = new WordReportFormatter(reporter, 
				WordReportFormatter.OutputFormat.MTX, 
				new File("/Users/will/test/mtxtest"),
				Charset.forName("UTF-8"), 
				Locale.ENGLISH,
				new File[]{new File("/Users/will/test/", "m1.txt"), 
			               new File("/Users/will/test/", "m2.txt")});
		
		
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
	*/
}
