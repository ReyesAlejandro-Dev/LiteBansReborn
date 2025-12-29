package com.nuvik.litebansreborn.api;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.models.Punishment;
import com.nuvik.litebansreborn.models.PunishmentType;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * LiteBansReborn API - For developers to interact with the plugin
 */
public class LiteBansRebornAPI {

    private static LiteBansRebornAPI instance;
    private final LiteBansReborn plugin;
    
    public LiteBansRebornAPI(LiteBansReborn plugin) {
        this.plugin = plugin;
        instance = this;
    }
    
    /**
     * Get the API instance
     */
    public static LiteBansRebornAPI getInstance() {
        return instance;
    }
    
    // ==================== Ban Methods ====================
    
    /**
     * Check if a player is banned
     */
    public CompletableFuture<Boolean> isBanned(UUID uuid) {
        return plugin.getBanManager().getActiveBan(uuid)
                .thenApply(ban -> ban != null && ban.isActiveAndValid());
    }
    
    /**
     * Get the active ban for a player
     */
    public CompletableFuture<Punishment> getBan(UUID uuid) {
        return plugin.getBanManager().getActiveBan(uuid);
    }
    
    /**
     * Check if an IP is banned
     */
    public CompletableFuture<Boolean> isIPBanned(String ip) {
        return plugin.getBanManager().getActiveIPBan(ip)
                .thenApply(ban -> ban != null && ban.isActiveAndValid());
    }
    
    /**
     * Get the active IP ban
     */
    public CompletableFuture<Punishment> getIPBan(String ip) {
        return plugin.getBanManager().getActiveIPBan(ip);
    }
    
    /**
     * Ban a player
     */
    public CompletableFuture<Punishment> ban(UUID targetUUID, String targetName, String targetIP,
                                              UUID executorUUID, String executorName,
                                              String reason, Long durationMs, boolean silent) {
        return plugin.getBanManager().ban(targetUUID, targetName, targetIP,
                executorUUID, executorName, reason, durationMs, silent, false);
    }
    
    /**
     * Unban a player
     */
    public CompletableFuture<Boolean> unban(UUID targetUUID, UUID executorUUID, String executorName) {
        return plugin.getBanManager().unban(targetUUID, executorUUID, executorName, "API unban");
    }
    
    // ==================== Mute Methods ====================
    
    /**
     * Check if a player is muted
     */
    public CompletableFuture<Boolean> isMuted(UUID uuid) {
        return plugin.getMuteManager().getActiveMute(uuid)
                .thenApply(mute -> mute != null && mute.isActiveAndValid());
    }
    
    /**
     * Get the active mute for a player
     */
    public CompletableFuture<Punishment> getMute(UUID uuid) {
        return plugin.getMuteManager().getActiveMute(uuid);
    }
    
    /**
     * Mute a player
     */
    public CompletableFuture<Punishment> mute(UUID targetUUID, String targetName, String targetIP,
                                               UUID executorUUID, String executorName,
                                               String reason, Long durationMs, boolean silent) {
        return plugin.getMuteManager().mute(targetUUID, targetName, targetIP,
                executorUUID, executorName, reason, durationMs, silent, false);
    }
    
    /**
     * Unmute a player
     */
    public CompletableFuture<Boolean> unmute(UUID targetUUID, UUID executorUUID, String executorName) {
        return plugin.getMuteManager().unmute(targetUUID, executorUUID, executorName, "API unmute");
    }
    
    // ==================== Warn Methods ====================
    
    /**
     * Get the warning count for a player
     */
    public CompletableFuture<Integer> getWarningCount(UUID uuid) {
        return plugin.getWarnManager().getActiveWarningCount(uuid);
    }
    
    /**
     * Get all warnings for a player
     */
    public CompletableFuture<List<Punishment>> getWarnings(UUID uuid) {
        return plugin.getWarnManager().getWarnings(uuid);
    }
    
    /**
     * Warn a player
     */
    public CompletableFuture<Punishment> warn(UUID targetUUID, String targetName,
                                               UUID executorUUID, String executorName,
                                               String reason, boolean silent) {
        return plugin.getWarnManager().warn(targetUUID, targetName, executorUUID, executorName, reason, silent);
    }
    
    // ==================== Freeze Methods ====================
    
    /**
     * Check if a player is frozen
     */
    public boolean isFrozen(UUID uuid) {
        return plugin.getFreezeManager().isFrozen(uuid);
    }
    
    /**
     * Freeze a player (must be online)
     */
    public void freeze(UUID uuid, UUID executorUUID, String executorName, String reason) {
        var player = plugin.getServer().getPlayer(uuid);
        if (player != null) {
            plugin.getFreezeManager().freeze(player, executorUUID, executorName, reason, false);
        }
    }
    
    /**
     * Unfreeze a player
     */
    public boolean unfreeze(UUID uuid, UUID executorUUID, String executorName) {
        return plugin.getFreezeManager().unfreeze(uuid, executorUUID, executorName);
    }
    
    // ==================== History Methods ====================
    
    /**
     * Get the punishment history for a player
     */
    public CompletableFuture<List<Punishment>> getHistory(UUID uuid) {
        return plugin.getHistoryManager().getPlayerHistory(uuid);
    }
    
    /**
     * Get a specific punishment by ID
     */
    public CompletableFuture<Punishment> getPunishment(long id) {
        return plugin.getHistoryManager().getPunishment(id);
    }
    
    // ==================== Points Methods ====================
    
    /**
     * Get the punishment points for a player
     */
    public CompletableFuture<Double> getPoints(UUID uuid) {
        return plugin.getPointManager().getPoints(uuid);
    }
    
    /**
     * Add punishment points to a player
     */
    public CompletableFuture<Double> addPoints(UUID uuid, double points) {
        return plugin.getPointManager().addPoints(uuid, points);
    }
    
    /**
     * Reset punishment points for a player
     */
    public CompletableFuture<Void> resetPoints(UUID uuid) {
        return plugin.getPointManager().resetPoints(uuid);
    }
}
