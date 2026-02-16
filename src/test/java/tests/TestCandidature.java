package tests;

import entities.Candidature;
import services.CandidatureService;

public class TestCandidature {
    public static void main(String[] args) {
        CandidatureService cs = new CandidatureService();

        try {
            // CREATE
            Candidature c = new Candidature();
            c.setNomCandidat("Test");
            c.setPrenomCandidat("CRUD");
            c.setEmail("crud_" + System.currentTimeMillis() + "@gmail.com");
            c.setStatut("EN_ATTENTE");

            int id = cs.addAndReturnId(c);
            System.out.println("Ajout OK, id=" + id);

            // READ
            System.out.println("existsById(" + id + ") = " + cs.existsById(id));
            System.out.println("---- TOP 5 ----");
            cs.findAll().stream().limit(5).forEach(System.out::println);

            // UPDATE
            c.setId(id);
            c.setStatut("ACCEPTE");
            cs.update(c);
            System.out.println("Update OK (statut=ACCEPTE)");

            // READ after update
            System.out.println("---- Après update ----");
            cs.findById(id).ifPresent(System.out::println);

            // DELETE
            cs.delete(id);
            System.out.println("Delete OK");

            // VERIFY DELETE
            System.out.println("existsById(" + id + ") = " + cs.existsById(id));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
