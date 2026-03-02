package org.example.GUI;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import org.example.model.Candidature;
import org.example.model.Offre;
import org.example.model.User;
import org.example.services.CandidatureService;
import org.example.services.OffreService;
import org.example.utils.EmailService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * Contrôleur de l'espace Candidat — Postuler aux offres et gérer ses candidatures.
 */
public class CandidatCandidatureController {

    private final OffreService offreService = new OffreService();
    private final CandidatureService candidatureService = new CandidatureService();

    @FXML private TextField tfOffreId, tfCvUrl, tfEmail;
    @FXML private TextArea taMotivation;
    @FXML private Label lblMsg;

    @FXML private TableView<Candidature> tvMesCandidatures;
    @FXML private TableColumn<Candidature, Integer> colId, colOffreId;
    @FXML private TableColumn<Candidature, String> colStatut, colCv, colMotivation, colTitreOffre;
    @FXML private TableColumn<Candidature, Object> colDate;

    @FXML private TableView<Offre> tvOffres;
    @FXML private TableColumn<Offre, Integer> colOffreListId;
    @FXML private TableColumn<Offre, String> colOffreListTitre, colOffreListLoc, colOffreListStatut, colOffreListPostule;

    private final ObservableList<Candidature> dataCandidatures = FXCollections.observableArrayList();
    private final ObservableList<Offre> dataOffres = FXCollections.observableArrayList();

    /** Set des offre IDs auxquels le candidat a déjà postulé */
    private java.util.Set<Integer> appliedOffreIds = new java.util.HashSet<>();

    private int getCurrentUserId() {
        User u = UserSession.getInstance();
        return (u != null) ? u.getId() : 1;
    }

    private String getCurrentUserEmail() {
        User u = UserSession.getInstance();
        return (u != null) ? u.getEmail() : "";
    }

