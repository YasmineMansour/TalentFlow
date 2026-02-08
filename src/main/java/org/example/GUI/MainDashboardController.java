package org.example.GUI; // Harmonisation avec le dossier physique

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import java.io.IOException;

public class MainDashboardController {
    @FXML private StackPane contentArea;
    @FXML private Button btnUsers;

    @FXML
    public void initialize() {
        // Utilisation correcte de la Session
        if (UserSession.getInstance() != null) {
            String role = UserSession.getInstance().getRole();
            if (!role.equalsIgnoreCase("ADMIN")) {
                if (btnUsers != null) {
                    btnUsers.setVisible(false);
                    btnUsers.setManaged(false);
                }
            }
        }
    }

    @FXML
    private void showUsers() {
        try {
            Parent view = FXMLLoader.load(getClass().getResource("/org/example/UserView.fxml"));
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        UserSession.cleanUserSession();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/org/example/LoginView.fxml"));
            btnUsers.getScene().setRoot(root);
        } catch (IOException e) {
            System.exit(0);
        }
    }
}