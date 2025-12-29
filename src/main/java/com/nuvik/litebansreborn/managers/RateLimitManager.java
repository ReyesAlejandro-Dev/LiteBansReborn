package com.nuvik.litebansreborn.managers;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.utils.ColorUtil;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Rate Limit Manager - Smart cooldowns and rate limiting
 * Prevents staff abuse and player spam
 */
public class RateLimitManager {

    private final LiteBansReborn plugin;
    
    // Staff rate limiting
    private final Map<UUID, StaffRateLimit> staffLimits;
    
    // Player rate limiting
    private final Map<UUID, PlayerRateLimit> playerLimits;
    
    // 2FA pending confirmations
    private final Map<UUID, PendingConfirmation> pendingConfirmations;

    public RateLimitManager(LiteBansReborn plugin) {
        this.plugin = plugin;
        this.staffLimits = new ConcurrentHashMap<>();
        this.playerLimits = new ConcurrentHashMap<>();
        this.pendingConfirmations = new ConcurrentHashMap<>();
    }

    /**
     * Staff rate limit tracker
     */
    private static class StaffRateLimit {
        final List<Long> bansThisHour = new ArrayList<>();
        final List<Long> permBansToday = new ArrayList<>();
        long lastBan = 0;
        
        void addBan(boolean permanent) {
            long now = System.currentTimeMillis();
            bansThisHour.add(now);
            lastBan = now;
            if (permanent) {
                permBansToday.add(now);
            }
            cleanup();
        }
        
