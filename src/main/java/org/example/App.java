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
            // 1. On charge la vue de CONNEXION au démarrage
            // C'est le point d'entrée sécurisé de TalentFlow
            Parent root = FXMLLoader.load(getClass().getResource("/org/example/LoginView.fxml"));

            // 2. Créer la scène initiale
            Scene scene = new Scene(root);

            // 3. Configurer la fenêtre principale
            stage.setTitle("TalentFlow - Connexion");
            stage.setScene(scene);

            // Définir une taille minimale raisonnable pour le Login
            stage.setMinWidth(600);
            stage.setMinHeight(450);

            // 4. Afficher la fenêtre
            stage.show();

            System.out.println("Page de connexion lancée avec succès !");

        } catch (IOException e) {
            System.err.println("Erreur de chargement FXML (Vérifiez le chemin de LoginView.fxml) : " + e.getMessage());
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