package entities;

import java.time.LocalDateTime;

public class Entretien {
    private int id;
    private int candidatureId;
    private LocalDateTime dateHeure;
    private String type;
    private String lieu;
    private String lien;
    private String statut;
    private Integer noteTechnique;
    private Integer noteCommunication;
    private String commentaire;

    private String nomComplet;

    public Entretien() {}

    public Entretien(int id, int candidatureId, LocalDateTime dateHeure, String type, String lieu, String lien,
                     String statut, Integer noteTechnique, Integer noteCommunication, String commentaire) {
        this.id = id;
        this.candidatureId = candidatureId;
        this.dateHeure = dateHeure;
        this.type = type;
        this.lieu = lieu;
        this.lien = lien;
        this.statut = statut;
        this.noteTechnique = noteTechnique;
        this.noteCommunication = noteCommunication;
        this.commentaire = commentaire;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCandidatureId() { return candidatureId; }
    public void setCandidatureId(int candidatureId) { this.candidatureId = candidatureId; }

    public LocalDateTime getDateHeure() { return dateHeure; }
    public void setDateHeure(LocalDateTime dateHeure) { this.dateHeure = dateHeure; }

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

    public String getNomComplet() { return nomComplet; }
    public void setNomComplet(String nomComplet) { this.nomComplet = nomComplet; }

    @Override
    public String toString() {
        return "Entretien{" +
                "id=" + id +
                ", candidatureId=" + candidatureId +
                ", nomComplet='" + nomComplet + '\'' +
                ", dateHeure=" + dateHeure +
                ", type='" + type + '\'' +
                ", lieu='" + lieu + '\'' +
                ", lien='" + lien + '\'' +
                ", statut='" + statut + '\'' +
                ", noteTechnique=" + noteTechnique +
                ", noteCommunication=" + noteCommunication +
                ", commentaire='" + commentaire + '\'' +
                '}';
    }
}
