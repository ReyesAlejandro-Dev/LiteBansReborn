package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StaffChatCommand implements CommandExecutor {
    private final LiteBansReborn plugin;
    public StaffChatCommand(LiteBansReborn plugin) { this.plugin = plugin; }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("litebansreborn.staffchat")) {
            plugin.getMessagesManager().send(sender, "general.no-permission");
            return true;
        }
        
        if (!(sender instanceof Player)) {
            plugin.getMessagesManager().send(sender, "general.players-only");
            return true;
        }
        
        Player player = (Player) sender;
        plugin.getCacheManager().toggleStaffChat(player.getUniqueId());
        
        if (plugin.getCacheManager().isStaffChatEnabled(player.getUniqueId())) {
            plugin.getMessagesManager().send(sender, "utility.staffchat.enabled");
        } else {
            plugin.getMessagesManager().send(sender, "utility.staffchat.disabled");
        }
        
        return true;
    }
}
