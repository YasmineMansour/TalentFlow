package org.example.model;

import java.time.LocalDateTime;

public class Entretien {

    private int id;
    private int candidatureId;
    private LocalDateTime dateEntretien;
    private String type;
    private String lieu;
    private String lien;
    private String statut;
    private Integer noteTechnique;
    private Integer noteCommunication;
    private String commentaire;
    private String emailCandidat; // transient, loaded via JOIN with candidature

    public Entretien() {}

    public Entretien(int id, int candidatureId, LocalDateTime dateEntretien,
                     String type, String lieu, String lien,
                     String statut, Integer noteTechnique,
                     Integer noteCommunication, String commentaire,
                     String emailCandidat) {
        this.id = id;
        this.candidatureId = candidatureId;
        this.dateEntretien = dateEntretien;
        this.type = type;
        this.lieu = lieu;
        this.lien = lien;
        this.statut = statut;
        this.noteTechnique = noteTechnique;
        this.noteCommunication = noteCommunication;
        this.commentaire = commentaire;
        this.emailCandidat = emailCandidat;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCandidatureId() { return candidatureId; }
    public void setCandidatureId(int candidatureId) { this.candidatureId = candidatureId; }

    public LocalDateTime getDateEntretien() { return dateEntretien; }
    public void setDateEntretien(LocalDateTime dateEntretien) { this.dateEntretien = dateEntretien; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getLieu() { return lieu; }
    public void setLieu(String lieu) { this.lieu = lieu; }

    public String getLien() { return lien; }
    public void setLien(String lien) { this.lien = lien; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public Integer getNoteTechnique() { return noteTechnique; }
    public void setNoteTechnique(Integer noteTechnique) { this.noteTechnique = noteTechnique; }

    public Integer getNoteCommunication() { return noteCommunication; }
    public void setNoteCommunication(Integer noteCommunication) { this.noteCommunication = noteCommunication; }

    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }

    public String getEmailCandidat() { return emailCandidat; }
    public void setEmailCandidat(String emailCandidat) { this.emailCandidat = emailCandidat; }

    // Scoring automatique (70% tech + 30% com)
    public static final double POIDS_TECHNIQUE = 0.7;
    public static final double POIDS_COMMUNICATION = 0.3;

    public Double getScoreFinal() {
        if (noteTechnique == null || noteCommunication == null) return null;
        return Math.round((noteTechnique * POIDS_TECHNIQUE
                + noteCommunication * POIDS_COMMUNICATION) * 100.0) / 100.0;
    }

    public String getNiveau() {
        Double score = getScoreFinal();
        if (score == null) return "-";
        if (score >= 16) return "EXCELLENT";
        if (score >= 12) return "BON";
        if (score >= 10) return "MOYEN";
        return "INSUFFISANT";
    }

    public String getStatutIntelligent() {
        if ("ANNULE".equals(statut)) return "ANNULE";
        if ("REALISE".equals(statut)) return "REALISE";
        if (dateEntretien != null && dateEntretien.isBefore(LocalDateTime.now())) {
            return "REALISE";
        }
        return "PLANIFIE";
    }

    @Override
    public String toString() {
        return "Entretien{id=" + id + ", type='" + type + "', statut='" + statut +
                "', emailCandidat='" + emailCandidat + "'}";
    }
}
