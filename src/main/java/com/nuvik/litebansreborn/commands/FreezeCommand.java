package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.utils.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Freeze command - /freeze <player> [reason]
 */
public class FreezeCommand implements CommandExecutor {

    private final LiteBansReborn plugin;
    
    public FreezeCommand(LiteBansReborn plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("litebansreborn.freeze")) {
            plugin.getMessagesManager().send(sender, "general.no-permission");
            return true;
        }
        
        if (args.length < 1) {
            plugin.getMessagesManager().send(sender, "freeze.usage");
            return true;
        }
        
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            plugin.getMessagesManager().send(sender, "general.player-not-found", "player", args[0]);
            return true;
        }
        
        StringBuilder reasonBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i > 1) reasonBuilder.append(" ");
            reasonBuilder.append(args[i]);
        }
        String reason = reasonBuilder.length() > 0 ? reasonBuilder.toString() : null;
        
        if (sender instanceof Player && ((Player) sender).getUniqueId().equals(target.getUniqueId())) {
            plugin.getMessagesManager().send(sender, "general.cannot-punish-self");
            return true;
        }
        
        if (target.hasPermission("litebansreborn.bypass.freeze")) {
            plugin.getMessagesManager().send(sender, "general.cannot-punish-exempt");
            return true;
        }
        
        if (plugin.getFreezeManager().isFrozen(target.getUniqueId())) {
            plugin.getMessagesManager().send(sender, "general.already-punished", "punishment", "frozen");
            return true;
        }
        
        String executorName = PlayerUtil.getExecutorName(sender);
        
        plugin.getFreezeManager().freeze(target, PlayerUtil.getExecutorUUID(sender), 
            executorName, reason, false);
        
        plugin.getMessagesManager().send(sender, "freeze.success", "player", target.getName());
        
        return true;
    }
}
