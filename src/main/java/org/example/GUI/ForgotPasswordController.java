package org.example.GUI;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.dao.UserDAO;
import org.example.model.User;
import org.example.utils.EmailService;
import org.example.utils.ValidationUtils;
import org.example.utils.VerificationService;
import org.example.utils.LanguageManager;
import org.example.utils.ThemeManager;

import java.io.IOException;

/**
 * Contrôleur de la page "Mot de passe oublié".
 * Permet à l'utilisateur de demander un code de réinitialisation par email.
 */
public class ForgotPasswordController {

    @FXML private TextField emailField;
    @FXML private Label statusLabel;
    @FXML private Button sendBtn;

    private UserDAO userDAO = new UserDAO();

    @FXML
    private void handleSendCode() {
        String email = emailField.getText().trim();

        if (email.isEmpty()) {
            showError(LanguageManager.get("forgot.error.empty"));
            return;
        }

        if (ValidationUtils.isInvalidEmail(email)) {
            showError(LanguageManager.get("forgot.error.email"));
            return;
        }

        // Vérifier si l'email existe
        User user = userDAO.findByEmail(email);
        if (user == null) {
            showError(LanguageManager.get("forgot.error.notfound"));
            return;
        }

        // Désactiver le bouton pendant l'envoi
        sendBtn.setDisable(true);
        statusLabel.setStyle("-fx-text-fill: #636e72;");
        statusLabel.setText(LanguageManager.get("forgot.sending"));

        // Générer et envoyer le code
        String code = VerificationService.generateCode(email);

        new Thread(() -> {
            boolean sent = EmailService.sendPasswordResetCode(email, code);
            Platform.runLater(() -> {
                sendBtn.setDisable(false);
                if (sent) {
                    // Stocker l'email pour la page suivante
                    UserSession.setPendingEmail(email);
                    loadResetPasswordView();
                } else {
                    // Mode dev : afficher le code si email non configuré
                    UserSession.setPendingEmail(email);
                    showError(LanguageManager.get("forgot.error.send"));
                }
            });
        }).start();
    }

    @FXML
    private void handleBackToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/org/example/LoginView.fxml"));
            Stage stage = (Stage) emailField.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("TalentFlow - " + LanguageManager.get("login.title"));
            ThemeManager.applyTheme(scene);
            stage.setMaximized(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadResetPasswordView() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/org/example/ResetPasswordView.fxml"));
            Stage stage = (Stage) emailField.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("TalentFlow - " + LanguageManager.get("forgot.reset.title"));
            ThemeManager.applyTheme(scene);
            stage.setMaximized(true);
        } catch (IOException e) {
            showError(LanguageManager.get("forgot.error.load"));
            e.printStackTrace();
        }
    }

    private void showError(String msg) {
        statusLabel.setStyle("-fx-text-fill: #d63031;");
        statusLabel.setText(msg);
    }
}
