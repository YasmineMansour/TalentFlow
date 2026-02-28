package services;

import entities.Avantage;
import utils.DB;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AvantageService {

    private Connection getConn() {
        return DB.getInstance().getConnection();
    }

    /** Vérifie si un avantage avec le même nom existe déjà pour cette offre (insensible à la casse) */
    public boolean nomExistePourOffre(String nom, int offreId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM avantage WHERE LOWER(TRIM(nom)) = LOWER(TRIM(?)) AND offre_id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, nom);
            ps.setInt(2, offreId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    // 1. AJOUTER UN AVANTAGE
    public void ajouter(Avantage a) throws SQLException {
        String sql = "INSERT INTO avantage (nom, description, type, offre_id) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, a.getNom());
            ps.setString(2, a.getDescription());
            ps.setString(3, a.getType() != null ? a.getType() : "AUTRE");
            ps.setInt(4, a.getOffreId());
            ps.executeUpdate();
            System.out.println("Avantage ajouté en base pour l'offre ID: " + a.getOffreId());
        }
    }

    // 2. AFFICHER LES AVANTAGES D'UNE OFFRE PRÉCISE (Crucial pour ton interface)
    public List<Avantage> recupererParOffre(int idOffre) throws SQLException {
        List<Avantage> liste = new ArrayList<>();
        String sql = "SELECT * FROM avantage WHERE offre_id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, idOffre);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Avantage a = new Avantage();
                    a.setId(rs.getInt("id"));
                    a.setNom(rs.getString("nom"));
                    a.setDescription(rs.getString("description"));
                    a.setType(rs.getString("type"));
                    a.setOffreId(rs.getInt("offre_id"));
                    liste.add(a);
                }
            }
        }
        return liste;
    }

    // 3. MODIFIER UN AVANTAGE
    public void modifier(Avantage a) throws SQLException {
        String sql = "UPDATE avantage SET nom=?, description=?, type=? WHERE id=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, a.getNom());
            ps.setString(2, a.getDescription());
            ps.setString(3, a.getType());
            ps.setInt(4, a.getId());

            int rowsUpdated = ps.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("Avantage mis à jour avec succès !");
            }
        }
    }

    // 4. SUPPRIMER UN AVANTAGE
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM avantage WHERE id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("Avantage supprimé !");
        }
    }

    public int calculerScoreAttractivite(int offreId) throws SQLException {
        String sql = "SELECT type, COUNT(*) AS total " +
                "FROM avantage " +
                "WHERE offre_id = ? " +
                "GROUP BY type";

        int score = 0;

        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, offreId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String type = rs.getString("type");
                    int total = rs.getInt("total");

                    int poids = 5; // AUTRE par défaut
                    if (type != null) {
                        switch (type.toUpperCase()) {
                            case "FINANCIER": poids = 20; break;
                            case "BIEN_ETRE": poids = 15; break;
                            case "MATERIEL":  poids = 10; break;
                            case "AUTRE":     poids = 5;  break;
                        }
                    }

                    score += total * poids;
                }
            }
        }

        // Optionnel: plafonner à 100 (souvent mieux en démo)
        return Math.min(score, 100);
    }

    /** Répartition par type pour une offre donnée : type → count */
    public Map<String, Integer> statsParType(int offreId) throws SQLException {
        Map<String, Integer> map = new LinkedHashMap<>();
        String sql = "SELECT type, COUNT(*) AS total FROM avantage WHERE offre_id = ? GROUP BY type ORDER BY total DESC";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, offreId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String type = rs.getString("type");
                    map.put(type != null ? type : "AUTRE", rs.getInt("total"));
                }
            }
        }
        return map;
    }

}