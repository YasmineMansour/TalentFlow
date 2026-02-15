package Entites;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PersonneTest {

    @Test
    void getId() {
        Personne p = new Personne("Ali","Sami",8);
        assertEquals(18,p.getAge(),"Age non conforme");
    }
}