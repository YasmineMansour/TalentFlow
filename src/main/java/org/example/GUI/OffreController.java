package org.example.GUI;

import org.example.model.Offre;
import org.example.services.OffreService;
import org.example.services.GeocodingService;
import org.example.services.TranslationService;
import org.example.services.TranslationService.Langue;
import org.example.services.CurrencyService;
import org.example.services.CurrencyService.Devise;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import org.example.utils.LanguageManager;

public class OffreController implements Initializable {

    // --- Form fields ---
    @FXML private TextField tfTitre, tfLocalisation;
    @FXML private TextArea taDescription;
    @FXML private ComboBox<String> comboStatut;
    @FXML private ComboBox<String> comboTypeContrat;
    @FXML private ComboBox<String> comboModeTravail;
    @FXML private TextField tfSalaireMin, tfSalaireMax;
    @FXML private Label lblLocValidation;

    // --- Table ---
    @FXML private TableView<Offre> tableOffres;
    @FXML private TableColumn<Offre, String> colTitre, colDesc, colLoc, colStatut, colContrat, colMode;
    @FXML private TableColumn<Offre, String> colSalaire;
    @FXML private TableColumn<Offre, String> colClassement;
    @FXML private TableColumn<Offre, String> colCoherence;

    // --- Search / Sort ---
    @FXML private TextField tfRecherche;
    @FXML private ComboBox<String> comboTri;

    // --- Translation ---
    @FXML private ComboBox<Langue> comboLangueSource;
    @FXML private ComboBox<Langue> comboLangueCible;
    @FXML private Label lblTraduction;
    @FXML private TitledPane paneTraduction;
    @FXML private Label lblTitreTraduit;
    @FXML private TextArea taDescriptionTraduite;
    @FXML private Label lblLocTraduite;

    // --- Currency conversion ---
    @FXML private ComboBox<Devise> comboDeviseSource;
    @FXML private TitledPane paneDevises;
    @FXML private TextArea taConversionResult;
    @FXML private Label lblConversionStatus;

    // --- Status bar ---
    @FXML private Label lblCount;

    private final OffreService service = new OffreService();
    private final GeocodingService geoService = new GeocodingService();
    private final TranslationService translationService = new TranslationService();
    private final CurrencyService currencyService = new CurrencyService();

    private boolean localisationValide = false;
    private String dernièreLocValidée = "";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        comboStatut.setItems(FXCollections.observableArrayList("PUBLISHED", "CLOSED", "ARCHIVED"));
        comboTypeContrat.setItems(FXCollections.observableArrayList("CDI", "CDD", "Stage", "Freelance", "Alternance"));
        comboModeTravail.setItems(FXCollections.observableArrayList("ON_SITE", "REMOTE", "HYBRID"));
        comboTri.setItems(FXCollections.observableArrayList(LanguageManager.get("offre.sort.titre"), LanguageManager.get("offre.sort.loc"), LanguageManager.get("offre.sort.statut"), LanguageManager.get("offre.sort.date"), LanguageManager.get("offre.sort.score")));

        comboDeviseSource.setItems(FXCollections.observableArrayList(Devise.values()));
        comboDeviseSource.setValue(Devise.TND);

        comboLangueSource.setItems(FXCollections.observableArrayList(Langue.values()));
        comboLangueCible.setItems(FXCollections.observableArrayList(Langue.values()));
        comboLangueSource.setValue(Langue.FRANCAIS);
        comboLangueCible.setValue(Langue.ANGLAIS);

