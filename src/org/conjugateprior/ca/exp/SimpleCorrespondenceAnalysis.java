package org.conjugateprior.ca.exp;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.DefaultRealMatrixChangingVisitor;
import org.apache.commons.math3.linear.DefaultRealMatrixPreservingVisitor;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;

class SimpleCorrespondenceAnalysis {
	
	double[] r; // in probs
	double[] c;
	double total;
	
	class MarginMaker extends DefaultRealMatrixPreservingVisitor {
		double[] rowMargin = null;
		double[] colMargin = null;
		double total = 0;
		
		@Override
		public void start(int rows, int columns, int startRow, int endRow,
				int startColumn, int endColumn) {
			super.start(rows, columns, startRow, endRow, startColumn, endColumn);
			rowMargin = new double[rows];
			colMargin = new double[columns];
		}
		
		@Override
		public void visit(int row, int column, double value) {
			rowMargin[row] += value;
			colMargin[column] += value;
		}
		
		@Override
		public double end() {
			for (int ii = 0; ii < rowMargin.length; ii++) {
				total += rowMargin[ii];
			}
			return total;
		}
	}
	
	class IndependenceResidualMaker extends DefaultRealMatrixChangingVisitor {
		@Override
		public double visit(int row, int column, double value) {
			return  (value/total - r[row]/total * c[column]/total) / 
					(Math.sqrt(r[row]/total) * Math.sqrt(c[column]/total));
		}
	}
	
	double[][] F;
	double[][] G;
	double[][] Phi;
	double[][] Lambda;
	double[] Eig;
	int dim;
	
	String[] columnNames;
	String[] rowNames;
	RealMatrix wfm;
	
	public SimpleCorrespondenceAnalysis(File fold, int dimension) throws Exception {
		dim = dimension;
		if ((new File(fold, "data.ldac")).exists())
			getLDAC(fold);
		else if ((new File(fold, "data.csv")).exists())
			getCSV(fold);
		else if ((new File(fold, "data.mtx")).exists())
			getMTX(fold);
		else
			throw new Exception("Could not find data.ldac (or .mtx or .csv) in " + fold.getAbsolutePath());
		
		computeSimpleCorrespondenceAnalysis();
	}

	protected void getCSV(File fold) throws Exception {
		File csvData = new File(fold, "data.csv");
		InputStreamReader reader = new InputStreamReader(
				new FileInputStream(csvData), Charset.forName("UTF-8"));
		
		List<CSVRecord> list = null;
		try (CSVParser parser = new CSVParser(reader, CSVFormat.EXCEL)){
			list = parser.getRecords();
		}
		
		CSVRecord lineOne = list.get(0);
		columnNames = new String[lineOne.size()-1];
		rowNames = new String[list.size()-1];
		wfm = new Array2DRowRealMatrix(rowNames.length, columnNames.length);
		
		Iterator<CSVRecord> iterator = list.iterator(); 
		CSVRecord header = iterator.next();
		for (int ii = 1; ii < header.size(); ii++)
			columnNames[ii-1] = header.get(ii);
		int row = 0;
		while (iterator.hasNext()) {
			CSVRecord csvRecord = iterator.next();
			rowNames[row] = csvRecord.get(0);
			for (int ii = 1; ii < csvRecord.size(); ii++)
				wfm.setEntry(row, ii-1, Integer.parseInt( csvRecord.get(ii) ));
			row++;
		}
	}

	protected void getMTX(File fold) throws Exception {
		throw new Exception("Not implemented!");
	}
	
	protected void getLDAC(File fold) throws Exception {
		File ldacData = new File(fold, "data.ldac");
		File documents = new File(fold, "documents.csv");
		File words = new File(fold, "words.csv");
		
		List<String> documentNames = new ArrayList<String>();
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
		List<String> wordNames = new ArrayList<String>();
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
		columnNames = wordNames.toArray(new String[wordNames.size()]);
		rowNames = documentNames.toArray(new String[documentNames.size()]);
		wfm = new Array2DRowRealMatrix(documentNames.size(), wordNames.size());

		Matcher m = Pattern.compile("(\\d+):(\\d+)").matcher("");

		it = FileUtils.lineIterator(ldacData, null);
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
	}
		
	public SimpleCorrespondenceAnalysis(RealMatrix wfmatrix, int dimension, String[] rNames, String[] cNames) {
		rowNames = rNames;
		columnNames = cNames;
		wfm = wfmatrix;
		dim = dimension; 	
		
		computeSimpleCorrespondenceAnalysis();
	}
	    	
