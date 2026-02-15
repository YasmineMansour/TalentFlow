package services;

import entites.Utilisateur;
import utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UtilisateurService {

    private Connection cnx = MyDatabase.getInstance().getCnx();

    public List<Utilisateur> afficherTout() {
        List<Utilisateur> liste = new ArrayList<>();
        String req = "SELECT id, nom, prenom FROM utilisateur"; // Assure-toi que les noms de colonnes sont exacts
        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(req);
            while (rs.next()) {
                liste.add(new Utilisateur(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getString("prenom")
                ));
            }
        } catch (SQLException ex) {
            System.out.println("Erreur UtilisateurService : " + ex.getMessage());
        }
        return liste;
    }
}