package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.utils.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Unban command - /unban <player>
 */
public class UnbanCommand implements CommandExecutor, TabCompleter {

    private final LiteBansReborn plugin;
    
    public UnbanCommand(LiteBansReborn plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("litebansreborn.unban")) {
            plugin.getMessagesManager().send(sender, "general.no-permission");
            return true;
        }
        
        if (args.length < 1) {
            plugin.getMessagesManager().send(sender, "unban.usage");
            return true;
        }
        
        String targetName = args[0];
        OfflinePlayer target = PlayerUtil.getOfflinePlayer(targetName);
        
        if (target == null) {
            plugin.getMessagesManager().send(sender, "general.player-not-found", "player", targetName);
            return true;
        }
        
        UUID targetUUID = target.getUniqueId();
        targetName = target.getName() != null ? target.getName() : targetName;
        
        UUID executorUUID = PlayerUtil.getExecutorUUID(sender);
        String executorName = PlayerUtil.getExecutorName(sender);
        
        String finalTargetName = targetName;
        plugin.getBanManager().unban(targetUUID, executorUUID, executorName, "Unbanned").thenAccept(success -> {
            if (success) {
                plugin.getMessagesManager().send(sender, "unban.success", "player", finalTargetName);
                
                // Broadcast
                String message = plugin.getMessagesManager().get("unban.broadcast",
                        "player", finalTargetName,
                        "executor", executorName);
                Bukkit.broadcastMessage(message);
            } else {
                plugin.getMessagesManager().send(sender, "unban.not-banned", "player", finalTargetName);
            }
        });
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // Could return list of banned players, but that's expensive
        return new ArrayList<>();
    }
}
