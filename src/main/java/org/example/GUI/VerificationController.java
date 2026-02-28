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
import org.example.model.User;
import org.example.utils.EmailService;
import org.example.utils.SmsService;
import org.example.utils.VerificationService;

import java.io.IOException;

/**
 * Contrôleur de la vérification 2FA.
 * Affiche un écran de saisie du code de vérification envoyé par email/SMS.
 */
public class VerificationController {

    @FXML private TextField codeField;
    @FXML private Label statusLabel;
    @FXML private Label infoLabel;
    @FXML private Button verifyBtn;
    @FXML private Button resendBtn;

    private int resendCooldown = 0;

    @FXML
    public void initialize() {
        User pending = UserSession.getPendingUser();
        if (pending != null && infoLabel != null) {
            String maskedEmail = maskEmail(pending.getEmail());
            infoLabel.setText("Un code a été envoyé à " + maskedEmail);
        }
    }

    @FXML
    private void handleVerify() {
        String code = codeField.getText().trim();

        if (code.isEmpty()) {
            showError("Veuillez entrer le code de vérification.");
            return;
        }

        if (code.length() != 6 || !code.matches("\\d{6}")) {
            showError("Le code doit contenir exactement 6 chiffres.");
            return;
        }

        User pending = UserSession.getPendingUser();
        if (pending == null) {
            showError("Session expirée. Veuillez vous reconnecter.");
            return;
        }

        if (VerificationService.verifyCode(pending.getEmail(), code)) {
            // Code correct → Activer la session et aller au dashboard
            UserSession.setInstance(pending);
            UserSession.clearPendingUser();
            loadDashboard();
        } else {
            showError("Code incorrect ou expiré. Veuillez réessayer.");
        }
    }

    @FXML
    private void handleResendCode() {
        User pending = UserSession.getPendingUser();
        if (pending == null) {
            showError("Session expirée. Veuillez vous reconnecter.");
            return;
        }

        // Cooldown pour éviter le spam
        if (resendCooldown > 0) {
            showError("Veuillez patienter avant de renvoyer un code.");
            return;
        }

        // Générer un nouveau code
        String newCode = VerificationService.generateCode(pending.getEmail());

        // Envoyer par email
        new Thread(() -> {
            boolean sent = EmailService.sendVerificationCode(pending.getEmail(), newCode);

            // Aussi par SMS si Twilio est configuré
            if (SmsService.isConfigured() && pending.getTelephone() != null && !pending.getTelephone().isEmpty()) {
                String formattedPhone = SmsService.formatPhoneNumber(pending.getTelephone());
                SmsService.sendVerificationCode(formattedPhone, newCode);
            }

            Platform.runLater(() -> {
                if (sent) {
                    showSuccess("✅ Nouveau code envoyé à votre adresse email !");
                } else {
                    showError("❌ Échec de l'envoi du code. Vérifiez votre connexion et réessayez.");
                }
            });
        }).start();

        // Démarrer le cooldown (30 secondes)
        startResendCooldown();
    }

    @FXML
    private void handleBackToLogin() {
        UserSession.cleanUserSession();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/org/example/LoginView.fxml"));
            Stage stage = (Stage) codeField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("TalentFlow - Connexion");
            stage.setMaximized(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadDashboard() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/org/example/MainDashboard.fxml"));
            Stage stage = (Stage) codeField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("TalentFlow - Dashboard");
            stage.setMinWidth(1200);
            stage.setMinHeight(750);
            stage.setMaximized(true);
        } catch (IOException e) {
            System.err.println("Erreur chargement Dashboard : " + e.getMessage());
        }
    }

    private void startResendCooldown() {
        resendCooldown = 30;
        resendBtn.setDisable(true);

        new Thread(() -> {
            while (resendCooldown > 0) {
                int remaining = resendCooldown;
                Platform.runLater(() -> resendBtn.setText("Renvoyer (" + remaining + "s)"));
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                resendCooldown--;
            }
            Platform.runLater(() -> {
                resendBtn.setDisable(false);
                resendBtn.setText("Renvoyer le code");
            });
        }).start();
    }

    private String maskEmail(String email) {
        if (email == null) return "";
        int at = email.indexOf('@');
        if (at <= 2) return email;
        return email.charAt(0) + "***" + email.substring(at - 1);
    }

    private void showError(String msg) {
        statusLabel.setStyle("-fx-text-fill: #d63031;");
        statusLabel.setText(msg);
    }

    private void showSuccess(String msg) {
        statusLabel.setStyle("-fx-text-fill: #00b894;");
        statusLabel.setText(msg);
    }
}
