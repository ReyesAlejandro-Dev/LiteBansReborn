package com.nuvik.litebansreborn.gui;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.utils.ColorUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public class ReasonGUI extends BaseGUI {

    private final String targetName;
    private final String type; // ban, mute, warn, kick
    private final String duration; // Can be null or empty

    public ReasonGUI(LiteBansReborn plugin, Player player, String targetName, String type, String duration) {
        super(plugin, player, "Select Reason: " + type.toUpperCase(), 27);
        this.targetName = targetName;
        this.type = type;
        this.duration = duration;
    }

    @Override
    public void initializeItems() {
        // Fill background
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " "));
        }
        
        String rollbackLore = (type.equals("ban") && plugin.getConfigManager().getBoolean("rollback.gui-button", true)) 
                ? "&eRight-Click to Ban & Rollback" : "";
        
        // Reason Items
        addItem(10, Material.DIAMOND_SWORD, "&c&lHacking", "&7Unfair Advantage/Cheating", rollbackLore);
        addItem(11, Material.TNT, "&c&lGriefing", "&7Destroying property", rollbackLore);
        addItem(12, Material.PAPER, "&e&lChat Violation", "&7Spam, caps, flood", rollbackLore);
        addItem(13, Material.LAVA_BUCKET, "&6&lToxicity", "&7Harassment, insults", rollbackLore);
        addItem(14, Material.GOLD_NUGGET, "&b&lAdvertising", "&7Sharing other server IPs", rollbackLore);
        addItem(15, Material.STRUCTURE_VOID, "&5&lBug Abuse", "&7Exploiting glitches", rollbackLore);
        
        // Custom Reason
        inventory.setItem(16, createGuiItem(Material.NAME_TAG, "&a&lCustom Reason", "&7Click to type in chat"));

        // Back
        inventory.setItem(22, createGuiItem(Material.ARROW, "&cBack", "&7Return to previous menu"));
    }
    
    private void addItem(int slot, Material mat, String name, String desc, String extra) {
        if (extra != null && !extra.isEmpty()) {
            inventory.setItem(slot, createGuiItem(mat, name, desc, extra));
        } else {
            inventory.setItem(slot, createGuiItem(mat, name, desc));
        }
    }

    @Override
    protected void handleClick(InventoryClickEvent event) {
        Player clicker = (Player) event.getWhoClicked();
        String reason = "";
        
        boolean rollback = event.isRightClick() && type.equals("ban") && plugin.getConfigManager().getBoolean("rollback.gui-button", true);

        switch (event.getRawSlot()) {
            case 10: reason = "Hacking / Cheating"; break;
            case 11: reason = "Griefing"; break;
            case 12: reason = "Chat Violation"; break;
            case 13: reason = "Toxicity"; break;
            case 14: reason = "Advertising"; break;
            case 15: reason = "Bug Abuse"; break;
            case 16: // Custom
                clicker.closeInventory();
                clicker.sendMessage(ColorUtil.translate("&aType the reason in chat for &e" + type + " &aon &e" + targetName));
                
                String baseCmd = getCommandPrefix();
                String durStr = (duration != null && !duration.isEmpty()) ? " " + duration : "";
                
                clicker.sendMessage(ColorUtil.translate("&7Command suggestion: &f/" + baseCmd + " " + targetName + durStr + " <reason>"));
                return;
            case 22: // Back
                clicker.closeInventory();
                if (type.equals("ban") || type.equals("mute")) {
                   new DurationGUI(plugin, clicker, targetName, type).open();
                } else {
                   new PunishGUI(plugin, clicker, targetName).open();
                }
                return;
            default: return;
        }

        // Execute logic
        clicker.closeInventory();
        executePunishment(clicker, reason, rollback);
    }
    
    private String getCommandPrefix() {
        if (type.equals("ban") || type.equals("mute")) {
             if (duration == null || duration.isEmpty()) return type; // permanent ban/mute
             return "temp" + type; // tempban/tempmute
        }
        return type; // warn/kick
    }

    private void executePunishment(Player executor, String reason, boolean rollback) {
        String cmd = getCommandPrefix();
        String finalCmd;
        
        if (duration != null && !duration.isEmpty()) {
            finalCmd = cmd + " " + targetName + " " + duration + " " + reason;
        } else {
            finalCmd = cmd + " " + targetName + " " + reason;
        }
        
        executor.performCommand(finalCmd);
        
        if (rollback) {
             String rbCmd = plugin.getConfigManager().getString("rollback.command", "co rollback u:{player} t:24h #silent")
                        .replace("{player}", targetName)
                        .replace("{time}", plugin.getConfigManager().getString("rollback.time", "24h"));
                
             // Execute from console as rollback usually requires op/console
             plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), rbCmd);
             executor.sendMessage(ColorUtil.translate("&a&l[+] &aAuto-Rollback initiated for " + targetName));
        }
    }
}
