package controllers;

import entities.Candidature;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import services.CandidatureService;
import utils.DialogUtil;

public class CandidatureController {

    @FXML private TextField tfSearch;
    @FXML private TableView<Candidature> tvCandidatures;
    @FXML private TableColumn<Candidature, Integer> colId;
    @FXML private TableColumn<Candidature, String> colNom;
    @FXML private TableColumn<Candidature, String> colPrenom;
    @FXML private TableColumn<Candidature, String> colEmail;
    @FXML private TableColumn<Candidature, String> colStatut;

    @FXML private TextField tfNom;
    @FXML private TextField tfPrenom;
    @FXML private TextField tfEmail;
    @FXML private ComboBox<String> cbStatut;
    @FXML private Label lblError;

    private final CandidatureService service = new CandidatureService();

    private final ObservableList<Candidature> master = FXCollections.observableArrayList();
    private final ObservableList<Candidature> filtered = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nomCandidat"));
        colPrenom.setCellValueFactory(new PropertyValueFactory<>("prenomCandidat"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));

        tvCandidatures.setItems(filtered);
        cbStatut.setItems(FXCollections.observableArrayList("EN_ATTENTE", "ACCEPTE", "REFUSE"));

        onRefresh();
        onClear();
    }

    @FXML
    private void onRefresh() {
        hideError();
        try {
            master.setAll(service.findAll());
            applyFilter();
            tvCandidatures.refresh();
        } catch (Exception e) {
            DialogUtil.error("Erreur", "Chargement impossible : " + e.getMessage());
        }
    }

    @FXML
    private void onSearch() {
        applyFilter();
    }

    private void applyFilter() {
        String q = tfSearch.getText() == null ? "" : tfSearch.getText().trim().toLowerCase();
        filtered.setAll(master.filtered(c ->
                q.isEmpty()
                        || (c.getNomCandidat() != null && c.getNomCandidat().toLowerCase().contains(q))
                        || (c.getPrenomCandidat() != null && c.getPrenomCandidat().toLowerCase().contains(q))
                        || (c.getEmail() != null && c.getEmail().toLowerCase().contains(q))
        ));
    }

    @FXML
    private void onTableClick() {
        Candidature c = tvCandidatures.getSelectionModel().getSelectedItem();
        if (c == null) return;

        tfNom.setText(c.getNomCandidat());
        tfPrenom.setText(c.getPrenomCandidat());
        tfEmail.setText(c.getEmail());
        cbStatut.setValue(c.getStatut());
    }

    @FXML
    private void onAjouter() {
        hideError();
        try {
            Candidature c = buildFromForm();
            service.add(c);
            DialogUtil.info("Succès", "Candidature ajoutée");
            onRefresh();
            onClear();
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void onModifier() {
        hideError();
        Candidature row = tvCandidatures.getSelectionModel().getSelectedItem();
        if (row == null) {
            showError("Sélectionnez une candidature.");
            return;
        }

        try {
            Candidature c = buildFromForm();
            c.setId(row.getId());
            service.update(c);
            DialogUtil.info("Succès", "Candidature modifiée");
            onRefresh();
            onClear();
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void onSupprimer() {
        hideError();
        Candidature row = tvCandidatures.getSelectionModel().getSelectedItem();
        if (row == null) {
            showError("Sélectionnez une candidature.");
            return;
        }

        if (!DialogUtil.confirmYesNo("Confirmation", "Suppression",
                "Êtes-vous sûr de supprimer la candidature #" + row.getId() + " (" + row.getNomComplet() + ") ?"))
            return;

        try {
            service.delete(row.getId());
            DialogUtil.info("Succès", "Candidature supprimée");
            onRefresh();
            onClear();
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void onClear() {
        tvCandidatures.getSelectionModel().clearSelection();
        tfNom.clear();
        tfPrenom.clear();
        tfEmail.clear();
        cbStatut.setValue(null);
        hideError();
    }

    private Candidature buildFromForm() {
        Candidature c = new Candidature();
        c.setNomCandidat(tfNom.getText());
        c.setPrenomCandidat(tfPrenom.getText());
        c.setEmail(tfEmail.getText());
        c.setStatut(cbStatut.getValue());
        return c;
    }

    private void showError(String msg) {
        lblError.setManaged(true);
        lblError.setVisible(true);
        lblError.setText(msg);
    }

    private void hideError() {
        lblError.setManaged(false);
        lblError.setVisible(false);
        lblError.setText("");
    }
}
