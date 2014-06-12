package org.conjugateprior.ca.exp;


import java.util.List;
import java.util.Optional;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.util.Callback;

import org.conjugateprior.ca.DCat;
import org.conjugateprior.ca.DPat;
import org.conjugateprior.ca.FXCategoryDictionary;
import org.controlsfx.control.ButtonBar;
import org.controlsfx.control.ButtonBar.ButtonType;
import org.controlsfx.control.action.AbstractAction;
import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialog;
import org.controlsfx.dialog.DialogStyle;
import org.controlsfx.dialog.Dialogs;


public class FXCatDictPanel {

	protected String duplicateErrorMessageTitle = "Problem";
	
	protected FXCategoryDictionary model;
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
						dc.setName(newname);
						commitEdit(dc);
						//rearrangeTree();
					} else if (event.getCode() == KeyCode.ESCAPE)
						cancelEdit();
				}
			});
		}
		
		private String getString(){
			return getItem() == null ? "" : getItem().toString(); 
		}
	}
	
	public TreeItem<DCat> addCategory(String name, Color color, TreeItem<DCat> par) throws Exception {
		DCat dc = new DCat(name, color);
		TreeItem<DCat> item = new TreeItem<DCat>(dc);			
		try {
			model.addCategoryToParentCategory(item, par);
		} catch (Exception ex){
			duplicateYelp(name, par.getValue().getName());
		}
		tree.getSelectionModel().select(item);
		tree.requestFocus();
		return item;
	}

	public TreeItem<DCat> addPatternToCategory(String val, TreeItem<DCat> par) throws Exception {
		DCat dc = par.getValue();
		DPat dp = new DPat(val);
		dc.getPatterns().add(dp);
		updatePatternList();
		list.getSelectionModel().select(dp);
		tree.requestFocus();
		return par;
	}

	
	protected void duplicateYelp(String duplicatedName, String parentName){
		String message = "There is already a subcategory of " + parentName +
				" called \"" + duplicatedName + "\"";
		Dialogs.create().owner(borderPane).message(message)
			.title(duplicateErrorMessageTitle).masthead(null)
			.showInformation();
	}
	
	public DCat removePattern(DCat cat, DPat item) {
		cat.getPatterns().remove(item);
		return cat;
	}
	
	public TreeItem<DCat> removeCategory(TreeItem<DCat> item) throws Exception {
		if (item.equals(model.getCategoryRoot()))
			throw new Exception("Can't delete the dictionary root");
		else {
			TreeItem<DCat> nextSelection = null;
			TreeItem<DCat> after = item.nextSibling();
			if (after != null){
				nextSelection = after;
			} else {
				TreeItem<DCat> before = item.previousSibling();
				if (before != null)
					nextSelection = before;
				else {
					nextSelection = item.getParent();
				}
			}
			model.removeCategory(item);
			tree.getSelectionModel().select(nextSelection);
			tree.requestFocus();
		}
		return item;
	}
	
	public void rearrangeTree(){
		TreeItem<DCat> obj = tree.getSelectionModel().getSelectedItem();
		TreeItem<DCat> par = obj.getParent();
		model.removeCategory(obj);
		try {
			model.addCategoryToParentCategory(obj, par);
			tree.getSelectionModel().select(obj);
		} catch (Exception e){
			System.err.println("oops");
		}
	}
	
	public FXCatDictPanel(FXCategoryDictionary cdict) {
				
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
		//tree.setEditable(true);
		/*
		tree.setOnEditCommit(new EventHandler<TreeView.EditEvent<DCat>>() {
			@Override
			public void handle(EditEvent<DCat> event) {
				DCat dc = event.getOldValue();
				DCat dcnew = event.getNewValue();
				System.out.println(dc.name + " -> " + dcnew.name);
			}
		});
		*/
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

        // custom dialog for new category
        
        Button add = new Button("+");
        add.setOnAction(new EventHandler<ActionEvent>() {
        	@Override
        	public void handle(ActionEvent event) {
        		TreeItem<DCat> sel = tree.getSelectionModel().getSelectedItem();
        		
        		showNewCategoryDialog(sel.getValue());
        		/*
        		String s = Dialogs.create().owner(borderPane).message("Name")
        				.title("New Category").masthead(null).nativeTitleBar().showTextInput();
        		if (s != null){
        			String val = s.trim();
        			if (val.length()>0){
        				try {
        					addCategory(val, null, sel);
        				} catch (Exception ex){
        					duplicateYelp(val, sel.getValue().name);
        				}
        			}
        		}
        		*/
        	}
        });
        Button addPattern = new Button("P");
        addPattern.setOnAction(new EventHandler<ActionEvent>() {
        	@Override
        	public void handle(ActionEvent event) {
        		TreeItem<DCat> sel = tree.getSelectionModel().getSelectedItem();
        		Optional<String> s = Dialogs.create().owner(borderPane).message("Name")
        				.title("New Pattern").masthead(null).showTextInput();
        		if (s.isPresent()){
        			String val = s.get().trim();
        			if (val.length()>0){
        				try {
        					addPatternToCategory(val, sel);
        				} catch (Exception ex){
        					//duplicateYelp(val, sel.getValue().name);
        				}
        			}
        		}
        	}
        });
        
        Button remove = new Button("-");
        remove.setOnAction(new EventHandler<ActionEvent>() {
        	@Override
        	public void handle(ActionEvent event) {
        		boolean aboutPattern = list.isFocused();
        		if (aboutPattern){
        			int jj = list.getSelectionModel().getSelectedIndex();
        			DPat pat = list.getSelectionModel().getSelectedItem();
        			DCat cat = tree.getSelectionModel().getSelectedItem().getValue();
        			removePattern(cat, pat);
        			
        			
        		} else {
        			TreeItem<DCat> sel = tree.getSelectionModel().getSelectedItem();
        			try {
        				removeCategory(sel);
        			} catch (Exception ex){
        				tree.requestFocus();
        			}
        		}
        	}
        });
        
        HBox buttonBox = new HBox(5);
        buttonBox.getChildren().addAll(add, remove, addPattern);
        
        borderPane = new BorderPane();
        borderPane.setCenter(sp);
        borderPane.setBottom(box);
        borderPane.setRight(buttonBox);
	
        // TODO lose the in place editing and just go back to business
        // as usual
        
        tree.getSelectionModel().select(0);
		updatePatternList();
	}

	private TextField diaCategoryName = new TextField();
    private ColorPicker diaColorPicker = new ColorPicker();
	private Dialog newCategoryDialog = null;
	private DCat editedDcat = null;
	
	private DCat showNewCategoryDialog(DCat dcat) {			
		if (newCategoryDialog == null){
			newCategoryDialog = new Dialog(null, "New Category", false, DialogStyle.NATIVE);
			final Action actionLogin = new AbstractAction("Create") {
				{  ButtonBar.setType(this, ButtonType.OK_DONE); }
				@Override
				public void handle(ActionEvent ae) {
					Dialog dlg = (Dialog) ae.getSource();
					String newcat = diaCategoryName.getText();
					Color c = diaColorPicker.getValue();
					editedDcat = new DCat(newcat, c); // override the the thing
					dlg.hide();	
				}
			};
			ChangeListener<String> changeListener = new ChangeListener<String>() {
				public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
					actionLogin.disabledProperty().set(
						diaCategoryName.getText().trim().isEmpty());
				}
			};
			diaCategoryName.textProperty().addListener(changeListener);
			// validate
			actionLogin.disabledProperty().set(
					diaCategoryName.getText().trim().isEmpty());
			
			final GridPane content = new GridPane();
			content.setHgap(10);
			content.setVgap(10);

			content.add(new Label("User name"), 0, 0);
			content.add(diaCategoryName, 1, 0);
			GridPane.setHgrow(diaCategoryName, Priority.ALWAYS);
			content.add(new Label("Colour"), 0, 1);
			content.add(diaColorPicker, 1, 1);
			//GridPane.setHgrow(txPassword, Priority.ALWAYS);

			newCategoryDialog.setResizable(false);
			newCategoryDialog.setIconifiable(false);
			//dlg.setGraphic(new ImageView(HelloDialog.class.getResource("login.png").toString()));
			newCategoryDialog.setContent(content);
			newCategoryDialog.getActions().addAll(actionLogin, Dialog.Actions.CANCEL);
		} 
		
		if (dcat != null){
			//editedDcat;
			//newdc.color = dcat.color;		
			diaCategoryName.setText(dcat.getName());
			diaColorPicker.setValue(dcat.getColor());
		} else {
			diaCategoryName.setText("");
			diaColorPicker.setValue(null);
		}
		Platform.runLater(new Runnable() {
			public void run() {
				diaCategoryName.requestFocus();
			}
		});
		newCategoryDialog.show();
		
		return null; // TODO FIXME THIS IS BUST
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

