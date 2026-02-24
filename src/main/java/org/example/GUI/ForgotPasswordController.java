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
            showError("Veuillez entrer votre adresse email.");
            return;
        }

        if (ValidationUtils.isInvalidEmail(email)) {
            showError("Format d'email invalide.");
            return;
        }

        // Vérifier si l'email existe
        User user = userDAO.findByEmail(email);
        if (user == null) {
            showError("Aucun compte associé à cet email.");
            return;
        }

        // Désactiver le bouton pendant l'envoi
        sendBtn.setDisable(true);
        statusLabel.setStyle("-fx-text-fill: #636e72;");
        statusLabel.setText("Envoi du code en cours...");

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
                    statusLabel.setStyle("-fx-text-fill: #e67e22;");
                    statusLabel.setText("⚠️ Email non configuré. Code : " + code + " — Cliquez pour continuer.");

                    // Permettre de continuer même sans email configuré
                    new Thread(() -> {
                        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                        Platform.runLater(this::loadResetPasswordView);
                    }).start();
                }
            });
        }).start();
    }

    @FXML
    private void handleBackToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/org/example/LoginView.fxml"));
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("TalentFlow - Connexion");
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadResetPasswordView() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/org/example/ResetPasswordView.fxml"));
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("TalentFlow - Nouveau mot de passe");
            stage.centerOnScreen();
        } catch (IOException e) {
            showError("Erreur de chargement de la page.");
            e.printStackTrace();
        }
    }

    private void showError(String msg) {
        statusLabel.setStyle("-fx-text-fill: #d63031;");
        statusLabel.setText(msg);
    }
}
