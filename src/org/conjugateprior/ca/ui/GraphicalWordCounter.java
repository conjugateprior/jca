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
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

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
	
	// TODO
	protected String[] stemLangs = new String[]{"English", 
		"French", "German", "Russian", "Dutch"};
	
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
	protected CheckBox cbGzip = new CheckBox();
	// output format
	protected ChoiceBox<String> outputFormat = new ChoiceBox<String>();
	protected TextField stopsDesc = new TextField();
	
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
		Text labNoNumbers = new Text("No numbers:");
				
		// no currency
		Text labNoCurrency = new Text("No currency:");

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
		
		//System.err.println(listLocale.getItems().get(0).locale);
		
		// charsets
		List<CharsetWrap> cs = CharsetWrap.getAllCharsetWraps();
		listCharset = new ChoiceBox<CharsetWrap>(FXCollections.observableArrayList(cs));
		Charset mine = Charset.defaultCharset();
		CharsetWrap mywrap = new CharsetWrap(mine);
		listCharset.getSelectionModel().select(mywrap);
		Text labCharset = new Text("Encoding:");

		// stem 
		listStemming = new ChoiceBox<String>();
		Text labStemming = new Text("Stem:");
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
				}
			}
		});
			
		// output format
		Text labFormat = new Text("Output format:");		
		outputFormat.getItems().addAll("LDA-C", "Matrix Market");
		outputFormat.getSelectionModel().select(0);
		
		// gzip output file
		Text labGzip = new Text("GZIP data file:");
			
		// stopwords
		stopsDesc.setEditable(false);
		Text labStops = new Text("Remove stopwords:");
		stopsBtn = new Button("Choose");
		stopsBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				File f = stopwordFileChooser.showOpenDialog(primaryStage);
				if (f != null){
					stopsFile = f;
					stopsDesc.setText(stopsFile.getAbsolutePath());
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
		
		grid.add(labLocale, 0, 0);
		listLocale.setMaxWidth(Double.MAX_VALUE);
		grid.add(listLocale, 1, 0, 3, 1);

		grid.add(labCharset, 0, 1);
		listCharset.setMaxWidth(Double.MAX_VALUE);
		grid.add(listCharset, 1, 1, 3, 1);

		grid.add(labLowercase, 0, 2);
		grid.add(cbLowercase, 1, 2);

		grid.add(labNoNumbers, 0, 3);
		grid.add(cbNoNumbers, 1, 3);

		grid.add(labNoCurrency, 0, 4);
		grid.add(cbNoCurrency, 1, 4);
	
		grid.add(labStops, 0, 5);
		grid.add(cbStop, 1, 5);
		grid.add(stopsDesc, 2, 5);
		grid.add(stopsBtn, 3, 5);	
				
		grid.add(labStemming, 0, 6);
		grid.add(cbStem, 1, 6);
		//listStemming.setMaxWidth(Double.MAX_VALUE);
		grid.add(listStemming, 2, 6, 3, 1);
		
		grid.add(labFolder, 0, 7);
		grid.add(dirDesc, 1, 7, 2, 1);
		grid.add(btn, 3, 7);	
		
		grid.add(labFormat, 0, 8);
		outputFormat.setMaxWidth(150);
		grid.add(outputFormat, 1, 8, 3, 1);
		
		//grid.add(labGzip, 0, 9);
		//grid.add(cbGzip, 1, 9);	
		
		// make the splitpane
		SplitPane sp = new SplitPane();
		
		// left hand side
        Label lab1 = new Label("Document Properties");
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
        
        // add them
        sp.getItems().addAll(propertiespane, listpane);
        
        
        
		//grid.setGridLinesVisible(true);
		Scene scene = new Scene(sp, 1000, 550); 
		
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
