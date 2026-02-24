package org.example.model;

import java.time.LocalDateTime;

public class User {
    private int id;
    private String nom, prenom, email, password, role, telephone;
    private boolean active;
    private LocalDateTime createdAt;

    // Constructeur principal à 7 arguments
    public User(int id, String nom, String prenom, String email, String password, String role, String telephone) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.password = password;
        this.role = role;
        this.telephone = telephone;
        this.active = true;
        this.createdAt = LocalDateTime.now();
    }

    // Constructeur complet (utilisé lors de la lecture depuis la BDD)
    public User(int id, String nom, String prenom, String email, String password,
                String role, String telephone, boolean active, LocalDateTime createdAt) {
        this(id, nom, prenom, email, password, role, telephone);
        this.active = active;
        this.createdAt = createdAt;
    }

    // GETTERS
    public int getId() { return id; }
    public String getNom() { return nom; }
    public String getPrenom() { return prenom; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getRole() { return role; }
    public String getTelephone() { return telephone; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    /** Retourne le nom complet (Prénom Nom) */
    public String getFullName() { return prenom + " " + nom; }

    // SETTERS
    public void setId(int id) { this.id = id; }
    public void setNom(String nom) { this.nom = nom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    public void setEmail(String email) { this.email = email; }
    public void setPassword(String password) { this.password = password; }
    public void setRole(String role) { this.role = role; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
    public void setActive(boolean active) { this.active = active; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return String.format("[%d] %s %s | %s | %s | Tel: %s | Actif: %s",
                id, prenom, nom, email, role, telephone, active ? "Oui" : "Non");
    }
}