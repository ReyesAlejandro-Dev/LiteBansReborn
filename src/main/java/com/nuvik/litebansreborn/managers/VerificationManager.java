package com.nuvik.litebansreborn.managers;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Verification Manager - Link Minecraft and Discord accounts
 * Features:
 * - Generate verification codes
 * - Link/unlink accounts
 * - Grant verified role
 * - Two-way lookup
 */
public class VerificationManager {

    private final LiteBansReborn plugin;
    private final Map<UUID, VerifiedPlayer> verificationCache = new ConcurrentHashMap<>();
    private final Map<String, PendingVerification> pendingVerifications = new ConcurrentHashMap<>();

    public VerificationManager(LiteBansReborn plugin) {
        this.plugin = plugin;
        initializeDatabase();
    }

    /**
     * Verified player data
     */
    public static class VerifiedPlayer {
        private UUID minecraftUUID;
        private String minecraftName;
        private long discordId;
        private String discordName;
        private long linkedAt;

        // Getters and setters
        public UUID getMinecraftUUID() { return minecraftUUID; }
        public void setMinecraftUUID(UUID minecraftUUID) { this.minecraftUUID = minecraftUUID; }
        public String getMinecraftName() { return minecraftName; }
        public void setMinecraftName(String minecraftName) { this.minecraftName = minecraftName; }
        public long getDiscordId() { return discordId; }
        public void setDiscordId(long discordId) { this.discordId = discordId; }
        public String getDiscordName() { return discordName; }
        public void setDiscordName(String discordName) { this.discordName = discordName; }
        public long getLinkedAt() { return linkedAt; }
        public void setLinkedAt(long linkedAt) { this.linkedAt = linkedAt; }
    }

    /**
     * Pending verification
     */
    private static class PendingVerification {
        final UUID minecraftUUID;
        final String minecraftName;
        final String code;
        final long createdAt;

        PendingVerification(UUID uuid, String name, String code) {
            this.minecraftUUID = uuid;
            this.minecraftName = name;
            this.code = code;
            this.createdAt = System.currentTimeMillis();
        }

        boolean isExpired() {
            // Expires after 10 minutes
            return System.currentTimeMillis() - createdAt > 600000;
        }
    }

    // ==================== DATABASE ====================

    private void initializeDatabase() {
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            
            String sql = "CREATE TABLE IF NOT EXISTS " + plugin.getDatabaseManager().getTable("verified_players") + " (" +
                "minecraft_uuid VARCHAR(36) PRIMARY KEY, " +
                "minecraft_name VARCHAR(16), " +
                "discord_id BIGINT UNIQUE, " +
                "discord_name VARCHAR(64), " +
                "linked_at BIGINT" +
                ")";
            
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }
            
