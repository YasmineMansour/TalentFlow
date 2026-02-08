package org.example.model;

public class User {
    private int id;
    private String nom;
    private String prenom;
    private String email;
    private String password;
    private String role;
    private String telephone;

    // 1. Constructeur par défaut (nécessaire pour certains frameworks)
    public User() {
    }

    // 2. Constructeur complet (utilisé pour lire les données depuis la DB)
    public User(int id, String nom, String prenom, String email, String password, String role, String telephone) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.password = password;
        this.role = role;
        this.telephone = telephone;
    }

    // 3. Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    // 4. toString pour afficher les infos proprement dans la console
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", prenom='" + prenom + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                ", tel='" + telephone + '\'' +
                '}';
    }
}