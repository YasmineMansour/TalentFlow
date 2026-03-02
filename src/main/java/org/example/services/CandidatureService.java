package org.example.services;

import org.example.model.Candidature;
import org.example.utils.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CandidatureService {

    private Connection getConn() {
        return MyConnection.getInstance().getConnection();
    }

    // Auto-create table on first use with proper foreign keys
    static {
        try {
            Connection conn = MyConnection.getInstance().getConnection();
            Statement st = conn.createStatement();

            // Désactiver les vérifications FK pour forcer le DROP
            st.executeUpdate("SET FOREIGN_KEY_CHECKS = 0");
            st.executeUpdate("DROP TABLE IF EXISTS piece_jointe");
            st.executeUpdate("DROP TABLE IF EXISTS candidature");
            st.executeUpdate("SET FOREIGN_KEY_CHECKS = 1");

            st.executeUpdate("CREATE TABLE candidature ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "user_id INT NOT NULL, "
                    + "offre_id INT NOT NULL, "
                    + "cv_url VARCHAR(500), "
                    + "motivation TEXT, "
                    + "date_postulation TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "statut VARCHAR(50) DEFAULT 'EN_ATTENTE', "
                    + "email VARCHAR(255), "
                    + "langue VARCHAR(50) DEFAULT 'INCONNU', "
                    + "FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE, "
                    + "FOREIGN KEY (offre_id) REFERENCES offre(id) ON DELETE CASCADE"
                    + ")");

            st.executeUpdate("CREATE TABLE piece_jointe ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "candidature_id INT NOT NULL, "
                    + "titre VARCHAR(255) NOT NULL, "
                    + "type_doc VARCHAR(50) NOT NULL, "
                    + "url VARCHAR(500) NOT NULL, "
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "FOREIGN KEY (candidature_id) REFERENCES candidature(id) ON DELETE CASCADE"
                    + ")");

            System.out.println("✅ Tables candidature + piece_jointe créées avec succès.");
        } catch (Exception e) {
            System.err.println("❌ Erreur création tables candidature : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void add(Candidature c) throws SQLException {
        String sql = "INSERT INTO candidature (user_id, offre_id, cv_url, motivation, statut, email, langue) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, c.getUserId());
            ps.setInt(2, c.getOffreId());
            ps.setString(3, c.getCvUrl());
            ps.setString(4, c.getMotivation());
            ps.setString(5, c.getStatut());
            ps.setString(6, c.getEmail());
            ps.setString(7, c.getLangue() == null ? "INCONNU" : c.getLangue());
            ps.executeUpdate();
        }
    }

    public List<Candidature> getAll() throws SQLException {
        List<Candidature> list = new ArrayList<>();
        String sql = "SELECT c.*, u.prenom, u.nom, o.titre AS titre_offre "
                + "FROM candidature c "
                + "LEFT JOIN user u ON c.user_id = u.id "
                + "LEFT JOIN offre o ON c.offre_id = o.id "
                + "ORDER BY c.date_postulation DESC";
        try (Statement st = getConn().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapWithJoins(rs));
            }
        }
        return list;
    }

    public List<Candidature> getByUser(int userId) throws SQLException {
        List<Candidature> list = new ArrayList<>();
        String sql = "SELECT c.*, u.prenom, u.nom, o.titre AS titre_offre "
                + "FROM candidature c "
                + "LEFT JOIN user u ON c.user_id = u.id "
                + "LEFT JOIN offre o ON c.offre_id = o.id "
                + "WHERE c.user_id = ? "
                + "ORDER BY c.date_postulation DESC";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapWithJoins(rs));
                }
            }
        }
        return list;
    }

    public List<Candidature> getByOffre(int offreId) throws SQLException {
        List<Candidature> list = new ArrayList<>();
        String sql = "SELECT c.*, u.prenom, u.nom, o.titre AS titre_offre "
                + "FROM candidature c "
                + "LEFT JOIN user u ON c.user_id = u.id "
                + "LEFT JOIN offre o ON c.offre_id = o.id "
                + "WHERE c.offre_id = ? "
                + "ORDER BY c.date_postulation DESC";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, offreId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapWithJoins(rs));
                }
            }
        }
        return list;
    }

    public void updateStatut(int id, String statut) throws SQLException {
        String sql = "UPDATE candidature SET statut = ? WHERE id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, statut);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void update(Candidature c) throws SQLException {
        String sql = "UPDATE candidature SET cv_url=?, motivation=?, email=? WHERE id=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, c.getCvUrl());
            ps.setString(2, c.getMotivation());
            ps.setString(3, c.getEmail());
            ps.setInt(4, c.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        // Les pièces jointes seront supprimées en cascade
        String sql = "DELETE FROM candidature WHERE id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public boolean alreadyApplied(int userId, int offreId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM candidature WHERE user_id=? AND offre_id=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, offreId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public int countByStatut(String statut) throws SQLException {
        String sql = "SELECT COUNT(*) FROM candidature WHERE statut = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, statut);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    public int countTotal() throws SQLException {
        String sql = "SELECT COUNT(*) FROM candidature";
        try (Statement st = getConn().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    public void updateLangue(int id, String langue) throws SQLException {
        String sql = "UPDATE candidature SET langue = ? WHERE id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, langue);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    // Mapping complet avec jointures (user + offre)
    private Candidature mapWithJoins(ResultSet rs) throws SQLException {
        Candidature c = new Candidature();
        c.setId(rs.getInt("id"));
        c.setUserId(rs.getInt("user_id"));
        c.setOffreId(rs.getInt("offre_id"));
        c.setCvUrl(rs.getString("cv_url"));
        c.setMotivation(rs.getString("motivation"));
        c.setDatePostulation(rs.getTimestamp("date_postulation"));
        c.setStatut(rs.getString("statut"));
        c.setEmail(rs.getString("email"));
        c.setLangue(rs.getString("langue"));

        // Jointures
        try {
            String prenom = rs.getString("prenom");
            String nom = rs.getString("nom");
            if (prenom != null && nom != null) {
                c.setNomCandidat(prenom + " " + nom);
            }
        } catch (SQLException ignored) {}

        try {
            String titreOffre = rs.getString("titre_offre");
            if (titreOffre != null) {
                c.setTitreOffre(titreOffre);
            }
        } catch (SQLException ignored) {}

        return c;
    }
}
