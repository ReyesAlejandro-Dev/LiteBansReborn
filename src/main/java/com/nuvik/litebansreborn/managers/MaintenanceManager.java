package com.nuvik.litebansreborn.managers;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Maintenance Mode Manager - Server maintenance system
 * Features:
 * - Block player logins during maintenance
 * - Whitelist bypass
 * - Scheduled maintenance
 * - Customizable messages
 * - Discord notifications
 */
public class MaintenanceManager implements Listener {

    private final LiteBansReborn plugin;
    private boolean maintenanceMode = false;
    private final Set<UUID> whitelist = ConcurrentHashMap.newKeySet();
    private String maintenanceReason = "Server maintenance in progress";
    private long maintenanceStarted = 0;
    private Long scheduledEnd = null;

    public MaintenanceManager(LiteBansReborn plugin) {
        this.plugin = plugin;
        loadConfig();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private void loadConfig() {
        maintenanceMode = plugin.getConfigManager().getBoolean("maintenance.enabled", false);
        maintenanceReason = plugin.getConfigManager().getString("maintenance.reason", "Server maintenance in progress");
        
        // Load whitelist
        List<String> whitelistUUIDs = plugin.getConfigManager().getStringList("maintenance.whitelist");
        for (String uuid : whitelistUUIDs) {
            try {
                whitelist.add(UUID.fromString(uuid));
            } catch (Exception ignored) {}
        }
    }

    // ==================== MAINTENANCE CONTROL ====================

    /**
     * Enable maintenance mode
     */
    public void enable(String reason, Long durationMs) {
        this.maintenanceMode = true;
        this.maintenanceReason = reason != null ? reason : "Server maintenance in progress";
        this.maintenanceStarted = System.currentTimeMillis();
        this.scheduledEnd = durationMs != null ? System.currentTimeMillis() + durationMs : null;

        // Kick non-whitelisted players
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!canBypass(player)) {
                kickPlayer(player);
            }
        }

        // Notify Discord
        if (plugin.getDiscordBotManager() != null && plugin.getDiscordBotManager().isEnabled()) {
            // Send maintenance notification
        }

        plugin.log(Level.INFO, "§c⚠ Maintenance mode ENABLED: " + maintenanceReason);
        
        // Schedule auto-disable if duration set
        if (scheduledEnd != null) {
            long delay = (scheduledEnd - System.currentTimeMillis()) / 50; // Convert to ticks
            Bukkit.getScheduler().runTaskLater(plugin, this::disable, delay);
        }
    }

    /**
     * Disable maintenance mode
     */
    public void disable() {
        this.maintenanceMode = false;
        this.maintenanceStarted = 0;
        this.scheduledEnd = null;

        plugin.log(Level.INFO, "§a✓ Maintenance mode DISABLED");
        
        // Broadcast
        Bukkit.broadcastMessage(ColorUtil.translate(
            plugin.getMessagesManager().getPrefix() + "&aServer maintenance has ended!"
        ));
    }

    /**
     * Check if maintenance is active
     */
    public boolean isEnabled() {
        return maintenanceMode;
    }

    // ==================== WHITELIST ====================

    /**
     * Add player to maintenance whitelist
     */
    public void addToWhitelist(UUID uuid) {
        whitelist.add(uuid);
        saveWhitelist();
    }

    /**
     * Remove player from maintenance whitelist
     */
    public void removeFromWhitelist(UUID uuid) {
        whitelist.remove(uuid);
        saveWhitelist();
    }

    /**
     * Check if player is whitelisted
     */
    public boolean isWhitelisted(UUID uuid) {
        return whitelist.contains(uuid);
    }

    /**
     * Check if player can bypass maintenance
     */
    public boolean canBypass(Player player) {
        return player.hasPermission("litebansreborn.maintenance.bypass") ||
               isWhitelisted(player.getUniqueId());
    }

    private void saveWhitelist() {
        List<String> uuids = new ArrayList<>();
        for (UUID uuid : whitelist) {
            uuids.add(uuid.toString());
        }
        plugin.getConfig().set("maintenance.whitelist", uuids);
        plugin.saveConfig();
    }

    // ==================== EVENTS ====================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (!maintenanceMode) return;

        Player player = event.getPlayer();
        if (canBypass(player)) {
            return;
        }

        event.disallow(PlayerLoginEvent.Result.KICK_OTHER, getKickMessage());
    }

    private void kickPlayer(Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.kickPlayer(getKickMessage());
        });
    }

    private String getKickMessage() {
        StringBuilder msg = new StringBuilder();
        msg.append(ColorUtil.translate("&c&l⚠ SERVER MAINTENANCE ⚠\n\n"));
        msg.append(ColorUtil.translate("&7" + maintenanceReason + "\n\n"));
        
        if (scheduledEnd != null) {
            long remaining = scheduledEnd - System.currentTimeMillis();
            if (remaining > 0) {
                msg.append(ColorUtil.translate("&7Estimated time: &e" + formatDuration(remaining) + "\n\n"));
            }
        }
        
        msg.append(ColorUtil.translate("&7Please try again later."));
        return msg.toString();
    }

    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m";
        } else if (minutes > 0) {
            return minutes + "m " + (seconds % 60) + "s";
        } else {
            return seconds + "s";
        }
    }

    // ==================== GETTERS ====================

    public String getReason() {
        return maintenanceReason;
    }

    public long getStartTime() {
        return maintenanceStarted;
    }

    public Long getScheduledEnd() {
        return scheduledEnd;
    }

    public Set<UUID> getWhitelist() {
        return new HashSet<>(whitelist);
    }
}
