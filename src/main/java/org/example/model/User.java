package org.example.model;

public class User {
    private int id;
    private String nom, prenom, email, password, role, telephone;

    // Constructeur à 7 arguments (celui que le RegisterController doit utiliser)
    public User(int id, String nom, String prenom, String email, String password, String role, String telephone) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.password = password;
        this.role = role;
        this.telephone = telephone;
    }

    // GETTERS
    public int getId() { return id; }
    public String getNom() { return nom; }
    public String getPrenom() { return prenom; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getRole() { return role; }
    public String getTelephone() { return telephone; }

    // SETTERS (Essentiels pour corriger les erreurs de compilation du CRUD)
    public void setNom(String nom) { this.nom = nom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    public void setEmail(String email) { this.email = email; }
    public void setPassword(String password) { this.password = password; }
    public void setRole(String role) { this.role = role; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
}