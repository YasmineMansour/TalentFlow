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
import org.example.utils.SmsService;
import org.example.utils.ValidationUtils;
import org.example.utils.VerificationService;

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
            errorLabel.setText("Veuillez remplir tous les champs !");
            return;
        }

        if (ValidationUtils.isInvalidEmail(email)) {
            errorLabel.setText("Format d'email invalide !");
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
                    errorLabel.setText("Envoi du code de vérification...");

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
                                // Mode dev : afficher le code si email non configuré
                                System.out.println("🔑 [DEV] Code 2FA : " + code);
                            }
                            loadVerification();
                        });
                    }).start();
                } else {
                    // 2FA désactivé → aller directement au dashboard
                    UserSession.setInstance(user);
                    loadDashboard();
                }
            } else {
                errorLabel.setStyle("-fx-text-fill: #d63031;");
                errorLabel.setText("Email ou mot de passe incorrect !");
            }
        } catch (Exception e) {
            errorLabel.setStyle("-fx-text-fill: #d63031;");
            errorLabel.setText("Erreur de connexion au serveur.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleShowRegister() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/org/example/RegisterView.fxml"));
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("TalentFlow - Inscription");
            stage.centerOnScreen();
        } catch (IOException e) {
            errorLabel.setText("Erreur de chargement de la page d'inscription.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleForgotPassword() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/org/example/ForgotPasswordView.fxml"));
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("TalentFlow - Mot de passe oublié");
            stage.centerOnScreen();
        } catch (IOException e) {
            errorLabel.setText("Erreur de chargement de la page.");
            e.printStackTrace();
        }
    }

    private void loadVerification() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/org/example/VerificationView.fxml"));
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("TalentFlow - Vérification 2FA");
            stage.centerOnScreen();
        } catch (IOException e) {
            System.err.println("Erreur chargement Verification FXML: " + e.getMessage());
        }
    }

    private void loadDashboard() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/org/example/MainDashboard.fxml"));
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("TalentFlow - Dashboard");
            stage.setMinWidth(1200);
            stage.setMinHeight(750);
            stage.centerOnScreen();
        } catch (IOException e) {
            System.err.println("Erreur chargement Dashboard FXML: " + e.getMessage());
        }
    }
}