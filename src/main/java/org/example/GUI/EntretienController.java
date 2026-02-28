package org.example.GUI;

import org.example.model.Entretien;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.example.services.EntretienEmailService;
import org.example.services.EntretienService;
import org.example.services.GoogleCalendarService;
import org.example.services.DecisionService;
import org.example.utils.DialogUtil;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class EntretienController {

    // ── Table ──
    @FXML private TableView<Entretien> tvEntretiens;

    @FXML private TableColumn<Entretien, String> colDateHeure;
    @FXML private TableColumn<Entretien, String> colType;
    @FXML private TableColumn<Entretien, String> colStatut;
    @FXML private TableColumn<Entretien, String> colLieu;
    @FXML private TableColumn<Entretien, String> colLien;
    @FXML private TableColumn<Entretien, String> colEmail;
    @FXML private TableColumn<Entretien, String> colNoteTech;
    @FXML private TableColumn<Entretien, String> colNoteCom;
    @FXML private TableColumn<Entretien, String> colScore;

    // ── Form fields ──
    @FXML private TextField tfEmail;
    @FXML private DatePicker dpDate;
    @FXML private TextField tfHeure;
    @FXML private ComboBox<String> cbType;
    @FXML private ComboBox<String> cbStatut;
    @FXML private TextField tfLieu;
    @FXML private TextField tfLien;
    @FXML private TextField tfNoteTech;
    @FXML private TextField tfNoteCom;
    @FXML private TextArea taCommentaire;
    @FXML private Label lblScore;
    @FXML private Button btnFeedback;

    // ── Search & Filters ──
    @FXML private TextField tfSearch;
    @FXML private ComboBox<String> cbFilterStatut;
    @FXML private ComboBox<String> cbFilterType;

    // ── Services ──
    private final EntretienService service = new EntretienService();
    private final EntretienEmailService emailService = new EntretienEmailService();
    private final GoogleCalendarService calendarService = GoogleCalendarService.getInstance();

    private final ObservableList<Entretien> masterList = FXCollections.observableArrayList();
    private FilteredList<Entretien> filteredList;
    private Entretien selected;

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm");

    // ────────────────────────── INIT ──────────────────────────

    @FXML
    public void initialize() {

        colDateHeure.setCellValueFactory(c -> {
            LocalDateTime dt = c.getValue().getDateEntretien();
            return new SimpleStringProperty(dt == null ? "" : dt.format(DT_FMT));
        });

        colType.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getType()));

        colStatut.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getStatut()));

        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String statut, boolean empty) {
                super.updateItem(statut, empty);
                if (empty || statut == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(statut);
                    switch (statut) {
                        case "PLANIFIE" -> setStyle("-fx-text-fill:#2980b9; -fx-font-weight:bold;");
                        case "REALISE"  -> setStyle("-fx-text-fill:#27ae60; -fx-font-weight:bold;");
                        case "ANNULE"   -> setStyle("-fx-text-fill:#c0392b; -fx-font-weight:bold;");
                        default         -> setStyle("");
                    }
                }
            }
        });

        colLieu.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getLieu()));

        colLien.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getLien()));

        colEmail.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getEmailCandidat()));

        colNoteTech.setCellValueFactory(c -> {
            Integer n = c.getValue().getNoteTechnique();
            return new SimpleStringProperty(n == null ? "-" : n + "/20");
        });

        colNoteCom.setCellValueFactory(c -> {
            Integer n = c.getValue().getNoteCommunication();
            return new SimpleStringProperty(n == null ? "-" : n + "/20");
        });

        colScore.setCellValueFactory(c -> {
            Double s = c.getValue().getScoreFinal();
            if (s == null) return new SimpleStringProperty("-");
            return new SimpleStringProperty(s + " (" + c.getValue().getNiveau() + ")");
        });

        colScore.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null || "-".equals(val)) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(val);
                    if (val.contains("EXCELLENT"))      setStyle("-fx-text-fill:#27ae60; -fx-font-weight:bold;");
                    else if (val.contains("BON"))       setStyle("-fx-text-fill:#2980b9; -fx-font-weight:bold;");
                    else if (val.contains("MOYEN"))     setStyle("-fx-text-fill:#f39c12; -fx-font-weight:bold;");
                    else if (val.contains("INSUFFISANT")) setStyle("-fx-text-fill:#e74c3c; -fx-font-weight:bold;");
                    else setStyle("");
                }
            }
        });

        cbType.getItems().addAll("EN_LIGNE", "PRESENTIEL", "TELEPHONIQUE");
        cbStatut.getItems().addAll("PLANIFIE", "REALISE", "ANNULE");

        cbType.valueProperty().addListener((obs, o, n) -> toggleLieuLien());

        addLiveValidation();

        tvEntretiens.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldSel, newSel) -> {
                    if (newSel != null) populateForm(newSel);
                });

        cbFilterStatut.setItems(FXCollections.observableArrayList(
                "Tous", "PLANIFIE", "REALISE", "ANNULE"));
        cbFilterStatut.setValue("Tous");
        cbFilterType.setItems(FXCollections.observableArrayList(
                "Tous", "PRESENTIEL", "EN_LIGNE", "TELEPHONIQUE"));
        cbFilterType.setValue("Tous");

        cbFilterStatut.setOnAction(e -> applyFilters());
        cbFilterType.setOnAction(e -> applyFilters());

        filteredList = new FilteredList<>(masterList, p -> true);
        SortedList<Entretien> sortedList = new SortedList<>(filteredList);
        sortedList.comparatorProperty().bind(tvEntretiens.comparatorProperty());
        tvEntretiens.setItems(sortedList);

        tfSearch.textProperty().addListener((obs, o, n) -> applyFilters());

        loadData();
    }

    // ────────────────────────── FILTERS ──────────────────────────

    private void applyFilters() {
        String search = tfSearch.getText() == null ? "" : tfSearch.getText().toLowerCase().trim();
        String statut = cbFilterStatut.getValue();
        String type = cbFilterType.getValue();

        filteredList.setPredicate(e -> {
            if (statut != null && !"Tous".equals(statut)) {
                if (e.getStatut() == null || !e.getStatut().equals(statut)) return false;
            }
            if (type != null && !"Tous".equals(type)) {
                if (e.getType() == null || !e.getType().equals(type)) return false;
            }
            if (!search.isEmpty()) {
                boolean match = false;
                if (e.getEmailCandidat() != null && e.getEmailCandidat().toLowerCase().contains(search)) match = true;
                if (e.getType() != null && e.getType().toLowerCase().contains(search)) match = true;
                if (e.getStatut() != null && e.getStatut().toLowerCase().contains(search)) match = true;
                if (e.getLieu() != null && e.getLieu().toLowerCase().contains(search)) match = true;
                if (e.getCommentaire() != null && e.getCommentaire().toLowerCase().contains(search)) match = true;
                if (!match) return false;
            }
            return true;
        });
    }

    // ────────────────────────── HELPERS ──────────────────────────

    private void toggleLieuLien() {
        if ("EN_LIGNE".equals(cbType.getValue())) {
            tfLieu.setDisable(true);
            tfLieu.clear();
            tfLien.setDisable(false);
        } else if ("TELEPHONIQUE".equals(cbType.getValue())) {
            tfLieu.setDisable(true);
            tfLieu.clear();
            tfLien.setDisable(true);
            tfLien.clear();
        } else {
            tfLieu.setDisable(false);
            tfLien.setDisable(true);
            tfLien.clear();
        }
    }

    private void addLiveValidation() {
        tfEmail.textProperty().addListener((obs, o, n) -> validateEmail());
        tfHeure.textProperty().addListener((obs, o, n) -> validateHeure());
        tfNoteTech.textProperty().addListener((obs, o, n) -> { validateNote(tfNoteTech); updateScoreFromFields(); });
        tfNoteCom.textProperty().addListener((obs, o, n) -> { validateNote(tfNoteCom); updateScoreFromFields(); });
    }

    private void validateEmail() {
        String email = tfEmail.getText();
        if (email != null && email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
            setValid(tfEmail);
        else
            setInvalid(tfEmail);
    }

    private void validateHeure() {
        try {
            LocalTime.parse(tfHeure.getText());
            setValid(tfHeure);
        } catch (Exception e) {
            setInvalid(tfHeure);
        }
    }

    private void validateNote(TextField field) {
        String text = field.getText();
        if (text == null || text.isBlank()) {
            field.setStyle("");
            return;
        }
        try {
            int val = Integer.parseInt(text);
            if (val >= 0 && val <= 20) setValid(field);
            else setInvalid(field);
        } catch (Exception e) {
            setInvalid(field);
        }
    }

    private void setValid(Control c) {
        c.setStyle("-fx-border-color: #2ecc71; -fx-border-width:2; -fx-border-radius:8;");
    }

    private void setInvalid(Control c) {
        c.setStyle("-fx-border-color: #e74c3c; -fx-border-width:2; -fx-border-radius:8;");
    }

    // ────────────────────────── TABLE LOAD ──────────────────────────

    private void loadData() {
        try {
            int updated = service.autoUpdateStatuts();
            if (updated > 0) {
                System.out.println("Statut auto-update: " + updated + " entretien(s) passé(s) à REALISE.");
            }
            masterList.setAll(service.getAll());
        } catch (SQLException e) {
            DialogUtil.showError("Erreur chargement", e.getMessage());
        }
    }

    // ────────────────────────── FORM <-> ENTRETIEN ──────────────────────────

    private void populateForm(Entretien e) {
        selected = e;
        tfEmail.setText(e.getEmailCandidat());
        dpDate.setValue(e.getDateEntretien() == null ? null : e.getDateEntretien().toLocalDate());
        tfHeure.setText(e.getDateEntretien() == null ? "" :
                e.getDateEntretien().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        cbType.setValue(e.getType());
        cbStatut.setValue(e.getStatut());
        tfLieu.setText(e.getLieu());
        tfLien.setText(e.getLien());

        boolean futur = e.getDateEntretien() != null && e.getDateEntretien().isAfter(LocalDateTime.now());

        tfNoteTech.setDisable(futur);
        tfNoteCom.setDisable(futur);
        taCommentaire.setDisable(futur);
        if (btnFeedback != null) btnFeedback.setDisable(futur);

        if (futur) {
            tfNoteTech.clear();
            tfNoteCom.clear();
            taCommentaire.clear();
            tfNoteTech.setPromptText("Disponible après réalisation");
            tfNoteCom.setPromptText("Disponible après réalisation");
            taCommentaire.setPromptText("Disponible après réalisation");
        } else {
            tfNoteTech.setText(e.getNoteTechnique() == null ? "" : String.valueOf(e.getNoteTechnique()));
            tfNoteCom.setText(e.getNoteCommunication() == null ? "" : String.valueOf(e.getNoteCommunication()));
            taCommentaire.setText(e.getCommentaire());
            tfNoteTech.setPromptText("0 - 20");
            tfNoteCom.setPromptText("0 - 20");
            taCommentaire.setPromptText("Commentaire...");
        }
        updateScoreLabel(e);
    }

    private void updateScoreLabel(Entretien e) {
        Double score = e.getScoreFinal();
        displayScore(score);
    }

    private void updateScoreFromFields() {
        try {
            String techText = tfNoteTech.getText();
            String comText = tfNoteCom.getText();
            if (techText == null || techText.isBlank() || comText == null || comText.isBlank()) {
                displayScore(null);
                return;
            }
            int tech = Integer.parseInt(techText.trim());
            int com = Integer.parseInt(comText.trim());
            if (tech < 0 || tech > 20 || com < 0 || com > 20) {
                displayScore(null);
                return;
            }
            double score = Math.round((tech * 0.7 + com * 0.3) * 100.0) / 100.0;
            displayScore(score);
        } catch (NumberFormatException ex) {
            displayScore(null);
        }
    }

    private void displayScore(Double score) {
        if (score == null) {
            lblScore.setText("Score : -");
            lblScore.setStyle("-fx-text-fill: #7f8c8d; -fx-font-weight: bold; -fx-font-size: 14;");
        } else {
            String niveau;
            if (score >= 16) niveau = "EXCELLENT";
            else if (score >= 12) niveau = "BON";
            else if (score >= 8) niveau = "MOYEN";
            else niveau = "INSUFFISANT";
            lblScore.setText("Score : " + score + " / 20  —  " + niveau);
            String color = switch (niveau) {
                case "EXCELLENT" -> "#27ae60";
                case "BON" -> "#2980b9";
                case "MOYEN" -> "#f39c12";
                default -> "#e74c3c";
            };
            lblScore.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-font-size: 14;");
        }
    }

    private Entretien buildFromForm() {
        StringBuilder erreurs = new StringBuilder();
        String errStyle = "-fx-border-color: #e74c3c; -fx-border-width: 2; -fx-border-radius: 4;";
        String okStyle = "";

        tfEmail.setStyle(okStyle);
        dpDate.getEditor().setStyle(okStyle);
        tfHeure.setStyle(okStyle);
        cbType.setStyle(okStyle);
        cbStatut.setStyle(okStyle);

        String email = tfEmail.getText();
        if (email == null || email.isBlank()) {
            erreurs.append("• Email candidat obligatoire\n");
            tfEmail.setStyle(errStyle);
        } else {
            email = email.trim();
            if (!email.matches("^[\\w.%+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
                erreurs.append("• Email invalide\n");
                tfEmail.setStyle(errStyle);
            }
        }

        if (dpDate.getValue() == null) {
            erreurs.append("• Date obligatoire\n");
            dpDate.getEditor().setStyle(errStyle);
        }

        LocalTime time = null;
        if (tfHeure.getText() == null || tfHeure.getText().isBlank()) {
            erreurs.append("• Heure obligatoire\n");
            tfHeure.setStyle(errStyle);
        } else {
            try {
                time = LocalTime.parse(tfHeure.getText());
            } catch (Exception e) {
                erreurs.append("• Heure invalide (format HH:mm)\n");
                tfHeure.setStyle(errStyle);
            }
        }

        if (cbType.getValue() == null) {
            erreurs.append("• Type obligatoire\n");
            cbType.setStyle(errStyle);
        }

        if (!erreurs.isEmpty()) {
            throw new IllegalArgumentException(erreurs.toString().trim());
        }

        LocalDateTime dateTime = LocalDateTime.of(dpDate.getValue(), time);

        String statut;
        if (dateTime.isBefore(LocalDateTime.now())) {
            statut = "REALISE";
            cbStatut.setValue("REALISE");
        } else {
            if (cbStatut.getValue() == null) {
                cbStatut.setStyle(errStyle);
                throw new IllegalArgumentException("Statut obligatoire.");
            }
            statut = cbStatut.getValue();
        }

        Integer noteTech = null;
        Integer noteCom  = null;
        String commentaire = "";
        if (dateTime.isBefore(LocalDateTime.now())) {
            noteTech = parseNote(tfNoteTech);
            noteCom  = parseNote(tfNoteCom);
            commentaire = taCommentaire.getText();
        }

        // Look up candidature_id from the email
        int candidatureId;
        try {
            candidatureId = service.findCandidatureIdByEmail(email);
        } catch (java.sql.SQLException ex) {
            throw new IllegalArgumentException("Erreur recherche candidature : " + ex.getMessage());
        }
        if (candidatureId <= 0) {
            tfEmail.setStyle(errStyle);
            throw new IllegalArgumentException("Aucune candidature trouvee pour cet email.");
        }

        Entretien ent = new Entretien(
                0, candidatureId, dateTime,
                cbType.getValue(),
                tfLieu.getText(),
                tfLien.getText(),
                statut,
                noteTech, noteCom,
                commentaire,
                email
        );
        return ent;
    }

    private Integer parseNote(TextField f) {
        if (f.getText() == null || f.getText().isBlank()) return null;
        int val = Integer.parseInt(f.getText());
        if (val < 0 || val > 20)
            throw new IllegalArgumentException("Note doit etre entre 0 et 20.");
        return val;
    }

    // ────────────────────────── ACTIONS ──────────────────────────

    @FXML
    private void onAjouter() {
        try {
            Entretien e = buildFromForm();

            if (service.existeConflit(e.getDateEntretien())) {
                DialogUtil.showWarning("Conflit horaire",
                        "Un entretien existe deja a cette date/heure.\n" +
                        "Choisissez un autre creneau.");
                return;
            }

            service.add(e);
            DialogUtil.showInfo("Succes", "Entretien ajoute avec succes.");

            if (calendarService.isConfigured() && "EN_LIGNE".equals(e.getType())) {
                syncToGoogleCalendarThenEmail(e);
            } else {
                sendConfirmationEmail(e, null);
                syncToGoogleCalendar(e);
            }

            reset();
            loadData();
        } catch (Exception e) {
            DialogUtil.showError("Erreur", e.getMessage());
        }
    }

    @FXML
    private void onModifier() {
        if (selected == null) {
            DialogUtil.showWarning("Attention", "Selectionnez un entretien dans la liste.");
            return;
        }
        try {
            Entretien e = buildFromForm();
            e.setId(selected.getId());
            service.update(e);

            try {
                DecisionService ds = new DecisionService();
                ds.updateScoreByEntretienId(e.getId(), e.getScoreFinal());
                ds.autoDecideSingle(e.getId(),
                        DecisionService.getDefaultSeuilAccepte(),
                        DecisionService.getDefaultSeuilRefuse());
            } catch (Exception ignored) { }

            if (calendarService.isConfigured()) {
                calendarService.creerEvenementAsync(e).exceptionally(ex -> {
                    System.err.println("Google Calendar update: " + ex.getMessage());
                    return null;
                });
            }

            DialogUtil.showInfo("Succes", "Entretien modifie avec succes.");
            reset();
            loadData();
        } catch (Exception e) {
            DialogUtil.showError("Erreur", e.getMessage());
        }
    }

    @FXML
    private void onSupprimer() {
        if (selected == null) {
            DialogUtil.showWarning("Attention", "Selectionnez un entretien dans la liste.");
            return;
        }

        boolean confirmed = DialogUtil.confirmYesNo(
                "Confirmation",
                "Supprimer l'entretien #" + selected.getId() + " ?",
                "Cette action est irreversible."
        );
        if (!confirmed) return;

        try {
            service.delete(selected.getId());
            DialogUtil.showInfo("Succes", "Entretien supprime.");
            reset();
            loadData();
        } catch (Exception e) {
            DialogUtil.showError("Erreur", e.getMessage());
        }
    }

    @FXML private void onClear() { reset(); }
    @FXML private void onRefresh() { loadData(); }

    private boolean feedbackMode = false;

    @FXML
    private void onFeedback() {
        if (selected == null) {
            DialogUtil.showWarning("Attention", "Selectionnez un entretien dans la liste.");
            return;
        }

        if (!feedbackMode) {
            feedbackMode = true;

            tfNoteTech.setDisable(false);
            tfNoteCom.setDisable(false);
            taCommentaire.setDisable(false);
            tfNoteTech.setPromptText("Note /20");
            tfNoteCom.setPromptText("Note /20");
            taCommentaire.setPromptText("Saisissez votre feedback...");

            tfNoteTech.clear();
            tfNoteCom.clear();
            taCommentaire.clear();

            cbStatut.setValue("REALISE");

            btnFeedback.setText("Valider Feedback");
            btnFeedback.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");

            DialogUtil.showInfo("Mode Feedback",
                    "Les champs notes et commentaire sont maintenant actifs.\n" +
                    "Saisissez vos notes puis cliquez sur 'Valider Feedback'.");
            return;
        }

        try {
            Integer noteTech = parseNote(tfNoteTech);
            Integer noteCom  = parseNote(tfNoteCom);

            if (noteTech == null && noteCom == null && (taCommentaire.getText() == null || taCommentaire.getText().isBlank())) {
                DialogUtil.showWarning("Feedback vide",
                        "Remplissez au moins une note ou un commentaire.");
                return;
            }

            selected.setNoteTechnique(noteTech);
            selected.setNoteCommunication(noteCom);
            selected.setCommentaire(taCommentaire.getText());
            selected.setStatut("REALISE");

            service.update(selected);

            Double score = selected.getScoreFinal();
            DecisionService decisionService = new DecisionService();
            try {
                decisionService.updateScoreByEntretienId(selected.getId(), score);
            } catch (Exception ignored) { }

            String autoDecision = null;
            try {
                autoDecision = decisionService.autoDecideSingle(
                        selected.getId(),
                        DecisionService.getDefaultSeuilAccepte(),
                        DecisionService.getDefaultSeuilRefuse());
            } catch (Exception ignored) { }

            String niveau = selected.getNiveau();
            String autoMsg = "";
            if (autoDecision != null && !"EN_ATTENTE".equals(autoDecision)) {
                autoMsg = "\n\nDécision automatique : " + autoDecision +
                          "\nEmail envoyé au candidat.";
            }
            DialogUtil.showInfo("Feedback enregistré",
                    "Statut -> RÉALISÉ\n" +
                    "Score final : " + (score != null ? String.format("%.1f", score) + " / 20" : "-") + "\n" +
                    "Niveau : " + niveau + autoMsg);

            feedbackMode = false;
            btnFeedback.setText("Ajouter feedback + REALISE");
            btnFeedback.setStyle("");
            reset();
            loadData();
        } catch (Exception e) {
            DialogUtil.showError("Erreur", e.getMessage());
        }
    }

    // ────────────────────────── RESET ──────────────────────────

    private void reset() {
        selected = null;
        tvEntretiens.getSelectionModel().clearSelection();
        tfEmail.clear();     tfEmail.setStyle("");
        dpDate.setValue(null);
        tfHeure.clear();     tfHeure.setStyle("");
        cbType.setValue(null);
        cbStatut.setValue(null);
        tfLieu.clear();      tfLieu.setDisable(false);
        tfLien.clear();      tfLien.setDisable(false);
        tfNoteTech.clear();  tfNoteTech.setStyle("");  tfNoteTech.setDisable(false);  tfNoteTech.setPromptText("0 - 20");
        tfNoteCom.clear();   tfNoteCom.setStyle("");   tfNoteCom.setDisable(false);   tfNoteCom.setPromptText("0 - 20");
        taCommentaire.clear(); taCommentaire.setDisable(false); taCommentaire.setPromptText("Commentaire...");
        if (btnFeedback != null) {
            btnFeedback.setDisable(false);
            btnFeedback.setText("Ajouter feedback + REALISE");
            btnFeedback.setStyle("-fx-font-size:14;");
        }
        feedbackMode = false;
        lblScore.setText("Score : -");
        lblScore.setStyle("-fx-text-fill: #7f8c8d; -fx-font-weight: bold; -fx-font-size: 14;");
    }

    // ────────────────────────── GOOGLE CALENDAR ──────────────────────────

    private void syncToGoogleCalendar(Entretien e) {
        if (!calendarService.isConfigured()) {
            System.out.println("Google Calendar: credentials.json non configuré, skip.");
            return;
        }

        calendarService.creerEvenementAsync(e)
                .thenAccept(result -> {
                    String[] parts = result.split("\\|", 2);
                    System.out.println("Google Calendar: événement créé, ID=" + parts[0]);
                    javafx.application.Platform.runLater(() ->
                        DialogUtil.showInfo("Google Calendar",
                                "Événement créé avec succès !\n" +
                                "Invitation envoyée à " + e.getEmailCandidat())
                    );
                })
                .exceptionally(ex -> {
                    System.err.println("Google Calendar sync: " + ex.getMessage());
                    return null;
                });
    }

    private void syncToGoogleCalendarThenEmail(Entretien e) {
        calendarService.creerEvenementAsync(e)
                .thenAccept(result -> {
                    String[] parts = result.split("\\|", 2);
                    String eventId = parts[0];
                    String meetLink = parts.length > 1 && !parts[1].isEmpty() ? parts[1] : null;

                    System.out.println("Google Calendar: événement créé, ID=" + eventId);
                    if (meetLink != null) {
                        System.out.println("Meet link récupéré: " + meetLink);
                    }

                    javafx.application.Platform.runLater(() -> {
                        sendConfirmationEmail(e, meetLink);
                        DialogUtil.showInfo("Google Calendar + Email",
                                "Événement créé avec succès !\n" +
                                "Lien Meet : " + (meetLink != null ? meetLink : "N/A") + "\n" +
                                "Email envoyé à " + e.getEmailCandidat() + " avec le lien Meet.");
                    });
                })
                .exceptionally(ex -> {
                    System.err.println("Google Calendar sync: " + ex.getMessage());
                    javafx.application.Platform.runLater(() -> sendConfirmationEmail(e, null));
                    return null;
                });
    }

    // ────────────────────────── EMAIL ──────────────────────────

    private void sendConfirmationEmail(Entretien e, String meetLink) {
        if (e.getEmailCandidat() == null || e.getEmailCandidat().isBlank()) return;

        String subject = "TalentFlow - Entretien planifie le " +
                e.getDateEntretien().format(DT_FMT);

        String meetSection = "";
        if ("EN_LIGNE".equals(e.getType()) && meetLink != null && !meetLink.isBlank()) {
            meetSection = """
                    <tr><td style="padding:6px; font-weight:bold;">Lien Google Meet</td>
                        <td style="padding:6px;">
                          <a href="%s" style="color:#1a73e8; font-weight:bold; font-size:15px;"
                             target="_blank">%s</a>
                        </td></tr>
                    """.formatted(meetLink, meetLink);
        }

        String lieuOuLien = "EN_LIGNE".equals(e.getType())
                ? (e.getLien() != null && !e.getLien().isBlank() ? e.getLien() : "En ligne")
                : (e.getLieu() != null ? e.getLieu() : "Non précisé");

        String body = """
                <div style="font-family:Arial,sans-serif; max-width:600px; margin:auto;
                            border:1px solid #e0e0e0; border-radius:8px; padding:24px;">
                  <h2 style="color:#1a237e;">TalentFlow - Confirmation d'entretien</h2>
                  <p>Bonjour,</p>
                  <p>Votre entretien a été planifié avec les détails suivants :</p>
                  <table style="border-collapse:collapse; width:100%%; background:#f8f9fa;
                              border-radius:6px;">
                    <tr><td style="padding:10px; font-weight:bold;">Date / Heure</td>
                        <td style="padding:10px;">%s</td></tr>
                    <tr><td style="padding:10px; font-weight:bold;">Type</td>
                        <td style="padding:10px;">%s</td></tr>
                    <tr><td style="padding:10px; font-weight:bold;">Lieu / Lien</td>
                        <td style="padding:10px;">%s</td></tr>
                    %s
                  </table>
                  %s
                  <p style="margin-top:18px;">Cordialement,<br><strong>Équipe TalentFlow</strong></p>
                </div>
                """.formatted(
                e.getDateEntretien().format(DT_FMT),
                e.getType(),
                lieuOuLien,
                meetSection,
                ("EN_LIGNE".equals(e.getType()) && meetLink != null && !meetLink.isBlank())
                    ? "<p style='margin-top:14px; padding:12px; background:#e8f5e9; border-radius:6px;'>"
                      + "Rejoignez l'entretien : "
                      + "<a href='" + meetLink + "' style='color:#1a73e8; font-size:16px;'>" + meetLink + "</a></p>"
                    : ""
        );

        emailService.sendHtmlAsync(e.getEmailCandidat(), subject, body)
                .thenRun(() -> System.out.println("Email envoyé à " + e.getEmailCandidat()
                        + (meetLink != null ? " avec lien Meet" : "")))
                .exceptionally(ex -> {
                    System.err.println("Email non envoyé: " + ex.getMessage());
                    return null;
                });
    }
}
