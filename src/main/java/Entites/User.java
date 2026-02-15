package Entites;

public class User {
    private int id;
    private String username;
    private String password;
    private String role;

    // This is the constructor the compiler is looking for!
    public User(int id, String username, String password, String role) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
    }

    // Default constructor (often needed for frameworks)
    public User() {}

    // Getters
    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getRole() { return role; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
    public void setRole(String role) { this.role = role; }
}