package services;

import entities.Offre;
import utils.DB;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OffreService {
    private Connection getConn() {
        return DB.getInstance().getConnection();
    }

    // 1. AJOUTER UNE OFFRE (retourne l'ID généré)
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
                    System.out.println("Offre ajoutée en base ! ID: " + generatedId);
                    return generatedId;
                }
            }
        }
        return -1;
    }

    // 2. AFFICHER TOUTES LES OFFRES
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

    // 3. MODIFIER UNE OFFRE (met à jour TOUS les champs)
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
            if (!conn.getAutoCommit()) {
                conn.commit();
            }
            if (rowsUpdated > 0) {
                System.out.println("Mise à jour réussie pour l'ID: " + o.getId());
            } else {
                throw new SQLException("Aucune ligne modifiée. L'offre ID " + o.getId() + " est introuvable.");
            }
        }
    }

    // 4. SUPPRIMER UNE OFFRE
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM offre WHERE id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("Offre supprimée avec succès !");
        }
    }

    // 5. CHERCHER PAR ID (charge TOUS les champs)
    public Offre chercherParId(int id) throws SQLException {
        String sql = "SELECT * FROM offre WHERE id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToOffre(rs);
                }
            }
        }
        return null;
    }

    // Helper : convertit un ResultSet en Offre (évite la duplication)
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

    // 6. RECHERCHER (titre/description/localisation)
    public List<Offre> rechercher(String keyword) throws SQLException {
        List<Offre> liste = new ArrayList<>();

        if (keyword == null) keyword = "";
        keyword = keyword.trim();

        String sql = "SELECT * FROM offre " +
                "WHERE LOWER(titre) LIKE LOWER(?) " +
                "OR LOWER(description) LIKE LOWER(?) " +
                "OR LOWER(localisation) LIKE LOWER(?) " +
                "ORDER BY id DESC";

        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            String pattern = "%" + keyword + "%";
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    liste.add(mapResultSetToOffre(rs));
                }
            }
        }

        return liste;
    }

    public String statistiquesParStatut() throws SQLException {
        String sql = "SELECT statut, COUNT(*) as total FROM offre GROUP BY statut";
        StringBuilder result = new StringBuilder();

        try (PreparedStatement ps = getConn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String statut = rs.getString("statut");
                int total = rs.getInt("total");
                result.append(statut).append(" : ").append(total).append("\n");
            }
        }

        return result.toString();
    }

    public String topLocalisations(int limit) throws SQLException {
        String sql = "SELECT localisation, COUNT(*) as total " +
                "FROM offre " +
                "WHERE localisation IS NOT NULL AND localisation <> '' " +
                "GROUP BY localisation " +
                "ORDER BY total DESC " +
                "LIMIT ?";

        StringBuilder sb = new StringBuilder("Top localisations :\n");

        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, limit);

            try (ResultSet rs = ps.executeQuery()) {
                int rank = 1;
                while (rs.next()) {
                    String loc = rs.getString("localisation");
                    int total = rs.getInt("total");
                    sb.append(rank++).append(") ").append(loc).append(" : ").append(total).append("\n");
                }
            }
        }

        return sb.toString();
    }

    public String nombreAvantagesParOffre() throws SQLException {
        String sql =
                "SELECT o.titre, COUNT(a.id) AS total " +
                        "FROM offre o " +
                        "LEFT JOIN avantage a ON a.offre_id = o.id " +
                        "GROUP BY o.id, o.titre " +
                        "ORDER BY total DESC";

        StringBuilder sb = new StringBuilder("Nombre d'avantages par offre :\n");

        try (PreparedStatement ps = getConn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String titre = rs.getString("titre");
                int total = rs.getInt("total");
                sb.append("- ").append(titre).append(" : ").append(total).append("\n");
            }
        }

        return sb.toString();
    }

    public List<Offre> trierPar(String colonne) throws SQLException {
        List<Offre> liste = new ArrayList<>();

        // Sécurité : on autorise seulement certaines colonnes
        if (!colonne.equals("titre") &&
                !colonne.equals("localisation") &&
                !colonne.equals("statut") &&
                !colonne.equals("id")) {

            colonne = "id";
        }

        String sql = "SELECT * FROM offre ORDER BY " + colonne + " ASC";

        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                liste.add(mapResultSetToOffre(rs));
            }
        }

        return liste;
    }

    // COMPTER le nombre total d'offres
    public int compterTotal() throws SQLException {
        String sql = "SELECT COUNT(*) FROM offre";
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    // VERIFIER si un titre existe déjà (pour éviter les doublons)
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

    // ══════════════════════════════════════════════════════════
    //  STATISTIQUES AVANCÉES (retournent des Map pour les charts)
    // ══════════════════════════════════════════════════════════

    /** Répartition par statut : PUBLISHED, CLOSED, ARCHIVED → count */
    public Map<String, Integer> statsParStatut() throws SQLException {
        return execGroupByCount("SELECT statut, COUNT(*) as total FROM offre GROUP BY statut");
    }

    /** Répartition par type de contrat : CDI, CDD, Stage, etc. → count */
    public Map<String, Integer> statsParTypeContrat() throws SQLException {
        return execGroupByCount("SELECT type_contrat, COUNT(*) as total FROM offre GROUP BY type_contrat");
    }

    /** Répartition par mode de travail : ON_SITE, REMOTE, HYBRID → count */
    public Map<String, Integer> statsParModeTravail() throws SQLException {
        return execGroupByCount("SELECT mode_travail, COUNT(*) as total FROM offre GROUP BY mode_travail");
    }

    /** Top N localisations → count (ordonnées par fréquence) */
    public Map<String, Integer> statsTopLocalisations(int limit) throws SQLException {
        Map<String, Integer> map = new LinkedHashMap<>();
        String sql = "SELECT localisation, COUNT(*) as total FROM offre " +
                     "WHERE localisation IS NOT NULL AND localisation <> '' " +
                     "GROUP BY localisation ORDER BY total DESC LIMIT ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getString(1), rs.getInt(2));
                }
            }
        }
        return map;
    }

    /** Nombre d'avantages par offre → Map(titre offre → count) */
    public Map<String, Integer> statsAvantagesParOffre() throws SQLException {
        Map<String, Integer> map = new LinkedHashMap<>();
        String sql = "SELECT o.titre, COUNT(a.id) AS total FROM offre o " +
                     "LEFT JOIN avantage a ON a.offre_id = o.id " +
                     "GROUP BY o.id, o.titre ORDER BY total DESC";
        try (PreparedStatement ps = getConn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getString("titre"), rs.getInt("total"));
            }
        }
        return map;
    }

    /** Statistiques de salaire : salaire_moy_min, salaire_moy_max, salaire_global_min, salaire_global_max */
    public Map<String, Double> statsSalaire() throws SQLException {
        Map<String, Double> map = new LinkedHashMap<>();
        String sql = "SELECT " +
                     "  AVG(salaire_min) as avg_min, AVG(salaire_max) as avg_max, " +
                     "  MIN(salaire_min) as global_min, MAX(salaire_max) as global_max " +
                     "FROM offre WHERE salaire_min > 0 OR salaire_max > 0";
        try (PreparedStatement ps = getConn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                map.put("Salaire Moy. Min", rs.getDouble("avg_min"));
                map.put("Salaire Moy. Max", rs.getDouble("avg_max"));
                map.put("Salaire Global Min", rs.getDouble("global_min"));
                map.put("Salaire Global Max", rs.getDouble("global_max"));
            }
        }
        return map;
    }

    /** Scores d'attractivité pour toutes les offres : offreId → score (plafonné à 100) */
    public Map<Integer, Integer> scoresAttractivite() throws SQLException {
        Map<Integer, Integer> scores = new LinkedHashMap<>();
        String sql = "SELECT o.id, " +
                     "  COALESCE(SUM(CASE " +
                     "    WHEN a.type = 'FINANCIER' THEN 20 " +
                     "    WHEN a.type = 'BIEN_ETRE' THEN 15 " +
                     "    WHEN a.type = 'MATERIEL'  THEN 10 " +
                     "    ELSE 5 END), 0) AS score " +
                     "FROM offre o LEFT JOIN avantage a ON a.offre_id = o.id " +
                     "GROUP BY o.id";
        try (PreparedStatement ps = getConn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                scores.put(rs.getInt("id"), Math.min(rs.getInt("score"), 100));
            }
        }
        return scores;
    }

    /**
     * Indice de cohérence salariale vs avantages pour chaque offre.
     * Compare le salaire moyen de chaque offre à la moyenne globale,
     * puis croise avec le score d'attractivité des avantages.
     *
     * Matrice :
     *   Salaire bas  + peu d'avantages  → ⚠ Offre Faible
     *   Salaire bas  + bon package      → 🔄 Compensée
     *   Salaire haut + peu d'avantages  → ✅ Acceptable
     *   Salaire haut + bon package      → 🌟 Excellente
     *
     * @return offreId → label de cohérence
     */
    public Map<Integer, String> indicesCoherence() throws SQLException {
        // 1. Salaire moyen par offre
        Map<Integer, Double> salaires = new LinkedHashMap<>();
        String sql = "SELECT id, (salaire_min + salaire_max) / 2.0 AS sal_moy FROM offre";
        try (PreparedStatement ps = getConn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                salaires.put(rs.getInt("id"), rs.getDouble("sal_moy"));
            }
        }

        // 2. Moyenne globale (offres ayant un salaire renseigné)
        double globalAvg = salaires.values().stream()
                .filter(s -> s > 0)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0);

        // 3. Score d'attractivité
        Map<Integer, Integer> scores = scoresAttractivite();

        // 4. Croisement salaire × avantages
        Map<Integer, String> result = new LinkedHashMap<>();
        for (Map.Entry<Integer, Double> entry : salaires.entrySet()) {
            int id = entry.getKey();
            double salMoy = entry.getValue();
            int score = scores.getOrDefault(id, 0);

            boolean salaireBas = (salMoy <= globalAvg) || (salMoy == 0);
            boolean bonPackage = score >= 50;

            if (salaireBas && !bonPackage) {
                result.put(id, "\u26a0 Faible");
            } else if (salaireBas && bonPackage) {
                result.put(id, "\ud83d\udd04 Compensée");
            } else if (!salaireBas && !bonPackage) {
                result.put(id, "\u2705 Acceptable");
            } else {
                result.put(id, "\ud83c\udf1f Excellente");
            }
        }
        return result;
    }

    /** Répartition des indices de cohérence : label → count (pour PieChart) */
    public Map<String, Integer> statsCoherence() throws SQLException {
        Map<Integer, String> indices = indicesCoherence();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String label : indices.values()) {
            counts.merge(label, 1, Integer::sum);
        }
        return counts;
    }

    /** Helper générique : exécute un SELECT col, COUNT(*)... et retourne un Map ordonné */
    private Map<String, Integer> execGroupByCount(String sql) throws SQLException {
        Map<String, Integer> map = new LinkedHashMap<>();
        try (PreparedStatement ps = getConn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String key = rs.getString(1);
                map.put(key != null ? key : "Non défini", rs.getInt(2));
            }
        }
        return map;
    }
}
