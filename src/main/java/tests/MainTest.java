package tests;

import entites.Candidature;
import services.CandidatureService;
import java.util.List;

public class MainTest {
    public static void main(String[] args) {
        // 1. Initialisation du service
        CandidatureService cs = new CandidatureService();

        System.out.println("--- DÉBUT DU TEST CRUD ---");

        // 2. Test AJOUT (CREATE)
        // Note : On met des IDs fictifs (1 et 5), assure-toi qu'ils ne bloquent pas
        // si tu as des clés étrangères activées dans XAMPP.
        Candidature nouvelle = new Candidature(1, 1, "cv_test.pdf", "Motivation", "En attente");
        cs.ajouter(nouvelle);

        // 3. Test AFFICHAGE (READ)
        System.out.println("\n--- LISTE DES CANDIDATURES ---");
        List<Candidature> liste = cs.afficherTout();
        if (liste.isEmpty()) {
            System.out.println("La base est vide.");
        } else {
            for (Candidature c : liste) {
                System.out.println("ID: " + c.getId() +
                        " | User: " + c.getUser_id() +
                        " | Offre: " + c.getOffre_id() +
                        " | Statut: " + c.getStatut());
            }
        }

        System.out.println("\n--- FIN DU TEST ---");
    }

    public class AppLauncher {
        public static void main(String[] args) {
            MainFX.main(args);
        }
    }
}