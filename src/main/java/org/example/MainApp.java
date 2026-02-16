package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        URL fxmlUrl = getClass().getResource("/view/Home_page.fxml");

        System.out.println("FXML URL = " + fxmlUrl);

        if (fxmlUrl == null) {
            throw new IllegalStateException("FXML introuvable. Vérifie: src/main/resources/view/Home_page.fxml");
        }

        FXMLLoader loader = new FXMLLoader(fxmlUrl);

        Scene scene = new Scene(loader.load());

        URL cssUrl = getClass().getResource("/styles/app.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        stage.setTitle("TalentFlow - Gestion Entretiens");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
