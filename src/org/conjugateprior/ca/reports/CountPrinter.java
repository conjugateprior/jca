package org.conjugateprior.ca.reports;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Locale;

import javafx.application.Platform;
import javafx.concurrent.Task;

import org.conjugateprior.ca.AbstractYoshikoderDocument;
import org.conjugateprior.ca.OldCategoryDictionary;
import org.conjugateprior.ca.IYoshikoderDocument;
import org.conjugateprior.ca.SimpleDocumentTokenizer;
import org.conjugateprior.ca.SimpleYoshikoderDocument;

/**
 *
 */
public abstract class CountPrinter implements ICountPrinter {
	
	protected int progress = 0;
	protected int maxProgress = 0; // stages + documents
	private PropertyChangeSupport mPcs = new PropertyChangeSupport(this);
	
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
	
	public CountingTask getNewCountingTask(){
		return new CountingTask(getMaxProgress());
	}
	
	public class CountingTask extends Task<Void> {

		private double max;
		
		public CountingTask(int totalProg) {
			super();
			max = (double)totalProg;
		}
		
		private boolean writeDF() throws Exception {
			boolean interrupted = false;
			
			//BufferedWriter writer = null;
			try (
					OutputStreamWriter osw = new OutputStreamWriter(
						new FileOutputStream(new File(folder, datafilename)), outputCharset);
					BufferedWriter writer = new BufferedWriter(osw);
				){
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
						updateMessage("Processing " + doc.getTitle());
						
						// subclasses override this
						String s = makeLineFromDocument(doc);
						writer.write(s);
					
					} catch (InterruptedException iex){
						System.err.println("Interrupted - closing down");
						if (writer != null)
							writer.close();
						interrupted = true;
						break;
					
					} catch (Exception ex){
						ex.printStackTrace();
						throw new Exception("Problem with " + doc.getTitle() +
								" [" + ex.getMessage() + "] Skipping this document");
					} finally {
						counter++; // even if we failed, make sure progress goes up
						updateProgress(counter, max); // preprocess + files to date
					}
				}
				
			} catch (Exception ex){
				ex.printStackTrace();
				throw ex;
			}
			return interrupted;
		}
		
		@Override protected void succeeded() {
            super.succeeded();
            System.err.println("succeeded");

            updateMessage("Done!");
        }

        @Override protected void cancelled() {
            super.cancelled();
            System.err.println("cancelled");
            updateMessage("Cancelled!");
        }

        @Override protected void failed() {
            super.failed();
            System.err.println("failed");
            updateMessage("Failed!");
        }
		
        protected void overwritingPreprocess() throws Exception {
        	if (!folder.exists()){
        		boolean b = folder.mkdir();
    			if (!b) throw new Exception("Could not create all the folder elements in " + 
    				folder.getAbsolutePath());
        	}
        }
        
		@Override
		protected Void call() throws Exception {
			double max = (double)getMaxProgress();
			//preProcess();
			overwritingPreprocess(); // bc the interface will have checked
			updateProgress(1.0, max);
			
			boolean interrupted = writeDF();
			if (interrupted)
				return null; // bail out unceremoniously
			
			updateProgress(files.length + 1, max); // even if we blew out on a few
			
			writeColumnsFile();
			updateProgress(files.length + 2, max);
			
			writeRowsFile();
			updateProgress(files.length + 3, max);
			
			writeReadmeFile();
			updateProgress(files.length + 4, max);
			
			postProcess();
			updateProgress(files.length + 5, max);
			
			return null;
		}
	}
	
	public int getMaxProgress() {
		return maxProgress;
	}
	
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
		int oldprog = progress;
		progress = ii;
		mPcs.firePropertyChange("progress", oldprog, ii);
	}
	
	public String getReadmefilename() {
		return readmefilename;
	}
	
	public CountPrinter(File f, String df, File[] fs, Charset chset, Locale loc){
		folder = f;
		datafilename = df;
		files = fs;
		charset = chset;
		locale = loc;
		tokenizer = new SimpleDocumentTokenizer(locale);

		maxProgress = files.length + 5;
		setProgress(0);
	}
	/**
	 * The order of operations: preProcess, writeDataFile, writeColumnsfile,
	 * writeRowsFile, writeReadmeFile, postProcess
	 */
	public File processFiles(boolean showProgress) throws Exception {
		preProcess();
		setProgress(1);
		
		writeDataFile(showProgress);
		setProgress(files.length + 1); // even if we blew out on a few
		
		writeColumnsFile();
		setProgress(files.length + 2);
		
		writeRowsFile();
		setProgress(files.length + 3);
		
		writeReadmeFile();
		setProgress(files.length + 4);
		
		postProcess();
		setProgress(files.length + 5);
		
		return folder;
	}
	
	// makes folder
	protected void preProcess() throws Exception {
		if (folder.exists()){
			System.err.println("Folder already exists");
			throw new Exception("Folder " + folder.getAbsolutePath() + " already exists.");
		}
		boolean b = folder.mkdirs();
		if (!b) throw new Exception("Could not create all the folder elements in " + 
				folder.getAbsolutePath());
		
	}
	
	// this should include the newline if implemented in a subclass
	protected String getDataHeader(){ return null; };
	
	protected void writeDataFile(boolean showProgress) throws Exception {	
		try (
				OutputStreamWriter osw = new OutputStreamWriter(
					new FileOutputStream(new File(folder, datafilename)), outputCharset);
				BufferedWriter writer = new BufferedWriter(osw);
			){
			
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
					setProgress( counter ); // preprocess + files to date
				
				} catch (Exception ex){
					ex.printStackTrace();
					// do something sensible with this
					throw new Exception("Problem with " + doc.getTitle() +
							" [" + ex.getMessage() + "] Skipping this document");
				}
			}
			
		} catch (Exception ex){
			Platform.runLater(new Runnable(){ 
            	public void run() { ex.printStackTrace(); } ;	
            });
		}
	}

	// must be overridden in subclasses
	abstract public String makeLineFromDocument(IYoshikoderDocument doc);

	// null implementations may be overridden in subclasses
	protected void writeRowsFile() throws Exception {}
	protected void writeColumnsFile() throws Exception {}
	protected void postProcess() throws Exception {}
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
	
	public static CountPrinter getCategoryCountPrinter(OldCategoryDictionary dict,
			Format out, File folder, Charset c, Locale l, File[] fs) 
					throws Exception {
		if (out.equals(Format.CSV))
			return new OldCSVCategoryCountPrinter(dict, folder, c, l, fs);
		else
			throw new Exception("No category counters are available for format " + out);
	}
	
	public static CountPrinter getWordCountPrinter(WordCounter rep, 
			Format out, File folder, Charset c, Locale l, File[] fs)
					throws Exception {
		
		if (out == Format.LDAC)
			return new LDACWordCountPrinter(rep, folder, c, l, fs);
		else if (out == Format.MTX)
			return new MTXWordCountPrinter(rep, folder, c, l, fs);
		else
			throw new Exception("No word counters are available for format " + out);
	}
	
	public void addPropertyChangeListener(PropertyChangeListener listener) {
        mPcs.addPropertyChangeListener(listener);
    }
    
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        mPcs.removePropertyChangeListener(listener);
    }

}

