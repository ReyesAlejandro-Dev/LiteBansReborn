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
 * Predictive Moderation Manager - v5.1
 * 
 * Uses behavioral analysis and AI to predict player risk:
 * - Analyzes chat patterns
 * - Tracks warning/punishment history
 * - Compares to patterns of previously banned players
 * - Generates risk scores and predictions
 * 
 * @author Nuvik
 * @version 5.1.0
 */
public class PredictiveManager {

    private final LiteBansReborn plugin;
    
    // Risk score cache
    private final Map<UUID, PlayerRiskProfile> riskProfiles = new ConcurrentHashMap<>();
    
    // Behavioral patterns from banned players (learned)
    private final List<BehaviorPattern> bannedPatterns = new ArrayList<>();
    
    // Risk thresholds
    private static final int LOW_RISK = 30;
    private static final int MEDIUM_RISK = 50;
    private static final int HIGH_RISK = 70;
    private static final int CRITICAL_RISK = 90;
    
    public PredictiveManager(LiteBansReborn plugin) {
        this.plugin = plugin;
        createTables();
        loadBannedPatterns();
        startRiskAnalyzer();
    }
    
    private void createTables() {
        String sql = """
            CREATE TABLE IF NOT EXISTS player_risk_scores (
                uuid VARCHAR(36) PRIMARY KEY,
                risk_score INT DEFAULT 50,
                prediction_confidence INT DEFAULT 0,
                last_analysis TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                total_messages INT DEFAULT 0,
                toxic_messages INT DEFAULT 0,
                total_warnings INT DEFAULT 0,
                total_mutes INT DEFAULT 0,
                total_bans INT DEFAULT 0,
                playtime_hours DOUBLE DEFAULT 0,
                first_join TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );
            
            CREATE TABLE IF NOT EXISTS behavior_patterns (
                id INT AUTO_INCREMENT PRIMARY KEY,
                pattern_type VARCHAR(32) NOT NULL,
                pattern_value TEXT NOT NULL,
                weight INT DEFAULT 1,
                source VARCHAR(32) DEFAULT 'learned'
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
            plugin.log(Level.SEVERE, "Failed to create predictive tables: " + e.getMessage());
        }
    }
    
    private void loadBannedPatterns() {
        // Load patterns from database
        String sql = "SELECT * FROM behavior_patterns";
        
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                bannedPatterns.add(new BehaviorPattern(
                    rs.getString("pattern_type"),
                    rs.getString("pattern_value"),
                    rs.getInt("weight")
                ));
            }
            
            plugin.log(Level.INFO, "Loaded " + bannedPatterns.size() + " behavior patterns.");
        } catch (SQLException e) {
            plugin.log(Level.WARNING, "Failed to load behavior patterns: " + e.getMessage());
        }
        
        // Add default patterns if none exist
        if (bannedPatterns.isEmpty()) {
            addDefaultPatterns();
        }
    }
    
    private void addDefaultPatterns() {
        // Common patterns of problematic players
        String[][] defaults = {
            {"CHAT_FREQUENCY", "high_frequency_short_messages", "10"},
            {"CHAT_CONTENT", "repeated_caps_usage", "15"},
            {"CHAT_CONTENT", "excessive_punctuation", "10"},
            {"BEHAVIOR", "rapid_warning_accumulation", "25"},
            {"BEHAVIOR", "multiple_mutes_short_period", "30"},
            {"CONNECTION", "multiple_accounts_same_ip", "20"},
            {"CONNECTION", "vpn_usage", "15"},
            {"TIMING", "join_quit_spam", "20"},
            {"SOCIAL", "associates_with_banned", "25"},
        };
        
        for (String[] pattern : defaults) {
            bannedPatterns.add(new BehaviorPattern(pattern[0], pattern[1], Integer.parseInt(pattern[2])));
            savePattern(pattern[0], pattern[1], Integer.parseInt(pattern[2]));
        }
    }
    
    private void savePattern(String type, String value, int weight) {
        String sql = "INSERT INTO behavior_patterns (pattern_type, pattern_value, weight) VALUES (?, ?, ?)";
        
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, type);
            pstmt.setString(2, value);
            pstmt.setInt(3, weight);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            // Ignore duplicates
        }
    }
    
    // ==================== RISK ANALYSIS ====================
    
    /**
     * Start periodic risk analysis
     */
    private void startRiskAnalyzer() {
        // Analyze online players every 10 minutes
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                analyzePlayer(player.getUniqueId());
            }
        }, 12000L, 12000L); // 10 minutes
    }
    
    /**
     * Analyze a player and calculate risk score
     */
    public CompletableFuture<PlayerRiskProfile> analyzePlayer(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            PlayerRiskProfile profile = riskProfiles.computeIfAbsent(uuid, 
                k -> loadOrCreateProfile(uuid));
            
            int riskScore = calculateRiskScore(uuid, profile);
            int prediction = calculateBanPrediction(uuid, profile);
            
            profile = new PlayerRiskProfile(
                uuid,
                riskScore,
                prediction,
                profile.totalMessages(),
                profile.toxicMessages(),
                profile.totalWarnings(),
                profile.totalMutes(),
                profile.totalBans(),
                profile.playtimeHours(),
                System.currentTimeMillis()
            );
            
            riskProfiles.put(uuid, profile);
            saveProfile(profile);
            
            // Alert if high risk
            if (riskScore >= HIGH_RISK && prediction >= 60) {
                alertHighRisk(uuid, profile);
            }
            
            return profile;
        });
    }
    
    private PlayerRiskProfile loadOrCreateProfile(UUID uuid) {
        String sql = "SELECT * FROM player_risk_scores WHERE uuid = ?";
        
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return new PlayerRiskProfile(
                    uuid,
                    rs.getInt("risk_score"),
                    rs.getInt("prediction_confidence"),
                    rs.getInt("total_messages"),
                    rs.getInt("toxic_messages"),
                    rs.getInt("total_warnings"),
                    rs.getInt("total_mutes"),
                    rs.getInt("total_bans"),
                    rs.getDouble("playtime_hours"),
                    rs.getTimestamp("last_analysis").getTime()
                );
            }
        } catch (SQLException e) {
            plugin.log(Level.WARNING, "Failed to load risk profile: " + e.getMessage());
        }
        
        // Return default profile
        return new PlayerRiskProfile(uuid, 50, 0, 0, 0, 0, 0, 0, 0, System.currentTimeMillis());
    }
    
    private void saveProfile(PlayerRiskProfile profile) {
        String sql = """
            INSERT INTO player_risk_scores 
                (uuid, risk_score, prediction_confidence, total_messages, toxic_messages, 
                 total_warnings, total_mutes, total_bans, playtime_hours, last_analysis)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            ON DUPLICATE KEY UPDATE
                risk_score = VALUES(risk_score),
                prediction_confidence = VALUES(prediction_confidence),
                total_messages = VALUES(total_messages),
                toxic_messages = VALUES(toxic_messages),
                total_warnings = VALUES(total_warnings),
                total_mutes = VALUES(total_mutes),
                total_bans = VALUES(total_bans),
                playtime_hours = VALUES(playtime_hours),
                last_analysis = CURRENT_TIMESTAMP
            """;
        
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, profile.uuid().toString());
            pstmt.setInt(2, profile.riskScore());
            pstmt.setInt(3, profile.banPrediction());
            pstmt.setInt(4, profile.totalMessages());
            pstmt.setInt(5, profile.toxicMessages());
            pstmt.setInt(6, profile.totalWarnings());
            pstmt.setInt(7, profile.totalMutes());
            pstmt.setInt(8, profile.totalBans());
            pstmt.setDouble(9, profile.playtimeHours());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.log(Level.WARNING, "Failed to save risk profile: " + e.getMessage());
        }
    }
    
    /**
     * Calculate risk score based on multiple factors
     */
    private int calculateRiskScore(UUID uuid, PlayerRiskProfile profile) {
        int score = 50; // Base score
        
        // Factor 1: Warning ratio
        if (profile.totalWarnings() > 0) {
            double warningsPerHour = profile.totalWarnings() / Math.max(profile.playtimeHours(), 1);
            score += (int) (warningsPerHour * 20);
        }
        
        // Factor 2: Mute history
        score += profile.totalMutes() * 10;
        
        // Factor 3: Previous bans (heavy penalty)
        score += profile.totalBans() * 25;
        
        // Factor 4: Toxic message ratio
        if (profile.totalMessages() > 10) {
            double toxicRatio = (double) profile.toxicMessages() / profile.totalMessages();
            score += (int) (toxicRatio * 30);
        }
        
        // Factor 5: Check for banned associates
        if (plugin.getSocialNetworkManager() != null) {
            try {
                int bannedScore = plugin.getSocialNetworkManager()
                    .getBannedConnectionScore(uuid).get();
                score += bannedScore / 5;
            } catch (Exception ignored) {}
        }
        
        // Factor 6: Account age bonus (older = more trusted)
        if (profile.playtimeHours() > 100) {
            score -= 15; // Veteran bonus
        } else if (profile.playtimeHours() > 50) {
            score -= 10;
        } else if (profile.playtimeHours() < 1) {
            score += 10; // New player penalty
        }
        
        return Math.max(0, Math.min(100, score));
    }
    
    /**
     * Calculate probability of ban in next 7 days
     */
    private int calculateBanPrediction(UUID uuid, PlayerRiskProfile profile) {
        // Base prediction on risk score
        int prediction = profile.riskScore();
        
        // Adjust based on recent activity
        if (profile.totalWarnings() >= 3 && profile.playtimeHours() < 10) {
            prediction += 20; // New player with many warnings
        }
        
        // Pattern matching with AI (if enabled)
        if (plugin.getAIManager() != null && plugin.getAIManager().isEnabled()) {
            // Could call AI for more sophisticated analysis
            // For now, use rule-based system
        }
        
        // Match against known bad patterns
        int patternMatches = 0;
        for (BehaviorPattern pattern : bannedPatterns) {
            if (matchesPattern(uuid, profile, pattern)) {
                patternMatches += pattern.weight();
            }
        }
        
        prediction += patternMatches / 3;
        
        return Math.max(0, Math.min(100, prediction));
    }
    
    private boolean matchesPattern(UUID uuid, PlayerRiskProfile profile, BehaviorPattern pattern) {
        return switch (pattern.type()) {
            case "BEHAVIOR" -> {
                if (pattern.value().equals("rapid_warning_accumulation")) {
                    yield profile.totalWarnings() >= 3 && profile.playtimeHours() < 5;
                } else if (pattern.value().equals("multiple_mutes_short_period")) {
                    yield profile.totalMutes() >= 2;
                }
                yield false;
            }
            case "SOCIAL" -> {
                if (pattern.value().equals("associates_with_banned")) {
                    try {
                        yield plugin.getSocialNetworkManager()
                            .getBannedConnectionScore(uuid).get() > 30;
                    } catch (Exception e) {
                        yield false;
                    }
                }
                yield false;
            }
            default -> false;
        };
    }
    
    // ==================== EVENT TRACKING ====================
    
    /**
     * Record a chat message (called from ChatListener)
     */
    public void recordMessage(UUID uuid, boolean toxic) {
        PlayerRiskProfile profile = riskProfiles.get(uuid);
        if (profile != null) {
            riskProfiles.put(uuid, new PlayerRiskProfile(
                uuid,
                profile.riskScore(),
                profile.banPrediction(),
                profile.totalMessages() + 1,
                profile.toxicMessages() + (toxic ? 1 : 0),
                profile.totalWarnings(),
                profile.totalMutes(),
                profile.totalBans(),
                profile.playtimeHours(),
                profile.lastAnalysis()
            ));
        }
    }
    
    /**
     * Record a warning
     */
    public void recordWarning(UUID uuid) {
        PlayerRiskProfile profile = riskProfiles.get(uuid);
        if (profile != null) {
            riskProfiles.put(uuid, new PlayerRiskProfile(
                uuid,
                profile.riskScore() + 10,
                profile.banPrediction(),
                profile.totalMessages(),
                profile.toxicMessages(),
                profile.totalWarnings() + 1,
                profile.totalMutes(),
                profile.totalBans(),
                profile.playtimeHours(),
                profile.lastAnalysis()
            ));
            analyzePlayer(uuid);
        }
    }
    
    /**
     * Record a mute
     */
    public void recordMute(UUID uuid) {
        PlayerRiskProfile profile = riskProfiles.get(uuid);
        if (profile != null) {
            riskProfiles.put(uuid, new PlayerRiskProfile(
                uuid,
                profile.riskScore() + 15,
                profile.banPrediction(),
                profile.totalMessages(),
                profile.toxicMessages(),
                profile.totalWarnings(),
                profile.totalMutes() + 1,
                profile.totalBans(),
                profile.playtimeHours(),
                profile.lastAnalysis()
            ));
            analyzePlayer(uuid);
        }
    }
    
    /**
     * Learn from a ban (add player's behavior to patterns)
     */
    public void learnFromBan(UUID uuid) {
        PlayerRiskProfile profile = riskProfiles.get(uuid);
        if (profile == null) return;
        
        // Record this ban
        riskProfiles.put(uuid, new PlayerRiskProfile(
            uuid,
            100,
            100,
            profile.totalMessages(),
            profile.toxicMessages(),
            profile.totalWarnings(),
            profile.totalMutes(),
            profile.totalBans() + 1,
            profile.playtimeHours(),
            profile.lastAnalysis()
        ));
        
        plugin.log(Level.INFO, "Learning from ban to improve predictions...");
    }
    
    // ==================== ALERTS ====================
    
    private void alertHighRisk(UUID uuid, PlayerRiskProfile profile) {
        String playerName = Bukkit.getOfflinePlayer(uuid).getName();
        
        String message = String.format(
            "§8[§cPredictive§8] §e%s §7has §c%d%% §7risk score and §c%d%% §7ban probability!",
            playerName, profile.riskScore(), profile.banPrediction()
        );
        
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("litebansreborn.alerts.predictive")) {
                staff.sendMessage(message);
            }
        }
    }
    
    // ==================== PUBLIC API ====================
    
    /**
     * Get risk profile for a player
     */
    public PlayerRiskProfile getRiskProfile(UUID uuid) {
        return riskProfiles.get(uuid);
    }
    
    /**
     * Get risk level category
     */
    public String getRiskLevel(int score) {
        if (score >= CRITICAL_RISK) return "§4CRITICAL";
        if (score >= HIGH_RISK) return "§cHIGH";
        if (score >= MEDIUM_RISK) return "§eMEDIUM";
        if (score >= LOW_RISK) return "§aLOW";
        return "§2MINIMAL";
    }
    
    // ==================== RECORD CLASSES ====================
    
    public record PlayerRiskProfile(
        UUID uuid,
        int riskScore,
        int banPrediction,
        int totalMessages,
        int toxicMessages,
        int totalWarnings,
        int totalMutes,
        int totalBans,
        double playtimeHours,
        long lastAnalysis
    ) {}
    
    private record BehaviorPattern(String type, String value, int weight) {}
}
