package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/org/example/LoginView.fxml"));
            Scene scene = new Scene(root);

            // Charger la feuille de style globale
            String css = getClass().getResource("/org/example/styles.css").toExternalForm();
            scene.getStylesheets().add(css);

            stage.setTitle("TalentFlow - Connexion");
            stage.setScene(scene);
            stage.setMinWidth(900);
            stage.setMinHeight(550);
            stage.setMaximized(true);
            stage.centerOnScreen();
            stage.show();

            System.out.println("✅ TalentFlow lancé avec succès !");

        } catch (IOException e) {
            System.err.println("Erreur de chargement FXML : " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Erreur inattendue : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}