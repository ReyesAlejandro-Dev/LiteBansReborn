package com.nuvik.litebansreborn.managers;

import com.nuvik.litebansreborn.LiteBansReborn;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Social Network Analysis Manager - v5.1
 * 
 * Detects and tracks player relationships:
 * - Alt accounts (same IP, same HWID)
 * - Associates (played together frequently)
 * - Network of banned players
 * 
 * @author Nuvik
 * @version 5.1.0
 */
public class SocialNetworkManager {

    private final LiteBansReborn plugin;
    
    // In-memory cache of player sessions for relationship tracking
    private final Map<UUID, Set<UUID>> sessionPartners = new ConcurrentHashMap<>();
    private final Map<UUID, Long> sessionStartTimes = new ConcurrentHashMap<>();
    
    // Relationship types
    public enum RelationType {
        ALT_ACCOUNT("Same IP/HWID", 100),
        FREQUENT_PARTNER("Played together often", 70),
        SAME_SESSION("Online at same time", 30),
        BANNED_ASSOCIATE("Connected to banned player", 50);
        
        private final String description;
        private final int weight;
        
        RelationType(String description, int weight) {
            this.description = description;
            this.weight = weight;
        }
        
        public String getDescription() { return description; }
        public int getWeight() { return weight; }
    }
    
    public SocialNetworkManager(LiteBansReborn plugin) {
        this.plugin = plugin;
        createTables();
        startSessionTracker();
    }
    
