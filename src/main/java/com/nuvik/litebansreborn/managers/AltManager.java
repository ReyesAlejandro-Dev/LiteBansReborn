package com.nuvik.litebansreborn.managers;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.models.PlayerData;
import com.nuvik.litebansreborn.models.Punishment;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Alt Manager - Handles alt account detection and related operations
 */
public class AltManager {

    private final LiteBansReborn plugin;
    
    public AltManager(LiteBansReborn plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Check for alt accounts on player join
     */
    public CompletableFuture<List<AltAccount>> checkAlts(UUID uuid, String ip) {
        if (!plugin.getConfigManager().getBoolean("alt-detection.enabled")) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }
        
        return plugin.getDatabaseManager().queryAsync(conn -> {
            // Get all UUIDs that have used this IP
            String sql = "SELECT DISTINCT p.uuid, p.last_known_name, p.last_seen " +
                    "FROM " + plugin.getDatabaseManager().getTable("players") + " p " +
                    "JOIN " + plugin.getDatabaseManager().getTable("player_ips") + " pi ON p.uuid = pi.uuid " +
                    "WHERE pi.ip = ? AND p.uuid != ?";
            
            List<AltAccount> alts = new ArrayList<>();
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, ip);
                stmt.setString(2, uuid.toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        AltAccount alt = new AltAccount(
                            UUID.fromString(rs.getString("uuid")),
                            rs.getString("last_known_name"),
                            rs.getTimestamp("last_seen") != null ? 
                                rs.getTimestamp("last_seen").toInstant() : null
                        );
                        
                        // Check if alt is banned
                        alt.setBanned(isPlayerBanned(conn, alt.getUuid()));
                        
                        alts.add(alt);
                    }
                }
            }
            
