package org.example.GUI;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import org.example.dao.UserDAO;
import org.example.model.User;
import java.io.IOException;

public class RegisterController {

    @FXML private TextField nomField, prenomField, emailField, telField;
    @FXML private PasswordField passwordField, confirmPasswordField;
    @FXML private Label statusLabel;

    private UserDAO userDAO = new UserDAO();

    @FXML
    private void handleRegister() {
        statusLabel.setText("");

        if (!validerChamps()) return;

        // On ajoute '0' comme premier argument pour l'ID
        User newUser = new User(
                0,
                nomField.getText().trim(),
                prenomField.getText().trim(),
                emailField.getText().trim(),
                passwordField.getText(),
                "CANDIDAT",
                telField.getText().trim()
        );

        try {
            userDAO.create(newUser);
            statusLabel.setStyle("-fx-text-fill: green;");
            statusLabel.setText("✅ Compte créé avec succès !");
            handleBackToLogin();
        } catch (Exception e) {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("❌ Erreur : Email déjà utilisé ou problème BDD.");
            e.printStackTrace();
        }
    }

    private boolean validerChamps() {
        if (nomField.getText().isEmpty() || emailField.getText().isEmpty() || passwordField.getText().isEmpty()) {
            statusLabel.setText("⚠️ Veuillez remplir tous les champs.");
            return false;
        }
        if (!passwordField.getText().equals(confirmPasswordField.getText())) {
            statusLabel.setText("⚠️ Les mots de passe ne correspondent pas.");
            return false;
        }
        if (telField.getText().length() != 8) {
            statusLabel.setText("⚠️ Le téléphone doit avoir 8 chiffres.");
            return false;
        }
        return true;
    }

    @FXML
    private void handleBackToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/org/example/LoginView.fxml"));
            if (nomField.getScene() != null) {
                nomField.getScene().setRoot(root);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}