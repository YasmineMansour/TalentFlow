package entites;

public class Utilisateur {
    private int id;
    private String nom;
    private String prenom;
    private String email;

    // Constructeur vide
    public Utilisateur() {}

    // Constructeur pour la ComboBox (ID, Nom, Prénom)
    public Utilisateur(int id, String nom, String prenom) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
    }

    // --- GETTERS & SETTERS ---
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    // Très important : cette méthode définit ce qui s'affiche dans la console ou certains logs
    @Override
    public String toString() {
        return nom + " " + prenom;
    }
}