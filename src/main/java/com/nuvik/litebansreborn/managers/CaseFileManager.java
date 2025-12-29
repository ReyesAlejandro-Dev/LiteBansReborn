package com.nuvik.litebansreborn.managers;

import com.nuvik.litebansreborn.LiteBansReborn;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Automatic Case File Manager - v5.1
 * 
 * Automatically collects evidence when staff freezes a player:
 * - Last 200 chat messages
 * - Recent commands
 * - Movement history
 * - Connection info
 * - Screenshots (if supported)
 * 
 * @author Nuvik
 * @version 5.1.0
 */
public class CaseFileManager {

    private final LiteBansReborn plugin;
    
    // Rolling buffers for each player
    private final Map<UUID, LinkedList<ChatEntry>> chatHistory = new ConcurrentHashMap<>();
    private final Map<UUID, LinkedList<CommandEntry>> commandHistory = new ConcurrentHashMap<>();
    private final Map<UUID, LinkedList<MovementEntry>> movementHistory = new ConcurrentHashMap<>();
    
    private static final int MAX_CHAT_HISTORY = 200;
    private static final int MAX_COMMAND_HISTORY = 50;
    private static final int MAX_MOVEMENT_HISTORY = 100;
    
    public CaseFileManager(LiteBansReborn plugin) {
        this.plugin = plugin;
        createTables();
        startMovementTracker();
    }
    
