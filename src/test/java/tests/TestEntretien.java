package tests;

import entities.Entretien;
import services.EntretienService;

import java.time.LocalDateTime;

public class TestEntretien {
    public static void main(String[] args) {
        EntretienService es = new EntretienService();

        try {
            // IMPORTANT : mets un candidature_id existant dans ta BD (ex: 6,7,8...)
            int candidatureId = 6;

            // CREATE (EN_LIGNE)
            Entretien e = new Entretien();
            e.setCandidatureId(candidatureId);
            e.setDateHeure(LocalDateTime.now().plusDays(1));
            e.setType("EN_LIGNE");
            e.setLien("https://meet.google.com/test-" + System.currentTimeMillis());
            e.setLieu(null);
            e.setStatut("PLANIFIE");
            e.setNoteTechnique(15);
            e.setNoteCommunication(16);
            e.setCommentaire("Test entretien en ligne");

            int id = es.addAndReturnId(e);
            System.out.println("Ajout OK, id=" + id);
            System.out.println("existsById(" + id + ") = " + es.existsById(id));

            // READ
            System.out.println("---- Après ajout ----");
            es.findById(id).ifPresent(System.out::println);

            // UPDATE (passer à PRESENTIEL)
            e.setId(id);
            e.setType("PRESENTIEL");
            e.setLieu("EPI Digital School - Salle 3");
            e.setLien(null);
            e.setStatut("REALISE");
            e.setNoteTechnique(18);
            e.setNoteCommunication(17);
            e.setCommentaire("Entretien réalisé en présentiel");
            es.update(e);
            System.out.println("Update OK");

            System.out.println("---- Après update ----");
            es.findById(id).ifPresent(System.out::println);

            // DELETE
            es.delete(id);
            System.out.println("Delete OK");
            System.out.println("existsById(" + id + ") = " + es.existsById(id));

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
