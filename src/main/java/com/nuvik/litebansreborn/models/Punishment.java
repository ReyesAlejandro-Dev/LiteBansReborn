package com.nuvik.litebansreborn.models;

import com.nuvik.litebansreborn.utils.PlayerUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents a punishment entry in LiteBansReborn
 * This is the base class for all punishment types
 * 
 * Uses Builder pattern for complex object construction
 * 
 * @author Nuvik
 * @version 6.0.0
 */
public class Punishment {
    
    private long id;
    private final PunishmentType type;
    private final UUID targetUUID;
    private final String targetName;
    private final String targetIP;
    private final UUID executorUUID;
    private final String executorName;
    private final String reason;
    private final String server;
    private final Instant createdAt;
    private final Instant expiresAt;
    private boolean active;
    private Instant removedAt;
    private UUID removedByUUID;
    private String removedByName;
    private String removeReason;
    private boolean silent;
    private boolean ipBased;
    
    /**
     * Private constructor - use Builder instead
     */
    private Punishment(Builder builder) {
        this.id = builder.id;
        this.type = builder.type;
        this.targetUUID = builder.targetUUID;
        this.targetName = builder.targetName;
        this.targetIP = builder.targetIP;
        this.executorUUID = builder.executorUUID;
        this.executorName = builder.executorName;
        this.reason = builder.reason;
        this.server = builder.server;
        this.createdAt = builder.createdAt;
        this.expiresAt = builder.expiresAt;
        this.active = builder.active;
        this.removedAt = builder.removedAt;
        this.removedByUUID = builder.removedByUUID;
        this.removedByName = builder.removedByName;
        this.removeReason = builder.removeReason;
        this.silent = builder.silent;
        this.ipBased = builder.ipBased;
    }
    
    /**
     * Create a new punishment (no ID yet - for new punishments)
     * @deprecated Use Builder instead
     */
    @Deprecated
    public Punishment(PunishmentType type, UUID targetUUID, String targetName, String targetIP,
                     UUID executorUUID, String executorName, String reason, String server,
                     Instant expiresAt, boolean silent, boolean ipBased) {
        this.type = type;
        this.targetUUID = targetUUID;
        this.targetName = targetName;
        this.targetIP = targetIP;
        this.executorUUID = executorUUID;
        this.executorName = executorName;
        this.reason = reason;
        this.server = server;
        this.createdAt = Instant.now();
        this.expiresAt = expiresAt;
        this.active = true;
        this.silent = silent;
        this.ipBased = ipBased;
    }
    
    /**
     * Load an existing punishment from database
     * @deprecated Use Builder or fromResultSet instead
     */
    @Deprecated
    public Punishment(long id, PunishmentType type, UUID targetUUID, String targetName, String targetIP,
                     UUID executorUUID, String executorName, String reason, String server,
                     Instant createdAt, Instant expiresAt, boolean active,
                     Instant removedAt, UUID removedByUUID, String removedByName, String removeReason,
                     boolean silent, boolean ipBased) {
        this.id = id;
        this.type = type;
        this.targetUUID = targetUUID;
        this.targetName = targetName;
        this.targetIP = targetIP;
        this.executorUUID = executorUUID;
        this.executorName = executorName;
        this.reason = reason;
        this.server = server;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.active = active;
        this.removedAt = removedAt;
        this.removedByUUID = removedByUUID;
        this.removedByName = removedByName;
        this.removeReason = removeReason;
        this.silent = silent;
        this.ipBased = ipBased;
    }
    
    // ==================== BUILDER PATTERN ====================
    
    /**
     * Builder for creating Punishment instances
     */
    public static class Builder {
        // Required fields
        private final PunishmentType type;
        private final String targetName;
        
