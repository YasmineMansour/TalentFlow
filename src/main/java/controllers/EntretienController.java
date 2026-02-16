package controllers;

import entities.Entretien;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import services.EntretienService;
import utils.DialogUtil;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class EntretienController {

    // ───── TABLE ─────
    @FXML private TextField tfSearch;
    @FXML private TableView<Entretien> tvEntretiens;
    @FXML private TableColumn<Entretien, Integer> colId;
    @FXML private TableColumn<Entretien, String>  colCandidature;
    @FXML private TableColumn<Entretien, Object>  colDateHeure;
    @FXML private TableColumn<Entretien, String>  colType;
    @FXML private TableColumn<Entretien, String>  colStatut;
    @FXML private TableColumn<Entretien, String>  colNotes;
    @FXML private TableColumn<Entretien, String>  colLieu;
    @FXML private TableColumn<Entretien, String>  colLien;

    // ───── FORM ─────
    @FXML private ComboBox<String> cbCandidature;
    @FXML private DatePicker dpDate;
    @FXML private TextField tfHeure;
    @FXML private ComboBox<String> cbType;
    @FXML private ComboBox<String> cbStatut;
    @FXML private TextField tfLieu;
    @FXML private TextField tfLien;
    @FXML private TextField tfNoteTech;
    @FXML private TextField tfNoteCom;
    @FXML private TextArea  taCommentaire;
    @FXML private Label     lblError;

    // ───── STATE ─────
    private final EntretienService service = new EntretienService();
    private final ObservableList<Entretien> master   = FXCollections.observableArrayList();
    private final ObservableList<Entretien> filtered = FXCollections.observableArrayList();

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    // ═══════════════════════════════════════════════════════════════
    //  INIT
    // ═══════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        // Table columns
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCandidature.setCellValueFactory(new PropertyValueFactory<>("nomComplet"));
        colDateHeure.setCellValueFactory(new PropertyValueFactory<>("dateHeure"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colLieu.setCellValueFactory(new PropertyValueFactory<>("lieu"));
        colLien.setCellValueFactory(new PropertyValueFactory<>("lien"));

        // Notes column: "tech / com"
        colNotes.setCellValueFactory(cellData -> {
            Entretien e = cellData.getValue();
            String tech = e.getNoteTechnique() != null ? String.valueOf(e.getNoteTechnique()) : "-";
            String com  = e.getNoteCommunication() != null ? String.valueOf(e.getNoteCommunication()) : "-";
            return new javafx.beans.property.SimpleStringProperty(tech + " / " + com);
        });

        tvEntretiens.setItems(filtered);

        // ComboBoxes
        cbType.setItems(FXCollections.observableArrayList("EN_LIGNE", "PRESENTIEL", "TELEPHONIQUE"));
        cbStatut.setItems(FXCollections.observableArrayList("PLANIFIE", "REALISE", "ANNULE"));

        // Lieu / Lien toggle based on type
        cbType.valueProperty().addListener((obs, oldVal, newVal) -> updateLieuLienUX(newVal));

        // Live validation on time field
        setupTimeValidation();

        // Load data
        try {
            loadCandidatures();
            loadEntretiens();
        } catch (SQLException e) {
            DialogUtil.error("Erreur", "Chargement impossible : " + e.getMessage());
        }

        clearForm();
    }

    // ═══════════════════════════════════════════════════════════════
    //  UX: enable/disable lieu vs lien based on type
    // ═══════════════════════════════════════════════════════════════
    private void updateLieuLienUX(String type) {
        if (type == null) {
            tfLieu.setDisable(false);
            tfLien.setDisable(false);
            return;
        }
        switch (type) {
            case "EN_LIGNE":
                tfLieu.setDisable(true);
                tfLieu.clear();
                tfLien.setDisable(false);
                break;
            case "PRESENTIEL":
                tfLien.setDisable(true);
                tfLien.clear();
                tfLieu.setDisable(false);
                break;
            case "TELEPHONIQUE":
                tfLieu.setDisable(false);
                tfLien.setDisable(false);
                break;
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  DATA LOADING
    // ═══════════════════════════════════════════════════════════════
    private void loadEntretiens() throws SQLException {
        master.setAll(service.findAll());
        applyFilter();
    }

    private void loadCandidatures() throws SQLException {
        cbCandidature.setItems(FXCollections.observableArrayList(service.findAllCandidaturesLabels()));
    }

    private void applyFilter() {
        String q = tfSearch.getText() == null ? "" : tfSearch.getText().trim().toLowerCase();
        filtered.setAll(master.filtered(e ->
                q.isEmpty()
                        || (e.getNomComplet() != null && e.getNomComplet().toLowerCase().contains(q))
                        || (e.getType() != null && e.getType().toLowerCase().contains(q))
                        || (e.getStatut() != null && e.getStatut().toLowerCase().contains(q))
                        || (e.getLieu() != null && e.getLieu().toLowerCase().contains(q))
                        || (e.getCommentaire() != null && e.getCommentaire().toLowerCase().contains(q))
        ));
    }

    // ═══════════════════════════════════════════════════════════════
    //  ACTIONS
    // ═══════════════════════════════════════════════════════════════
    @FXML
    private void onRefresh() {
        hideError();
        try {
            loadCandidatures();
            loadEntretiens();
            tvEntretiens.refresh();
        } catch (Exception e) {
            DialogUtil.error("Erreur", "Chargement impossible : " + e.getMessage());
        }
    }

    @FXML
    private void onSearch() {
        applyFilter();
    }

    @FXML
    private void onTableClick() {
        Entretien e = tvEntretiens.getSelectionModel().getSelectedItem();
        if (e == null) return;
        fillForm(e);
    }

    @FXML
    private void onAjouter() {
        hideError();
        try {
            Entretien e = buildFromForm();
            service.add(e);
            DialogUtil.info("Succès", "Entretien ajouté avec succès.");
            onRefresh();
            clearForm();
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    @FXML
    private void onModifier() {
        hideError();
        Entretien row = tvEntretiens.getSelectionModel().getSelectedItem();
        if (row == null) {
            showError("Sélectionnez un entretien dans la table.");
            return;
        }
        try {
            Entretien e = buildFromForm();
            e.setId(row.getId());
            service.update(e);
            DialogUtil.info("Succès", "Entretien modifié avec succès.");
            onRefresh();
            clearForm();
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    @FXML
    private void onSupprimer() {
        hideError();
        Entretien row = tvEntretiens.getSelectionModel().getSelectedItem();
        if (row == null) {
            showError("Sélectionnez un entretien dans la table.");
            return;
        }
        if (!DialogUtil.confirmYesNo("Confirmation", "Suppression",
                "Êtes-vous sûr de supprimer l'entretien #" + row.getId() + " ?"))
            return;
        try {
            service.delete(row.getId());
            DialogUtil.info("Succès", "Entretien supprimé.");
            onRefresh();
            clearForm();
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    @FXML
    private void onClear() {
        clearForm();
    }

    @FXML
    private void onAjouterFeedback() {
        hideError();
        Entretien row = tvEntretiens.getSelectionModel().getSelectedItem();
        if (row == null) {
            showError("Sélectionnez un entretien pour ajouter un feedback.");
            return;
        }
        try {
            Integer noteTech = parseOptionalNote(tfNoteTech.getText(), "Note technique");
            Integer noteCom  = parseOptionalNote(tfNoteCom.getText(), "Note communication");
            String commentaire = taCommentaire.getText();

            row.setNoteTechnique(noteTech);
            row.setNoteCommunication(noteCom);
            row.setCommentaire(commentaire);
            row.setStatut("REALISE");

            service.update(row);
            DialogUtil.info("Succès", "Feedback enregistré.");
            onRefresh();
            clearForm();
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  FORM BUILD / FILL / CLEAR
    // ═══════════════════════════════════════════════════════════════
    private Entretien buildFromForm() {
        Entretien e = new Entretien();

        // Candidature
        String candidatureLabel = cbCandidature.getValue();
        if (candidatureLabel == null || candidatureLabel.isBlank()) {
            throw new IllegalArgumentException("Sélectionnez une candidature.");
        }
        int candidatureId = Integer.parseInt(candidatureLabel.split(" - ")[0].trim());
        e.setCandidatureId(candidatureId);

        // Date + heure
        LocalDate date = dpDate.getValue();
        if (date == null) throw new IllegalArgumentException("Sélectionnez une date.");

        String heureStr = tfHeure.getText();
        if (heureStr == null || heureStr.isBlank()) throw new IllegalArgumentException("Saisissez l'heure (HH:mm).");
        if (!isValidTime(heureStr.trim())) throw new IllegalArgumentException("Format heure invalide (HH:mm).");

        LocalTime time = LocalTime.parse(heureStr.trim(), TIME_FMT);
        e.setDateHeure(LocalDateTime.of(date, time));

        // Type
        e.setType(cbType.getValue());

        // Statut
        e.setStatut(cbStatut.getValue());

        // Lieu / Lien
        e.setLieu(tfLieu.getText());
        e.setLien(tfLien.getText());

        // Notes
        e.setNoteTechnique(parseOptionalNote(tfNoteTech.getText(), "Note technique"));
        e.setNoteCommunication(parseOptionalNote(tfNoteCom.getText(), "Note communication"));

        // Commentaire
        e.setCommentaire(taCommentaire.getText());

        return e;
    }

    private Integer parseOptionalNote(String s, String label) {
        if (s == null || s.trim().isEmpty()) return null;
        try {
            int val = Integer.parseInt(s.trim());
            if (val < 0 || val > 20) throw new IllegalArgumentException(label + " doit être entre 0 et 20.");
            return val;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(label + " : nombre invalide.");
        }
    }

    private void fillForm(Entretien e) {
        selectCandidatureLabel(e.getCandidatureId());

        if (e.getDateHeure() != null) {
            dpDate.setValue(e.getDateHeure().toLocalDate());
            tfHeure.setText(e.getDateHeure().toLocalTime().format(TIME_FMT));
        }

        cbType.setValue(e.getType());
        cbStatut.setValue(e.getStatut());
        tfLieu.setText(e.getLieu() != null ? e.getLieu() : "");
        tfLien.setText(e.getLien() != null ? e.getLien() : "");
        tfNoteTech.setText(e.getNoteTechnique() != null ? String.valueOf(e.getNoteTechnique()) : "");
        tfNoteCom.setText(e.getNoteCommunication() != null ? String.valueOf(e.getNoteCommunication()) : "");
        taCommentaire.setText(e.getCommentaire() != null ? e.getCommentaire() : "");

        updateLieuLienUX(e.getType());
    }

    private void selectCandidatureLabel(int candidatureId) {
        for (String label : cbCandidature.getItems()) {
            if (label.startsWith(candidatureId + " - ")) {
                cbCandidature.setValue(label);
                return;
            }
        }
        cbCandidature.setValue(null);
    }

    private void clearForm() {
        tvEntretiens.getSelectionModel().clearSelection();
        cbCandidature.setValue(null);
        dpDate.setValue(null);
        tfHeure.clear();
        cbType.setValue(null);
        cbStatut.setValue(null);
        tfLieu.clear();
        tfLieu.setDisable(false);
        tfLien.clear();
        tfLien.setDisable(false);
        tfNoteTech.clear();
        tfNoteCom.clear();
        taCommentaire.clear();
        hideError();
    }

    // ═══════════════════════════════════════════════════════════════
    //  VALIDATION HELPERS
    // ═══════════════════════════════════════════════════════════════
    private void setupTimeValidation() {
        tfHeure.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isEmpty()) {
                clearMark(tfHeure);
            } else {
                mark(tfHeure, isValidTime(newVal.trim()));
            }
            validateLive();
        });

        tfNoteTech.textProperty().addListener((obs, o, n) -> {
            mark(tfNoteTech, isValidOptionalNote(n));
            validateLive();
        });

        tfNoteCom.textProperty().addListener((obs, o, n) -> {
            mark(tfNoteCom, isValidOptionalNote(n));
            validateLive();
        });
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

    private void mark(Control c, boolean ok) {
        c.setStyle(ok ? "-fx-border-color: #27ae60;" : "-fx-border-color: #e74c3c;");
    }

    private void clearMark(Control c) {
        c.setStyle("");
    }

    private boolean isValidTime(String h) {
        if (h == null) return false;
        return h.matches("^([01]\\d|2[0-3]):[0-5]\\d$");
    }

    private boolean isValidOptionalNote(String s) {
        if (s == null || s.trim().isEmpty()) return true;
        try {
            int v = Integer.parseInt(s.trim());
            return v >= 0 && v <= 20;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void validateLive() {
        // Optional: could aggregate and show live errors
    }
}