	protected void computeSimpleCorrespondenceAnalysis(){
		MarginMaker mm = new MarginMaker();
    	total = wfm.walkInOptimizedOrder(mm);
    	r = mm.rowMargin;
    	c = mm.colMargin;

    	Phi = new double[r.length][dim];
    	Lambda = new double[c.length][dim];
    	F = new double[r.length][dim];
    	G = new double[c.length][dim];

    	RealMatrix P = wfm.copy();
    	P.walkInOptimizedOrder(new IndependenceResidualMaker());
    	SingularValueDecomposition svd = new SingularValueDecomposition(P);
    	svd.getU().copySubMatrix(0, r.length-1, 0, dim-1, Phi);
    	svd.getV().copySubMatrix(0, c.length-1, 0, dim-1, Lambda);
    	double[] Da = svd.getSingularValues();
    	Eig = new double[Da.length];
    	for (int ii = 0; ii < Da.length; ii++)
			Eig[ii] = Da[ii] * Da[ii];
		// finish reweighting U and V
    	for (int jj = 0; jj < dim; jj++) {
    		for (int ii = 0; ii < r.length; ii++) {
    			Phi[ii][jj] /= Math.sqrt(r[ii]/total);
    			F[ii][jj] = Phi[ii][jj] * Da[jj];
    		}
    		for (int ii = 0; ii < c.length; ii++) {
    			Lambda[ii][jj] /= Math.sqrt(c[ii]/total);
    			G[ii][jj] = Lambda[ii][jj] * Da[jj];
    		}
    	}       
	}
	
	public String[] getColumnNames() {
		return columnNames;
	}

	public String[] getRowNames() {
		return rowNames;
	}

	double[] getRowTotals(){
		return r;
	}
	
	double[] getColumnTotals(){
		return c;
	}
	
	double getTotalCount(){
		return total;
	}
	
	double[][] getPrincipalRowCoordinates(){
		return F;
	}
	
	double[][] getPrincipalColumnCoordinates(){
		return G;
	}
	
	double[][] getStandardRowCoordinates(){
		return Phi;
	}
	
	double[][] getStandardColumnCoordinates(){
		return Lambda;
	}
	
	double[] getEigValues(){
		return Eig;
	}
	
	double[] getEigVariances(){
		double[] eigs = getEigValues();
		double[] per = new double[eigs.length];
		double tot = 0;
		for (int ii = 0; ii < per.length; ii++){
			per[ii] = eigs[ii];
			tot += per[ii];
		}
		for (int ii = 0; ii < per.length; ii++)
			per[ii] = per[ii]/tot * 100;
		return per;
	}
	
	int getDimension(){
		return dim;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		Formatter f = new Formatter(sb, Locale.UK);
		double[] eigs = getEigValues();
		double[] per = getEigVariances();
		double rest = 0;
		for (int ii = 2; ii < per.length; ii++)
			rest += per[ii];
		
		sb.append("Inertia\n\n");
		f.format("First: %3.2f%% Second: %3.2f%% Rest: %3.2f%%\n", per[0], per[1], rest);
		
		sb.append("\nPrincipal Rows\n\n");
		for (int ii = 0; ii < F.length; ii++) 
			f.format("%s\t%8.4f\t%8.4f\n", getRowNames()[ii], F[ii][0], F[ii][1]);
		sb.append("\nPrincipal Columns\n\n");
		for (int ii = 0; ii < G.length; ii++) 
			f.format("%s\t%8.4f\t%8.4f\n", getColumnNames()[ii], G[ii][0], G[ii][1]);
		sb.append("\nStandardized Rows\n\n");
		for (int ii = 0; ii < Phi.length; ii++) 
			f.format("%s\t%8.4f\t%8.4f\n", getRowNames()[ii], Phi[ii][0], Phi[ii][1]);
		sb.append("\nStandardized Columns\n\n");
		for (int ii = 0; ii < Lambda.length; ii++) 
			f.format("%s\t%8.4f\t%8.4f\n", getColumnNames()[ii], Lambda[ii][0], Lambda[ii][1]);
		
		return sb.toString();
	}
	
	public static void main(String[] args) {
		RealMatrix smoke = new Array2DRowRealMatrix(new double[][]{
        		{4,2,3,2},{4,3,7,4},{25,10,12,4},{18,24,33,13},
        		{10,6,7,2}});
        System.err.println(smoke);
        SimpleCorrespondenceAnalysis sca = new SimpleCorrespondenceAnalysis(smoke, 2, 
        		new String[]{"SM", "JM", "SE", "JE", "SC"}, 
        		new String[]{"none", "light",  "medium", "heavy"});
        System.out.println(sca);
	}
}