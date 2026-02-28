package org.example.GUI;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.example.dao.UserDAO;
import org.example.utils.ValidationUtils;
import org.example.utils.VerificationService;

import java.io.IOException;

/**
 * Contrôleur de la page de réinitialisation du mot de passe.
 * L'utilisateur entre le code reçu par email + son nouveau mot de passe.
 */
public class ResetPasswordController {

    @FXML private TextField codeField;
    @FXML private PasswordField newPasswordField;
    @FXML private TextField newPasswordVisible;
    @FXML private Button togglePasswordBtn;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField confirmPasswordVisible;
    @FXML private Button toggleConfirmBtn;
    @FXML private Label statusLabel;
    @FXML private Label infoLabel;

    private UserDAO userDAO = new UserDAO();
    private boolean passwordShown = false;
    private boolean confirmShown = false;

    @FXML
    public void initialize() {
        // Synchroniser les champs de mot de passe
        if (newPasswordVisible != null) {
            newPasswordVisible.textProperty().bindBidirectional(newPasswordField.textProperty());
        }
        if (confirmPasswordVisible != null) {
            confirmPasswordVisible.textProperty().bindBidirectional(confirmPasswordField.textProperty());
        }

        // Afficher l'email masqué
        String email = UserSession.getPendingEmail();
        if (email != null && infoLabel != null) {
            infoLabel.setText("Code envoyé à " + maskEmail(email));
        }
    }

    @FXML
    private void togglePassword() {
        passwordShown = !passwordShown;
        newPasswordField.setVisible(!passwordShown);
        newPasswordVisible.setVisible(passwordShown);
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
    private void handleResetPassword() {
        String code = codeField.getText().trim();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String email = UserSession.getPendingEmail();

        // Validations
        if (email == null || email.isEmpty()) {
            showError("Session expirée. Veuillez recommencer.");
            return;
        }

        if (code.isEmpty()) {
            showError("Veuillez entrer le code de vérification.");
            return;
        }

        if (code.length() != 6 || !code.matches("\\d{6}")) {
            showError("Le code doit contenir 6 chiffres.");
            return;
        }

        if (newPassword.isEmpty()) {
            showError("Veuillez entrer un nouveau mot de passe.");
            return;
        }

        if (ValidationUtils.isInvalidPassword(newPassword)) {
            String weakness = ValidationUtils.getPasswordWeakness(newPassword);
            showError(weakness != null ? weakness : "Mot de passe invalide.");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showError("Les mots de passe ne correspondent pas.");
            return;
        }

        // Vérifier le code
        if (!VerificationService.verifyCode(email, code)) {
            showError("Code incorrect ou expiré.");
            return;
        }

        // Réinitialiser le mot de passe
        boolean success = userDAO.updatePassword(email, newPassword);
        if (success) {
            UserSession.clearPendingEmail();
            statusLabel.setStyle("-fx-text-fill: #00b894;");
            statusLabel.setText("✅ Mot de passe réinitialisé avec succès !");

            // Rediriger vers la page de connexion après 2 secondes
            new Thread(() -> {
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                Platform.runLater(this::handleBackToLogin);
            }).start();
        } else {
            showError("Erreur lors de la réinitialisation. Veuillez réessayer.");
        }
    }

    @FXML
    private void handleBackToLogin() {
        UserSession.clearPendingEmail();
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
}
