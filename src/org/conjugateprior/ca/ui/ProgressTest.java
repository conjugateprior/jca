package org.conjugateprior.ca.ui;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Locale;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import org.conjugateprior.ca.FXCategoryDictionary;
import org.conjugateprior.ca.reports.CSVFXCategoryDictionaryCountPrinter;
import org.conjugateprior.ca.reports.CountPrinter;
import org.conjugateprior.ca.reports.CountPrinter.CountingTask;
import org.controlsfx.control.ButtonBar;
import org.controlsfx.control.ButtonBar.ButtonType;
import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.DefaultDialogAction;
import org.controlsfx.dialog.Dialog;
import org.controlsfx.dialog.DialogStyle;
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
        
        final Button button = new Button("Go	!");
        //button.setPadding(new Insets(100));
        button.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				try {
					Task<Void> task = getTask();
					Dialogs.create().owner(stage).style(DialogStyle.NATIVE).showWorkerProgress(task);
					
					//bar.progressProperty().unbind(); // last task
					//bar.progressProperty().bind(task.progressProperty());
					Thread th = new Thread(task);
	                th.setDaemon(true);
	               	th.start();
	               	
	               	Throwable thro = task.getException();
	                if (thro != null)
	                	thro.printStackTrace();
	               	
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		});
        
        final Button btn = new Button("Show");
        btn.setOnAction(new EventHandler<ActionEvent>() {

        	final ProgressBar bar = new ProgressBar();
            final Task<Void> task = getTask();
            //Text updating = new Text("");
            
            final DefaultDialogAction actionLogin = new DefaultDialogAction("Login") {
                {
                    ButtonBar.setType(this, ButtonType.OK_DONE);
                }
 
                @Override
                public void handle(ActionEvent ae) {
                    Dialog dlg = (Dialog) ae.getSource();
                    System.err.println("cancelling the task");
                    task.cancel();
                    
                    dlg.hide();
                }
 
                public String toString() {
                    return "CANCELLED";
                }
            };
            /*
            private void validate() {
                actionLogin.disabledProperty().set(
                        txUserName.getText().trim().isEmpty()
                                || txPassword.getText().trim().isEmpty());
            }
 	         */
 
            @Override
            public void handle(ActionEvent arg0) {
                Dialog dlg = new Dialog(stage, "Login Dialog", false, DialogStyle.NATIVE); // native
                
                /*
                ChangeListener<String> changeListener = new ChangeListener<String>() {
                    @Override
                    public void changed(
                            ObservableValue<? extends String> observable,
                            String oldValue, String newValue) {
                        //validate();
                    }
                };
 
                txUserName.textProperty().addListener(changeListener);
                txPassword.textProperty().addListener(changeListener);
 				*/
 
                final GridPane content = new GridPane();
                content.setHgap(10);
                content.setVgap(10);
 
                content.add(new Label("Progress"), 0, 0);
                content.add(bar, 1, 0);
                GridPane.setHgrow(bar, Priority.ALWAYS);
                
                dlg.setResizable(false);
                dlg.setIconifiable(false);
                //dlg.setGraphic(new ImageView(HelloDialog.class.getResource(
                //        "login.png").toString()));
                dlg.setContent(content);
                dlg.getActions().addAll(actionLogin);
                //validate();
                /*
                Platform.runLater(new Runnable(){ 
                	public void run() { txUserName.requestFocus(); } ;	
                });
                */
 
                
                bar.progressProperty().bind(task.progressProperty());
                
                Thread th = new Thread(task);
                th.setDaemon(true);
                th.start();

                Action response = dlg.show();
                
                System.out.println("response: " + response);
                
                Throwable thro = task.getException();
                if (thro != null)
                	thro.printStackTrace();
                
            }
        });
        
        final HBox hb = new HBox();
        hb.setPadding(new Insets(200));
        hb.setSpacing(5);
        hb.setAlignment(Pos.CENTER);
        hb.getChildren().addAll(btn);
        
        scene.setRoot(hb);
        
        stage.show();
    }
    
    protected Task<Void> getTask(){
    	String fname = "/Users/will/Dropbox/projects/bara-et-al/"
        		+ "2007_abortion_dictionary.ykd";
        FXCategoryDictionary dict = null;
        try {
        	dict = FXCategoryDictionary.readXmlCategoryDictionaryFromFile(new File(fname));
        } catch (Exception ex){
        	ex.printStackTrace();
        }
        
        CountPrinter printer = new CSVFXCategoryDictionaryCountPrinter(
        		dict, 
        		new File("/Users/will/Desktop/fold2"),
        		"data.csv",
				(new File("/Users/will/Dropbox/projects/bara-et-al/"
						+ "debate/abortion-debate-by-speaker/")).listFiles(),
				Charset.forName("UTF8"), Locale.ENGLISH);
		
        CountingTask task = printer.getNewCountingTask();
        task.setOnFailed(new EventHandler<WorkerStateEvent>() {
        	   @Override public void handle(WorkerStateEvent t) {
        		     Throwable ouch = task.getException();
        		     Dialogs.create().owner(stage)
        		        .title("Error").style(DialogStyle.NATIVE)
        		        .showException(ouch);
        		     System.out.println(ouch.getClass().getName() + " -> " + ouch.getMessage());
        		   }
        		 });
        
        return task;
    }
        
    public static void main(String[] args) {
        launch(args);
    }
}

/*
.setOnAction(new EventHandler<actionevent>() {
	 
    final TextField txUserName = new TextField();
    final PasswordField txPassword = new PasswordField();
    final Action actionLogin = new DefaultDialogAction("Login",
            ActionTrait.CLOSING, ActionTrait.DEFAULT) {

        {
            ButtonBar.setType(this, ButtonType.OK_DONE);
        }

        @Override
        public void handle(ActionEvent ae) {
            Dialog dlg = (Dialog) ae.getSource();
            // real login code here
            dlg.setResult(this);
        }

        public String toString() {
            return "LOGIN";
        };
    };

    private void validate() {
        actionLogin.disabledProperty().set(
                txUserName.getText().trim().isEmpty()
                        || txPassword.getText().trim().isEmpty());
    }

    @Override
    public void handle(ActionEvent arg0) {
        Dialog dlg = new Dialog(includeOwner() ? stage : null,
                "Login Dialog", cbUseLightweightDialog.isSelected(),getDialogStyle());
        if (cbShowMasthead.isSelected()) {
            dlg.setMasthead("Login to ControlsFX");
        }

        ChangeListener<string> changeListener = new ChangeListener<string>() {
            @Override
            public void changed(
                    ObservableValue<!--? extends String--> observable,
                    String oldValue, String newValue) {
                validate();
            }
        };

        txUserName.textProperty().addListener(changeListener);
        txPassword.textProperty().addListener(changeListener);

        final GridPane content = new GridPane();
        content.setHgap(10);
        content.setVgap(10);

        content.add(new Label("User name"), 0, 0);
        content.add(txUserName, 1, 0);
        GridPane.setHgrow(txUserName, Priority.ALWAYS);
        content.add(new Label("Password"), 0, 1);
        content.add(txPassword, 1, 1);
        GridPane.setHgrow(txPassword, Priority.ALWAYS);

        dlg.setResizable(false);
        dlg.setIconifiable(false);
        dlg.setGraphic(new ImageView(HelloDialog.class.getResource(
                "login.png").toString()));
        dlg.setContent(content);
        dlg.getActions().addAll(actionLogin, Dialog.Actions.CANCEL);
        validate();

        Platform.runLater( () -> txUserName.requestFocus() );

        Action response = dlg.show();
        System.out.println("response: " + response);
    }
});
*/
