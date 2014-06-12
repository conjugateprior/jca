package org.conjugateprior.ca.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeSelectionModel;

import org.conjugateprior.ca.OldCategoryDictionary;
import org.conjugateprior.ca.OldCategoryDictionary.DictionaryCategory;
import org.conjugateprior.ca.OldCategoryDictionary.DictionaryPattern;

public class CategoryDictionaryPanel extends JPanel {
	
	protected OldCategoryDictionary model;
	protected JTree tree;
	protected JList list;
	protected DefaultListModel listModel;
		
	protected boolean showPatternsInSubtree = false; 
	
    public class MyCellRenderer extends DefaultTreeCellRenderer{
		
		private Icon categoryOpenIcon;
	    private Icon categoryClosedIcon;
		
	    private String openPath = "icons/TreeCategoryOpenIcon.png";
		private String closedPath = "icons/TreeCategoryClosedIcon.png";
	    
		public MyCellRenderer() {
	        super();
	       
	        try {
	            ClassLoader cl = 
	                MyCellRenderer.class.getClassLoader();
	            categoryOpenIcon = 
	                new ImageIcon(cl.getResource(openPath));
	            categoryClosedIcon = 
	                new ImageIcon(cl.getResource(closedPath));
	        } catch (NullPointerException npe){
	            System.err.println("Could not find icons in resource bundle");
	            try {
	                categoryOpenIcon = 
	                	new ImageIcon("resources/" + openPath);
	                categoryClosedIcon = 
	                	new ImageIcon("resources/" + closedPath);
	            } catch (Exception ioe){
	                System.err.println("Or in the filesystem"); 
	            }
	        }
	        
	    }
	
		@Override
		public Component getTreeCellRendererComponent(JTree tr, Object value,
				boolean isSelected, boolean expanded, boolean leaf, int row,
				boolean hFocus) {
			
			JComponent c = (JComponent) super.getTreeCellRendererComponent(tr, value, 
					isSelected, expanded, leaf, row, hFocus);
			//System.err.println(value);
			//System.err.println(value.getClass().getName());
			DictionaryCategory node = (DictionaryCategory) value; 
			Color col = node.getColor();
			c.setForeground(col); 				
			
			if (expanded)
				setIcon(categoryOpenIcon);
			else
				setIcon(categoryClosedIcon);
			//setToolTipText("(" + node.getPatterns().size() + ")");
	        //setText(n.getName());
	        			
			return c; 
		}
	}
	
	public CategoryDictionaryPanel(OldCategoryDictionary cdict) {
		super(new BorderLayout());
				
		model = cdict;
		listModel = new DefaultListModel();
		list = new JList(listModel);
		
		tree = new JTree(model);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.addTreeSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				updatePatternList();
			}
		});
        tree.setCellRenderer(new MyCellRenderer());
        tree.setSelectionRow(0); // trigger a refill
        tree.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
               
		JSplitPane pane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		JScrollPane catView = new JScrollPane(tree);
		catView.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
		JScrollPane patView = new JScrollPane(list);
		patView.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
		pane.setLeftComponent(catView);
		pane.setRightComponent(patView);
		
		Dimension minimumSize = new Dimension(100, 100);
        catView.setMinimumSize(minimumSize);
        patView.setMinimumSize(minimumSize);
        pane.setDividerLocation(250); 
        pane.setPreferredSize(new Dimension(500, 500));
        pane.setBorder(BorderFactory.createEmptyBorder());
		
		add(pane, BorderLayout.CENTER);
		
		final JCheckBox box = new JCheckBox("Show patterns hierarchically");
		box.setSelected(showPatternsInSubtree);
		box.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setShowPatternsInSubtree(box.isSelected());
				updatePatternList();
			}
		});
		
		add(box, BorderLayout.SOUTH);
		//setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
	}

	protected void updatePatternList(){
		DictionaryCategory node = (DictionaryCategory)
				tree.getLastSelectedPathComponent();
		if (node == null)
			return;
		List<DictionaryPattern> leaves = null;
		if (showPatternsInSubtree)
			leaves = model.getSortedPatternsInSubtree(node);
		else
			leaves = model.getSortedPatterns(node);
		
		listModel.clear();
		for (DictionaryPattern pat : leaves) 
			listModel.addElement(pat);
		//refillList(leaves);
	}

	public void setShowPatternsInSubtree(boolean show) {
		showPatternsInSubtree = show;
	}
	
	public boolean getShowPatternsInSubtree(){
		return showPatternsInSubtree;
	}
	
	public static void main(String[] args) throws Exception {
		
        //Create and set up the window.
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		OldCategoryDictionary mod = new OldCategoryDictionary();
		DictionaryCategory n = mod.getCategoryRoot();
		mod.addPatternToCategory("pattern 1", n);
		mod.addPatternToCategory("pattern 2", n);
		
		DictionaryCategory c = mod.addCategoryToParentCategory("Part 1", Color.BLUE, n);
		mod.addPatternToCategory("pattern 3", c);
		mod.addPatternToCategory("pattern 4", c);
		
		mod.addCategoryToParentCategory("Part 2", c);
		
		CategoryDictionaryPanel dictFront = new CategoryDictionaryPanel(mod);
		
        //Add content to the window.
        frame.getContentPane().add(dictFront, BorderLayout.CENTER);
         
        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }
	
}

/*
public class YoshikoderCategoryDictionaryPanel extends CategoryDictionaryPanel {
	
	public class MyCellRenderer extends DefaultTreeCellRenderer{
		
		private Icon categoryOpenIcon;
	    private Icon categoryClosedIcon;
		
	    private String openPath = "icons/TreeCategoryOpenIcon.png";
		private String closedPath = "icons/TreeCategoryClosedIcon.png";
	    
		public MyCellRenderer() {
	        super();
	       
	        try {
	            ClassLoader cl = 
	                MyCellRenderer.class.getClassLoader();
	            categoryOpenIcon = 
	                new ImageIcon(cl.getResource(openPath));
	            categoryClosedIcon = 
	                new ImageIcon(cl.getResource(closedPath));
	        } catch (NullPointerException npe){
	            System.err.println("Could not find icons in resource bundle");
	            try {
	                categoryOpenIcon = 
	                	new ImageIcon("resources/" + openPath);
	                categoryClosedIcon = 
	                	new ImageIcon("resources/" + closedPath);
	            } catch (Exception ioe){
	                System.err.println("Or in the filesystem"); 
	            }
	        }
	        
	    }
	
		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value,
				boolean isSelected, boolean expanded, boolean leaf, int row,
				boolean hasFocus) {
			
			JComponent c = (JComponent) super.getTreeCellRendererComponent(tree, value, 
					isSelected, expanded, leaf, row, hasFocus);
			//System.err.println(value);
			//System.err.println(value.getClass().getName());
			YoshikoderDictionaryCategory node = (YoshikoderDictionaryCategory) value; 
			Color col = node.getColor();
			c.setForeground(col); 				
			
			if (expanded)
				setIcon(categoryOpenIcon);
			else
				setIcon(categoryClosedIcon);
			//setToolTipText("(" + node.getPatterns().size() + ")");
	        //setText(n.getName());
	        			
			return c; 
		}
	}
	
	public YoshikoderCategoryDictionaryPanel(YoshikoderCategoryDictionary cdict) {
		super(cdict);
		tree.setCellRenderer(new MyCellRenderer());
	}

	
	public static void main(String[] args) {
		

	}

}
*/
