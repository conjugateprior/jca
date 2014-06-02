package org.conjugateprior.ca.exp;

import java.io.File;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class FXCatDictApp extends Application {
	@Override
	public void start(Stage primaryStage) throws Exception {
		primaryStage.setTitle("FXCatDict");
		BorderPane border = new BorderPane();
		
		//border.setPadding(new Insets(25, 25, 25, 25));
		
		Scene scene = new Scene(border, 600, 550); 
		
		File f = new File("/Users/will/Documents/scratch/2007_abortion_dictionary.ykd");
		FXCatDict dict = FXCatDict.readXmlCategoryDictionaryFromFile(f);
		TreeView<DCat> tree = new TreeView<DCat>();
		tree.setRoot(dict.getCategoryRoot());
		border.setCenter(tree);
		
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