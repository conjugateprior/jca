package org.conjugateprior.ca.exp;

import java.io.File;
import java.io.IOException;

import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;

import javax.imageio.ImageIO;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;

public class Biplot extends Application {
	 
    private NumberAxis xAxis;
    private NumberAxis yAxis;
        
    SimpleCorrespondenceAnalysis sca;
    
    private ScatterChart<Number, Number> createChart(SimpleCorrespondenceAnalysis ca) {
        xAxis = new NumberAxis();
        yAxis = new NumberAxis();
        //final BubbleChart<Number, Number> bc = new BubbleChart<>
        final ScatterChart<Number, Number> bc = new ScatterChart<>(xAxis, yAxis);
        // setup chart
        bc.setTitle("Simple Correspondence Analysis");
        xAxis.setLabel(String.format("Axis 1 (%3.2f%%)", sca.getEigVariances()[0]));
        yAxis.setLabel(String.format("Axis 2 (%3.2f%%)", sca.getEigVariances()[1]));
        // add starting data
        XYChart.Series<Number, Number> series1 = new XYChart.Series<>();
        
        series1.setName("Rows");
        double[][] rowprinc = ca.getPrincipalRowCoordinates();
        double[][] colstand = ca.getStandardColumnCoordinates();
        for (int ii = 0; ii < rowprinc.length; ii++){ 
        	XYChart.Data<Number, Number> nd = 
        			new XYChart.Data<Number, Number>(rowprinc[ii][0], rowprinc[ii][1]);
        	nd.setNode(new Label(sca.getRowNames()[ii]));
        	series1.getData().add(nd);
        }
        XYChart.Series<Number, Number> series2 = new XYChart.Series<>();
        series2.setName("Columns");
        for (int ii = 0; ii < colstand.length; ii++) {
        	XYChart.Data<Number, Number> nd = 
			new XYChart.Data<Number, Number>(colstand[ii][0], colstand[ii][1]);
        	nd.setNode(new Label(sca.getColumnNames()[ii]));
        	series2.getData().add(nd);
        }
        bc.getData().addAll(series1, series2);
        return bc;
    }
 
    public void saveAsPng(ScatterChart<Number, Number> bc, File file) {
        WritableImage image = bc.snapshot(new SnapshotParameters(), null);
        try {
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
        } catch (IOException e) {
        	e.printStackTrace();
        }
    }
 
    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setScene(new Scene(createChart(sca)));
        primaryStage.show();
    }
 
    @Override
    public void init() throws Exception {
    	RealMatrix smoke = new Array2DRowRealMatrix(new double[][]{
        		{4,2,3,2},{4,3,7,4},{25,10,12,4},{18,24,33,13},
        		{10,6,7,2}});
        System.err.println(smoke);
        sca = new SimpleCorrespondenceAnalysis(smoke, 2, 
        		new String[]{"SM", "JM", "SE", "JE", "SC"}, 
        		new String[]{"none", "light",  "medium", "heavy"});
        System.out.println(sca);
    }
    
    /**
     * Java main for when running without JavaFX launcher
     */
    public static void main(String[] args) {
    	launch(args);
    }
}