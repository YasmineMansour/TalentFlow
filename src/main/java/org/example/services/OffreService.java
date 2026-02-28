package org.example.services;

import org.example.model.Offre;
import org.example.utils.MyConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OffreService {
    private Connection getConn() {
        return MyConnection.getInstance().getConnection();
    }

    // Auto-create tables on first use
    static {
        try {
            Connection conn = MyConnection.getInstance().getConnection();
            Statement st = conn.createStatement();
            st.executeUpdate("CREATE TABLE IF NOT EXISTS offre ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "titre VARCHAR(255) NOT NULL, "
                    + "description TEXT, "
                    + "localisation VARCHAR(255), "
                    + "type_contrat VARCHAR(50) DEFAULT 'CDI', "
                    + "mode_travail VARCHAR(50) DEFAULT 'ON_SITE', "
                    + "salaire_min DOUBLE DEFAULT 0, "
                    + "salaire_max DOUBLE DEFAULT 0, "
                    + "is_active BOOLEAN DEFAULT TRUE, "
                    + "statut VARCHAR(50) DEFAULT 'PUBLISHED')");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS avantage ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "nom VARCHAR(255) NOT NULL, "
                    + "description TEXT, "
                    + "type VARCHAR(50) DEFAULT 'AUTRE', "
                    + "offre_id INT NOT NULL, "
                    + "FOREIGN KEY (offre_id) REFERENCES offre(id) ON DELETE CASCADE)");
        } catch (Exception e) {
            System.err.println("Avertissement : impossible de creer les tables offre/avantage : " + e.getMessage());
        }
    }

    public int ajouter(Offre o) throws SQLException {
        String sql = "INSERT INTO offre (titre, description, localisation, type_contrat, mode_travail, salaire_min, salaire_max, statut, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, o.getTitre());
            ps.setString(2, o.getDescription());
            ps.setString(3, o.getLocalisation());
            ps.setString(4, o.getTypeContrat() != null ? o.getTypeContrat() : "CDI");
            ps.setString(5, o.getModeTravail() != null ? o.getModeTravail() : "ON_SITE");
            ps.setDouble(6, o.getSalaireMin());
            ps.setDouble(7, o.getSalaireMax());
            ps.setString(8, o.getStatut());
            ps.setBoolean(9, true);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int generatedId = keys.getInt(1);
                    o.setId(generatedId);
                    return generatedId;
                }
            }
        }
        return -1;
    }

    public List<Offre> afficher() throws SQLException {
        List<Offre> liste = new ArrayList<>();
        String sql = "SELECT * FROM offre ORDER BY id DESC";
        try (Statement st = getConn().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                liste.add(mapResultSetToOffre(rs));
            }
        }
        return liste;
    }

    public void modifier(Offre o) throws SQLException {
        String sql = "UPDATE offre SET titre=?, description=?, localisation=?, type_contrat=?, mode_travail=?, salaire_min=?, salaire_max=?, is_active=?, statut=? WHERE id=?";
        Connection conn = getConn();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, o.getTitre());
            ps.setString(2, o.getDescription());
            ps.setString(3, o.getLocalisation());
            ps.setString(4, o.getTypeContrat());
            ps.setString(5, o.getModeTravail());
            ps.setDouble(6, o.getSalaireMin());
            ps.setDouble(7, o.getSalaireMax());
            ps.setBoolean(8, o.isActive());
            ps.setString(9, o.getStatut());
            ps.setInt(10, o.getId());
            int rowsUpdated = ps.executeUpdate();
            if (rowsUpdated == 0) {
                throw new SQLException("Aucune ligne modifiee. L'offre ID " + o.getId() + " est introuvable.");
            }
        }
    }

    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM offre WHERE id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public Offre chercherParId(int id) throws SQLException {
        String sql = "SELECT * FROM offre WHERE id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapResultSetToOffre(rs);
            }
        }
        return null;
    }

    private Offre mapResultSetToOffre(ResultSet rs) throws SQLException {
        Offre o = new Offre();
        o.setId(rs.getInt("id"));
        o.setTitre(rs.getString("titre"));
        o.setDescription(rs.getString("description"));
        o.setLocalisation(rs.getString("localisation"));
        o.setTypeContrat(rs.getString("type_contrat"));
        o.setModeTravail(rs.getString("mode_travail"));
        o.setSalaireMin(rs.getDouble("salaire_min"));
        o.setSalaireMax(rs.getDouble("salaire_max"));
        o.setStatut(rs.getString("statut"));
        o.setActive(rs.getBoolean("is_active"));
        return o;
    }

    public List<Offre> rechercher(String keyword) throws SQLException {
        List<Offre> liste = new ArrayList<>();
        if (keyword == null) keyword = "";
        keyword = keyword.trim();
        String sql = "SELECT * FROM offre WHERE LOWER(titre) LIKE LOWER(?) OR LOWER(description) LIKE LOWER(?) OR LOWER(localisation) LIKE LOWER(?) ORDER BY id DESC";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            String pattern = "%" + keyword + "%";
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) liste.add(mapResultSetToOffre(rs));
            }
        }
        return liste;
    }

    public List<Offre> trierPar(String colonne) throws SQLException {
        List<Offre> liste = new ArrayList<>();
        if (!colonne.equals("titre") && !colonne.equals("localisation") && !colonne.equals("statut") && !colonne.equals("id")) {
            colonne = "id";
        }
        String sql = "SELECT * FROM offre ORDER BY " + colonne + " ASC";
        try (Statement st = getConn().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) liste.add(mapResultSetToOffre(rs));
        }
        return liste;
    }

    public int compterTotal() throws SQLException {
        String sql = "SELECT COUNT(*) FROM offre";
        try (Statement st = getConn().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    public boolean titreExiste(String titre, int excludeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM offre WHERE LOWER(titre) = LOWER(?) AND id != ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, titre.trim());
            ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    // ── Stats avancees ──

    public Map<String, Integer> statsParStatut() throws SQLException {
        return execGroupByCount("SELECT statut, COUNT(*) as total FROM offre GROUP BY statut");
    }

    public Map<String, Integer> statsParTypeContrat() throws SQLException {
        return execGroupByCount("SELECT type_contrat, COUNT(*) as total FROM offre GROUP BY type_contrat");
    }

    public Map<String, Integer> statsParModeTravail() throws SQLException {
        return execGroupByCount("SELECT mode_travail, COUNT(*) as total FROM offre GROUP BY mode_travail");
    }

    public Map<String, Integer> statsTopLocalisations(int limit) throws SQLException {
        Map<String, Integer> map = new LinkedHashMap<>();
        String sql = "SELECT localisation, COUNT(*) as total FROM offre WHERE localisation IS NOT NULL AND localisation <> '' GROUP BY localisation ORDER BY total DESC LIMIT ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) map.put(rs.getString(1), rs.getInt(2));
            }
        }
        return map;
    }

    public Map<String, Integer> statsAvantagesParOffre() throws SQLException {
        Map<String, Integer> map = new LinkedHashMap<>();
        String sql = "SELECT o.titre, COUNT(a.id) AS total FROM offre o LEFT JOIN avantage a ON a.offre_id = o.id GROUP BY o.id, o.titre ORDER BY total DESC";
        try (PreparedStatement ps = getConn().prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) map.put(rs.getString("titre"), rs.getInt("total"));
        }
        return map;
    }

    public Map<String, Double> statsSalaire() throws SQLException {
        Map<String, Double> map = new LinkedHashMap<>();
        String sql = "SELECT AVG(salaire_min) as avg_min, AVG(salaire_max) as avg_max, MIN(salaire_min) as global_min, MAX(salaire_max) as global_max FROM offre WHERE salaire_min > 0 OR salaire_max > 0";
        try (PreparedStatement ps = getConn().prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                map.put("Salaire Moy. Min", rs.getDouble("avg_min"));
                map.put("Salaire Moy. Max", rs.getDouble("avg_max"));
                map.put("Salaire Global Min", rs.getDouble("global_min"));
                map.put("Salaire Global Max", rs.getDouble("global_max"));
            }
        }
        return map;
    }

    public Map<Integer, Integer> scoresAttractivite() throws SQLException {
        Map<Integer, Integer> scores = new LinkedHashMap<>();
        String sql = "SELECT o.id, COALESCE(SUM(CASE WHEN a.type = 'FINANCIER' THEN 20 WHEN a.type = 'BIEN_ETRE' THEN 15 WHEN a.type = 'MATERIEL' THEN 10 ELSE 5 END), 0) AS score FROM offre o LEFT JOIN avantage a ON a.offre_id = o.id GROUP BY o.id";
        try (PreparedStatement ps = getConn().prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) scores.put(rs.getInt("id"), Math.min(rs.getInt("score"), 100));
        }
        return scores;
    }

    public Map<Integer, String> indicesCoherence() throws SQLException {
        Map<Integer, Double> salaires = new LinkedHashMap<>();
        String sql = "SELECT id, (salaire_min + salaire_max) / 2.0 AS sal_moy FROM offre";
        try (PreparedStatement ps = getConn().prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) salaires.put(rs.getInt("id"), rs.getDouble("sal_moy"));
        }

        double globalAvg = salaires.values().stream().filter(s -> s > 0).mapToDouble(Double::doubleValue).average().orElse(0);
        Map<Integer, Integer> scores = scoresAttractivite();
        Map<Integer, String> result = new LinkedHashMap<>();

        for (Map.Entry<Integer, Double> entry : salaires.entrySet()) {
            int id = entry.getKey();
            double salMoy = entry.getValue();
            int score = scores.getOrDefault(id, 0);
            boolean salaireBas = (salMoy <= globalAvg) || (salMoy == 0);
            boolean bonPackage = score >= 50;

            if (salaireBas && !bonPackage) result.put(id, "\u26a0 Faible");
            else if (salaireBas && bonPackage) result.put(id, "\ud83d\udd04 Compensee");
            else if (!salaireBas && !bonPackage) result.put(id, "\u2705 Acceptable");
            else result.put(id, "\ud83c\udf1f Excellente");
        }
        return result;
    }

    public Map<String, Integer> statsCoherence() throws SQLException {
        Map<Integer, String> indices = indicesCoherence();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String label : indices.values()) counts.merge(label, 1, Integer::sum);
        return counts;
    }

    private Map<String, Integer> execGroupByCount(String sql) throws SQLException {
        Map<String, Integer> map = new LinkedHashMap<>();
        try (PreparedStatement ps = getConn().prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String key = rs.getString(1);
                map.put(key != null ? key : "Non defini", rs.getInt(2));
            }
        }
        return map;
    }
}
