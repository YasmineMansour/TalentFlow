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
        comboTri.setItems(FXCollections.observableArrayList("Titre", "Localisation", "Statut", "Date (ID)", "Score \u2193"));

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
                    lblLocValidation.setText("\u26a0 Trop court \u2014 saisissez un nom de ville valide");
                    lblLocValidation.setStyle("-fx-text-fill: #D97706; -fx-font-size: 11px;");
                } else if (!newVal.matches("[a-zA-Z\u00c0-\u00ff\\s\\-',\\.]+")) {
                    lblLocValidation.setText("\u26a0 Caract\u00e8res invalides \u2014 lettres, espaces et tirets uniquement");
                    lblLocValidation.setStyle("-fx-text-fill: #D97706; -fx-font-size: 11px;");
                } else {
                    lblLocValidation.setText("\ud83d\udd0e Quittez le champ pour v\u00e9rifier l'adresse...");
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
            afficherAlerte(Alert.AlertType.ERROR, "Erreur de navigation", e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════
    //  VALIDATION
    // ══════════════════════════════════════════════════════════

    private void validerLocalisationAsync(String location) {
        lblLocValidation.setText("\u23f3 V\u00e9rification de l'adresse en cours...");
        lblLocValidation.setStyle("-fx-text-fill: #2563EB; -fx-font-size: 11px;");

        Task<double[]> task = new Task<>() {
            @Override protected double[] call() { return geoService.geocode(location); }
        };

        task.setOnSucceeded(e -> {
            double[] coords = task.getValue();
            if (coords != null) {
                localisationValide = true;
                derni\u00e8reLocValid\u00e9e = location;
                lblLocValidation.setText("\u2705 Adresse valide \u2014 " + String.format("%.4f, %.4f", coords[0], coords[1]));
                lblLocValidation.setStyle("-fx-text-fill: #059669; -fx-font-size: 11px; -fx-font-weight: bold;");
            } else {
                localisationValide = false;
                derni\u00e8reLocValid\u00e9e = "";
                lblLocValidation.setText("\u274c Adresse introuvable \u2014 v\u00e9rifiez l'orthographe (ex: Tunis, Ariana, Sousse)");
                lblLocValidation.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 11px; -fx-font-weight: bold;");
            }
        });

        task.setOnFailed(e -> {
            localisationValide = false;
            derni\u00e8reLocValid\u00e9e = "";
            lblLocValidation.setText("\u26a0 Impossible de v\u00e9rifier \u2014 v\u00e9rifiez votre connexion internet");
            lblLocValidation.setStyle("-fx-text-fill: #D97706; -fx-font-size: 11px;");
        });

        new Thread(task).start();
    }

    private boolean validerFormulaire() {
        StringBuilder erreurs = new StringBuilder();

        if (tfTitre.getText() == null || tfTitre.getText().trim().isEmpty())
            erreurs.append("\u2022 Le titre est obligatoire.\n");
        else if (tfTitre.getText().trim().length() < 3)
            erreurs.append("\u2022 Le titre doit contenir au moins 3 caract\u00e8res.\n");
        else if (tfTitre.getText().trim().length() > 100)
            erreurs.append("\u2022 Le titre ne doit pas d\u00e9passer 100 caract\u00e8res.\n");

        if (taDescription.getText() == null || taDescription.getText().trim().isEmpty())
            erreurs.append("\u2022 La description est obligatoire.\n");
        else if (taDescription.getText().trim().length() < 10)
            erreurs.append("\u2022 La description doit contenir au moins 10 caract\u00e8res.\n");

        String loc = tfLocalisation.getText();
        if (loc == null || loc.trim().isEmpty()) {
            erreurs.append("\u2022 La localisation est obligatoire.\n");
        } else if (loc.trim().length() < 2) {
            erreurs.append("\u2022 La localisation doit contenir au moins 2 caract\u00e8res.\n");
        } else if (!loc.matches("[a-zA-Z\u00c0-\u00ff\\s\\-',\\.]+")) {
            erreurs.append("\u2022 La localisation contient des caract\u00e8res invalides.\n");
        } else if (!localisationValide) {
            double[] coords = geoService.geocode(loc.trim());
            if (coords != null) {
                localisationValide = true;
                derni\u00e8reLocValid\u00e9e = loc.trim();
                lblLocValidation.setText("\u2705 Adresse valide \u2014 " + String.format("%.4f, %.4f", coords[0], coords[1]));
                lblLocValidation.setStyle("-fx-text-fill: #059669; -fx-font-size: 11px; -fx-font-weight: bold;");
            } else {
                erreurs.append("\u2022 La localisation '" + loc.trim() + "' est introuvable.\n");
            }
        }

        if (comboStatut.getValue() == null) erreurs.append("\u2022 Le statut est obligatoire.\n");
        if (comboTypeContrat.getValue() == null) erreurs.append("\u2022 Le type de contrat est obligatoire.\n");
        if (comboModeTravail.getValue() == null) erreurs.append("\u2022 Le mode de travail est obligatoire.\n");

        double salMin = parseSalaire(tfSalaireMin.getText());
        double salMax = parseSalaire(tfSalaireMax.getText());
        if (salMin < 0) erreurs.append("\u2022 Le salaire minimum ne peut pas \u00eatre n\u00e9gatif.\n");
        if (salMax < 0) erreurs.append("\u2022 Le salaire maximum ne peut pas \u00eatre n\u00e9gatif.\n");
        if (salMin > 0 && salMax > 0 && salMin > salMax)
            erreurs.append("\u2022 Le salaire minimum ne peut pas \u00eatre sup\u00e9rieur au maximum.\n");

        if (erreurs.length() > 0) {
            afficherAlerte(Alert.AlertType.WARNING, "Validation \u00e9chou\u00e9e", erreurs.toString());
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
                afficherAlerte(Alert.AlertType.WARNING, "Doublon d\u00e9tect\u00e9",
                        "Une offre avec ce titre existe d\u00e9j\u00e0. Veuillez choisir un autre titre.");
                return;
            }
            Offre o = buildOffreFromForm();
            service.ajouter(o);
            rafraichir();
            nettoyer();
            afficherAlerte(Alert.AlertType.INFORMATION, "Succ\u00e8s", "L'offre a \u00e9t\u00e9 ajout\u00e9e avec succ\u00e8s !");
        } catch (SQLException e) {
            e.printStackTrace();
            afficherAlerte(Alert.AlertType.ERROR, "Erreur de base de donn\u00e9es", "D\u00e9tail : " + e.getMessage());
        }
    }

    @FXML
    private void handleModifier() {
        Offre selection = tableOffres.getSelectionModel().getSelectedItem();
        if (selection == null) {
            afficherAlerte(Alert.AlertType.WARNING, "S\u00e9lection requise", "Veuillez s\u00e9lectionner une offre dans le tableau.");
            return;
        }
        if (!validerFormulaire()) return;
        if (!confirmerAction("Voulez-vous modifier cette offre ?")) return;

        try {
            if (service.titreExiste(tfTitre.getText().trim(), selection.getId())) {
                afficherAlerte(Alert.AlertType.WARNING, "Doublon d\u00e9tect\u00e9",
                        "Une autre offre avec ce titre existe d\u00e9j\u00e0.");
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
            afficherAlerte(Alert.AlertType.INFORMATION, "Succ\u00e8s", "L'offre a \u00e9t\u00e9 modifi\u00e9e !");
        } catch (SQLException e) {
            e.printStackTrace();
            afficherAlerte(Alert.AlertType.ERROR, "Erreur de modification", e.getMessage());
        }
    }

    @FXML
    private void handleSupprimer() {
        Offre selection = tableOffres.getSelectionModel().getSelectedItem();
        if (selection == null) {
            afficherAlerte(Alert.AlertType.WARNING, "S\u00e9lection requise", "Veuillez s\u00e9lectionner une offre \u00e0 supprimer.");
            return;
        }
        if (!confirmerAction("Voulez-vous vraiment supprimer l'offre : " + selection.getTitre() + " ?\n\n\u26a0 Cela supprimera aussi ses avantages.")) return;

        try {
            service.supprimer(selection.getId());
            rafraichir();
            nettoyer();
            afficherAlerte(Alert.AlertType.INFORMATION, "Succ\u00e8s", "L'offre a \u00e9t\u00e9 supprim\u00e9e.");
        } catch (SQLException e) {
            e.printStackTrace();
            afficherAlerte(Alert.AlertType.ERROR, "Erreur de suppression", e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════
    //  NAVIGATION
    // ══════════════════════════════════════════════════════════
    @FXML
    private void ouvrirAvantages() {
        Offre selection = tableOffres.getSelectionModel().getSelectedItem();
        if (selection == null) {
            afficherAlerte(Alert.AlertType.WARNING, "S\u00e9lection requise",
                    "Veuillez s\u00e9lectionner une offre pour voir ses avantages.");
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
            afficherAlerte(Alert.AlertType.ERROR, "Erreur", "Impossible de charger la page des avantages : " + e.getMessage());
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
            afficherAlerte(Alert.AlertType.ERROR, "Erreur", "Impossible de charger la carte : " + e.getMessage());
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
            afficherAlerte(Alert.AlertType.WARNING, "Rien \u00e0 traduire",
                    "Veuillez s\u00e9lectionner une offre ou remplir le formulaire avant de traduire.");
            return;
        }

        Langue source = comboLangueSource.getValue();
        Langue cible = comboLangueCible.getValue();

        if (source == null || cible == null) {
            afficherAlerte(Alert.AlertType.WARNING, "Langues manquantes",
                    "Veuillez s\u00e9lectionner la langue source et la langue cible.");
            return;
        }

        if (source == cible) {
            afficherAlerte(Alert.AlertType.INFORMATION, "M\u00eame langue",
                    "La langue source et la langue cible sont identiques.");
            return;
        }

        lblTraduction.setText("\u23f3 Traduction en cours (" + source.getLabel() + " \u2192 " + cible.getLabel() + ")...");
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
                lblTraduction.setText("\u2705 Traduit : " + source.getLabel() + " \u2192 " + cible.getLabel());
                lblTraduction.setStyle("-fx-text-fill: #059669; -fx-font-size: 11px; -fx-font-weight: bold;");
            } else {
                lblTraduction.setText("\u274c \u00c9chec de la traduction \u2014 v\u00e9rifiez votre connexion internet.");
                lblTraduction.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 11px; -fx-font-weight: bold;");
            }
        });

        task.setOnFailed(e -> {
            lblTraduction.setText("\u274c Erreur : " + task.getException().getMessage());
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
            afficherAlerte(Alert.AlertType.WARNING, "Aucun salaire",
                    "Veuillez saisir un salaire ou s\u00e9lectionner une offre avec un salaire d\u00e9fini.");
            return;
        }

        Devise source = comboDeviseSource.getValue();
        if (source == null) { source = Devise.TND; comboDeviseSource.setValue(source); }

        if (lblConversionStatus != null) {
            lblConversionStatus.setText("\u23f3 Conversion en cours...");
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
                    lblConversionStatus.setText("\u274c R\u00e9sultat vide.");
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
                    lblConversionStatus.setText("\u274c Impossible de r\u00e9cup\u00e9rer les taux.");
                    lblConversionStatus.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 11px; -fx-font-weight: bold;");
                } else {
                    lblConversionStatus.setText("\u2705 Conversion r\u00e9ussie \u2014 " + convResult.getConversions().size() + " devise(s)");
                    lblConversionStatus.setStyle("-fx-text-fill: #059669; -fx-font-size: 11px; -fx-font-weight: bold;");
                }
            }
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            String msg = ex != null ? ex.getMessage() : "Erreur inconnue";
            if (lblConversionStatus != null) {
                lblConversionStatus.setText("\u274c Erreur : " + msg);
                lblConversionStatus.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 11px;");
            }
            if (taConversionResult != null) taConversionResult.setText("Une erreur s'est produite.\n" + msg);
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
            lblLocValidation.setText("\u2705 Adresse existante");
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
            afficherAlerte(Alert.AlertType.ERROR, "Erreur recherche", e.getMessage());
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

            if ("Score \u2193".equals(choix)) {
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
            if ("Titre".equals(choix)) colonne = "titre";
            else if ("Localisation".equals(choix)) colonne = "localisation";
            else if ("Statut".equals(choix)) colonne = "statut";

            List<Offre> sorted = service.trierPar(colonne);
            appliquerClassement(sorted);
            tableOffres.setItems(FXCollections.observableArrayList(sorted));
            updateCount(sorted.size());
        } catch (SQLException e) {
            e.printStackTrace();
            afficherAlerte(Alert.AlertType.ERROR, "Erreur tri", e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════
    //  UTILITIES
    // ══════════════════════════════════════════════════════════
    private boolean confirmerAction(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
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
            lblCount.setText(count + " offre" + (count > 1 ? "s" : "") + " trouv\u00e9e" + (count > 1 ? "s" : ""));
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
