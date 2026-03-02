package org.example.model;

import java.sql.Timestamp;

public class Candidature {

    private int id;
    private int userId;
    private int offreId;
    private String cvUrl;
    private String motivation;
    private Timestamp datePostulation;
    private String statut;
    private String langue;
    private String email;

    // Champs transients pour l'affichage (jointures)
    private transient String nomCandidat;
    private transient String titreOffre;

    public Candidature() {}

    public Candidature(int userId, int offreId, String cvUrl, String motivation, String statut, String email) {
        this.userId = userId;
        this.offreId = offreId;
        this.cvUrl = cvUrl;
        this.motivation = motivation;
        this.statut = statut;
        this.email = email;
        this.langue = "INCONNU";
    }

    // --- GETTERS & SETTERS ---

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getOffreId() { return offreId; }
    public void setOffreId(int offreId) { this.offreId = offreId; }

    public String getCvUrl() { return cvUrl; }
    public void setCvUrl(String cvUrl) { this.cvUrl = cvUrl; }

    public String getMotivation() { return motivation; }
    public void setMotivation(String motivation) { this.motivation = motivation; }

    public Timestamp getDatePostulation() { return datePostulation; }
    public void setDatePostulation(Timestamp datePostulation) { this.datePostulation = datePostulation; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getLangue() { return langue; }
    public void setLangue(String langue) { this.langue = langue; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getNomCandidat() { return nomCandidat; }
    public void setNomCandidat(String nomCandidat) { this.nomCandidat = nomCandidat; }

    public String getTitreOffre() { return titreOffre; }
    public void setTitreOffre(String titreOffre) { this.titreOffre = titreOffre; }

    @Override
    public String toString() {
        return "Candidature #" + id + " — " + statut + " (Offre #" + offreId + ")";
    }
}
