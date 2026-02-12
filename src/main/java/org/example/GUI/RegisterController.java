package org.example.GUI;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import org.example.dao.UserDAO;
import org.example.model.User;
import org.example.utils.ValidationUtils;

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
        if (nomField.getText().isEmpty() || prenomField.getText().isEmpty() || emailField.getText().isEmpty()) {
            statusLabel.setText("⚠️ Tous les champs sont obligatoires.");
            return false;
        }
        if (ValidationUtils.isInvalidName(nomField.getText())) {
            statusLabel.setText("⚠️ Nom invalide (lettres uniquement).");
            return false;
        }
        if (ValidationUtils.isInvalidEmail(emailField.getText())) {
            statusLabel.setText("⚠️ Format email incorrect.");
            return false;
        }
        if (ValidationUtils.isInvalidTel(telField.getText())) {
            statusLabel.setText("⚠️ Le téléphone doit contenir 8 chiffres.");
            return false;
        }
        if (ValidationUtils.isInvalidPassword(passwordField.getText())) {
            statusLabel.setText("⚠️ Password: 8 caractères, 1 Maj, 1 Chiffre, 1 Symbole.");
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