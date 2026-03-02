package org.example.GUI;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.Node;
import javafx.stage.Stage;
import org.example.model.User;

import java.io.IOException;
import java.net.URL;

public class MainDashboardController {

    @FXML private StackPane contentArea;
    @FXML private Label welcomeLabel;

    // ── Dashboard (standalone) ──
    @FXML private Button btnDashboard;

    // ── Sections (VBox wrappers for hide/show) ──
    @FXML private VBox sectionUsers;
    @FXML private VBox sectionOffres;
    @FXML private VBox sectionCandidatures;
    @FXML private VBox sectionEntretiens;
    @FXML private VBox sectionForum;

    // ── Section headers ──
    @FXML private Button btnUsersHeader;
    @FXML private Button btnOffresHeader;
    @FXML private Button btnCandidaturesHeader;
    @FXML private Button btnEntretiensHeader;
    @FXML private Button btnForumHeader;

    // ── Sub-menu containers ──
    @FXML private VBox subMenuUsers;
    @FXML private VBox subMenuOffres;
    @FXML private VBox subMenuCandidatures;
    @FXML private VBox subMenuEntretiens;
    @FXML private VBox subMenuForum;

    // ── Sub-menu buttons ──
    @FXML private Button btnUsers;
    @FXML private Button btnUsersDashboard;
    @FXML private Button btnOffres;
    @FXML private Button btnOffresConsult;
    @FXML private Button btnOffresStats;
    @FXML private Button btnMap;
    @FXML private Button btnOffresDisponibles;
    @FXML private Button btnCandidatures;
    @FXML private Button btnPiecesJointes;
    @FXML private Button btnEntretiens;
    @FXML private Button btnDecisions;
    @FXML private Button btnStatsEntretiens;
    @FXML private Button btnForumFeed;
    @FXML private Button btnForumMessages;
    @FXML private Button btnForumAI;

    // ── Active state tracking ──
    private Button activeButton;
    private Button activeSectionHeader;

    @FXML
    public void initialize() {
        User currentUser = UserSession.getInstance();

        if (currentUser == null) {
            System.err.println("Accès refusé : Aucune session active.");
            return;
        }

        if (welcomeLabel != null) {
            welcomeLabel.setText(currentUser.getPrenom() + " " + currentUser.getNom()
                    + "  •  " + currentUser.getRole().toUpperCase());
        }

        applySecurityRestrictions(currentUser.getRole().toUpperCase());

        // Charger la page d'accueil par défaut
        loadView("/org/example/WelcomeView.fxml");
    }

    private void applySecurityRestrictions(String role) {
        // ── CANDIDAT : Offres (lecture) + Candidatures. Pas d'Utilisateurs ni Entretiens ──
        if (role.equalsIgnoreCase("CANDIDAT")) {
            hideNode(sectionUsers);
            hideNode(sectionEntretiens);

            // Offres : seulement consultation (pas de CRUD ni stats)
            hideNode(btnOffres);        // "Gestion des offres" → réservé RH/Admin
            hideNode(btnOffresStats);   // Statistiques → réservé RH/Admin
            // Garde : btnOffresConsult (Consulter les offres) + btnMap (Carte)

            // Candidatures : vue candidat
            if (btnCandidatures != null) btnCandidatures.setText("Mes candidatures");
        }

        // ── RH : Offres + Candidatures + Entretiens. Pas d'Utilisateurs ──
        else if (role.equalsIgnoreCase("RH")) {
            hideNode(sectionUsers);
            hideNode(btnOffresConsult); // RH utilise "Gestion des offres"

            // Candidatures : vue RH (pas "Offres disponibles" qui est pour candidats)
            hideNode(btnOffresDisponibles);
            if (btnCandidatures != null) btnCandidatures.setText("Gestion des candidatures");
        }

        // ── ADMIN : tout visible ──
        else if (role.equalsIgnoreCase("ADMIN")) {
            hideNode(btnOffresConsult); // Admin utilise "Gestion des offres"
            // Candidatures : vue RH + tout
            hideNode(btnOffresDisponibles); // Les offres sont déjà dans la section Offres
            if (btnCandidatures != null) btnCandidatures.setText("Gestion des candidatures");
        }
    }

    private void hideNode(javafx.scene.Node node) {
        if (node != null) {
            node.setVisible(false);
            node.setManaged(false);
        }
    }

    // ═══════════════════════════════════════════
    //  Toggle sub-menus (accordion behavior)
    // ═══════════════════════════════════════════

    @FXML
    private void toggleUsers() {
        toggleSubMenu(subMenuUsers, btnUsersHeader);
    }

    @FXML
    private void toggleOffres() {
        toggleSubMenu(subMenuOffres, btnOffresHeader);
    }

    @FXML
    private void toggleCandidatures() {
        toggleSubMenu(subMenuCandidatures, btnCandidaturesHeader);
    }

    @FXML
    private void toggleEntretiens() {
        toggleSubMenu(subMenuEntretiens, btnEntretiensHeader);
    }

    @FXML
    private void toggleForum() {
        toggleSubMenu(subMenuForum, btnForumHeader);
    }

    private void toggleSubMenu(VBox subMenu, Button header) {
        boolean isVisible = subMenu.isVisible();
        subMenu.setVisible(!isVisible);
        subMenu.setManaged(!isVisible);

        // Swap arrow indicator ▸ ↔ ▾
        String text = header.getText();
        if (isVisible) {
            header.setText(text.replace("▾", "▸"));
        } else {
            header.setText(text.replace("▸", "▾"));
        }
    }

