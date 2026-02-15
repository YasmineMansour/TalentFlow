package Entites;

import java.time.LocalDateTime;

public class Post {
    private int id;
    private String title;
    private String content;
    private String authorName;
    private String authorRole; // "ADMIN", "CANDIDATE", "JOB_PROVIDER"
    private int upvotes;
    private LocalDateTime createdAt;

    // Default Constructor
    public Post() {}

    // Constructor for creating a new post
    public Post(String title, String content, String authorName, String authorRole) {
        this.title = title;
        this.content = content;
        this.authorName = authorName;
        this.authorRole = authorRole;
        this.upvotes = 0;
        this.createdAt = LocalDateTime.now();
    }

    // Full Constructor (for Database retrieval)
    public Post(int id, String title, String content, String authorName, String authorRole, int upvotes) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.authorName = authorName;
        this.authorRole = authorRole;
        this.upvotes = upvotes;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getAuthorRole() { return authorRole; }
    public void setAuthorRole(String authorRole) { this.authorRole = authorRole; }

    public int getUpvotes() { return upvotes; }
    public void setUpvotes(int upvotes) { this.upvotes = upvotes; }

    @Override
    public String toString() {
        return "[" + authorRole + "] " + authorName + " : " + title + " (" + upvotes + " votes)";
    }
}