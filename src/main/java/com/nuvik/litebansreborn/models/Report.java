package com.nuvik.litebansreborn.models;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a report submitted by a player
 */
public class Report {
    
    private long id;
    private final UUID reporterUUID;
    private final String reporterName;
    private final UUID reportedUUID;
    private final String reportedName;
    private final String reason;
    private final String category;
    private final String server;
    private final Instant createdAt;
    private ReportStatus status;
    private Instant handledAt;
    private UUID handledByUUID;
    private String handledByName;
    private String resolution;
    
    /**
     * Create a new report
     */
    public Report(UUID reporterUUID, String reporterName, UUID reportedUUID, String reportedName,
                  String reason, String category, String server) {
        this.reporterUUID = reporterUUID;
        this.reporterName = reporterName;
        this.reportedUUID = reportedUUID;
        this.reportedName = reportedName;
        this.reason = reason;
        this.category = category;
        this.server = server;
        this.createdAt = Instant.now();
        this.status = ReportStatus.PENDING;
    }
    
    /**
     * Load an existing report from database
     */
    public Report(long id, UUID reporterUUID, String reporterName, UUID reportedUUID, String reportedName,
                  String reason, String category, String server, Instant createdAt, ReportStatus status,
                  Instant handledAt, UUID handledByUUID, String handledByName, String resolution) {
        this.id = id;
        this.reporterUUID = reporterUUID;
        this.reporterName = reporterName;
        this.reportedUUID = reportedUUID;
        this.reportedName = reportedName;
        this.reason = reason;
        this.category = category;
        this.server = server;
        this.createdAt = createdAt;
        this.status = status;
        this.handledAt = handledAt;
        this.handledByUUID = handledByUUID;
        this.handledByName = handledByName;
        this.resolution = resolution;
    }
    
    /**
     * Handle the report
     */
    public void handle(UUID handledByUUID, String handledByName, ReportStatus status, String resolution) {
        this.handledByUUID = handledByUUID;
        this.handledByName = handledByName;
        this.status = status;
        this.resolution = resolution;
        this.handledAt = Instant.now();
    }
    
    // Getters and Setters
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public UUID getReporterUUID() {
        return reporterUUID;
    }
    
    public String getReporterName() {
        return reporterName;
    }
    
    public UUID getReportedUUID() {
        return reportedUUID;
    }
    
    public String getReportedName() {
        return reportedName;
    }
    
    public String getReason() {
        return reason;
    }
    
    public String getCategory() {
        return category;
    }
    
    public String getServer() {
        return server;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public ReportStatus getStatus() {
        return status;
    }
    
    public void setStatus(ReportStatus status) {
        this.status = status;
    }
    
    public Instant getHandledAt() {
        return handledAt;
    }
    
    public UUID getHandledByUUID() {
        return handledByUUID;
    }
    
    public String getHandledByName() {
        return handledByName;
    }
    
    public String getResolution() {
        return resolution;
    }
    
    /**
     * Report status enum
     */
    public enum ReportStatus {
        PENDING("pending", "§ePending"),
        ACCEPTED("accepted", "§aAccepted"),
        DENIED("denied", "§cDenied"),
        RESOLVED("resolved", "§bResolved");
        
        private final String id;
        private final String display;
        
        ReportStatus(String id, String display) {
            this.id = id;
            this.display = display;
        }
        
        public String getId() {
            return id;
        }
        
        public String getDisplay() {
            return display;
        }
        
        public static ReportStatus fromId(String id) {
            for (ReportStatus status : values()) {
                if (status.id.equalsIgnoreCase(id)) {
                    return status;
                }
            }
            return PENDING;
        }
    }
}
