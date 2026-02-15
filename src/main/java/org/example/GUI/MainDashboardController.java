package org.example.GUI;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.example.model.User;

import java.io.IOException;
import java.net.URL;

public class MainDashboardController {

    @FXML private StackPane contentArea;
    @FXML private Button btnUsers;
    @FXML private Button btnOffres;
    @FXML private Label welcomeLabel;

    @FXML
    public void initialize() {
        User currentUser = UserSession.getInstance();

        if (currentUser == null) {
            System.err.println("Accès refusé : Aucune session active.");
            return;
        }

        if (welcomeLabel != null) {
            welcomeLabel.setText("Bienvenue, " + currentUser.getPrenom() + " [" + currentUser.getRole().toUpperCase() + "]");
        }

        applySecurityRestrictions(currentUser.getRole().toUpperCase());
    }

    private void applySecurityRestrictions(String role) {
        if (!role.equalsIgnoreCase("ADMIN")) {
            if (btnUsers != null) {
                btnUsers.setVisible(false);
                btnUsers.setManaged(false);
            }
        }
    }

    @FXML
    private void showUsers() {
        // Chemin absolu à partir de la racine des ressources
        loadView("/org/example/UserView.fxml");
    }

    @FXML
    private void showOffres() {
        loadView("/org/example/OffresView.fxml");
    }

    /**
     * Charge dynamiquement un fichier FXML dans la zone centrale
     */
    private void loadView(String fxmlPath) {
        try {
            URL resource = getClass().getResource(fxmlPath);

            if (resource == null) {
                throw new IOException("Fichier introuvable : " + fxmlPath);
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Parent view = loader.load();

            // On vide et on remplace le contenu de la zone centrale
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);

        } catch (IOException e) {
            showErrorAlert("Erreur de navigation", "Impossible de charger la page : " + fxmlPath);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        UserSession.cleanUserSession();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/org/example/LoginView.fxml"));
            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("TalentFlow - Connexion");
            stage.centerOnScreen();
        } catch (IOException e) {
            System.err.println("Erreur lors de la déconnexion : " + e.getMessage());
            System.exit(0);
        }
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}