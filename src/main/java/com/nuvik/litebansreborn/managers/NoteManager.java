package com.nuvik.litebansreborn.managers;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.models.Note;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Note Manager - Handles staff notes on players
 */
public class NoteManager {

    private final LiteBansReborn plugin;
    
    public NoteManager(LiteBansReborn plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Add a note to a player
     */
    public CompletableFuture<Note> addNote(UUID targetUUID, String targetName,
                                           UUID authorUUID, String authorName, String content) {
        
        Note note = new Note(targetUUID, targetName, authorUUID, authorName, content,
                plugin.getConfigManager().getServerName());
        
        return plugin.getDatabaseManager().queryAsync(conn -> {
            String sql = "INSERT INTO " + plugin.getDatabaseManager().getTable("notes") +
                    " (target_uuid, target_name, author_uuid, author_name, content, server, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, targetUUID.toString());
                stmt.setString(2, targetName);
                stmt.setString(3, authorUUID.toString());
                stmt.setString(4, authorName);
                stmt.setString(5, content);
                stmt.setString(6, note.getServer());
                stmt.setTimestamp(7, Timestamp.from(note.getCreatedAt()));
                
                stmt.executeUpdate();
                
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        note.setId(rs.getLong(1));
                    }
                }
            }
            
            return note;
        });
    }
    
    /**
     * Get notes for a player
     */
    public CompletableFuture<List<Note>> getNotes(UUID targetUUID) {
        return plugin.getDatabaseManager().queryAsync(conn -> {
            String sql = "SELECT * FROM " + plugin.getDatabaseManager().getTable("notes") +
                    " WHERE target_uuid = ? ORDER BY created_at DESC";
            
            List<Note> notes = new ArrayList<>();
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, targetUUID.toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        notes.add(parseNote(rs));
                    }
                }
            }
            
            return notes;
        });
    }
    
    /**
     * Get a note by ID
     */
    public CompletableFuture<Note> getNote(long id) {
        return plugin.getDatabaseManager().queryAsync(conn -> {
            String sql = "SELECT * FROM " + plugin.getDatabaseManager().getTable("notes") +
                    " WHERE id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, id);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return parseNote(rs);
                    }
                }
            }
            
            return null;
        });
    }
    
    /**
     * Delete a note
     */
    public CompletableFuture<Boolean> deleteNote(long id) {
        return plugin.getDatabaseManager().queryAsync(conn -> {
            String sql = "DELETE FROM " + plugin.getDatabaseManager().getTable("notes") +
                    " WHERE id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, id);
                
                return stmt.executeUpdate() > 0;
            }
        });
    }
    
    /**
     * Get note count for a player
     */
    public CompletableFuture<Integer> getNoteCount(UUID targetUUID) {
        return plugin.getDatabaseManager().queryAsync(conn -> {
            String sql = "SELECT COUNT(*) FROM " + plugin.getDatabaseManager().getTable("notes") +
                    " WHERE target_uuid = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, targetUUID.toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
            
            return 0;
        });
    }
    
    private Note parseNote(ResultSet rs) throws SQLException {
        return new Note(
            rs.getLong("id"),
            UUID.fromString(rs.getString("target_uuid")),
            rs.getString("target_name"),
            UUID.fromString(rs.getString("author_uuid")),
            rs.getString("author_name"),
            rs.getString("content"),
            rs.getString("server"),
            rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toInstant() : null
        );
    }
}
