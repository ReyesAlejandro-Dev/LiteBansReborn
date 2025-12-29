package com.nuvik.litebansreborn.models;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a player's data including their history and cached information
 */
public class PlayerData {
    
    private final UUID uuid;
    private String lastKnownName;
    private String lastKnownIP;
    private Instant firstJoin;
    private Instant lastSeen;
    private double punishmentPoints;
    private final List<String> knownIPs;
    private final List<String> knownNames;
    private boolean ipBanExempt;
    
    /**
     * Create new player data
     */
    public PlayerData(UUID uuid, String name, String ip) {
        this.uuid = uuid;
        this.lastKnownName = name;
        this.lastKnownIP = ip;
        this.firstJoin = Instant.now();
        this.lastSeen = Instant.now();
        this.punishmentPoints = 0;
        this.knownIPs = new ArrayList<>();
        this.knownNames = new ArrayList<>();
        this.ipBanExempt = false;
        
        if (ip != null && !ip.isEmpty()) {
            this.knownIPs.add(ip);
        }
        if (name != null && !name.isEmpty()) {
            this.knownNames.add(name);
        }
    }
    
    /**
     * Load existing player data from database
     */
    public PlayerData(UUID uuid, String lastKnownName, String lastKnownIP,
                     Instant firstJoin, Instant lastSeen, double punishmentPoints,
                     List<String> knownIPs, List<String> knownNames, boolean ipBanExempt) {
        this.uuid = uuid;
        this.lastKnownName = lastKnownName;
        this.lastKnownIP = lastKnownIP;
        this.firstJoin = firstJoin;
        this.lastSeen = lastSeen;
        this.punishmentPoints = punishmentPoints;
        this.knownIPs = knownIPs != null ? knownIPs : new ArrayList<>();
        this.knownNames = knownNames != null ? knownNames : new ArrayList<>();
        this.ipBanExempt = ipBanExempt;
    }
    
    /**
     * Update player data on join
     */
    public void updateOnJoin(String name, String ip) {
        this.lastKnownName = name;
        this.lastKnownIP = ip;
        this.lastSeen = Instant.now();
        
        if (name != null && !name.isEmpty() && !knownNames.contains(name)) {
            knownNames.add(name);
        }
        if (ip != null && !ip.isEmpty() && !knownIPs.contains(ip)) {
            knownIPs.add(ip);
        }
    }
    
    /**
     * Add punishment points
     */
    public void addPoints(double points) {
        this.punishmentPoints += points;
    }
    
    /**
     * Remove punishment points
     */
    public void removePoints(double points) {
        this.punishmentPoints = Math.max(0, this.punishmentPoints - points);
    }
    
    /**
     * Decay points by a percentage
     */
    public void decayPoints(double decayAmount) {
        this.punishmentPoints = Math.max(0, this.punishmentPoints - decayAmount);
    }
    
    // Getters and Setters
    public UUID getUuid() {
        return uuid;
    }
    
    public String getLastKnownName() {
        return lastKnownName;
    }
    
    public void setLastKnownName(String lastKnownName) {
        this.lastKnownName = lastKnownName;
    }
    
    public String getLastKnownIP() {
        return lastKnownIP;
    }
    
    public void setLastKnownIP(String lastKnownIP) {
        this.lastKnownIP = lastKnownIP;
    }
    
    public Instant getFirstJoin() {
        return firstJoin;
    }
    
    public void setFirstJoin(Instant firstJoin) {
        this.firstJoin = firstJoin;
    }
    
    public Instant getLastSeen() {
        return lastSeen;
    }
    
    public void setLastSeen(Instant lastSeen) {
        this.lastSeen = lastSeen;
    }
    
    public double getPunishmentPoints() {
        return punishmentPoints;
    }
    
    public void setPunishmentPoints(double punishmentPoints) {
        this.punishmentPoints = punishmentPoints;
    }
    
    public List<String> getKnownIPs() {
        return knownIPs;
    }
    
    public List<String> getKnownNames() {
        return knownNames;
    }
    
    public boolean isIpBanExempt() {
        return ipBanExempt;
    }
    
    public void setIpBanExempt(boolean ipBanExempt) {
        this.ipBanExempt = ipBanExempt;
    }
}