    @FXML
    public void initialize() {
        // Colonnes candidatures
        colId.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getId()));
        colOffreId.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getOffreId()));
        colStatut.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatut()));
        colDate.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getDatePostulation()));
        colCv.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCvUrl()));
        colMotivation.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMotivation()));

        if (colTitreOffre != null) {
            colTitreOffre.setCellValueFactory(c -> new SimpleStringProperty(
                    c.getValue().getTitreOffre() != null ? c.getValue().getTitreOffre() : "Offre #" + c.getValue().getOffreId()));
        }

        // Coloration statut
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                switch (item) {
                    case "ACCEPTE" -> setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    case "EN_ATTENTE" -> setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                    case "EN_COURS" -> setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
                    case "REFUSE" -> setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    default -> setStyle("");
                }
            }
        });

        tvMesCandidatures.setItems(dataCandidatures);

        // Colonnes offres
        colOffreListId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getId()).asObject());
        colOffreListTitre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitre()));
        colOffreListLoc.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getLocalisation()));
        if (colOffreListStatut != null) {
            colOffreListStatut.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatut()));
        }
        // Colonne "Postulé ?" — indique si le candidat a déjà postulé
        if (colOffreListPostule != null) {
            colOffreListPostule.setCellValueFactory(c -> {
                boolean applied = appliedOffreIds.contains(c.getValue().getId());
                return new SimpleStringProperty(applied ? "✅ Oui" : "—");
            });
            colOffreListPostule.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { setText(null); setStyle(""); return; }
                    setText(item);
                    if (item.contains("Oui")) {
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #95a5a6;");
                    }
                }
            });
        }
        // Coloration des lignes d'offres (déjà postulé = fond vert clair)
        tvOffres.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Offre item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else if (appliedOffreIds.contains(item.getId())) {
                    setStyle("-fx-background-color: #e8f5e9;");
                } else {
                    setStyle("");
                }
            }
        });
        tvOffres.setItems(dataOffres);

        // Sélection offre → remplir l'ID
        tvOffres.getSelectionModel().selectedItemProperty().addListener((obs, old, newSel) -> {
            if (newSel != null) tfOffreId.setText(String.valueOf(newSel.getId()));
        });

        // Sélection candidature → remplir le formulaire
        tvMesCandidatures.getSelectionModel().selectedItemProperty().addListener((obs, old, newSel) -> {
            if (newSel != null) {
                tfOffreId.setText(String.valueOf(newSel.getOffreId()));
                tfCvUrl.setText(newSel.getCvUrl());
                taMotivation.setText(newSel.getMotivation());
                tfEmail.setText(newSel.getEmail());
            }
        });

        // Pré-remplir l'email depuis la session
        String sessionEmail = getCurrentUserEmail();
        if (sessionEmail != null && !sessionEmail.isEmpty()) {
            tfEmail.setText(sessionEmail);
        }

        refresh();
        loadOffres();
    }

    @FXML
    private void handlePostuler() {
        try {
            if (tfEmail.getText().isEmpty() || !tfEmail.getText().contains("@")) {
                showError("⚠️ Email invalide !");
                return;
            }
            if (tfOffreId.getText().isEmpty()) {
                showError("⚠️ Sélectionnez une offre dans la liste !");
                return;
            }

            int oId = Integer.parseInt(tfOffreId.getText());
            int userId = getCurrentUserId();

            if (candidatureService.alreadyApplied(userId, oId)) {
                showError("📌 Vous avez déjà postulé à cette offre !");
                return;
            }

            Candidature c = new Candidature(
                    userId,
                    oId,
                    tfCvUrl.getText(),
                    taMotivation.getText(),
                    "EN_ATTENTE",
                    tfEmail.getText()
            );

            candidatureService.add(c);

            // Envoi email de confirmation en arrière-plan
            String email = tfEmail.getText();
            new Thread(() -> {
                try {
                    EmailService.sendCandidatureConfirmation(email, oId);
                } catch (Exception ex) {
                    System.err.println("Erreur envoi email: " + ex.getMessage());
                }
            }).start();

            showOk("🎉 Candidature envoyée avec succès ! 📧 Email de confirmation envoyé.");
            handleVider();
            refresh();
        } catch (NumberFormatException e) {
            showError("❌ ID offre invalide.");
        } catch (Exception e) {
            showError("❌ Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleModifier() {
        Candidature selected = tvMesCandidatures.getSelectionModel().getSelectedItem();
        if (selected == null) { showError("❌ Sélectionnez une candidature à modifier."); return; }
        if (!"EN_ATTENTE".equals(selected.getStatut())) {
            showError("⚠️ Vous ne pouvez modifier qu'une candidature en attente.");
            return;
        }
        if (tfEmail.getText().isEmpty() || !tfEmail.getText().contains("@")) {
            showError("⚠️ Email invalide !");
            return;
        }
        try {
            selected.setCvUrl(tfCvUrl.getText());
            selected.setMotivation(taMotivation.getText());
            selected.setEmail(tfEmail.getText());
            candidatureService.update(selected);
            showOk("✅ Candidature mise à jour !");
            refresh();
        } catch (SQLException e) {
            showError("❌ Erreur SQL : " + e.getMessage());
        }
    }

    @FXML
    private void handleSupprimer() {
        Candidature selected = tvMesCandidatures.getSelectionModel().getSelectedItem();
        if (selected == null) { showError("❌ Sélectionnez une candidature."); return; }
        if (!"EN_ATTENTE".equals(selected.getStatut())) {
            showError("⚠️ Vous ne pouvez supprimer qu'une candidature en attente.");
            return;
        }
        try {
            candidatureService.delete(selected.getId());
            showOk("🗑 Candidature supprimée.");
            handleVider();
            refresh();
        } catch (SQLException e) {
            showError("❌ Erreur suppression : " + e.getMessage());
        }
    }

    @FXML
    private void handleVider() {
        tfOffreId.clear();
        tfCvUrl.clear();
        taMotivation.clear();
        // Re-remplir l'email depuis la session
        String sessionEmail = getCurrentUserEmail();
        tfEmail.setText(sessionEmail != null ? sessionEmail : "");
        tvMesCandidatures.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleGererPieces() {
        Candidature selected = tvMesCandidatures.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("⚠️ Sélectionnez une candidature pour gérer les pièces jointes !");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/PieceJointeView.fxml"));
            Parent view = loader.load();
            PieceJointeController controller = loader.getController();
            controller.setCandidatureId(selected.getId());

            StackPane contentArea = (StackPane) tvMesCandidatures.getScene().lookup(".content-area");
            if (contentArea != null) {
                contentArea.getChildren().clear();
                contentArea.getChildren().add(view);
                if (view instanceof Region region) {
                    region.prefWidthProperty().bind(contentArea.widthProperty());
                    region.prefHeightProperty().bind(contentArea.heightProperty());
                }
            }
        } catch (IOException e) {
            showError("❌ Erreur chargement pièces jointes.");
            e.printStackTrace();
        }
    }

    private void refresh() {
        try {
            dataCandidatures.setAll(candidatureService.getByUser(getCurrentUserId()));
            // Mettre à jour le set des offres auxquelles le candidat a postulé
            appliedOffreIds.clear();
            for (Candidature c : dataCandidatures) {
                appliedOffreIds.add(c.getOffreId());
            }
            // Forcer le rafraîchissement visuel du tableau des offres
            tvOffres.refresh();
        } catch (Exception e) {
            showError("❌ Erreur chargement candidatures.");
            e.printStackTrace();
        }
    }

    private void loadOffres() {
        try {
            List<Offre> offres = offreService.afficher();
            dataOffres.setAll(offres);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showError(String msg) {
        lblMsg.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        lblMsg.setText(msg);
    }

    private void showOk(String msg) {
        lblMsg.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
        lblMsg.setText(msg);
    }
}
