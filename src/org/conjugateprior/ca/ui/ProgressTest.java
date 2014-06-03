package org.conjugateprior.ca.ui;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Locale;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import org.conjugateprior.ca.exp.FXCatDict;
import org.conjugateprior.ca.reports.CSVFXCatDictCategoryCountPrinter;
import org.conjugateprior.ca.reports.CountPrinter;
import org.conjugateprior.ca.reports.CountPrinter.CountingTask;
import org.controlsfx.dialog.Dialogs;

public class ProgressTest extends Application {
	 
	Stage stage;
	
    @Override
    public void start(Stage st) {
        stage = st;
    	Group root = new Group();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Progress Controls");
        
        final Button button = new Button("Stop!");
        button.setPadding(new Insets(100));
        button.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				
				String fname = "/Users/will/Dropbox/projects/bara-et-al/"
		        		+ "2007_abortion_dictionary.ykd";
		        FXCatDict dict = null;
		        try {
		        	dict = FXCatDict.readXmlCategoryDictionaryFromFile(new File(fname));
		        } catch (Exception ex){
		        	ex.printStackTrace();
		        }
		        
		        CountPrinter printer = new CSVFXCatDictCategoryCountPrinter(
		        		dict, 
		        		new File("/Users/will/Desktop/fold2"),
		        		"data.csv",
						(new File("/Users/will/Dropbox/projects/bara-et-al/"
								+ "debate/abortion-debate-by-speaker/")).listFiles(),
						Charset.forName("UTF8"), Locale.ENGLISH);
				
		        CountingTask task = printer.getNewCountingTask();
		        Dialogs.create().owner(stage).nativeTitleBar().showWorkerProgress(task);
				
				//bar.progressProperty().unbind(); // last task
				//bar.progressProperty().bind(task.progressProperty());
				new Thread(task).start();
			}	
		});
 
        
        // TODO fixme!
        final HBox hb = new HBox();
        hb.setSpacing(5);
        hb.setAlignment(Pos.CENTER);
        hb.getChildren().addAll(button);
        
        scene.setRoot(hb);
        
        stage.show();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