            loadCache();
        } catch (Exception e) {
            plugin.log(Level.WARNING, "Failed to initialize verification table: " + e.getMessage());
        }
    }

    private void loadCache() {
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            String sql = "SELECT * FROM " + plugin.getDatabaseManager().getTable("verified_players");
            
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    VerifiedPlayer vp = new VerifiedPlayer();
                    vp.setMinecraftUUID(UUID.fromString(rs.getString("minecraft_uuid")));
                    vp.setMinecraftName(rs.getString("minecraft_name"));
                    vp.setDiscordId(rs.getLong("discord_id"));
                    vp.setDiscordName(rs.getString("discord_name"));
                    vp.setLinkedAt(rs.getLong("linked_at"));
                    verificationCache.put(vp.getMinecraftUUID(), vp);
                }
            }
            
            plugin.log(Level.INFO, "Loaded " + verificationCache.size() + " verified players.");
        } catch (Exception e) {
            plugin.log(Level.WARNING, "Failed to load verification cache: " + e.getMessage());
        }
    }

    // ==================== VERIFICATION ====================

    /**
     * Generate verification code for a player
     */
    public String generateVerificationCode(Player player) {
        // Check if already verified
        if (isVerified(player.getUniqueId())) {
            player.sendMessage(ColorUtil.translate("&cYou are already verified!"));
            player.sendMessage(ColorUtil.translate("&7Use &e/unlink &7to unlink your account first."));
            return null;
        }

        // Check for existing pending verification
        String existingCode = null;
        for (Map.Entry<String, PendingVerification> entry : pendingVerifications.entrySet()) {
            if (entry.getValue().minecraftUUID.equals(player.getUniqueId())) {
                if (!entry.getValue().isExpired()) {
                    existingCode = entry.getKey();
                } else {
                    pendingVerifications.remove(entry.getKey());
                }
                break;
            }
        }

        if (existingCode != null) {
            return existingCode;
        }

        // Generate new code
        String code = generateCode();
        pendingVerifications.put(code, new PendingVerification(
            player.getUniqueId(), player.getName(), code));

        // Clean up expired codes
        pendingVerifications.entrySet().removeIf(e -> e.getValue().isExpired());

        return code;
    }

    /**
     * Verify with code from Discord
     */
    public CompletableFuture<Boolean> verifyWithCode(String code, long discordId, String discordName) {
        PendingVerification pending = pendingVerifications.get(code.toUpperCase());
        
        if (pending == null || pending.isExpired()) {
            pendingVerifications.remove(code.toUpperCase());
            return CompletableFuture.completedFuture(false);
        }

        return linkAccounts(pending.minecraftUUID, discordId, pending.minecraftName, discordName)
            .thenApply(success -> {
                if (success) {
                    pendingVerifications.remove(code.toUpperCase());
                    
                    // Notify Minecraft player if online
                    Player player = Bukkit.getPlayer(pending.minecraftUUID);
                    if (player != null) {
                        player.sendMessage(ColorUtil.translate("&a&l✓ Verification successful!"));
                        player.sendMessage(ColorUtil.translate("&7Your account is now linked to Discord: &f" + discordName));
                    }
                }
                return success;
            });
    }

    /**
     * Link Minecraft and Discord accounts
     */
    public CompletableFuture<Boolean> linkAccounts(UUID minecraftUUID, long discordId, String minecraftName, String discordName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Connection conn = plugin.getDatabaseManager().getConnection();
                
                // Check if Discord ID is already linked
                String checkSql = "SELECT minecraft_uuid FROM " + plugin.getDatabaseManager().getTable("verified_players") +
                    " WHERE discord_id = ?";
                try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                    checkStmt.setLong(1, discordId);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next()) {
                            return false; // Already linked to another account
                        }
                    }
                }
                
                String sql = "INSERT INTO " + plugin.getDatabaseManager().getTable("verified_players") +
                    " (minecraft_uuid, minecraft_name, discord_id, discord_name, linked_at) VALUES (?, ?, ?, ?, ?)";
                
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, minecraftUUID.toString());
                    stmt.setString(2, minecraftName);
                    stmt.setLong(3, discordId);
                    stmt.setString(4, discordName);
                    stmt.setLong(5, System.currentTimeMillis());
                    stmt.executeUpdate();
                }

                // Update cache
                VerifiedPlayer vp = new VerifiedPlayer();
                vp.setMinecraftUUID(minecraftUUID);
                vp.setMinecraftName(minecraftName);
                vp.setDiscordId(discordId);
                vp.setDiscordName(discordName);
                vp.setLinkedAt(System.currentTimeMillis());
                verificationCache.put(minecraftUUID, vp);

                return true;
            } catch (Exception e) {
                plugin.log(Level.SEVERE, "Failed to link accounts: " + e.getMessage());
                return false;
            }
        });
    }

    /**
     * Simplified link for Discord bot
     */
    public CompletableFuture<Boolean> linkAccounts(UUID minecraftUUID, long discordId) {
        String mcName = Bukkit.getOfflinePlayer(minecraftUUID).getName();
        return linkAccounts(minecraftUUID, discordId, mcName, "Unknown");
    }

    /**
     * Unlink accounts
     */
    public CompletableFuture<Boolean> unlinkAccounts(UUID minecraftUUID) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Connection conn = plugin.getDatabaseManager().getConnection();
                String sql = "DELETE FROM " + plugin.getDatabaseManager().getTable("verified_players") +
                    " WHERE minecraft_uuid = ?";
                
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, minecraftUUID.toString());
                    int affected = stmt.executeUpdate();
                    
                    if (affected > 0) {
                        verificationCache.remove(minecraftUUID);
                        return true;
                    }
                }
                
                return false;
            } catch (Exception e) {
                plugin.log(Level.SEVERE, "Failed to unlink accounts: " + e.getMessage());
                return false;
            }
        });
    }

    // ==================== LOOKUPS ====================

    /**
     * Check if player is verified
     */
    public boolean isVerified(UUID minecraftUUID) {
        return verificationCache.containsKey(minecraftUUID);
    }

    /**
     * Get verified player by Minecraft UUID
     */
    public VerifiedPlayer getVerifiedPlayer(UUID minecraftUUID) {
        return verificationCache.get(minecraftUUID);
    }

    /**
     * Get verified player by Discord ID
     */
    public VerifiedPlayer getVerifiedPlayerByDiscord(long discordId) {
        for (VerifiedPlayer vp : verificationCache.values()) {
            if (vp.getDiscordId() == discordId) {
                return vp;
            }
        }
        return null;
    }

    /**
     * Get Discord ID for Minecraft player
     */
    public Long getDiscordId(UUID minecraftUUID) {
        VerifiedPlayer vp = verificationCache.get(minecraftUUID);
        return vp != null ? vp.getDiscordId() : null;
    }

    /**
     * Get Minecraft UUID for Discord user
     */
    public UUID getMinecraftUUID(long discordId) {
        VerifiedPlayer vp = getVerifiedPlayerByDiscord(discordId);
        return vp != null ? vp.getMinecraftUUID() : null;
    }

    // ==================== UTILITY ====================

    private String generateCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }

    public int getVerifiedCount() {
        return verificationCache.size();
    }

    public Collection<VerifiedPlayer> getAllVerifiedPlayers() {
        return verificationCache.values();
    }

    /**
     * Update Minecraft name if changed
     */
    public void updateMinecraftName(UUID minecraftUUID, String newName) {
        VerifiedPlayer vp = verificationCache.get(minecraftUUID);
        if (vp != null && !vp.getMinecraftName().equals(newName)) {
            vp.setMinecraftName(newName);
            
            // Update database
            CompletableFuture.runAsync(() -> {
                try {
                    Connection conn = plugin.getDatabaseManager().getConnection();
                    String sql = "UPDATE " + plugin.getDatabaseManager().getTable("verified_players") +
                        " SET minecraft_name = ? WHERE minecraft_uuid = ?";
                    
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setString(1, newName);
                        stmt.setString(2, minecraftUUID.toString());
                        stmt.executeUpdate();
                    }
                } catch (Exception e) {
                    plugin.log(Level.WARNING, "Failed to update Minecraft name: " + e.getMessage());
                }
            });
        }
    }
}
