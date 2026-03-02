package org.example.model;

import java.time.LocalDateTime;

/**
 * Entité représentant un commentaire sur un post du forum.
 * Table : comments (talent_flow_db)
 */
public class Comment {
    private int id;
    private int postId;
    private int authorId;
    private String authorName;
    private String content;
    private LocalDateTime createdAt;

    public Comment() {}

    public Comment(int postId, String authorName, String content) {
        this.postId = postId;
        this.authorName = authorName;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }

    // Getters
    public int getId() { return id; }
    public int getPostId() { return postId; }
    public int getAuthorId() { return authorId; }
    public String getAuthorName() { return authorName; }
    public String getContent() { return content; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setPostId(int postId) { this.postId = postId; }
    public void setAuthorId(int authorId) { this.authorId = authorId; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public void setContent(String content) { this.content = content; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
