package org.example;

import org.example.controller.UserController;

public class Main {
    public static void main(String[] args) {
        UserController controller = new UserController();

        System.out.println("=== TEST DU SYSTÈME TALENTFLOW ===");

        // 1. TEST : CREATE (Ajout d'un utilisateur)
        // Utilisation d'un email dynamique pour éviter l'erreur "Duplicate entry"
        String uniqueEmail = "candidat" + System.currentTimeMillis() + "@test.tn";
        System.out.println("\n--- 1. Tentative d'ajout ---");
        controller.addUser("Trabelsi", "Sami", uniqueEmail, "password123", "candidat", "216554433");

        // 2. TEST : READ (Affichage initial)
        System.out.println("\n--- 2. Liste des utilisateurs en base ---");
        controller.displayUsers();

        // 3. TEST : UPDATE (Modification)
        // Note : On suppose ici que l'ID 1 existe.
        // Si tu as vidé ta base, vérifie l'ID dans l'affichage précédent.
        System.out.println("\n--- 3. Tentative de modification de l'utilisateur ID 1 ---");
        controller.updateUser(1, "Ben Salah", "Ahmed", "ahmed.updated@test.tn", "newpass", "admin", "55123456");

        // 4. TEST : DELETE (Suppression)
        // ATTENTION : Cette ligne supprimera l'utilisateur avec l'ID spécifié.
        // Décommente la ligne suivante pour tester la suppression :
        // controller.removeUser(2);

        // 5. AFFICHAGE FINAL
        System.out.println("\n--- 4. État final de la base de données ---");
        controller.displayUsers();

        System.out.println("\n=== FIN DES TESTS CRUD ===");
    }
}