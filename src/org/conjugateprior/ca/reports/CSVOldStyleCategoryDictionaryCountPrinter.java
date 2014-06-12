package org.conjugateprior.ca.reports;

import java.io.File;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import javafx.scene.control.TreeItem;

import org.conjugateprior.ca.DCat;
import org.conjugateprior.ca.DPat;
import org.conjugateprior.ca.FXCategoryDictionary;
import org.conjugateprior.ca.IYoshikoderDocument;

public class CSVOldStyleCategoryDictionaryCountPrinter extends
		CSVFXCategoryDictionaryCountPrinter {

	public CSVOldStyleCategoryDictionaryCountPrinter(FXCategoryDictionary dict, File f,
			String df, File[] fs, Charset chset, Locale loc) {
		super(dict, f, df, fs, chset, loc);
	}

	// this time with match counts not the indices
	protected void fillTreeWithMatchCounts(IYoshikoderDocument doc){
		// optimise later
		//Set<String> vocab = doc.getWordTypes();
		for (TreeItem<DCat> node : categoryNodesInPrintOrder){
			Set<Integer> indexMatches = new HashSet<Integer>();
			Set<DPat> pats = node.getValue().getPatterns();
			int rawCount = 0; // including double counted items!
			for (DPat pat : pats) {
				Set<Integer> indices = 
						doc.getAllMatchingTokenIndexesForPattern(pat.getRegexps());
				rawCount += indices.size();
			}
			indexMatches.add(new Integer(rawCount)); // here one number, the total count
			node.getValue().setMatchedIndices(indexMatches);
		}
		// percolate
		for (TreeItem<DCat> node : categoryNodesInPrintOrder){
			if (node.isLeaf()){
				TreeItem<DCat> current = node;
				TreeItem<DCat> parent = node.getParent();
				while (parent != null){
					// (mis)use the indices to hold and update single counts 
					Set<Integer> count = current.getValue().getMatchedIndices();
					Integer co = count.iterator().next();
					Set<Integer> pcount = parent.getValue().getMatchedIndices();
					Integer pco = pcount.iterator().next();
					pcount.clear();
					pcount.add(new Integer(pco + co));
					
					current = parent;
					parent = current.getParent();
				}
			}
		}
	}
	
	public String makeLineFromDocument(IYoshikoderDocument doc){
		fillTreeWithMatchCounts(doc);

		StringBuilder sb = new StringBuilder();
		sb.append(excelEscape(doc.getTitle()));
		for (TreeItem<DCat> cn : categoryNodesInPrintOrder) {
			sb.append(fieldSeparator);
			// the diff is right here:
			Integer num = cn.getValue().getMatchedIndices().iterator().next();
			sb.append(num);
		}
		sb.append(fieldSeparator);
		sb.append(doc.getDocumentLength());
		sb.append(newline);
		return sb.toString();
	}
	
}