        // Column bindings
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
        colLoc.setCellValueFactory(new PropertyValueFactory<>("localisation"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colContrat.setCellValueFactory(new PropertyValueFactory<>("typeContrat"));
        colMode.setCellValueFactory(new PropertyValueFactory<>("modeTravail"));
        colSalaire.setCellValueFactory(new PropertyValueFactory<>("salaireRange"));
        colClassement.setCellValueFactory(new PropertyValueFactory<>("classement"));
        colCoherence.setCellValueFactory(new PropertyValueFactory<>("coherence"));

        // Color-coded coherence column
        colCoherence.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null); setStyle("");
                } else {
                    setText(item);
                    if (item.contains("Faible")) setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold;");
                    else if (item.contains("Compens\u00e9e")) setStyle("-fx-text-fill: #D97706; -fx-font-weight: bold;");
                    else if (item.contains("Acceptable")) setStyle("-fx-text-fill: #2563EB; -fx-font-weight: bold;");
                    else if (item.contains("Excellente")) setStyle("-fx-text-fill: #059669; -fx-font-weight: bold;");
                    else setStyle("");
                }
            }
        });

        // Color-coded classement column
        colClassement.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null); setStyle("");
                } else {
                    setText(item);
                    if (item.contains("Or")) setStyle("-fx-text-fill: #D97706; -fx-font-weight: bold;");
                    else if (item.contains("Argent")) setStyle("-fx-text-fill: #6B7280; -fx-font-weight: bold;");
                    else setStyle("-fx-text-fill: #B45309; -fx-font-weight: bold;");
                }
            }
        });

        // Color-coded statut column
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null); setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "PUBLISHED" -> setStyle("-fx-text-fill: #059669; -fx-font-weight: bold;");
                        case "CLOSED"    -> setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold;");
                        case "ARCHIVED"  -> setStyle("-fx-text-fill: #94A3B8; -fx-font-style: italic;");
                        default          -> setStyle("");
                    }
                }
            }
        });

        // Salary fields: numbers only
        tfSalaireMin.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*\\.?\\d*")) tfSalaireMin.setText(oldVal);
        });
        tfSalaireMax.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*\\.?\\d*")) tfSalaireMax.setText(oldVal);
        });

        // Location validation listener
        tfLocalisation.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.trim().equals(derni\u00e8reLocValid\u00e9e)) {
                localisationValide = false;
                if (newVal.trim().isEmpty()) {
                    lblLocValidation.setText("");
                    lblLocValidation.setStyle("-fx-font-size: 11px;");
                } else if (newVal.trim().length() < 2) {
                    lblLocValidation.setText(LanguageManager.get("offre.loc.short.hint"));
                    lblLocValidation.setStyle("-fx-text-fill: #D97706; -fx-font-size: 11px;");
                } else if (!newVal.matches("[a-zA-Z\u00c0-\u00ff\\s\\-',\\.]+")) {
                    lblLocValidation.setText(LanguageManager.get("offre.loc.chars.hint"));
                    lblLocValidation.setStyle("-fx-text-fill: #D97706; -fx-font-size: 11px;");
                } else {
                    lblLocValidation.setText(LanguageManager.get("offre.loc.hint"));
                    lblLocValidation.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 11px;");
                }
            }
        });

        // Geocode on focus lost
        tfLocalisation.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                String loc = tfLocalisation.getText();
                if (loc != null && !loc.trim().isEmpty() && loc.trim().length() >= 2
                        && !loc.trim().equals(derni\u00e8reLocValid\u00e9e)) {
                    validerLocalisationAsync(loc.trim());
                }
            }
        });

        rafraichir();
    }

    // ══════════════════════════════════════════════════════════
    //  NAVIGATION (adapted for TalentFlow contentArea)
    // ══════════════════════════════════════════════════════════

    private void navigateToView(String fxmlPath, Node currentNode) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            StackPane contentArea = (StackPane) currentNode.getScene().lookup(".content-area");
            if (contentArea != null) {
                contentArea.getChildren().clear();
                contentArea.getChildren().add(view);
                if (view instanceof javafx.scene.layout.Region region) {
                    region.prefWidthProperty().bind(contentArea.widthProperty());
                    region.prefHeightProperty().bind(contentArea.heightProperty());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            afficherAlerte(Alert.AlertType.ERROR, LanguageManager.get("offre.error.navigation"), e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════
    //  VALIDATION
    // ══════════════════════════════════════════════════════════

    private void validerLocalisationAsync(String location) {
        lblLocValidation.setText(LanguageManager.get("offre.loc.verifying"));
        lblLocValidation.setStyle("-fx-text-fill: #2563EB; -fx-font-size: 11px;");

        Task<double[]> task = new Task<>() {
            @Override protected double[] call() { return geoService.geocode(location); }
        };

        task.setOnSucceeded(e -> {
            double[] coords = task.getValue();
            if (coords != null) {
                localisationValide = true;
                derni\u00e8reLocValid\u00e9e = location;
                lblLocValidation.setText(LanguageManager.get("offre.loc.valid.coords").replace("{0}", String.format("%.4f, %.4f", coords[0], coords[1])));
                lblLocValidation.setStyle("-fx-text-fill: #059669; -fx-font-size: 11px; -fx-font-weight: bold;");
            } else {
                localisationValide = false;
                derni\u00e8reLocValid\u00e9e = "";
                lblLocValidation.setText(LanguageManager.get("offre.loc.notfound.hint"));
                lblLocValidation.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 11px; -fx-font-weight: bold;");
            }
        });

        task.setOnFailed(e -> {
            localisationValide = false;
            derni\u00e8reLocValid\u00e9e = "";
            lblLocValidation.setText(LanguageManager.get("offre.loc.verify.error"));
            lblLocValidation.setStyle("-fx-text-fill: #D97706; -fx-font-size: 11px;");
        });

        new Thread(task).start();
    }

    private boolean validerFormulaire() {
        StringBuilder erreurs = new StringBuilder();

        if (tfTitre.getText() == null || tfTitre.getText().trim().isEmpty())
            erreurs.append("\u2022 " + LanguageManager.get("offre.valid.titre") + "\n");
        else if (tfTitre.getText().trim().length() < 3)
            erreurs.append("\u2022 " + LanguageManager.get("offre.valid.titre.min") + "\n");
        else if (tfTitre.getText().trim().length() > 100)
            erreurs.append("\u2022 " + LanguageManager.get("offre.valid.titre.max") + "\n");

        if (taDescription.getText() == null || taDescription.getText().trim().isEmpty())
            erreurs.append("\u2022 " + LanguageManager.get("offre.valid.desc") + "\n");
        else if (taDescription.getText().trim().length() < 10)
            erreurs.append("\u2022 " + LanguageManager.get("offre.valid.desc.min") + "\n");

        String loc = tfLocalisation.getText();
        if (loc == null || loc.trim().isEmpty()) {
            erreurs.append("\u2022 " + LanguageManager.get("offre.valid.loc") + "\n");
        } else if (loc.trim().length() < 2) {
            erreurs.append("\u2022 " + LanguageManager.get("offre.valid.loc.min") + "\n");
        } else if (!loc.matches("[a-zA-Z\u00c0-\u00ff\\s\\-',\\.]+")) {
            erreurs.append("\u2022 " + LanguageManager.get("offre.valid.loc.invalid") + "\n");
        } else if (!localisationValide) {
            double[] coords = geoService.geocode(loc.trim());
            if (coords != null) {
                localisationValide = true;
                derni\u00e8reLocValid\u00e9e = loc.trim();
                lblLocValidation.setText(LanguageManager.get("offre.loc.valid.coords").replace("{0}", String.format("%.4f, %.4f", coords[0], coords[1])));
                lblLocValidation.setStyle("-fx-text-fill: #059669; -fx-font-size: 11px; -fx-font-weight: bold;");
            } else {
                erreurs.append("\u2022 " + LanguageManager.get("offre.valid.loc.notfound").replace("{0}", loc.trim()) + "\n");
            }
        }

        if (comboStatut.getValue() == null) erreurs.append("\u2022 " + LanguageManager.get("offre.valid.statut") + "\n");
        if (comboTypeContrat.getValue() == null) erreurs.append("\u2022 " + LanguageManager.get("offre.valid.contrat") + "\n");
        if (comboModeTravail.getValue() == null) erreurs.append("\u2022 " + LanguageManager.get("offre.valid.mode") + "\n");

        double salMin = parseSalaire(tfSalaireMin.getText());
        double salMax = parseSalaire(tfSalaireMax.getText());
        if (salMin < 0) erreurs.append("\u2022 " + LanguageManager.get("offre.valid.salaire.min") + "\n");
        if (salMax < 0) erreurs.append("\u2022 " + LanguageManager.get("offre.valid.salaire.max") + "\n");
        if (salMin > 0 && salMax > 0 && salMin > salMax)
            erreurs.append("\u2022 " + LanguageManager.get("offre.valid.salaire.order") + "\n");

        if (erreurs.length() > 0) {
            afficherAlerte(Alert.AlertType.WARNING, LanguageManager.get("offre.valid.title"), erreurs.toString());
            return false;
        }
        return true;
    }

    private double parseSalaire(String text) {
        if (text == null || text.trim().isEmpty()) return 0;
        try { return Double.parseDouble(text.trim()); }
        catch (NumberFormatException e) { return 0; }
    }

    private Offre buildOffreFromForm() {
        Offre o = new Offre();
        o.setTitre(tfTitre.getText().trim());
        o.setDescription(taDescription.getText().trim());
        o.setLocalisation(tfLocalisation.getText().trim());
        o.setStatut(comboStatut.getValue());
        o.setTypeContrat(comboTypeContrat.getValue());
        o.setModeTravail(comboModeTravail.getValue());
        o.setSalaireMin(parseSalaire(tfSalaireMin.getText()));
        o.setSalaireMax(parseSalaire(tfSalaireMax.getText()));
        o.setActive(true);
        return o;
    }

    // ══════════════════════════════════════════════════════════
    //  CRUD ACTIONS
    // ══════════════════════════════════════════════════════════
    @FXML
    private void handleAjouter() {
        if (!validerFormulaire()) return;
        try {
            if (service.titreExiste(tfTitre.getText().trim(), 0)) {
                afficherAlerte(Alert.AlertType.WARNING, LanguageManager.get("offre.duplicate"),
                        LanguageManager.get("offre.duplicate.msg.choose"));
                return;
            }
            Offre o = buildOffreFromForm();
            service.ajouter(o);
            rafraichir();
            nettoyer();
            afficherAlerte(Alert.AlertType.INFORMATION, LanguageManager.get("common.success"), LanguageManager.get("offre.success.add"));
        } catch (SQLException e) {
            e.printStackTrace();
            afficherAlerte(Alert.AlertType.ERROR, LanguageManager.get("offre.error.db"), LanguageManager.get("offre.error.detail").replace("{0}", e.getMessage()));
        }
    }

    @FXML
    private void handleModifier() {
        Offre selection = tableOffres.getSelectionModel().getSelectedItem();
        if (selection == null) {
            afficherAlerte(Alert.AlertType.WARNING, LanguageManager.get("offre.select.required"), LanguageManager.get("offre.select.msg"));
            return;
        }
        if (!validerFormulaire()) return;
        if (!confirmerAction(LanguageManager.get("offre.confirm.edit"))) return;

        try {
            if (service.titreExiste(tfTitre.getText().trim(), selection.getId())) {
                afficherAlerte(Alert.AlertType.WARNING, LanguageManager.get("offre.duplicate"),
                        LanguageManager.get("offre.duplicate.msg.other"));
                return;
            }
            selection.setTitre(tfTitre.getText().trim());
            selection.setDescription(taDescription.getText().trim());
            selection.setLocalisation(tfLocalisation.getText().trim());
            selection.setStatut(comboStatut.getValue());
            selection.setTypeContrat(comboTypeContrat.getValue());
            selection.setModeTravail(comboModeTravail.getValue());
            selection.setSalaireMin(parseSalaire(tfSalaireMin.getText()));
            selection.setSalaireMax(parseSalaire(tfSalaireMax.getText()));

            service.modifier(selection);
            tableOffres.refresh();
            rafraichir();
            nettoyer();
            afficherAlerte(Alert.AlertType.INFORMATION, LanguageManager.get("common.success"), LanguageManager.get("offre.success.edit"));
        } catch (SQLException e) {
            e.printStackTrace();
            afficherAlerte(Alert.AlertType.ERROR, LanguageManager.get("offre.error.edit"), e.getMessage());
        }
    }

    @FXML
    private void handleSupprimer() {
        Offre selection = tableOffres.getSelectionModel().getSelectedItem();
        if (selection == null) {
            afficherAlerte(Alert.AlertType.WARNING, LanguageManager.get("offre.select.required"), LanguageManager.get("offre.select.delete"));
            return;
        }
        if (!confirmerAction(LanguageManager.get("offre.confirm.delete.full").replace("{0}", selection.getTitre()))) return;

        try {
            service.supprimer(selection.getId());
            rafraichir();
            nettoyer();
            afficherAlerte(Alert.AlertType.INFORMATION, LanguageManager.get("common.success"), LanguageManager.get("offre.success.delete"));
        } catch (SQLException e) {
            e.printStackTrace();
            afficherAlerte(Alert.AlertType.ERROR, LanguageManager.get("offre.error.delete"), e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════
    //  NAVIGATION
    // ══════════════════════════════════════════════════════════
    @FXML
    private void ouvrirAvantages() {
        Offre selection = tableOffres.getSelectionModel().getSelectedItem();
        if (selection == null) {
            afficherAlerte(Alert.AlertType.WARNING, LanguageManager.get("offre.select.required"),
                    LanguageManager.get("offre.select.avantages"));
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/AvantagesView.fxml"));
            Parent view = loader.load();
            AvantageController controller = loader.getController();
            controller.setOffre(selection);

            StackPane contentArea = (StackPane) tableOffres.getScene().lookup(".content-area");
            if (contentArea != null) {
                contentArea.getChildren().clear();
                contentArea.getChildren().add(view);
                if (view instanceof javafx.scene.layout.Region region) {
                    region.prefWidthProperty().bind(contentArea.widthProperty());
                    region.prefHeightProperty().bind(contentArea.heightProperty());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            afficherAlerte(Alert.AlertType.ERROR, LanguageManager.get("common.error"), LanguageManager.get("offre.error.load.avantages").replace("{0}", e.getMessage()));
        }
    }

    @FXML
    private void ouvrirCarte() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/MapView.fxml"));
            Parent view = loader.load();

            Offre selection = tableOffres.getSelectionModel().getSelectedItem();
            if (selection != null) {
                MapController mapCtrl = loader.getController();
                mapCtrl.setOffre(selection);
            }

            StackPane contentArea = (StackPane) tableOffres.getScene().lookup(".content-area");
            if (contentArea != null) {
                contentArea.getChildren().clear();
                contentArea.getChildren().add(view);
                if (view instanceof javafx.scene.layout.Region region) {
                    region.prefWidthProperty().bind(contentArea.widthProperty());
                    region.prefHeightProperty().bind(contentArea.heightProperty());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            afficherAlerte(Alert.AlertType.ERROR, LanguageManager.get("common.error"), LanguageManager.get("offre.error.load.map").replace("{0}", e.getMessage()));
        }
    }

    // ══════════════════════════════════════════════════════════
    //  TRADUCTION AUTOMATIQUE
    // ══════════════════════════════════════════════════════════
    @FXML
    private void handleTraduire() {
        String titre = tfTitre.getText();
        String description = taDescription.getText();
        String localisation = tfLocalisation.getText();

        if ((titre == null || titre.isBlank()) && (description == null || description.isBlank())) {
            afficherAlerte(Alert.AlertType.WARNING, LanguageManager.get("offre.translate.nothing"),
                    LanguageManager.get("offre.translate.nothing.msg"));
            return;
        }

        Langue source = comboLangueSource.getValue();
        Langue cible = comboLangueCible.getValue();

        if (source == null || cible == null) {
            afficherAlerte(Alert.AlertType.WARNING, LanguageManager.get("offre.translate.lang.missing"),
                    LanguageManager.get("offre.translate.lang.missing.msg"));
            return;
        }

        if (source == cible) {
            afficherAlerte(Alert.AlertType.INFORMATION, LanguageManager.get("offre.translate.same.lang"),
                    LanguageManager.get("offre.translate.same.lang.msg"));
            return;
        }

        lblTraduction.setText(LanguageManager.get("offre.translate.progress").replace("{0}", source.getLabel()).replace("{1}", cible.getLabel()));
        lblTraduction.setStyle("-fx-text-fill: #2563EB; -fx-font-size: 11px;");

        Task<String[]> task = new Task<>() {
            @Override
            protected String[] call() {
                return translationService.traduireOffre(
                        titre != null ? titre.trim() : "",
                        description != null ? description.trim() : "",
                        localisation != null ? localisation.trim() : "",
                        source, cible
                );
            }
        };

        task.setOnSucceeded(e -> {
            String[] result = task.getValue();
            if (result != null) {
                lblTitreTraduit.setText(result[0]);
                taDescriptionTraduite.setText(result[1]);
                lblLocTraduite.setText(result[2]);
                paneTraduction.setExpanded(true);
                lblTraduction.setText(LanguageManager.get("offre.translate.success").replace("{0}", source.getLabel()).replace("{1}", cible.getLabel()));
                lblTraduction.setStyle("-fx-text-fill: #059669; -fx-font-size: 11px; -fx-font-weight: bold;");
            } else {
                lblTraduction.setText(LanguageManager.get("offre.translate.failed"));
                lblTraduction.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 11px; -fx-font-weight: bold;");
            }
        });

        task.setOnFailed(e -> {
            lblTraduction.setText(LanguageManager.get("offre.translate.error").replace("{0}", task.getException().getMessage()));
            lblTraduction.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 11px;");
        });

        new Thread(task).start();
    }

    // ══════════════════════════════════════════════════════════
    //  CONVERSION MULTI-DEVISES
    // ══════════════════════════════════════════════════════════
    @FXML
    private void handleConvertirDevise() {
        double salMin = parseSalaire(tfSalaireMin.getText());
        double salMax = parseSalaire(tfSalaireMax.getText());

        if (salMin == 0 && salMax == 0) {
            Offre selection = tableOffres.getSelectionModel().getSelectedItem();
            if (selection != null) {
                salMin = selection.getSalaireMin();
                salMax = selection.getSalaireMax();
                if (salMin > 0) tfSalaireMin.setText(String.valueOf(salMin));
                if (salMax > 0) tfSalaireMax.setText(String.valueOf(salMax));
            }
        }

        if (salMin == 0 && salMax == 0) {
            afficherAlerte(Alert.AlertType.WARNING, LanguageManager.get("offre.currency.none"),
                    LanguageManager.get("offre.currency.none.msg"));
            return;
        }

        Devise source = comboDeviseSource.getValue();
        if (source == null) { source = Devise.TND; comboDeviseSource.setValue(source); }

        if (lblConversionStatus != null) {
            lblConversionStatus.setText(LanguageManager.get("offre.currency.progress"));
            lblConversionStatus.setStyle("-fx-text-fill: #2563EB; -fx-font-size: 11px;");
        }
        if (taConversionResult != null) taConversionResult.setText("");
        if (paneDevises != null) paneDevises.setExpanded(true);

        final double fMin = salMin, fMax = salMax;
        final Devise fSource = source;

        Task<CurrencyService.ConversionResult> task = new Task<>() {
            @Override protected CurrencyService.ConversionResult call() {
                return currencyService.convertir(fMin, fMax, fSource);
            }
        };

        task.setOnSucceeded(e -> {
            CurrencyService.ConversionResult convResult = task.getValue();
            if (convResult == null) {
                if (lblConversionStatus != null) {
                    lblConversionStatus.setText(LanguageManager.get("offre.currency.empty"));
                    lblConversionStatus.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 11px; -fx-font-weight: bold;");
                }
                return;
            }
            if (taConversionResult != null) taConversionResult.setText(convResult.toDisplayText());
            if (lblConversionStatus != null) {
                if (convResult.getErrorMessage() != null) {
                    lblConversionStatus.setText("\u274c " + convResult.getErrorMessage());
                    lblConversionStatus.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 11px; -fx-font-weight: bold;");
                } else if (convResult.getConversions().isEmpty()) {
                    lblConversionStatus.setText(LanguageManager.get("offre.currency.rates.error"));
                    lblConversionStatus.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 11px; -fx-font-weight: bold;");
                } else {
                    lblConversionStatus.setText(LanguageManager.get("offre.currency.success").replace("{0}", String.valueOf(convResult.getConversions().size())));
                    lblConversionStatus.setStyle("-fx-text-fill: #059669; -fx-font-size: 11px; -fx-font-weight: bold;");
                }
            }
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            String msg = ex != null ? ex.getMessage() : LanguageManager.get("offre.currency.error.unknown");
            if (lblConversionStatus != null) {
                lblConversionStatus.setText(LanguageManager.get("offre.currency.error").replace("{0}", msg));
                lblConversionStatus.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 11px;");
            }
            if (taConversionResult != null) taConversionResult.setText(LanguageManager.get("offre.currency.error.detail").replace("{0}", msg));
            if (ex != null) ex.printStackTrace();
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    // ══════════════════════════════════════════════════════════
    //  SEARCH / SORT / STATS
    // ══════════════════════════════════════════════════════════
    @FXML
    private void chargerSelection() {
        Offre selection = tableOffres.getSelectionModel().getSelectedItem();
        if (selection != null) {
            tfTitre.setText(selection.getTitre());
            taDescription.setText(selection.getDescription());
            tfLocalisation.setText(selection.getLocalisation());
            comboStatut.setValue(selection.getStatut());
            comboTypeContrat.setValue(selection.getTypeContrat());
            comboModeTravail.setValue(selection.getModeTravail());
            tfSalaireMin.setText(selection.getSalaireMin() > 0 ? String.valueOf(selection.getSalaireMin()) : "");
            tfSalaireMax.setText(selection.getSalaireMax() > 0 ? String.valueOf(selection.getSalaireMax()) : "");
            localisationValide = true;
            derni\u00e8reLocValid\u00e9e = selection.getLocalisation();
            lblLocValidation.setText(LanguageManager.get("offre.loc.existing"));
            lblLocValidation.setStyle("-fx-text-fill: #059669; -fx-font-size: 11px; -fx-font-weight: bold;");
        }
    }

    @FXML
    private void handleRechercher() {
        try {
            List<Offre> results = service.rechercher(tfRecherche.getText());
            appliquerClassement(results);
            tableOffres.setItems(FXCollections.observableArrayList(results));
            updateCount(results.size());
        } catch (SQLException e) {
            e.printStackTrace();
            afficherAlerte(Alert.AlertType.ERROR, LanguageManager.get("offre.error.search"), e.getMessage());
        }
    }

    @FXML
    private void handleAfficherTout() {
        rafraichir();
        if (tfRecherche != null) tfRecherche.clear();
    }

    @FXML
    private void handleStats() {
        navigateToView("/org/example/OffreStatistiquesView.fxml", tableOffres);
    }

    @FXML
    private void handleTri() {
        try {
            String choix = comboTri.getValue();

            if (LanguageManager.get("offre.sort.score").equals(choix)) {
                List<Offre> offres = service.afficher();
                Map<Integer, Integer> scores = service.scoresAttractivite();
                for (Offre o : offres) {
                    int score = scores.getOrDefault(o.getId(), 0);
                    if (score >= 70) o.setClassement("\ud83e\udd47 Or (" + score + ")");
                    else if (score >= 40) o.setClassement("\ud83e\udd48 Argent (" + score + ")");
                    else o.setClassement("\ud83e\udd49 Bronze (" + score + ")");
                }
                offres.sort((a, b) -> {
                    int sa = scores.getOrDefault(a.getId(), 0);
                    int sb = scores.getOrDefault(b.getId(), 0);
                    return Integer.compare(sb, sa);
                });
                tableOffres.setItems(FXCollections.observableArrayList(offres));
                updateCount(offres.size());
                return;
            }

            String colonne = "id";
            if (LanguageManager.get("offre.sort.titre").equals(choix)) colonne = "titre";
            else if (LanguageManager.get("offre.sort.loc").equals(choix)) colonne = "localisation";
            else if (LanguageManager.get("offre.sort.statut").equals(choix)) colonne = "statut";

            List<Offre> sorted = service.trierPar(colonne);
            appliquerClassement(sorted);
            tableOffres.setItems(FXCollections.observableArrayList(sorted));
            updateCount(sorted.size());
        } catch (SQLException e) {
            e.printStackTrace();
            afficherAlerte(Alert.AlertType.ERROR, LanguageManager.get("offre.error.sort"), e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════
    //  UTILITIES
    // ══════════════════════════════════════════════════════════
    private boolean confirmerAction(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(LanguageManager.get("offre.confirm.title"));
        alert.setHeaderText(null);
        alert.setContentText(message);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void afficherAlerte(Alert.AlertType type, String titre, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void rafraichir() {
        try {
            List<Offre> offres = service.afficher();
            appliquerClassement(offres);
            tableOffres.setItems(FXCollections.observableArrayList(offres));
            updateCount(offres.size());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void appliquerClassement(List<Offre> offres) {
        try {
            Map<Integer, Integer> scores = service.scoresAttractivite();
            Map<Integer, String> coherences = service.indicesCoherence();
            for (Offre o : offres) {
                int score = scores.getOrDefault(o.getId(), 0);
                if (score >= 70) o.setClassement("\ud83e\udd47 Or (" + score + ")");
                else if (score >= 40) o.setClassement("\ud83e\udd48 Argent (" + score + ")");
                else o.setClassement("\ud83e\udd49 Bronze (" + score + ")");
                o.setCoherence(coherences.getOrDefault(o.getId(), "\u2014"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateCount(int count) {
        if (lblCount != null) {
            lblCount.setText(LanguageManager.get("offre.count").replace("{0}", String.valueOf(count)));
        }
    }

    private void nettoyer() {
        tfTitre.clear();
        taDescription.clear();
        tfLocalisation.clear();
        tfSalaireMin.clear();
        tfSalaireMax.clear();
        comboStatut.setValue(null);
        comboTypeContrat.setValue(null);
        comboModeTravail.setValue(null);
        tableOffres.getSelectionModel().clearSelection();
        localisationValide = false;
        derni\u00e8reLocValid\u00e9e = "";
        lblLocValidation.setText("");
        lblLocValidation.setStyle("-fx-font-size: 11px;");
    }
}
