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
            return;
        }
        
        // Check for mute
        Punishment mute = plugin.getCacheManager().getMute(uuid);
        if (mute != null && mute.isActiveAndValid()) {
            event.setCancelled(true);
            plugin.getMuteManager().denyChat(player, mute);
            return;
        }
        
        // Check for IP mute
        String ip = PlayerUtil.getPlayerIP(player);
        if (ip != null) {
            Punishment ipMute = plugin.getCacheManager().getIPMute(ip);
            if (ipMute != null && ipMute.isActiveAndValid()) {
                event.setCancelled(true);
                plugin.getMuteManager().denyChat(player, ipMute);
                return;
            }
        }
        
        // Async check in database if not in cache
        plugin.getMuteManager().getActiveMute(uuid).thenAccept(dbMute -> {
            if (dbMute != null && dbMute.isActiveAndValid()) {
                // Cancel the message (it's too late, but cache for next time)
                plugin.getCacheManager().cacheMute(dbMute);
            }
        });
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
}
