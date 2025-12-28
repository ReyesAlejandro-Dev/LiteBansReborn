package com.nuvik.litebansreborn.managers;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.config.MessagesManager;
import com.nuvik.litebansreborn.models.Punishment;
import com.nuvik.litebansreborn.models.PunishmentType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Kick Manager - Handles all kick-related operations
 */
public class KickManager {

    private final LiteBansReborn plugin;
    
    public KickManager(LiteBansReborn plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Kick a player
     */
    public CompletableFuture<Punishment> kick(Player target, UUID executorUUID, String executorName,
                                               String reason, boolean silent) {
        
        if (reason == null || reason.isEmpty()) {
            reason = plugin.getConfigManager().getDefaultKickReason();
        }
        
        UUID targetUUID = target.getUniqueId();
        String targetName = target.getName();
        
        Punishment punishment = new Punishment(
                PunishmentType.KICK, targetUUID, targetName, null,
                executorUUID, executorName, reason,
                plugin.getConfigManager().getServerName(),
                null, silent, false
        );
        
        String finalReason = reason;
        
        // Kick the player immediately (sync)
        List<String> kickScreen = plugin.getMessagesManager().getList("kick.screen",
            MessagesManager.placeholders(
                "reason", finalReason,
                "executor", executorName
            )
        );
        target.kickPlayer(String.join("\n", kickScreen));
        
        // Log to database async
        return plugin.getDatabaseManager().queryAsync(conn -> {
            String sql = "INSERT INTO " + plugin.getDatabaseManager().getTable("punishments") +
                    " (type, target_uuid, target_name, executor_uuid, executor_name, " +
                    "reason, server, created_at, active, silent, ip_based) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, PunishmentType.KICK.getId());
                stmt.setString(2, targetUUID.toString());
                stmt.setString(3, targetName);
                stmt.setString(4, executorUUID.toString());
                stmt.setString(5, executorName);
                stmt.setString(6, finalReason);
                stmt.setString(7, punishment.getServer());
                stmt.setTimestamp(8, Timestamp.from(punishment.getCreatedAt()));
                stmt.setBoolean(9, false); // Kicks are instant, no "active" state
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
        }).thenApply(kick -> {
            // Add punishment points
            if (plugin.getConfigManager().getBoolean("points.enabled")) {
                plugin.getPointManager().addPoints(targetUUID,
                    plugin.getConfigManager().getDouble("points.punishment-points.kick", 3));
            }
            
            // Send notifications
            sendKickNotifications(kick);
            
            plugin.debug("Player " + targetName + " has been kicked");
            
            return kick;
        });
    }
    
    /**
     * Kick all players
     */
    public int kickAll(UUID executorUUID, String executorName, String reason, boolean silent) {
        if (reason == null || reason.isEmpty()) {
            reason = plugin.getConfigManager().getDefaultKickReason();
        }
        
        int count = 0;
        List<String> kickScreen = plugin.getMessagesManager().getList("kick.screen",
            MessagesManager.placeholders(
                "reason", reason,
                "executor", executorName
            )
        );
        String kickMessage = String.join("\n", kickScreen);
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Skip players with bypass permission
            if (player.hasPermission("litebansreborn.bypass.kick")) {
                continue;
            }
            // Skip the executor if they're online
            if (player.getUniqueId().equals(executorUUID)) {
                continue;
            }
            
            player.kickPlayer(kickMessage);
            count++;
        }
        
        // Broadcast
        String finalReason = reason;
        if (plugin.getConfigManager().getBoolean("punishments.kick.broadcast")) {
            String message = plugin.getMessagesManager().get("kick.all.broadcast",
                "executor", executorName,
                "reason", finalReason,
                "count", String.valueOf(count)
            );
            
            // Only staff see the message since everyone is kicked
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission("litebansreborn.notify")) {
                    player.sendMessage(message);
                }
            }
        }
        
        return count;
    }
    
    /**
     * Send kick notifications
     */
    private void sendKickNotifications(Punishment kick) {
        if (plugin.getConfigManager().getBoolean("punishments.kick.broadcast")) {
            String message = plugin.getMessagesManager().get("kick.broadcast",
                "player", kick.getTargetName(),
                "executor", kick.getExecutorName(),
                "reason", kick.getReason()
            );
            
            if (kick.isSilent()) {
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
            plugin.getDiscordNotifier().sendKickNotification(kick);
        }
    }
}
