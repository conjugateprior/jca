package org.conjugateprior.ca.app;

import java.io.BufferedWriter;
import java.io.File;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.conjugateprior.ca.AbstractYoshikoderDocument;
import org.conjugateprior.ca.IYoshikoderDocument;
import org.conjugateprior.ca.SimpleDocumentTokenizer;
import org.conjugateprior.ca.SimpleYoshikoderDocument;

public class Description extends AbstractCounter {

	protected Set<String> vocab = new HashSet<String>();
	protected String descriptionFilename = "description.csv";
	
	static class Desc {
		int wordCount;
		int tokenCount; 
		int hapaxes;
		double propOfVocab;
		
		static private DecimalFormat df = new DecimalFormat("#.###");
		
		public Desc(int wc, int tk, int hap) {
			wordCount = wc;
			tokenCount = tk;
			hapaxes = hap;
		}
		
		public String toString(String sep) {
			return "" + wordCount + sep + tokenCount + sep + hapaxes + sep +
					df.format(propOfVocab) + sep + 
					df.format(((double)tokenCount)/wordCount) ;
		}

		static public String getHeader(String sep) {
			return "Document" + sep + "TokenCount" + sep + "TypeCount" + 
					sep + "HapaxCount" + sep + "PropVocabUsed" + sep + "TypeTokenRatio";
		}
		
	}

	protected Map<String,Desc> docToDesc = new HashMap<String,Desc>();

	public Description() {
		super();
	}

	public void processFiles() throws Exception {
		SimpleDocumentTokenizer tok = 
				new SimpleDocumentTokenizer(locale);
		for (File f : files) {
			IYoshikoderDocument idoc = 
					new SimpleYoshikoderDocument(f.getName(), 
							AbstractYoshikoderDocument.getTextFromFile(f, encoding),
							null, tok);	
			Map <String,Integer> map = idoc.getWordCountMap();
			int hap = 0;
			for (Map.Entry<String,Integer> entry : map.entrySet())
				if (entry.getValue() == 1) hap += 1;
			vocab.addAll(map.keySet());
			
			Desc d = new Desc(idoc.getDocumentLength(), map.size(), hap);
			docToDesc.put(f.getName(), d); // in on the filename 
		}
		int wcAll = 0;
		int tkAll = 0;
		//Desc all = new Desc(wc, tk, hap);
		
		double vocabSize = vocab.size();
		if (outputFolder != null){
			FileUtils.forceMkdir(outputFolder);
			File ff = new File(outputFolder, descriptionFilename);	
			try (BufferedWriter writer = getBufferedWriter(ff)){
				writer.write(Desc.getHeader(","));
				writer.newLine();
				
				for (File f : files) {
					Desc d = docToDesc.get(f.getName());
					d.propOfVocab = d.tokenCount / vocabSize;
					writer.write(StringEscapeUtils.escapeCsv(f.getName()));
					writer.write("," + d.toString(","));
					writer.newLine();
				}
				writer.flush(); // do we need this really?			
			}		
		} else {
			try (BufferedWriter writer = getBufferedWriter()){
				writer.write(Desc.getHeader("\t"));
				writer.newLine();
				
				for (File f : files) {
					Desc d = docToDesc.get(f.getName());
					d.propOfVocab = d.tokenCount / vocabSize;
					writer.write(f.getName());
					writer.write("\t" + d.toString("\t"));
					writer.newLine();
				}
				writer.flush(); // do we need this really?			
			}	
		}
	}
	
	
	public static void main(String[] args) throws Exception {
		Description de = new Description();
		de.setFiles(new File[]{new File("/Users/will/Dropbox/blogposts/speeches")});
		de.processFiles();
	}

}
