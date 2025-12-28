package com.nuvik.litebansreborn.managers;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.models.Report;
import com.nuvik.litebansreborn.models.Report.ReportStatus;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Report Manager - Handles player reports
 */
public class ReportManager {

    private final LiteBansReborn plugin;
    
    public ReportManager(LiteBansReborn plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Create a new report
     */
    public CompletableFuture<Report> createReport(UUID reporterUUID, String reporterName,
                                                   UUID reportedUUID, String reportedName,
                                                   String reason, String category) {
        
        Report report = new Report(reporterUUID, reporterName, reportedUUID, reportedName,
                reason, category, plugin.getConfigManager().getServerName());
        
        return plugin.getDatabaseManager().queryAsync(conn -> {
            String sql = "INSERT INTO " + plugin.getDatabaseManager().getTable("reports") +
                    " (reporter_uuid, reporter_name, reported_uuid, reported_name, " +
                    "reason, category, server, created_at, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, reporterUUID.toString());
                stmt.setString(2, reporterName);
                stmt.setString(3, reportedUUID.toString());
                stmt.setString(4, reportedName);
                stmt.setString(5, reason);
                stmt.setString(6, category);
                stmt.setString(7, report.getServer());
                stmt.setTimestamp(8, Timestamp.from(report.getCreatedAt()));
                stmt.setString(9, ReportStatus.PENDING.getId());
                
                stmt.executeUpdate();
                
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        report.setId(rs.getLong(1));
                    }
                }
            }
            
            return report;
        });
    }
    
    /**
     * Get pending reports
     */
    public CompletableFuture<List<Report>> getPendingReports(int page, int perPage) {
        return plugin.getDatabaseManager().queryAsync(conn -> {
            String sql = "SELECT * FROM " + plugin.getDatabaseManager().getTable("reports") +
                    " WHERE status = 'pending' ORDER BY created_at DESC LIMIT ? OFFSET ?";
            
            List<Report> reports = new ArrayList<>();
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, perPage);
                stmt.setInt(2, (page - 1) * perPage);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        reports.add(parseReport(rs));
                    }
                }
            }
            
            return reports;
        });
    }
    
    /**
     * Get a report by ID
     */
    public CompletableFuture<Report> getReport(long id) {
        return plugin.getDatabaseManager().queryAsync(conn -> {
            String sql = "SELECT * FROM " + plugin.getDatabaseManager().getTable("reports") +
                    " WHERE id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, id);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return parseReport(rs);
                    }
                }
            }
            
            return null;
        });
    }
    
    /**
     * Handle a report
     */
    public CompletableFuture<Boolean> handleReport(long id, UUID handledByUUID, String handledByName,
                                                    ReportStatus status, String resolution) {
        return plugin.getDatabaseManager().queryAsync(conn -> {
            String sql = "UPDATE " + plugin.getDatabaseManager().getTable("reports") +
                    " SET status = ?, handled_at = ?, handled_by_uuid = ?, " +
                    "handled_by_name = ?, resolution = ? WHERE id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, status.getId());
                stmt.setTimestamp(2, Timestamp.from(Instant.now()));
                stmt.setString(3, handledByUUID.toString());
                stmt.setString(4, handledByName);
                stmt.setString(5, resolution);
                stmt.setLong(6, id);
                
                return stmt.executeUpdate() > 0;
            }
        });
    }
    
    /**
     * Check if player has recently reported another player
     */
    public CompletableFuture<Boolean> hasRecentlyReported(UUID reporterUUID, UUID reportedUUID) {
        return plugin.getDatabaseManager().queryAsync(conn -> {
            int cooldownSeconds = plugin.getConfigManager().getInt("reports.cooldown", 60);
            
            String sql = "SELECT id FROM " + plugin.getDatabaseManager().getTable("reports") +
                    " WHERE reporter_uuid = ? AND reported_uuid = ? AND created_at > ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, reporterUUID.toString());
                stmt.setString(2, reportedUUID.toString());
                stmt.setTimestamp(3, Timestamp.from(Instant.now().minusSeconds(cooldownSeconds)));
                
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next();
                }
            }
        });
    }
    
    /**
     * Get daily report count for a player
     */
    public CompletableFuture<Integer> getDailyReportCount(UUID reporterUUID) {
        return plugin.getDatabaseManager().queryAsync(conn -> {
            String sql = "SELECT COUNT(*) FROM " + plugin.getDatabaseManager().getTable("reports") +
                    " WHERE reporter_uuid = ? AND created_at > ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, reporterUUID.toString());
                stmt.setTimestamp(2, Timestamp.from(Instant.now().minusSeconds(86400)));
                
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
     * Get pending reports count
     */
    public CompletableFuture<Integer> getPendingReportsCount() {
        return plugin.getDatabaseManager().queryAsync(conn -> {
            String sql = "SELECT COUNT(*) FROM " + plugin.getDatabaseManager().getTable("reports") +
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
    
    private Report parseReport(ResultSet rs) throws SQLException {
        return new Report(
            rs.getLong("id"),
            UUID.fromString(rs.getString("reporter_uuid")),
            rs.getString("reporter_name"),
            UUID.fromString(rs.getString("reported_uuid")),
            rs.getString("reported_name"),
            rs.getString("reason"),
            rs.getString("category"),
            rs.getString("server"),
            rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toInstant() : null,
            ReportStatus.fromId(rs.getString("status")),
            rs.getTimestamp("handled_at") != null ? rs.getTimestamp("handled_at").toInstant() : null,
            rs.getString("handled_by_uuid") != null ? UUID.fromString(rs.getString("handled_by_uuid")) : null,
            rs.getString("handled_by_name"),
            rs.getString("resolution")
        );
    }
}
