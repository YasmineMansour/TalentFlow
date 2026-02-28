package org.example.GUI;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.Node;
import javafx.stage.Stage;
import org.example.model.User;

import java.io.IOException;
import java.net.URL;

public class MainDashboardController {

    @FXML private StackPane contentArea;
    @FXML private Button btnUsers;
    @FXML private Button btnOffres;
    @FXML private Button btnDashboard;
    @FXML private Button btnEntretiens;
    @FXML private Button btnDecisions;
    @FXML private Button btnStatsEntretiens;
    @FXML private Label welcomeLabel;

    @FXML
    public void initialize() {
        User currentUser = UserSession.getInstance();

        if (currentUser == null) {
            System.err.println("Accès refusé : Aucune session active.");
            return;
        }

        if (welcomeLabel != null) {
            welcomeLabel.setText(currentUser.getPrenom() + " " + currentUser.getNom() + " [" + currentUser.getRole().toUpperCase() + "]");
        }

        applySecurityRestrictions(currentUser.getRole().toUpperCase());

        // Charger le tableau de bord statistique par défaut
        showDashboard();
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
    private void showDashboard() {
        loadView("/org/example/DashboardHome.fxml");
    }

    @FXML
    private void showUsers() {
        loadView("/org/example/UserView.fxml");
    }

    @FXML
    private void showOffres() {
        loadView("/org/example/OffresView.fxml");
    }

    @FXML
    private void showEntretiens() {
        loadView("/org/example/EntretienView.fxml");
    }

    @FXML
    private void showDecisions() {
        loadView("/org/example/DecisionView.fxml");
    }

    @FXML
    private void showEntretienDashboard() {
        loadView("/org/example/EntretienDashboardView.fxml");
    }

    private void loadView(String fxmlPath) {
        try {
            URL resource = getClass().getResource(fxmlPath);

            if (resource == null) {
                showErrorAlert("Module non disponible",
                        "La vue " + fxmlPath.substring(fxmlPath.lastIndexOf('/') + 1)
                        + " n'est pas encore implémentée.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Parent view = loader.load();

            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);

            // Forcer la vue chargée à remplir tout l'espace disponible
            if (view instanceof Node) {
                StackPane.setAlignment(view, javafx.geometry.Pos.TOP_LEFT);
            }
            if (view instanceof javafx.scene.layout.Region region) {
                region.prefWidthProperty().bind(contentArea.widthProperty());
                region.prefHeightProperty().bind(contentArea.heightProperty());
            }

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