        // Optional fields with defaults
        private long id = 0;
        private UUID targetUUID;
        private String targetIP;
        private UUID executorUUID = PlayerUtil.CONSOLE_UUID;
        private String executorName = PlayerUtil.CONSOLE_NAME;
        private String reason = "No reason specified";
        private String server = "server";
        private Instant createdAt = Instant.now();
        private Instant expiresAt;
        private boolean active = true;
        private Instant removedAt;
        private UUID removedByUUID;
        private String removedByName;
        private String removeReason;
        private boolean silent = false;
        private boolean ipBased = false;
        
        /**
         * Create a new builder with required fields
         */
        public Builder(PunishmentType type, String targetName) {
            this.type = type;
            this.targetName = targetName;
        }
        
        public Builder id(long id) {
            this.id = id;
            return this;
        }
        
        public Builder targetUUID(UUID uuid) {
            this.targetUUID = uuid;
            return this;
        }
        
        public Builder targetIP(String ip) {
            this.targetIP = ip;
            return this;
        }
        
        public Builder executor(UUID uuid, String name) {
            this.executorUUID = uuid != null ? uuid : PlayerUtil.CONSOLE_UUID;
            this.executorName = name != null ? name : PlayerUtil.CONSOLE_NAME;
            return this;
        }
        
        public Builder executorUUID(UUID uuid) {
            this.executorUUID = uuid != null ? uuid : PlayerUtil.CONSOLE_UUID;
            return this;
        }
        
        public Builder executorName(String name) {
            this.executorName = name != null ? name : PlayerUtil.CONSOLE_NAME;
            return this;
        }
        
        public Builder reason(String reason) {
            this.reason = reason != null ? reason : "No reason specified";
            return this;
        }
        
        public Builder server(String server) {
            this.server = server != null ? server : "server";
            return this;
        }
        
        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt != null ? createdAt : Instant.now();
            return this;
        }
        
