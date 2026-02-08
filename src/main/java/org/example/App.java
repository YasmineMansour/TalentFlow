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
            // 1. Charger le fichier FXML depuis le dossier resources
            // Le "/" au début est CRUCIAL, il part de la racine du dossier resources
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/UserView.fxml"));
            Parent root = loader.load();

            // 2. Créer la scène avec le contenu du FXML
            Scene scene = new Scene(root);

            // 3. Configurer la fenêtre principale
            stage.setTitle("TalentFlow - Système de Gestion de Candidatures");
            stage.setScene(scene);

            // Empêcher la fenêtre d'être trop petite
            stage.setMinWidth(600);
            stage.setMinHeight(500);

            // 4. Afficher la fenêtre
            stage.show();

            System.out.println("Interface graphique lancée avec succès !");

        } catch (IOException e) {
            System.err.println("Erreur lors du chargement de l'interface FXML : " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Une erreur inattendue est survenue : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // Lance l'application JavaFX
        launch(args);
    }
}