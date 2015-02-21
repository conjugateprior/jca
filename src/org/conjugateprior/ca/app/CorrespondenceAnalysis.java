package org.conjugateprior.ca.app;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.OpenMapRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;

public class CorrespondenceAnalysis extends AbstractCounter {

	protected File folder;
	protected File liwcData;
	protected File documents;
	protected File words;
	protected int dim = 2;
	
	protected List<String> documentNames;
	protected List<String> wordNames;
	
	public CorrespondenceAnalysis(File fold, int ndim) {
		super();
		folder = fold;
		liwcData = new File(folder, "data.liwc");
		documents = new File(folder, "documents.csv");
		words = new File(folder, "words.csv"); // TODO or vocabulary?
		dim = ndim;
	}
	
	public RealMatrix getLiwcMatrixFromFile(File f) throws IOException {

		// check this puts zeros in by default
		Array2DRowRealMatrix wfm = 
				new Array2DRowRealMatrix(documentNames.size(), wordNames.size());
		LineIterator it = FileUtils.lineIterator(f, null); // platform default enc
		int lineNumber = 0;
		try {
			while (it.hasNext()){
				String line = it.nextLine();
				String[] spl = line.split(" "); // TODO make me more efficient
				for (int i = 1; i < spl.length; i++) {
					int ind = spl[i].indexOf(":");
					int wd = Integer.parseInt( spl[i].substring(0, ind) );
					int count = Integer.parseInt( spl[i].substring(ind+1, spl[i].length()) );
					wfm.setEntry(lineNumber, wd, count);
				}
			}
			return wfm;
		} finally {
			it.close();
		}
	}
	
	public void processFiles() throws Exception {
		documentNames = new ArrayList<String>();
		LineIterator it = FileUtils.lineIterator(documents, getEncoding().name());
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
		it = FileUtils.lineIterator(words, getEncoding().name());
		try {
			while (it.hasNext()){
				String line = it.nextLine();
				if (line.trim().length() > 0)
					wordNames.add(line.trim());
			}
		} finally {
			it.close();
		}
		
		RealMatrix wfm = getLiwcMatrixFromFile(liwcData);
		
		// well this is clunky...
		double[] colSums = new double[wfm.getColumnDimension()];
		double[] rowSums = new double[wfm.getRowDimension()];
		for (int ii = 0; ii < rowSums.length; ii++) {
			for (int jj = 0; jj < colSums.length; jj++) {
				double e = wfm.getEntry(ii, jj);
				colSums[jj] += e;
				rowSums[ii] += e;
			}
		}
		int N = 0;
		for (int ii = 0; ii < rowSums.length; ii++)
			N += rowSums[ii]; 
		for (int ii = 0; ii < rowSums.length; ii++) {
			for (int jj = 0; jj < colSums.length; jj++) {
				double e = wfm.getEntry(ii, jj);
				double exp = (rowSums[ii] * colSums[jj]) / (N * N); 
				wfm.setEntry(ii, jj, (e/N - exp)/Math.sqrt(exp)); // chi-squared residual
			}
		}
		SingularValueDecomposition svd = 
				new SingularValueDecomposition(wfm);
		
		/*
	    Dr <- drop(x %*% (rep(1/N, ncol(x))))
	    Dc <- drop((rep(1/N, nrow(x))) %*% x)
	    if (any(Dr == 0) || any(Dc == 0)) 
	        stop("empty row or column in table")
	    x1 <- x/N - outer(Dr, Dc)
	    Dr <- 1/sqrt(Dr)
	    Dc <- 1/sqrt(Dc)
	    if (is.null(dimnames(x))) 
	        dimnames(x) <- list(Row = paste("R", 1L:nrow(x)), Col = paste("C", 
	            1L:ncol(x)))
	    if (is.null(names(dimnames(x)))) 
	        names(dimnames(x)) <- c("Row", "Column")
	    X.svd <- svd(t(t(x1 * Dr) * Dc))
	    dimnames(X.svd$u) <- list(rownames(x), NULL)
	    dimnames(X.svd$v) <- list(colnames(x), NULL)
	    res <- list(cor = X.svd$d[1L:nf], rscore = X.svd$u[, 1L:nf] * 
	        Dr, cscore = X.svd$v[, 1L:nf] * Dc, Freq = x)
		
		
		
		*/
	}

	public static void main(String[] args) {
		Array2DRowRealMatrix wfm = 
				new Array2DRowRealMatrix(3, 2);
		System.err.println(wfm);
	}
	
}