        void cleanup() {
            long hourAgo = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1);
            long dayAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1);
            bansThisHour.removeIf(t -> t < hourAgo);
            permBansToday.removeIf(t -> t < dayAgo);
        }
        
        int getBansThisHour() {
            cleanup();
            return bansThisHour.size();
        }
        
        int getPermBansToday() {
            cleanup();
            return permBansToday.size();
        }
    }

    /**
     * Player rate limit tracker
     */
    private static class PlayerRateLimit {
        long lastReport = 0;
        long lastAppeal = 0;
        int reportsToday = 0;
        long dayStart = System.currentTimeMillis();
        
        void addReport() {
            checkDayReset();
            lastReport = System.currentTimeMillis();
            reportsToday++;
        }
        
        void addAppeal() {
            lastAppeal = System.currentTimeMillis();
        }
        
        void checkDayReset() {
            if (System.currentTimeMillis() - dayStart > TimeUnit.DAYS.toMillis(1)) {
                reportsToday = 0;
                dayStart = System.currentTimeMillis();
            }
        }
    }

    /**
     * Pending 2FA confirmation
     */
    public static class PendingConfirmation {
        public final String action;
        public final String target;
        public final String reason;
        public final long expiry;
        public final String confirmCode;

        public PendingConfirmation(String action, String target, String reason) {
            this.action = action;
            this.target = target;
            this.reason = reason;
            this.expiry = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(2);
            this.confirmCode = String.valueOf(new Random().nextInt(9000) + 1000); // 4-digit code
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expiry;
        }
    }

    // ==================== STAFF RATE LIMITING ====================

    /**
     * Check if staff can perform a ban
     */
    public boolean canBan(UUID staffUUID, boolean permanent) {
        if (!plugin.getConfigManager().getBoolean("smart-limits.enabled", true)) {
            return true;
        }
        
        StaffRateLimit limit = staffLimits.computeIfAbsent(staffUUID, k -> new StaffRateLimit());
        
        int maxPerHour = plugin.getConfigManager().getInt("smart-limits.staff.max-bans-per-hour", 20);
        int maxPermPerDay = plugin.getConfigManager().getInt("smart-limits.staff.max-permanent-per-day", 5);
        
        if (limit.getBansThisHour() >= maxPerHour) {
            return false;
        }
        
        if (permanent && limit.getPermBansToday() >= maxPermPerDay) {
            return false;
        }
        
        return true;
    }

    /**
     * Record a ban by staff
     */
    public void recordBan(UUID staffUUID, boolean permanent) {
        staffLimits.computeIfAbsent(staffUUID, k -> new StaffRateLimit()).addBan(permanent);
    }

    /**
     * Check if permanent ban requires 2FA confirmation
     */
    public boolean requiresConfirmation(UUID staffUUID, boolean permanent) {
        if (!permanent) return false;
        return plugin.getConfigManager().getBoolean("smart-limits.staff.require-2fa-for-perma", true);
    }

    /**
     * Create a pending confirmation
     */
    public PendingConfirmation createConfirmation(UUID staffUUID, String action, String target, String reason) {
        PendingConfirmation confirmation = new PendingConfirmation(action, target, reason);
        pendingConfirmations.put(staffUUID, confirmation);
        return confirmation;
    }

    /**
     * Confirm a pending action
     */
    public PendingConfirmation confirmAction(UUID staffUUID, String code) {
        PendingConfirmation pending = pendingConfirmations.get(staffUUID);
        if (pending == null || pending.isExpired()) {
            pendingConfirmations.remove(staffUUID);
            return null;
        }
        
        if (pending.confirmCode.equals(code)) {
            pendingConfirmations.remove(staffUUID);
            return pending;
        }
        
        return null;
    }

    /**
     * Get staff rate limit info
     */
    public String getStaffLimitInfo(UUID staffUUID) {
        StaffRateLimit limit = staffLimits.get(staffUUID);
        if (limit == null) {
            return "No bans recorded";
        }
        
        int maxPerHour = plugin.getConfigManager().getInt("smart-limits.staff.max-bans-per-hour", 20);
        int maxPermPerDay = plugin.getConfigManager().getInt("smart-limits.staff.max-permanent-per-day", 5);
        
        return String.format("Bans: %d/%d this hour | Perm: %d/%d today",
            limit.getBansThisHour(), maxPerHour,
            limit.getPermBansToday(), maxPermPerDay
        );
    }

    // ==================== PLAYER RATE LIMITING ====================

    /**
     * Check if player can submit a report
     */
    public boolean canReport(UUID playerUUID) {
        if (!plugin.getConfigManager().getBoolean("smart-limits.enabled", true)) {
            return true;
        }
        
        PlayerRateLimit limit = playerLimits.computeIfAbsent(playerUUID, k -> new PlayerRateLimit());
        limit.checkDayReset();
        
        long cooldownMs = parseDuration(plugin.getConfigManager().getString("smart-limits.players.report-cooldown", "5m"));
        int maxPerDay = plugin.getConfigManager().getInt("smart-limits.players.max-reports-per-day", 10);
        
        if (System.currentTimeMillis() - limit.lastReport < cooldownMs) {
            return false;
        }
        
        if (limit.reportsToday >= maxPerDay) {
            return false;
        }
        
        return true;
    }

    /**
     * Record a report by player
     */
    public void recordReport(UUID playerUUID) {
        playerLimits.computeIfAbsent(playerUUID, k -> new PlayerRateLimit()).addReport();
    }

    /**
     * Get remaining cooldown for report
     */
    public long getReportCooldown(UUID playerUUID) {
        PlayerRateLimit limit = playerLimits.get(playerUUID);
        if (limit == null) return 0;
        
        long cooldownMs = parseDuration(plugin.getConfigManager().getString("smart-limits.players.report-cooldown", "5m"));
        long remaining = cooldownMs - (System.currentTimeMillis() - limit.lastReport);
        return remaining > 0 ? remaining : 0;
    }

    /**
     * Check if player can submit an appeal
     */
    public boolean canAppeal(UUID playerUUID) {
        if (!plugin.getConfigManager().getBoolean("smart-limits.enabled", true)) {
            return true;
        }
        
        PlayerRateLimit limit = playerLimits.get(playerUUID);
        if (limit == null) return true;
        
        long cooldownMs = parseDuration(plugin.getConfigManager().getString("smart-limits.players.appeal-cooldown", "24h"));
        return System.currentTimeMillis() - limit.lastAppeal >= cooldownMs;
    }

    /**
     * Record an appeal
     */
    public void recordAppeal(UUID playerUUID) {
        playerLimits.computeIfAbsent(playerUUID, k -> new PlayerRateLimit()).addAppeal();
    }

    /**
     * Get remaining cooldown for appeal
     */
    public long getAppealCooldown(UUID playerUUID) {
        PlayerRateLimit limit = playerLimits.get(playerUUID);
        if (limit == null) return 0;
        
        long cooldownMs = parseDuration(plugin.getConfigManager().getString("smart-limits.players.appeal-cooldown", "24h"));
        long remaining = cooldownMs - (System.currentTimeMillis() - limit.lastAppeal);
        return remaining > 0 ? remaining : 0;
    }

    // ==================== UTILITY ====================

    /**
     * Parse duration string to milliseconds
     */
    private long parseDuration(String duration) {
        if (duration == null || duration.isEmpty()) return 0;
        
        duration = duration.toLowerCase().trim();
        long multiplier = 1000; // seconds by default
        
        if (duration.endsWith("s")) {
            multiplier = 1000;
            duration = duration.substring(0, duration.length() - 1);
        } else if (duration.endsWith("m")) {
            multiplier = 60 * 1000;
            duration = duration.substring(0, duration.length() - 1);
        } else if (duration.endsWith("h")) {
            multiplier = 60 * 60 * 1000;
            duration = duration.substring(0, duration.length() - 1);
        } else if (duration.endsWith("d")) {
            multiplier = 24 * 60 * 60 * 1000;
            duration = duration.substring(0, duration.length() - 1);
        }
        
        try {
            return Long.parseLong(duration) * multiplier;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Format milliseconds to readable string
     */
    public String formatDuration(long ms) {
        if (ms < 1000) return "0s";
        
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) return days + "d " + (hours % 24) + "h";
        if (hours > 0) return hours + "h " + (minutes % 60) + "m";
        if (minutes > 0) return minutes + "m " + (seconds % 60) + "s";
        return seconds + "s";
    }

    /**
     * Send confirmation request to player
     */
    public void sendConfirmationRequest(Player player, PendingConfirmation confirmation) {
        player.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        player.sendMessage(ColorUtil.translate("&c&l⚠ CONFIRMATION REQUIRED"));
        player.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        player.sendMessage(ColorUtil.translate("&7You are about to permanently ban: &c" + confirmation.target));
        player.sendMessage(ColorUtil.translate("&7Reason: &f" + confirmation.reason));
        player.sendMessage("");
        player.sendMessage(ColorUtil.translate("&7To confirm, type: &e/lbr confirm " + confirmation.confirmCode));
        player.sendMessage(ColorUtil.translate("&7Expires in 2 minutes."));
        player.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
    }
}
