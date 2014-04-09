package org.conjugateprior.ca.exp;

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;

public class DCat implements Comparable<DCat> {
	public String name;
	public Set<DPat> patterns = new HashSet<DPat>();
	public Color color = null;
	public Set<Integer> matchedIndices = new HashSet<Integer>();

	@Override
	public int compareTo(DCat o) {
		return name.compareTo(o.name);
	}
	
	@Override
	public String toString() {
		return name;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
}
