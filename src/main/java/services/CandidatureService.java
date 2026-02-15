package services;

import entities.Candidature;
import utils.DB;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CandidatureService {
    private Connection getConn() {
        return DB.getInstance().getConnection();
    }

    public void postuler(Candidature c) throws SQLException {
        String sql = "INSERT INTO candidature (nom_candidat, email, cv_path, statut, offre_id) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, c.getNomCandidat());
            ps.setString(2, c.getEmail());
            ps.setString(3, c.getCvPath());
            ps.setString(4, c.getStatut());
            ps.setInt(5, c.getOffre().getId()); // Liaison via l'ID de l'offre
            ps.executeUpdate();
        }
    }

    public List<Candidature> getCandidaturesParOffre(int offreId) throws SQLException {
        List<Candidature> liste = new ArrayList<>();
        String sql = "SELECT * FROM candidature WHERE offre_id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, offreId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Candidature c = new Candidature();
                c.setId(rs.getInt("id"));
                c.setNomCandidat(rs.getString("nom_candidat"));
                c.setEmail(rs.getString("email"));
                c.setStatut(rs.getString("statut"));
                liste.add(c);
            }
        }
        return liste;
    }
}