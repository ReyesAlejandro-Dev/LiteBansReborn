package com.nuvik.litebansreborn.models;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a staff note on a player
 */
public class Note {
    
    private long id;
    private final UUID targetUUID;
    private final String targetName;
    private final UUID authorUUID;
    private final String authorName;
    private final String content;
    private final String server;
    private final Instant createdAt;
    
    /**
     * Create a new note
     */
    public Note(UUID targetUUID, String targetName, UUID authorUUID, 
                String authorName, String content, String server) {
        this.targetUUID = targetUUID;
        this.targetName = targetName;
        this.authorUUID = authorUUID;
        this.authorName = authorName;
        this.content = content;
        this.server = server;
        this.createdAt = Instant.now();
    }
    
    /**
     * Load an existing note from database
     */
    public Note(long id, UUID targetUUID, String targetName, UUID authorUUID,
                String authorName, String content, String server, Instant createdAt) {
        this.id = id;
        this.targetUUID = targetUUID;
        this.targetName = targetName;
        this.authorUUID = authorUUID;
        this.authorName = authorName;
        this.content = content;
        this.server = server;
        this.createdAt = createdAt;
    }
    
    // Getters and Setters
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public UUID getTargetUUID() {
        return targetUUID;
    }
    
    public String getTargetName() {
        return targetName;
    }
    
    public UUID getAuthorUUID() {
        return authorUUID;
    }
    
    public String getAuthorName() {
        return authorName;
    }
    
    public String getContent() {
        return content;
    }
    
    public String getServer() {
        return server;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
}
