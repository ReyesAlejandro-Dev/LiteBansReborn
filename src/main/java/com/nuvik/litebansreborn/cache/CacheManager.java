package com.nuvik.litebansreborn.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.models.PlayerData;
import com.nuvik.litebansreborn.models.Punishment;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Cache Manager - Handles all in-memory caching for performance
 */
public class CacheManager {

    private final LiteBansReborn plugin;
    
    // Player data cache (UUID -> PlayerData)
    private final Cache<UUID, PlayerData> playerCache;
    
    // Active bans cache (UUID -> Punishment)
    private final Cache<UUID, Punishment> activeBansCache;
    
    // Active mutes cache (UUID -> Punishment)
    private final Cache<UUID, Punishment> activeMutesCache;
    
    // IP bans cache (IP -> Punishment)
    private final Cache<String, Punishment> ipBansCache;
    
    // IP mutes cache (IP -> Punishment)
    private final Cache<String, Punishment> ipMutesCache;
    
    // Frozen players (UUID -> reason)
    private final Map<UUID, String> frozenPlayers;
    
    // Staff chat toggle (UUID -> enabled)
    private final Set<UUID> staffChatEnabled;
    
    // Cooldowns (type:uuid -> expiry time)
    private final Cache<String, Long> cooldowns;
    
    public CacheManager(LiteBansReborn plugin) {
        this.plugin = plugin;
        
        // Initialize caches with expiration
        this.playerCache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .build();
        
        this.activeBansCache = Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .build();
        
        this.activeMutesCache = Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .build();
        
        this.ipBansCache = Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .build();
        
        this.ipMutesCache = Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .build();
        
        this.frozenPlayers = new ConcurrentHashMap<>();
        this.staffChatEnabled = ConcurrentHashMap.newKeySet();
        
        this.cooldowns = Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build();
    }
    
    // ==================== Player Cache ====================
    
    public void cachePlayer(PlayerData data) {
        playerCache.put(data.getUuid(), data);
    }
    
    public PlayerData getPlayer(UUID uuid) {
        return playerCache.getIfPresent(uuid);
    }
    
    public void invalidatePlayer(UUID uuid) {
        playerCache.invalidate(uuid);
    }
    
    // ==================== Ban Cache ====================
    
    public void cacheBan(Punishment ban) {
        if (ban.getTargetUUID() != null) {
            activeBansCache.put(ban.getTargetUUID(), ban);
        }
        if (ban.isIpBased() && ban.getTargetIP() != null) {
            ipBansCache.put(ban.getTargetIP(), ban);
        }
    }
    
    public Punishment getBan(UUID uuid) {
        Punishment ban = activeBansCache.getIfPresent(uuid);
        if (ban != null && ban.isActiveAndValid()) {
            return ban;
        }
        return null;
    }
    
    public Punishment getIPBan(String ip) {
        Punishment ban = ipBansCache.getIfPresent(ip);
        if (ban != null && ban.isActiveAndValid()) {
            return ban;
        }
        return null;
    }
    
    public void invalidateBan(UUID uuid) {
        activeBansCache.invalidate(uuid);
    }
    
    public void invalidateIPBan(String ip) {
        ipBansCache.invalidate(ip);
    }
    
    // ==================== Mute Cache ====================
    
    public void cacheMute(Punishment mute) {
        if (mute.getTargetUUID() != null) {
            activeMutesCache.put(mute.getTargetUUID(), mute);
        }
        if (mute.isIpBased() && mute.getTargetIP() != null) {
            ipMutesCache.put(mute.getTargetIP(), mute);
        }
    }
    
    public Punishment getMute(UUID uuid) {
        Punishment mute = activeMutesCache.getIfPresent(uuid);
        if (mute != null && mute.isActiveAndValid()) {
            return mute;
        }
        return null;
    }
    
    public Punishment getIPMute(String ip) {
        Punishment mute = ipMutesCache.getIfPresent(ip);
        if (mute != null && mute.isActiveAndValid()) {
            return mute;
        }
        return null;
    }
    
    public void invalidateMute(UUID uuid) {
        activeMutesCache.invalidate(uuid);
    }
    
    public void invalidateIPMute(String ip) {
        ipMutesCache.invalidate(ip);
    }
    
