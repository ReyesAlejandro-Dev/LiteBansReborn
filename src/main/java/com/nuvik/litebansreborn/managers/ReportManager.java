package com.nuvik.litebansreborn.managers;

import com.nuvik.litebansreborn.LiteBansReborn;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ReportManager {

    private final LiteBansReborn plugin;

    public ReportManager(LiteBansReborn plugin) {
        this.plugin = plugin;
    }

    /**
     * Create a new report
     */
    public CompletableFuture<Void> createReport(Player reporter, Player reported, String reason) {
        return plugin.getDatabaseManager().executeAsync(conn -> {
            String sql = "INSERT INTO " + plugin.getDatabaseManager().getTable("reports") + 
                         " (reporter_uuid, reporter_name, reported_uuid, reported_name, reason, server, status) VALUES (?, ?, ?, ?, ?, ?, 'pending')";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, reporter.getUniqueId().toString());
                stmt.setString(2, reporter.getName());
                stmt.setString(3, reported.getUniqueId().toString());
                stmt.setString(4, reported.getName());
                stmt.setString(5, reason);
                stmt.setString(6, Bukkit.getServer().getName()); // Simple server name
                
                stmt.executeUpdate();
            }
        });
    }

    /**
     * Get pending reports
     */
    public CompletableFuture<List<Report>> getPendingReports(int limit, int offset) {
        return plugin.getDatabaseManager().queryAsync(conn -> {
            String sql = "SELECT * FROM " + plugin.getDatabaseManager().getTable("reports") + 
                         " WHERE status = 'pending' ORDER BY created_at DESC LIMIT ? OFFSET ?";
            
            List<Report> reports = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, limit);
                stmt.setInt(2, offset);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        reports.add(mapReport(rs));
                    }
                }
            }
            return reports;
        });
    }
    
    /**
     * Get report by ID
     */
    public CompletableFuture<Report> getReport(int id) {
        return plugin.getDatabaseManager().queryAsync(conn -> {
            String sql = "SELECT * FROM " + plugin.getDatabaseManager().getTable("reports") + " WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return mapReport(rs);
                    }
                }
            }
            return null;
        });
    }

    /**
     * Handle a report (Activity/Deny/Resolve)
     */
    public CompletableFuture<Void> handleReport(int reportId, String status, UUID handlerUuid, String handlerName, String resolution) {
        return plugin.getDatabaseManager().executeAsync(conn -> {
            String sql = "UPDATE " + plugin.getDatabaseManager().getTable("reports") + 
                         " SET status = ?, handled_by_uuid = ?, handled_by_name = ?, resolution = ?, handled_at = CURRENT_TIMESTAMP WHERE id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, status);
                stmt.setString(2, handlerUuid.toString());
                stmt.setString(3, handlerName);
                stmt.setString(4, resolution);
                stmt.setInt(5, reportId);
                
                stmt.executeUpdate();
            }
        });
    }

    /**
     * Get count of pending reports
     */
    public CompletableFuture<Integer> getPendingReportsCount() {
        return plugin.getDatabaseManager().queryAsync(conn -> {
            String sql = "SELECT COUNT(*) FROM " + plugin.getDatabaseManager().getTable("reports") + " WHERE status = 'pending'";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
            return 0;
        });
    }

    private Report mapReport(ResultSet rs) throws SQLException {
        return new Report(
            rs.getInt("id"),
            UUID.fromString(rs.getString("reporter_uuid")),
            rs.getString("reporter_name"),
            UUID.fromString(rs.getString("reported_uuid")),
            rs.getString("reported_name"),
            rs.getString("reason"),
            rs.getString("created_at"), // Simplification, handle timestamp properly if needed
            rs.getString("status")
        );
    }
    
    public static class Report {
        public final int id;
        public final UUID reporterUuid;
        public final String reporterName;
        public final UUID reportedUuid;
        public final String reportedName;
        public final String reason;
        public final String date;
        public final String status;
        
        public Report(int id, UUID reporterUuid, String reporterName, UUID reportedUuid, String reportedName, String reason, String date, String status) {
            this.id = id;
            this.reporterUuid = reporterUuid;
            this.reporterName = reporterName;
            this.reportedUuid = reportedUuid;
            this.reportedName = reportedName;
            this.reason = reason;
            this.date = date;
            this.status = status;
        }
    }
}
