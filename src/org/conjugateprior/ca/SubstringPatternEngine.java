package org.conjugateprior.ca;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class SubstringPatternEngine implements IPatternEngine {

    protected int reFlags = Pattern.CASE_INSENSITIVE;
          
    /**
     * Converts a candidate string into regular expression that looks for
     * case-insensitive word-internal exact matches, or substring matches
     * if * is applied in the pattern.
     */
    public Pattern[] makeRegexp(String pstring) throws PatternSyntaxException{
        String[] spl = pstring.split("[ ]+"); // TODO check this is robust 
        Pattern[] pats = new Pattern[spl.length];
        for (int ii = 0; ii < pats.length; ii++){
        	String escaped = escape(spl[ii]);
            pats[ii] = Pattern.compile(escaped, reFlags);			
        }
		return pats;
    }
    
    public Pattern[] makeRegexp(String[] spl) throws PatternSyntaxException{
        Pattern[] pats = new Pattern[spl.length];
        for (int ii = 0; ii < pats.length; ii++){
        	String escaped = escape(spl[ii]);
            pats[ii] = Pattern.compile(escaped, reFlags);			
        }
		return pats;
    }
    
    private String escape(String pstring){
        StringBuffer sb = new StringBuffer();
        char[] pchar = pstring.toCharArray();
        for (int ii=0; ii<pchar.length; ii++){
            if (pchar[ii] == '*'){
                if (ii == 0)
                    sb.append("\\S*\\Q");
                else if (ii == pchar.length-1)
                    sb.append("\\E\\S*"); 
                else 
                    sb.append("\\E\\S*\\Q"); // end quot \S* start quot
            } else {
                if (ii == 0)
                    sb.append("\\b\\Q");   // add prefix if at start 
                sb.append(pchar[ii]);      // add letter
                if (ii == pchar.length-1)  // add suffix if at end
                    sb.append("\\E\\b"); 
            }
        }
        return sb.toString();
    }
    
    public static void main(String[] args) {
        SubstringPatternEngine engine = new SubstringPatternEngine();
        Pattern[] p = engine.makeRegexp("*f*k*");
        System.out.println(p[0].pattern());
        Matcher m = p[0].matcher("fgk"); 
        System.out.println(m.matches());
    }
  
}
