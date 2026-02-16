package services;

import utils.MyDataBase;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class DashboardService {

    private final Connection connection;

    public DashboardService() {
        connection = MyDataBase.getInstance().getConnection();
    }

    // ===== KPI CARDS =====
    public int countCandidatures() throws SQLException {
        return count("SELECT COUNT(*) FROM candidature");
    }

    public int countEntretiens() throws SQLException {
        return count("SELECT COUNT(*) FROM entretien");
    }

    public int countRecrutements() throws SQLException {
        return count("SELECT COUNT(*) FROM candidature WHERE UPPER(statut)='ACCEPTE'");
    }

    private int count(String sql) throws SQLException {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    // ===== PIE: candidatures par statut =====
    public Map<String, Integer> candidaturesByStatut() throws SQLException {
        Map<String, Integer> map = new LinkedHashMap<>();
        String sql = "SELECT UPPER(statut) AS statut, COUNT(*) AS cnt " +
                "FROM candidature GROUP BY UPPER(statut) ORDER BY cnt DESC";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                map.put(rs.getString("statut"), rs.getInt("cnt"));
            }
        }
        return map;
    }

    // ===== BAR: entretiens par mois (YYYY-MM) =====
    public Map<String, Integer> entretiensParMois(int lastNMonths) throws SQLException {
        Map<String, Integer> map = new LinkedHashMap<>();
        String sql =
                "SELECT DATE_FORMAT(date_heure, '%Y-%m') AS mois, COUNT(*) AS cnt " +
                        "FROM entretien " +
                        "WHERE date_heure >= DATE_SUB(CURDATE(), INTERVAL ? MONTH) " +
                        "GROUP BY mois " +
                        "ORDER BY mois ASC";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, lastNMonths);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getString("mois"), rs.getInt("cnt"));
                }
            }
        }
        return map;
    }

    // ===== LISTE DU JOUR: entretiens d'aujourd'hui =====
    public ResultSet entretiensDuJour() throws SQLException {
        String sql =
                "SELECT e.id, e.date_heure, e.type, e.statut, c.nom_candidat, c.prenom_candidat " +
                        "FROM entretien e " +
                        "JOIN candidature c ON e.candidature_id = c.id " +
                        "WHERE DATE(e.date_heure) = CURDATE() " +
                        "ORDER BY e.date_heure ASC";
        Statement st = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        return st.executeQuery(sql);
    }
}
