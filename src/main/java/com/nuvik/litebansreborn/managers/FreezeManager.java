package com.nuvik.litebansreborn.managers;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.config.MessagesManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

/**
 * Freeze Manager - Handles freezing players in place
 */
public class FreezeManager {

    private final LiteBansReborn plugin;
    
    public FreezeManager(LiteBansReborn plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Freeze a player
     */
    public void freeze(Player target, UUID executorUUID, String executorName, String reason, boolean silent) {
        if (reason == null || reason.isEmpty()) {
            reason = plugin.getConfigManager().getDefaultFreezeReason();
        }
        
        UUID targetUUID = target.getUniqueId();
        String targetName = target.getName();
        
        // Add to frozen cache
        plugin.getCacheManager().freezePlayer(targetUUID, reason);
        
        // Notify the player
        plugin.getMessagesManager().sendList(target, "freeze.frozen-message",
            MessagesManager.placeholders(
                "reason", reason,
                "executor", executorName
            )
        );
        
        // Broadcast
        if (plugin.getConfigManager().getBoolean("punishments.freeze.broadcast")) {
            String message = plugin.getMessagesManager().get("freeze.broadcast",
                "player", targetName,
                "executor", executorName
            );
            
            if (silent) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.hasPermission("litebansreborn.notify")) {
                        player.sendMessage(plugin.getMessagesManager().get("broadcasts.silent-prefix") + message);
                    }
                }
            } else {
                Bukkit.broadcastMessage(message);
            }
        }
        
        // Start reminder task
        startFreezeReminder(targetUUID);
        
        plugin.debug("Player " + targetName + " has been frozen");
    }
    
    /**
     * Unfreeze a player
     */
    public boolean unfreeze(UUID targetUUID, UUID executorUUID, String executorName) {
        if (!plugin.getCacheManager().isFrozen(targetUUID)) {
            return false;
        }
        
        plugin.getCacheManager().unfreezePlayer(targetUUID);
        
        // Notify the player if online
        Player target = Bukkit.getPlayer(targetUUID);
        if (target != null) {
            plugin.getMessagesManager().send(target, "unfreeze.unfrozen-message",
                "executor", executorName
            );
        }
        
        // Broadcast
        String targetName = target != null ? target.getName() : "Unknown";
        if (plugin.getConfigManager().getBoolean("punishments.freeze.broadcast")) {
            String message = plugin.getMessagesManager().get("unfreeze.broadcast",
                "player", targetName,
                "executor", executorName
            );
            Bukkit.broadcastMessage(message);
        }
        
        plugin.debug("Player " + targetName + " has been unfrozen");
        
        return true;
    }
    
    /**
     * Check if a player is frozen
     */
    public boolean isFrozen(UUID uuid) {
        return plugin.getCacheManager().isFrozen(uuid);
    }
    
    /**
     * Get freeze reason
     */
    public String getFreezeReason(UUID uuid) {
        return plugin.getCacheManager().getFreezeReason(uuid);
    }
    
    /**
     * Get all frozen players
     */
    public Set<UUID> getFrozenPlayers() {
        return plugin.getCacheManager().getFrozenPlayers();
    }
    
    /**
     * Handle frozen player disconnect (ban for leaving while frozen)
     */
    public void handleFrozenDisconnect(Player player) {
        UUID uuid = player.getUniqueId();
        String name = player.getName();
        
        if (!isFrozen(uuid)) {
            return;
        }
        
        // Unfreeze (since they left)
        plugin.getCacheManager().unfreezePlayer(uuid);
        
        // Ban them for leaving while frozen
        if (plugin.getConfigManager().getBoolean("punishments.freeze.ban-on-logout", true)) {
            plugin.getBanManager().ban(
                uuid, name, null,
                UUID.nameUUIDFromBytes("AutoMod".getBytes()), "AutoMod",
                "Disconnected while frozen",
                null, // permanent or configurable
                false, false
            );
            
            String message = plugin.getMessagesManager().get("freeze.logout-ban",
                "player", name
            );
            Bukkit.broadcastMessage(message);
        }
    }
    
    /**
     * Send periodic reminders to frozen players
     */
    private void startFreezeReminder(UUID uuid) {
        // Send reminder every 5 seconds
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (!plugin.getCacheManager().isFrozen(uuid)) {
                task.cancel();
                return;
            }
            
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                task.cancel();
                return;
            }
            
            String reason = plugin.getCacheManager().getFreezeReason(uuid);
            plugin.getMessagesManager().send(player, "freeze.reminder",
                "reason", reason != null ? reason : "Frozen"
            );
        }, 100L, 100L); // 5 seconds
    }
    
    /**
     * Block frozen player action
     */
    public void blockAction(Player player, String action) {
        String message;
        switch (action) {
            case "movement":
                message = plugin.getMessagesManager().get("freeze.blocked.movement");
                break;
            case "chat":
                message = plugin.getMessagesManager().get("freeze.blocked.chat");
                break;
            case "command":
                message = plugin.getMessagesManager().get("freeze.blocked.command");
                break;
            case "interact":
                message = plugin.getMessagesManager().get("freeze.blocked.interact");
                break;
            default:
                message = plugin.getMessagesManager().get("freeze.reminder");
        }
        player.sendMessage(message);
    }
}
