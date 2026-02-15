package Entites;

import java.time.LocalDateTime;

public class Comment {
    private int id;
    private int postId; // Foreign Key link to the Post
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

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPostId() { return postId; }
    public void setPostId(int postId) { this.postId = postId; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}