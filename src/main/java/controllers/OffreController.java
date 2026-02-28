package controllers;

import entities.Offre;
import services.OffreService;
import services.GeocodingService;
import services.TranslationService;
import services.TranslationService.Langue;
import services.CurrencyService;
import services.CurrencyService.Devise;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import services.AvantageService;

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

    // État de validation de la localisation
    private boolean localisationValide = false;
    private String dernièreLocValidée = "";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // ComboBox items
        comboStatut.setItems(FXCollections.observableArrayList("PUBLISHED", "CLOSED", "ARCHIVED"));
        comboTypeContrat.setItems(FXCollections.observableArrayList("CDI", "CDD", "Stage", "Freelance", "Alternance"));
        comboModeTravail.setItems(FXCollections.observableArrayList("ON_SITE", "REMOTE", "HYBRID"));
        comboTri.setItems(FXCollections.observableArrayList("Titre", "Localisation", "Statut", "Date (ID)", "Score ↓"));

        // Translation ComboBoxes
        // Currency ComboBox
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

        // Color-coded cohérence column
        colCoherence.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.contains("Faible")) {
                        setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold;");
                    } else if (item.contains("Compensée")) {
                        setStyle("-fx-text-fill: #D97706; -fx-font-weight: bold;");
                    } else if (item.contains("Acceptable")) {
                        setStyle("-fx-text-fill: #2563EB; -fx-font-weight: bold;");
                    } else if (item.contains("Excellente")) {
                        setStyle("-fx-text-fill: #059669; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        // Color-coded classement column (🥇 Or / 🥈 Argent / 🥉 Bronze)
        colClassement.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.contains("Or")) {
                        setStyle("-fx-text-fill: #D97706; -fx-font-weight: bold;");
                    } else if (item.contains("Argent")) {
                        setStyle("-fx-text-fill: #6B7280; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #B45309; -fx-font-weight: bold;");
                    }
                }
            }
        });

        // Color-coded statut column
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
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

        // Salary fields: allow numbers/decimals only
        tfSalaireMin.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*\\.?\\d*")) tfSalaireMin.setText(oldVal);
        });
        tfSalaireMax.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*\\.?\\d*")) tfSalaireMax.setText(oldVal);
        });

        // ── Contrôle de saisie pour la localisation ──
        // Validation basique en temps réel (caractères autorisés)
        tfLocalisation.textProperty().addListener((obs, oldVal, newVal) -> {
            // Réinitialiser la validation quand le texte change
            if (!newVal.trim().equals(dernièreLocValidée)) {
                localisationValide = false;
                if (newVal.trim().isEmpty()) {
                    lblLocValidation.setText("");
                    lblLocValidation.setStyle("-fx-font-size: 11px;");
                } else if (newVal.trim().length() < 2) {
                    lblLocValidation.setText("⚠ Trop court — saisissez un nom de ville valide");
                    lblLocValidation.setStyle("-fx-text-fill: #D97706; -fx-font-size: 11px;");
                } else if (!newVal.matches("[a-zA-ZÀ-ÿ\\s\\-',\\.]+")) {
                    lblLocValidation.setText("⚠ Caractères invalides — lettres, espaces et tirets uniquement");
                    lblLocValidation.setStyle("-fx-text-fill: #D97706; -fx-font-size: 11px;");
                } else {
                    lblLocValidation.setText("🔎 Quittez le champ pour vérifier l'adresse...");
                    lblLocValidation.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 11px;");
                }
            }
        });

        // Vérification géocodage quand l'utilisateur quitte le champ
        tfLocalisation.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) { // Focus perdu → vérifier
                String loc = tfLocalisation.getText();
                if (loc != null && !loc.trim().isEmpty() && loc.trim().length() >= 2
                        && !loc.trim().equals(dernièreLocValidée)) {
                    validerLocalisationAsync(loc.trim());
                }
            }
        });

        rafraichir();
    }

    // ══════════════════════════════════════════════════════════
    //  VALIDATION
    // ══════════════════════════════════════════════════════════

    /**
     * Vérifie la localisation en arrière-plan via Nominatim (géocodage).
     * Met à jour l'indicateur visuel lblLocValidation.
     */
    private void validerLocalisationAsync(String location) {
        lblLocValidation.setText("⏳ Vérification de l'adresse en cours...");
        lblLocValidation.setStyle("-fx-text-fill: #2563EB; -fx-font-size: 11px;");

        Task<double[]> task = new Task<>() {
            @Override
            protected double[] call() {
                return geoService.geocode(location);
            }
        };

        task.setOnSucceeded(e -> {
            double[] coords = task.getValue();
            if (coords != null) {
                localisationValide = true;
                dernièreLocValidée = location;
                lblLocValidation.setText("✅ Adresse valide — " + String.format("%.4f, %.4f", coords[0], coords[1]));
                lblLocValidation.setStyle("-fx-text-fill: #059669; -fx-font-size: 11px; -fx-font-weight: bold;");
            } else {
                localisationValide = false;
                dernièreLocValidée = "";
                lblLocValidation.setText("❌ Adresse introuvable — vérifiez l'orthographe (ex: Tunis, Ariana, Sousse)");
                lblLocValidation.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 11px; -fx-font-weight: bold;");
            }
        });

        task.setOnFailed(e -> {
            localisationValide = false;
            dernièreLocValidée = "";
            lblLocValidation.setText("⚠ Impossible de vérifier — vérifiez votre connexion internet");
            lblLocValidation.setStyle("-fx-text-fill: #D97706; -fx-font-size: 11px;");
        });

        new Thread(task).start();
    }

    private boolean validerFormulaire() {
        StringBuilder erreurs = new StringBuilder();

        if (tfTitre.getText() == null || tfTitre.getText().trim().isEmpty()) {
            erreurs.append("• Le titre est obligatoire.\n");
        } else if (tfTitre.getText().trim().length() < 3) {
            erreurs.append("• Le titre doit contenir au moins 3 caractères.\n");
        } else if (tfTitre.getText().trim().length() > 100) {
            erreurs.append("• Le titre ne doit pas dépasser 100 caractères.\n");
        }

        if (taDescription.getText() == null || taDescription.getText().trim().isEmpty()) {
            erreurs.append("• La description est obligatoire.\n");
        } else if (taDescription.getText().trim().length() < 10) {
            erreurs.append("• La description doit contenir au moins 10 caractères.\n");
        }

        String loc = tfLocalisation.getText();
        if (loc == null || loc.trim().isEmpty()) {
            erreurs.append("• La localisation est obligatoire.\n");
        } else if (loc.trim().length() < 2) {
            erreurs.append("• La localisation doit contenir au moins 2 caractères.\n");
        } else if (!loc.matches("[a-zA-ZÀ-ÿ\\s\\-',\\.]+")) {
            erreurs.append("• La localisation contient des caractères invalides (chiffres, symboles).\n");
        } else if (!localisationValide) {
            // Tenter une vérification synchrone si pas encore validée
            double[] coords = geoService.geocode(loc.trim());
            if (coords != null) {
                localisationValide = true;
                dernièreLocValidée = loc.trim();
                lblLocValidation.setText("✅ Adresse valide — " + String.format("%.4f, %.4f", coords[0], coords[1]));
                lblLocValidation.setStyle("-fx-text-fill: #059669; -fx-font-size: 11px; -fx-font-weight: bold;");
            } else {
                erreurs.append("• La localisation '" + loc.trim() + "' est introuvable. Saisissez une adresse valide (ex: Tunis, Ariana, Sousse).\n");
            }
        }

        if (comboStatut.getValue() == null) {
            erreurs.append("• Le statut est obligatoire.\n");
        }

        if (comboTypeContrat.getValue() == null) {
            erreurs.append("• Le type de contrat est obligatoire.\n");
        }

        if (comboModeTravail.getValue() == null) {
            erreurs.append("• Le mode de travail est obligatoire.\n");
        }

        // Salary validation
        double salMin = parseSalaire(tfSalaireMin.getText());
        double salMax = parseSalaire(tfSalaireMax.getText());
        if (salMin < 0) {
            erreurs.append("• Le salaire minimum ne peut pas être négatif.\n");
        }
        if (salMax < 0) {
            erreurs.append("• Le salaire maximum ne peut pas être négatif.\n");
        }
        if (salMin > 0 && salMax > 0 && salMin > salMax) {
            erreurs.append("• Le salaire minimum ne peut pas être supérieur au maximum.\n");
        }

        if (erreurs.length() > 0) {
            afficherAlerte(Alert.AlertType.WARNING, "Validation échouée", erreurs.toString());
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
            // Vérifier doublon de titre
            if (service.titreExiste(tfTitre.getText().trim(), 0)) {
                afficherAlerte(Alert.AlertType.WARNING, "Doublon détecté",
                        "Une offre avec ce titre existe déjà. Veuillez choisir un autre titre.");
                return;
            }

            Offre o = buildOffreFromForm();
            service.ajouter(o);
            rafraichir();
            nettoyer();
            afficherAlerte(Alert.AlertType.INFORMATION, "Succès", "L'offre a été ajoutée avec succès !");
        } catch (SQLException e) {
            e.printStackTrace();
            afficherAlerte(Alert.AlertType.ERROR, "Erreur de base de données", "Détail : " + e.getMessage());
        }
    }

    @FXML
    private void handleModifier() {
        Offre selection = tableOffres.getSelectionModel().getSelectedItem();
        if (selection == null) {
            afficherAlerte(Alert.AlertType.WARNING, "Sélection requise", "Veuillez sélectionner une offre dans le tableau.");
            return;
        }
        if (!validerFormulaire()) return;
        if (!confirmerAction("Voulez-vous modifier cette offre ?")) return;

        try {
            // Vérifier doublon (exclure l'offre actuelle)
            if (service.titreExiste(tfTitre.getText().trim(), selection.getId())) {
                afficherAlerte(Alert.AlertType.WARNING, "Doublon détecté",
                        "Une autre offre avec ce titre existe déjà.");
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
            afficherAlerte(Alert.AlertType.INFORMATION, "Succès", "L'offre a été modifiée !");
        } catch (SQLException e) {
            e.printStackTrace();
            afficherAlerte(Alert.AlertType.ERROR, "Erreur de modification", e.getMessage());
        }
    }

    @FXML
    private void handleSupprimer() {
        Offre selection = tableOffres.getSelectionModel().getSelectedItem();
        if (selection == null) {
            afficherAlerte(Alert.AlertType.WARNING, "Sélection requise", "Veuillez sélectionner une offre à supprimer.");
            return;
        }
        if (!confirmerAction("Voulez-vous vraiment supprimer l'offre : " + selection.getTitre() + " ?\n\n⚠ Cela supprimera aussi ses avantages.")) return;

        try {
            service.supprimer(selection.getId());
            rafraichir();
            nettoyer();
            afficherAlerte(Alert.AlertType.INFORMATION, "Succès", "L'offre a été supprimée.");
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
            afficherAlerte(Alert.AlertType.WARNING, "Sélection requise",
                    "Veuillez sélectionner une offre pour voir ses avantages.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/avantages.fxml"));
            Parent root = loader.load();
            AvantageController controller = loader.getController();
            controller.setOffre(selection);
            Stage stage = (Stage) tableOffres.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Avantages pour : " + selection.getTitre());
        } catch (IOException e) {
            e.printStackTrace();
            afficherAlerte(Alert.AlertType.ERROR, "Erreur",
                    "Impossible de charger la page des avantages : " + e.getMessage());
        }
    }

    @FXML
    private void ouvrirCarte() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/map.fxml"));
            Parent root = loader.load();

            // Si une offre est sélectionnée, centrer la carte dessus
            Offre selection = tableOffres.getSelectionModel().getSelectedItem();
            if (selection != null) {
                MapController mapCtrl = loader.getController();
                mapCtrl.setOffre(selection);
            }

            Stage stage = (Stage) tableOffres.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("TalentFlow — Carte des Offres");
        } catch (IOException e) {
            e.printStackTrace();
            afficherAlerte(Alert.AlertType.ERROR, "Erreur",
                    "Impossible de charger la carte : " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════
    //  TRADUCTION AUTOMATIQUE
    // ══════════════════════════════════════════════════════════
    @FXML
    private void handleTraduire() {
        // Vérifier qu'une offre est sélectionnée ou que le formulaire est rempli
        String titre = tfTitre.getText();
        String description = taDescription.getText();
        String localisation = tfLocalisation.getText();

        if ((titre == null || titre.isBlank()) && (description == null || description.isBlank())) {
            afficherAlerte(Alert.AlertType.WARNING, "Rien à traduire",
                    "Veuillez sélectionner une offre ou remplir le formulaire avant de traduire.");
            return;
        }

        Langue source = comboLangueSource.getValue();
        Langue cible = comboLangueCible.getValue();

        if (source == null || cible == null) {
            afficherAlerte(Alert.AlertType.WARNING, "Langues manquantes",
                    "Veuillez sélectionner la langue source et la langue cible.");
            return;
        }

        if (source == cible) {
            afficherAlerte(Alert.AlertType.INFORMATION, "Même langue",
                    "La langue source et la langue cible sont identiques.");
            return;
        }

        // Afficher le statut de la traduction
        lblTraduction.setText("⏳ Traduction en cours (" + source.getLabel() + " → " + cible.getLabel() + ")...");
        lblTraduction.setStyle("-fx-text-fill: #2563EB; -fx-font-size: 11px;");

        // Exécuter la traduction en arrière-plan
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

                // Ouvrir le panneau traduction
                paneTraduction.setExpanded(true);

                lblTraduction.setText("✅ Traduit : " + source.getLabel() + " → " + cible.getLabel());
                lblTraduction.setStyle("-fx-text-fill: #059669; -fx-font-size: 11px; -fx-font-weight: bold;");
            } else {
                lblTraduction.setText("❌ Échec de la traduction — vérifiez votre connexion internet.");
                lblTraduction.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 11px; -fx-font-weight: bold;");
            }
        });

        task.setOnFailed(e -> {
            lblTraduction.setText("❌ Erreur : " + task.getException().getMessage());
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
            // Tenter depuis la sélection du tableau
            Offre selection = tableOffres.getSelectionModel().getSelectedItem();
            if (selection != null) {
                salMin = selection.getSalaireMin();
                salMax = selection.getSalaireMax();
                // Mettre à jour les champs pour montrer à l'utilisateur les valeurs converties
                if (salMin > 0) tfSalaireMin.setText(String.valueOf(salMin));
                if (salMax > 0) tfSalaireMax.setText(String.valueOf(salMax));
            }
        }

        if (salMin == 0 && salMax == 0) {
            afficherAlerte(Alert.AlertType.WARNING, "Aucun salaire",
                    "Veuillez saisir un salaire ou sélectionner une offre avec un salaire défini.");
            return;
        }

        Devise source = comboDeviseSource.getValue();
        if (source == null) {
            source = Devise.TND;
            comboDeviseSource.setValue(source);
        }

        // Afficher le statut immédiatement
        if (lblConversionStatus != null) {
            lblConversionStatus.setText("⏳ Conversion en cours...");
            lblConversionStatus.setStyle("-fx-text-fill: #2563EB; -fx-font-size: 11px;");
        }
        if (taConversionResult != null) {
            taConversionResult.setText("");
        }
        if (paneDevises != null) {
            paneDevises.setExpanded(true);
        }

        final double fMin = salMin;
        final double fMax = salMax;
        final Devise fSource = source;

        Task<CurrencyService.ConversionResult> task = new Task<>() {
            @Override
            protected CurrencyService.ConversionResult call() {
                return currencyService.convertir(fMin, fMax, fSource);
            }
        };

        task.setOnSucceeded(e -> {
            CurrencyService.ConversionResult convResult = task.getValue();
            if (convResult == null) {
                if (lblConversionStatus != null) {
                    lblConversionStatus.setText("❌ Résultat vide.");
                    lblConversionStatus.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 11px; -fx-font-weight: bold;");
                }
                return;
            }

            if (taConversionResult != null) {
                taConversionResult.setText(convResult.toDisplayText());
            }

            if (lblConversionStatus != null) {
                if (convResult.getErrorMessage() != null) {
                    lblConversionStatus.setText("❌ " + convResult.getErrorMessage());
                    lblConversionStatus.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 11px; -fx-font-weight: bold;");
                } else if (convResult.getConversions().isEmpty()) {
                    lblConversionStatus.setText("❌ Impossible de récupérer les taux — vérifiez votre connexion.");
                    lblConversionStatus.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 11px; -fx-font-weight: bold;");
                } else {
                    lblConversionStatus.setText("✅ Conversion réussie — " + convResult.getConversions().size() + " devise(s)");
                    lblConversionStatus.setStyle("-fx-text-fill: #059669; -fx-font-size: 11px; -fx-font-weight: bold;");
                }
            }
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            String msg = ex != null ? ex.getMessage() : "Erreur inconnue";
            if (lblConversionStatus != null) {
                lblConversionStatus.setText("❌ Erreur : " + msg);
                lblConversionStatus.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 11px;");
            }
            if (taConversionResult != null) {
                taConversionResult.setText("Une erreur s'est produite.\n" + msg);
            }
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
            // L'offre existante a déjà une localisation valide
            localisationValide = true;
            dernièreLocValidée = selection.getLocalisation();
            lblLocValidation.setText("✅ Adresse existante");
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
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/statistiques.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) tableOffres.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("TalentFlow — Statistiques");
        } catch (IOException e) {
            e.printStackTrace();
            afficherAlerte(Alert.AlertType.ERROR, "Erreur",
                    "Impossible de charger le tableau de bord : " + e.getMessage());
        }
    }

    @FXML
    private void handleTri() {
        try {
            String choix = comboTri.getValue();

            // Tri par score : tri côté Java après application du classement
            if ("Score ↓".equals(choix)) {
                List<Offre> offres = service.afficher();
                Map<Integer, Integer> scores = service.scoresAttractivite();
                for (Offre o : offres) {
                    int score = scores.getOrDefault(o.getId(), 0);
                    if (score >= 70) o.setClassement("🥇 Or (" + score + ")");
                    else if (score >= 40) o.setClassement("🥈 Argent (" + score + ")");
                    else o.setClassement("🥉 Bronze (" + score + ")");
                }
                offres.sort((a, b) -> {
                    int sa = scores.getOrDefault(a.getId(), 0);
                    int sb = scores.getOrDefault(b.getId(), 0);
                    return Integer.compare(sb, sa); // descending
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

    /** Calcule le classement Or/Argent/Bronze et l'indice de cohérence pour chaque offre */
    private void appliquerClassement(List<Offre> offres) {
        try {
            Map<Integer, Integer> scores = service.scoresAttractivite();
            Map<Integer, String> coherences = service.indicesCoherence();
            for (Offre o : offres) {
                int score = scores.getOrDefault(o.getId(), 0);
                if (score >= 70) {
                    o.setClassement("\ud83e\udd47 Or (" + score + ")");
                } else if (score >= 40) {
                    o.setClassement("\ud83e\udd48 Argent (" + score + ")");
                } else {
                    o.setClassement("\ud83e\udd49 Bronze (" + score + ")");
                }
                o.setCoherence(coherences.getOrDefault(o.getId(), "—"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateCount(int count) {
        if (lblCount != null) {
            lblCount.setText(count + " offre" + (count > 1 ? "s" : "") + " trouvée" + (count > 1 ? "s" : ""));
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
        // Réinitialiser la validation de localisation
        localisationValide = false;
        dernièreLocValidée = "";
        lblLocValidation.setText("");
        lblLocValidation.setStyle("-fx-font-size: 11px;");
    }

    @FXML
    private void handleQuitter() {
        if (confirmerAction("Voulez-vous vraiment quitter l'application ?")) {
            Stage stage = (Stage) tableOffres.getScene().getWindow();
            stage.close();
        }
    }
}