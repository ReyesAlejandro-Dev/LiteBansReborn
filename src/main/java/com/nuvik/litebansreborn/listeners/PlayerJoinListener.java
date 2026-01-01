package com.nuvik.litebansreborn.listeners;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.antivpn.VPNManager;
import com.nuvik.litebansreborn.antivpn.VPNResult;
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

import java.net.InetAddress;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

/**
 * Listener for player join events - handles ban checks, GeoIP, VPN detection, and alt detection
 */
public class PlayerJoinListener implements Listener {

    // Constants
    private static final long DB_TIMEOUT_SECONDS = 3;
    private static final String PERM_NOTIFY = "litebansreborn.notify";
    private static final String PERM_BYPASS_VPN = "litebansreborn.bypass.vpn";
    
    private final LiteBansReborn plugin;
    
    public PlayerJoinListener(LiteBansReborn plugin) {
        this.plugin = plugin;
    }
    
    // ==================== PRE-LOGIN (BAN CHECKS) ====================
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        String name = event.getName();
        String ip = event.getAddress().getHostAddress();
        
        try {
            // Check UUID ban
            Punishment ban = awaitFuture(plugin.getBanManager().getActiveBan(uuid), "uuid-ban", name);
            if (isActiveBan(ban)) {
                disallowLogin(event, ban);
                return;
            }
            
            // Check IP ban
            Punishment ipBan = awaitFuture(plugin.getBanManager().getActiveIPBan(ip), "ip-ban", name);
            if (isActiveBan(ipBan)) {
                boolean allowed = awaitFuture(plugin.getAltManager().isAllowed(uuid), "alt-allowed", name);
                if (!allowed) {
                    disallowLogin(event, ipBan);
                }
            }
        } catch (TimeoutException e) {
            plugin.log(Level.WARNING, "Database timeout for " + name + " - allowing join. CHECK IF BANNED!");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            plugin.log(Level.SEVERE, "Database error for " + name + ": " + (cause != null ? cause.getMessage() : e.getMessage()));
            if (plugin.getConfigManager().getBoolean("general.debug", false) && cause != null) {
                cause.printStackTrace();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            plugin.log(Level.WARNING, "Thread interrupted during ban check for " + name);
        } catch (Exception e) {
            plugin.log(Level.SEVERE, "Unexpected error for " + name + ": " + e.getMessage());
            if (plugin.getConfigManager().getBoolean("general.debug", false)) {
                e.printStackTrace();
            }
        }
    }
    
    // ==================== PLAYER JOIN ====================
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String name = player.getName();
        String ip = PlayerUtil.getPlayerIP(player);
        
        // Early return if no IP
        if (ip == null) {
            plugin.debug("PlayerJoin: No IP for " + name);
            return;
        }
        
        // Update player data for alt detection
        plugin.getAltManager().updatePlayerData(uuid, name, ip);
        
        // GeoIP lookup (skip private IPs)
        handleGeoIPLookup(player, uuid, name, ip);
        
        // Cache active mute
        cacheMuteIfPresent(uuid);
        
        // VPN check
        if (shouldCheckVPN(player)) {
            checkVPN(player, uuid, name, ip);
        }
        
        // Alt detection
        if (shouldCheckAlts(ip)) {
            checkAlts(player, uuid, ip);
        }
        
