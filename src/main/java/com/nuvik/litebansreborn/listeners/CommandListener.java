package com.nuvik.litebansreborn.listeners;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.models.Punishment;
import com.nuvik.litebansreborn.utils.PlayerUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

/**
 * Listener for command events - blocks commands for muted/frozen players
 */
public class CommandListener implements Listener {

    private final LiteBansReborn plugin;
    
    public CommandListener(LiteBansReborn plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String command = event.getMessage().toLowerCase();
        
        // Check for frozen player
        if (plugin.getFreezeManager().isFrozen(player.getUniqueId())) {
            if (plugin.getConfigManager().getBoolean("punishments.freeze.block-commands")) {
                // Allow some essential commands
                if (!isAllowedWhileFrozen(command)) {
                    event.setCancelled(true);
                    plugin.getFreezeManager().blockAction(player, "command");
                    return;
                }
            }
        }
        
        // Check for mute bypass
        if (player.hasPermission("litebansreborn.bypass.mute")) {
            return;
        }
        
        // Check if command is blocked while muted
        if (plugin.getMuteManager().isCommandBlocked(command)) {
            // Check for mute
            Punishment mute = plugin.getCacheManager().getMute(player.getUniqueId());
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
        }
    }
    
    /**
     * Check if a command is allowed while frozen
     */
    private boolean isAllowedWhileFrozen(String command) {
        String baseCommand = command.split(" ")[0].replace("/", "").toLowerCase();
        
        // Allow these commands even when frozen
        return baseCommand.equals("appeal") ||
               baseCommand.equals("helpop") ||
               baseCommand.equals("msg") ||
               baseCommand.equals("tell") ||
               baseCommand.equals("r") ||
               baseCommand.equals("reply");
    }
}
