package org.conjugateprior.ca.exp;

import java.io.File;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class FXCatDictPanelApp extends Application {
	@Override
	public void start(Stage primaryStage) throws Exception {
		primaryStage.setTitle("FXCatDict");
		File f = new File("/Users/will/Documents/scratch/2007_abortion_dictionary.ykd");
		FXCatDict dict = FXCatDict.readXmlCategoryDictionaryFromFile(f);
		FXCatDictPanel panel = new FXCatDictPanel(dict);
		
		Scene scene = new Scene(panel.getBorderPane(), 600, 550); 
		primaryStage.setScene(scene);
		primaryStage.show();
	}
	
	public static void main(String[] args) throws Exception {
		File f = new File("/Users/will/Documents/scratch/2007_abortion_dictionary.ykd");
		FXCatDict dict = FXCatDict.readXmlCategoryDictionaryFromFile(f);

		System.out.println(dict);
        
        launch(args);
	}
}