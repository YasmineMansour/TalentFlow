package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import services.DashboardService;
import utils.DialogUtil;

import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class DashboardController {

    @FXML private Label lblTotalCandidatures;
    @FXML private Label lblTotalEntretiens;
    @FXML private Label lblRecrutements;

    @FXML private PieChart pieCandidatures;
    @FXML private BarChart<String, Number> barEntretiens;
    @FXML private CategoryAxis barX;
    @FXML private NumberAxis barY;

    @FXML private ListView<String> lvToday;

    private final DashboardService service = new DashboardService();

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    public void initialize() {
        try {
            loadCards();
            loadPie();
            loadBar();
            loadTodayList();
        } catch (Exception e) {
            DialogUtil.error("Erreur", "Dashboard: " + e.getMessage());
        }
    }

    private void loadCards() throws Exception {
        lblTotalCandidatures.setText(String.valueOf(service.countCandidatures()));
        lblTotalEntretiens.setText(String.valueOf(service.countEntretiens()));
        lblRecrutements.setText(String.valueOf(service.countRecrutements()));
    }

    private void loadPie() throws Exception {
        Map<String, Integer> stats = service.candidaturesByStatut();
        ObservableList<PieChart.Data> data = FXCollections.observableArrayList();
        stats.forEach((statut, cnt) -> data.add(new PieChart.Data(statut + " (" + cnt + ")", cnt)));
        pieCandidatures.setData(data);
        pieCandidatures.setLegendVisible(true);
    }

    private void loadBar() throws Exception {
        barEntretiens.getData().clear();

        Map<String, Integer> m = service.entretiensParMois(6);
        XYChart.Series<String, Number> s = new XYChart.Series<>();
        s.setName("Entretiens");

        m.forEach((mois, cnt) -> s.getData().add(new XYChart.Data<>(mois, cnt)));

        barEntretiens.getData().add(s);
    }

    private void loadTodayList() throws Exception {
        ObservableList<String> items = FXCollections.observableArrayList();

        try (ResultSet rs = service.entretiensDuJour()) {
            while (rs.next()) {
                String nom = rs.getString("nom_candidat");
                String prenom = rs.getString("prenom_candidat");
                LocalDateTime dt = rs.getTimestamp("date_heure").toLocalDateTime();
                String type = rs.getString("type");
                String statut = rs.getString("statut");

                items.add(DT_FMT.format(dt.toLocalTime()) + " - " + nom + " " + prenom + " | " + type + " | " + statut);
            }
        }

        if (items.isEmpty()) items.add("Aucun entretien prévu aujourd'hui.");
        lvToday.setItems(items);
    }
}
