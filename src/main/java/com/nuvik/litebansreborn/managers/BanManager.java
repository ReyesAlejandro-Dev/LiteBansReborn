package com.nuvik.litebansreborn.managers;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.config.MessagesManager;
import com.nuvik.litebansreborn.models.Punishment;
import com.nuvik.litebansreborn.models.PunishmentType;
import com.nuvik.litebansreborn.utils.PlayerUtil;
import com.nuvik.litebansreborn.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Ban Manager - Handles all ban-related operations
 */
public class BanManager {

    private final LiteBansReborn plugin;
    
    public BanManager(LiteBansReborn plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Ban a player
     */
    public CompletableFuture<Punishment> ban(UUID targetUUID, String targetName, String targetIP,
                                              UUID executorUUID, String executorName,
                                              String reason, Long duration, boolean silent, boolean ipBased) {
        
        // Calculate expiry
        Instant expiresAt = duration == null || duration < 0 ? null : TimeUtil.calculateExpiry(duration);
        
        // Determine punishment type
        PunishmentType type;
        if (ipBased) {
            type = PunishmentType.IP_BAN;
        } else if (expiresAt == null) {
            type = PunishmentType.BAN;
        } else {
            type = PunishmentType.TEMP_BAN;
        }
        
        // Use default reason if none provided
        if (reason == null || reason.isEmpty()) {
            reason = plugin.getConfigManager().getDefaultBanReason();
        }
        
        // Create punishment object
        Punishment punishment = new Punishment(
                type, targetUUID, targetName, targetIP,
                executorUUID, executorName, reason,
                plugin.getConfigManager().getServerName(),
                expiresAt, silent, ipBased
        );
        
        String finalReason = reason;
        
        return plugin.getDatabaseManager().queryAsync(conn -> {
            // Insert into database
            String sql = "INSERT INTO " + plugin.getDatabaseManager().getTable("punishments") +
                    " (type, target_uuid, target_name, target_ip, executor_uuid, executor_name, " +
                    "reason, server, created_at, expires_at, active, silent, ip_based) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, punishment.getType().getId());
                stmt.setString(2, targetUUID != null ? targetUUID.toString() : null);
                stmt.setString(3, targetName);
                stmt.setString(4, targetIP);
                stmt.setString(5, executorUUID.toString());
                stmt.setString(6, executorName);
                stmt.setString(7, finalReason);
                stmt.setString(8, punishment.getServer());
                stmt.setTimestamp(9, Timestamp.from(punishment.getCreatedAt()));
                stmt.setTimestamp(10, expiresAt != null ? Timestamp.from(expiresAt) : null);
                stmt.setBoolean(11, true);
                stmt.setBoolean(12, silent);
                stmt.setBoolean(13, ipBased);
                
                stmt.executeUpdate();
                
                // Get generated ID
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        punishment.setId(rs.getLong(1));
                    }
                }
            }
            
            return punishment;
        }).thenApply(ban -> {
            // Cache the ban
            plugin.getCacheManager().cacheBan(ban);
            
            // Kick the player if online
            if (plugin.getConfigManager().getBoolean("punishments.ban.kick-on-ban")) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Player player = Bukkit.getPlayer(targetUUID);
                    if (player != null) {
                        kickBannedPlayer(player, ban);
                    }
                    
                    // For IP bans, kick all players with that IP
                    if (ipBased && targetIP != null) {
                        for (Player online : Bukkit.getOnlinePlayers()) {
                            String playerIP = PlayerUtil.getPlayerIP(online);
                            if (targetIP.equals(playerIP)) {
                                kickBannedPlayer(online, ban);
                            }
                        }
                    }
                });
            }
            
            // Add punishment points
            if (plugin.getConfigManager().getBoolean("points.enabled")) {
                plugin.getPointManager().addPoints(targetUUID, 
                    plugin.getConfigManager().getDouble("points.punishment-points.ban", 10));
            }
            
            // Send notifications
            sendBanNotifications(ban);
            
            // Save chat snapshot
            if (plugin.getSnapshotManager() != null) {
                plugin.getSnapshotManager().saveSnapshot(targetName, finalReason, type.name());
            }

            // Auto Rollback (if configured for permanent bans)
            if (type == PunishmentType.BAN && plugin.getConfigManager().getBoolean("rollback.enabled", true) && 
                plugin.getConfigManager().getBoolean("rollback.on-perm-ban", false)) {
                
                String cmd = plugin.getConfigManager().getString("rollback.command", "co rollback u:{player} t:24h #silent")
                        .replace("{player}", targetName)
                        .replace("{time}", plugin.getConfigManager().getString("rollback.time", "24h"));
                
                Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd));
                plugin.log(Level.INFO, "Triggered auto-rollback for " + targetName);
            }
            
            plugin.debug("Player " + targetName + " has been banned");

            // Wipe Data
            if (plugin.getConfigManager().getBoolean("punishments.ban.wipe-data.enabled", false)) {
                 boolean permanent = (expiresAt == null);
                 boolean onlyPerm = plugin.getConfigManager().getBoolean("punishments.ban.wipe-data.only-permanent", true);
                 
                 if (permanent || !onlyPerm) {
                     // Run delayed to ensure kick happened (1 second)
                     Bukkit.getScheduler().runTaskLater(plugin, () -> wipePlayerData(targetUUID, targetName), 20L);
                 }
            }
            
            return ban;
        });
    }
    
    /**
     * Unban a player
     */
    public CompletableFuture<Boolean> unban(UUID targetUUID, UUID executorUUID, String executorName, String reason) {
        return plugin.getDatabaseManager().queryAsync(conn -> {
            String sql = "UPDATE " + plugin.getDatabaseManager().getTable("punishments") +
                    " SET active = FALSE, removed_at = ?, removed_by_uuid = ?, " +
                    "removed_by_name = ?, remove_reason = ? " +
                    "WHERE target_uuid = ? AND type IN ('ban', 'tempban') AND active = TRUE";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setTimestamp(1, Timestamp.from(Instant.now()));
                stmt.setString(2, executorUUID.toString());
                stmt.setString(3, executorName);
                stmt.setString(4, reason);
                stmt.setString(5, targetUUID.toString());
                
                int affected = stmt.executeUpdate();
                return affected > 0;
            }
        }).thenApply(success -> {
            if (success) {
                plugin.getCacheManager().invalidateBan(targetUUID);
            }
            return success;
        });
    }
    
    /**
     * Unban an IP
     */
    public CompletableFuture<Boolean> unbanIP(String ip, UUID executorUUID, String executorName, String reason) {
        return plugin.getDatabaseManager().queryAsync(conn -> {
            String sql = "UPDATE " + plugin.getDatabaseManager().getTable("punishments") +
                    " SET active = FALSE, removed_at = ?, removed_by_uuid = ?, " +
                    "removed_by_name = ?, remove_reason = ? " +
                    "WHERE target_ip = ? AND type = 'ipban' AND active = TRUE";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setTimestamp(1, Timestamp.from(Instant.now()));
                stmt.setString(2, executorUUID.toString());
                stmt.setString(3, executorName);
                stmt.setString(4, reason);
                stmt.setString(5, ip);
                
                int affected = stmt.executeUpdate();
                return affected > 0;
            }
        }).thenApply(success -> {
            if (success) {
                plugin.getCacheManager().invalidateIPBan(ip);
            }
            return success;
        });
    }
    
    /**
     * Check if a player is banned
     */
    public CompletableFuture<Punishment> getActiveBan(UUID uuid) {
        // Check cache first
        Punishment cached = plugin.getCacheManager().getBan(uuid);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        
        return plugin.getDatabaseManager().queryAsync(conn -> {
            String sql = "SELECT * FROM " + plugin.getDatabaseManager().getTable("punishments") +
                    " WHERE target_uuid = ? AND type IN ('ban', 'tempban') AND active = TRUE " +
                    "AND (expires_at IS NULL OR expires_at > ?) ORDER BY created_at DESC LIMIT 1";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                stmt.setTimestamp(2, Timestamp.from(Instant.now()));
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return parsePunishment(rs);
                    }
                }
            }
            return null;
        }).thenApply(ban -> {
            if (ban != null) {
                plugin.getCacheManager().cacheBan(ban);
            }
            return ban;
        });
    }
    
    /**
     * Check if an IP is banned
     */
    public CompletableFuture<Punishment> getActiveIPBan(String ip) {
        // Check cache first
        Punishment cached = plugin.getCacheManager().getIPBan(ip);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        
        return plugin.getDatabaseManager().queryAsync(conn -> {
            String sql = "SELECT * FROM " + plugin.getDatabaseManager().getTable("punishments") +
                    " WHERE target_ip = ? AND type = 'ipban' AND active = TRUE " +
                    "AND (expires_at IS NULL OR expires_at > ?) ORDER BY created_at DESC LIMIT 1";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, ip);
                stmt.setTimestamp(2, Timestamp.from(Instant.now()));
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return parsePunishment(rs);
                    }
                }
            }
            return null;
        }).thenApply(ban -> {
            if (ban != null) {
                plugin.getCacheManager().cacheBan(ban);
            }
            return ban;
        });
    }
    
    /**
     * Get all active bans
     */
    public CompletableFuture<List<Punishment>> getActiveBans(int page, int perPage) {
        return plugin.getDatabaseManager().queryAsync(conn -> {
            String sql = "SELECT * FROM " + plugin.getDatabaseManager().getTable("punishments") +
                    " WHERE type IN ('ban', 'tempban', 'ipban') AND active = TRUE " +
                    "AND (expires_at IS NULL OR expires_at > ?) " +
                    "ORDER BY created_at DESC LIMIT ? OFFSET ?";
            
            List<Punishment> bans = new ArrayList<>();
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setTimestamp(1, Timestamp.from(Instant.now()));
                stmt.setInt(2, perPage);
                stmt.setInt(3, (page - 1) * perPage);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        bans.add(parsePunishment(rs));
                    }
                }
            }
            return bans;
        });
    }
    
    /**
     * Get total active bans count
     */
    public CompletableFuture<Integer> getActiveBansCount() {
        return plugin.getDatabaseManager().queryAsync(conn -> {
            String sql = "SELECT COUNT(*) FROM " + plugin.getDatabaseManager().getTable("punishments") +
                    " WHERE type IN ('ban', 'tempban', 'ipban') AND active = TRUE " +
                    "AND (expires_at IS NULL OR expires_at > ?)";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setTimestamp(1, Timestamp.from(Instant.now()));
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
            return 0;
        });
    }
    
    /**
     * Kick a banned player with the ban screen
     */
    private void kickBannedPlayer(Player player, Punishment ban) {
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
        
        player.kickPlayer(String.join("\n", screen));
    }
    
    /**
     * Send ban notifications
     */
    private void sendBanNotifications(Punishment ban) {
        // Broadcast
        if (plugin.getConfigManager().getBoolean("punishments.ban.broadcast")) {
            String message;
            if (ban.isPermanent()) {
                message = plugin.getMessagesManager().get("ban.broadcast.permanent",
                    "player", ban.getTargetName(),
                    "executor", ban.getExecutorName(),
                    "reason", ban.getReason()
                );
            } else {
                message = plugin.getMessagesManager().get("ban.broadcast.temporary",
                    "player", ban.getTargetName(),
                    "executor", ban.getExecutorName(),
                    "reason", ban.getReason(),
                    "duration", TimeUtil.formatDuration(ban.getRemainingTime())
                );
            }
            
            if (ban.isSilent()) {
                // Only send to staff
                String silentPrefix = plugin.getMessagesManager().get("ban.broadcast.silent");
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.hasPermission("litebansreborn.notify")) {
                        player.sendMessage(silentPrefix.replace("%message%", message));
                    }
                }
            } else {
                Bukkit.broadcastMessage(message);
            }
        }
        
        // Discord notification
        if (plugin.getDiscordNotifier() != null) {
            plugin.getDiscordNotifier().sendBanNotification(ban);
        }
        
        // Telegram notification
        if (plugin.getTelegramNotifier() != null) {
            plugin.getTelegramNotifier().sendBanNotification(ban);
        }
    }
    
    /**
     * Parse a Punishment from ResultSet using centralized method
     */
    private Punishment parsePunishment(ResultSet rs) throws SQLException {
        return Punishment.fromResultSet(rs);
    }
    
    /**
     * Wipe player data (Inventory, EC, etc) and run cleanup commands
     */
    private void wipePlayerData(UUID uuid, String name) {
        plugin.log(Level.INFO, "Wiping data for banned player: " + name + " (" + uuid + ")");
        
        // 1. Run commands
        List<String> commands = plugin.getConfigManager().getStringList("punishments.ban.wipe-data.commands");
        if (commands != null) {
            for (String cmd : commands) {
                String finalCmd = cmd.replace("{player}", name).replace("{uuid}", uuid.toString());
                Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd));
            }
        }
        
        // 2. Delete world data
        if (plugin.getConfigManager().getBoolean("punishments.ban.wipe-data.delete-world-data", false)) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                for (org.bukkit.World world : Bukkit.getWorlds()) {
                    // Player Data
                    File playerFile = new File(world.getWorldFolder(), "playerdata/" + uuid + ".dat");
                    if (playerFile.exists()) {
                        if (playerFile.delete()) {
                            plugin.log(Level.INFO, "Deleted playerdata in " + world.getName());
                        }
                    }
                    
                    // Stats
                    File statsFile = new File(world.getWorldFolder(), "stats/" + uuid + ".json");
                    if (statsFile.exists()) statsFile.delete();
                    
                    // Advancements
                    File advFile = new File(world.getWorldFolder(), "advancements/" + uuid + ".json");
                    if (advFile.exists()) advFile.delete();
                }
            });
        }
    }
}
