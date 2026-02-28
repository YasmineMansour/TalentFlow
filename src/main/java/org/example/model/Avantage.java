package org.example.model;

public class Avantage {
    private int id;
    private String nom;
    private String description;
    private String type;
    private int offreId;

    public Avantage() {}

    public Avantage(String nom, String description, int offreId) {
        this.nom = nom;
        this.description = description;
        this.offreId = offreId;
    }

    public Avantage(String nom, String description, String type, int offreId) {
        this.nom = nom;
        this.description = description;
        this.type = type;
        this.offreId = offreId;
    }

    public Avantage(int id, String nom, String description, String type, int offreId) {
        this.id = id;
        this.nom = nom;
        this.description = description;
        this.type = type;
        this.offreId = offreId;
    }

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
        return "Avantage{id=" + id + ", nom='" + nom + "', type='" + type + "', offreId=" + offreId + "}";
    }
}
