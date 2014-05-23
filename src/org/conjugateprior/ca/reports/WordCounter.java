package org.conjugateprior.ca.reports;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.conjugateprior.ca.IDocumentTokenizer;
import org.conjugateprior.ca.IYoshikoderDocument;
import org.conjugateprior.ca.SimpleDocumentTokenizer;
import org.conjugateprior.ca.SimpleYoshikoderDocument;
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

public class WordCounter {
	
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
			return wd; // pass everything through 
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
	
	protected Stemmer getStemmerByName(String name){
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
	
	public WordCounter() {
		fp = new FilterPipe();
	}
	
	public void addFilter(BaseFilter filt){
		fp.addFilter(filt);
	}
	
	public Map<String,Integer> getWordCountMapFromDocument(IYoshikoderDocument doc){
		Map<String,Integer> map = applyFilters(doc.getWordCountMap(), fp);
		return map;
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
	
	public void addNoCurrencyFilter(){
		fp.addFilter(new NoCurrencyFilter());
	}

	public void addNoNumberFilter(){
		fp.addFilter(new NoNumberFilter());
	}

	public void addStemmingFilter(String name) throws Exception {
		Stemmer st = getStemmerByName(name);
		if (st == null)
			throw new Exception("Could not construct a stemmer for language " + 
					name);
		fp.addFilter(st);
	}

	public void addStopwordFilter(File stopwordFile) throws Exception {
		StopwordFilter sf = new StopwordFilter(stopwordFile);
		fp.addFilter(sf);
	}
	
	public void addStopwordFilter(Set<String> stopwords) throws Exception {
		StopwordFilter sf = new StopwordFilter(stopwords);
		fp.addFilter(sf);
	}
	
	
	public static void main(String[] args) throws Exception {
		String doc1 = "Mary had a  lttle Lamb. Mary had 1 little lamb, her fleece was white as snow";
		String doc2 = "And everywhere that Mary went, the lamb was sure to go";
		IDocumentTokenizer tok = new SimpleDocumentTokenizer(Locale.ENGLISH);
		IYoshikoderDocument d1 = new SimpleYoshikoderDocument("doc1", doc1, null, tok);
		IYoshikoderDocument d2 = new SimpleYoshikoderDocument("doc2", doc2, null, tok);
		
		WordCounter rep = new WordCounter();
		rep.addNoNumberFilter();
		Set<String> ss = new HashSet<String>();
		ss.add("as");
		ss.add("hert");
		rep.addStopwordFilter(ss);
		Map<String,Integer> mp = rep.getWordCountMapFromDocument(d1);
		System.out.println( mp );
		
		/*
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ByteArrayOutputStream out1 = new ByteArrayOutputStream();
		ByteArrayOutputStream out2 = new ByteArrayOutputStream();

		WordReporter rep = new WordReporter();
		rep.addFilter(rep.new NoNumberFilter());
		rep.addFilter(rep.getStemmerByName("english"));
		
		
	    WordPrinter worker = WordReportFormatter.getPrinter(rep, 
				WordReportFormatter.OutputFormat.LDAC , 
				new File("/Users/will/Desktop/fold"), 
				Charset.forName("UTF8"), Locale.ENGLISH, 
				new File[]{new File("/Users/will/Desktop/jfreqing/d1.txt"),
			new File("/Users/will/Desktop/jfreqing/d2.txt")});
		
	    
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
		*/
		
		//rep.openStreamingReport(out, out1, out2);
		//rep.streamMTXReportLine(d1);
		//rep.streamMTXReportLine(d2);
		//rep.closeStreamingReport();
		
		//System.out.println(out.toString());
		//System.out.println(out1.toString());
		//System.out.println(out2.toString());
		
	}
	
}
