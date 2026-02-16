package tests;

import entities.Entretien;
import services.EntretienService;

import java.time.LocalDateTime;

public class TestEntretienValidation {

    @FunctionalInterface
    interface ThrowingRunnable {
        void run() throws Exception;
    }

    public static void main(String[] args) {
        EntretienService es = new EntretienService();

        test("Date passée", () -> {
            Entretien e = base(es);
            e.setDateHeure(LocalDateTime.now().minusDays(1));
            es.add(e);
        });

        test("EN_LIGNE sans lien", () -> {
            Entretien e = base(es);
            e.setType("EN_LIGNE");
            e.setLien("   ");
            e.setLieu(null);
            es.add(e);
        });

        test("PRESENTIEL sans lieu", () -> {
            Entretien e = base(es);
            e.setType("PRESENTIEL");
            e.setLieu("   ");
            e.setLien(null);
            es.add(e);
        });

        test("Note technique > 20", () -> {
            Entretien e = base(es);
            e.setNoteTechnique(25);
            es.add(e);
        });

        test("CandidatureId invalide (<=0)", () -> {
            Entretien e = base(es);
            e.setCandidatureId(-1);
            es.add(e);
        });

        test("CandidatureId inexistant (999999)", () -> {
            Entretien e = base(es);
            e.setCandidatureId(999999);
            es.add(e);
        });
    }

    private static Entretien base(EntretienService es) throws Exception {
        String first = es.findAllCandidaturesLabels().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Aucune candidature en BD."));

        int candidatureId = Integer.parseInt(first.split(" - ")[0].trim());

        Entretien e = new Entretien();
        e.setCandidatureId(candidatureId);
        e.setDateHeure(LocalDateTime.now().plusDays(1));
        e.setType("EN_LIGNE");
        e.setLien("https://meet.google.com/test");
        e.setStatut("PLANIFIE");
        e.setNoteTechnique(10);
        e.setNoteCommunication(10);
        e.setCommentaire("test");
        return e;
    }

    private static void test(String label, ThrowingRunnable action) {
        try {
            action.run();
            System.out.println("FAIL " + label + " : aurait dû être bloqué");
        } catch (Exception ex) {
            System.out.println("OK " + label + " : " + ex.getMessage());
        }
    }
}