    // ═══════════════════════════════════════════
    //  Navigation methods
    // ═══════════════════════════════════════════

    @FXML
    private void showDashboard() {
        setActiveButton(btnDashboard, null);
        loadView("/org/example/DashboardHome.fxml");
    }

    @FXML
    private void showUsers() {
        setActiveButton(btnUsers, btnUsersHeader);
        loadView("/org/example/UserView.fxml");
    }

    @FXML
    private void showDashboardFromUsers() {
        setActiveButton(btnUsersDashboard, btnUsersHeader);
        loadView("/org/example/DashboardHome.fxml");
    }

    @FXML
    private void showOffres() {
        setActiveButton(btnOffres, btnOffresHeader);
        loadView("/org/example/OffresView.fxml");
    }

    @FXML
    private void showOffresConsult() {
        setActiveButton(btnOffresConsult, btnOffresHeader);
        loadView("/org/example/OffresView.fxml");
    }

    @FXML
    private void showOffreStatistiques() {
        setActiveButton(btnOffresStats, btnOffresHeader);
        loadView("/org/example/OffreStatistiquesView.fxml");
    }

    @FXML
    private void showMap() {
        setActiveButton(btnMap, btnOffresHeader);
        loadView("/org/example/MapView.fxml");
    }

    @FXML
    private void showOffresDisponibles() {
        setActiveButton(btnOffresDisponibles, btnCandidaturesHeader);
        loadView("/org/example/OffresView.fxml");
    }

    @FXML
    private void showCandidatures() {
        setActiveButton(btnCandidatures, btnCandidaturesHeader);
        User currentUser = UserSession.getInstance();
        if (currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.getRole())) {
            loadView("/org/example/RhCandidatureView.fxml");
        } else {
            loadView("/org/example/CandidatCandidatureView.fxml");
        }
    }

    @FXML
    private void showPiecesJointes() {
        setActiveButton(btnPiecesJointes, btnCandidaturesHeader);
        loadView("/org/example/PieceJointeView.fxml");
    }

    @FXML
    private void showEntretiens() {
        setActiveButton(btnEntretiens, btnEntretiensHeader);
        loadView("/org/example/EntretienView.fxml");
    }

    @FXML
    private void showDecisions() {
        setActiveButton(btnDecisions, btnEntretiensHeader);
        loadView("/org/example/DecisionView.fxml");
    }

    @FXML
    private void showEntretienDashboard() {
        setActiveButton(btnStatsEntretiens, btnEntretiensHeader);
        loadView("/org/example/EntretienDashboardView.fxml");
    }

    @FXML
    private void showForumFeed() {
        setActiveButton(btnForumFeed, btnForumHeader);
        loadView("/org/example/ForumFeed.fxml");
    }

    @FXML
    private void showForumMessages() {
        setActiveButton(btnForumMessages, btnForumHeader);
        loadView("/org/example/ForumMessaging.fxml");
    }

    @FXML
    private void showForumAI() {
        setActiveButton(btnForumAI, btnForumHeader);
        loadView("/org/example/ForumAIChat.fxml");
    }

    // ═══════════════════════════════════════════
    //  Active state management
    // ═══════════════════════════════════════════

    private void setActiveButton(Button newActive, Button sectionHeader) {
        // Remove active from previous button
        if (activeButton != null) {
            activeButton.getStyleClass().remove("active");
        }
        // Remove active from previous section header
        if (activeSectionHeader != null) {
            activeSectionHeader.getStyleClass().remove("active");
        }

        // Set new active
        if (newActive != null && !newActive.getStyleClass().contains("active")) {
            newActive.getStyleClass().add("active");
        }
        if (sectionHeader != null && !sectionHeader.getStyleClass().contains("active")) {
            sectionHeader.getStyleClass().add("active");
        }

        activeButton = newActive;
        activeSectionHeader = sectionHeader;
    }

    // ═══════════════════════════════════════════
    //  View loader
    // ═══════════════════════════════════════════

    private void loadView(String fxmlPath) {
        try {
            URL resource = getClass().getResource(fxmlPath);

            if (resource == null) {
                showErrorAlert("Module non disponible",
                        "La vue " + fxmlPath.substring(fxmlPath.lastIndexOf('/') + 1)
                        + " n'est pas encore implémentée.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Parent view = loader.load();

            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);

            // Forcer la vue chargée à remplir tout l'espace disponible
            if (view instanceof Node) {
                StackPane.setAlignment(view, javafx.geometry.Pos.TOP_LEFT);
            }
            if (view instanceof javafx.scene.layout.Region region) {
                region.prefWidthProperty().bind(contentArea.widthProperty());
                region.prefHeightProperty().bind(contentArea.heightProperty());
            }

        } catch (IOException e) {
            showErrorAlert("Erreur de navigation", "Impossible de charger la page : " + fxmlPath);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        UserSession.cleanUserSession();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/org/example/LoginView.fxml"));
            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("TalentFlow - Connexion");
            stage.centerOnScreen();
        } catch (IOException e) {
            System.err.println("Erreur lors de la déconnexion : " + e.getMessage());
            System.exit(0);
        }
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}