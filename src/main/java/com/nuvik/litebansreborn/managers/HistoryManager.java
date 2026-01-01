package com.nuvik.litebansreborn.managers;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.models.Punishment;
import com.nuvik.litebansreborn.models.PunishmentType;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * History Manager - Handles punishment history queries
 */
public class HistoryManager {

    private final LiteBansReborn plugin;
    
    public HistoryManager(LiteBansReborn plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Get all punishments for a player
     */
    public CompletableFuture<List<Punishment>> getPlayerHistory(UUID uuid) {
        return plugin.getDatabaseManager().queryAsync(conn -> {
            String sql = "SELECT * FROM " + plugin.getDatabaseManager().getTable("punishments") +
                    " WHERE target_uuid = ? ORDER BY created_at DESC";
            
            List<Punishment> history = new ArrayList<>();
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        history.add(parsePunishment(rs));
                    }
                }
            }
            return history;
        });
    }
    
    /**
     * Get player history with pagination
     */
    public CompletableFuture<List<Punishment>> getPlayerHistory(UUID uuid, int page, int perPage) {
        return plugin.getDatabaseManager().queryAsync(conn -> {
            String sql = "SELECT * FROM " + plugin.getDatabaseManager().getTable("punishments") +
                    " WHERE target_uuid = ? ORDER BY created_at DESC LIMIT ? OFFSET ?";
            
            List<Punishment> history = new ArrayList<>();
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                stmt.setInt(2, perPage);
                stmt.setInt(3, (page - 1) * perPage);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        history.add(parsePunishment(rs));
                    }
                }
            }
            return history;
        });
    }
    
    /**
     * Get total history count for a player
     */
    public CompletableFuture<Integer> getPlayerHistoryCount(UUID uuid) {
        return plugin.getDatabaseManager().queryAsync(conn -> {
            String sql = "SELECT COUNT(*) FROM " + plugin.getDatabaseManager().getTable("punishments") +
                    " WHERE target_uuid = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                
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
     * Get all punishments issued by a staff member
     */
    public CompletableFuture<List<Punishment>> getStaffHistory(UUID staffUUID) {
        return plugin.getDatabaseManager().queryAsync(conn -> {
            String sql = "SELECT * FROM " + plugin.getDatabaseManager().getTable("punishments") +
                    " WHERE executor_uuid = ? ORDER BY created_at DESC";
            
            List<Punishment> history = new ArrayList<>();
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, staffUUID.toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        history.add(parsePunishment(rs));
                    }
                }
            }
            return history;
        });
    }
    
    /**
     * Get staff history with pagination
     */
    public CompletableFuture<List<Punishment>> getStaffHistory(UUID staffUUID, int page, int perPage) {
        return plugin.getDatabaseManager().queryAsync(conn -> {
            String sql = "SELECT * FROM " + plugin.getDatabaseManager().getTable("punishments") +
                    " WHERE executor_uuid = ? ORDER BY created_at DESC LIMIT ? OFFSET ?";
            
            List<Punishment> history = new ArrayList<>();
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, staffUUID.toString());
                stmt.setInt(2, perPage);
                stmt.setInt(3, (page - 1) * perPage);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        history.add(parsePunishment(rs));
                    }
                }
            }
            return history;
        });
    }
    
    /**
     * Rollback all punishments by a staff member
     */
    public CompletableFuture<Integer> rollbackStaff(UUID staffUUID, UUID removedByUUID, String removedByName, Long sinceMillis) {
        return plugin.getDatabaseManager().queryAsync(conn -> {
            String sql;
            PreparedStatement stmt;
            
            if (sinceMillis != null && sinceMillis > 0) {
                sql = "UPDATE " + plugin.getDatabaseManager().getTable("punishments") +
                        " SET active = FALSE, removed_at = ?, removed_by_uuid = ?, removed_by_name = ?, " +
                        "remove_reason = 'Staff rollback' " +
                        "WHERE executor_uuid = ? AND active = TRUE AND created_at > ?";
                stmt = conn.prepareStatement(sql);
                stmt.setTimestamp(1, Timestamp.from(Instant.now()));
                stmt.setString(2, removedByUUID.toString());
                stmt.setString(3, removedByName);
                stmt.setString(4, staffUUID.toString());
                stmt.setTimestamp(5, Timestamp.from(Instant.now().minusMillis(sinceMillis)));
            } else {
                sql = "UPDATE " + plugin.getDatabaseManager().getTable("punishments") +
                        " SET active = FALSE, removed_at = ?, removed_by_uuid = ?, removed_by_name = ?, " +
                        "remove_reason = 'Staff rollback' " +
                        "WHERE executor_uuid = ? AND active = TRUE";
                stmt = conn.prepareStatement(sql);
                stmt.setTimestamp(1, Timestamp.from(Instant.now()));
                stmt.setString(2, removedByUUID.toString());
                stmt.setString(3, removedByName);
                stmt.setString(4, staffUUID.toString());
            }
            
            int affected = stmt.executeUpdate();
            stmt.close();
            return affected;
        }).thenApply(count -> {
            // Clear caches since punishments were removed
            plugin.getCacheManager().clearAll();
            return count;
        });
    }
    
    /**
     * Get count of rollback candidates
     */
    public CompletableFuture<Integer> getRollbackCount(UUID staffUUID, Long sinceMillis) {
        return plugin.getDatabaseManager().queryAsync(conn -> {
            String sql;
            PreparedStatement stmt;
            
            if (sinceMillis != null && sinceMillis > 0) {
                sql = "SELECT COUNT(*) FROM " + plugin.getDatabaseManager().getTable("punishments") +
                        " WHERE executor_uuid = ? AND active = TRUE AND created_at > ?";
                stmt = conn.prepareStatement(sql);
                stmt.setString(1, staffUUID.toString());
                stmt.setTimestamp(2, Timestamp.from(Instant.now().minusMillis(sinceMillis)));
            } else {
                sql = "SELECT COUNT(*) FROM " + plugin.getDatabaseManager().getTable("punishments") +
                        " WHERE executor_uuid = ? AND active = TRUE";
                stmt = conn.prepareStatement(sql);
                stmt.setString(1, staffUUID.toString());
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    stmt.close();
                    return count;
                }
            }
            stmt.close();
            return 0;
        });
    }
    
    /**
     * Get a specific punishment by ID
     */
    public CompletableFuture<Punishment> getPunishment(long id) {
        return plugin.getDatabaseManager().queryAsync(conn -> {
            String sql = "SELECT * FROM " + plugin.getDatabaseManager().getTable("punishments") +
                    " WHERE id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, id);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return parsePunishment(rs);
                    }
                }
            }
            return null;
        });
    }
    
    /**
     * Get punishment statistics
     */
    public CompletableFuture<PunishmentStats> getStats() {
        return plugin.getDatabaseManager().queryAsync(conn -> {
            PunishmentStats stats = new PunishmentStats();
            
            String sql = "SELECT type, COUNT(*) as count FROM " + 
                    plugin.getDatabaseManager().getTable("punishments") +
                    " GROUP BY type";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String type = rs.getString("type");
                    int count = rs.getInt("count");
                    stats.setCounts(type, count);
                }
            }
            
            // Get active counts
            sql = "SELECT type, COUNT(*) as count FROM " + 
                    plugin.getDatabaseManager().getTable("punishments") +
                    " WHERE active = TRUE AND (expires_at IS NULL OR expires_at > ?) GROUP BY type";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setTimestamp(1, Timestamp.from(Instant.now()));
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String type = rs.getString("type");
                        int count = rs.getInt("count");
                        stats.setActiveCounts(type, count);
                    }
                }
            }
            
            return stats;
        });
    }
    
    /**
     * Parse a Punishment from ResultSet using centralized method
     */
    private Punishment parsePunishment(ResultSet rs) throws SQLException {
        return Punishment.fromResultSet(rs);
    }
    
    /**
     * Get all punishments with pagination and type filter
     */
    public CompletableFuture<List<Punishment>> getAllPunishments(String type, int page, int perPage) {
        return plugin.getDatabaseManager().queryAsync(conn -> {
            String sql;
            if (type == null || type.equals("all")) {
                sql = "SELECT * FROM " + plugin.getDatabaseManager().getTable("punishments") +
                        " ORDER BY created_at DESC LIMIT ? OFFSET ?";
            } else {
                sql = "SELECT * FROM " + plugin.getDatabaseManager().getTable("punishments") +
                        " WHERE type LIKE ? ORDER BY created_at DESC LIMIT ? OFFSET ?";
            }
            
            List<Punishment> history = new ArrayList<>();
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                if (type == null || type.equals("all")) {
                    stmt.setInt(1, perPage);
                    stmt.setInt(2, (page - 1) * perPage);
                } else {
                    stmt.setString(1, "%" + type + "%");
                    stmt.setInt(2, perPage);
                    stmt.setInt(3, (page - 1) * perPage);
                }
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        history.add(parsePunishment(rs));
                    }
                }
            }
            return history;
        });
    }
    
    /**
     * Get total punishment count for pagination
     */
    public CompletableFuture<Integer> getTotalPunishmentCount(String type) {
        return plugin.getDatabaseManager().queryAsync(conn -> {
            String sql;
            if (type == null || type.equals("all")) {
                sql = "SELECT COUNT(*) FROM " + plugin.getDatabaseManager().getTable("punishments");
            } else {
                sql = "SELECT COUNT(*) FROM " + plugin.getDatabaseManager().getTable("punishments") +
                        " WHERE type LIKE ?";
            }
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                if (type != null && !type.equals("all")) {
                    stmt.setString(1, "%" + type + "%");
                }
                
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
     * Punishment statistics container
     */
    public static class PunishmentStats {
        private int totalBans = 0;
        private int totalMutes = 0;
        private int totalKicks = 0;
        private int totalWarns = 0;
        private int activeBans = 0;
        private int activeMutes = 0;
        
        public void setCounts(String type, int count) {
            switch (type.toLowerCase()) {
                case "ban":
                case "tempban":
                case "ipban":
                    totalBans += count;
                    break;
                case "mute":
                case "tempmute":
                case "ipmute":
                    totalMutes += count;
                    break;
                case "kick":
                    totalKicks = count;
                    break;
                case "warn":
                    totalWarns = count;
                    break;
            }
        }
        
        public void setActiveCounts(String type, int count) {
            switch (type.toLowerCase()) {
                case "ban":
                case "tempban":
                case "ipban":
                    activeBans += count;
                    break;
                case "mute":
                case "tempmute":
                case "ipmute":
                    activeMutes += count;
                    break;
            }
        }
        
        // Getters
        public int getTotalBans() { return totalBans; }
        public int getTotalMutes() { return totalMutes; }
        public int getTotalKicks() { return totalKicks; }
        public int getTotalWarns() { return totalWarns; }
        public int getActiveBans() { return activeBans; }
        public int getActiveMutes() { return activeMutes; }
        public int getTotal() { return totalBans + totalMutes + totalKicks + totalWarns; }
    }
}
