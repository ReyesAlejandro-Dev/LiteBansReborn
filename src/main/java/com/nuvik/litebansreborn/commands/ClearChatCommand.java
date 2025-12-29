package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ClearChatCommand implements CommandExecutor {
    private final LiteBansReborn plugin;
    public ClearChatCommand(LiteBansReborn plugin) { this.plugin = plugin; }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("litebansreborn.clearchat")) {
            plugin.getMessagesManager().send(sender, "general.no-permission");
            return true;
        }
        
        // Send 100 blank lines to all players
        for (int i = 0; i < 100; i++) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!player.hasPermission("litebansreborn.bypass.clearchat")) {
                    player.sendMessage("");
                }
            }
        }
        
        // Broadcast the clear message
        String name = sender instanceof Player ? sender.getName() : "Console";
        Bukkit.broadcastMessage(plugin.getMessagesManager().get("utility.clearchat.broadcast", "player", name));
        
        return true;
    }
}