            return alts;
        });
    }
    
    /**
     * Get all alt accounts for a player
     */
    public CompletableFuture<List<AltAccount>> getAlts(UUID uuid) {
        return plugin.getDatabaseManager().queryAsync(conn -> {
            // First get all IPs used by this player
            String ipSql = "SELECT DISTINCT ip FROM " + 
                    plugin.getDatabaseManager().getTable("player_ips") +
                    " WHERE uuid = ?";
            
            List<String> ips = new ArrayList<>();
            
            try (PreparedStatement stmt = conn.prepareStatement(ipSql)) {
                stmt.setString(1, uuid.toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        ips.add(rs.getString("ip"));
                    }
                }
            }
            
            if (ips.isEmpty()) {
                return new ArrayList<>();
            }
            
            // Now find all players who used any of these IPs
            StringBuilder placeholders = new StringBuilder();
            for (int i = 0; i < ips.size(); i++) {
                placeholders.append(i == 0 ? "?" : ", ?");
            }
            
            String sql = "SELECT DISTINCT p.uuid, p.last_known_name, p.last_seen " +
                    "FROM " + plugin.getDatabaseManager().getTable("players") + " p " +
                    "JOIN " + plugin.getDatabaseManager().getTable("player_ips") + " pi ON p.uuid = pi.uuid " +
                    "WHERE pi.ip IN (" + placeholders + ") AND p.uuid != ?";
            
            List<AltAccount> alts = new ArrayList<>();
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                int index = 1;
                for (String ip : ips) {
                    stmt.setString(index++, ip);
                }
                stmt.setString(index, uuid.toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        AltAccount alt = new AltAccount(
                            UUID.fromString(rs.getString("uuid")),
                            rs.getString("last_known_name"),
                            rs.getTimestamp("last_seen") != null ? 
                                rs.getTimestamp("last_seen").toInstant() : null
                        );
                        
                        alt.setBanned(isPlayerBanned(conn, alt.getUuid()));
                        alts.add(alt);
                    }
                }
            }
            
            return alts;
        });
    }
    
    /**
     * Get all players with a specific IP
     */
    public CompletableFuture<List<AltAccount>> getPlayersWithIP(String ip) {
        return plugin.getDatabaseManager().queryAsync(conn -> {
            String sql = "SELECT DISTINCT p.uuid, p.last_known_name, p.last_seen " +
                    "FROM " + plugin.getDatabaseManager().getTable("players") + " p " +
                    "JOIN " + plugin.getDatabaseManager().getTable("player_ips") + " pi ON p.uuid = pi.uuid " +
                    "WHERE pi.ip = ?";
            
            List<AltAccount> players = new ArrayList<>();
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, ip);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        AltAccount alt = new AltAccount(
                            UUID.fromString(rs.getString("uuid")),
                            rs.getString("last_known_name"),
                            rs.getTimestamp("last_seen") != null ? 
                                rs.getTimestamp("last_seen").toInstant() : null
                        );
                        
                        alt.setBanned(isPlayerBanned(conn, alt.getUuid()));
                        players.add(alt);
                    }
                }
            }
            
            return players;
        });
    }
    
    /**
     * Record player IP for alt detection
     */
    public void recordPlayerIP(UUID uuid, String ip) {
        plugin.getDatabaseManager().executeAsync(conn -> {
            String sql = "INSERT INTO " + plugin.getDatabaseManager().getTable("player_ips") +
                    " (uuid, ip, first_seen, last_seen) VALUES (?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE last_seen = ?";
            
            Timestamp now = Timestamp.from(Instant.now());
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, ip);
                stmt.setTimestamp(3, now);
                stmt.setTimestamp(4, now);
                stmt.setTimestamp(5, now);
                
                stmt.executeUpdate();
            }
        });
    }
    
    /**
     * Update player data on join
     */
    public void updatePlayerData(UUID uuid, String name, String ip) {
        plugin.getDatabaseManager().executeAsync(conn -> {
            // Update or insert player data
            String sql = "INSERT INTO " + plugin.getDatabaseManager().getTable("players") +
                    " (uuid, last_known_name, last_known_ip, first_join, last_seen) " +
                    "VALUES (?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE last_known_name = ?, last_known_ip = ?, last_seen = ?";
            
            Timestamp now = Timestamp.from(Instant.now());
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, name);
                stmt.setString(3, ip);
                stmt.setTimestamp(4, now);
                stmt.setTimestamp(5, now);
                stmt.setString(6, name);
                stmt.setString(7, ip);
                stmt.setTimestamp(8, now);
                
                stmt.executeUpdate();
            }
            
            // Record name history
            sql = "INSERT INTO " + plugin.getDatabaseManager().getTable("player_names") +
                    " (uuid, name, first_seen, last_seen) VALUES (?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE last_seen = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, name);
                stmt.setTimestamp(3, now);
                stmt.setTimestamp(4, now);
                stmt.setTimestamp(5, now);
                
                stmt.executeUpdate();
            }
        });
        
        // Record IP
        recordPlayerIP(uuid, ip);
    }
    
    /**
     * Check if a player is allowed to bypass IP bans
     */
    public CompletableFuture<Boolean> isAllowed(UUID uuid) {
        return plugin.getDatabaseManager().queryAsync(conn -> {
            String sql = "SELECT uuid FROM " + plugin.getDatabaseManager().getTable("allowed_players") +
                    " WHERE uuid = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next();
                }
            }
        });
    }
    
    /**
     * Allow a player to bypass IP bans
     */
    public CompletableFuture<Boolean> allowPlayer(UUID uuid, UUID addedByUUID, String addedByName) {
        return plugin.getDatabaseManager().queryAsync(conn -> {
            String sql = "INSERT INTO " + plugin.getDatabaseManager().getTable("allowed_players") +
                    " (uuid, added_by_uuid, added_by_name, added_at) VALUES (?, ?, ?, ?)";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, addedByUUID.toString());
                stmt.setString(3, addedByName);
                stmt.setTimestamp(4, Timestamp.from(Instant.now()));
                
                return stmt.executeUpdate() > 0;
            } catch (SQLException e) {
                // Already exists
                return false;
            }
        });
    }
    
    /**
     * Check if player is banned (helper method)
     */
    private boolean isPlayerBanned(Connection conn, UUID uuid) throws SQLException {
        String sql = "SELECT id FROM " + plugin.getDatabaseManager().getTable("punishments") +
                " WHERE target_uuid = ? AND type IN ('ban', 'tempban') AND active = TRUE " +
                "AND (expires_at IS NULL OR expires_at > ?) LIMIT 1";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setTimestamp(2, Timestamp.from(Instant.now()));
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }
    
    /**
     * Alt account data class
     */
    public static class AltAccount {
        private final UUID uuid;
        private final String name;
        private final Instant lastSeen;
        private boolean banned;
        
        public AltAccount(UUID uuid, String name, Instant lastSeen) {
            this.uuid = uuid;
            this.name = name;
            this.lastSeen = lastSeen;
        }
        
        public UUID getUuid() { return uuid; }
        public String getName() { return name; }
        public Instant getLastSeen() { return lastSeen; }
        public boolean isBanned() { return banned; }
        public void setBanned(boolean banned) { this.banned = banned; }
    }
}
