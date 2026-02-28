package org.example.services;

import org.example.utils.MyConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;

public class DashboardService {

    private Connection cnx() throws SQLException {
        Connection c = MyConnection.getInstance().getConnection();
        if (c == null || c.isClosed()) throw new SQLException("Connexion BD indisponible.");
        return c;
    }

    public int countEntretiens() throws SQLException {
        String sql = "SELECT COUNT(*) FROM entretien";
        try (Statement st = cnx().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    public int countEntretiensParStatut(String statut) throws SQLException {
        String sql = "SELECT COUNT(*) FROM entretien WHERE statut = ?";
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setString(1, statut);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public Map<String, Integer> entretiensByStatut() throws SQLException {
        String sql = "SELECT statut, COUNT(*) cnt FROM entretien GROUP BY statut ORDER BY cnt DESC";
        Map<String, Integer> map = new LinkedHashMap<>();
        try (Statement st = cnx().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                map.put(rs.getString("statut"), rs.getInt("cnt"));
            }
        }
        return map;
    }

    public Map<String, Integer> entretiensParMois(int monthsBack) throws SQLException {
        Map<String, Integer> result = new LinkedHashMap<>();
        YearMonth now = YearMonth.now();

        for (int i = monthsBack - 1; i >= 0; i--) {
            YearMonth ym = now.minusMonths(i);
            String key = ym.getMonthValue() + "/" + ym.getYear();
            result.put(key, 0);
        }

        String sql =
                "SELECT YEAR(date_heure) y, MONTH(date_heure) m, COUNT(*) cnt " +
                        "FROM entretien " +
                        "WHERE date_heure >= ? " +
                        "GROUP BY YEAR(date_heure), MONTH(date_heure) " +
                        "ORDER BY y, m";

        LocalDate start = now.minusMonths(monthsBack - 1).atDay(1);

        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(start.atStartOfDay()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int y = rs.getInt("y");
                    int m = rs.getInt("m");
                    int cnt = rs.getInt("cnt");
                    String key = m + "/" + y;
                    if (result.containsKey(key)) result.put(key, cnt);
                }
            }
        }
        return result;
    }

    public ResultSet entretiensDuJour() throws SQLException {
        String sql =
                "SELECT date_heure, type, statut, lieu, lien " +
                        "FROM entretien " +
                        "WHERE DATE(date_heure) = CURDATE() " +
                        "ORDER BY date_heure ASC";

        PreparedStatement ps = cnx().prepareStatement(sql);
        return ps.executeQuery();
    }

    // ─────── Decision stats ───────

    public int countDecisions() throws SQLException {
        String sql = "SELECT COUNT(*) FROM decision_finale";
        try (Statement st = cnx().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    public int countDecisionsParType(String type) throws SQLException {
        String sql = "SELECT COUNT(*) FROM decision_finale WHERE decision = ?";
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setString(1, type);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public Map<String, Integer> decisionsByType() throws SQLException {
        String sql = "SELECT decision, COUNT(*) cnt FROM decision_finale GROUP BY decision ORDER BY cnt DESC";
        Map<String, Integer> map = new LinkedHashMap<>();
        try (Statement st = cnx().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                map.put(rs.getString("decision"), rs.getInt("cnt"));
            }
        }
        return map;
    }

    public double tauxAcceptation() throws SQLException {
        int total = countDecisions();
        if (total == 0) return 0;
        int acceptes = countDecisionsParType("ACCEPTE");
        return Math.round((acceptes * 100.0 / total) * 10) / 10.0;
    }

    // ─────── Analytics RH avancés ───────

    public double tauxAnnulation() throws SQLException {
        int total = countEntretiens();
        if (total == 0) return 0;
        int annules = countEntretiensParStatut("ANNULE");
        return Math.round((annules * 100.0 / total) * 10) / 10.0;
    }

    public double moyenneNoteTechnique() throws SQLException {
        String sql = "SELECT AVG(note_technique) FROM entretien WHERE note_technique IS NOT NULL";
        try (Statement st = cnx().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return Math.round(rs.getDouble(1) * 10) / 10.0;
        }
    }

    public double moyenneNoteCommunication() throws SQLException {
        String sql = "SELECT AVG(note_communication) FROM entretien WHERE note_communication IS NOT NULL";
        try (Statement st = cnx().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return Math.round(rs.getDouble(1) * 10) / 10.0;
        }
    }

    public double moyenneGenerale() throws SQLException {
        String sql = "SELECT AVG((note_technique + note_communication) / 2.0) " +
                     "FROM entretien WHERE note_technique IS NOT NULL AND note_communication IS NOT NULL";
        try (Statement st = cnx().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return Math.round(rs.getDouble(1) * 10) / 10.0;
        }
    }

    public Map<String, Integer> entretiensByType() throws SQLException {
        String sql = "SELECT type, COUNT(*) cnt FROM entretien GROUP BY type ORDER BY cnt DESC";
        Map<String, Integer> map = new LinkedHashMap<>();
        try (Statement st = cnx().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                map.put(rs.getString("type"), rs.getInt("cnt"));
            }
        }
        return map;
    }

    public Map<String, Integer> distributionNotes() throws SQLException {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("0-5", 0);
        map.put("6-10", 0);
        map.put("11-15", 0);
        map.put("16-20", 0);

        String sql = "SELECT " +
                "SUM(CASE WHEN note_technique BETWEEN 0 AND 5 THEN 1 ELSE 0 END) t1, " +
                "SUM(CASE WHEN note_technique BETWEEN 6 AND 10 THEN 1 ELSE 0 END) t2, " +
                "SUM(CASE WHEN note_technique BETWEEN 11 AND 15 THEN 1 ELSE 0 END) t3, " +
                "SUM(CASE WHEN note_technique BETWEEN 16 AND 20 THEN 1 ELSE 0 END) t4 " +
                "FROM entretien WHERE note_technique IS NOT NULL";

        try (Statement st = cnx().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                map.put("0-5", rs.getInt("t1"));
                map.put("6-10", rs.getInt("t2"));
                map.put("11-15", rs.getInt("t3"));
                map.put("16-20", rs.getInt("t4"));
            }
        }
        return map;
    }

    public int countEntretiensNotes() throws SQLException {
        String sql = "SELECT COUNT(*) FROM entretien WHERE note_technique IS NOT NULL AND note_communication IS NOT NULL";
        try (Statement st = cnx().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
