package services;

import entities.Offre;
import utils.DB;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OffreService {
    private Connection getConn() {
        return DB.getInstance().getConnection();
    }

    // 1. AJOUTER UNE OFFRE
    public void ajouter(Offre o) throws SQLException {
        String sql = "INSERT INTO offre (titre, description, localisation, type_contrat, mode_travail, statut, is_active) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, o.getTitre());
            ps.setString(2, o.getDescription());
            ps.setString(3, o.getLocalisation());
            ps.setString(4, o.getTypeContrat() != null ? o.getTypeContrat() : "CDI");
            ps.setString(5, o.getModeTravail() != null ? o.getModeTravail() : "ON_SITE");
            ps.setString(6, o.getStatut());
            ps.setBoolean(7, true); // Par défaut active lors de l'ajout
            ps.executeUpdate();
            System.out.println("Offre ajoutée en base !");
        }
    }

    // 2. AFFICHER TOUTES LES OFFRES
    public List<Offre> afficher() throws SQLException {
        List<Offre> liste = new ArrayList<>();
        String sql = "SELECT * FROM offre";
        try (Statement st = getConn().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Offre o = new Offre();
                o.setId(rs.getInt("id"));
                o.setTitre(rs.getString("titre"));
                o.setDescription(rs.getString("description"));
                o.setLocalisation(rs.getString("localisation"));
                o.setTypeContrat(rs.getString("type_contrat"));
                o.setModeTravail(rs.getString("mode_travail"));
                o.setStatut(rs.getString("statut"));
                o.setActive(rs.getBoolean("is_active"));
                liste.add(o);
            }
        }
        return liste;
    }

    // 3. MODIFIER UNE OFFRE (Celle-ci est cruciale pour ton nouveau bouton)
    public void modifier(Offre o) throws SQLException {
        String sql = "UPDATE offre SET titre=?, description=?, localisation=?, statut=? WHERE id=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, o.getTitre());
            ps.setString(2, o.getDescription());
            ps.setString(3, o.getLocalisation());
            ps.setString(4, o.getStatut());
            ps.setInt(5, o.getId());

            int rowsUpdated = ps.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("Mise à jour réussie pour l'ID: " + o.getId());
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

    // 5. CHERCHER PAR ID (Optionnel, utile pour les détails)
    public Offre chercherParId(int id) throws SQLException {
        String sql = "SELECT * FROM offre WHERE id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Offre o = new Offre();
                    o.setId(rs.getInt("id"));
                    o.setTitre(rs.getString("titre"));
                    o.setDescription(rs.getString("description"));
                    o.setLocalisation(rs.getString("localisation"));
                    o.setStatut(rs.getString("statut"));
                    return o;
                }
            }
        }
        return null;
    }
}