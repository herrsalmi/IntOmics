package org.pmoi.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RandomWalkLineChart extends Application {

    private static List<XYChart.Data> data;
    private static List<XYChart.Data> data2;

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("LineChart Experiments");

        NumberAxis xAxis = new NumberAxis();

        NumberAxis yAxis = new NumberAxis();

        LineChart lineChart = new LineChart(xAxis, yAxis);

        XYChart.Series dataSeries1 = new XYChart.Series();
        dataSeries1.setName("hit");

        XYChart.Series dataSeries2 = new XYChart.Series();
        dataSeries2.setName("miss");

        dataSeries1.getData().addAll(data);

        if (data2 != null)
            dataSeries2.getData().addAll(data2);


        lineChart.getData().add(dataSeries1);
        if (data2 != null)
            lineChart.getData().add(dataSeries2);
        lineChart.setCreateSymbols(false);

        VBox vbox = new VBox(lineChart);

        Scene scene = new Scene(vbox, 400, 200);

        primaryStage.setScene(scene);
        primaryStage.setHeight(300);
        primaryStage.setWidth(1200);

        primaryStage.show();
    }


    public static void main(List<Double> runningSum) {
        data = new ArrayList<>(runningSum.size());
        AtomicInteger xPos = new AtomicInteger(0);
        runningSum.forEach(e -> data.add(new XYChart.Data<>(xPos.getAndIncrement(), e)));
        Application.launch();
    }

    public static void main(List<Double> runningSum, List<Double> runningSum2) {
        data = new ArrayList<>(runningSum.size());
        data2 = new ArrayList<>(runningSum2.size());
        AtomicInteger xPos = new AtomicInteger(0);
        runningSum.forEach(e -> data.add(new XYChart.Data<>(xPos.getAndIncrement(), e)));
        xPos.set(0);
        runningSum2.forEach(e -> data2.add(new XYChart.Data<>(xPos.getAndIncrement(), e)));
        Application.launch();
    }
}
