package org.example.tests;

import org.example.dao.UserDAO;
import org.example.model.User;
import java.util.List;

public class ConnectionTest {
    public static void main(String[] args) {
        System.out.println("=== DÉMARRAGE DU TEST DE CONNEXION TALENTFLOW ===");

        try {
            // 1. Initialisation du DAO
            // Le constructeur du DAO va tenter de récupérer la connexion via MyConnection
            UserDAO dao = new UserDAO();

            // 2. Création d'un utilisateur de test avec un email garanti unique
            String emailUnique = "test." + System.currentTimeMillis() + "@talentflow.com";

            User testUser = new User(
                    0,
                    "Alpha",
                    "Tester",
                    emailUnique,
                    "password123",
                    "CANDIDAT", // Utilisation du rôle par défaut
                    "22334455"
            );

            // 3. Test de l'ajout (CREATE)
            System.out.println("\n[1/2] Tentative d'insertion de l'utilisateur : " + emailUnique);
            dao.create(testUser);
            // Si aucune exception n'est levée ici, l'insertion a réussi

            // 4. Test de la lecture (READ)
            System.out.println("\n[2/2] Vérification des données présentes dans la table 'user' :");
            List<User> users = dao.readAll();

            if (users.isEmpty()) {
                System.out.println("⚠️ La table est vide ou la lecture a échoué.");
            } else {
                users.forEach(u -> System.out.println(" -> Trouvé : " + u.getPrenom() + " " + u.getNom() + " | Email: " + u.getEmail() + " | Rôle: " + u.getRole()));
            }

            System.out.println("\n✅ TEST TERMINÉ AVEC SUCCÈS !");

        } catch (Exception e) {
            System.err.println("\n❌ LE TEST A ÉCHOUÉ !");
            System.err.println("Cause possible : MySQL est éteint dans XAMPP ou nom de BDD incorrect.");
            e.printStackTrace();
        }
    }
}