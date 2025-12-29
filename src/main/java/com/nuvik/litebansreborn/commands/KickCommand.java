package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.utils.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Kick command - /kick <player> [reason]
 */
public class KickCommand implements CommandExecutor {

    private final LiteBansReborn plugin;
    
    public KickCommand(LiteBansReborn plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("litebansreborn.kick")) {
            plugin.getMessagesManager().send(sender, "general.no-permission");
            return true;
        }
        
        if (args.length < 1) {
            plugin.getMessagesManager().send(sender, "kick.usage");
            return true;
        }
        
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            plugin.getMessagesManager().send(sender, "kick.not-online", "player", args[0]);
            return true;
        }
        
        boolean silent = false;
        StringBuilder reasonBuilder = new StringBuilder();
        int startIndex = 1;
        
        if (args.length > 1 && args[1].equalsIgnoreCase("-s")) {
            if (!sender.hasPermission("litebansreborn.silent")) {
                plugin.getMessagesManager().send(sender, "general.no-permission");
                return true;
            }
            silent = true;
            startIndex = 2;
        }
        
        for (int i = startIndex; i < args.length; i++) {
            if (i > startIndex) reasonBuilder.append(" ");
            reasonBuilder.append(args[i]);
        }
        
        String reason = reasonBuilder.length() > 0 ? reasonBuilder.toString() : null;
        
        if (sender instanceof Player && ((Player) sender).getUniqueId().equals(target.getUniqueId())) {
            plugin.getMessagesManager().send(sender, "general.cannot-punish-self");
            return true;
        }
        
        if (target.hasPermission("litebansreborn.bypass.kick")) {
            plugin.getMessagesManager().send(sender, "general.cannot-punish-exempt");
            return true;
        }
        
        UUID executorUUID = PlayerUtil.getExecutorUUID(sender);
        String executorName = PlayerUtil.getExecutorName(sender);
        
        plugin.getKickManager().kick(target, executorUUID, executorName, reason, silent).thenAccept(kick -> {
            plugin.getMessagesManager().send(sender, "kick.success", "player", target.getName());
        });
        
        return true;
    }
}
