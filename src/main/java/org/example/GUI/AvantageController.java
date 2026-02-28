package org.example.GUI;

import org.example.model.Avantage;
import org.example.model.Offre;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.example.services.AvantageService;
import org.example.services.AvantageSuggestionService;
import org.example.services.TranslationService;
import org.example.services.TranslationService.Langue;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AvantageController {
    @FXML private TextField tfNom;
    @FXML private TextArea taDescription;
    @FXML private ComboBox<String> comboType;
    @FXML private TableView<Avantage> tableAvantages;
    @FXML private TableColumn<Avantage, String> colNom, colDesc, colType;
    @FXML private Label lblOffreTitre;
    @FXML private Button btnModifier;
    @FXML private TextField tfRechercheAv;
    @FXML private Label lblScore;
    @FXML private Label lblBadge;
    @FXML private PieChart pieTypeStats;

    // --- Translation ---
    @FXML private ComboBox<Langue> comboLangueSource;
    @FXML private ComboBox<Langue> comboLangueCible;
    @FXML private Label lblTraduction;
    @FXML private TitledPane paneTraduction;
    @FXML private Label lblNomTraduit;
    @FXML private TextArea taDescriptionTraduite;

    // --- Suggestions IA ---
    @FXML private VBox vboxSuggestions;
    @FXML private TitledPane paneSuggestions;
    @FXML private Label lblSuggestionStatus;

    private final AvantageService service = new AvantageService();
    private final TranslationService translationService = new TranslationService();
    private final AvantageSuggestionService suggestionService = new AvantageSuggestionService();
    private Offre selectedOffre;

    public void setOffre(Offre offre) {
        this.selectedOffre = offre;
        lblOffreTitre.setText("Avantages pour : " + offre.getTitre());
        rafraichir();
    }

    @FXML
    public void initialize() {
        comboType.setItems(FXCollections.observableArrayList("FINANCIER", "BIEN_ETRE", "MATERIEL", "AUTRE"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));

        comboLangueSource.setItems(FXCollections.observableArrayList(Langue.values()));
        comboLangueCible.setItems(FXCollections.observableArrayList(Langue.values()));
        comboLangueSource.setValue(Langue.FRANCAIS);
        comboLangueCible.setValue(Langue.ANGLAIS);
    }

    @FXML
    private void handleAjouter() {
        String nom = tfNom.getText();
        if (nom == null || nom.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Champ requis", "Le nom de l'avantage est obligatoire.");
            return;
        }
        try {
            if (service.nomExistePourOffre(nom, selectedOffre.getId())) {
                showAlert(Alert.AlertType.WARNING, "Doublon d\u00e9tect\u00e9",
                        "L'avantage \"" + nom.trim() + "\" existe d\u00e9j\u00e0 pour cette offre.");
                return;
            }
            Avantage a = new Avantage();
            a.setNom(nom);
            a.setDescription(taDescription.getText());
            a.setType(comboType.getValue());
            a.setOffreId(selectedOffre.getId());
            service.ajouter(a);
            rafraichir();
            nettoyer();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSupprimer() {
        Avantage selection = tableAvantages.getSelectionModel().getSelectedItem();
        if (selection != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirmation");
            alert.setHeaderText(null);
            alert.setContentText("Voulez-vous vraiment supprimer l'avantage : " + selection.getNom() + " ?");
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    service.supprimer(selection.getId());
                    rafraichir();
                    showAlert(Alert.AlertType.INFORMATION, "Succ\u00e8s", "Avantage supprim\u00e9 avec succ\u00e8s !");
                } catch (SQLException e) {
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de supprimer l'avantage : " + e.getMessage());
                }
            }
        } else {
            showAlert(Alert.AlertType.WARNING, "S\u00e9lection requise", "Veuillez s\u00e9lectionner un avantage \u00e0 supprimer.");
        }
    }

    private void rafraichir() {
        if (selectedOffre == null) return;
        try {
            tableAvantages.setItems(FXCollections.observableArrayList(
                    service.recupererParOffre(selectedOffre.getId())
            ));
            int score = service.calculerScoreAttractivite(selectedOffre.getId());
            lblScore.setText("Score : " + score + " / 100");
            if (score >= 70) lblBadge.setText("\u2b50 Offre Premium");
            else if (score >= 40) lblBadge.setText("\u2705 Offre Attractive");
            else lblBadge.setText("\u26aa Offre Standard");
            chargerPieTypeStats();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void retourOffres() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/OffresView.fxml"));
            Parent view = loader.load();
            StackPane contentArea = (StackPane) lblOffreTitre.getScene().lookup(".content-area");
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
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible de charger OffresView.fxml.");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleModifier() {
        Avantage selection = tableAvantages.getSelectionModel().getSelectedItem();
        if (selection != null) {
            try {
                selection.setNom(tfNom.getText());
                selection.setDescription(taDescription.getText());
                selection.setType(comboType.getValue());
                service.modifier(selection);
                rafraichir();
                nettoyer();
                showAlert(Alert.AlertType.INFORMATION, "Succ\u00e8s", "Avantage modifi\u00e9 avec succ\u00e8s !");
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de modifier l'avantage : " + e.getMessage());
            }
        } else {
            showAlert(Alert.AlertType.WARNING, "S\u00e9lection requise", "Veuillez s\u00e9lectionner un avantage \u00e0 modifier.");
        }
    }

    @FXML
    private void chargerSelection() {
        Avantage selection = tableAvantages.getSelectionModel().getSelectedItem();
        if (selection != null) {
            tfNom.setText(selection.getNom());
            taDescription.setText(selection.getDescription());
            comboType.setValue(selection.getType());
            btnModifier.setStyle("-fx-background-color: #FFA500; -fx-text-fill: white;");
        }
    }

    @FXML
    private void handleScoreAttractivite() {
        if (selectedOffre == null) return;
        try {
            int score = service.calculerScoreAttractivite(selectedOffre.getId());
            lblScore.setText("Score : " + score + " / 100");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void chargerPieTypeStats() {
        if (selectedOffre == null || pieTypeStats == null) return;
        try {
            Map<String, Integer> data = service.statsParType(selectedOffre.getId());
            int total = data.values().stream().mapToInt(Integer::intValue).sum();

            if (total == 0) {
                pieTypeStats.setData(FXCollections.observableArrayList());
                pieTypeStats.setTitle("Aucun avantage");
                return;
            }

            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
            for (Map.Entry<String, Integer> entry : data.entrySet()) {
                String label = formatTypeLabel(entry.getKey());
                double pct = entry.getValue() * 100.0 / total;
                pieData.add(new PieChart.Data(
                        label + " \u2014 " + String.format("%.0f%%", pct),
                        entry.getValue()
                ));
            }

            pieTypeStats.setData(pieData);
            pieTypeStats.setTitle("R\u00e9partition par type (" + total + ")");
            pieTypeStats.setLabelsVisible(true);
            pieTypeStats.setLegendVisible(true);

            for (PieChart.Data d : pieTypeStats.getData()) {
                double pct = d.getPieValue() * 100.0 / total;
                Tooltip tooltip = new Tooltip(d.getName() + "\n" + (int) d.getPieValue() + " avantage(s) \u2014 " + String.format("%.1f%%", pct));
                tooltip.setShowDelay(Duration.millis(100));
                Tooltip.install(d.getNode(), tooltip);
                d.getNode().setOnMouseEntered(e -> d.getNode().setStyle("-fx-opacity: 0.8;"));
                d.getNode().setOnMouseExited(e -> d.getNode().setStyle("-fx-opacity: 1;"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String formatTypeLabel(String raw) {
        if (raw == null) return "Autre";
        return switch (raw.toUpperCase()) {
            case "FINANCIER" -> "Financier";
            case "BIEN_ETRE" -> "Bien-\u00eatre";
            case "MATERIEL"  -> "Mat\u00e9riel";
            case "AUTRE"     -> "Autre";
            default          -> raw;
        };
    }

    // ══════════════════════════════════════════════════════════
    //  TRADUCTION AUTOMATIQUE
    // ══════════════════════════════════════════════════════════
    @FXML
    private void handleTraduire() {
        String nom = tfNom.getText();
        String description = taDescription.getText();

        if ((nom == null || nom.isBlank()) && (description == null || description.isBlank())) {
            showAlert(Alert.AlertType.WARNING, "Rien \u00e0 traduire",
                    "Veuillez s\u00e9lectionner un avantage ou remplir le formulaire avant de traduire.");
            return;
        }

        Langue source = comboLangueSource.getValue();
        Langue cible = comboLangueCible.getValue();

        if (source == null || cible == null) {
            showAlert(Alert.AlertType.WARNING, "Langues manquantes",
                    "Veuillez s\u00e9lectionner la langue source et la langue cible.");
            return;
        }
        if (source == cible) {
            showAlert(Alert.AlertType.INFORMATION, "M\u00eame langue",
                    "La langue source et la langue cible sont identiques.");
            return;
        }

        lblTraduction.setText("\u23f3 Traduction en cours (" + source.getLabel() + " \u2192 " + cible.getLabel() + ")...");
        lblTraduction.setStyle("-fx-text-fill: #2563EB; -fx-font-size: 11px;");

        Task<String[]> task = new Task<>() {
            @Override protected String[] call() {
                String nomT = translationService.traduire(nom != null ? nom.trim() : "", source, cible);
                String descT = translationService.traduire(description != null ? description.trim() : "", source, cible);
                return new String[]{
                        nomT != null ? nomT : (nom != null ? nom : ""),
                        descT != null ? descT : (description != null ? description : "")
                };
            }
        };

        task.setOnSucceeded(e -> {
            String[] result = task.getValue();
            lblNomTraduit.setText(result[0]);
            taDescriptionTraduite.setText(result[1]);
            paneTraduction.setExpanded(true);
            lblTraduction.setText("\u2705 Traduit : " + source.getLabel() + " \u2192 " + cible.getLabel());
            lblTraduction.setStyle("-fx-text-fill: #059669; -fx-font-size: 11px; -fx-font-weight: bold;");
        });

        task.setOnFailed(e -> {
            lblTraduction.setText("\u274c Erreur : " + task.getException().getMessage());
            lblTraduction.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 11px;");
        });

        new Thread(task).start();
    }

    // ══════════════════════════════════════════════════════════
    //  IA \u2013 G\u00c9N\u00c9RATEUR INTELLIGENT D'AVANTAGES
    // ══════════════════════════════════════════════════════════
    @FXML
    private void handleSuggererAvantages() {
        if (selectedOffre == null) {
            showAlert(Alert.AlertType.WARNING, "Aucune offre", "Aucune offre s\u00e9lectionn\u00e9e pour g\u00e9n\u00e9rer des suggestions.");
            return;
        }

        if (lblSuggestionStatus != null) {
            lblSuggestionStatus.setText("Analyse de l'offre en cours...");
            lblSuggestionStatus.setStyle("-fx-text-fill: #2563EB; -fx-font-size: 11px;");
        }

        Task<List<Avantage>> task = new Task<>() {
            @Override protected List<Avantage> call() {
                return suggestionService.suggerer(selectedOffre);
            }
        };

        task.setOnSucceeded(e -> {
            List<Avantage> suggestions = task.getValue();
            if (vboxSuggestions != null) vboxSuggestions.getChildren().clear();

            if (suggestions == null || suggestions.isEmpty()) {
                if (lblSuggestionStatus != null) {
                    lblSuggestionStatus.setText("Aucune suggestion trouv\u00e9e pour cette offre.");
                    lblSuggestionStatus.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 11px;");
                }
                return;
            }

            if (lblSuggestionStatus != null) {
                lblSuggestionStatus.setText(suggestions.size() + " suggestion(s) g\u00e9n\u00e9r\u00e9e(s) avec succ\u00e8s !");
                lblSuggestionStatus.setStyle("-fx-text-fill: #059669; -fx-font-size: 11px; -fx-font-weight: bold;");
            }
            if (paneSuggestions != null) paneSuggestions.setExpanded(true);

            if (vboxSuggestions != null) {
                for (Avantage sugg : suggestions) {
                    VBox card = new VBox(4);
                    card.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; "
                            + "-fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8;");

                    String typeEmoji = switch (sugg.getType() != null ? sugg.getType() : "") {
                        case "FINANCIER" -> "[FIN]";
                        case "BIEN_ETRE" -> "[BE]";
                        case "MATERIEL"  -> "[MAT]";
                        default          -> "[+]";
                    };

                    Label lblNom = new Label(typeEmoji + " " + sugg.getNom());
                    lblNom.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #1e293b;");
                    lblNom.setWrapText(true);

                    Label lblDesc = new Label(sugg.getDescription());
                    lblDesc.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
                    lblDesc.setWrapText(true);

                    Label lblType = new Label("Type : " + formatTypeLabel(sugg.getType()));
                    lblType.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8; -fx-font-style: italic;");

                    Button btnAdd = new Button("+ Ajouter cet avantage");
                    btnAdd.setMaxWidth(Double.MAX_VALUE);
                    btnAdd.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; "
                            + "-fx-background-radius: 4; -fx-font-size: 11px; -fx-cursor: hand;");
                    btnAdd.setOnAction(ev -> ajouterSuggestion(sugg, card));

                    card.getChildren().addAll(lblNom, lblDesc, lblType, btnAdd);
                    card.setOnMouseEntered(me -> card.setStyle(
                            "-fx-background-color: #eff6ff; -fx-border-color: #3b82f6; "
                                    + "-fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8;"));
                    card.setOnMouseExited(me -> card.setStyle(
                            "-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; "
                                    + "-fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8;"));

                    vboxSuggestions.getChildren().add(card);
                }
            }
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            if (lblSuggestionStatus != null) {
                lblSuggestionStatus.setText("Erreur : " + (ex != null ? ex.getMessage() : "inconnue"));
                lblSuggestionStatus.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 11px;");
            }
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void ajouterSuggestion(Avantage suggestion, VBox card) {
        try {
            if (service.nomExistePourOffre(suggestion.getNom(), selectedOffre.getId())) {
                showAlert(Alert.AlertType.WARNING, "D\u00e9j\u00e0 existant",
                        "L'avantage \"" + suggestion.getNom() + "\" est d\u00e9j\u00e0 associ\u00e9 \u00e0 cette offre.");
                return;
            }
            service.ajouter(suggestion);
            rafraichir();
            if (vboxSuggestions != null) vboxSuggestions.getChildren().remove(card);
            if (vboxSuggestions != null && vboxSuggestions.getChildren().isEmpty()) {
                lblSuggestionStatus.setText("Toutes les suggestions ont \u00e9t\u00e9 ajout\u00e9es !");
                lblSuggestionStatus.setStyle("-fx-text-fill: #059669; -fx-font-size: 11px; -fx-font-weight: bold;");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ajouter la suggestion : " + ex.getMessage());
        }
    }

    private void nettoyer() {
        tfNom.clear();
        taDescription.clear();
        comboType.setValue(null);
        tableAvantages.getSelectionModel().clearSelection();
        btnModifier.setStyle("");
    }
}
