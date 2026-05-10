package org.example.GUI;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import org.example.model.User;
import org.example.utils.AvatarUtils;
import org.example.utils.LanguageManager;

public class WelcomeController {

    @FXML private Label lblWelcome;
    @FXML private Label lblUserName;
    @FXML private Label lblRole;
    @FXML private Label lblTip;
    @FXML private StackPane avatarContainer;

    @FXML
    public void initialize() {
        User user = UserSession.getInstance();
        if (user == null) return;

        String prenom = user.getPrenom();
        String nom = user.getNom();
        String role = user.getRole().toUpperCase();

        lblWelcome.setText(LanguageManager.get("welcome.title"));
        lblUserName.setText(prenom + " " + nom);

        // Avatar dans la section hero
        if (avatarContainer != null) {
            StackPane avatar = AvatarUtils.createAvatar(prenom + " " + nom, 40);
            avatarContainer.getChildren().add(avatar);
        }

        String roleLabel = switch (role) {
            case "ADMIN" -> LanguageManager.get("welcome.role.admin");
            case "RH" -> LanguageManager.get("welcome.role.rh");
            case "CANDIDAT" -> LanguageManager.get("welcome.role.candidat");
            default -> role;
        };
        lblRole.setText(LanguageManager.get("welcome.role.prefix") + " " + roleLabel);

        // Astuce contextuelle selon le rôle
        String tip = switch (role) {
            case "ADMIN" -> LanguageManager.get("welcome.tip.admin");
            case "RH" -> LanguageManager.get("welcome.tip.rh");
            case "CANDIDAT" -> LanguageManager.get("welcome.tip.candidat");
            default -> LanguageManager.get("welcome.tip.default");
        };
        lblTip.setText(tip);
    }
}
