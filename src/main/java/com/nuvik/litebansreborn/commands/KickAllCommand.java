package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.utils.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class KickAllCommand implements CommandExecutor {
    private final LiteBansReborn plugin;
    public KickAllCommand(LiteBansReborn plugin) { this.plugin = plugin; }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("litebansreborn.kickall")) {
            plugin.getMessagesManager().send(sender, "general.no-permission");
            return true;
        }
        
        StringBuilder reasonBuilder = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) reasonBuilder.append(" ");
            reasonBuilder.append(args[i]);
        }
        String reason = reasonBuilder.length() > 0 ? reasonBuilder.toString() : null;
        
        int count = plugin.getKickManager().kickAll(
            PlayerUtil.getExecutorUUID(sender),
            PlayerUtil.getExecutorName(sender),
            reason, false);
        
        plugin.getMessagesManager().send(sender, "kick.all.success", "count", String.valueOf(count));
        return true;
    }
}
