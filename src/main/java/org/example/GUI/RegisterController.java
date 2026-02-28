package org.example.GUI;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.dao.UserDAO;
import org.example.model.FraudCheckResult;
import org.example.model.User;
import org.example.utils.EmailService;
import org.example.utils.FraudDetectionService;
import org.example.utils.GoogleAuthService;
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

        String nom = nomField.getText().trim();
        String prenom = prenomField.getText().trim();
        String email = emailField.getText().trim();
        String tel = telField.getText().trim();

        // 🛡️ Détection de fraude IA avant inscription
        FraudCheckResult fraudCheck = FraudDetectionService.analyzeUser(email, nom, prenom, tel);
        if (fraudCheck.isFlaggedForReview()) {
            statusLabel.setStyle("-fx-text-fill: #d63031;");
            statusLabel.setText("🛡️ Inscription bloquée : " + fraudCheck.getRiskLevel().getLabel()
                    + " — " + String.join(", ", fraudCheck.getFlags()));
            return;
        }
        // Avertissement pour risque moyen (mais on laisse passer)
        if (fraudCheck.getRiskLevel() == FraudCheckResult.RiskLevel.MOYEN) {
            System.out.println("⚠️ Inscription avec risque moyen — Flags: " + fraudCheck.getFlags());
        }

        User newUser = new User(
                0, nom, prenom, email,
                passwordField.getText(),
                "CANDIDAT", tel
        );

        try {
            boolean created = userDAO.create(newUser);
            if (created) {
                statusLabel.setStyle("-fx-text-fill: #00b894;");
                statusLabel.setText("✅ Compte créé avec succès !");

                // Envoyer un email de bienvenue en arrière-plan
                final String userEmail = newUser.getEmail();
                final String userPrenom = newUser.getPrenom();
                new Thread(() -> EmailService.sendWelcomeEmail(userEmail, userPrenom)).start();

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
        // 🛡️ Vérification rapide email jetable en temps réel
        String emailWarning = FraudDetectionService.quickEmailCheck(email);
        if (emailWarning != null) {
            statusLabel.setText(emailWarning);
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
    private void handleGoogleRegister() {
        if (!GoogleAuthService.isConfigured()) {
            statusLabel.setStyle("-fx-text-fill: #d63031;");
            statusLabel.setText("⚠️ Connexion Google non configurée. Configurez CLIENT_ID et CLIENT_SECRET.");
            return;
        }

        statusLabel.setStyle("-fx-text-fill: #636e72;");
        statusLabel.setText("Ouverture du navigateur pour l'inscription Google...");

        GoogleAuthService.authenticate().thenAccept(userInfo -> {
            Platform.runLater(() -> {
                try {
                    // Vérifier si l'utilisateur existe déjà
                    User existingUser = userDAO.findByEmail(userInfo.getEmail());
                    if (existingUser != null) {
                        statusLabel.setStyle("-fx-text-fill: #fdcb6e;");
                        statusLabel.setText("ℹ️ Ce compte Google existe déjà. Redirection vers la connexion...");
                        new Thread(() -> {
                            try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                            Platform.runLater(this::handleBackToLogin);
                        }).start();
                        return;
                    }

                    // Créer le nouvel utilisateur
                    User user = userDAO.findOrCreateGoogleUser(
                            userInfo.getEmail(),
                            userInfo.getFamilyName(),
                            userInfo.getGivenName()
                    );

                    if (user != null) {
                        // Envoyer un email de bienvenue
                        String email = user.getEmail();
                        String prenom = user.getPrenom();
                        new Thread(() -> EmailService.sendWelcomeEmail(email, prenom)).start();

                        statusLabel.setStyle("-fx-text-fill: #00b894;");
                        statusLabel.setText("✅ Compte créé avec Google ! Redirection...");

                        new Thread(() -> {
                            try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                            Platform.runLater(this::handleBackToLogin);
                        }).start();
                    } else {
                        statusLabel.setStyle("-fx-text-fill: #d63031;");
                        statusLabel.setText("❌ Erreur lors de la création du compte Google.");
                    }
                } catch (Exception e) {
                    statusLabel.setStyle("-fx-text-fill: #d63031;");
                    statusLabel.setText("❌ Erreur lors de l'inscription Google.");
                    e.printStackTrace();
                }
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                statusLabel.setStyle("-fx-text-fill: #d63031;");
                statusLabel.setText("❌ Inscription Google annulée ou échouée.");
            });
            return null;
        });
    }

    @FXML
    private void handleBackToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/org/example/LoginView.fxml"));
            Stage stage = (Stage) nomField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("TalentFlow - Connexion");
            stage.setMaximized(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}