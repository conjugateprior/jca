package org.conjugateprior.ca.reports;

import java.io.File;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.conjugateprior.ca.OldCategoryDictionary;
import org.conjugateprior.ca.OldCategoryDictionary.DictionaryCategory;
import org.conjugateprior.ca.OldCategoryDictionary.DictionaryPattern;
import org.conjugateprior.ca.IYoshikoderDocument;

/**
 * @deprecated
 * @author will
 *
 */
public class OldCSVOldStyleCategoryCountPrinter extends OldCSVCategoryCountPrinter {

	public OldCSVOldStyleCategoryCountPrinter(OldCategoryDictionary dict, File folder,
			Charset c, Locale l, File[] f) {
		super(dict, folder, c, l, f);
	}

	// this time with match counts not the indices
	protected void fillTreeWithMatchCounts(IYoshikoderDocument doc){
		// optimise later
		//Set<String> vocab = doc.getWordTypes();
		for (DictionaryCategory node : categoryNodesInPrintOrder){
			Set<Integer> indexMatches = new HashSet<Integer>();
			Set<DictionaryPattern> pats = node.getPatterns();
			int rawCount = 0; // including double counted items!
			for (DictionaryPattern pat : pats) {
				Set<Integer> indices = 
						doc.getAllMatchingTokenIndexesForPattern(pat.getRegexps());
				rawCount += indices.size();
			}
			indexMatches.add(new Integer(rawCount)); // here one number, the total count
			node.setMatchedIndices(indexMatches);
		}
		// percolate
		for (DictionaryCategory node : categoryNodesInPrintOrder){
			if (node.isLeaf()){
				DictionaryCategory current = node;
				DictionaryCategory parent = (DictionaryCategory)node.getParent();
				while (parent != null){
					// (mis)use the indices to hold and update single counts 
					Set<Integer> count = current.getMatchedIndices();
					Integer co = count.iterator().next();
					Set<Integer> pcount = parent.getMatchedIndices();
					Integer pco = pcount.iterator().next();
					pcount.clear();
					pcount.add(new Integer(pco + co));
					
					current = parent;
					parent = (DictionaryCategory)current.getParent();
				}
			}
		}
	}
	
	public String makeLineFromDocument(IYoshikoderDocument doc){
		fillTreeWithMatchCounts(doc);

		StringBuilder sb = new StringBuilder();
		sb.append(excelEscape(doc.getTitle()));
		for (DictionaryCategory cn : categoryNodesInPrintOrder) {
			sb.append(fieldSeparator);
			// the diff is right here:
			Integer num = cn.getMatchedIndices().iterator().next();
			sb.append(num);
		}
		sb.append(fieldSeparator);
		sb.append(doc.getDocumentLength());
		sb.append(newline);
		return sb.toString();
	}
	
}
