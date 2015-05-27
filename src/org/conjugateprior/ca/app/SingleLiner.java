package org.conjugateprior.ca.app;

import java.io.BufferedWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.conjugateprior.ca.AbstractYoshikoderDocument;
import org.conjugateprior.ca.YoshikoderDocument;
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
	
	protected String[] fileLabels;
	
	public String[] getRecursiveDepthOneFileArrayLabels(String[] files) throws Exception {
		List<File> filelist = new ArrayList<File>();
		for (int ii = 0; ii < files.length; ii++) {
			try {
				File g = new File(files[ii]);
				if (!g.exists())
					throw new Exception(g.getName() + " does not exist");
				filelist.add(g);
			} catch (Exception ex){
				System.err.println(ex.getMessage());
			}
		}
		return getRecursiveDepthOneFileArrayLabels(filelist.toArray(new File[filelist.size()]));
	}
	
	public String[] getRecursiveDepthOneFileArrayLabels(File[] files) throws Exception {
		List<String> filelist = new ArrayList<String>();
		File fail = null;
		for (int ii = 0;  ii < files.length; ii++) {
			File f = files[ii];
			if (!f.exists()){
				fail = f;
				break;
			} if (f.isDirectory()){
				File[] contents = f.listFiles();
				for (int jj = 0; jj < contents.length; jj++) {
					if (!contents[jj].isDirectory() && !contents[jj].getName().startsWith("."))
						if (contents[jj].length() > 0)
							filelist.add(f.getName()); // add the folder name
				}
			} else {
				if (f.length() > 0)
					filelist.add("NoLabel"); // add the fact that there is no folder
			}
		}
		if (fail != null)
			throw new Exception("File " + fail.getAbsolutePath() + " does not exist.");
		
		return filelist.toArray(new String[filelist.size()]);
	}
	
	public void setFiles(String[] files) throws Exception {
		this.fileLabels = getRecursiveDepthOneFileArrayLabels(files); 
		this.files = getRecursiveDepthOneFileArray(files);
	}

	public void setFiles(File[] files) throws Exception {
		this.fileLabels = getRecursiveDepthOneFileArrayLabels(files);
		this.files = getRecursiveDepthOneFileArray(files);
	}

	protected String removeWhiteSpace(String s){
		return s.replaceAll("[\\s]+", "-");
	}
	
	public void processFiles() throws Exception {
		try (BufferedWriter writer = getBufferedWriter(outputFile)){			
			SimpleDocumentTokenizer tok = 
					new SimpleDocumentTokenizer(locale);
			int findex = 0;
			for (File f : files) {
				YoshikoderDocument idoc = 
						new SimpleYoshikoderDocument(f.getName(), 
								AbstractYoshikoderDocument.getTextFromFile(f, encoding),
								null, tok);	
				writer.write(removeWhiteSpace(fileLabels[findex++]));
				writer.write("\t" + removeWhiteSpace(idoc.getTitle()) + "\t"); 
				writer.write(idoc.getText().replaceAll("[\\s]+", " "));
				writer.newLine();
				
				if (!getSilent())
					System.err.print(".");
				
				//writer.flush(); // do we need this really?
			}
			if (!getSilent())
				System.err.println();
		}
	}

	public static void main(String[] args) throws Exception {
		// /Users/will/Dropbox/blogposts/speeches
		SingleLiner sl = new SingleLiner();
		sl.setFiles(new String[]{"/Users/will/Dropbox/blogposts/speeches/2010_BUDGET_01_Brian_Lenihan_FF.txt"});
		sl.setOutputFile(new File("/Users/will/Dropbox/blogposts/speeches-as-one-file.txt"));
		sl.processFiles();
	}
	
}
