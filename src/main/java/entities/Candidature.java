package entities;

public class Candidature {
    private int id;
    private String nomCandidat;
    private String prenomCandidat;
    private String email;
    private String statut;

    public Candidature() {}

    public Candidature(int id, String nomCandidat, String prenomCandidat, String email, String statut) {
        this.id = id;
        this.nomCandidat = nomCandidat;
        this.prenomCandidat = prenomCandidat;
        this.email = email;
        this.statut = statut;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNomCandidat() { return nomCandidat; }
    public void setNomCandidat(String nomCandidat) { this.nomCandidat = nomCandidat; }

    public String getPrenomCandidat() { return prenomCandidat; }
    public void setPrenomCandidat(String prenomCandidat) { this.prenomCandidat = prenomCandidat; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getNomComplet() {
        return (nomCandidat == null ? "" : nomCandidat) + " " + (prenomCandidat == null ? "" : prenomCandidat);
    }

    @Override
    public String toString() {
        return "Candidature{" +
                "id=" + id +
                ", nomCandidat='" + nomCandidat + '\'' +
                ", prenomCandidat='" + prenomCandidat + '\'' +
                ", email='" + email + '\'' +
                ", statut='" + statut + '\'' +
                '}';
    }
}
