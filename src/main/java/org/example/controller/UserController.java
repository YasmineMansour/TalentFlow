package org.example.controller;

import org.example.dao.UserDAO;
import org.example.model.User;
import java.util.List;

public class UserController {
    // Instance du DAO pour interagir avec la base de données
    private UserDAO userDAO = new UserDAO();

    // CREATE : Ajouter un utilisateur (0 pour l'ID car auto-incrément)
    public void addUser(String nom, String prenom, String email, String password, String role, String telephone) {
        User user = new User(0, nom, prenom, email, password, role, telephone);
        userDAO.create(user);
    }

    // READ : Récupérer et afficher la liste des utilisateurs
    public void displayUsers() {
        List<User> users = userDAO.readAll();
        if (users.isEmpty()) {
            System.out.println("La base de données est vide.");
        } else {
            users.forEach(System.out::println);
        }
    }

    // READ (Retourne la liste) : Utile pour remplir un TableView en JavaFX
    public List<User> getAllUsers() {
        return userDAO.readAll();
    }

    // UPDATE : Modifier un utilisateur existant
    public void updateUser(int id, String nom, String prenom, String email, String password, String role, String telephone) {
        User user = new User(id, nom, prenom, email, password, role, telephone);
        userDAO.update(user);
    }

    // DELETE : Supprimer un utilisateur par son ID
    public void removeUser(int id) {
        userDAO.delete(id);
    }
}