package org.conjugateprior.ca;

import java.io.IOException;
import java.util.Date;
import java.util.Locale;

public interface IYoshikoderDocument extends ITokenization, Comparable<IYoshikoderDocument> {
	
	public String getTitle();

	public void setTitle(String title);

	public String getText() throws IOException;	

	public Date getDate();
	
	public void setDate(Date d);
	
	public Locale getLocale();
	
	public void setLocale(Locale locale, IDocumentTokenizer tokenizerForLocale) throws Exception;
	
}
