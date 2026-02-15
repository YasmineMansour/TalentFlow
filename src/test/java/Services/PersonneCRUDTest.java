package Services;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PersonneCRUDTest {

    static PersonneCRUD pc;
    @BeforeAll
    static void setup(){
        pc = new PersonneCRUD();
    }

    @Test
    void ajouter() {
        Personne p = new Personne("Ben salah","Amin",27);
        try {
            pc.ajouter(p);
            List<Personne> data = pc.afficher();
            assertFalse(data.isEmpty(),"Liste vide");
            assertTrue(data.stream().anyMatch(r->r.getPrenom().equals("Nedia"))
                    ,"Personne inexistant");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Test
    void afficher() {
    }
}