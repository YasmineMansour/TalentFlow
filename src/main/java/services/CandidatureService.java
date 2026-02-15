package services;

import entites.Candidature;
import utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CandidatureService {
    private Connection cnx;

    public CandidatureService() {
        cnx = MyDatabase.getInstance().getCnx();
    }

    public void ajouter(Candidature c) {
        String query = "INSERT INTO candidature (user_id, offre_id, cv_url, motivation, statut) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setInt(1, c.getUser_id());
            pst.setInt(2, c.getOffre_id());
            pst.setString(3, c.getCv_url());
            pst.setString(4, c.getMotivation());
            pst.setString(5, c.getStatut());
            pst.executeUpdate();
            System.out.println("✅ Succès : Candidature enregistrée !");
        } catch (SQLException e) {
            System.err.println("❌ Erreur d'ajout : " + e.getMessage());
        }
    }

    public List<Candidature> afficherTout() {
        List<Candidature> liste = new ArrayList<>();
        String query = "SELECT * FROM candidature";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                Candidature c = new Candidature();
                c.setId(rs.getInt("id"));
                c.setUser_id(rs.getInt("user_id"));
                c.setOffre_id(rs.getInt("offre_id"));
                c.setCv_url(rs.getString("cv_url"));
                c.setMotivation(rs.getString("motivation"));
                if (rs.getDate("date_postulation") != null) {
                    c.setDate_postulation(rs.getDate("date_postulation").toString());
                }                c.setStatut(rs.getString("statut"));
                liste.add(c);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur de lecture : " + e.getMessage());
        }
        return liste;
    }

    public void modifier(Candidature c) {
        String query = "UPDATE candidature SET user_id=?, offre_id=?, cv_url=?, motivation=?, statut=? WHERE id=?";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setInt(1, c.getUser_id());
            pst.setInt(2, c.getOffre_id());
            pst.setString(3, c.getCv_url());
            pst.setString(4, c.getMotivation());
            pst.setString(5, c.getStatut());
            pst.setInt(6, c.getId());
            pst.executeUpdate();
            System.out.println("✅ Succès : Candidature mise à jour !");
        } catch (SQLException e) {
            System.err.println("❌ Erreur de modification : " + e.getMessage());
        }
    }

    public void supprimer(int id) {
        String query = "DELETE FROM candidature WHERE id = ?";
        try (PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setInt(1, id);
            pst.executeUpdate();
            System.out.println("✅ Succès : Candidature supprimée !");
        } catch (SQLException e) {
            System.err.println("❌ Erreur de suppression : " + e.getMessage());
        }
    }
}