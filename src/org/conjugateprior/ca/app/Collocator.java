package org.conjugateprior.ca.app;

import java.util.HashMap;
import java.util.Map;

public class Collocator extends AbstractCounter {

	protected Map<String,Integer> wordFreq = 
			new HashMap<String,Integer>();
	
	protected Map<String[],Integer> nGramToScore = 
			new HashMap<String[],Integer>();
	
	public Collocator() {
		super();
	}
	
	// sentence breaks first, then accurate statistics 
	// of surprise and coincidence
	
	@Override
	public void processFiles() throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	public static void main(String[] args) {
		// gg
	}
}
