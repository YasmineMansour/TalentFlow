package entites;

public class Candidature {
    private int id;
    private int user_id;
    private int offre_id;
    private String cv_url;
    private String motivation;
    private String statut;
    private String date_postulation;

    public Candidature() {}

    // Constructeur pour l'ajout (5 paramètres de base)
    public Candidature(int user_id, int offre_id, String cv_url, String motivation, String statut) {
        this.user_id = user_id;
        this.offre_id = offre_id;
        this.cv_url = cv_url;
        this.motivation = motivation;
        this.statut = statut;
    }

    // Constructeur pour l'ajout avec date
    public Candidature(int user_id, int offre_id, String cv_url, String motivation, String statut, String date_postulation) {
        this.user_id = user_id;
        this.offre_id = offre_id;
        this.cv_url = cv_url;
        this.motivation = motivation;
        this.statut = statut;
        this.date_postulation = date_postulation;
    }

    // Constructeur complet (pour TableView et Update)
    public Candidature(int id, int user_id, int offre_id, String cv_url, String motivation, String statut, String date_postulation) {
        this.id = id;
        this.user_id = user_id;
        this.offre_id = offre_id;
        this.cv_url = cv_url;
        this.motivation = motivation;
        this.statut = statut;
        this.date_postulation = date_postulation;
    }

    // --- GETTERS & SETTERS ---
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUser_id() { return user_id; }
    public void setUser_id(int user_id) { this.user_id = user_id; }
    public int getOffre_id() { return offre_id; }
    public void setOffre_id(int offre_id) { this.offre_id = offre_id; }
    public String getCv_url() { return cv_url; }
    public void setCv_url(String cv_url) { this.cv_url = cv_url; }
    public String getMotivation() { return motivation; }
    public void setMotivation(String motivation) { this.motivation = motivation; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public String getDate_postulation() { return date_postulation; }
    public void setDate_postulation(String date_postulation) { this.date_postulation = date_postulation; }
}