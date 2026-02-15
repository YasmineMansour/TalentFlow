package services;

import entites.Offre;
import utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OffreService {

    private Connection cnx = MyDatabase.getInstance().getCnx();

    public List<Offre> afficherTout() {
        List<Offre> liste = new ArrayList<>();
        // On récupère l'ID pour la liaison et le Titre pour l'affichage
        String req = "SELECT id, titre FROM offre";
        try {
            Statement st = cnx.createStatement();
            ResultSet rs = st.executeQuery(req);
            while (rs.next()) {
                liste.add(new Offre(
                        rs.getInt("id"),
                        rs.getString("titre")
                ));
            }
        } catch (SQLException ex) {
            System.out.println("Erreur OffreService : " + ex.getMessage());
        }
        return liste;
    }
}