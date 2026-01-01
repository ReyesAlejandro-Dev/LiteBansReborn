package com.nuvik.litebansreborn.web;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Analytics Handler - Provides data for charts, heatmaps, and statistics
 */
public class AnalyticsHandler {
    
    private final LiteBansReborn plugin;
    
    public AnalyticsHandler(LiteBansReborn plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Get punishment heatmap data (hour of day distribution)
     */
    public CompletableFuture<Map<String, Object>> getActivityHeatmap() {
        return plugin.getDatabaseManager().queryAsync(conn -> {
            Map<String, Object> result = new HashMap<>();
            
            // Get punishments by hour of day
            // Get punishments by hour of day
            // Get punishments by hour of day
            com.nuvik.litebansreborn.database.DatabaseManager.DatabaseType dbType = plugin.getDatabaseManager().getDatabaseType();
            String sql;
            boolean useParameter = true;

            if (dbType == com.nuvik.litebansreborn.database.DatabaseManager.DatabaseType.SQLITE) {
                sql = "SELECT strftime('%H', created_at) as h, COUNT(*) as count " +
                      "FROM " + plugin.getDatabaseManager().getTable("punishments") +
                      " WHERE created_at > datetime('now', '-30 days') GROUP BY strftime('%H', created_at)";
                useParameter = false;
            } else if (dbType == com.nuvik.litebansreborn.database.DatabaseManager.DatabaseType.H2) {
                // Use 'h' instead of 'hour' because 'hour' is a reserved word in H2
                sql = "SELECT HOUR(created_at) as h, COUNT(*) as count " +
                      "FROM " + plugin.getDatabaseManager().getTable("punishments") +
                      " WHERE created_at > ? GROUP BY HOUR(created_at) ORDER BY h";
            } else {
                sql = "SELECT EXTRACT(HOUR FROM created_at) as h, COUNT(*) as count " +
                      "FROM " + plugin.getDatabaseManager().getTable("punishments") +
                      " WHERE created_at > ? GROUP BY EXTRACT(HOUR FROM created_at) ORDER BY h";
            }
            
            int[] hourlyData = new int[24];
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                // Last 30 days
                if (useParameter) {
                    stmt.setTimestamp(1, Timestamp.from(Instant.now().minus(30, ChronoUnit.DAYS)));
                }
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        int hourVal;
                        if (dbType == com.nuvik.litebansreborn.database.DatabaseManager.DatabaseType.SQLITE) {
                             hourVal = Integer.parseInt(rs.getString("h"));
                        } else {
                             hourVal = rs.getInt("h");
                        }
                        
                        int count = rs.getInt("count");
                        if (hourVal >= 0 && hourVal < 24) {
                            hourlyData[hourVal] = count;
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load heapmap data: " + e.getMessage());
            }
            
            result.put("hourly", hourlyData);
            result.put("peakHour", findPeakHour(hourlyData));
            
            return result;
        });
    }
    
    /**
     * Get punishment trends over time (daily/weekly/monthly)
     */
    public CompletableFuture<Map<String, Object>> getPunishmentTrends(String period) {
        return plugin.getDatabaseManager().queryAsync(conn -> {
            Map<String, Object> result = new HashMap<>();
            List<Map<String, Object>> data = new ArrayList<>();
            
            int days = switch (period) {
                case "week" -> 7;
                case "month" -> 30;
                case "year" -> 365;
                default -> 7;
            };
            
            // Get daily counts
            String sql = "SELECT DATE(created_at) as day, type, COUNT(*) as count " +
                        "FROM " + plugin.getDatabaseManager().getTable("punishments") +
                        " WHERE created_at > ? GROUP BY DATE(created_at), type ORDER BY day";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setTimestamp(1, Timestamp.from(Instant.now().minus(days, ChronoUnit.DAYS)));
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> entry = new HashMap<>();
                        entry.put("date", rs.getString("day"));
                        entry.put("type", rs.getString("type"));
                        entry.put("count", rs.getInt("count"));
                        data.add(entry);
                    }
                }
            }
            
            result.put("period", period);
            result.put("data", data);
            result.put("totalDays", days);
            
            return result;
        });
    }
    
    /**
     * Get staff leaderboard with statistics
     */
    public CompletableFuture<Map<String, Object>> getStaffLeaderboard() {
        return plugin.getDatabaseManager().queryAsync(conn -> {
            Map<String, Object> result = new HashMap<>();
            List<Map<String, Object>> leaderboard = new ArrayList<>();
            
            String sql = "SELECT executor_name, " +
                        "COUNT(*) as total, " +
                        "SUM(CASE WHEN type = 'ban' OR type = 'tempban' THEN 1 ELSE 0 END) as bans, " +
                        "SUM(CASE WHEN type = 'mute' OR type = 'tempmute' THEN 1 ELSE 0 END) as mutes, " +
                        "SUM(CASE WHEN type = 'warn' THEN 1 ELSE 0 END) as warns, " +
                        "SUM(CASE WHEN type = 'kick' THEN 1 ELSE 0 END) as kicks " +
                        "FROM " + plugin.getDatabaseManager().getTable("punishments") +
                        " WHERE executor_name IS NOT NULL AND executor_name != 'Console' " +
                        "GROUP BY executor_name ORDER BY total DESC LIMIT 20";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                int rank = 1;
                while (rs.next()) {
                    Map<String, Object> staff = new HashMap<>();
                    staff.put("rank", rank++);
                    staff.put("name", rs.getString("executor_name"));
                    staff.put("total", rs.getInt("total"));
                    staff.put("bans", rs.getInt("bans"));
                    staff.put("mutes", rs.getInt("mutes"));
                    staff.put("warns", rs.getInt("warns"));
                    staff.put("kicks", rs.getInt("kicks"));
                    leaderboard.add(staff);
                }
            }
            
            result.put("leaderboard", leaderboard);
            result.put("totalStaff", leaderboard.size());
            
            return result;
        });
    }
    
    /**
     * Get recent events timeline
     */
    public CompletableFuture<Map<String, Object>> getEventsTimeline(int limit) {
        return plugin.getDatabaseManager().queryAsync(conn -> {
            Map<String, Object> result = new HashMap<>();
            List<Map<String, Object>> events = new ArrayList<>();
            
            String sql = "SELECT id, type, target_name, executor_name, reason, created_at, active " +
                        "FROM " + plugin.getDatabaseManager().getTable("punishments") +
                        " ORDER BY created_at DESC LIMIT ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, limit);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> event = new HashMap<>();
                        event.put("id", rs.getLong("id"));
                        event.put("type", rs.getString("type"));
                        event.put("target", rs.getString("target_name"));
                        event.put("executor", rs.getString("executor_name"));
                        event.put("reason", rs.getString("reason"));
                        event.put("timestamp", rs.getTimestamp("created_at").toString());
                        event.put("active", rs.getBoolean("active"));
                        events.add(event);
                    }
                }
            }
            
            result.put("events", events);
            result.put("count", events.size());
            
            return result;
        });
    }
    
    /**
     * Get GeoIP statistics for map visualization
     * Returns two datasets:
     * - bannedCountries: Countries with active IP bans (shown in red)
     * - playerCountries: Countries of normal players (shown in green)
     */
    public CompletableFuture<Map<String, Object>> getGeoStats() {
        return plugin.getDatabaseManager().queryAsync(conn -> {
            Map<String, Object> result = new HashMap<>();
            Map<String, Integer> bannedCountries = new HashMap<>();
            Map<String, Integer> playerCountries = new HashMap<>();
            
            // 1. Get countries from normal players (green markers)
            String playerSql = "SELECT country, COUNT(*) as count FROM " + 
                        plugin.getDatabaseManager().getTable("players") +
                        " WHERE country IS NOT NULL AND country != '' GROUP BY country ORDER BY count DESC";
            
            try (PreparedStatement stmt = conn.prepareStatement(playerSql);
                 ResultSet rs = stmt.executeQuery()) {
                int playerRows = 0;
                while (rs.next()) {
                    String country = rs.getString("country");
                    if (country != null && !country.isEmpty()) {
                        playerCountries.put(country, rs.getInt("count"));
                        playerRows++;
                    }
                }
                plugin.debug("GeoStats: Found " + playerRows + " countries with players");
            } catch (Exception e) {
                plugin.getLogger().warning("Error fetching player geo stats: " + e.getMessage());
            }
            
            // 2. Get IPs from active IP bans and resolve countries (red markers)
            String ipBanSql = "SELECT DISTINCT target_ip FROM " + 
                              plugin.getDatabaseManager().getTable("punishments") +
                              " WHERE (type = 'IPBAN' OR ip_based = true) AND active = true AND target_ip IS NOT NULL";
            
            try (PreparedStatement stmt = conn.prepareStatement(ipBanSql);
                 ResultSet rs = stmt.executeQuery()) {
                
                // Collect all banned IPs
                java.util.List<String> bannedIPs = new java.util.ArrayList<>();
                while (rs.next()) {
                    String ip = rs.getString("target_ip");
                    if (ip != null && !ip.isEmpty()) {
                        bannedIPs.add(ip);
                    }
                }
                
                plugin.debug("GeoStats: Found " + bannedIPs.size() + " banned IPs to resolve");
                
                // Now resolve each IP to country
                for (String ip : bannedIPs) {
                    String country = resolveIPToCountry(conn, ip);
                    plugin.debug("GeoStats: Resolved IP " + ip + " -> " + country);
                    if (country != null && !country.isEmpty()) {
                        bannedCountries.merge(country, 1, Integer::sum);
                    } else {
                        bannedCountries.merge("Unknown", 1, Integer::sum);
                    }
                }
                
            } catch (Exception e) {
                plugin.getLogger().warning("Error fetching IP ban geo stats: " + e.getMessage());
            }
            
            result.put("bannedCountries", bannedCountries);
            result.put("playerCountries", playerCountries);
            result.put("totalBanned", bannedCountries.values().stream().mapToInt(Integer::intValue).sum());
            result.put("totalPlayers", playerCountries.values().stream().mapToInt(Integer::intValue).sum());
            
            plugin.debug("GeoStats: Total players=" + result.get("totalPlayers") + ", banned=" + result.get("totalBanned"));
            
            return result;
        });
    }
    
    /**
     * Resolve an IP address to a country name
     * Uses cached data first, then VPN service if available
     */
    private String resolveIPToCountry(Connection conn, String ip) {
        // First check if we have this IP's country cached in player_ips or similar
        try {
            // Check if player with this IP has country stored
            String sql = "SELECT country FROM " + plugin.getDatabaseManager().getTable("players") +
                        " WHERE last_known_ip = ? AND country IS NOT NULL LIMIT 1";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, ip);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String country = rs.getString("country");
                        if (country != null && !country.isEmpty()) {
                            return country;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignore, try next method
        }
        
        // Use GeoIPManager if available (works independently of Anti-VPN)
        if (plugin.getGeoIPManager() != null) {
            try {
                var result = plugin.getGeoIPManager().lookup(ip).get(2, java.util.concurrent.TimeUnit.SECONDS);
                if (result != null && result.getCountry() != null) {
                    return result.getCountry();
                }
            } catch (Exception e) {
                // Timeout or error, skip
            }
        }
        
        return null;
    }
    
    /**
     * Get player risk assessment
     */
    public CompletableFuture<Map<String, Object>> getPlayerRisk(String playerName) {
        return plugin.getDatabaseManager().queryAsync(conn -> {
            Map<String, Object> result = new HashMap<>();
            
            // Count past punishments
            String sql = "SELECT type, COUNT(*) as count FROM " + 
                        plugin.getDatabaseManager().getTable("punishments") +
                        " WHERE target_name = ? GROUP BY type";
            
            int totalPunishments = 0;
            int bans = 0, mutes = 0, warns = 0, kicks = 0;
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerName);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String type = rs.getString("type");
                        int count = rs.getInt("count");
                        totalPunishments += count;
                        
                        switch (type.toLowerCase()) {
                            case "ban", "tempban" -> bans += count;
                            case "mute", "tempmute" -> mutes += count;
                            case "warn" -> warns += count;
                            case "kick" -> kicks += count;
                        }
                    }
                }
            }
            
            // Calculate risk score (0-100)
            int riskScore = Math.min(100, (bans * 30) + (mutes * 15) + (warns * 10) + (kicks * 5));
            String riskLevel = riskScore > 70 ? "HIGH" : riskScore > 40 ? "MEDIUM" : "LOW";
            
            result.put("player", playerName);
            result.put("riskScore", riskScore);
            result.put("riskLevel", riskLevel);
            result.put("totalPunishments", totalPunishments);
            result.put("bans", bans);
            result.put("mutes", mutes);
            result.put("warns", warns);
            result.put("kicks", kicks);
            
            // Suggest ban duration based on history
            String suggestedDuration = switch (riskLevel) {
                case "HIGH" -> "permanent";
                case "MEDIUM" -> "7d";
                default -> "1d";
            };
            result.put("suggestedDuration", suggestedDuration);
            
            return result;
        });
    }
    
    private int findPeakHour(int[] hourlyData) {
        int peak = 0;
        int peakHour = 0;
        for (int i = 0; i < hourlyData.length; i++) {
            if (hourlyData[i] > peak) {
                peak = hourlyData[i];
                peakHour = i;
            }
        }
        return peakHour;
    }
}