    // ==================== Freeze Cache ====================
    
    public void freezePlayer(UUID uuid, String reason) {
        frozenPlayers.put(uuid, reason);
    }
    
    public void unfreezePlayer(UUID uuid) {
        frozenPlayers.remove(uuid);
    }
    
    public boolean isFrozen(UUID uuid) {
        return frozenPlayers.containsKey(uuid);
    }
    
    public String getFreezeReason(UUID uuid) {
        return frozenPlayers.get(uuid);
    }
    
    public Set<UUID> getFrozenPlayers() {
        return new HashSet<>(frozenPlayers.keySet());
    }
    
    // ==================== Staff Chat ====================
    
    public void toggleStaffChat(UUID uuid) {
        if (staffChatEnabled.contains(uuid)) {
            staffChatEnabled.remove(uuid);
        } else {
            staffChatEnabled.add(uuid);
        }
    }
    
    public boolean isStaffChatEnabled(UUID uuid) {
        return staffChatEnabled.contains(uuid);
    }
    
    public void disableStaffChat(UUID uuid) {
        staffChatEnabled.remove(uuid);
    }
    
    public Set<UUID> getStaffChatPlayers() {
        return new HashSet<>(staffChatEnabled);
    }
    
    // ==================== Cooldowns ====================
    
    public void setCooldown(String type, UUID uuid, long durationMillis) {
        String key = type + ":" + uuid.toString();
        cooldowns.put(key, System.currentTimeMillis() + durationMillis);
    }
    
    public boolean isOnCooldown(String type, UUID uuid) {
        String key = type + ":" + uuid.toString();
        Long expiry = cooldowns.getIfPresent(key);
        if (expiry == null) {
            return false;
        }
        return System.currentTimeMillis() < expiry;
    }
    
    public long getCooldownRemaining(String type, UUID uuid) {
        String key = type + ":" + uuid.toString();
        Long expiry = cooldowns.getIfPresent(key);
        if (expiry == null) {
            return 0;
        }
        return Math.max(0, expiry - System.currentTimeMillis());
    }
    
    public void clearCooldown(String type, UUID uuid) {
        String key = type + ":" + uuid.toString();
        cooldowns.invalidate(key);
    }
    
    // ==================== Utility Methods ====================
    
    /**
     * Cleanup expired entries
     */
    public void cleanup() {
        // Caffeine handles most cleanup automatically
        // But we can explicitly cleanup inactive punishments
        
        activeBansCache.asMap().entrySet().removeIf(entry -> 
            !entry.getValue().isActiveAndValid());
        
        activeMutesCache.asMap().entrySet().removeIf(entry -> 
            !entry.getValue().isActiveAndValid());
        
        ipBansCache.asMap().entrySet().removeIf(entry -> 
            !entry.getValue().isActiveAndValid());
        
        ipMutesCache.asMap().entrySet().removeIf(entry -> 
            !entry.getValue().isActiveAndValid());
        
        plugin.debug("Cache cleanup completed");
    }
    
    /**
     * Clear all caches
     */
    public void clearAll() {
        playerCache.invalidateAll();
        activeBansCache.invalidateAll();
        activeMutesCache.invalidateAll();
        ipBansCache.invalidateAll();
        ipMutesCache.invalidateAll();
        frozenPlayers.clear();
        // Don't clear staff chat toggles
        cooldowns.invalidateAll();
        
        plugin.debug("All caches cleared");
    }
    
    /**
     * Save all cached data to database (for shutdown)
     */
    public void saveAll() {
        // Currently, most data is saved immediately
        // This is for any pending operations
        plugin.debug("Cache data saved");
    }
    
    /**
     * Get cache statistics
     */
    public Map<String, Long> getStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("players", playerCache.estimatedSize());
        stats.put("bans", activeBansCache.estimatedSize());
        stats.put("mutes", activeMutesCache.estimatedSize());
        stats.put("ipBans", ipBansCache.estimatedSize());
        stats.put("ipMutes", ipMutesCache.estimatedSize());
        stats.put("frozen", (long) frozenPlayers.size());
        stats.put("staffChat", (long) staffChatEnabled.size());
        return stats;
    }
}
