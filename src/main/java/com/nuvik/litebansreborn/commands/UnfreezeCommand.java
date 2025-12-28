package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.utils.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Unfreeze command - /unfreeze <player>
 */
public class UnfreezeCommand implements CommandExecutor {

    private final LiteBansReborn plugin;
    
    public UnfreezeCommand(LiteBansReborn plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("litebansreborn.unfreeze")) {
            plugin.getMessagesManager().send(sender, "general.no-permission");
            return true;
        }
        
        if (args.length < 1) {
            plugin.getMessagesManager().send(sender, "unfreeze.usage");
            return true;
        }
        
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            plugin.getMessagesManager().send(sender, "general.player-not-found", "player", args[0]);
            return true;
        }
        
        if (!plugin.getFreezeManager().isFrozen(target.getUniqueId())) {
            plugin.getMessagesManager().send(sender, "unfreeze.not-frozen", "player", target.getName());
            return true;
        }
        
        String executorName = PlayerUtil.getExecutorName(sender);
        
        plugin.getFreezeManager().unfreeze(target.getUniqueId(), 
            PlayerUtil.getExecutorUUID(sender), executorName);
        
        plugin.getMessagesManager().send(sender, "unfreeze.success", "player", target.getName());
        
        return true;
    }
}
