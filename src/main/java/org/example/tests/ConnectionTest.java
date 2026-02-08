package org.example.tests;

import org.example.dao.UserDAO;
import org.example.model.User;

public class ConnectionTest {
    public static void main(String[] args) {
        // 1. Initialisation du DAO
        UserDAO dao = new UserDAO();

        // 2. Création d'un utilisateur de test avec les 7 paramètres requis
        // (id, nom, prenom, email, password, role, telephone)
        // Note: L'ID est mis à 0 car il sera auto-incrémenté par MySQL
        User testUser = new User(
                0,
                "TestNom",
                "TestPrenom",
                "test.unique" + System.currentTimeMillis() + "@email.com", // Email unique pour éviter l'erreur Duplicate
                "pass123",
                "USER",
                "00000000"
        );

        // 3. Test de l'ajout
        System.out.println("Tentative d'insertion via ConnectionTest...");
        dao.create(testUser);

        // 4. Test de la lecture
        System.out.println("\n--- Vérification des données en base ---");
        dao.readAll().forEach(System.out::println);
    }
}