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
 * Mute Manager - Handles all mute-related operations
 */
public class MuteManager {

    private final LiteBansReborn plugin;
    
    public MuteManager(LiteBansReborn plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Mute a player
     */
    public CompletableFuture<Punishment> mute(UUID targetUUID, String targetName, String targetIP,
                                               UUID executorUUID, String executorName,
                                               String reason, Long duration, boolean silent, boolean ipBased) {
        
        Instant expiresAt = duration == null || duration < 0 ? null : TimeUtil.calculateExpiry(duration);
        
        PunishmentType type;
        if (ipBased) {
            type = PunishmentType.IP_MUTE;
        } else if (expiresAt == null) {
            type = PunishmentType.MUTE;
        } else {
            type = PunishmentType.TEMP_MUTE;
        }
        
        if (reason == null || reason.isEmpty()) {
            reason = plugin.getConfigManager().getDefaultMuteReason();
        }
        
        Punishment punishment = new Punishment(
                type, targetUUID, targetName, targetIP,
                executorUUID, executorName, reason,
                plugin.getConfigManager().getServerName(),
                expiresAt, silent, ipBased
        );
        
        String finalReason = reason;
        
        return plugin.getDatabaseManager().queryAsync(conn -> {
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
                
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        punishment.setId(rs.getLong(1));
                    }
                }
            }
            
            return punishment;
        }).thenApply(mute -> {
            // Cache the mute
            plugin.getCacheManager().cacheMute(mute);
            
            // Notify the player if online
            Bukkit.getScheduler().runTask(plugin, () -> {
                Player player = Bukkit.getPlayer(targetUUID);
                if (player != null) {
                    notifyMutedPlayer(player, mute);
                }
            });
            
            // Add punishment points
            if (plugin.getConfigManager().getBoolean("points.enabled")) {
                plugin.getPointManager().addPoints(targetUUID,
                    plugin.getConfigManager().getDouble("points.punishment-points.mute", 2));
            }
            
            // Send notifications
            sendMuteNotifications(mute);
            
            plugin.debug("Player " + targetName + " has been muted");
            
            return mute;
        });
    }
    
    /**
     * Unmute a player
     */
    public CompletableFuture<Boolean> unmute(UUID targetUUID, UUID executorUUID, String executorName, String reason) {
        return plugin.getDatabaseManager().queryAsync(conn -> {
            String sql = "UPDATE " + plugin.getDatabaseManager().getTable("punishments") +
                    " SET active = FALSE, removed_at = ?, removed_by_uuid = ?, " +
                    "removed_by_name = ?, remove_reason = ? " +
                    "WHERE target_uuid = ? AND type IN ('mute', 'tempmute') AND active = TRUE";
            
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
                plugin.getCacheManager().invalidateMute(targetUUID);
                
                // Notify player
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Player player = Bukkit.getPlayer(targetUUID);
                    if (player != null) {
                        plugin.getMessagesManager().send(player, "unmute.player-notification");
                    }
                });
            }
            return success;
        });
    }
    
    /**
     * Unmute an IP
     */
    public CompletableFuture<Boolean> unmuteIP(String ip, UUID executorUUID, String executorName, String reason) {
        return plugin.getDatabaseManager().queryAsync(conn -> {
            String sql = "UPDATE " + plugin.getDatabaseManager().getTable("punishments") +
                    " SET active = FALSE, removed_at = ?, removed_by_uuid = ?, " +
                    "removed_by_name = ?, remove_reason = ? " +
                    "WHERE target_ip = ? AND type = 'ipmute' AND active = TRUE";
            
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
                plugin.getCacheManager().invalidateIPMute(ip);
            }
            return success;
        });
    }
    
    /**
     * Check if a player is muted
     */
    public CompletableFuture<Punishment> getActiveMute(UUID uuid) {
        Punishment cached = plugin.getCacheManager().getMute(uuid);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        
        return plugin.getDatabaseManager().queryAsync(conn -> {
            String sql = "SELECT * FROM " + plugin.getDatabaseManager().getTable("punishments") +
                    " WHERE target_uuid = ? AND type IN ('mute', 'tempmute') AND active = TRUE " +
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
        }).thenApply(mute -> {
            if (mute != null) {
                plugin.getCacheManager().cacheMute(mute);
            }
            return mute;
        });
    }
    
    /**
     * Check if an IP is muted
     */
    public CompletableFuture<Punishment> getActiveIPMute(String ip) {
        Punishment cached = plugin.getCacheManager().getIPMute(ip);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        
        return plugin.getDatabaseManager().queryAsync(conn -> {
            String sql = "SELECT * FROM " + plugin.getDatabaseManager().getTable("punishments") +
                    " WHERE target_ip = ? AND type = 'ipmute' AND active = TRUE " +
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
        }).thenApply(mute -> {
            if (mute != null) {
                plugin.getCacheManager().cacheMute(mute);
            }
            return mute;
        });
    }
    
    /**
     * Check if player is muted (sync check from cache only)
     */
    public boolean isMuted(UUID uuid) {
        return plugin.getCacheManager().getMute(uuid) != null;
    }
    
    /**
     * Check if command is blocked while muted
     */
    public boolean isCommandBlocked(String command) {
        List<String> blockedCommands = plugin.getConfigManager().getMuteBlockedCommands();
        String baseCommand = command.split(" ")[0].toLowerCase().replace("/", "");
        
        for (String blocked : blockedCommands) {
            if (baseCommand.equals(blocked.toLowerCase()) || 
                baseCommand.startsWith(blocked.toLowerCase() + " ")) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get all active mutes
     */
    public CompletableFuture<List<Punishment>> getActiveMutes(int page, int perPage) {
        return plugin.getDatabaseManager().queryAsync(conn -> {
            String sql = "SELECT * FROM " + plugin.getDatabaseManager().getTable("punishments") +
                    " WHERE type IN ('mute', 'tempmute', 'ipmute') AND active = TRUE " +
                    "AND (expires_at IS NULL OR expires_at > ?) " +
                    "ORDER BY created_at DESC LIMIT ? OFFSET ?";
            
            List<Punishment> mutes = new ArrayList<>();
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setTimestamp(1, Timestamp.from(Instant.now()));
                stmt.setInt(2, perPage);
                stmt.setInt(3, (page - 1) * perPage);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        mutes.add(parsePunishment(rs));
                    }
                }
            }
            return mutes;
        });
    }
    
    /**
     * Notify a muted player
     */
    private void notifyMutedPlayer(Player player, Punishment mute) {
        plugin.getMessagesManager().sendList(player, "mute.denied",
            MessagesManager.placeholders(
                "reason", mute.getReason(),
                "remaining", mute.isPermanent() ? "Permanent" : TimeUtil.formatDuration(mute.getRemainingTime()),
                "id", String.valueOf(mute.getId())
            )
        );
    }
    
    /**
     * Deny a muted player's chat
     */
    public void denyChat(Player player, Punishment mute) {
        plugin.getMessagesManager().sendList(player, "mute.denied",
            MessagesManager.placeholders(
                "reason", mute.getReason(),
                "remaining", mute.isPermanent() ? "Permanent" : TimeUtil.formatDuration(mute.getRemainingTime()),
                "id", String.valueOf(mute.getId())
            )
        );
    }
    
    /**
     * Send mute notifications
     */
    private void sendMuteNotifications(Punishment mute) {
        if (plugin.getConfigManager().getBoolean("punishments.mute.broadcast")) {
            String message;
            if (mute.isPermanent()) {
                message = plugin.getMessagesManager().get("mute.broadcast.permanent",
                    "player", mute.getTargetName(),
                    "executor", mute.getExecutorName(),
                    "reason", mute.getReason()
                );
            } else {
                message = plugin.getMessagesManager().get("mute.broadcast.temporary",
                    "player", mute.getTargetName(),
                    "executor", mute.getExecutorName(),
                    "reason", mute.getReason(),
                    "duration", TimeUtil.formatDuration(mute.getRemainingTime())
                );
            }
            
            if (mute.isSilent()) {
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
            plugin.getDiscordNotifier().sendMuteNotification(mute);
        }
        
        if (plugin.getTelegramNotifier() != null) {
            plugin.getTelegramNotifier().sendMuteNotification(mute);
        }
    }
    
    private Punishment parsePunishment(ResultSet rs) throws SQLException {
        return new Punishment(
            rs.getLong("id"),
            PunishmentType.fromId(rs.getString("type")),
            rs.getString("target_uuid") != null ? UUID.fromString(rs.getString("target_uuid")) : null,
            rs.getString("target_name"),
            rs.getString("target_ip"),
            UUID.fromString(rs.getString("executor_uuid")),
            rs.getString("executor_name"),
            rs.getString("reason"),
            rs.getString("server"),
            rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toInstant() : null,
            rs.getTimestamp("expires_at") != null ? rs.getTimestamp("expires_at").toInstant() : null,
            rs.getBoolean("active"),
            rs.getTimestamp("removed_at") != null ? rs.getTimestamp("removed_at").toInstant() : null,
            rs.getString("removed_by_uuid") != null ? UUID.fromString(rs.getString("removed_by_uuid")) : null,
            rs.getString("removed_by_name"),
            rs.getString("remove_reason"),
            rs.getBoolean("silent"),
            rs.getBoolean("ip_based")
        );
    }
}
