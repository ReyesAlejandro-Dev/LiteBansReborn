package com.nuvik.litebansreborn.gui;

import com.nuvik.litebansreborn.LiteBansReborn;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public class DurationGUI extends BaseGUI {

    private final String targetName;
    private final String Type; // "ban" or "mute"

    public DurationGUI(LiteBansReborn plugin, Player player, String targetName, String type) {
        super(plugin, player, "Select Duration: " + type.toUpperCase(), 27);
        this.targetName = targetName;
        this.Type = type;
    }

    @Override
    public void initializeItems() {
        // Fill background with GRAY pane and coloured borders based on type
        Material borderMat = Type.equals("ban") ? Material.RED_STAINED_GLASS_PANE : Material.YELLOW_STAINED_GLASS_PANE;
        
        for (int i = 0; i < 27; i++) {
             inventory.setItem(i, createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " "));
        }
        
        // Borders
        int[] borders = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 19, 20, 21, 23, 24, 25, 26};
        for (int i : borders) {
            inventory.setItem(i, createGuiItem(borderMat, " "));
        }

        // 30 Minutes
        inventory.setItem(10, createGuiItem(Material.FEATHER, "&e30 Minutes", "&7Click to apply"));
        
        // 1 Hour
        inventory.setItem(11, createGuiItem(Material.COAL, "&e1 Hour", "&7Click to apply"));

        // 1 Day
        inventory.setItem(12, createGuiItem(Material.IRON_INGOT, "&61 Day", "&7Click to apply"));
        
        // 1 Week
        inventory.setItem(13, createGuiItem(Material.GOLD_INGOT, "&61 Week", "&7Click to apply"));
        
        // 1 Month
        inventory.setItem(14, createGuiItem(Material.DIAMOND, "&c1 Month", "&7Click to apply"));

        // Permanent
        inventory.setItem(15, createGuiItem(Material.BEDROCK, "&4&lPERMANENT", "&7Click to apply permanent " + Type));
        
        // Back
        inventory.setItem(22, createGuiItem(Material.ARROW, "&cBack", "&7Return to main menu"));
    }

    @Override
    protected void handleClick(InventoryClickEvent event) {
        Player clicker = (Player) event.getWhoClicked();
        
        String duration = "";
        
        switch (event.getRawSlot()) {
            case 10: duration = "30m"; break;
            case 11: duration = "1h"; break;
            case 12: duration = "1d"; break;
            case 13: duration = "1w"; break;
            case 14: duration = "1mo"; break;
            case 15: duration = ""; break; // Permanent
            case 22: // Back
                clicker.closeInventory();
                new PunishGUI(plugin, clicker, targetName).open();
                return;
            default: return;
        }
        
        clicker.closeInventory();
        // Open Reason Selector instead of executing immediately
        new ReasonGUI(plugin, clicker, targetName, Type.toLowerCase(), duration).open();
    }
}
