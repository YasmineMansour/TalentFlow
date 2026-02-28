package org.example.model;

import java.time.LocalDateTime;

public class DecisionFinale {

    private int id;
    private int entretienId;
    private String decision;   // ACCEPTE, REFUSE, EN_ATTENTE
    private String motif;
    private LocalDateTime dateDecision;
    private Double score;

    public DecisionFinale() {}

    public DecisionFinale(int id, int entretienId, String decision,
                          String motif, LocalDateTime dateDecision, Double score) {
        this.id = id;
        this.entretienId = entretienId;
        this.decision = decision;
        this.motif = motif;
        this.dateDecision = dateDecision;
        this.score = score;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getEntretienId() { return entretienId; }
    public void setEntretienId(int entretienId) { this.entretienId = entretienId; }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }

    public LocalDateTime getDateDecision() { return dateDecision; }
    public void setDateDecision(LocalDateTime dateDecision) { this.dateDecision = dateDecision; }

    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }

    @Override
    public String toString() {
        return "DecisionFinale{id=" + id + ", entretienId=" + entretienId +
                ", decision='" + decision + "', score=" + score + "}";
    }
}
