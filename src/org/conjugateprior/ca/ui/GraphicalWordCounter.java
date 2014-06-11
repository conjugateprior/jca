package org.conjugateprior.ca.ui;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import org.conjugateprior.ca.exp.FXCatDict;
import org.conjugateprior.ca.reports.CSVFXCatDictCategoryCountPrinter;
import org.conjugateprior.ca.reports.CountPrinter;
import org.conjugateprior.ca.reports.CountPrinter.CountingTask;
import org.conjugateprior.ca.reports.LDACWordCountPrinter;
import org.conjugateprior.ca.reports.MTXWordCountPrinter;
import org.conjugateprior.ca.reports.WordCounter;
import org.controlsfx.dialog.DialogStyle;
import org.controlsfx.dialog.Dialogs;

public class GraphicalWordCounter extends Application {

	public static class Wrapper implements Comparable<Wrapper> {
		String name; // for sorting
		public Wrapper(String n) {
			name = n;
		}
		@Override
		public String toString() {
			return getDisplayName();
		}
		@Override
		public int compareTo(Wrapper o) {
			return name.compareTo(o.name);
		}
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Wrapper)
				return ((Wrapper)obj).name.equals(name);
			return super.equals(obj);
		}
		public String getDisplayName(){
			return name;
		}
	}
	
	static private class LocaleWrap extends Wrapper {
		Locale locale;
		public LocaleWrap(Locale loc) {
			super(loc.toString());
			locale = loc;
		}
		public String getDisplayName(){
			String country = locale.getDisplayCountry();
			return locale.getDisplayLanguage() + 
			 (country.length() > 0 ? ": " + country : "") +
			  " (" + locale.toString() + ")";
		}	
		static List<LocaleWrap> getAllLocaleWraps(){
			Locale[] locs = Locale.getAvailableLocales();
			List<LocaleWrap> locWrapList = new ArrayList<LocaleWrap>(locs.length);
			for (Locale locale : locs) 
				if (locale.getLanguage().length() != 0) // weird locale first
					locWrapList.add(new LocaleWrap(locale));
			Collections.sort(locWrapList);
			return locWrapList;
		}		
	}
	
	static class CharsetWrap extends Wrapper {
		Charset charset;		
		public CharsetWrap(Charset cs) {
			super(cs.name());			
			charset = cs;
		}
		static List<CharsetWrap> getAllCharsetWraps(){
			SortedMap<String, Charset> smap = GraphicalWordCounter.getCharsetMap();
			Set<String> names = smap.keySet();
			List<String> sortedNames = new ArrayList<String>(names);
			Collections.sort(sortedNames);
			List<CharsetWrap> charsetWrapList = new ArrayList<CharsetWrap>(sortedNames.size());
			for (String cs : sortedNames) {
				Charset ch = smap.get(cs);
				if (ch.displayName() != null)
					charsetWrapList.add(new CharsetWrap(ch));
			}
			return charsetWrapList;
		}
	}
	
	protected String[] stemLangs = new String[]{
		"Danish", "Dutch", "English", "Finnish", "German", "Hungarian", "Italian",
		"Norwegian", "Portuguese", "Romanian", "Russian", 
		"Spanish", "Swedish", "Turkish"};
	
	protected CheckBox cbLowercase = new CheckBox();
	protected CheckBox cbNoNumbers = new CheckBox();
	protected CheckBox cbNoCurrency = new CheckBox();
	protected ChoiceBox<LocaleWrap> listLocale;
	protected ChoiceBox<CharsetWrap> listCharset;
	protected ChoiceBox<String> listStemming;
	protected CheckBox cbStem = new CheckBox();
	protected FileChooser dirChooser = new FileChooser();
	protected File directory;
	protected FileChooser stopwordFileChooser = new FileChooser();
	protected File stopsFile;
	protected CheckBox cbStop = new CheckBox();
	protected Button stopsBtn;
	
	protected FXCatDict dictionary;
	
	// dict stuff 
	protected FileChooser dictFileChooser = new FileChooser();
	protected File dictFile;
	protected CheckBox cbDict = new CheckBox();
	protected Button dictBtn;
	
	protected CheckBox cbGzip = new CheckBox();
	// output format
	protected ChoiceBox<String> outputFormat = new ChoiceBox<String>();
	
	protected TextField stopsDesc = new TextField();
	protected TextField dictDesc = new TextField();
	
	protected Text labStops, labStemming, labNoNumbers, labNoCurrency;
	// go
	protected Button goButton = new Button("PROCESS");
	
	protected ListView<File> list;
	
	@Override
	public void start(final Stage primaryStage) throws Exception {
		primaryStage.setTitle("JFreq");
		
		GridPane grid = new GridPane(); 
		grid.setAlignment(Pos.TOP_CENTER); 
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(10));

		/*
		ColumnConstraints col1 = new ColumnConstraints();
		//col1.setPercentWidth(25);
		ColumnConstraints col2 = new ColumnConstraints();
		col2.setFillWidth(true);
		ColumnConstraints col3 = new ColumnConstraints();
		col3.setFillWidth(true);
		//col3.setPercentWidth(25);
		ColumnConstraints col4 = new ColumnConstraints();
		grid.getColumnConstraints().addAll(col1, col2, col3, col4);	
		*/
		
		list = new ListView<File>();
		list.setOnDragOver(new EventHandler <DragEvent>() {
            public void handle(DragEvent event) {
                if (event.getDragboard().hasFiles())
                	event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                event.consume();
            }
        });
		list.setOnDragDropped(new EventHandler <DragEvent>() {
            public void handle(DragEvent event) {
                Dragboard db = event.getDragboard();
                boolean success = false;
                if (db.hasFiles()) {
                    for (File file : db.getFiles()) {
						if (file.isDirectory()){
							for (File subfile : file.listFiles()) {
								if (!subfile.isDirectory()){
									if (!list.getItems().contains(subfile))
										list.getItems().add(subfile);
								}
							}
						} else {
							if (!list.getItems().contains(file))
								list.getItems().add(file);
						}
                    }
                    success = true;
                }
                event.setDropCompleted(success);       
                event.consume();
            }
        });
		list.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		list.setOnKeyPressed(new EventHandler <KeyEvent>(){
			@Override
			public void handle(KeyEvent event) {
				if (event.getCode().equals(KeyCode.BACK_SPACE) ||
					event.getCode().equals((KeyCode.DELETE))){
					// workaround for bug: http://javafx-jira.kenai.com/browse/RT-24367
					ObservableList<File> sels = 
			            FXCollections.observableArrayList( //copy
			            		list.getSelectionModel().getSelectedItems());
					if (sels != null) {
						//heroes.addAll(sels);
						list.getItems().removeAll(sels);
						list.getSelectionModel().clearSelection();
					}
				}
			}
		});
		
		// lowercase
		Text labLowercase = new Text("Lowercase:");
		labLowercase.setDisable(true);
				
		// no numbers
		cbLowercase.setSelected(true);
		cbLowercase.setDisable(true); 
		labNoNumbers = new Text("No numbers:");
				
		// no currency
		labNoCurrency = new Text("No currency:");

		// locale
		List<LocaleWrap> locs = LocaleWrap.getAllLocaleWraps();
		listLocale = new ChoiceBox<LocaleWrap>(FXCollections.observableArrayList(locs));
		LocaleWrap here = new LocaleWrap(Locale.getDefault());
		listLocale.getSelectionModel().select(here);
		listLocale.setMaxWidth(Double.MAX_VALUE);
				
		// resize to maximum element (not a default behaviour)
		double maxWidth = 0;
	    for (Node n: listLocale.lookupAll(".text"))
	    	maxWidth = Math.max(maxWidth, n.getBoundsInParent().getWidth() + 8);
	    	
	    listLocale.setPrefWidth(maxWidth);
		Text labLocale = new Text("Locale:");
		
		// charsets
		List<CharsetWrap> cs = CharsetWrap.getAllCharsetWraps();
		listCharset = new ChoiceBox<CharsetWrap>(FXCollections.observableArrayList(cs));
		Charset mine = Charset.defaultCharset();
		CharsetWrap mywrap = new CharsetWrap(mine);
		listCharset.getSelectionModel().select(mywrap);
		Text labCharset = new Text("Encoding:");

		// stem 
		listStemming = new ChoiceBox<String>();
		labStemming = new Text("Stem:");
		for (String lang : stemLangs)
			listStemming.getItems().add(lang);
		listStemming.getSelectionModel().select("English");
		cbStem.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				listStemming.setDisable(!cbStem.isSelected());
		    }
		});
		listStemming.setDisable(!cbStem.isSelected());
		
		// output folder
		final TextField dirDesc = new TextField();
		dirDesc.setEditable(false);
		Text labFolder = new Text("Output folder:");
		Button btn = new Button("Choose");
		btn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				File f = dirChooser.showSaveDialog(primaryStage);
				if (f != null){
					directory = f;
					dirDesc.setText(directory.getAbsolutePath());
					dirDesc.positionCaret(dirDesc.getText().length());
				}
			}
		});
			
		// output format
		Text labFormat = new Text("Output format:");		
		outputFormat.getItems().addAll("LDA-C", "Matrix Market");
		outputFormat.getSelectionModel().select(0);
			
		// stopwords
		stopsDesc.setEditable(false);
		labStops = new Text("Remove stopwords:");
		stopsBtn = new Button("Choose");
		stopsBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				File f = stopwordFileChooser.showOpenDialog(primaryStage);
				if (f != null){
					stopsFile = f;
					stopsDesc.setText(stopsFile.getAbsolutePath());
					stopsDesc.positionCaret(stopsDesc.getText().length());
				}
			}
		});
		cbStop.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				boolean sel = !cbStop.isSelected();
				stopsBtn.setDisable(sel);
				stopsDesc.setDisable(sel);
		    }
		});
		boolean sel = !cbStop.isSelected();
		stopsBtn.setDisable(sel);
		stopsDesc.setDisable(sel);
		
		// dictionary
		Text labDict = new Text("Apply dictionary:");
		
		dictBtn = new Button("Choose");
		dictBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				File f = dictFileChooser.showOpenDialog(primaryStage);
				if (f != null){
					try {
						dictionary = getCategoryDictionaryFromFile(f);
						dictDesc.setText(f.getAbsolutePath());
						dictDesc.positionCaret(dictDesc.getText().length());
					} catch (Exception ex){
						Dialogs.create().style(DialogStyle.NATIVE)
						.title("Could not read dictionary file")
						.message(ex.getMessage())
						.showException(ex);
					}
				}
			}
		});
		cbDict.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				boolean sel = !cbDict.isSelected();
				dictBtn.setDisable(sel);
				dictDesc.setDisable(sel);
			}
		});
		sel = !cbDict.isSelected();
		dictBtn.setDisable(sel);
		dictDesc.setDisable(sel);

		cbDict.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				boolean sel = !cbDict.isSelected();
				setWordsRatherThanCategories(sel);
			}
		});
		sel = !cbDict.isSelected();
		setWordsRatherThanCategories(sel);
		
		//////////////////////
		int row = 0;
		
		grid.add(labLocale, 0, row);
		listLocale.setMaxWidth(Double.MAX_VALUE);
		grid.add(listLocale, 1, row, 3, 1);
		row++;
		
		grid.add(labCharset, 0, row);
		listCharset.setMaxWidth(Double.MAX_VALUE);
		grid.add(listCharset, 1, row, 3, 1);
		row++;
		
		grid.add(new Separator(Orientation.HORIZONTAL), 0, row, 5, 1);
		row++;
		
		// dictionary
		grid.add(labDict, 0, row);
		grid.add(cbDict, 1, row);
		grid.add(dictDesc, 2, row);
		grid.add(dictBtn, 3, row);
		row++;
		
		grid.add(new Separator(Orientation.HORIZONTAL), 0, row, 5, 1);
		row++;
		
		grid.add(labLowercase, 0, row);
		grid.add(cbLowercase, 1, row);
		row++;
		
		grid.add(labNoNumbers, 0, row);
		grid.add(cbNoNumbers, 1, row);
		row++;
		
		grid.add(labNoCurrency, 0, row);
		grid.add(cbNoCurrency, 1, row);
		row++;
		
		grid.add(labStops, 0, row);
		grid.add(cbStop, 1, row);
		grid.add(stopsDesc, 2, row);
		grid.add(stopsBtn, 3, row);	
		row++;
		
		grid.add(labStemming, 0, row);
		grid.add(cbStem, 1, row);
		//listStemming.setMaxWidth(Double.MAX_VALUE);
		grid.add(listStemming, 2, row, 3, 1);
		row++;
		
		grid.add(labFormat, 0, row);
		outputFormat.setMaxWidth(150);
		grid.add(outputFormat, 1, row, 3, 1);
		row++;
			
		grid.add(new Separator(Orientation.HORIZONTAL), 0, row, 5, 1);
		row++;
		
		grid.add(labFolder, 0, row);
		grid.add(dirDesc, 1, row, 2, 1);
		grid.add(btn, 3, row);	
		row++;
		
		//grid.add(labGzip, 0, 9);
		//grid.add(cbGzip, 1, 9);	
		
		// make the splitpane
		SplitPane sp = new SplitPane();
		
		// left hand side
        
		Label lab1 = new Label("Processing");
        lab1.setFont(Font.font(null, FontWeight.BOLD, 20));
        lab1.setPadding(new Insets(20,10,20,10));
        lab1.setAlignment(Pos.CENTER);
                
        BorderPane propertiespane = new BorderPane();
        
        propertiespane.setTop(lab1);
        BorderPane.setAlignment(lab1, Pos.CENTER);
        
        propertiespane.setCenter(grid);
        BorderPane.setAlignment(grid, Pos.TOP_CENTER);

        // right hand side
        BorderPane listpane = new BorderPane();
        listpane.setCenter(list);
        Label lab = new Label("Documents");
        lab.setFont(Font.font(null, FontWeight.BOLD, 20));
        lab.setPadding(new Insets(20,10,20,10));
        listpane.setTop(lab);
        BorderPane.setAlignment(lab, Pos.CENTER);
		
        listpane.setBottom(goButton);
        BorderPane.setMargin(goButton, new Insets(10));
        BorderPane.setAlignment(goButton, Pos.CENTER);
        
        goButton.setOnAction(new EventHandler<ActionEvent>() {
        	@Override
        	public void handle(ActionEvent ae) {
        		processTheFiles();
        	}
        });
        
        // add them
        sp.getItems().addAll(propertiespane, listpane);
        
        
        final BorderPane rootGroup = new BorderPane();
        //rootGroup.setPadding(new Insets(0));
		//grid.setGridLinesVisible(true);
		
		MenuBar mbar = new MenuBar();
		final Menu menu1 = new Menu("File");
		final Menu menu2 = new Menu("Edit");
		final Menu menu3 = new Menu("Help");
		mbar.getMenus().addAll(menu1, menu2, menu3);
		//mbar.setUseSystemMenuBar(true);
		rootGroup.setTop(mbar); 
        rootGroup.setCenter(sp);
        Scene scene = new Scene(rootGroup, 1000, 550);
		
		/*
		progressBar.setMaxWidth(Double.MAX_VALUE);
		grid.add(progressBar, 0, 10, 3, 1);
		grid.add(goButton, 3, 10);
		*/
        
		primaryStage.setScene(scene);
		primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			public void handle(WindowEvent wev) {
				try {
					saveGUIStateToPreferences();
				} catch (BackingStoreException bse){
					System.err.println("Could not save preferences");
					bse.printStackTrace();
				}
			};
		});
		
		//AquaFx.style();
		
		configureGUIFromPreferences();
		
		primaryStage.show();
	}
	
	protected Label makeLabel(String s){
        Label tt = new Label(s);
        tt.setFont(Font.font(null, FontWeight.BOLD, 12));
        tt.setPadding(new Insets(10,0,0,0));
        return tt;
	}
	
	protected FXCatDict getCategoryDictionaryFromFile(File sf) throws Exception {
		if (!sf.exists()){
			throw new Exception("Dictionary file cannot be found at"
					+ sf.getAbsolutePath());
		}
		FXCatDict dict = null;
		String fname = sf.getName();
		if (fname.toLowerCase().endsWith(".ykd") || 
			fname.toLowerCase().endsWith(".lcd")){
			dict = FXCatDict.readXmlCategoryDictionaryFromFile(sf); 	
		
		} else if (fname.toLowerCase().endsWith(".vbpro")){
			dict = FXCatDict.importCategoryDictionaryFromFileVBPRO(sf); 
		
		} else if (fname.toLowerCase().endsWith(".dic")){
			dict = FXCatDict.importCategoryDictionaryFromFileLIWC(sf); 
		
		} else if (fname.toLowerCase().endsWith(".cat")){
			dict = FXCatDict.importCategoryDictionaryFromFileWordstat(sf);
		
		} else if (fname.toLowerCase().endsWith(".xml")) {
			// windows or server .xml addition?
			dict = FXCatDict.readXmlCategoryDictionaryFromFile(sf); 
				
		} else {
			throw new Exception(
					"Dictionary file format could not be read." +
				    "It must be a Yoshikoder ('.ykd'), Lexicoder ('.lcd'), " +
				    "Wordstat ('.CAT'), LIWC (.dic), or VBPro ('.vbpro') file");
		}
		return dict;
	}
	
	protected void setWordsRatherThanCategories(boolean onoff){
		if (onoff){
			// doesn't quite work as expected, ah well
			labStops.setDisable(false);
			labStemming.setDisable(false);
			labNoCurrency.setDisable(false);
			labNoNumbers.setDisable(false);
			
			cbNoCurrency.setDisable(false);
			cbNoNumbers.setDisable(false);
			
			cbStem.setDisable(false);
			if (cbStem.isSelected())
				listStemming.setDisable(false);
			
			cbStop.setDisable(false);
			if (cbStop.isSelected()){
			  stopsBtn.setDisable(false);
			  stopsDesc.setDisable(false);
			}
			// cbDict is off
			dictDesc.setDisable(true);
			dictBtn.setDisable(true);
			
			outputFormat.setDisable(false);
			
		} else {
			labStops.setDisable(true);
			labStemming.setDisable(true);
			labNoCurrency.setDisable(true);
			labNoNumbers.setDisable(true);
			
			cbNoCurrency.setDisable(true);
			cbNoNumbers.setDisable(true);
			
			cbStem.setDisable(true);
			listStemming.setDisable(true);
			
			cbStop.setDisable(true);
			stopsBtn.setDisable(true);
			stopsDesc.setDisable(true);
			
			// cbDict is off
			dictDesc.setDisable(false);
			dictBtn.setDisable(false);

			outputFormat.setDisable(true);

		}
	}
	
	protected void processTheFiles() {
		/*
		if (directory.exists()){
			Dialogs.create().style(DialogStyle.NATIVE).title("Output folder already exists")
			.message("Please choose another output folder. This one already exists").showError();
			return;
		}
		*/
		if (directory == null){
			Dialogs.create().style(DialogStyle.NATIVE).title("No output folder")
			.message("Please choose an output folder").showError();
			return;
		}
		if (list.getItems().size() < 1)
			return; // just don't do anything
		
		CountPrinter printer = null;
		if (!cbDict.isSelected()){

			WordCounter counter = new WordCounter();
			if (cbNoNumbers.isSelected())
				counter.addNoNumberFilter();
			if (cbNoCurrency.isSelected())
				counter.addNoCurrencyFilter();
			if (cbStop.isSelected()){
				if (stopsFile == null){
					Dialogs.create().style(DialogStyle.NATIVE).title("No stop word file")
					.message("Please choose a file of stopwords or uncheck this option")
					.showError();
					return;
				} else {
					try {
						counter.addStopwordFilter(stopsFile);
					} catch (Exception ex){
						Dialogs.create().style(DialogStyle.NATIVE).title("No stop word file")
						.message("Please choose a file of stopwords or uncheck this option")
						.showException(ex);
						return;
					}
				}
			}
			if (cbStem.isSelected()){
				try {
					String stemWord = listStemming
							.getSelectionModel().getSelectedItem();
					counter.addStemmingFilter(stemWord.toLowerCase());
				} catch (Exception ex){
					Dialogs.create().style(DialogStyle.NATIVE).title("Problem with stemmer")
					.message("There was a problem with the stemmer")
					.showException(ex);
					return;
				}
			}
			if (outputFormat.getSelectionModel().selectedItemProperty().get().equals("LDA-C"))
				printer = new LDACWordCountPrinter(counter, 
						directory, 
						listCharset.getSelectionModel().getSelectedItem().charset, 
						listLocale.getSelectionModel().getSelectedItem().locale, 
						list.getItems().toArray(new File[0]));
			else if (outputFormat.getSelectionModel().selectedItemProperty().get().equals("Matrix Market")){
				printer = new MTXWordCountPrinter(counter, 
						directory, 
						listCharset.getSelectionModel().getSelectedItem().charset, 
						listLocale.getSelectionModel().getSelectedItem().locale, 
						list.getItems().toArray(new File[0]));
			}
		} else {
			if (dictionary == null){
				Dialogs.create().style(DialogStyle.NATIVE).title("No dictionary")
				.message("Please choose a dictionary to apply").showError();
				return;
			}
			
			Charset cs = listCharset.getSelectionModel().getSelectedItem().charset;
			Locale ll = listLocale.getSelectionModel().getSelectedItem().locale;
			printer = new CSVFXCatDictCategoryCountPrinter(dictionary, 
				directory, "data.csv", list.getItems().toArray(new File[0]),
				cs, ll);
			
		}
		
		CountingTask task = printer.getNewCountingTask();
     
		task.setOnFailed(new EventHandler<WorkerStateEvent>() {
			@Override public void handle(WorkerStateEvent t) {
				Throwable ouch = task.getException();
				Dialogs.create()
				.title("Error").style(DialogStyle.NATIVE)
				.showException(ouch);
			}
		});
        		
		Dialogs.create().style(DialogStyle.NATIVE).showWorkerProgress(task);
		
		Thread th = new Thread(task);
        th.setDaemon(true);
       	th.start();
	}
	
	
	protected void saveGUIStateToPreferences() throws BackingStoreException {
		Preferences prefs = 
				Preferences.userRoot().node("org.conjugateprior.jfreq");
		prefs.putBoolean("remove_currency", cbNoCurrency.isSelected());
		prefs.putBoolean("remove_numbers", cbNoNumbers.isSelected());
		//prefs.putBoolean("gzip_file_data", cbGzip.isSelected());
		prefs.putBoolean("remove_stopwords", cbStop.isSelected());
		prefs.put("stopword_file", 
			(stopsFile == null ? "" : stopsFile.getAbsolutePath()));
		prefs.putBoolean("stem", cbStem.isSelected());
		String wrap = listStemming.getSelectionModel().getSelectedItem();
		prefs.put("stem_language", wrap);
		String oformat = outputFormat.getSelectionModel().getSelectedItem();
		prefs.put("output_format", oformat);
		String loc = listLocale.getSelectionModel().getSelectedItem().locale.toString();
		prefs.put("locale", loc);
		String cs = listCharset.getSelectionModel().getSelectedItem().charset.name();
		prefs.put("charset", cs);
		
		prefs.putBoolean("dictionary", cbDict.isSelected());
		prefs.put("dictionary_file", 
			(dictDesc.getText() == null ? "" : dictDesc.getText()));
		
		prefs.flush();
		/*
		for (String k : prefs.keys()) {
			System.err.println(k + " -> " + prefs.get(k, "NOTHING STORED"));
		}
		*/
	}
	
	protected void configureGUIFromPreferences(){
		Preferences prefs = 
				Preferences.userRoot().node("org.conjugateprior.jfreq");
		cbNoCurrency.setSelected(prefs.getBoolean("remove_currency", true));
		cbNoNumbers.setSelected(prefs.getBoolean("remove_numbers", true));
		//cbGzip.setSelected(prefs.getBoolean("gzip_data_file", false));
		boolean removeStops = prefs.getBoolean("remove_stopwords", false);
		cbStop.setSelected(removeStops);
		stopsBtn.setDisable(!removeStops);
		stopsDesc.setDisable(!removeStops);
		
		String sfile = prefs.get("stopword_file", "");
		if (!sfile.equals("")){
			File sf = new File(sfile);
			if (sf.exists()){
				stopsFile = sf;
				stopsDesc.setText(stopsFile.getAbsolutePath());
			}
		}
				
		boolean val = prefs.getBoolean("stem", false);
		cbStem.setSelected(val);
		listStemming.setDisable(!val);
		String slang = prefs.get("stem_language", "English");		
		listStemming.getSelectionModel().select(slang);
		listStemming.setDisable(!val);
		
		String out = prefs.get("output_format", "LDA-C");			
		outputFormat.getSelectionModel().select(out);
		
		// don't remember the output folder
		String loc = prefs.get("locale", Locale.getDefault().toString());
		for (Locale locale : Locale.getAvailableLocales()) {
			if (loc.equals(locale.toString())){
				listLocale.getSelectionModel().select(new LocaleWrap(locale));
				break;
			}
		}
		String enc = prefs.get("charset", Charset.defaultCharset().name());
		Map<String,Charset> sets = GraphicalWordCounter.getCharsetMap();
		for (String csname : sets.keySet()) {
			if (csname.equals(enc)){
				Charset theone = sets.get(csname);
				listCharset.getSelectionModel().select(new CharsetWrap(theone));
				break;
			}
		}
		
		// finally, do the dictionary and switchoff anything that
		// ought to be switched off
		boolean usedict = prefs.getBoolean("dictionary", false);
		String dfile = prefs.get("dictionary_file", "");
		if (!dfile.equals("")){
			try {
				File sf = new File(dfile);
				dictionary = getCategoryDictionaryFromFile(sf);
				dictDesc.setText(sf.getAbsolutePath());
			} catch (Exception ex){
				//
			}
		}
		if (usedict)
			cbDict.setSelected(true);
		setWordsRatherThanCategories(!usedict);

		
	}
	
	public static void main(String[] args) {
		launch(args);
	}
	
	// cached copy of this
	static SortedMap <String, Charset> charsetMap;
	static protected SortedMap<String, Charset> getCharsetMap(){
		if (charsetMap == null)
			charsetMap = Charset.availableCharsets();
		return charsetMap;
	}
	
	protected File[] getRecursiveDepthOneFileArray(String[] files) throws Exception {
		List<File> filelist = new ArrayList<File>();
		File fail = null;
		for (int ii = 0;  ii < files.length; ii++) {
			File f = new File(files[ii]);
			if (!f.exists()){
				fail = f;
				break;
			} if (f.isDirectory()){
				File[] contents = f.listFiles();
				for (int jj = 0; jj < contents.length; jj++) {
					if (!contents[jj].isDirectory() && !contents[jj].getName().startsWith("."))
						if (contents[jj].length() > 0)
							filelist.add(contents[jj]); // an imperfect filter but...
				}
			} else {
				if (f.length() > 0)
					filelist.add(f);
			}
		}
		if (fail != null)
			throw new Exception("File " + fail.getAbsolutePath() + " does not exist.");
		
		return filelist.toArray(new File[filelist.size()]);
	}
	
	/*
	final JProgressBar progressBar = new JProgressBar(0,100);
    progressBar.setValue(0);
    progressBar.setStringPainted(true);
    
    
	printer.addPropertyChangeListener(new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			if ("progress" == evt.getPropertyName()) {
	            int progress = (Integer) evt.getNewValue();
	            progressBar.setValue(progress);
	        } 
		}
	});
	
	JFrame f = new JFrame();
	f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	JPanel p = new JPanel(new BorderLayout());
	p.add(progressBar, BorderLayout.CENTER);
	
	JButton cancel = new JButton("Stop!");
	cancel.addActionListener(new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			boolean b = printer.cancel(false);
			System.err.println("Could we cancel? " + b);
			progressBar.setValue(0);
		}
	});
	p.add(cancel, BorderLayout.EAST);
	
	f.getContentPane().add(p);
	f.pack();
	f.setLocation(200, 200);
	f.setVisible(true);
	*/
	
}
