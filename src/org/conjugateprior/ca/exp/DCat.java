package org.conjugateprior.ca.exp;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javafx.scene.control.TreeItem;
import javafx.scene.paint.Color;

public class DCat implements Comparable<DCat> {
	private String name;
	private Set<DPat> patterns = new HashSet<DPat>();
	private Color color = null;
	private Set<Integer> matchedIndices = new HashSet<Integer>();

	public DCat(String nm, Color col) {
		name = nm;
		color = col;
	}
	
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
	
	public Color getColor(){
		return color;
	}
	
	public void setColor(Color newcol){
		color = newcol;
	}
	
	public Set<DPat> getPatterns(){
		return patterns;
	}
	
	public Set<Integer> getMatchedIndices() {
		return matchedIndices;
	}
	
	public void setMatchedIndices(Set<Integer> matchedInds){
		matchedIndices = matchedInds;
	}
	
	public static String getPathAsString(TreeItem<DCat> node, String sep){
		TreeItem<DCat> me = node;
		List<String> arr = new ArrayList<String>();
		arr.add(me.getValue().getName());
		TreeItem<DCat> parent = me.getParent();
		while (parent != null){
			me = parent;
			arr.add(sep);
			arr.add(me.getValue().getName());
			parent = me.getParent();
		}
        StringBuffer sb = new StringBuffer();
        for (int ii = arr.size()-1; ii >= 0; ii--)
			sb.append(arr.get(ii));
		return sb.toString();
    }

	public static void main(String[] args) {
		DCat dc = new DCat("fred", null);
		TreeItem<DCat> node = new TreeItem<DCat>(dc);
		DCat dc2 = new DCat("bloggs", null);
		TreeItem<DCat> node2 = new TreeItem<DCat>(dc2);
		node.getChildren().add(node2);
		System.err.println( DCat.getPathAsString(node2, ">") );
	}
	
}
