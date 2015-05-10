package org.conjugateprior.ca;

import java.io.Serializable;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * @author will
 */
public interface PatternEngine extends Serializable{
	
    /**
     * Compile an appropriate regular expression from a string.
     * @param pstring
     * @return regular expression
     */
    public Pattern[] makeRegexp(String pstring) throws PatternSyntaxException;
    
    public Pattern[] makeRegexp(String[] pstring) throws PatternSyntaxException;
    
}
