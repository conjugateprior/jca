package org.conjugateprior.ca;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.danishStemmer;
import org.tartarus.snowball.ext.dutchStemmer;
import org.tartarus.snowball.ext.englishStemmer;
import org.tartarus.snowball.ext.finnishStemmer;
import org.tartarus.snowball.ext.germanStemmer;
import org.tartarus.snowball.ext.hungarianStemmer;
import org.tartarus.snowball.ext.italianStemmer;
import org.tartarus.snowball.ext.norwegianStemmer;
import org.tartarus.snowball.ext.portugueseStemmer;
import org.tartarus.snowball.ext.romanianStemmer;
import org.tartarus.snowball.ext.russianStemmer;
import org.tartarus.snowball.ext.spanishStemmer;
import org.tartarus.snowball.ext.swedishStemmer;
import org.tartarus.snowball.ext.turkishStemmer;

public class WordReporter {

	protected BufferedWriter writer;
	protected BufferedWriter docsWriter;
	protected BufferedWriter wordsWriter;
	protected Charset outputCharset = Charset.forName("UTF8");
	protected String newline = "\n";
	
	protected int idIndex = 0;
	protected Map<String,Integer> wordToId = new HashMap<String,Integer>();
	protected FilterPipe fp;
	
	public class FilterPipe {
		BaseFilter placeholder = new BaseFilter();
		List<BaseFilter> pipe = new ArrayList<BaseFilter>();

		public FilterPipe(){
			pipe.add(placeholder); // placeholder filter
		}
		
		public void addFilter(BaseFilter filt){
			if (pipe.get(0).equals(placeholder))
				pipe.clear();
			pipe.add(filt);
		}
		
		public String filter(String s){
			String ss = s;
			for (BaseFilter f : pipe) {
				ss = f.filter(ss);
				if (ss == null)
					return null;
			}
			return ss;
		}
	}
	
	// the base class
	public class BaseFilter {		
		public String filter(String wd){
			return wd; // filtered result or null if you don't 
		}
	}
	// don't need a lowercase filter because the tokeniser already does that
	public class NoCurrencyFilter extends BaseFilter {
		public String filter(String wd){
			if (Character.getType(wd.charAt(0)) == Character.CURRENCY_SYMBOL)
				return null;
			return wd;
		}
	}
	
	public class NoNumberFilter extends BaseFilter {
		public String filter(String wd){
			if (Character.isDigit(wd.charAt(0)))
				return null;
			return wd;
		}
	}
	
	public class Stemmer extends BaseFilter {	
		SnowballStemmer stemmer; // default
		Stemmer(SnowballStemmer stem){
			stemmer = stem;
		}
		public String filter(String wd){
			stemmer.setCurrent(wd);
			boolean b = stemmer.stem();
			if (b)
				return stemmer.getCurrent();
			return null;
		}
	}
	
	public Stemmer getStemmerByName(String name){
		if (name.equals("english")) return new Stemmer(new englishStemmer());
		else if (name.equals("dutch")) return new Stemmer(new dutchStemmer()); 
		else if (name.equals("danish")) return new Stemmer(new danishStemmer()); 
		else if (name.equals("finnish")) return new Stemmer(new finnishStemmer()); 
		else if (name.equals("german")) return new Stemmer(new germanStemmer()); 
		else if (name.equals("hungarian")) return new Stemmer(new hungarianStemmer()); 
		else if (name.equals("italian")) return new Stemmer(new italianStemmer()); 
		else if (name.equals("norwegian")) return new Stemmer(new norwegianStemmer()); 
		else if (name.equals("turkish")) return new Stemmer(new turkishStemmer()); 
		else if (name.equals("portuguese")) return new Stemmer(new portugueseStemmer()); 
		else if (name.equals("russian")) return new Stemmer(new russianStemmer()); 
		else if (name.equals("spanish")) return new Stemmer(new spanishStemmer()); 
		else if (name.equals("romanian")) return new Stemmer(new romanianStemmer()); 
		else if (name.equals("swedish")) return new Stemmer(new swedishStemmer()); 
		else return null;
	}
	
	public class StopwordFilter extends BaseFilter {
		Set<String> stops = new HashSet<String>();
		
		public StopwordFilter(Set<String> sts){
			stops = sts;
		}
		public StopwordFilter(File rfile) throws Exception {
			BufferedReader sb = null;
			try {
				sb = new BufferedReader(
						new InputStreamReader(new FileInputStream(rfile), "UTF-8"));
				String line = null;
				while ((line = sb.readLine()) != null){
					String l = line.trim();
					if (l.length() > 0)
						stops.add(l);
				}
			} catch (Exception ex){
				throw new Exception("Could not get stopwords from " + rfile.getAbsolutePath());
				
			} finally {
				if (sb != null)
					sb.close();
			}
		}
		public String filter(String wd){
			if (stops.contains(wd))
				return null;
			return wd;
		}
	}
	