        // Staff notifications
        if (player.hasPermission(PERM_NOTIFY)) {
            showStaffNotifications(player);
        }
    }
    
    // ==================== HELPER METHODS ====================
    
    /**
     * Await a CompletableFuture with timeout and proper error context
     */
    private <T> T awaitFuture(CompletableFuture<T> future, String context, String playerName) 
            throws TimeoutException, ExecutionException, InterruptedException {
        try {
            return future.get(DB_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            plugin.log(Level.SEVERE, "Timeout (" + context + ") for " + playerName);
            throw e;
        }
    }
    
    /**
     * Check if a punishment is an active ban
     */
    private boolean isActiveBan(Punishment ban) {
        return ban != null && ban.isActiveAndValid();
    }
    
    /**
     * Disallow login with formatted ban screen
     */
    private void disallowLogin(AsyncPlayerPreLoginEvent event, Punishment ban) {
        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, formatBanScreen(ban));
    }
    
    /**
     * Check if IP is private/local using InetAddress (more robust than string matching)
     */
    private boolean isPrivateOrLocalIP(String ip) {
        try {
            InetAddress addr = InetAddress.getByName(ip);
            return addr.isAnyLocalAddress() 
                || addr.isLoopbackAddress() 
                || addr.isLinkLocalAddress() 
                || addr.isSiteLocalAddress();
        } catch (Exception e) {
            // If parsing fails, assume it's not private (fail-open for GeoIP)
            return false;
        }
    }
    
    /**
     * Run a task on the main server thread
     */
    private void runSync(Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }
    
    /**
     * Notify all online staff with a permission
     */
    private void notifyStaff(String permission, String message) {
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission(permission)) {
                staff.sendMessage(message);
            }
        }
    }
    
    /**
     * Send a count notification to a player (with online check)
     */
    private void sendCountNotification(Player player, String messageKey, int count) {
        if (count > 0) {
            runSync(() -> {
                if (player.isOnline()) {
                    player.sendMessage(plugin.getMessagesManager().get(messageKey, "count", String.valueOf(count)));
                }
            });
        }
    }
    
    // ==================== GEOIP ====================
    
    private void handleGeoIPLookup(Player player, UUID uuid, String name, String ip) {
        boolean isPrivate = isPrivateOrLocalIP(ip);
        plugin.debug("GeoIP: " + name + " IP=" + ip + " (private=" + isPrivate + ")");
        
        if (isPrivate) {
            plugin.debug("GeoIP: Skipping private IP " + ip);
            return;
        }
        
        if (plugin.getGeoIPManager() == null) {
            return;
        }
        
        plugin.debug("GeoIP: Starting lookup for " + ip);
        plugin.getGeoIPManager().lookup(ip).thenAccept(result -> {
            if (result == null || result.getCountry() == null || result.getCountry().isEmpty()) {
                plugin.debug("GeoIP: No country returned for " + ip);
                return;
            }
            
            String country = result.getCountry();
            plugin.debug("GeoIP: Resolved " + ip + " -> " + country);
            
            plugin.getDatabaseManager().executeAsync(conn -> {
                String sql = "UPDATE " + plugin.getDatabaseManager().getTable("players") + 
                            " SET country = ? WHERE uuid = ?";
                try (var stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, country);
                    stmt.setString(2, uuid.toString());
                    int updated = stmt.executeUpdate();
                    plugin.debug("GeoIP: " + (updated > 0 ? "Updated" : "No rows for") + " " + name + " -> " + country);
                }
            });
        }).exceptionally(ex -> {
            plugin.debug("GeoIP: Failed for " + ip + ": " + ex.getMessage());
            return null;
        });
    }
    
    // ==================== MUTE CACHE ====================
    
    private void cacheMuteIfPresent(UUID uuid) {
        plugin.getMuteManager().getActiveMute(uuid).thenAccept(mute -> {
            if (mute != null) {
                plugin.getCacheManager().cacheMute(mute);
            }
        });
    }
    
    // ==================== VPN CHECK ====================
    
    private boolean shouldCheckVPN(Player player) {
        return plugin.getVPNManager() != null 
            && plugin.getVPNManager().isEnabled() 
            && !player.hasPermission(PERM_BYPASS_VPN);
    }
    
    private void checkVPN(Player player, UUID uuid, String name, String ip) {
        plugin.getVPNManager().checkIP(ip).thenAccept(result -> {
            if (result == null || !result.isDangerous()) {
                plugin.getVPNManager().getDatabase().trackIP(uuid, name, ip, false);
                return;
            }
            
            VPNManager.VPNAction action = plugin.getVPNManager().getAction();
            
            // Log detection
            plugin.getVPNManager().logDetection(result, uuid, name, action);
            logVPNToConsole(name, ip, result, action);
            
            // Execute action
            runSync(() -> {
                if (!player.isOnline()) return;
                
                switch (action) {
                    case KICK -> kickPlayerForVPN(player, result);
                    case WARN, ALLOW -> notifyStaffAboutVPN(player, result);
                    case NONE -> { /* Do nothing */ }
                }
            });
        }).exceptionally(ex -> {
            plugin.debug("VPN check failed for " + name + ": " + ex.getMessage());
            return null;
        });
    }
    
    private void logVPNToConsole(String name, String ip, VPNResult result, VPNManager.VPNAction action) {
        if (plugin.getConfigManager().getBoolean("anti-vpn.notifications.log-to-console", true)) {
            plugin.log(Level.WARNING, "[AntiVPN] " + name + " (" + ip + ") - " + 
                result.getType() + " (" + result.getServiceName() + ") - Action: " + action);
        }
    }
    
    private void kickPlayerForVPN(Player player, VPNResult result) {
        List<String> kickScreen = plugin.getMessagesManager().getList("anti-vpn.kick-screen",
            MessagesManager.placeholders(
                "type", result.getType(),
                "provider", result.getServiceName(),
                "ip", result.getIp(),
                "country", result.getCountry()
            )
        );
        player.kickPlayer(String.join("\n", kickScreen));
    }
    
    private void notifyStaffAboutVPN(Player vpnPlayer, VPNResult result) {
        if (!plugin.getConfigManager().getBoolean("anti-vpn.notifications.notify-staff", true)) {
            return;
        }
        
        String message = plugin.getMessagesManager().get("anti-vpn.staff-notify",
            "player", vpnPlayer.getName(),
            "type", result.getType(),
            "provider", result.getServiceName(),
            "country", result.getCountry(),
            "ip", result.getIp()
        );
        
        notifyStaff(PERM_NOTIFY, message);
    }
    
    // ==================== ALT DETECTION ====================
    
    private boolean shouldCheckAlts(String ip) {
        return ip != null && plugin.getConfigManager().getBoolean("alt-detection.check-on-join", false);
    }
    
    private void checkAlts(Player player, UUID uuid, String ip) {
        plugin.getAltManager().checkAlts(uuid, ip).thenAccept(alts -> {
            if (alts == null || alts.isEmpty()) return;
            
            boolean hasBannedAlt = alts.stream().anyMatch(AltManager.AltAccount::isBanned);
            
            if (hasBannedAlt) {
                handleBannedAltDetected(player, uuid, ip, alts);
            } else if (plugin.getConfigManager().getBoolean("alt-detection.notify-staff", false)) {
                notifyAltCount(player, alts.size());
            }
        });
    }
    
    private void handleBannedAltDetected(Player player, UUID uuid, String ip, List<AltManager.AltAccount> alts) {
        if (!plugin.getConfigManager().getBoolean("alt-detection.notify-staff", false)) {
            return;
        }
        
        AltManager.AltAccount bannedAlt = alts.stream()
            .filter(AltManager.AltAccount::isBanned)
            .findFirst()
            .orElse(null);
        
        if (bannedAlt == null) return;
        
        String message = plugin.getMessagesManager().get("alts.notify.join",
            "player", player.getName(),
            "banned", bannedAlt.getName()
        );
        
        runSync(() -> notifyStaff(PERM_NOTIFY, message));
        
        // Auto-action if configured
        if (plugin.getConfigManager().getBoolean("alt-detection.auto-action.enabled", false)) {
            String action = plugin.getConfigManager().getString("alt-detection.auto-action.action", "ban");
            String reason = plugin.getConfigManager().getString("alt-detection.auto-action.reason", "Alt of banned player");
            
            if ("ban".equalsIgnoreCase(action)) {
                plugin.getBanManager().ban(uuid, player.getName(), ip,
                    PlayerUtil.CONSOLE_UUID, "AutoMod", reason, null, false, false);
            }
        }
    }
    
    private void notifyAltCount(Player player, int count) {
        String message = plugin.getMessagesManager().get("alts.notify.detected",
            "player", player.getName(),
            "count", String.valueOf(count)
        );
        runSync(() -> notifyStaff(PERM_NOTIFY, message));
    }
    
    // ==================== STAFF NOTIFICATIONS ====================
    
    private void showStaffNotifications(Player player) {
        // Pending reports
        if (plugin.getConfigManager().getBoolean("reports.enabled", true)) {
            plugin.getReportManager().getPendingReportsCount().thenAccept(count -> 
                sendCountNotification(player, "reports.notify", count)
            );
        }
        
        // Pending appeals
        if (plugin.getConfigManager().getBoolean("appeals.enabled", true)) {
            plugin.getAppealManager().getPendingAppealsCount().thenAccept(count ->
                sendCountNotification(player, "appeals.notify", count)
            );
        }
    }
    
    // ==================== BAN SCREEN ====================
    
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
