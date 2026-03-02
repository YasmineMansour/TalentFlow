package org.example.GUI;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.example.model.User;

public class WelcomeController {

    @FXML private Label lblWelcome;
    @FXML private Label lblUserName;
    @FXML private Label lblRole;
    @FXML private Label lblTip;

    @FXML
    public void initialize() {
        User user = UserSession.getInstance();
        if (user == null) return;

        String prenom = user.getPrenom();
        String nom = user.getNom();
        String role = user.getRole().toUpperCase();

        lblWelcome.setText("Bienvenue sur TalentFlow !");
        lblUserName.setText(prenom + " " + nom);

        String roleLabel = switch (role) {
            case "ADMIN" -> "Administrateur";
            case "RH" -> "Ressources Humaines";
            case "CANDIDAT" -> "Candidat";
            default -> role;
        };
        lblRole.setText("Connecté en tant que : " + roleLabel);

        // Astuce contextuelle selon le rôle
        String tip = switch (role) {
            case "ADMIN" -> "En tant qu'administrateur, vous avez un accès complet : gestion des utilisateurs et des droits d'accès, gestion des offres d'emploi, suivi des candidatures, planification des entretiens et prise de décisions.";
            case "RH" -> "En tant que RH, vous gérez les offres d'emploi, traitez les candidatures reçues, planifiez les entretiens avec les candidats, prenez les décisions finales et envoyez les résultats par email.";
            case "CANDIDAT" -> "En tant que candidat, consultez les offres d'emploi disponibles, postulez en joignant votre CV et lettre de motivation, puis suivez l'avancement de vos candidatures.";
            default -> "Utilisez le menu latéral pour naviguer entre les différents modules.";
        };
        lblTip.setText(tip);
    }
}
