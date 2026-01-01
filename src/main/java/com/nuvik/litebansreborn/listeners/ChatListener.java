package com.nuvik.litebansreborn.listeners;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.models.Punishment;
import com.nuvik.litebansreborn.utils.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.UUID;

/**
 * Listener for chat events - handles mutes and staff chat
 */
public class ChatListener implements Listener {

    private final LiteBansReborn plugin;
    
    public ChatListener(LiteBansReborn plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String message = event.getMessage();
        
        // Add to snapshot buffer (capture everything attempted)
        if (plugin.getSnapshotManager() != null) {
            plugin.getSnapshotManager().addMessage(player, message);
        }
        
        // Check for Ghost Mute (Shadow Mute) - Priority highest to fool the player
        if (plugin.getConfigManager().getBoolean("ghost-mute.enabled", true) && 
            plugin.getGhostMuteManager() != null && 
            plugin.getGhostMuteManager().isGhostMuted(uuid)) {
            
            plugin.getGhostMuteManager().processGhostChat(player, message, event.getRecipients());
            return; // Don't check other mutes, let them think they are talking
        }

        // Check for staff chat prefix
        if (plugin.getConfigManager().getBoolean("staff-chat.enabled")) {
            String mode = plugin.getConfigManager().getString("staff-chat.mode", "toggle");
            String prefix = plugin.getConfigManager().getString("staff-chat.prefix", "#");
            
            // Prefix mode
            if (mode.equalsIgnoreCase("prefix") && message.startsWith(prefix)) {
                if (player.hasPermission("litebansreborn.staffchat")) {
                    event.setCancelled(true);
                    sendStaffChat(player, message.substring(prefix.length()));
                    return;
                }
            }
            
            // Toggle mode
            if (mode.equalsIgnoreCase("toggle") && plugin.getCacheManager().isStaffChatEnabled(uuid)) {
                event.setCancelled(true);
                sendStaffChat(player, message);
                return;
            }
        }
        
        // Check for frozen player
        if (plugin.getFreezeManager().isFrozen(uuid)) {
            if (plugin.getConfigManager().getBoolean("punishments.freeze.block-chat")) {
                event.setCancelled(true);
                plugin.getFreezeManager().blockAction(player, "chat");
                return;
            }
        }
        
        // Check for chat mute
        if (plugin.isChatMuted()) {
            if (!player.hasPermission("litebansreborn.bypass.mutechat")) {
                event.setCancelled(true);
                plugin.getMessagesManager().send(player, "utility.mutechat.denied");
                return;
            }
        }
        
        // Check for mute bypass
        if (player.hasPermission("litebansreborn.bypass.mute")) {
            // Still check chat filter even with bypass
            checkChatFilter(event, player, message);
            return;
        }
        
        // Check for mute
        Punishment mute = plugin.getCacheManager().getMute(uuid);
        if (mute != null && mute.isActiveAndValid()) {
            event.setCancelled(true);
            plugin.getMuteManager().denyChat(player, mute);
            return;
        }
        
        // Check for IP mute (Priority check before filter)
        String ip = PlayerUtil.getPlayerIP(player);
        if (ip != null) {
            Punishment ipMute = plugin.getCacheManager().getIPMute(ip);
            if (ipMute != null && ipMute.isActiveAndValid()) {
                event.setCancelled(true);
                plugin.getMuteManager().denyChat(player, ipMute);
                return;
            }
        }
        
        // Check database async if not in cache (for future messages)
        // Can't cancel current event safely from async callback, but helps sync cache
        plugin.getMuteManager().getActiveMute(uuid).thenAccept(dbMute -> {
            if (dbMute != null && dbMute.isActiveAndValid()) {
                // Sync back to main thread for thread safety when modifying cache
                Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.getCacheManager().cacheMute(dbMute);
                });
            }
        }).exceptionally(ex -> {
            plugin.debug("Error checking mute from DB: " + ex.getMessage());
            return null;
        });
        
        // Chat Filter (v4.5)
        checkChatFilter(event, player, message);
    }
    
    /**
     * Send a message to staff chat
     */
    private void sendStaffChat(Player sender, String message) {
        String format = plugin.getMessagesManager().get("utility.staffchat.format",
                "player", sender.getName(),
                "message", message
        );
        
        // Send to all staff
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("litebansreborn.staffchat")) {
                player.sendMessage(format);
            }
        }
        
        // Log to console
        Bukkit.getConsoleSender().sendMessage(format);
    }
    
    /**
     * Check chat filter (v4.5)
     */
    private void checkChatFilter(AsyncPlayerChatEvent event, Player player, String message) {
        if (plugin.getChatFilterManager() == null || !plugin.getChatFilterManager().isEnabled()) {
            return;
        }
        
        var result = plugin.getChatFilterManager().filterMessage(player, message);
        
        if (result.isBlocked()) {
            event.setCancelled(true);
            
            // Send warning message using configurable messages
            String warnKey = switch (result.getReason()) {
                case SPAM, FLOOD, CHAR_SPAM -> "filter.spam";
                case CAPS -> "filter.caps";
                case ADVERTISEMENT, LINK -> "filter.advertisement";
                case PROFANITY, BLOCKED_WORD -> "filter.warn";
                default -> "filter.blocked";
            };
            
            plugin.getMessagesManager().send(player, warnKey, "reason", result.getReason().name());
            
            // Handle violation (count warnings, auto-mute)
            plugin.getChatFilterManager().handleViolation(player, result.getReason());
            return;
        }
        
        // If message was filtered (not blocked but modified)
        if (result.getFilteredMessage() != null && !result.getFilteredMessage().equals(message)) {
            event.setMessage(result.getFilteredMessage());
        }
    }
}
