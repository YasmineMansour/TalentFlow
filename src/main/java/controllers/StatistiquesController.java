package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import services.OffreService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

public class StatistiquesController {

    // ── Pie Charts ──
    @FXML private PieChart pieStatut;
    @FXML private PieChart pieContrat;

    // ── Bar Charts ──
    @FXML private BarChart<String, Number> barLocalisations;
    @FXML private CategoryAxis barLocX;
    @FXML private NumberAxis barLocY;

    @FXML private BarChart<String, Number> barModeTravail;
    @FXML private CategoryAxis barModeX;
    @FXML private NumberAxis barModeY;

    @FXML private BarChart<String, Number> barAvantages;
    @FXML private CategoryAxis barAvX;
    @FXML private NumberAxis barAvY;

    // ── Coherence Pie Chart ──
    @FXML private PieChart pieCoherence;

    // ── KPI Labels ──
    @FXML private Label lblTotal;
    @FXML private Label lblPublished;
    @FXML private Label lblSalaireMoyMin;
    @FXML private Label lblSalaireMoyMax;

    private final OffreService service = new OffreService();

    @FXML
    public void initialize() {
        try {
            chargerKPIs();
            chargerPieStatut();
            chargerPieContrat();
            chargerBarLocalisations();
            chargerBarModeTravail();
            chargerBarAvantages();
            chargerPieCoherence();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════════════════
    //  KPI CARDS (chiffres clés)
    // ══════════════════════════════════════════════════════════
    private void chargerKPIs() throws SQLException {
        int total = service.compterTotal();
        lblTotal.setText(String.valueOf(total));

        Map<String, Integer> statuts = service.statsParStatut();
        int published = statuts.getOrDefault("PUBLISHED", 0);
        lblPublished.setText(String.valueOf(published));

        Map<String, Double> salaires = service.statsSalaire();
        if (!salaires.isEmpty()) {
            lblSalaireMoyMin.setText(String.format("%.0f DT", salaires.getOrDefault("Salaire Moy. Min", 0.0)));
            lblSalaireMoyMax.setText(String.format("%.0f DT", salaires.getOrDefault("Salaire Moy. Max", 0.0)));
        } else {
            lblSalaireMoyMin.setText("—");
            lblSalaireMoyMax.setText("—");
        }
    }

    // ══════════════════════════════════════════════════════════
    //  PIE CHART : Répartition par Statut
    // ══════════════════════════════════════════════════════════
    private void chargerPieStatut() throws SQLException {
        Map<String, Integer> data = service.statsParStatut();
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();

        int totalItems = data.values().stream().mapToInt(Integer::intValue).sum();

        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            String label = formatLabel(entry.getKey());
            double pct = totalItems > 0 ? (entry.getValue() * 100.0 / totalItems) : 0;
            pieData.add(new PieChart.Data(label + " (" + String.format("%.0f", pct) + "%)", entry.getValue()));
        }

        pieStatut.setData(pieData);
        pieStatut.setTitle("Répartition par Statut");
        pieStatut.setLabelsVisible(true);
        pieStatut.setLegendVisible(true);

        addPieTooltips(pieStatut, totalItems);
    }

    // ══════════════════════════════════════════════════════════
    //  PIE CHART : Répartition par Type de Contrat
    // ══════════════════════════════════════════════════════════
    private void chargerPieContrat() throws SQLException {
        Map<String, Integer> data = service.statsParTypeContrat();
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();

        int totalItems = data.values().stream().mapToInt(Integer::intValue).sum();

        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            String label = formatLabel(entry.getKey());
            double pct = totalItems > 0 ? (entry.getValue() * 100.0 / totalItems) : 0;
            pieData.add(new PieChart.Data(label + " (" + String.format("%.0f", pct) + "%)", entry.getValue()));
        }

        pieContrat.setData(pieData);
        pieContrat.setTitle("Répartition par Contrat");
        pieContrat.setLabelsVisible(true);
        pieContrat.setLegendVisible(true);

        addPieTooltips(pieContrat, totalItems);
    }

    // ══════════════════════════════════════════════════════════
    //  BAR CHART : Top Localisations
    // ══════════════════════════════════════════════════════════
    private void chargerBarLocalisations() throws SQLException {
        Map<String, Integer> data = service.statsTopLocalisations(8);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Nombre d'offres");

        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        barLocalisations.getData().clear();
        barLocalisations.getData().add(series);
        barLocalisations.setTitle("Top Localisations");
        barLocalisations.setLegendVisible(false);
        barLocalisations.setAnimated(true);

        addBarTooltips(series);
    }

