package com.nuvik.litebansreborn.managers;

import com.nuvik.litebansreborn.LiteBansReborn;

import java.sql.*;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Point Manager - Handles punishment point system
 */
public class PointManager {

    private final LiteBansReborn plugin;
    
    public PointManager(LiteBansReborn plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Add points to a player
     */
    public CompletableFuture<Double> addPoints(UUID uuid, double points) {
        return plugin.getDatabaseManager().queryAsync(conn -> {
            // First get current points
            double current = getPointsSync(conn, uuid);
            double newTotal = current + points;
            
            // Update points
            String sql = "UPDATE " + plugin.getDatabaseManager().getTable("players") +
                    " SET punishment_points = ? WHERE uuid = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setDouble(1, newTotal);
                stmt.setString(2, uuid.toString());
                stmt.executeUpdate();
            }
            
            return newTotal;
        }).thenCompose(total -> {
            // Check thresholds
            checkThresholds(uuid, total);
            return CompletableFuture.completedFuture(total);
        });
    }
    
    /**
     * Remove points from a player
     */
    public CompletableFuture<Double> removePoints(UUID uuid, double points) {
        return plugin.getDatabaseManager().queryAsync(conn -> {
            double current = getPointsSync(conn, uuid);
            double newTotal = Math.max(0, current - points);
            
            String sql = "UPDATE " + plugin.getDatabaseManager().getTable("players") +
                    " SET punishment_points = ? WHERE uuid = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setDouble(1, newTotal);
                stmt.setString(2, uuid.toString());
                stmt.executeUpdate();
            }
            
            return newTotal;
        });
    }
    
    /**
     * Get points for a player
     */
    public CompletableFuture<Double> getPoints(UUID uuid) {
        return plugin.getDatabaseManager().queryAsync(conn -> getPointsSync(conn, uuid));
    }
    
    /**
     * Get points synchronously (with connection)
     */
    private double getPointsSync(Connection conn, UUID uuid) throws SQLException {
        String sql = "SELECT punishment_points FROM " + plugin.getDatabaseManager().getTable("players") +
                " WHERE uuid = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("punishment_points");
                }
            }
        }
        
        return 0;
    }
    
    /**
     * Set points for a player
     */
    public CompletableFuture<Void> setPoints(UUID uuid, double points) {
        return plugin.getDatabaseManager().executeAsync(conn -> {
            String sql = "UPDATE " + plugin.getDatabaseManager().getTable("players") +
                    " SET punishment_points = ? WHERE uuid = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setDouble(1, Math.max(0, points));
                stmt.setString(2, uuid.toString());
                stmt.executeUpdate();
            }
        });
    }
    
    /**
     * Decay points for all players (called periodically)
     */
    public void decayPoints() {
        double decayPerDay = plugin.getConfigManager().getDouble("points.decay-per-day", 0.5);
        if (decayPerDay <= 0) return;
        
        plugin.getDatabaseManager().executeAsync(conn -> {
            // Calculate hourly decay (since this runs hourly)
            double hourlyDecay = decayPerDay / 24.0;
            
            String sql = "UPDATE " + plugin.getDatabaseManager().getTable("players") +
                    " SET punishment_points = GREATEST(0, punishment_points - ?) " +
                    "WHERE punishment_points > 0";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setDouble(1, hourlyDecay);
                int affected = stmt.executeUpdate();
                
                if (affected > 0) {
                    plugin.debug("Decayed points for " + affected + " players");
                }
            }
        });
    }
    
    /**
     * Check point thresholds and execute actions
     */
    private void checkThresholds(UUID uuid, double points) {
        if (!plugin.getConfigManager().getBoolean("points.enabled")) return;
        
        var thresholds = plugin.getConfigManager().getConfig()
                .getConfigurationSection("points.thresholds");
        
        if (thresholds == null) return;
        
        for (String key : thresholds.getKeys(false)) {
            int threshold = plugin.getConfigManager().getInt("points.thresholds." + key + ".points");
            
            if (points >= threshold) {
                String action = plugin.getConfigManager().getString("points.thresholds." + key + ".action");
                String duration = plugin.getConfigManager().getString("points.thresholds." + key + ".duration");
                String reason = plugin.getConfigManager().getString("points.thresholds." + key + ".reason");
                
                // Check if already actioned at this threshold
                // For simplicity, we just apply the action
                // In a real implementation, you'd track which thresholds have been triggered
                
                plugin.debug("Player " + uuid + " reached " + threshold + " points, action: " + action);
                
                // The actual action would be executed here
                // This is left as a hook for the BanManager/MuteManager
            }
        }
    }
    
    /**
     * Reset points for a player
     */
    public CompletableFuture<Void> resetPoints(UUID uuid) {
        return setPoints(uuid, 0);
    }
}
