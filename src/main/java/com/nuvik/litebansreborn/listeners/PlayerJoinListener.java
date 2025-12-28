package com.nuvik.litebansreborn.listeners;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.config.MessagesManager;
import com.nuvik.litebansreborn.managers.AltManager;
import com.nuvik.litebansreborn.models.Punishment;
import com.nuvik.litebansreborn.utils.PlayerUtil;
import com.nuvik.litebansreborn.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;
import java.util.UUID;

/**
 * Listener for player join events
 */
public class PlayerJoinListener implements Listener {

    private final LiteBansReborn plugin;
    
    public PlayerJoinListener(LiteBansReborn plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Handle pre-login to check for bans
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        String name = event.getName();
        String ip = event.getAddress().getHostAddress();
        
        // Check for UUID ban
        plugin.getBanManager().getActiveBan(uuid).thenAccept(ban -> {
            if (ban != null && ban.isActiveAndValid()) {
                String kickMessage = formatBanScreen(ban);
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, kickMessage);
                return;
            }
            
            // Check for IP ban
            plugin.getBanManager().getActiveIPBan(ip).thenAccept(ipBan -> {
                if (ipBan != null && ipBan.isActiveAndValid()) {
                    // Check if player is allowed to bypass
                    plugin.getAltManager().isAllowed(uuid).thenAccept(allowed -> {
                        if (!allowed) {
                            String kickMessage = formatBanScreen(ipBan);
                            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, kickMessage);
                        }
                    }).join();
                }
            }).join();
        }).join();
    }
    
    /**
     * Handle player join
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String name = player.getName();
        String ip = PlayerUtil.getPlayerIP(player);
        
        // Update player data for alt detection
        if (ip != null) {
            plugin.getAltManager().updatePlayerData(uuid, name, ip);
        }
        
        // Check for mute and cache it
        plugin.getMuteManager().getActiveMute(uuid).thenAccept(mute -> {
            if (mute != null) {
                plugin.getCacheManager().cacheMute(mute);
            }
        });
        
        // Check for alt accounts
        if (ip != null && plugin.getConfigManager().getBoolean("alt-detection.check-on-join")) {
            checkAlts(player, uuid, ip);
        }
        
        // Show pending reports/appeals to staff
        if (player.hasPermission("litebansreborn.notify")) {
            showStaffNotifications(player);
        }
    }
    
    /**
     * Check for alt accounts and notify staff
     */
    private void checkAlts(Player player, UUID uuid, String ip) {
        plugin.getAltManager().checkAlts(uuid, ip).thenAccept(alts -> {
            if (alts.isEmpty()) return;
            
            // Check if any alts are banned
            boolean hasBannedAlt = alts.stream().anyMatch(AltManager.AltAccount::isBanned);
            
            if (hasBannedAlt && plugin.getConfigManager().getBoolean("alt-detection.notify-staff")) {
                // Find the banned alt
                AltManager.AltAccount bannedAlt = alts.stream()
                        .filter(AltManager.AltAccount::isBanned)
                        .findFirst()
                        .orElse(null);
                
                if (bannedAlt != null) {
                    String message = plugin.getMessagesManager().get("alts.notify.join",
                            "player", player.getName(),
                            "banned", bannedAlt.getName()
                    );
                    
                    // Notify staff
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        for (Player staff : Bukkit.getOnlinePlayers()) {
                            if (staff.hasPermission("litebansreborn.notify")) {
                                staff.sendMessage(message);
                            }
                        }
                    });
                    
                    // Auto-action if configured
                    if (plugin.getConfigManager().getBoolean("alt-detection.auto-action.enabled")) {
                        String action = plugin.getConfigManager().getString("alt-detection.auto-action.action");
                        String reason = plugin.getConfigManager().getString("alt-detection.auto-action.reason");
                        
                        if ("ban".equalsIgnoreCase(action)) {
                            plugin.getBanManager().ban(uuid, player.getName(), ip,
                                    PlayerUtil.CONSOLE_UUID, "AutoMod",
                                    reason, null, false, false);
                        }
                    }
                }
            } else if (alts.size() > 0 && plugin.getConfigManager().getBoolean("alt-detection.notify-staff")) {
                String message = plugin.getMessagesManager().get("alts.notify.detected",
                        "player", player.getName(),
                        "count", String.valueOf(alts.size())
                );
                
                Bukkit.getScheduler().runTask(plugin, () -> {
                    for (Player staff : Bukkit.getOnlinePlayers()) {
                        if (staff.hasPermission("litebansreborn.notify")) {
                            staff.sendMessage(message);
                        }
                    }
                });
            }
        });
    }
    
    /**
     * Show pending reports and appeals to staff
     */
    private void showStaffNotifications(Player player) {
        // Check pending reports
        if (plugin.getConfigManager().getBoolean("reports.enabled")) {
            plugin.getReportManager().getPendingReportsCount().thenAccept(count -> {
                if (count > 0) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.sendMessage(plugin.getMessagesManager().get("reports.notify",
                                "count", String.valueOf(count)));
                    });
                }
            });
        }
        
        // Check pending appeals
        if (plugin.getConfigManager().getBoolean("appeals.enabled")) {
            plugin.getAppealManager().getPendingAppealsCount().thenAccept(count -> {
                if (count > 0) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.sendMessage(plugin.getMessagesManager().get("appeals.notify",
                                "count", String.valueOf(count)));
                    });
                }
            });
        }
    }
    
    /**
     * Format the ban screen message
     */
    private String formatBanScreen(Punishment ban) {
        List<String> screen = plugin.getMessagesManager().getList("ban.screen",
                MessagesManager.placeholders(
                        "reason", ban.getReason(),
                        "executor", ban.getExecutorName(),
                        "date", TimeUtil.formatDate(ban.getCreatedAt()),
                        "duration", ban.isPermanent() ? "Permanent" : TimeUtil.formatDuration(ban.getRemainingTime()),
                        "expires", ban.isPermanent() ? "Never" : TimeUtil.formatDate(ban.getExpiresAt()),
                        "id", String.valueOf(ban.getId())
                )
        );
        
        return String.join("\n", screen);
    }
}
