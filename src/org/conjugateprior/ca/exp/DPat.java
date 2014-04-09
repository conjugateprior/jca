package org.conjugateprior.ca.exp;

import java.util.regex.Pattern;

public class DPat implements Comparable<DPat>{

	private String[] elements;
	private String name;
	private Pattern[] regexps;

	public DPat(String s){
		elements = s.split("[ ]+");
		regexps = FXCatDict.patternEngine.makeRegexp(elements);
		fixName();
	}

	public String[] getElements() {
		return elements;
	}

	public Pattern[] getRegexps() {
		return regexps;
	}

	protected void fixName(){
		if (elements.length > 1){
			StringBuffer sb = new StringBuffer();
			for (String s: elements) {
				sb.append(s);
				sb.append(" ");
			}
			name = sb.subSequence(0, sb.length()-1).toString();
		} else {
			name = elements[0];
		}			
	}

	public String getName() {
		return name;
	}

	public String toString() {
		return name;
	}

	public int compareTo(DPat o) {
		return this.toString().compareTo(o.toString());
	}


}
