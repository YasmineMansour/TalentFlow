package org.example.GUI;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.dao.UserDAO;
import org.example.model.User;
import java.io.IOException;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private UserDAO userDAO = new UserDAO();

    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Veuillez remplir tous les champs !");
            return;
        }

        try {
            User user = userDAO.login(email, password);
            if (user != null) {
                UserSession.setInstance(user);
                loadDashboard();
            } else {
                errorLabel.setText("Email ou mot de passe incorrect !");
            }
        } catch (Exception e) {
            errorLabel.setText("Erreur de connexion.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleShowRegister() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/org/example/RegisterView.fxml"));
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("TalentFlow - Inscription");
        } catch (IOException e) {
            errorLabel.setText("Erreur de chargement de la page d'inscription.");
            e.printStackTrace();
        }
    }

    private void loadDashboard() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/org/example/MainDashboard.fxml"));
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("TalentFlow - Dashboard");
            stage.centerOnScreen();
        } catch (IOException e) {
            System.err.println("Erreur chargement Dashboard FXML: " + e.getMessage());
        }
    }
}