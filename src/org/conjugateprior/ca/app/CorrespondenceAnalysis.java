package org.conjugateprior.ca.app;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;

public class CorrespondenceAnalysis {

	protected File folder;
	protected File liwcData;
	protected File documents;
	protected File words;
	protected int dim = 2;
	
	protected List<String> documentNames;
	protected List<String> wordNames;
	
	
	public CorrespondenceAnalysis(File fold, int ndim) throws Exception {
		super();
		folder = fold;
		liwcData = new File(folder, "data.ldac");
		documents = new File(folder, "documents.csv");
		words = new File(folder, "words.csv");
		
		documentNames = new ArrayList<String>();
		LineIterator it = FileUtils.lineIterator(documents, "UTF-8");
		try {
			while (it.hasNext()){
				String line = it.nextLine();
				if (line.trim().length() > 0)
					documentNames.add(line.trim());
			}
		} finally {
			it.close();
		}
		wordNames = new ArrayList<String>();
		it = FileUtils.lineIterator(words, "UTF-8");
		try {
			while (it.hasNext()){
				String line = it.nextLine();
				if (line.trim().length() > 0)
					wordNames.add(line.trim());
			}
		} finally {
			it.close();
		}
		dim = ndim;
	}
	
	public RealMatrix getLDACMatrixFromFile(File f) throws Exception {

		// check this puts zeros in by default
		Array2DRowRealMatrix wfm = 
				new Array2DRowRealMatrix(documentNames.size(), wordNames.size());

		Matcher m = Pattern.compile("(\\d+):(\\d+)").matcher("");
		
		LineIterator it = FileUtils.lineIterator(f, null);
		try {
			int lineNumber = 0;
			while (it.hasNext()) {
				String line = it.nextLine();
				String[] spl = line.split(" ");
				for (int i = 1; i < spl.length; i++) {
					if (!m.reset(spl[i]).find())
						throw new Exception("Failed to parse LDAC element " + spl[i]);
					int wd = Integer.parseInt( m.group(1) );	
					int count = Integer.parseInt( m.group(2) );

					wfm.setEntry(lineNumber, wd, count);		
				}
				lineNumber++;
			}

		} finally {
			LineIterator.closeQuietly(it);
		}
		
		return wfm;
	}
	
	
	public static void main(String[] args) throws Exception {
		File fo = new File("/Users/will/Dropbox/teaching/archived/texas/debate-ldac");
		
		CorrespondenceAnalysis an = new CorrespondenceAnalysis(fo, 1);
		an.getLDACMatrixFromFile(new File(fo, "data.ldac"));
			
	}
	
}
