package org.conjugateprior.ca;

import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.logging.Logger;

public class FileBasedYoshikoderDocument extends SimpleYoshikoderDocument {

	private static Logger log = Logger.getLogger(FileBasedYoshikoderDocument.class.getName());
	
	File file;
	Charset charset;
	SoftReference<String> textReference;
	
	public FileBasedYoshikoderDocument(String docTitle, String txt, Date d,
			DocumentTokenizer tok, File f, Charset cs) throws Exception {
		super(docTitle, txt, d, tok);
		file = f;
		charset = cs;
	}
	
	@Override
	protected String loadText() throws IOException {
		String txt = textReference.get();
		if (txt == null){
			log.info("Empty soft reference, reloading from file...");
			txt = AbstractYoshikoderDocument.getTextFromFile(file, charset);
			textReference = new SoftReference<String>(txt);
			log.info("and back in place");
		}
		return txt;
	}

}
