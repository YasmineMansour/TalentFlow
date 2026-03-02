package org.example.model;

import java.time.LocalDateTime;

public class PieceJointe {
    private int id;
    private int candidatureId;
    private String titre;
    private String typeDoc;
    private String url;
    private LocalDateTime createdAt;

    // Constructeur complet (SELECT / UPDATE)
    public PieceJointe(int id, int candidatureId, String titre, String typeDoc, String url, LocalDateTime createdAt) {
        this.id = id;
        this.candidatureId = candidatureId;
        this.titre = titre;
        this.typeDoc = typeDoc;
        this.url = url;
        this.createdAt = createdAt;
    }

    // Constructeur court (INSERT)
    public PieceJointe(int candidatureId, String titre, String typeDoc, String url) {
        this.candidatureId = candidatureId;
        this.titre = titre;
        this.typeDoc = typeDoc;
        this.url = url;
    }

    // Getters
    public int getId() { return id; }
    public int getCandidatureId() { return candidatureId; }
    public String getTitre() { return titre; }
    public String getTypeDoc() { return typeDoc; }
    public String getUrl() { return url; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setCandidatureId(int candidatureId) { this.candidatureId = candidatureId; }
    public void setTitre(String titre) { this.titre = titre; }
    public void setTypeDoc(String typeDoc) { this.typeDoc = typeDoc; }
    public void setUrl(String url) { this.url = url; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "PieceJointe{id=" + id + ", titre='" + titre + "', type='" + typeDoc + "'}";
    }
}
