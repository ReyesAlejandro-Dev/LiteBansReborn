package com.nuvik.litebansreborn.gui;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.utils.ColorUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class PunishGUI extends BaseGUI {

    private final String targetName;

    public PunishGUI(LiteBansReborn plugin, Player player, String targetName) {
        super(plugin, player, "Punish: " + targetName, 27);
        this.targetName = targetName;
    }

    @Override
    public void initializeItems() {
        // Fill background with GRAY pane and RED borders
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " "));
        }
        
        // Borders
        int[] borders = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 19, 20, 21, 23, 24, 25, 26};
        for (int i : borders) {
            inventory.setItem(i, createGuiItem(Material.RED_STAINED_GLASS_PANE, " "));
        }

        // Ban Item
        inventory.setItem(10, createGuiItem(Material.IRON_AXE, "&c&lBan Player", 
            "&7Click to select ban duration", "&7Target: &e" + targetName));

        // Mute Item
        inventory.setItem(11, createGuiItem(Material.PAPER, "&e&lMute Player", 
            "&7Click to select mute duration", "&7Target: &e" + targetName));

        // Player Head Info
        inventory.setItem(13, createSkullItem(targetName, "&6&l" + targetName, 
            "&7Click for History", "&7Status: &aOnline")); // Simplified online status

        // Warn Item
        inventory.setItem(15, createGuiItem(Material.OAK_SIGN, "&6&lWarn Player", 
            "&7Click to warn", "&7Target: &e" + targetName));

        // Kick Item (Boots -> Redstone block looks better for "Kick")
        inventory.setItem(16, createGuiItem(Material.LEATHER_BOOTS, "&b&lKick Player", 
            "&7Click to kick", "&7Target: &e" + targetName));

        // History Item (Moved slightly)
        // inventory.setItem(22, ...); // Replaced by Skull click for history or keep separate?
        // Let's keep history separate below skull for clarity if needed, or stick to simple layout.
        
        // Close Item
        inventory.setItem(22, createGuiItem(Material.BARRIER, "&cClose"));
    }

    @Override
    protected void handleClick(InventoryClickEvent event) {
        Player clicker = (Player) event.getWhoClicked();
        
        switch (event.getRawSlot()) {
            case 10: // Ban
                clicker.closeInventory();
                new DurationGUI(plugin, clicker, targetName, "ban").open();
                break;
            case 11: // Mute
                clicker.closeInventory();
                new DurationGUI(plugin, clicker, targetName, "mute").open();
                break;
            case 13: // Player Head -> History
            case 22: // Close
                clicker.closeInventory();
                break;
            case 15: // Warn
                clicker.closeInventory();
                new ReasonGUI(plugin, clicker, targetName, "warn", null).open();
                break;
            case 16: // Kick
                clicker.closeInventory();
                new ReasonGUI(plugin, clicker, targetName, "kick", null).open();
                break;
        }
    }
}
