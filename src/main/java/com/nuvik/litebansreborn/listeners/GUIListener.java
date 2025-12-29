package com.nuvik.litebansreborn.listeners;

import com.nuvik.litebansreborn.LiteBansReborn;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Listener for GUI events
 */
public class GUIListener implements Listener {

    private final LiteBansReborn plugin;
    
    public GUIListener(LiteBansReborn plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        InventoryHolder holder = event.getInventory().getHolder();
        
        // Check if this is one of our GUIs
        if (holder instanceof GUIHolder) {
            event.setCancelled(true);
            
            if (event.getCurrentItem() == null) return;
            
            GUIHolder guiHolder = (GUIHolder) holder;
            guiHolder.handleClick(event);
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        InventoryHolder holder = event.getInventory().getHolder();
        
        if (holder instanceof GUIHolder) {
            GUIHolder guiHolder = (GUIHolder) holder;
            guiHolder.handleClose(event);
        }
    }
    
    /**
     * Interface for GUI holders
     */
    public interface GUIHolder extends InventoryHolder {
        void handleClick(InventoryClickEvent event);
        void handleClose(InventoryCloseEvent event);
    }
}
