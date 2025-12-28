package com.nuvik.litebansreborn.models;

/**
 * Enum representing all punishment types in LiteBansReborn
 */
public enum PunishmentType {
    
    BAN("ban", "Ban", "banned", "§c"),
    TEMP_BAN("tempban", "Temp Ban", "temporarily banned", "§c"),
    IP_BAN("ipban", "IP Ban", "IP banned", "§4"),
    MUTE("mute", "Mute", "muted", "§e"),
    TEMP_MUTE("tempmute", "Temp Mute", "temporarily muted", "§e"),
    IP_MUTE("ipmute", "IP Mute", "IP muted", "§6"),
    KICK("kick", "Kick", "kicked", "§6"),
    WARN("warn", "Warning", "warned", "§d"),
    FREEZE("freeze", "Freeze", "frozen", "§b"),
    NOTE("note", "Note", "noted", "§7");
    
    private final String id;
    private final String displayName;
    private final String pastTense;
    private final String color;
    
    PunishmentType(String id, String displayName, String pastTense, String color) {
        this.id = id;
        this.displayName = displayName;
        this.pastTense = pastTense;
        this.color = color;
    }
    
    public String getId() {
        return id;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getPastTense() {
        return pastTense;
    }
    
    public String getColor() {
        return color;
    }
    
    /**
     * Check if this is a ban-type punishment
     */
    public boolean isBanType() {
        return this == BAN || this == TEMP_BAN || this == IP_BAN;
    }
    
    /**
     * Check if this is a mute-type punishment
     */
    public boolean isMuteType() {
        return this == MUTE || this == TEMP_MUTE || this == IP_MUTE;
    }
    
    /**
     * Check if this is a temporary punishment
     */
    public boolean isTemporary() {
        return this == TEMP_BAN || this == TEMP_MUTE;
    }
    
    /**
     * Check if this is an IP-based punishment
     */
    public boolean isIpBased() {
        return this == IP_BAN || this == IP_MUTE;
    }
    
    /**
     * Check if this punishment type has a duration
     */
    public boolean hasDuration() {
        return this != KICK && this != WARN && this != NOTE;
    }
    
    /**
     * Get a PunishmentType from its ID
     */
    public static PunishmentType fromId(String id) {
        for (PunishmentType type : values()) {
            if (type.id.equalsIgnoreCase(id)) {
                return type;
            }
        }
        return null;
    }
    
    /**
     * Get base type (e.g., TEMP_BAN -> BAN)
     */
    public PunishmentType getBaseType() {
        switch (this) {
            case TEMP_BAN:
            case IP_BAN:
                return BAN;
            case TEMP_MUTE:
            case IP_MUTE:
                return MUTE;
            default:
                return this;
        }
    }
}
