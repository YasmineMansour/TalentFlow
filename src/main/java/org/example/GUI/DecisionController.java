package org.example.GUI;

import org.example.model.DecisionFinale;
import org.example.model.Entretien;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.SpinnerValueFactory.DoubleSpinnerValueFactory;
import javafx.scene.control.cell.PropertyValueFactory;
import org.example.services.DecisionService;
import org.example.services.EntretienService;

import java.sql.SQLException;
import java.time.LocalDateTime;

public class DecisionController {

    @FXML private TableView<DecisionFinale> tvDecisions;
    @FXML private TableColumn<DecisionFinale, String> colEmailCandidat;
    @FXML private TableColumn<DecisionFinale, String> colDecision;
    @FXML private TableColumn<DecisionFinale, String> colMotif;
    @FXML private TableColumn<DecisionFinale, String> colScore;

    @FXML private ComboBox<String> cbEntretien;
    private final java.util.Map<String, Integer> emailToEntretienId = new java.util.HashMap<>();
    private final java.util.Map<Integer, String> entretienIdToEmail = new java.util.HashMap<>();
    @FXML private ComboBox<String> cbDecision;
    @FXML private TextArea tfMotif;
    @FXML private TextField tfSearch;
    @FXML private ComboBox<String> cbFilterDecision;
    @FXML private ComboBox<String> cbTriScore;
    @FXML private Label lblError;
    @FXML private Label lblScore;
    @FXML private Spinner<Double> spinAccepte;
    @FXML private Spinner<Double> spinRefuse;
    @FXML private CheckBox chkAutoEngine;
    @FXML private Label lblAutoResult;

    private final DecisionService service = new DecisionService();
    private final EntretienService entretienService = new EntretienService();

    private final ObservableList<DecisionFinale> master = FXCollections.observableArrayList();
    private final ObservableList<DecisionFinale> filtered = FXCollections.observableArrayList();

