package org.conjugateprior.ca.ui;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Locale;

import org.conjugateprior.ca.reports.LDACWordCountTask;
import org.conjugateprior.ca.reports.WordCounter;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class ProgressTest extends Application {
	 
    @Override
    public void start(Stage stage) {
        Group root = new Group();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Progress Controls");
 
        final ProgressBar bar = new ProgressBar();
        
        final WordCounter rep = new WordCounter();        
        final Button button = new Button("Stop!");
        button.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				bar.progressProperty().unbind(); // last task
				LDACWordCountTask task = new LDACWordCountTask(rep,
						new File("/Users/will/Desktop/fold"), 
						Charset.forName("UTF8"), Locale.ENGLISH, 
						new File[]{new File("/Users/will/Desktop/jfreqing/d1.txt"),
						new File("/Users/will/Desktop/jfreqing/d2.txt")});
				bar.progressProperty().bind(task.progressProperty());
				new Thread(task).start();
			}	
		});
 
        
        // TODO fixme!
        final HBox hb = new HBox();
        hb.setSpacing(5);
        hb.setAlignment(Pos.CENTER);
        //hb.getChildren().addAll(button, pb, pi);
        scene.setRoot(hb);
        stage.show();
    }
    public static void main(String[] args) {
        launch(args);
    }
}
