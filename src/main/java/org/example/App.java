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
            // Charger la vue de gestion des offres d'emploi
            Parent root = FXMLLoader.load(getClass().getResource("/offres.fxml"));

            Scene scene = new Scene(root);

            stage.setTitle("TalentFlow - Gestion des Offres d'Emploi");
            stage.setScene(scene);

            stage.setMinWidth(920);
            stage.setMinHeight(600);

            stage.show();

            System.out.println("Page des offres lancée avec succès !");

        } catch (IOException e) {
            System.err.println("Erreur de chargement FXML (Vérifiez le chemin de offres.fxml) : " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Une erreur inattendue est survenue : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}