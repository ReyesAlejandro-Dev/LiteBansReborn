package com.nuvik.litebansreborn.listeners;

import com.nuvik.litebansreborn.LiteBansReborn;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;

/**
 * Listener for freeze events - blocks frozen player actions
 */
public class FreezeListener implements Listener {

    private final LiteBansReborn plugin;
    
    public FreezeListener(LiteBansReborn plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Block movement for frozen players
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMove(PlayerMoveEvent event) {
        if (!plugin.getConfigManager().getBoolean("punishments.freeze.block-movement")) {
            return;
        }
        
        Player player = event.getPlayer();
        if (!plugin.getFreezeManager().isFrozen(player.getUniqueId())) {
            return;
        }
        
        Location from = event.getFrom();
        Location to = event.getTo();
        
        if (to == null) return;
        
        // Allow looking around, but not moving
        if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
            event.setTo(new Location(from.getWorld(), from.getX(), from.getY(), from.getZ(),
                    to.getYaw(), to.getPitch()));
        }
    }
    
    /**
     * Block interactions for frozen players
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (!plugin.getConfigManager().getBoolean("punishments.freeze.block-interactions")) {
            return;
        }
        
        Player player = event.getPlayer();
        if (plugin.getFreezeManager().isFrozen(player.getUniqueId())) {
            event.setCancelled(true);
            plugin.getFreezeManager().blockAction(player, "interact");
        }
    }
    
    /**
     * Block entity interactions for frozen players
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        if (!plugin.getConfigManager().getBoolean("punishments.freeze.block-interactions")) {
            return;
        }
        
        Player player = event.getPlayer();
        if (plugin.getFreezeManager().isFrozen(player.getUniqueId())) {
            event.setCancelled(true);
            plugin.getFreezeManager().blockAction(player, "interact");
        }
    }
    
    /**
     * Block block breaking for frozen players
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (plugin.getFreezeManager().isFrozen(player.getUniqueId())) {
            event.setCancelled(true);
            plugin.getFreezeManager().blockAction(player, "interact");
        }
    }
    
    /**
     * Block block placing for frozen players
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (plugin.getFreezeManager().isFrozen(player.getUniqueId())) {
            event.setCancelled(true);
            plugin.getFreezeManager().blockAction(player, "interact");
        }
    }
    
    /**
     * Block inventory interactions for frozen players
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        if (plugin.getFreezeManager().isFrozen(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }
    
    /**
     * Block inventory opening for frozen players
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        Player player = (Player) event.getPlayer();
        if (plugin.getFreezeManager().isFrozen(player.getUniqueId())) {
            event.setCancelled(true);
            plugin.getFreezeManager().blockAction(player, "interact");
        }
    }
    
    /**
     * Block item dropping for frozen players
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (plugin.getFreezeManager().isFrozen(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }
    
    /**
     * Block item pickup for frozen players
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        
        Player player = (Player) event.getEntity();
        if (plugin.getFreezeManager().isFrozen(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }
    
    /**
     * Block damage to frozen players
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        
        Player player = (Player) event.getEntity();
        if (plugin.getFreezeManager().isFrozen(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }
    
    /**
     * Block frozen players from dealing damage
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        
        Player player = (Player) event.getDamager();
        if (plugin.getFreezeManager().isFrozen(player.getUniqueId())) {
            event.setCancelled(true);
            plugin.getFreezeManager().blockAction(player, "interact");
        }
    }
}
