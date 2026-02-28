package entities;

public class Avantage {
    private int id;
    private String nom;
    private String description;
    private String type; // ex: Financier, Bien-être, Matériel
    private int offreId; // La clé étrangère

    // 1. Constructeur par défaut
    public Avantage() {}

    // 2. Constructeur simplifié (pour les formulaires rapides)
    public Avantage(String nom, String description, int offreId) {
        this.nom = nom;
        this.description = description;
        this.offreId = offreId;
    }

    // 3. Constructeur complet sans ID (pour l'insertion en DB)
    public Avantage(String nom, String description, String type, int offreId) {
        this.nom = nom;
        this.description = description;
        this.type = type;
        this.offreId = offreId;
    }

    // 4. Constructeur complet avec ID (pour la récupération DB)
    public Avantage(int id, String nom, String description, String type, int offreId) {
        this.id = id;
        this.nom = nom;
        this.description = description;
        this.type = type;
        this.offreId = offreId;
    }

    // --- GETTERS & SETTERS ---

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getOffreId() { return offreId; }
    public void setOffreId(int offreId) { this.offreId = offreId; }

    @Override
    public String toString() {
        return "Avantage{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", type='" + type + '\'' +
                ", offreId=" + offreId +
                '}';
    }
}