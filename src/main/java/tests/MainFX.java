package tests;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainFX extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {

        Parent root = FXMLLoader.load(getClass().getResource("/AjouterCandidature.fxml"));

        // Titre de la fenêtre basé sur ton projet [cite: 1]
        primaryStage.setTitle("TalentFlow - Gestion des Candidatures");

        // Création de la scène avec les dimensions du FXML
        Scene scene = new Scene(root);

        primaryStage.setScene(scene);
        primaryStage.setResizable(false); // Optionnel : empêche de déformer le design
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}