package entities;

import java.time.LocalDateTime;

public class Candidature {
    private int id;
    private String nomCandidat;
    private String email;
    private String cvPath; // Chemin vers le fichier PDF
    private String statut; // PENDING, ACCEPTED, REJECTED
    private LocalDateTime datePostulation;
    private Offre offre; // La liaison avec la table Offre

    public Candidature() {
        this.datePostulation = LocalDateTime.now();
        this.statut = "PENDING";
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNomCandidat() { return nomCandidat; }
    public void setNomCandidat(String nomCandidat) { this.nomCandidat = nomCandidat; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCvPath() { return cvPath; }
    public void setCvPath(String cvPath) { this.cvPath = cvPath; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public Offre getOffre() { return offre; }
    public void setOffre(Offre offre) { this.offre = offre; }
}