package org.example.controller;

import org.example.model.User;
import org.example.dao.UserDAO;
import java.util.List;

public class UserController {

    // IL FAUT ABSOLUMENT CETTE LIGNE POUR RÉPARER TES ERREURS ROUGES
    private UserDAO userDAO = new UserDAO();

    public void addUser(String nom, String prenom, String email, String password, String role, String telephone) {
        User user = new User(0, nom, prenom, email, password, role, telephone);
        userDAO.create(user);
    }

    public void displayUsers() {
        List<User> users = userDAO.readAll();
        if (users.isEmpty()) {
            System.out.println("La base de données est vide.");
        } else {
            users.forEach(System.out::println);
        }
    }

    public List<User> getAllUsers() {
        return userDAO.readAll();
    }

    public void updateUser(int id, String nom, String prenom, String email, String password, String role, String telephone) {
        User user = new User(id, nom, prenom, email, password, role, telephone);
        userDAO.update(user);
    }

    public void deleteUser(int id) {
        userDAO.delete(id);
    }
}