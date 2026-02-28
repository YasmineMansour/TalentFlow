package org.example.model;

public class Offre {
    private int id;
    private String titre;
    private String description;
    private String localisation;
    private String typeContrat;
    private String modeTravail;
    private double salaireMin;
    private double salaireMax;
    private boolean active;
    private String statut;

    // Champ transient pour le classement (Or/Argent/Bronze)
    private transient String classement;

    // Champ transient pour l'indice de coherence salariale
    private transient String coherence;

    public Offre() {}

    public Offre(String titre, String description, String localisation, String statut) {
        this.titre = titre;
        this.description = description;
        this.localisation = localisation;
        this.statut = statut;
    }

    public Offre(String titre, String description, String localisation, String typeContrat,
                 String modeTravail, double salaireMin, double salaireMax, boolean active, String statut) {
        this.titre = titre;
        this.description = description;
        this.localisation = localisation;
        this.typeContrat = typeContrat;
        this.modeTravail = modeTravail;
        this.salaireMin = salaireMin;
        this.salaireMax = salaireMax;
        this.active = active;
        this.statut = statut;
    }

    public Offre(int id, String titre, String description, String localisation, String typeContrat,
                 String modeTravail, double salaireMin, double salaireMax, boolean active, String statut) {
        this(titre, description, localisation, typeContrat, modeTravail, salaireMin, salaireMax, active, statut);
        this.id = id;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocalisation() { return localisation; }
    public void setLocalisation(String localisation) { this.localisation = localisation; }

    public String getTypeContrat() { return typeContrat; }
    public void setTypeContrat(String typeContrat) { this.typeContrat = typeContrat; }

    public String getModeTravail() { return modeTravail; }
    public void setModeTravail(String modeTravail) { this.modeTravail = modeTravail; }

    public double getSalaireMin() { return salaireMin; }
    public void setSalaireMin(double salaireMin) { this.salaireMin = salaireMin; }

    public double getSalaireMax() { return salaireMax; }
    public void setSalaireMax(double salaireMax) { this.salaireMax = salaireMax; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getClassement() { return classement; }
    public void setClassement(String classement) { this.classement = classement; }

    public String getCoherence() { return coherence; }
    public void setCoherence(String coherence) { this.coherence = coherence; }

    public String getSalaireRange() {
        if (salaireMin == 0 && salaireMax == 0) return "Non precise";
        return String.format("%.0f - %.0f DT", salaireMin, salaireMax);
    }

    @Override
    public String toString() {
        return "Offre{id=" + id + ", titre='" + titre + "', localisation='" + localisation + "'}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Offre offre = (Offre) o;
        return id == offre.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