    private void createTables() {
        String sql = """
            CREATE TABLE IF NOT EXISTS case_files (
                id INT AUTO_INCREMENT PRIMARY KEY,
                case_id VARCHAR(36) UNIQUE NOT NULL,
                target_uuid VARCHAR(36) NOT NULL,
                target_name VARCHAR(16) NOT NULL,
                creator_uuid VARCHAR(36) NOT NULL,
                creator_name VARCHAR(16) NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                status VARCHAR(16) DEFAULT 'OPEN',
                notes TEXT,
                verdict VARCHAR(32)
            );
            
            CREATE TABLE IF NOT EXISTS case_evidence (
                id INT AUTO_INCREMENT PRIMARY KEY,
                case_id VARCHAR(36) NOT NULL,
                evidence_type VARCHAR(32) NOT NULL,
                content TEXT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (case_id) REFERENCES case_files(case_id)
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
            plugin.log(Level.SEVERE, "Failed to create case file tables: " + e.getMessage());
        }
    }
    
    // ==================== REAL-TIME TRACKING ====================
    
    /**
     * Record a chat message
     */
    public void recordChat(Player player, String message) {
        UUID uuid = player.getUniqueId();
        chatHistory.computeIfAbsent(uuid, k -> new LinkedList<>());
        
        LinkedList<ChatEntry> history = chatHistory.get(uuid);
        synchronized (history) {
            history.addLast(new ChatEntry(System.currentTimeMillis(), message));
            while (history.size() > MAX_CHAT_HISTORY) {
                history.removeFirst();
            }
        }
    }
    
    /**
     * Record a command
     */
    public void recordCommand(Player player, String command) {
        UUID uuid = player.getUniqueId();
        commandHistory.computeIfAbsent(uuid, k -> new LinkedList<>());
        
        LinkedList<CommandEntry> history = commandHistory.get(uuid);
        synchronized (history) {
            history.addLast(new CommandEntry(System.currentTimeMillis(), command));
            while (history.size() > MAX_COMMAND_HISTORY) {
                history.removeFirst();
            }
        }
    }
    
    /**
     * Start movement tracking (every 2 seconds for online players)
     */
    private void startMovementTracker() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                recordMovement(player);
            }
        }, 40L, 40L); // Every 2 seconds
    }
    
    private void recordMovement(Player player) {
        UUID uuid = player.getUniqueId();
        Location loc = player.getLocation();
        
        movementHistory.computeIfAbsent(uuid, k -> new LinkedList<>());
        
        LinkedList<MovementEntry> history = movementHistory.get(uuid);
        synchronized (history) {
            history.addLast(new MovementEntry(
                System.currentTimeMillis(),
                loc.getWorld().getName(),
                loc.getX(), loc.getY(), loc.getZ(),
                player.isFlying(),
                player.isSprinting()
            ));
            while (history.size() > MAX_MOVEMENT_HISTORY) {
                history.removeFirst();
            }
        }
    }
    
    // ==================== CASE FILE CREATION ====================
    
    /**
     * Create a case file for a player (called when /freeze is used)
     */
    public CompletableFuture<CaseFile> createCaseFile(Player target, Player creator) {
        return CompletableFuture.supplyAsync(() -> {
            String caseId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            
            CaseFile caseFile = new CaseFile(
                caseId,
                target.getUniqueId(),
                target.getName(),
                creator.getUniqueId(),
                creator.getName(),
                System.currentTimeMillis()
            );
            
            // Save to database
            saveCaseFile(caseFile);
            
            // Collect all evidence
            collectEvidence(caseFile, target);
            
            plugin.log(Level.INFO, "Created case file " + caseId + " for " + target.getName());
            
            return caseFile;
        });
    }
    
    private void saveCaseFile(CaseFile caseFile) {
        String sql = """
            INSERT INTO case_files (case_id, target_uuid, target_name, creator_uuid, creator_name)
            VALUES (?, ?, ?, ?, ?)
            """;
        
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, caseFile.caseId());
            pstmt.setString(2, caseFile.targetUuid().toString());
            pstmt.setString(3, caseFile.targetName());
            pstmt.setString(4, caseFile.creatorUuid().toString());
            pstmt.setString(5, caseFile.creatorName());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.log(Level.SEVERE, "Failed to save case file: " + e.getMessage());
        }
    }
    
    private void collectEvidence(CaseFile caseFile, Player target) {
        UUID uuid = target.getUniqueId();
        
        // Collect chat history
        LinkedList<ChatEntry> chat = chatHistory.get(uuid);
        if (chat != null && !chat.isEmpty()) {
            StringBuilder chatLog = new StringBuilder();
            synchronized (chat) {
                for (ChatEntry entry : chat) {
                    chatLog.append("[").append(formatTime(entry.timestamp())).append("] ")
                           .append(entry.message()).append("\n");
                }
            }
            saveEvidence(caseFile.caseId(), "CHAT_HISTORY", chatLog.toString());
        }
        
        // Collect command history
        LinkedList<CommandEntry> commands = commandHistory.get(uuid);
        if (commands != null && !commands.isEmpty()) {
            StringBuilder cmdLog = new StringBuilder();
            synchronized (commands) {
                for (CommandEntry entry : commands) {
                    cmdLog.append("[").append(formatTime(entry.timestamp())).append("] /")
                          .append(entry.command()).append("\n");
                }
            }
            saveEvidence(caseFile.caseId(), "COMMAND_HISTORY", cmdLog.toString());
        }
        
        // Collect movement history
        LinkedList<MovementEntry> movements = movementHistory.get(uuid);
        if (movements != null && !movements.isEmpty()) {
            StringBuilder moveLog = new StringBuilder();
            synchronized (movements) {
                for (MovementEntry entry : movements) {
                    moveLog.append("[").append(formatTime(entry.timestamp())).append("] ")
                           .append(entry.world()).append(" ")
                           .append(String.format("%.1f, %.1f, %.1f", entry.x(), entry.y(), entry.z()))
                           .append(entry.flying() ? " [FLYING]" : "")
                           .append(entry.sprinting() ? " [SPRINT]" : "")
                           .append("\n");
                }
            }
            saveEvidence(caseFile.caseId(), "MOVEMENT_HISTORY", moveLog.toString());
        }
        
        // Collect connection info
        StringBuilder connInfo = new StringBuilder();
        connInfo.append("IP: ").append(target.getAddress().getAddress().getHostAddress()).append("\n");
        connInfo.append("Client: ").append("Unknown").append("\n");
        connInfo.append("Protocol: ").append("Unknown").append("\n");
        connInfo.append("Ping: ").append(target.getPing()).append("ms\n");
        connInfo.append("Game Mode: ").append(target.getGameMode()).append("\n");
        connInfo.append("Location: ").append(target.getLocation().toString()).append("\n");
        saveEvidence(caseFile.caseId(), "CONNECTION_INFO", connInfo.toString());
        
        // Collect player relationships
        if (plugin.getSocialNetworkManager() != null) {
            plugin.getSocialNetworkManager().getRelationships(uuid).thenAccept(relations -> {
                if (!relations.isEmpty()) {
                    StringBuilder relLog = new StringBuilder();
                    for (var rel : relations) {
                        String name = Bukkit.getOfflinePlayer(rel.player()).getName();
                        relLog.append("- ").append(name).append(" (")
                              .append(rel.type().getDescription()).append(", strength: ")
                              .append(rel.strength()).append(")\n");
                    }
                    saveEvidence(caseFile.caseId(), "PLAYER_RELATIONSHIPS", relLog.toString());
                }
            });
        }
    }
    
    private void saveEvidence(String caseId, String type, String content) {
        String sql = "INSERT INTO case_evidence (case_id, evidence_type, content) VALUES (?, ?, ?)";
        
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, caseId);
            pstmt.setString(2, type);
            pstmt.setString(3, content);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.log(Level.WARNING, "Failed to save evidence: " + e.getMessage());
        }
    }
    
    // ==================== CASE FILE RETRIEVAL ====================
    
    /**
     * Get a case file by ID
     */
    public CompletableFuture<CaseFile> getCaseFile(String caseId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM case_files WHERE case_id = ?";
            
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, caseId);
                ResultSet rs = pstmt.executeQuery();
                
                if (rs.next()) {
                    return new CaseFile(
                        rs.getString("case_id"),
                        UUID.fromString(rs.getString("target_uuid")),
                        rs.getString("target_name"),
                        UUID.fromString(rs.getString("creator_uuid")),
                        rs.getString("creator_name"),
                        rs.getTimestamp("created_at").getTime()
                    );
                }
            } catch (SQLException e) {
                plugin.log(Level.WARNING, "Failed to get case file: " + e.getMessage());
            }
            return null;
        });
    }
    
    /**
     * Get all evidence for a case
     */
    public CompletableFuture<Map<String, String>> getCaseEvidence(String caseId) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, String> evidence = new LinkedHashMap<>();
            String sql = "SELECT evidence_type, content FROM case_evidence WHERE case_id = ? ORDER BY created_at";
            
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, caseId);
                ResultSet rs = pstmt.executeQuery();
                
                while (rs.next()) {
                    evidence.put(rs.getString("evidence_type"), rs.getString("content"));
                }
            } catch (SQLException e) {
                plugin.log(Level.WARNING, "Failed to get case evidence: " + e.getMessage());
            }
            return evidence;
        });
    }
    
    /**
     * Get cases for a player
     */
    public CompletableFuture<List<CaseFile>> getCasesForPlayer(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            List<CaseFile> cases = new ArrayList<>();
            String sql = "SELECT * FROM case_files WHERE target_uuid = ? ORDER BY created_at DESC";
            
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                ResultSet rs = pstmt.executeQuery();
                
                while (rs.next()) {
                    cases.add(new CaseFile(
                        rs.getString("case_id"),
                        UUID.fromString(rs.getString("target_uuid")),
                        rs.getString("target_name"),
                        UUID.fromString(rs.getString("creator_uuid")),
                        rs.getString("creator_name"),
                        rs.getTimestamp("created_at").getTime()
                    ));
                }
            } catch (SQLException e) {
                plugin.log(Level.WARNING, "Failed to get player cases: " + e.getMessage());
            }
            return cases;
        });
    }
    
    // ==================== UTILITIES ====================
    
    private String formatTime(long timestamp) {
        return new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date(timestamp));
    }
    
    /**
     * Clean up player data on quit
     */
    public void cleanupPlayer(UUID uuid) {
        // Keep history for a bit after quit in case of quick rejoin
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            if (Bukkit.getPlayer(uuid) == null) {
                chatHistory.remove(uuid);
                commandHistory.remove(uuid);
                movementHistory.remove(uuid);
            }
        }, 6000L); // 5 minutes
    }
    
    // ==================== RECORD CLASSES ====================
    
    public record CaseFile(String caseId, UUID targetUuid, String targetName, 
                           UUID creatorUuid, String creatorName, long createdAt) {}
    
    private record ChatEntry(long timestamp, String message) {}
    private record CommandEntry(long timestamp, String command) {}
    private record MovementEntry(long timestamp, String world, double x, double y, double z, 
                                  boolean flying, boolean sprinting) {}
}
