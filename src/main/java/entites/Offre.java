package entites;

public class Offre {
    private int id;
    private String titre;

    public Offre() {}

    public Offre(int id, String titre) {
        this.id = id;
        this.titre = titre;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    // Très important pour l'affichage par défaut
    @Override
    public String toString() {
        return titre;
    }
}