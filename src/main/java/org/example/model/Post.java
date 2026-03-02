package org.example.model;

import java.time.LocalDateTime;

/**
 * Entité représentant un post du forum.
 * Table : posts (talent_flow_db)
 */
public class Post {
    private int id;
    private String title;
    private String content;
    private int authorId;
    private String authorName;
    private String authorRole;
    private int upvotes;
    private LocalDateTime createdAt;
    private String imagePath;

    public Post() {}

    public Post(int id, String title, String content, String authorName,
                String authorRole, int upvotes, String imagePath) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.authorName = authorName;
        this.authorRole = authorRole;
        this.upvotes = upvotes;
        this.imagePath = imagePath;
    }

    // Getters
    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public int getAuthorId() { return authorId; }
    public String getAuthorName() { return authorName; }
    public String getAuthorRole() { return authorRole; }
    public int getUpvotes() { return upvotes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getImagePath() { return imagePath; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
    public void setAuthorId(int authorId) { this.authorId = authorId; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public void setAuthorRole(String authorRole) { this.authorRole = authorRole; }
    public void setUpvotes(int upvotes) { this.upvotes = upvotes; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    @Override
    public String toString() {
        return "[" + authorRole + "] " + authorName + " : " + title + " (" + upvotes + " votes)";
    }
}