    private void createTables() {
        String sql = """
            CREATE TABLE IF NOT EXISTS player_relationships (
                id INT AUTO_INCREMENT PRIMARY KEY,
                player1_uuid VARCHAR(36) NOT NULL,
                player2_uuid VARCHAR(36) NOT NULL,
                relation_type VARCHAR(32) NOT NULL,
                strength INT DEFAULT 1,
                first_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UNIQUE KEY unique_relation (player1_uuid, player2_uuid, relation_type)
            );
            
            CREATE TABLE IF NOT EXISTS player_ips (
                uuid VARCHAR(36) NOT NULL,
                ip_address VARCHAR(45) NOT NULL,
                first_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                times_seen INT DEFAULT 1,
                PRIMARY KEY (uuid, ip_address)
            );
            
            CREATE TABLE IF NOT EXISTS player_sessions (
                uuid VARCHAR(36) NOT NULL,
                session_start TIMESTAMP NOT NULL,
                session_end TIMESTAMP,
                ip_address VARCHAR(45),
                PRIMARY KEY (uuid, session_start)
            );
            """;
        
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             Statement stmt = conn.createStatement()) {
            for (String query : sql.split(";")) {
                if (!query.trim().isEmpty()) {
                    stmt.execute(query.trim());
                }
            }
        } catch (SQLException e) {
            plugin.log(Level.SEVERE, "Failed to create social network tables: " + e.getMessage());
        }
    }
    
    // ==================== SESSION TRACKING ====================
    
    /**
     * Start tracking player session partnerships
     */
    private void startSessionTracker() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // Every 5 minutes, record who's playing together
            Set<UUID> onlinePlayers = new HashSet<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                onlinePlayers.add(p.getUniqueId());
            }
            
            // Record session partnerships
            for (UUID player : onlinePlayers) {
                sessionPartners.computeIfAbsent(player, k -> ConcurrentHashMap.newKeySet())
                    .addAll(onlinePlayers);
            }
        }, 6000L, 6000L); // Every 5 minutes
    }
    
    /**
     * Record player join
     */
    public void recordJoin(Player player, String ip) {
        UUID uuid = player.getUniqueId();
        sessionStartTimes.put(uuid, System.currentTimeMillis());
        sessionPartners.put(uuid, ConcurrentHashMap.newKeySet());
        
        // Record IP
        recordIP(uuid, ip);
        
        // Check for alts
        checkForAlts(uuid, ip);
        
        // Add current online players as session partners
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.getUniqueId().equals(uuid)) {
                sessionPartners.get(uuid).add(online.getUniqueId());
            }
        }
    }
    
    /**
     * Record player quit
     */
    public void recordQuit(Player player) {
        UUID uuid = player.getUniqueId();
        Long startTime = sessionStartTimes.remove(uuid);
        Set<UUID> partners = sessionPartners.remove(uuid);
        
        if (startTime != null && partners != null && !partners.isEmpty()) {
            // Save session partnerships to database
            long sessionDuration = System.currentTimeMillis() - startTime;
            if (sessionDuration > 300000) { // Only count sessions > 5 minutes
                saveSessionPartnerships(uuid, partners);
            }
        }
    }
    
    private void recordIP(UUID uuid, String ip) {
        CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT INTO player_ips (uuid, ip_address, times_seen) 
                VALUES (?, ?, 1)
                ON DUPLICATE KEY UPDATE 
                    times_seen = times_seen + 1,
                    last_seen = CURRENT_TIMESTAMP
                """;
            
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                pstmt.setString(2, ip);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.log(Level.WARNING, "Failed to record IP: " + e.getMessage());
            }
        });
    }
    
    private void checkForAlts(UUID uuid, String ip) {
        CompletableFuture.runAsync(() -> {
            String sql = "SELECT uuid FROM player_ips WHERE ip_address = ? AND uuid != ?";
            
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, ip);
                pstmt.setString(2, uuid.toString());
                
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    UUID altUuid = UUID.fromString(rs.getString("uuid"));
                    addRelationship(uuid, altUuid, RelationType.ALT_ACCOUNT);
                }
            } catch (SQLException e) {
                plugin.log(Level.WARNING, "Failed to check alts: " + e.getMessage());
            }
        });
    }
    
    private void saveSessionPartnerships(UUID player, Set<UUID> partners) {
        for (UUID partner : partners) {
            addRelationship(player, partner, RelationType.SAME_SESSION);
        }
    }
    
    // ==================== RELATIONSHIP MANAGEMENT ====================
    
    /**
     * Add or strengthen a relationship between two players
     */
    public void addRelationship(UUID player1, UUID player2, RelationType type) {
        // Ensure consistent ordering
        UUID first = player1.compareTo(player2) < 0 ? player1 : player2;
        UUID second = player1.compareTo(player2) < 0 ? player2 : player1;
        
        CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT INTO player_relationships (player1_uuid, player2_uuid, relation_type, strength)
                VALUES (?, ?, ?, 1)
                ON DUPLICATE KEY UPDATE 
                    strength = strength + 1,
                    last_seen = CURRENT_TIMESTAMP
                """;
            
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, first.toString());
                pstmt.setString(2, second.toString());
                pstmt.setString(3, type.name());
                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.log(Level.WARNING, "Failed to add relationship: " + e.getMessage());
            }
        });
    }
    
    /**
     * Get all relationships for a player
     */
    public CompletableFuture<List<PlayerRelation>> getRelationships(UUID player) {
        return CompletableFuture.supplyAsync(() -> {
            List<PlayerRelation> relations = new ArrayList<>();
            
            String sql = """
                SELECT player1_uuid, player2_uuid, relation_type, strength, last_seen
                FROM player_relationships
                WHERE player1_uuid = ? OR player2_uuid = ?
                ORDER BY strength DESC
                """;
            
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, player.toString());
                pstmt.setString(2, player.toString());
                
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    UUID p1 = UUID.fromString(rs.getString("player1_uuid"));
                    UUID p2 = UUID.fromString(rs.getString("player2_uuid"));
                    UUID other = p1.equals(player) ? p2 : p1;
                    
                    RelationType type = RelationType.valueOf(rs.getString("relation_type"));
                    int strength = rs.getInt("strength");
                    long lastSeen = rs.getTimestamp("last_seen").getTime();
                    
                    relations.add(new PlayerRelation(other, type, strength, lastSeen));
                }
            } catch (SQLException e) {
                plugin.log(Level.WARNING, "Failed to get relationships: " + e.getMessage());
            }
            
            return relations;
        });
    }
    
    /**
     * Get potential alts for a player
     */
    public CompletableFuture<List<UUID>> getAlts(UUID player) {
        return getRelationships(player).thenApply(relations -> {
            return relations.stream()
                .filter(r -> r.type() == RelationType.ALT_ACCOUNT)
                .map(PlayerRelation::player)
                .toList();
        });
    }
    
    /**
     * Get associates of banned players
     */
    public CompletableFuture<List<PlayerRelation>> getBannedAssociates(UUID player) {
        return getRelationships(player).thenApply(relations -> {
            List<PlayerRelation> bannedAssociates = new ArrayList<>();
            
            for (PlayerRelation relation : relations) {
                // Check if the related player is banned
                if (plugin.getBanManager().getActiveBan(relation.player()).join() != null) {
                    bannedAssociates.add(new PlayerRelation(
                        relation.player(),
                        RelationType.BANNED_ASSOCIATE,
                        relation.strength(),
                        relation.lastSeen()
                    ));
                }
            }
            
            return bannedAssociates;
        });
    }
    
    /**
     * Calculate connection score to banned players
     */
    public CompletableFuture<Integer> getBannedConnectionScore(UUID player) {
        return getBannedAssociates(player).thenApply(associates -> {
            int score = 0;
            for (PlayerRelation rel : associates) {
                score += rel.type().getWeight() * Math.min(rel.strength(), 10);
            }
            return Math.min(score, 100);
        });
    }
    
    /**
     * Alert staff about suspicious connections
     */
    public void alertStaffAboutConnections(Player player, List<PlayerRelation> relations) {
        if (relations.isEmpty()) return;
        
        StringBuilder msg = new StringBuilder();
        msg.append("§8[§cNetwork§8] §e").append(player.getName()).append(" §7has connections:\n");
        
        int count = 0;
        for (PlayerRelation rel : relations) {
            if (count++ >= 5) break;
            String name = Bukkit.getOfflinePlayer(rel.player()).getName();
            msg.append("  §7- §f").append(name).append(" §8(§e")
               .append(rel.type().getDescription()).append("§8)\n");
        }
        
        // Send to staff
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("litebansreborn.alerts.network")) {
                staff.sendMessage(msg.toString());
            }
        }
    }
    
    // ==================== RECORD CLASS ====================
    
    public record PlayerRelation(UUID player, RelationType type, int strength, long lastSeen) {}
}
