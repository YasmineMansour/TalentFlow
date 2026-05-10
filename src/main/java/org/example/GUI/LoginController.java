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
import org.example.utils.GoogleAuthService;
import org.example.utils.SmsService;
import org.example.utils.ValidationUtils;
import org.example.utils.VerificationService;
import org.example.utils.LanguageManager;
import org.example.utils.ThemeManager;

import java.io.IOException;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisible;
    @FXML private Button togglePasswordBtn;
    @FXML private Label errorLabel;

    private UserDAO userDAO = new UserDAO();
    private boolean passwordShown = false;

    @FXML
    public void initialize() {
        // Synchroniser les deux champs de mot de passe
        passwordVisible.textProperty().bindBidirectional(passwordField.textProperty());

        // Appliquer le thème sauvegardé
        Platform.runLater(() -> ThemeManager.applyTheme(emailField.getScene()));
    }

    @FXML
    private void togglePassword() {
        passwordShown = !passwordShown;
        passwordField.setVisible(!passwordShown);
        passwordVisible.setVisible(passwordShown);
        togglePasswordBtn.setText(passwordShown ? "🙈" : "👁");
    }

    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            errorLabel.setText(LanguageManager.get("login.error.empty"));
            return;
        }

        if (ValidationUtils.isInvalidEmail(email)) {
            errorLabel.setText(LanguageManager.get("login.error.email"));
            return;
        }

        try {
            User user = userDAO.login(email, password);
            if (user != null) {
                // Vérifier si le 2FA est activé
                if (VerificationService.TWO_FA_ENABLED) {
                    // Stocker l'utilisateur en attente de vérification
                    UserSession.setPendingUser(user);

                    // Générer un code de vérification
                    String code = VerificationService.generateCode(user.getEmail());

                    // Désactiver le bouton pendant l'envoi
                    errorLabel.setStyle("-fx-text-fill: #636e72;");
                    errorLabel.setText(LanguageManager.get("login.sending.code"));

                    // Capturer la référence du Stage AVANT le thread (sinon getScene() peut devenir null)
                    Stage currentStage = (Stage) emailField.getScene().getWindow();

                    // Envoyer le code en arrière-plan
                    new Thread(() -> {
                        boolean emailSent = EmailService.sendVerificationCode(user.getEmail(), code);

                        // Aussi par SMS si Twilio configuré
                        if (SmsService.isConfigured() && user.getTelephone() != null && !user.getTelephone().isEmpty()) {
                            String formattedPhone = SmsService.formatPhoneNumber(user.getTelephone());
                            SmsService.sendVerificationCode(formattedPhone, code);
                        }

                        Platform.runLater(() -> {
                            if (!emailSent) {
                                System.err.println("⚠️ Échec de l'envoi du code par email à : " + user.getEmail());
                            }
                            loadVerification(currentStage);
                        });
                    }).start();
                } else {
                    // 2FA désactivé → aller directement au dashboard
                    UserSession.setInstance(user);
                    loadDashboard();
                }
            } else {
                errorLabel.setStyle("-fx-text-fill: #d63031;");
                errorLabel.setText(LanguageManager.get("login.error.invalid"));
            }
        } catch (Exception e) {
            errorLabel.setStyle("-fx-text-fill: #d63031;");
            errorLabel.setText(LanguageManager.get("login.error.server"));
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGoogleLogin() {
        if (!GoogleAuthService.isConfigured()) {
            errorLabel.setStyle("-fx-text-fill: #d63031;");
            errorLabel.setText(LanguageManager.get("login.google.notconfigured"));
            return;
        }

        errorLabel.setStyle("-fx-text-fill: #636e72;");
        errorLabel.setText(LanguageManager.get("login.google.opening"));

        GoogleAuthService.authenticate().thenAccept(userInfo -> {
            Platform.runLater(() -> {
                try {
                    // Trouver ou créer l'utilisateur avec les infos Google
                    User user = userDAO.findOrCreateGoogleUser(
                            userInfo.getEmail(),
                            userInfo.getFamilyName(),
                            userInfo.getGivenName()
                    );

                    if (user != null) {
                        // Envoyer un email de bienvenue si nouvel utilisateur
                        if (user.getCreatedAt() != null &&
                                java.time.Duration.between(user.getCreatedAt(), java.time.LocalDateTime.now()).getSeconds() < 5) {
                            String email = user.getEmail();
                            String prenom = user.getPrenom();
                            new Thread(() -> EmailService.sendWelcomeEmail(email, prenom)).start();
                        }

                        // Connexion directe (pas de 2FA pour Google)
                        UserSession.setInstance(user);
                        errorLabel.setStyle("-fx-text-fill: #00b894;");
                        errorLabel.setText(LanguageManager.get("login.google.success"));
                        loadDashboard();
                    } else {
                        errorLabel.setStyle("-fx-text-fill: #d63031;");
                        errorLabel.setText(LanguageManager.get("login.google.error"));
                    }
                } catch (Exception e) {
                    errorLabel.setStyle("-fx-text-fill: #d63031;");
                    errorLabel.setText(LanguageManager.get("login.google.error"));
                    e.printStackTrace();
                }
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                errorLabel.setStyle("-fx-text-fill: #d63031;");
                errorLabel.setText(LanguageManager.get("login.google.cancel"));
            });
            return null;
        });
    }

    @FXML
    private void handleShowRegister() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/org/example/RegisterView.fxml"));
            Stage stage = (Stage) emailField.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("TalentFlow - " + LanguageManager.get("register.title"));
            ThemeManager.applyTheme(scene);
            stage.setMaximized(true);
        } catch (IOException e) {
            errorLabel.setText(LanguageManager.get("common.error"));
            e.printStackTrace();
        }
    }

    @FXML
    private void handleForgotPassword() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/org/example/ForgotPasswordView.fxml"));
            Stage stage = (Stage) emailField.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("TalentFlow - " + LanguageManager.get("forgot.title"));
            ThemeManager.applyTheme(scene);
            stage.setMaximized(true);
        } catch (IOException e) {
            errorLabel.setText(LanguageManager.get("common.error"));
            e.printStackTrace();
        }
    }

    private void loadVerification(Stage stage) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/org/example/VerificationView.fxml"));
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("TalentFlow - " + LanguageManager.get("verify.title"));
            ThemeManager.applyTheme(scene);
            stage.setMaximized(true);
        } catch (IOException e) {
            System.err.println("Erreur chargement Verification FXML: " + e.getMessage());
        }
    }

    private void loadDashboard() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/org/example/MainDashboard.fxml"));
            Stage stage = (Stage) emailField.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("TalentFlow - Dashboard");
            ThemeManager.applyTheme(scene);
            stage.setMinWidth(1200);
            stage.setMinHeight(750);
            stage.setMaximized(true);
        } catch (IOException e) {
            System.err.println("Erreur chargement Dashboard FXML: " + e.getMessage());
        }
    }
}