package org.conjugateprior.ca.exp;

import java.util.List;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.TreeView.EditEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.util.Callback;

public class FXCatDictPanel {

	protected FXCatDict model;
	protected TreeView <DCat> tree;
	protected ListView<DPat> list;
	protected CheckBox box;	
	
	protected BorderPane borderPane;
	
	protected boolean showPatternsInSubtree = false; 
	
	private final class TextFieldTreeCellImpl extends TreeCell<DCat> {
		private TextField textField;
		
		public TextFieldTreeCellImpl() {}
		
		@Override
		public void startEdit() {
			super.startEdit();
			if (textField == null)
				createNewTextField();
			setText(null);
			setGraphic(textField);
			textField.selectAll();
		}
		
		@Override
		public void cancelEdit() {
			super.cancelEdit();
			setText(getItem().toString());
			setGraphic(getTreeItem().getGraphic());
		}
		
		@Override
		protected void updateItem(DCat item, boolean empty) {
			super.updateItem(item, empty);
		
			if (empty){
				setText(null);
				setGraphic(null);
			} else {
				if (isEditing()){
					if (textField != null)
						textField.setText(getString());
					setText(null);
					setGraphic(textField);
				} else {
					setText(getString());
					setGraphic(getTreeItem().getGraphic());
				}
			}
		}
		
		private void createNewTextField(){
			textField = new TextField(getString()); 
			textField.setOnKeyReleased(new EventHandler<KeyEvent>() {
				@Override
				public void handle(KeyEvent event) {
					if (event.getCode() == KeyCode.ENTER){
						String newname = textField.getText();
						DCat dc = getItem();
						dc.name = newname;
						commitEdit(dc);
					} else if (event.getCode() == KeyCode.ESCAPE)
						cancelEdit();
				}
			});
		}
		
		private String getString(){
			return getItem() == null ? "" : getItem().toString(); 
		}
		
	}
	
	public FXCatDictPanel(FXCatDict cdict) {
				
		model = cdict;
		list = new ListView<DPat>();
		
		tree = new TreeView<DCat>();
		tree.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		tree.setRoot(cdict.getCategoryRoot());  
		tree.getSelectionModel().selectedItemProperty().addListener(
				new ChangeListener<TreeItem<DCat>>() {
			@Override
			public void changed(ObservableValue<? extends TreeItem<DCat>> observable,
				TreeItem<DCat> oldValue, TreeItem<DCat> newValue) {
				updatePatternList(); // checks for a non-null selection
			}
		});
		tree.setEditable(true);
		tree.setOnEditCommit(new EventHandler<TreeView.EditEvent<DCat>>() {
			@Override
			public void handle(EditEvent<DCat> event) {
				DCat dc = event.getOldValue();
				DCat dcnew = event.getNewValue();
				System.out.println(dc.name + " -> " + dcnew.name);
			}
		});
		tree.setCellFactory(new Callback<TreeView<DCat>, TreeCell<DCat>>() {
			@Override
			public TreeCell<DCat> call(TreeView<DCat> param) {
				return new TextFieldTreeCellImpl();
			}
		});
	
        SplitPane sp = new SplitPane();
        sp.getItems().addAll(tree, list);
        box = new CheckBox("Show patterns hierarchically");
        box.setPadding(new Insets(5));
        box.setOnAction(new EventHandler<ActionEvent>() {
        	@Override
        	public void handle(ActionEvent event) {
        		setShowPatternsInSubtree(box.isSelected());
				updatePatternList();
        	}
        });
        box.setSelected(false);
		
        borderPane = new BorderPane();
        borderPane.setCenter(sp);
        borderPane.setBottom(box);
	
        tree.getSelectionModel().select(0);
		updatePatternList();
	}

	public BorderPane getBorderPane() {
		return borderPane;
	}
	
	protected void updatePatternList(){
		TreeItem<DCat> node = tree.getSelectionModel().getSelectedItem();
		if (node == null) return;
		
		List<DPat> pats = null;
		if (showPatternsInSubtree)
			pats = model.getSortedPatternsInSubtree(node);
		else
			pats = model.getSortedPatterns(node);
		list.getItems().clear();
		list.getItems().addAll(pats);
	}

	public void setShowPatternsInSubtree(boolean show) {
		showPatternsInSubtree = show;
	}
	
	public boolean getShowPatternsInSubtree(){
		return showPatternsInSubtree;
	}
	
	// checks for duplicate names among siblings
	/*
	public void setName(String n) throws Exception {
		String myname = getValue().name;
		TreeItem<DCat> par = getParent();
		ObservableList<TreeItem<DCat>> siblings = par.getChildren();
		for (TreeItem<DCat> treeItem : siblings) {
			if ((treeItem != this) && 
					(myname.equals(treeItem.getValue().name)))
				throw new Exception(duplicateMessage);
		}
		getValue().name = n; // assign
	}
	
	public boolean equals(Object obj) {
		if (obj instanceof DCat)
			return getValue().name.equals(((DCat)obj).name);
		return false;
	}

	public String toString() {
		return getValue().name;
	}

	public void setMatchedIndices(Set<Integer> s){
		getValue().matchedIndices = s;
	}

	public void addMatchedIndices(Set<Integer> s){
		getValue().matchedIndices.addAll(s);
	}

	// constructed on the fly from wherever we are
	public String getPathAsString(String sep){
		DCat dcat = getValue();
		TreeItem<DCat> item = this;
		List<String> nm = new ArrayList<String>();
		nm.add(dcat.name);
		TreeItem<DCat> par = null;
		while((par = item.getParent()) != null){
			nm.add(sep);
			nm.add(par.getValue().name);
		}
		Collections.reverse(nm);
		StringBuilder builder = new StringBuilder(nm.size());
		for (String string : nm) 
			builder.append(string);
		return builder.toString();
	}

	@Override
	public int compareTo(FXDCat<DCat> o) {
		return getValue().name.compareTo(o.getValue().name);
	}
	*/	
	
	public static void main(String[] args) throws Exception {

    }
	
}

