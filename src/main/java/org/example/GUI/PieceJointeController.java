package org.example.GUI;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import org.example.model.Candidature;
import org.example.model.PieceJointe;
import org.example.services.CandidatureService;
import org.example.services.PieceJointeService;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Contrôleur de gestion des pièces jointes d'une candidature.
 */
public class PieceJointeController {

    @FXML private ListView<Candidature> lvCandidatures;
    @FXML private TextField tfTitre, tfUrl;
    @FXML private ComboBox<String> cbTypeDoc;
    @FXML private TableView<PieceJointe> tvPieces;
    @FXML private TableColumn<PieceJointe, String> colTitre, colType, colUrl;
    @FXML private Label lblMsg;

    private final PieceJointeService pjService = new PieceJointeService();
    private final CandidatureService candService = new CandidatureService();
    private int selectedCandidatureId = -1;

    @FXML
    public void initialize() {
        cbTypeDoc.setItems(FXCollections.observableArrayList("CV", "LM", "DIPLOME", "CERTIF", "AUTRE"));
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colType.setCellValueFactory(new PropertyValueFactory<>("typeDoc"));
        colUrl.setCellValueFactory(new PropertyValueFactory<>("url"));
        loadCandidatures();

        lvCandidatures.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                selectCandidature(newVal.getId());
            }
        });

        tvPieces.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (newVal != null) {
                tfTitre.setText(newVal.getTitre());
                cbTypeDoc.setValue(newVal.getTypeDoc());
                tfUrl.setText(newVal.getUrl());
            }
        });

        tfUrl.textProperty().addListener((observable, oldValue, newValue) -> {
            tfUrl.setStyle("");
        });
    }

    /** Pré-sélectionne une candidature (appelé depuis CandidatCandidatureController) */
    public void setCandidatureId(int id) {
        selectCandidature(id);
        for (Candidature c : lvCandidatures.getItems()) {
            if (c.getId() == id) {
                lvCandidatures.getSelectionModel().select(c);
                break;
            }
        }
    }

    private void selectCandidature(int id) {
        this.selectedCandidatureId = id;
        lblMsg.setText("📂 Dossier : Candidature n°" + id);
        lblMsg.setStyle("-fx-text-fill: #6c5ce7; -fx-font-weight: bold;");
        refreshTable();
    }

    private void loadCandidatures() {
        try {
            lvCandidatures.setItems(FXCollections.observableArrayList(candService.getAll()));
        } catch (SQLException e) {
            showError("❌ Erreur chargement candidatures.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAjouter() {
        if (selectedCandidatureId == -1) {
            showError("⚠️ Sélectionnez une candidature à gauche !");
            return;
        }

        String type = cbTypeDoc.getValue();
        String titre = tfTitre.getText();
        String url = tfUrl.getText().trim();

        if (type == null || titre.isEmpty() || url.isEmpty()) {
            showError("⚠️ Tous les champs sont obligatoires.");
            return;
        }

        // Validation URL
        String urlPattern = "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]"
                + "|^uploads/[a-zA-Z0-9._-]+";
        if (!url.matches(urlPattern)) {
            showError("❌ Format d'URL invalide (ex: https://... ou uploads/fichier.pdf)");
            tfUrl.setStyle("-fx-border-color: #e74c3c; -fx-border-width: 2px;");
            return;
        } else {
            tfUrl.setStyle("");
        }

        // Vérifier unicité CV/LM
        if (type.equals("CV") || type.equals("LM")) {
            for (PieceJointe existing : tvPieces.getItems()) {
                if (existing.getTypeDoc().equals(type)) {
                    showError("❌ Cette candidature possède déjà un(e) " + type);
                    return;
                }
            }
        }

        try {
            PieceJointe p = new PieceJointe(selectedCandidatureId, titre, type, url);
            pjService.ajouter(p);
            showSuccess("✅ " + type + " ajouté avec succès !");
            refreshTable();
            clearForm();
        } catch (SQLException e) {
            showError("❌ Erreur BDD : " + e.getMessage());
        }
    }

    @FXML
    private void handleModifier() {
        PieceJointe selected = tvPieces.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("⚠️ Choisissez un document dans le tableau.");
            return;
        }
        try {
            selected.setTitre(tfTitre.getText());
            selected.setTypeDoc(cbTypeDoc.getValue());
            selected.setUrl(tfUrl.getText());
            pjService.modifier(selected);
            showSuccess("✅ Document mis à jour !");
            refreshTable();
        } catch (SQLException e) {
            showError("❌ Erreur modification : " + e.getMessage());
        }
    }

    @FXML
    private void handleSupprimer() {
        PieceJointe selected = tvPieces.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("⚠️ Choisissez un document à supprimer.");
            return;
        }
        try {
            pjService.supprimer(selected.getId());
            showSuccess("🗑 Document supprimé.");
            refreshTable();
            clearForm();
        } catch (SQLException e) {
            showError("❌ Erreur suppression : " + e.getMessage());
        }
    }

    @FXML
    private void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/CandidatCandidatureView.fxml"));
            Parent view = loader.load();

            StackPane contentArea = (StackPane) lvCandidatures.getScene().lookup(".content-area");
            if (contentArea != null) {
                contentArea.getChildren().clear();
                contentArea.getChildren().add(view);
                if (view instanceof Region region) {
                    region.prefWidthProperty().bind(contentArea.widthProperty());
                    region.prefHeightProperty().bind(contentArea.heightProperty());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void refreshTable() {
        try {
            tvPieces.getItems().setAll(pjService.getByCandidature(selectedCandidatureId));
        } catch (SQLException e) {
            showError("❌ Erreur chargement documents.");
            e.printStackTrace();
        }
    }

    private void clearForm() {
        tfTitre.clear();
        tfUrl.clear();
        cbTypeDoc.setValue(null);
    }

    private void showError(String msg) {
        lblMsg.setText(msg);
        lblMsg.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
    }

    private void showSuccess(String msg) {
        lblMsg.setText(msg);
        lblMsg.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
    }
}
