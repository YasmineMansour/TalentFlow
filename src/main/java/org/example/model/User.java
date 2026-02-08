package org.example.model;

public class User {
    private int id;
    private String nom;
    private String prenom;
    private String email;
    private String password;
    private String role;
    private String telephone;

    // CONSTRUCTEUR 1 : Avec ID (pour la lecture depuis la BDD)
    public User(int id, String nom, String prenom, String email, String password, String role, String telephone) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.password = password;
        this.role = role;
        this.telephone = telephone;
    }

    // CONSTRUCTEUR 2 : Sans ID (Indispensable pour l'ajout/insertion)
    // C'est ce constructeur qui manque dans ton erreur d15c43.png
    public User(String nom, String prenom, String email, String password, String role, String telephone) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.password = password;
        this.role = role;
        this.telephone = telephone;
    }

    // Getters
    public int getId() { return id; }
    public String getNom() { return nom; }
    public String getPrenom() { return prenom; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getRole() { return role; }
    public String getTelephone() { return telephone; }
}