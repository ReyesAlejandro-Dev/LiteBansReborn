package com.nuvik.litebansreborn.models;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents an appeal submitted by a player for a punishment
 */
public class Appeal {
    
    private long id;
    private final long punishmentId;
    private final PunishmentType punishmentType;
    private final UUID playerUUID;
    private final String playerName;
    private final String message;
    private final Instant createdAt;
    private AppealStatus status;
    private Instant handledAt;
    private UUID handledByUUID;
    private String handledByName;
    private String response;
    
    /**
     * Create a new appeal
     */
    public Appeal(long punishmentId, PunishmentType punishmentType, UUID playerUUID, 
                  String playerName, String message) {
        this.punishmentId = punishmentId;
        this.punishmentType = punishmentType;
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.message = message;
        this.createdAt = Instant.now();
        this.status = AppealStatus.PENDING;
    }
    
    /**
     * Load an existing appeal from database
     */
    public Appeal(long id, long punishmentId, PunishmentType punishmentType, UUID playerUUID,
                  String playerName, String message, Instant createdAt, AppealStatus status,
                  Instant handledAt, UUID handledByUUID, String handledByName, String response) {
        this.id = id;
        this.punishmentId = punishmentId;
        this.punishmentType = punishmentType;
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.message = message;
        this.createdAt = createdAt;
        this.status = status;
        this.handledAt = handledAt;
        this.handledByUUID = handledByUUID;
        this.handledByName = handledByName;
        this.response = response;
    }
    
    /**
     * Handle the appeal
     */
    public void handle(UUID handledByUUID, String handledByName, AppealStatus status, String response) {
        this.handledByUUID = handledByUUID;
        this.handledByName = handledByName;
        this.status = status;
        this.response = response;
        this.handledAt = Instant.now();
    }
    
    // Getters and Setters
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public long getPunishmentId() {
        return punishmentId;
    }
    
    public PunishmentType getPunishmentType() {
        return punishmentType;
    }
    
    public UUID getPlayerUUID() {
        return playerUUID;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public String getMessage() {
        return message;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public AppealStatus getStatus() {
        return status;
    }
    
    public void setStatus(AppealStatus status) {
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
    
    public String getResponse() {
        return response;
    }
    
    /**
     * Appeal status enum
     */
    public enum AppealStatus {
        PENDING("pending", "§ePending"),
        ACCEPTED("accepted", "§aAccepted"),
        DENIED("denied", "§cDenied");
        
        private final String id;
        private final String display;
        
        AppealStatus(String id, String display) {
            this.id = id;
            this.display = display;
        }
        
        public String getId() {
            return id;
        }
        
        public String getDisplay() {
            return display;
        }
        
        public static AppealStatus fromId(String id) {
            for (AppealStatus status : values()) {
                if (status.id.equalsIgnoreCase(id)) {
                    return status;
                }
            }
            return PENDING;
        }
    }
}
