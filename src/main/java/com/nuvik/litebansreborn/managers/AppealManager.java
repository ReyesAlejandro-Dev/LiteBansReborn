package com.nuvik.litebansreborn.managers;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.models.Appeal;
import com.nuvik.litebansreborn.models.Appeal.AppealStatus;
import com.nuvik.litebansreborn.models.PunishmentType;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Appeal Manager - Handles punishment appeals
 */
public class AppealManager {

    private final LiteBansReborn plugin;
    
    public AppealManager(LiteBansReborn plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Create a new appeal
     */
    public CompletableFuture<Appeal> createAppeal(long punishmentId, PunishmentType type,
                                                   UUID playerUUID, String playerName, String message) {
        
        Appeal appeal = new Appeal(punishmentId, type, playerUUID, playerName, message);
        
        return plugin.getDatabaseManager().queryAsync(conn -> {
            String sql = "INSERT INTO " + plugin.getDatabaseManager().getTable("appeals") +
                    " (punishment_id, punishment_type, player_uuid, player_name, message, " +
                    "created_at, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setLong(1, punishmentId);
                stmt.setString(2, type.getId());
                stmt.setString(3, playerUUID.toString());
                stmt.setString(4, playerName);
                stmt.setString(5, message);
                stmt.setTimestamp(6, Timestamp.from(appeal.getCreatedAt()));
                stmt.setString(7, AppealStatus.PENDING.getId());
                
                stmt.executeUpdate();
                
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        appeal.setId(rs.getLong(1));
                    }
                }
            }
            
            return appeal;
        });
    }
    
    /**
     * Get pending appeals
     */
    public CompletableFuture<List<Appeal>> getPendingAppeals(int page, int perPage) {
        return plugin.getDatabaseManager().queryAsync(conn -> {
            String sql = "SELECT * FROM " + plugin.getDatabaseManager().getTable("appeals") +
                    " WHERE status = 'pending' ORDER BY created_at DESC LIMIT ? OFFSET ?";
            
            List<Appeal> appeals = new ArrayList<>();
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, perPage);
                stmt.setInt(2, (page - 1) * perPage);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        appeals.add(parseAppeal(rs));
                    }
                }
            }
            
            return appeals;
        });
    }
    
    /**
     * Get an appeal by ID
     */
    public CompletableFuture<Appeal> getAppeal(long id) {
        return plugin.getDatabaseManager().queryAsync(conn -> {
            String sql = "SELECT * FROM " + plugin.getDatabaseManager().getTable("appeals") +
                    " WHERE id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, id);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return parseAppeal(rs);
                    }
                }
            }
            
            return null;
        });
    }
    
    /**
     * Handle an appeal
     */
    public CompletableFuture<Boolean> handleAppeal(long id, UUID handledByUUID, String handledByName,
                                                    AppealStatus status, String response) {
        return plugin.getDatabaseManager().queryAsync(conn -> {
            String sql = "UPDATE " + plugin.getDatabaseManager().getTable("appeals") +
                    " SET status = ?, handled_at = ?, handled_by_uuid = ?, " +
                    "handled_by_name = ?, response = ? WHERE id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, status.getId());
                stmt.setTimestamp(2, Timestamp.from(Instant.now()));
                stmt.setString(3, handledByUUID.toString());
                stmt.setString(4, handledByName);
                stmt.setString(5, response);
                stmt.setLong(6, id);
                
                return stmt.executeUpdate() > 0;
            }
        });
    }
    
    /**
     * Get appeal count for a punishment
     */
    public CompletableFuture<Integer> getAppealCount(long punishmentId) {
        return plugin.getDatabaseManager().queryAsync(conn -> {
            String sql = "SELECT COUNT(*) FROM " + plugin.getDatabaseManager().getTable("appeals") +
                    " WHERE punishment_id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, punishmentId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
            
            return 0;
        });
    }
    
    /**
     * Check if player can appeal
     */
    public CompletableFuture<Boolean> canAppeal(UUID playerUUID, long punishmentId) {
        return plugin.getDatabaseManager().queryAsync(conn -> {
            int maxAppeals = plugin.getConfigManager().getInt("appeals.max-appeals", 2);
            int cooldownHours = plugin.getConfigManager().getInt("appeals.cooldown", 24);
            
            // Check appeal count
            String countSql = "SELECT COUNT(*) FROM " + plugin.getDatabaseManager().getTable("appeals") +
                    " WHERE punishment_id = ? AND player_uuid = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(countSql)) {
                stmt.setLong(1, punishmentId);
                stmt.setString(2, playerUUID.toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) >= maxAppeals) {
                        return false;
                    }
                }
            }
            
            // Check cooldown
            String cooldownSql = "SELECT created_at FROM " + plugin.getDatabaseManager().getTable("appeals") +
                    " WHERE player_uuid = ? ORDER BY created_at DESC LIMIT 1";
            
            try (PreparedStatement stmt = conn.prepareStatement(cooldownSql)) {
                stmt.setString(1, playerUUID.toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Timestamp lastAppeal = rs.getTimestamp("created_at");
                        if (lastAppeal != null) {
                            Instant cooldownEnd = lastAppeal.toInstant().plusSeconds(cooldownHours * 3600L);
                            if (Instant.now().isBefore(cooldownEnd)) {
                                return false;
                            }
                        }
                    }
                }
            }
            
            return true;
        });
    }
    
    /**
     * Get pending appeals count
     */
    public CompletableFuture<Integer> getPendingAppealsCount() {
        return plugin.getDatabaseManager().queryAsync(conn -> {
            String sql = "SELECT COUNT(*) FROM " + plugin.getDatabaseManager().getTable("appeals") +
                    " WHERE status = 'pending'";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            
            return 0;
        });
    }
    
    private Appeal parseAppeal(ResultSet rs) throws SQLException {
        return new Appeal(
            rs.getLong("id"),
            rs.getLong("punishment_id"),
            PunishmentType.fromId(rs.getString("punishment_type")),
            UUID.fromString(rs.getString("player_uuid")),
            rs.getString("player_name"),
            rs.getString("message"),
            rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toInstant() : null,
            AppealStatus.fromId(rs.getString("status")),
            rs.getTimestamp("handled_at") != null ? rs.getTimestamp("handled_at").toInstant() : null,
            rs.getString("handled_by_uuid") != null ? UUID.fromString(rs.getString("handled_by_uuid")) : null,
            rs.getString("handled_by_name"),
            rs.getString("response")
        );
    }
}
