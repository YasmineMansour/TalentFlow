package org.example.GUI;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import org.example.services.OffreService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

public class OffreStatistiquesController {

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
            lblSalaireMoyMin.setText("\u2014");
            lblSalaireMoyMax.setText("\u2014");
        }
    }

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
        pieStatut.setTitle("R\u00e9partition par Statut");
        pieStatut.setLabelsVisible(true);
        pieStatut.setLegendVisible(true);
        addPieTooltips(pieStatut, totalItems);
    }

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
        pieContrat.setTitle("R\u00e9partition par Contrat");
        pieContrat.setLabelsVisible(true);
        pieContrat.setLegendVisible(true);
        addPieTooltips(pieContrat, totalItems);
    }

    private void chargerBarLocalisations() throws SQLException {
        Map<String, Integer> data = service.statsTopLocalisations(8);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Nombre d'offres");
        for (Map.Entry<String, Integer> entry : data.entrySet())
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        barLocalisations.getData().clear();
        barLocalisations.getData().add(series);
        barLocalisations.setTitle("Top Localisations");
        barLocalisations.setLegendVisible(false);
        barLocalisations.setAnimated(true);
        addBarTooltips(series);
    }

    private void chargerBarModeTravail() throws SQLException {
        Map<String, Integer> data = service.statsParModeTravail();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Nombre d'offres");
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

    private void chargerBarAvantages() throws SQLException {
        Map<String, Integer> data = service.statsAvantagesParOffre();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Nombre d'avantages");
        int count = 0;
        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            if (count++ >= 10) break;
            String titre = entry.getKey().length() > 18 ? entry.getKey().substring(0, 15) + "..." : entry.getKey();
            series.getData().add(new XYChart.Data<>(titre, entry.getValue()));
        }
        barAvantages.getData().clear();
        barAvantages.getData().add(series);
        barAvantages.setTitle("Avantages par Offre");
        barAvantages.setLegendVisible(false);
        barAvantages.setAnimated(true);
        addBarTooltips(series);
    }

    private void addPieTooltips(PieChart chart, int total) {
        for (PieChart.Data d : chart.getData()) {
            double pct = total > 0 ? (d.getPieValue() * 100.0 / total) : 0;
            Tooltip tooltip = new Tooltip(d.getName() + "\n" + (int) d.getPieValue() + " offres \u2014 " + String.format("%.1f%%", pct));
            tooltip.setShowDelay(Duration.millis(100));
            Tooltip.install(d.getNode(), tooltip);
            d.getNode().setOnMouseEntered(e -> d.getNode().setStyle("-fx-opacity: 0.8;"));
            d.getNode().setOnMouseExited(e -> d.getNode().setStyle("-fx-opacity: 1;"));
        }
    }

    private void addBarTooltips(XYChart.Series<String, Number> series) {
        for (XYChart.Data<String, Number> d : series.getData()) {
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

    private String formatLabel(String raw) {
        if (raw == null) return "Non d\u00e9fini";
        return switch (raw) {
            case "ON_SITE"   -> "Sur site";
            case "REMOTE"    -> "T\u00e9l\u00e9travail";
            case "HYBRID"    -> "Hybride";
            case "PUBLISHED" -> "Publi\u00e9e";
            case "CLOSED"    -> "Ferm\u00e9e";
            case "ARCHIVED"  -> "Archiv\u00e9e";
            case "BIEN_ETRE" -> "Bien-\u00eatre";
            default          -> raw;
        };
    }

    @FXML
    private void retourOffres() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/OffresView.fxml"));
            Parent view = loader.load();
            StackPane contentArea = (StackPane) pieStatut.getScene().lookup(".content-area");
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
        }
    }

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
        pieCoherence.setTitle("Coh\u00e9rence Salaire vs Avantages");
        pieCoherence.setLabelsVisible(true);
        pieCoherence.setLegendVisible(true);
        addPieTooltips(pieCoherence, totalItems);
    }
}
