package org.pmoi.ui;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import org.pmoi.models.ResultsFX;

import java.io.IOException;
import java.util.List;

public class MainFX extends Application {

    private Stage primaryStage;
    private AnchorPane rootLayout;

    private static ObservableList<ResultsFX> data = FXCollections.observableArrayList();

    @Override
    public void start(Stage stage) throws Exception {
        this.primaryStage = stage;

        stage.setTitle("Hello World");

        initRootLayout();


    }

    private void initRootLayout() throws IOException {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(MainFX.class.getResource("/fxml/mainResults.fxml"));
        rootLayout = loader.load();
        primaryStage.setScene(new Scene(rootLayout));

        primaryStage.show();


        data.add(new ResultsFX("IGF2", "3481", "insulin like growth factor 2", "IGF1R",
                "3480", "insulin like growth factor 1 receptor", "0.997", "878000.0",
                "434000.0", "0,034", "1,4806", "NA"));
        Controller controller = loader.getController();
        controller.setMainApp(this);


    }

    public ObservableList<ResultsFX> getData() {
        return data;
    }

    public static void main(List<ResultsFX> args) {
        data.addAll(args);
        launch();
    }
}
