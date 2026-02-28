package entities;

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

    // Champ transient pour le classement (Or/Argent/Bronze) — non persisté en base
    private transient String classement;

    // Champ transient pour l'indice de cohérence salariale — non persisté en base
    private transient String coherence;

    // 1. Constructeur par défaut (Indispensable pour "new Offre()")
    public Offre() {}

    // 2. Constructeur pour le Contrôleur (4 paramètres principaux)
    public Offre(String titre, String description, String localisation, String statut) {
        this.titre = titre;
        this.description = description;
        this.localisation = localisation;
        this.statut = statut;
    }

    // 3. Constructeur utilisé par le test JUnit (9 paramètres)
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

    // 4. Constructeur complet pour la récupération DB (10 paramètres)
    public Offre(int id, String titre, String description, String localisation, String typeContrat,
                 String modeTravail, double salaireMin, double salaireMax, boolean active, String statut) {
        this(titre, description, localisation, typeContrat, modeTravail, salaireMin, salaireMax, active, statut);
        this.id = id;
    }

    // --- GETTERS & SETTERS (Tous inclus pour éviter le rouge dans le contrôleur) ---

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

    // --- Salary range display helper ---
    public String getSalaireRange() {
        if (salaireMin == 0 && salaireMax == 0) return "Non précisé";
        return String.format("%.0f - %.0f DT", salaireMin, salaireMax);
    }

    @Override
    public String toString() {
        return "Offre{" +
                "id=" + id +
                ", titre='" + titre + '\'' +
                ", localisation='" + localisation + '\'' +
                ", typeContrat='" + typeContrat + '\'' +
                ", modeTravail='" + modeTravail + '\'' +
                ", statut='" + statut + '\'' +
                '}';
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