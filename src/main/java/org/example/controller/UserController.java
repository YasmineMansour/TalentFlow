package org.example.controller;

import org.example.model.User;
import org.example.dao.UserDAO;

import java.util.List;

public class UserController {

    private UserDAO userDAO = new UserDAO();

    /** Ajouter un utilisateur (le mot de passe sera haché par le DAO) */
    public boolean addUser(String nom, String prenom, String email, String password, String role, String telephone) {
        User user = new User(0, nom, prenom, email, password, role, telephone);
        return userDAO.create(user);
    }

    /** Afficher tous les utilisateurs dans la console */
    public void displayUsers() {
        List<User> users = userDAO.readAll();
        if (users.isEmpty()) {
            System.out.println("La base de données est vide.");
        } else {
            users.forEach(System.out::println);
        }
    }

    /** Récupérer tous les utilisateurs */
    public List<User> getAllUsers() {
        return userDAO.readAll();
    }

    /** Rechercher des utilisateurs par mot-clé */
    public List<User> searchUsers(String keyword) {
        return userDAO.search(keyword);
    }

    /** Trouver un utilisateur par son ID */
    public User getUserById(int id) {
        return userDAO.findById(id);
    }

    /** Trouver un utilisateur par son email */
    public User getUserByEmail(String email) {
        return userDAO.findByEmail(email);
    }

    /** Vérifier si un email existe déjà */
    public boolean isEmailTaken(String email) {
        return userDAO.emailExists(email);
    }

    /** Mettre à jour un utilisateur */
    public boolean updateUser(int id, String nom, String prenom, String email, String password, String role, String telephone) {
        User user = new User(id, nom, prenom, email, password, role, telephone);
        return userDAO.update(user);
    }

    /** Supprimer un utilisateur par ID */
    public boolean deleteUser(int id) {
        return userDAO.delete(id);
    }

    /** Nombre total d'utilisateurs */
    public int getUserCount() {
        return userDAO.count();
    }
}