package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class HomeController {

    @FXML
    private BorderPane mainPane;

    @FXML
    private void loadDashboard(ActionEvent event) {
        switchCenter("/view/dashboard_view.fxml");
    }

    @FXML
    private void loadEntretiens(ActionEvent event) {
        switchCenter("/view/entretien_view.fxml");
    }

    @FXML
    private void loadCandidatures(ActionEvent event) {
        switchCenter("/view/candidature_view.fxml");
    }

    @FXML
    private void handleExit(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    private void switchCenter(String fxmlPath) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource(fxmlPath));
            mainPane.setCenter(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
