package services;

import entities.Offre;
import org.junit.jupiter.api.*;
import java.sql.SQLException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OffreServiceTest {
    private static OffreService service;
    private static int insertedId;

    @BeforeAll static void init() { service = new OffreService(); }

    @Test @Order(1)
    void testAjouter() throws SQLException {
        Offre o = new Offre("Test", "Desc", "Tunis", "CDI", "HYBRID", 1000, 1500, true, "PUBLISHED");
        service.ajouter(o);
        List<Offre> list = service.afficher();
        insertedId = list.get(list.size()-1).getId();
        assertTrue(insertedId > 0);
    }

    @Test @Order(2)
    void testChercher() throws SQLException {
        assertNotNull(service.chercherParId(insertedId));
    }

    @Test @Order(3)
    void testSupprimer() throws SQLException {
        service.supprimer(insertedId);
        assertNull(service.chercherParId(insertedId));
    }
}