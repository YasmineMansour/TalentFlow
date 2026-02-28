package org.example.GUI;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import org.example.services.DashboardService;
import org.example.services.EntretienService;
import org.example.utils.DialogUtil;

import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class EntretienDashboardController {

    // ── Row 1 KPI ──
    @FXML private Label lblTotalEntretiens;
    @FXML private Label lblPlanifies;
    @FXML private Label lblRealises;
    @FXML private Label lblAcceptes;
    @FXML private Label lblTauxReussite;

    // ── Row 2 KPI (analytics) ──
    @FXML private Label lblMoyTech;
    @FXML private Label lblMoyCom;
    @FXML private Label lblMoyGen;
    @FXML private Label lblTauxAnnulation;
    @FXML private Label lblEntretiensNotes;

    // ── Charts ──
    @FXML private PieChart pieStatuts;
    @FXML private PieChart pieDecisions;
    @FXML private PieChart pieTypes;
    @FXML private BarChart<String, Number> barEntretiens;
    @FXML private BarChart<String, Number> barDistribution;

    @FXML private ListView<String> lvToday;

    private final DashboardService service = new DashboardService();
    private final EntretienService entretienService = new EntretienService();

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    public void initialize() {
        try {
            entretienService.autoUpdateStatuts();

            loadCards();
            loadAnalyticsCards();
            loadPieStatuts();
            loadPieDecisions();
            loadPieTypes();
            loadBar();
            loadBarDistribution();
            loadTodayList();
        } catch (Exception e) {
            DialogUtil.error("Erreur", "Dashboard: " + e.getMessage());
        }
    }

    private void loadCards() throws Exception {
        int total     = service.countEntretiens();
        int planifies = service.countEntretiensParStatut("PLANIFIE");
        int realises  = service.countEntretiensParStatut("REALISE");
        int acceptes  = service.countDecisionsParType("ACCEPTE");
        double taux   = service.tauxAcceptation();

        lblTotalEntretiens.setText(String.valueOf(total));
        lblPlanifies.setText(String.valueOf(planifies));
        lblRealises.setText(String.valueOf(realises));
        lblAcceptes.setText(String.valueOf(acceptes));
        lblTauxReussite.setText(taux + " %");
    }

    private void loadAnalyticsCards() throws Exception {
        lblMoyTech.setText(service.moyenneNoteTechnique() + " / 20");
        lblMoyCom.setText(service.moyenneNoteCommunication() + " / 20");
        lblMoyGen.setText(service.moyenneGenerale() + " / 20");
        lblTauxAnnulation.setText(service.tauxAnnulation() + " %");
        lblEntretiensNotes.setText(String.valueOf(service.countEntretiensNotes()));
    }

    private void loadPieStatuts() throws Exception {
        Map<String, Integer> stats = service.entretiensByStatut();
        ObservableList<PieChart.Data> data = FXCollections.observableArrayList();
        stats.forEach((statut, count) ->
                data.add(new PieChart.Data(statut + " (" + count + ")", count)));

        pieStatuts.setData(data);
        pieStatuts.setLegendVisible(true);
        pieStatuts.setLegendSide(Side.BOTTOM);
        pieStatuts.setLabelsVisible(true);
        pieStatuts.setStartAngle(90);
        applyPieColors(pieStatuts, stats);
    }

    private void loadPieDecisions() throws Exception {
        Map<String, Integer> stats = service.decisionsByType();
        ObservableList<PieChart.Data> data = FXCollections.observableArrayList();
        stats.forEach((type, count) ->
                data.add(new PieChart.Data(type + " (" + count + ")", count)));

        pieDecisions.setData(data);
        pieDecisions.setLegendVisible(true);
        pieDecisions.setLegendSide(Side.BOTTOM);
        pieDecisions.setLabelsVisible(true);
        pieDecisions.setStartAngle(90);
        applyDecisionColors(pieDecisions, stats);
    }

    private void loadPieTypes() throws Exception {
        Map<String, Integer> stats = service.entretiensByType();
        ObservableList<PieChart.Data> data = FXCollections.observableArrayList();
        stats.forEach((type, count) ->
                data.add(new PieChart.Data(type + " (" + count + ")", count)));

        pieTypes.setData(data);
        pieTypes.setLegendVisible(true);
        pieTypes.setLegendSide(Side.BOTTOM);
        pieTypes.setLabelsVisible(true);
        pieTypes.setStartAngle(90);

        String[] colors = {"#1abc9c", "#e67e22", "#9b59b6", "#2980b9", "#e74c3c"};
        int i = 0;
        for (PieChart.Data d : pieTypes.getData()) {
            d.getNode().setStyle("-fx-pie-color: " + colors[i % colors.length] + ";");
            i++;
        }
    }

    private void loadBar() throws Exception {
        barEntretiens.getData().clear();
        Map<String, Integer> stats = service.entretiensParMois(6);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Entretiens");
        stats.forEach((mois, count) ->
                series.getData().add(new XYChart.Data<>(mois, count)));
        barEntretiens.getData().add(series);
        barEntretiens.setAnimated(false);
        barEntretiens.setCategoryGap(20);
        barEntretiens.setBarGap(5);
    }

    private void loadBarDistribution() throws Exception {
        barDistribution.getData().clear();
        Map<String, Integer> stats = service.distributionNotes();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Candidats");
        stats.forEach((tranche, count) ->
                series.getData().add(new XYChart.Data<>(tranche, count)));
        barDistribution.getData().add(series);
        barDistribution.setAnimated(false);
        barDistribution.setCategoryGap(15);
        barDistribution.setBarGap(3);
    }

    private void loadTodayList() throws Exception {
        ObservableList<String> items = FXCollections.observableArrayList();
        try (ResultSet rs = service.entretiensDuJour()) {
            while (rs.next()) {
                LocalDateTime dt = rs.getTimestamp("date_heure").toLocalDateTime();
                String type   = rs.getString("type");
                String statut = rs.getString("statut");
                String lieu   = rs.getString("lieu");
                String lien   = rs.getString("lien");

                String extra = "";
                if (lieu != null && !lieu.isBlank()) {
                    extra = " | " + lieu;
                } else if (lien != null && !lien.isBlank()) {
                    extra = " | " + lien;
                }
                items.add(TIME_FMT.format(dt.toLocalTime())
                        + " - " + type + " | " + statut + extra);
            }
        }
        if (items.isEmpty()) {
            items.add("Aucun entretien prévu aujourd'hui.");
        }
        lvToday.setItems(items);
    }

    private void applyPieColors(PieChart chart, Map<String, Integer> stats) {
        String[] colors = {"#3498db", "#2ecc71", "#e74c3c", "#f39c12", "#9b59b6"};
        int i = 0;
        for (PieChart.Data d : chart.getData()) {
            d.getNode().setStyle("-fx-pie-color: " + colors[i % colors.length] + ";");
            i++;
        }
    }

    private void applyDecisionColors(PieChart chart, Map<String, Integer> stats) {
        for (PieChart.Data d : chart.getData()) {
            String name = d.getName().toUpperCase();
            String color;
            if (name.startsWith("ACCEPTE"))        color = "#2ecc71";
            else if (name.startsWith("REFUSE"))    color = "#e74c3c";
            else if (name.startsWith("EN_ATTENTE")) color = "#f39c12";
            else                                     color = "#95a5a6";
            d.getNode().setStyle("-fx-pie-color: " + color + ";");
        }
    }
}
