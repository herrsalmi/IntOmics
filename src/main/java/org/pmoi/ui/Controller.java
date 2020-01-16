package org.pmoi.ui;

import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.pmoi.models.ResultsFX;

public class Controller {

    @FXML
    private TableView<ResultsFX> resultTable;
    @FXML
    private TableColumn<ResultsFX, String> proteinColumn;
    @FXML
    private TableColumn<ResultsFX, String> proteinIDColumn;
    @FXML
    private TableColumn<ResultsFX, String> proteinNameColumn;
    @FXML
    private TableColumn<ResultsFX, String> geneColumn;
    @FXML
    private TableColumn<ResultsFX, String> geneIDColumn;
    @FXML
    private TableColumn<ResultsFX, String> geneNameColumn;
    @FXML
    private TableColumn<ResultsFX, String> IScoreColumn;
    @FXML
    private TableColumn<ResultsFX, String> DScoreIDColumn;
    @FXML
    private TableColumn<ResultsFX, String> RScoreNameColumn;
    @FXML
    private TableColumn<ResultsFX, String> pvalueColumn;
    @FXML
    private TableColumn<ResultsFX, String> fcColumn;
    @FXML
    private TableColumn<ResultsFX, String> pathwayColumn;

    private MainFX mainFX;

    @FXML
    private void initialize() {
        proteinColumn.setCellValueFactory(cellData -> cellData.getValue().proteinProperty());
        proteinIDColumn.setCellValueFactory(cellData -> cellData.getValue().proteinIDProperty());
        proteinNameColumn.setCellValueFactory(cellData -> cellData.getValue().proteinNameProperty());
        geneColumn.setCellValueFactory(cellData -> cellData.getValue().geneProperty());
        geneIDColumn.setCellValueFactory(cellData -> cellData.getValue().geneIdProperty());
        geneNameColumn.setCellValueFactory(cellData -> cellData.getValue().geneNameProperty());
        IScoreColumn.setCellValueFactory(cellData -> cellData.getValue().IScoreProperty());
        DScoreIDColumn.setCellValueFactory(cellData -> cellData.getValue().DScoreProperty());
        RScoreNameColumn.setCellValueFactory(cellData -> cellData.getValue().RScoreProperty());
        pvalueColumn.setCellValueFactory(cellData -> cellData.getValue().pvalueProperty());
        fcColumn.setCellValueFactory(cellData -> cellData.getValue().fcProperty());
        pathwayColumn.setCellValueFactory(cellData -> cellData.getValue().pathwayProperty());
    }

    public void setMainApp(MainFX mainFX) {
        this.mainFX = mainFX;
        resultTable.setItems(mainFX.getData());
    }
}