    @FXML
    public void initialize() {

        colEmailCandidat.setCellValueFactory(c -> {
            int eid = c.getValue().getEntretienId();
            try {
                java.util.Optional<Entretien> ent = entretienService.findById(eid);
                return new SimpleStringProperty(ent.isPresent() ? ent.get().getEmailCandidat() : "ID:" + eid);
            } catch (Exception ex) {
                return new SimpleStringProperty("ID:" + eid);
            }
        });
        colDecision.setCellValueFactory(new PropertyValueFactory<>("decision"));
        colMotif.setCellValueFactory(new PropertyValueFactory<>("motif"));

        colScore.setCellValueFactory(c -> {
            Double s = c.getValue().getScore();
            return new SimpleStringProperty(s == null ? "-" : String.valueOf(s) + " / 20");
        });
        colScore.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null || "-".equals(val)) {
                    setText(null); setStyle("");
                } else {
                    setText(val);
                    try {
                        double score = Double.parseDouble(val.replace(" / 20", ""));
                        if (score >= 16) setStyle("-fx-text-fill:#27ae60; -fx-font-weight:bold;");
                        else if (score >= 12) setStyle("-fx-text-fill:#2980b9; -fx-font-weight:bold;");
                        else if (score >= 10) setStyle("-fx-text-fill:#f39c12; -fx-font-weight:bold;");
                        else setStyle("-fx-text-fill:#e74c3c; -fx-font-weight:bold;");
                    } catch (NumberFormatException e) {
                        setStyle("");
                    }
                }
            }
        });

        colDecision.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) { setText(null); setStyle(""); return; }
                setText(val);
                switch (val) {
                    case "ACCEPTE"   -> setStyle("-fx-text-fill:#27ae60; -fx-font-weight:bold;");
                    case "REFUSE"    -> setStyle("-fx-text-fill:#e74c3c; -fx-font-weight:bold;");
                    case "EN_ATTENTE" -> setStyle("-fx-text-fill:#f39c12; -fx-font-weight:bold;");
                    default -> setStyle("");
                }
            }
        });

        tvDecisions.setItems(filtered);

        cbDecision.setItems(FXCollections.observableArrayList(
                "ACCEPTE", "REFUSE", "EN_ATTENTE"
        ));

        cbFilterDecision.setItems(FXCollections.observableArrayList(
                "Toutes", "ACCEPTE", "REFUSE", "EN_ATTENTE"));
        cbFilterDecision.setValue("Toutes");
        cbTriScore.setItems(FXCollections.observableArrayList(
                "Par défaut", "Score (plus haut)", "Score (plus bas)"));
        cbTriScore.setValue("Par défaut");

        cbFilterDecision.setOnAction(e -> applyFilter());
        cbTriScore.setOnAction(e -> applyFilter());
        tfSearch.textProperty().addListener((obs, oldVal, newVal) -> applyFilter());

        spinAccepte.setValueFactory(new DoubleSpinnerValueFactory(0, 20, 14, 0.5));
        spinRefuse.setValueFactory(new DoubleSpinnerValueFactory(0, 20, 8, 0.5));
        chkAutoEngine.setSelected(false);

        tvDecisions.setOnMouseClicked(e -> fillForm());

        loadEntretiens();
        try {
            onRefresh();
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Erreur init: " + ex.getMessage());
        }
    }

    private void loadEntretiens() {
        try {
            cbEntretien.getItems().clear();
            emailToEntretienId.clear();
            entretienIdToEmail.clear();

            for (Entretien e : entretienService.findAll()) {
                if ("REALISE".equals(e.getStatut())) {
                    String email = e.getEmailCandidat() != null ? e.getEmailCandidat() : "ID:" + e.getId();
                    emailToEntretienId.put(email, e.getId());
                    entretienIdToEmail.put(e.getId(), email);
                    cbEntretien.getItems().add(email);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onRefresh() {
        try {
            int created = service.autoCreateForRealises();
            if (created > 0) {
                System.out.println("Auto-created " + created + " decision(s) for REALISE entretiens.");
            }
            master.setAll(service.findAll());
            applyFilter();
            loadEntretiens();
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    private void applyFilter() {
        String q = tfSearch.getText() == null ? "" : tfSearch.getText().toLowerCase();
        String decFilter = cbFilterDecision.getValue();
        String triScore = cbTriScore.getValue();

        java.util.List<DecisionFinale> result = master.stream().filter(d -> {
            if (decFilter != null && !"Toutes".equals(decFilter)) {
                if (d.getDecision() == null || !d.getDecision().equals(decFilter)) return false;
            }
            if (!q.isEmpty()) {
                String email = entretienIdToEmail.getOrDefault(d.getEntretienId(), "");
                return email.toLowerCase().contains(q)
                        || (d.getDecision() != null && d.getDecision().toLowerCase().contains(q))
                        || (d.getMotif() != null && d.getMotif().toLowerCase().contains(q));
            }
            return true;
        }).collect(java.util.stream.Collectors.toList());

        if ("Score (plus haut)".equals(triScore)) {
            result.sort((a, b) -> {
                Double sa = a.getScore(), sb = b.getScore();
                if (sa == null && sb == null) return 0;
                if (sa == null) return 1;
                if (sb == null) return -1;
                return Double.compare(sb, sa);
            });
        } else if ("Score (plus bas)".equals(triScore)) {
            result.sort((a, b) -> {
                Double sa = a.getScore(), sb = b.getScore();
                if (sa == null && sb == null) return 0;
                if (sa == null) return 1;
                if (sb == null) return -1;
                return Double.compare(sa, sb);
            });
        }

        filtered.setAll(result);
    }

    @FXML
    private void onAjouter() {
        try {
            DecisionFinale d = buildFromForm();
            service.add(d);
            showInfo("Décision ajoutée.");
            onRefresh();
            clearForm();
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void onModifier() {
        DecisionFinale selected = tvDecisions.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Sélectionnez une décision à modifier.");
            return;
        }
        try {
            DecisionFinale d = buildFromForm();
            d.setId(selected.getId());
            service.update(d);
            showInfo("Décision modifiée.");
            onRefresh();
            clearForm();
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void onSupprimer() {
        DecisionFinale selected = tvDecisions.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Sélectionnez une décision à supprimer.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setContentText("Voulez-vous supprimer cette décision ?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                service.delete(selected.getId());
                showInfo("Décision supprimée.");
                onRefresh();
                clearForm();
            } catch (Exception e) {
                showError(e.getMessage());
            }
        }
    }

    private void fillForm() {
        DecisionFinale selected = tvDecisions.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        String email = entretienIdToEmail.get(selected.getEntretienId());
        cbEntretien.setValue(email != null ? email : "ID:" + selected.getEntretienId());
        cbDecision.setValue(selected.getDecision());
        tfMotif.setText(selected.getMotif());

        if (selected.getScore() != null) {
            double sc = selected.getScore();
            String niveau;
            String color;
            if (sc >= 16) { niveau = "EXCELLENT"; color = "#27ae60"; }
            else if (sc >= 12) { niveau = "BON"; color = "#2980b9"; }
            else if (sc >= 10) { niveau = "MOYEN"; color = "#f39c12"; }
            else { niveau = "INSUFFISANT"; color = "#e74c3c"; }
            lblScore.setText("Score : " + sc + " / 20  —  " + niveau);
            lblScore.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-font-size: 14;");
        } else {
            lblScore.setText("Score : -");
            lblScore.setStyle("-fx-text-fill: #7f8c8d; -fx-font-weight: bold; -fx-font-size: 14;");
        }
    }

    @FXML
    private void onClear() {
        clearForm();
    }

    private void clearForm() {
        cbEntretien.setValue(null);
        cbDecision.setValue(null);
        tfMotif.clear();
        lblError.setVisible(false);
        lblError.setManaged(false);
        lblScore.setText("Score : -");
        lblScore.setStyle("-fx-text-fill: #7f8c8d; -fx-font-weight: bold; -fx-font-size: 14;");
    }

    private DecisionFinale buildFromForm() {
        if (cbEntretien.getValue() == null)
            throw new IllegalArgumentException("Email candidat obligatoire.");
        if (cbDecision.getValue() == null)
            throw new IllegalArgumentException("Décision obligatoire.");

        Integer entretienId = emailToEntretienId.get(cbEntretien.getValue());
        if (entretienId == null)
            throw new IllegalArgumentException("Candidat introuvable.");

        DecisionFinale d = new DecisionFinale();
        d.setEntretienId(entretienId);
        d.setDecision(cbDecision.getValue());
        d.setMotif(tfMotif.getText());
        d.setDateDecision(LocalDateTime.now());

        try {
            var ent = entretienService.findById(entretienId);
            if (ent.isPresent()) {
                d.setScore(ent.get().getScoreFinal());
            }
        } catch (Exception ignored) {}

        return d;
    }

    private void showError(String msg) {
        lblError.setText(msg);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setContentText(msg);
        a.show();
    }

    @FXML
    private void onAutoDecideAll() {
        double sa = spinAccepte.getValue();
        double sr = spinRefuse.getValue();

        if (sr >= sa) {
            showError("Le seuil REFUSE (" + sr + ") doit être < seuil ACCEPTE (" + sa + ").");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Moteur décisionnel");
        confirm.setHeaderText("Appliquer les seuils automatiques ?");
        confirm.setContentText(
                "Score >= " + sa + " -> ACCEPTE\n" +
                "Score < " + sr + " -> REFUSE\n" +
                "Entre " + sr + " et " + sa + " -> reste EN_ATTENTE\n\n" +
                "Un email sera envoyé automatiquement pour chaque décision.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            int[] result = service.autoDecideByScore(sa, sr);
            String msg = "Moteur décisionnel appliqué :\n" +
                    "   - " + result[0] + " ACCEPTE(s)\n" +
                    "   - " + result[1] + " REFUSE(s)\n" +
                    "   - " + result[2] + " inchangé(s)";
            lblAutoResult.setText(msg);
            lblAutoResult.setStyle("-fx-text-fill:#27ae60; -fx-font-style:italic;");
            showInfo(msg);
            onRefresh();
        } catch (Exception e) {
            showError("Erreur moteur : " + e.getMessage());
            lblAutoResult.setText("Erreur : " + e.getMessage());
            lblAutoResult.setStyle("-fx-text-fill:#e74c3c; -fx-font-style:italic;");
        }
    }
}