    // ══════════════════════════════════════════════════════════
    //  BAR CHART : Mode de Travail
    // ══════════════════════════════════════════════════════════
    private void chargerBarModeTravail() throws SQLException {
        Map<String, Integer> data = service.statsParModeTravail();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Nombre d'offres");

        // Couleur mapping pour le label
        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            String label = formatLabel(entry.getKey());
            series.getData().add(new XYChart.Data<>(label, entry.getValue()));
        }

        barModeTravail.getData().clear();
        barModeTravail.getData().add(series);
        barModeTravail.setTitle("Mode de Travail");
        barModeTravail.setLegendVisible(false);
        barModeTravail.setAnimated(true);

        addBarTooltips(series);
    }

    // ══════════════════════════════════════════════════════════
    //  BAR CHART : Avantages par Offre
    // ══════════════════════════════════════════════════════════
    private void chargerBarAvantages() throws SQLException {
        Map<String, Integer> data = service.statsAvantagesParOffre();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Nombre d'avantages");

        // Limiter à 10 offres max pour la lisibilité
        int count = 0;
        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            if (count++ >= 10) break;
            // Tronquer les titres longs
            String titre = entry.getKey().length() > 18
                    ? entry.getKey().substring(0, 15) + "..."
                    : entry.getKey();
            series.getData().add(new XYChart.Data<>(titre, entry.getValue()));
        }

        barAvantages.getData().clear();
        barAvantages.getData().add(series);
        barAvantages.setTitle("Avantages par Offre");
        barAvantages.setLegendVisible(false);
        barAvantages.setAnimated(true);

        addBarTooltips(series);
    }

    // ══════════════════════════════════════════════════════════
    //  UTILITAIRES
    // ══════════════════════════════════════════════════════════

    /** Ajoute des tooltips interactifs sur chaque part du PieChart */
    private void addPieTooltips(PieChart chart, int total) {
        for (PieChart.Data d : chart.getData()) {
            double pct = total > 0 ? (d.getPieValue() * 100.0 / total) : 0;
            Tooltip tooltip = new Tooltip(d.getName() + "\n" + (int) d.getPieValue() + " offres — " + String.format("%.1f%%", pct));
            tooltip.setShowDelay(Duration.millis(100));
            Tooltip.install(d.getNode(), tooltip);

            // Hover effect
            d.getNode().setOnMouseEntered(e -> d.getNode().setStyle("-fx-opacity: 0.8;"));
            d.getNode().setOnMouseExited(e -> d.getNode().setStyle("-fx-opacity: 1;"));
        }
    }

    /** Ajoute des tooltips sur chaque barre du BarChart */
    private void addBarTooltips(XYChart.Series<String, Number> series) {
        for (XYChart.Data<String, Number> d : series.getData()) {
            // Delayed because nodes are created after chart rendering
            d.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    Tooltip tooltip = new Tooltip(d.getXValue() + " : " + d.getYValue() + " offres");
                    tooltip.setShowDelay(Duration.millis(100));
                    Tooltip.install(newNode, tooltip);

                    newNode.setOnMouseEntered(e -> newNode.setStyle("-fx-opacity: 0.75;"));
                    newNode.setOnMouseExited(e -> newNode.setStyle("-fx-opacity: 1;"));
                }
            });
        }
    }

    /** Formate les labels techniques en labels lisibles */
    private String formatLabel(String raw) {
        if (raw == null) return "Non défini";
        return switch (raw) {
            case "ON_SITE"   -> "Sur site";
            case "REMOTE"    -> "Télétravail";
            case "HYBRID"    -> "Hybride";
            case "PUBLISHED" -> "Publiée";
            case "CLOSED"    -> "Fermée";
            case "ARCHIVED"  -> "Archivée";
            case "BIEN_ETRE" -> "Bien-être";
            default          -> raw;
        };
    }

    /** Retour vers la page des offres */
    @FXML
    private void retourOffres() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/offres.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) pieStatut.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("TalentFlow — Gestion des Offres");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════════════════
    //  PIE CHART : Indice de Cohérence Salariale
    // ══════════════════════════════════════════════════════════
    private void chargerPieCoherence() throws SQLException {
        Map<String, Integer> data = service.statsCoherence();
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();

        int totalItems = data.values().stream().mapToInt(Integer::intValue).sum();

        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            double pct = totalItems > 0 ? (entry.getValue() * 100.0 / totalItems) : 0;
            pieData.add(new PieChart.Data(
                    entry.getKey() + " (" + String.format("%.0f", pct) + "%)",
                    entry.getValue()));
        }

        pieCoherence.setData(pieData);
        pieCoherence.setTitle("Cohérence Salaire vs Avantages");
        pieCoherence.setLabelsVisible(true);
        pieCoherence.setLegendVisible(true);

        addPieTooltips(pieCoherence, totalItems);
    }
}
