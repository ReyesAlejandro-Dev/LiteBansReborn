package com.nuvik.litebansreborn.models;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a punishment entry in LiteBansReborn
 * This is the base class for all punishment types
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
     * Create a new punishment (no ID yet - for new punishments)
     */
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
     */
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
    
    // Getters and Setters
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
