package com.nuvik.litebansreborn.listeners;

import com.nuvik.litebansreborn.LiteBansReborn;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener for player quit events
 */
public class PlayerQuitListener implements Listener {

    private final LiteBansReborn plugin;
    
    public PlayerQuitListener(LiteBansReborn plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Handle frozen player disconnect
        if (plugin.getFreezeManager().isFrozen(player.getUniqueId())) {
            plugin.getFreezeManager().handleFrozenDisconnect(player);
        }
        
        // Clear staff chat if enabled
        plugin.getCacheManager().disableStaffChat(player.getUniqueId());
        
        // Invalidate player cache
        plugin.getCacheManager().invalidatePlayer(player.getUniqueId());
    }
}
