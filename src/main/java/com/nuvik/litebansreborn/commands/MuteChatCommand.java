package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MuteChatCommand implements CommandExecutor {
    private final LiteBansReborn plugin;
    public MuteChatCommand(LiteBansReborn plugin) { this.plugin = plugin; }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("litebansreborn.mutechat")) {
            plugin.getMessagesManager().send(sender, "general.no-permission");
            return true;
        }
        
        plugin.setChatMuted(!plugin.isChatMuted());
        
        String name = sender instanceof Player ? sender.getName() : "Console";
        
        if (plugin.isChatMuted()) {
            Bukkit.broadcastMessage(plugin.getMessagesManager().get("utility.mutechat.muted", "player", name));
        } else {
            Bukkit.broadcastMessage(plugin.getMessagesManager().get("utility.mutechat.unmuted", "player", name));
        }
        
        return true;
    }
}
