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

public class MainDashboardController {

    @FXML private StackPane contentArea;
    @FXML private Button btnUsers;
    @FXML private Button btnOffres;
    @FXML private Label welcomeLabel;

    @FXML
    public void initialize() {
        // 1. Vérifier si un utilisateur est bien connecté
        User currentUser = UserSession.getInstance();

        if (currentUser == null) {
            // Sécurité : si pas de session, on ferme ou on redirige
            System.err.println("Tentative d'accès sans session active.");
            return;
        }

        // 2. Personnaliser l'accueil
        if (welcomeLabel != null) {
            welcomeLabel.setText("Bienvenue, " + currentUser.getPrenom() + " (" + currentUser.getRole() + ")");
        }

        // 3. Gestion des droits d'accès (RBAC)
        String role = currentUser.getRole().toUpperCase();

        if (!role.equals("ADMIN")) {
            // Seul l'ADMIN peut voir le bouton de gestion des utilisateurs
            if (btnUsers != null) {
                btnUsers.setVisible(false);
                btnUsers.setManaged(false); // Libère l'espace dans la barre latérale
            }
        }

        // Logique spécifique pour le CANDIDAT
        if (role.equals("CANDIDAT")) {
            // Un candidat ne gère pas les offres, il les consulte seulement
            // Tu peux désactiver d'autres boutons ici
        }
    }

    @FXML
    private void showUsers() {
        loadView("/org/example/UserView.fxml");
    }

    @FXML
    private void showOffres() {
        // Simulation d'une vue pour le Front-Office (Candidat/RH)
        loadView("/org/example/OffresView.fxml");
    }

    /**
     * Méthode générique pour charger une vue dans la zone centrale
     */
    private void loadView(String fxmlPath) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur de navigation");
            alert.setHeaderText("Impossible de charger la vue");
            alert.setContentText("Le fichier FXML est introuvable : " + fxmlPath);
            alert.showAndWait();
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        // Nettoyer la session
        UserSession.cleanUserSession();

        try {
            // Retour à la page de connexion
            Parent root = FXMLLoader.load(getClass().getResource("/org/example/LoginView.fxml"));
            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("TalentFlow - Connexion");
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }
}