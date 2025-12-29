package com.nuvik.litebansreborn.managers;

import com.nuvik.litebansreborn.LiteBansReborn;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Manages Ghost Mutes (Shadow Mutes)
 * Players who are ghost muted see their own messages, but no one else does.
 */
public class GhostMuteManager {

    private final LiteBansReborn plugin;
    private final Set<UUID> ghostMutedPlayers = new HashSet<>();

    public GhostMuteManager(LiteBansReborn plugin) {
        this.plugin = plugin;
    }

    /**
     * Check if a player is ghost muted
     */
    public boolean isGhostMuted(UUID uuid) {
        return ghostMutedPlayers.contains(uuid);
    }
    
    /**
     * Toggle ghost mute for a player
     * @return true if now muted, false if unmuted
     */
    public boolean toggleGhostMute(UUID uuid) {
        if (ghostMutedPlayers.contains(uuid)) {
            ghostMutedPlayers.remove(uuid);
            return false;
        } else {
            ghostMutedPlayers.add(uuid);
            return true;
        }
    }
    
    /**
     * Add a player to ghost mute
     */
    public void addGhostMute(UUID uuid) {
        ghostMutedPlayers.add(uuid);
    }
    
    /**
     * Remove a player from ghost mute
     */
    public void removeGhostMute(UUID uuid) {
        ghostMutedPlayers.remove(uuid);
    }
    
    /**
     * Process a chat message from a ghost muted player
     * Returns true if handled (should cancel event), false otherwise
     */
    public boolean processGhostChat(Player player, String message, Set<Player> recipients) {
        if (!isGhostMuted(player.getUniqueId())) {
            return false;
        }
        
        // If config says don't show to self, we just cancel normally (like a mute)
        // But the point of Ghost Mute is to show to self.
        boolean showToSelf = plugin.getConfigManager().getBoolean("ghost-mute.show-to-self", true);
        
        // Remove everyone from recipients
        recipients.clear();
        
        // Add back the player if show-to-self is true
        if (showToSelf) {
            recipients.add(player);
        }
        
        // Add staff who can view ghost mutes
        if (plugin.getConfigManager().getBoolean("ghost-mute.staff-can-see", true)) {
            String prefix = plugin.getConfigManager().getString("ghost-mute.staff-prefix", "&7[👻] ");
            // We can't easily modify the message JUST for staff in the same event without NMS or complex logic
            // providing a "recipients" list only controls WHO gets it, not WHAT they get.
            // So we will send a manual message to staff and exclude them from the normal event recipient list (which we already cleared)
            
            String format = String.format(player.getDisplayName() + ": " + message); // Simple format for now
            // Better: use the event's format if possible, but async chat is tricky.
            
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                if (p.hasPermission("litebansreborn.ghostmute.view") && !p.equals(player)) {
                    p.sendMessage(com.nuvik.litebansreborn.utils.ColorUtil.translate(prefix + format));
                }
            }
        }
        
        // Returning FALSE because we are NOT cancelling the event entirely,
        // we are just manipulating recipients so ONLY the player (and maybe staff manually) see it.
        // If we cancel the event, the player won't see their own message in many chat plugins.
        // By clearing recipients and adding player back, we achieve the "Shadow" effect.
        
        return false; 
    }
}