        public Builder expiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }
        
        public Builder permanent() {
            this.expiresAt = null;
            return this;
        }
        
        public Builder duration(long durationMillis) {
            if (durationMillis < 0) {
                this.expiresAt = null; // Permanent
            } else {
                this.expiresAt = Instant.now().plusMillis(durationMillis);
            }
            return this;
        }
        
        public Builder active(boolean active) {
            this.active = active;
            return this;
        }
        
        public Builder removed(Instant removedAt, UUID removedByUUID, String removedByName, String removeReason) {
            this.removedAt = removedAt;
            this.removedByUUID = removedByUUID;
            this.removedByName = removedByName;
            this.removeReason = removeReason;
            return this;
        }
        
        public Builder silent(boolean silent) {
            this.silent = silent;
            return this;
        }
        
        public Builder ipBased(boolean ipBased) {
            this.ipBased = ipBased;
            return this;
        }
        
        /**
         * Build the Punishment instance
         */
        public Punishment build() {
            return new Punishment(this);
        }
    }
    
    /**
     * Create a new builder
     */
    public static Builder builder(PunishmentType type, String targetName) {
        return new Builder(type, targetName);
    }
    
    /**
     * Parse a Punishment from a ResultSet
     * Centralizes database parsing logic to avoid code duplication
     * 
     * @param rs The ResultSet to parse from
     * @return A Punishment object
     * @throws SQLException If there's an error reading from the ResultSet
     */
    public static Punishment fromResultSet(ResultSet rs) throws SQLException {
        // Parse UUIDs safely, handling null values
        String targetUuidStr = rs.getString("target_uuid");
        UUID targetUUID = targetUuidStr != null ? UUID.fromString(targetUuidStr) : null;
        
        String executorUuidStr = rs.getString("executor_uuid");
        UUID executorUUID = executorUuidStr != null ? UUID.fromString(executorUuidStr) : PlayerUtil.CONSOLE_UUID;
        
        String removedByUuidStr = rs.getString("removed_by_uuid");
        UUID removedByUUID = removedByUuidStr != null ? UUID.fromString(removedByUuidStr) : null;
        
        // Parse timestamps safely
        Timestamp createdAtTs = rs.getTimestamp("created_at");
        Instant createdAt = createdAtTs != null ? createdAtTs.toInstant() : null;
        
        Timestamp expiresAtTs = rs.getTimestamp("expires_at");
        Instant expiresAt = expiresAtTs != null ? expiresAtTs.toInstant() : null;
        
        Timestamp removedAtTs = rs.getTimestamp("removed_at");
        Instant removedAt = removedAtTs != null ? removedAtTs.toInstant() : null;
        
        // Get punishment type
        PunishmentType type = PunishmentType.fromId(rs.getString("type"));
        if (type == null) {
            type = PunishmentType.BAN; // Default fallback
        }
        
        return new Builder(type, rs.getString("target_name"))
            .id(rs.getLong("id"))
            .targetUUID(targetUUID)
            .targetIP(rs.getString("target_ip"))
            .executorUUID(executorUUID)
            .executorName(rs.getString("executor_name"))
            .reason(rs.getString("reason"))
            .server(rs.getString("server"))
            .createdAt(createdAt)
            .expiresAt(expiresAt)
            .active(rs.getBoolean("active"))
            .removed(removedAt, removedByUUID, rs.getString("removed_by_name"), rs.getString("remove_reason"))
            .silent(rs.getBoolean("silent"))
            .ipBased(rs.getBoolean("ip_based"))
            .build();
    }
    
    // ==================== BUSINESS LOGIC ====================
    
    /**
     * Check if the punishment is permanent
     */
    public boolean isPermanent() {
        return expiresAt == null;
    }
    
    /**
     * Check if the punishment has expired
     */
    public boolean hasExpired() {
        if (isPermanent()) {
            return false;
        }
        return Instant.now().isAfter(expiresAt);
    }
    
    /**
     * Check if the punishment is currently active (not expired and not removed)
     */
    public boolean isActiveAndValid() {
        return active && !hasExpired();
    }
    
    /**
     * Get remaining time in milliseconds
     */
    public long getRemainingTime() {
        if (isPermanent()) {
            return -1;
        }
        long remaining = expiresAt.toEpochMilli() - Instant.now().toEpochMilli();
        return Math.max(0, remaining);
    }
    
    /**
     * Remove this punishment
     */
    public void remove(UUID removedByUUID, String removedByName, String reason) {
        this.active = false;
        this.removedAt = Instant.now();
        this.removedByUUID = removedByUUID;
        this.removedByName = removedByName;
        this.removeReason = reason;
    }
    
    // ==================== GETTERS AND SETTERS ====================
    
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public PunishmentType getType() {
        return type;
    }
    
    public UUID getTargetUUID() {
        return targetUUID;
    }
    
    public String getTargetName() {
        return targetName;
    }
    
    public String getTargetIP() {
        return targetIP;
    }
    
    public UUID getExecutorUUID() {
        return executorUUID;
    }
    
    public String getExecutorName() {
        return executorName;
    }
    
    public String getReason() {
        return reason;
    }
    
    public String getServer() {
        return server;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public Instant getExpiresAt() {
        return expiresAt;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public Instant getRemovedAt() {
        return removedAt;
    }
    
    public UUID getRemovedByUUID() {
        return removedByUUID;
    }
    
    public String getRemovedByName() {
        return removedByName;
    }
    
    public String getRemoveReason() {
        return removeReason;
    }
    
    public boolean isSilent() {
        return silent;
    }
    
    public void setSilent(boolean silent) {
        this.silent = silent;
    }
    
    public boolean isIpBased() {
        return ipBased;
    }
    
    public void setIpBased(boolean ipBased) {
        this.ipBased = ipBased;
    }
    
    @Override
    public String toString() {
        return "Punishment{" +
                "id=" + id +
                ", type=" + type +
                ", targetName='" + targetName + '\'' +
                ", executorName='" + executorName + '\'' +
                ", reason='" + reason + '\'' +
                ", active=" + active +
                ", permanent=" + isPermanent() +
                '}';
    }
}
