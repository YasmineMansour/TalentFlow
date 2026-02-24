package org.example.GUI;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.dao.UserDAO;
import org.example.model.User;
import org.example.utils.EmailService;
import org.example.utils.ValidationUtils;

import java.io.IOException;

public class RegisterController {

    @FXML private TextField nomField, prenomField, emailField, telField;
    @FXML private PasswordField passwordField, confirmPasswordField;
    @FXML private TextField passwordVisible, confirmPasswordVisible;
    @FXML private Button togglePasswordBtn, toggleConfirmBtn;
    @FXML private Label statusLabel;

    private UserDAO userDAO = new UserDAO();
    private boolean passwordShown = false;
    private boolean confirmShown = false;

    @FXML
    public void initialize() {
        // Synchroniser password fields avec leurs TextField visibles
        passwordVisible.textProperty().bindBidirectional(passwordField.textProperty());
        confirmPasswordVisible.textProperty().bindBidirectional(confirmPasswordField.textProperty());
    }

    @FXML
    private void togglePassword() {
        passwordShown = !passwordShown;
        passwordField.setVisible(!passwordShown);
        passwordVisible.setVisible(passwordShown);
        togglePasswordBtn.setText(passwordShown ? "🙈" : "👁");
    }

    @FXML
    private void toggleConfirmPassword() {
        confirmShown = !confirmShown;
        confirmPasswordField.setVisible(!confirmShown);
        confirmPasswordVisible.setVisible(confirmShown);
        toggleConfirmBtn.setText(confirmShown ? "🙈" : "👁");
    }

    @FXML
    private void handleRegister() {
        statusLabel.setText("");
        statusLabel.setStyle("-fx-text-fill: #d63031;");

        if (!validerChamps()) return;

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
            boolean created = userDAO.create(newUser);
            if (created) {
                statusLabel.setStyle("-fx-text-fill: #00b894;");
                statusLabel.setText("✅ Compte créé avec succès !");

                // Envoyer un email de bienvenue en arrière-plan
                String email = newUser.getEmail();
                String prenom = newUser.getPrenom();
                new Thread(() -> EmailService.sendWelcomeEmail(email, prenom)).start();

                new Thread(() -> {
                    try {
                        Thread.sleep(1500);
                        Platform.runLater(this::handleBackToLogin);
                    } catch (InterruptedException ignored) {}
                }).start();
            } else {
                statusLabel.setText("❌ Email déjà utilisé. Veuillez en choisir un autre.");
            }
        } catch (Exception e) {
            statusLabel.setText("❌ Erreur lors de la création du compte.");
            e.printStackTrace();
        }
    }

    private boolean validerChamps() {
        String nom = nomField.getText().trim();
        String prenom = prenomField.getText().trim();
        String email = emailField.getText().trim();
        String tel = telField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || tel.isEmpty() || password.isEmpty()) {
            statusLabel.setText("⚠️ Tous les champs sont obligatoires.");
            return false;
        }
        if (ValidationUtils.isInvalidName(nom)) {
            statusLabel.setText("⚠️ Nom invalide (lettres uniquement, min 2 caractères).");
            return false;
        }
        if (ValidationUtils.isInvalidName(prenom)) {
            statusLabel.setText("⚠️ Prénom invalide (lettres uniquement, min 2 caractères).");
            return false;
        }
        if (ValidationUtils.isInvalidEmail(email)) {
            statusLabel.setText("⚠️ Format email incorrect.");
            return false;
        }
        if (ValidationUtils.isInvalidTel(tel)) {
            statusLabel.setText("⚠️ Le téléphone doit contenir exactement 8 chiffres.");
            return false;
        }
        if (ValidationUtils.isInvalidPassword(password)) {
            String weakness = ValidationUtils.getPasswordWeakness(password);
            statusLabel.setText("⚠️ " + (weakness != null ? weakness : "Mot de passe invalide."));
            return false;
        }
        if (!password.equals(confirmPassword)) {
            statusLabel.setText("⚠️ Les mots de passe ne correspondent pas.");
            return false;
        }
        return true;
    }

    @FXML
    private void handleBackToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/org/example/LoginView.fxml"));
            Stage stage = (Stage) nomField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("TalentFlow - Connexion");
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}