package com.nuvik.litebansreborn.managers;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.config.MessagesManager;
import com.nuvik.litebansreborn.models.Punishment;
import com.nuvik.litebansreborn.models.PunishmentType;
import com.nuvik.litebansreborn.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Warn Manager - Handles all warning-related operations
 */
public class WarnManager {

    private final LiteBansReborn plugin;
    
    public WarnManager(LiteBansReborn plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Warn a player
     */
    public CompletableFuture<Punishment> warn(UUID targetUUID, String targetName, 
                                               UUID executorUUID, String executorName,
                                               String reason, boolean silent) {
        
        if (reason == null || reason.isEmpty()) {
            reason = plugin.getConfigManager().getDefaultWarnReason();
        }
        
        Punishment punishment = new Punishment(
                PunishmentType.WARN, targetUUID, targetName, null,
                executorUUID, executorName, reason,
                plugin.getConfigManager().getServerName(),
                null, silent, false
        );
        
        String finalReason = reason;
        
        return plugin.getDatabaseManager().queryAsync(conn -> {
            String sql = "INSERT INTO " + plugin.getDatabaseManager().getTable("punishments") +
                    " (type, target_uuid, target_name, executor_uuid, executor_name, " +
                    "reason, server, created_at, active, silent, ip_based) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, PunishmentType.WARN.getId());
                stmt.setString(2, targetUUID.toString());
                stmt.setString(3, targetName);
                stmt.setString(4, executorUUID.toString());
                stmt.setString(5, executorName);
                stmt.setString(6, finalReason);
                stmt.setString(7, punishment.getServer());
                stmt.setTimestamp(8, Timestamp.from(punishment.getCreatedAt()));
                stmt.setBoolean(9, true);
                stmt.setBoolean(10, silent);
                stmt.setBoolean(11, false);
                
                stmt.executeUpdate();
                
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        punishment.setId(rs.getLong(1));
                    }
                }
            }
            
            return punishment;
        }).thenCompose(warn -> {
            // Get warning count for auto-action check
            return getActiveWarningCount(targetUUID).thenApply(count -> {
                // Store count for later use
                warn.setId(warn.getId()); // Just to use the variable
                return new Object[] { warn, count };
            });
        }).thenApply(result -> {
            Punishment warn = (Punishment) ((Object[]) result)[0];
            int count = (Integer) ((Object[]) result)[1];
            
            // Notify the player if online
            Bukkit.getScheduler().runTask(plugin, () -> {
                Player player = Bukkit.getPlayer(targetUUID);
                if (player != null) {
                    plugin.getMessagesManager().sendList(player, "warn.received",
                        MessagesManager.placeholders(
                            "reason", warn.getReason(),
                            "executor", warn.getExecutorName(),
                            "count", String.valueOf(count)
                        )
                    );
                }
            });
            
            // Add punishment points
            if (plugin.getConfigManager().getBoolean("points.enabled")) {
                plugin.getPointManager().addPoints(targetUUID,
                    plugin.getConfigManager().getDouble("points.punishment-points.warn", 1));
            }
            
            // Send notifications
            sendWarnNotifications(warn, count);
            
            // Check for auto-action
            checkAutoAction(targetUUID, targetName, count, executorUUID, executorName);
            
            plugin.debug("Player " + targetName + " has been warned (total: " + count + ")");
            
            return warn;
        });
    }
    
    /**
     * Remove a warning
     */
    public CompletableFuture<Boolean> unwarn(UUID targetUUID, long warningId, 
                                              UUID executorUUID, String executorName) {
        return plugin.getDatabaseManager().queryAsync(conn -> {
            String sql;
            PreparedStatement stmt;
            
            if (warningId > 0) {
                // Remove specific warning
                sql = "UPDATE " + plugin.getDatabaseManager().getTable("punishments") +
                        " SET active = FALSE, removed_at = ?, removed_by_uuid = ?, removed_by_name = ? " +
                        "WHERE id = ? AND target_uuid = ? AND type = 'warn' AND active = TRUE";
                stmt = conn.prepareStatement(sql);
                stmt.setTimestamp(1, Timestamp.from(Instant.now()));
                stmt.setString(2, executorUUID.toString());
                stmt.setString(3, executorName);
                stmt.setLong(4, warningId);
                stmt.setString(5, targetUUID.toString());
            } else {
                // Remove most recent warning
                sql = "UPDATE " + plugin.getDatabaseManager().getTable("punishments") +
                        " SET active = FALSE, removed_at = ?, removed_by_uuid = ?, removed_by_name = ? " +
                        "WHERE id = (SELECT id FROM " + plugin.getDatabaseManager().getTable("punishments") +
                        " WHERE target_uuid = ? AND type = 'warn' AND active = TRUE " +
                        "ORDER BY created_at DESC LIMIT 1)";
                stmt = conn.prepareStatement(sql);
                stmt.setTimestamp(1, Timestamp.from(Instant.now()));
                stmt.setString(2, executorUUID.toString());
                stmt.setString(3, executorName);
                stmt.setString(4, targetUUID.toString());
            }
            
            int affected = stmt.executeUpdate();
            stmt.close();
            return affected > 0;
        });
    }
    
    /**
     * Get active warning count for a player
     */
    public CompletableFuture<Integer> getActiveWarningCount(UUID uuid) {
        return plugin.getDatabaseManager().queryAsync(conn -> {
            int expiryDays = plugin.getConfigManager().getWarnExpiry();
            String sql;
            
            if (expiryDays > 0) {
                sql = "SELECT COUNT(*) FROM " + plugin.getDatabaseManager().getTable("punishments") +
                        " WHERE target_uuid = ? AND type = 'warn' AND active = TRUE " +
                        "AND created_at > ?";
            } else {
                sql = "SELECT COUNT(*) FROM " + plugin.getDatabaseManager().getTable("punishments") +
                        " WHERE target_uuid = ? AND type = 'warn' AND active = TRUE";
            }
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                if (expiryDays > 0) {
                    stmt.setTimestamp(2, Timestamp.from(Instant.now().minusSeconds(expiryDays * 24L * 60L * 60L)));
                }
                
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
     * Get all warnings for a player
     */
    public CompletableFuture<List<Punishment>> getWarnings(UUID uuid) {
        return plugin.getDatabaseManager().queryAsync(conn -> {
            String sql = "SELECT * FROM " + plugin.getDatabaseManager().getTable("punishments") +
                    " WHERE target_uuid = ? AND type = 'warn' AND active = TRUE " +
                    "ORDER BY created_at DESC";
            
            List<Punishment> warnings = new ArrayList<>();
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        warnings.add(parsePunishment(rs));
                    }
                }
            }
            return warnings;
        });
    }
    
    /**
     * Expire old warnings
     */
    public void expireOldWarnings() {
        int expiryDays = plugin.getConfigManager().getWarnExpiry();
        if (expiryDays <= 0) return;
        
        plugin.getDatabaseManager().executeAsync(conn -> {
            String sql = "UPDATE " + plugin.getDatabaseManager().getTable("punishments") +
                    " SET active = FALSE " +
                    "WHERE type = 'warn' AND active = TRUE AND created_at < ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setTimestamp(1, Timestamp.from(Instant.now().minusSeconds(expiryDays * 24L * 60L * 60L)));
                int expired = stmt.executeUpdate();
                
                if (expired > 0) {
                    plugin.debug("Expired " + expired + " old warnings");
                }
            }
        });
    }
    
    /**
     * Check and execute auto-action
     */
    private void checkAutoAction(UUID targetUUID, String targetName, int warningCount,
                                  UUID executorUUID, String executorName) {
        if (!plugin.getConfigManager().isWarnAutoActionEnabled()) {
            return;
        }
        
        int maxWarnings = plugin.getConfigManager().getMaxWarnings();
        if (warningCount < maxWarnings) {
            return;
        }
        
        String action = plugin.getConfigManager().getWarnAutoAction();
        String duration = plugin.getConfigManager().getWarnAutoActionDuration();
        String reason = plugin.getConfigManager().getWarnAutoActionReason();
        
        long durationMs = TimeUtil.parseDuration(duration);
        
        Bukkit.getScheduler().runTask(plugin, () -> {
            switch (action.toLowerCase()) {
                case "ban":
                    plugin.getBanManager().ban(targetUUID, targetName, null, 
                        executorUUID, "AutoMod", reason, durationMs, false, false);
                    break;
                case "mute":
                    plugin.getMuteManager().mute(targetUUID, targetName, null,
                        executorUUID, "AutoMod", reason, durationMs, false, false);
                    break;
                case "kick":
                    Player player = Bukkit.getPlayer(targetUUID);
                    if (player != null) {
                        plugin.getKickManager().kick(player, executorUUID, "AutoMod", reason, false);
                    }
                    break;
            }
            
            // Broadcast auto-action
            String message = plugin.getMessagesManager().get("warn.auto-action",
                "player", targetName,
                "action", action
            );
            Bukkit.broadcastMessage(message);
        });
    }
    
    /**
     * Send warning notifications
     */
    private void sendWarnNotifications(Punishment warn, int count) {
        if (plugin.getConfigManager().getBoolean("punishments.warn.broadcast")) {
            String message = plugin.getMessagesManager().get("warn.broadcast",
                "player", warn.getTargetName(),
                "executor", warn.getExecutorName(),
                "reason", warn.getReason(),
                "count", String.valueOf(count)
            );
            
            if (warn.isSilent()) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.hasPermission("litebansreborn.notify")) {
                        player.sendMessage(plugin.getMessagesManager().get("broadcasts.silent-prefix") + message);
                    }
                }
            } else {
                Bukkit.broadcastMessage(message);
            }
        }
        
        if (plugin.getDiscordNotifier() != null) {
            plugin.getDiscordNotifier().sendWarnNotification(warn);
        }
    }
    
    /**
     * Parse a Punishment from ResultSet using centralized method
     */
    private Punishment parsePunishment(ResultSet rs) throws SQLException {
        return Punishment.fromResultSet(rs);
    }
}