	public WordReporter() {
		fp = new FilterPipe();
	}
	
	public void addFilter(BaseFilter filt){
		fp.addFilter(filt);
	}
	
	protected Map<String,Integer> applyFilters(Map<String, Integer> map, FilterPipe filt){
		Map<String,Integer> m = new HashMap<String,Integer>();
		for (String key : map.keySet()) {
			String newKey = filt.filter(key);
			if (newKey == null)
				continue;
			
			Integer oldVal = map.get(key);
			Integer cnt = m.get(newKey);
			if (cnt == null)
				m.put(newKey, oldVal);
			else
				m.put(newKey, oldVal + cnt);
		} 
		return m;
	}
		
	// no newline
	public String makeLDALineFromDocument(IYoshikoderDocument doc){
		Map<String,Integer> map = applyFilters(doc.getWordCountMap(), fp);

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
		return sb.toString();
	}

	class MTXLine {
		int maxWordTypeCountId = -1;
		int maxWordIndex = -1;
		String vals = null;
		public MTXLine(int mwi, int mwtci, String s) {
			maxWordIndex = mwi;
			maxWordTypeCountId = mwtci;
			vals = s;
		}
	}
	
	// no newline TODO make an object that represents the highest word index, number of elements
	// and a newline separated stacked string
	public MTXLine makeMTXLineFromDocument(IYoshikoderDocument doc){
		Map<String,Integer> map = applyFilters(doc.getWordCountMap(), fp);

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
		
		return new MTXLine(mwi, mwtci, s).toString();
	}

	
	public void openStreamingReport(OutputStream out, OutputStream docsOut, OutputStream wordsOut) throws Exception {
		OutputStreamWriter osw = new OutputStreamWriter(out, outputCharset);
		writer = new BufferedWriter(osw);
		OutputStreamWriter words = new OutputStreamWriter(wordsOut, outputCharset);
		wordsWriter = new BufferedWriter(words);
		OutputStreamWriter docs = new OutputStreamWriter(docsOut, outputCharset);
		docsWriter = new BufferedWriter(docs);
	}
	
	public void openStreamingReport(File f, File fdocs, File fwords) throws Exception {
		openStreamingReport(new FileOutputStream(f), 
				new FileOutputStream(fdocs), new FileOutputStream(fwords));		
	}

	public void streamLDACReportLine(IYoshikoderDocument doc) throws IOException {
		docsWriter.write(doc.getTitle() + newline); 
		String line = makeLDALineFromDocument(doc);
		writer.write(line + newline);
	}
	
	// TODO fix me to use the functions above
	public void streamMTXReportLine(IYoshikoderDocument doc) throws IOException {
		docsWriter.write(doc.getTitle() + newline); 
		String line = makeMTXLineFromDocument(doc);
		writer.write(line + newline);
	}
	
	public void closeStreamingReport() throws IOException {
		if (writer != null)
			writer.close();
		if (docsWriter != null)
			docsWriter.close();
		
		// push out the words one per line, 
		// sorted by identifier so the row numbers are the feature id numbers.
		List<Entry<String,Integer>> lst = 
				new ArrayList<Entry<String,Integer>>(wordToId.entrySet());
		Collections.sort(lst, new Comparator<Entry<String, Integer>>() {
			public int compare(Entry<String, Integer> o1,
					Entry<String, Integer> o2) {
				return o1.getValue().compareTo(o2.getValue());
			}
		});
		for (Entry<String, Integer> entry : lst)
			wordsWriter.write(entry.getKey() + newline);
		
		if (wordsWriter != null)
			wordsWriter.close();
	}

	public static void main(String[] args) throws Exception {
		String doc1 = "Mary had 1 little lamb, her fleece was white as snow";
		String doc2 = "And everywhere that Mary went, the lamb was sure to go";
		IDocumentTokenizer tok = new SimpleDocumentTokenizer(Locale.ENGLISH);
		IYoshikoderDocument d1 = new SimpleYoshikoderDocument("doc1", doc1, null, tok);
		IYoshikoderDocument d2 = new SimpleYoshikoderDocument("doc2", doc2, null, tok);
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ByteArrayOutputStream out1 = new ByteArrayOutputStream();
		ByteArrayOutputStream out2 = new ByteArrayOutputStream();

		WordReporter rep = new WordReporter();
		rep.addFilter(rep.new NoNumberFilter());
		rep.addFilter(rep.getStemmerByName("english"));
		rep.openStreamingReport(out, out1, out2);
		rep.streamLDACReportLine(d1);
		rep.streamLDACReportLine(d2);
		rep.closeStreamingReport();
		
		System.out.println(out.toString());
		System.out.println(out1.toString());
		System.out.println(out2.toString());
		
	}
	
}
