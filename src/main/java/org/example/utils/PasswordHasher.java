package org.example.utils;

// Remplace l'import Spring par l'import jBCrypt
import org.mindrot.jbcrypt.BCrypt;

public class PasswordHasher {
    // Pour hacher le mot de passe avant de l'enregistrer en BDD
    public static String hash(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    // Pour vérifier si le mot de passe saisi correspond au hachage en BDD
    // Supporte $2y$ (PHP/Symfony) en plus de $2a$ et $2b$ (Java)
    public static boolean check(String password, String hashed) {
        if (hashed != null && hashed.startsWith("$2y$")) {
            hashed = "$2a$" + hashed.substring(4);
        }
        return BCrypt.checkpw(password, hashed);
    }
}