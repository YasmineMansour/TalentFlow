package controllers;

import entites.Candidature;
import entites.Utilisateur;
import entites.Offre;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import services.CandidatureService;
import services.UtilisateurService;
import services.OffreService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class AjouterCandidatureController {

    @FXML private ComboBox<Utilisateur> cbUtilisateurs;
    @FXML private ComboBox<Offre> cbOffres; // Changé de TextField à ComboBox
    @FXML private TextField tfCvUrl;
    @FXML private TextArea taMotivation;
    @FXML private TableView<Candidature> tvCandidatures;
    @FXML private TableColumn<Candidature, Integer> colId, colUser, colOffre;
    @FXML private TableColumn<Candidature, String> colCv, colStatut, colMotivation, colDate;

    private final CandidatureService cs = new CandidatureService();
    private final UtilisateurService us = new UtilisateurService();
    private final OffreService os = new OffreService();
    private int idSelectionne = -1;

    @FXML
    public void initialize() {
        // 1. Liaison Colonnes TableView
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUser.setCellValueFactory(new PropertyValueFactory<>("user_id"));
        colOffre.setCellValueFactory(new PropertyValueFactory<>("offre_id"));
        colCv.setCellValueFactory(new PropertyValueFactory<>("cv_url"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date_postulation"));
        colMotivation.setCellValueFactory(new PropertyValueFactory<>("motivation"));

        // 2. Charger les données
        refreshAll();

        // 3. Configuration ComboBox Utilisateurs
        cbUtilisateurs.setCellFactory(lv -> new ListCell<Utilisateur>() {
            @Override
            protected void updateItem(Utilisateur u, boolean empty) {
                super.updateItem(u, empty);
                setText(empty ? "" : u.getNom() + " " + u.getPrenom());
            }
        });
        cbUtilisateurs.setButtonCell(cbUtilisateurs.getCellFactory().call(null));

        // 4. Configuration ComboBox Offres
        cbOffres.setCellFactory(lv -> new ListCell<Offre>() {
            @Override
            protected void updateItem(Offre o, boolean empty) {
                super.updateItem(o, empty);
                setText(empty ? "" : o.getTitre());
            }
        });
        cbOffres.setButtonCell(cbOffres.getCellFactory().call(null));
    }

    private void refreshAll() {
        tvCandidatures.setItems(FXCollections.observableArrayList(cs.afficherTout()));
        cbUtilisateurs.setItems(FXCollections.observableArrayList(us.afficherTout()));
        cbOffres.setItems(FXCollections.observableArrayList(os.afficherTout()));
    }

    @FXML
    void enregistrerCandidature(ActionEvent event) {
        Utilisateur u = cbUtilisateurs.getSelectionModel().getSelectedItem();
        Offre o = cbOffres.getSelectionModel().getSelectedItem();

        if (u == null || o == null || tfCvUrl.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Erreur", "Veuillez remplir tous les champs !");
            return;
        }

        if (showConfirmation("Ajout", "Confirmer l'ajout de cette candidature ?")) {
            try {
                String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                Candidature c = new Candidature(u.getId(), o.getId(), tfCvUrl.getText(), taMotivation.getText(), "En attente", now);
                cs.ajouter(c);
                refreshAll();
                clearFields();
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
            }
        }
    }

    @FXML
    void modifierCandidature(ActionEvent event) {
        if (idSelectionne == -1) {
            showAlert(Alert.AlertType.WARNING, "Sélection", "Sélectionnez une candidature !");
            return;
        }
        Utilisateur u = cbUtilisateurs.getSelectionModel().getSelectedItem();
        Offre o = cbOffres.getSelectionModel().getSelectedItem();
        Candidature selected = tvCandidatures.getSelectionModel().getSelectedItem();

        if (showConfirmation("Modification", "Valider les changements ?")) {
            Candidature c = new Candidature(idSelectionne, u.getId(), o.getId(), tfCvUrl.getText(), taMotivation.getText(), "Modifié", selected.getDate_postulation());
            cs.modifier(c);
            refreshAll();
            clearFields();
        }
    }

    @FXML
    void supprimerCandidature(ActionEvent event) {
        Candidature c = tvCandidatures.getSelectionModel().getSelectedItem();
        if (c != null && showConfirmation("Suppression", "Voulez-vous supprimer cette candidature ?")) {
            cs.supprimer(c.getId());
            refreshAll();
            clearFields();
        }
    }

    @FXML
    void chargerSelection(MouseEvent event) {
        Candidature c = tvCandidatures.getSelectionModel().getSelectedItem();
        if (c != null) {
            idSelectionne = c.getId();
            tfCvUrl.setText(c.getCv_url());
            taMotivation.setText(c.getMotivation());

            // Sélection automatique dans les ComboBox
            for (Utilisateur u : cbUtilisateurs.getItems()) {
                if (u.getId() == c.getUser_id()) { cbUtilisateurs.getSelectionModel().select(u); break; }
            }
            for (Offre o : cbOffres.getItems()) {
                if (o.getId() == c.getOffre_id()) { cbOffres.getSelectionModel().select(o); break; }
            }
        }
    }

    @FXML void effacerChamps(ActionEvent event) { clearFields(); }

    private void clearFields() {
        cbUtilisateurs.getSelectionModel().clearSelection();
        cbOffres.getSelectionModel().clearSelection();
        tfCvUrl.clear(); taMotivation.clear();
        idSelectionne = -1;
    }

    private boolean showConfirmation(String t, String m) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle(t); a.setHeaderText(null); a.setContentText(m);
        return a.showAndWait().filter(r -> r == ButtonType.OK).isPresent();
    }

    private void showAlert(Alert.AlertType type, String t, String m) {
        Alert a = new Alert(type); a.setTitle(t); a.setHeaderText(null); a.setContentText(m); a.showAndWait();
    }
}