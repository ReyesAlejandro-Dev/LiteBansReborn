package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.utils.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.UUID;

public class UnmuteCommand implements CommandExecutor {
    private final LiteBansReborn plugin;
    public UnmuteCommand(LiteBansReborn plugin) { this.plugin = plugin; }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("litebansreborn.unmute")) {
            plugin.getMessagesManager().send(sender, "general.no-permission");
            return true;
        }
        
        if (args.length < 1) {
            plugin.getMessagesManager().send(sender, "unmute.usage");
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
        plugin.getMuteManager().unmute(targetUUID, executorUUID, executorName, "Unmuted").thenAccept(success -> {
            if (success) {
                plugin.getMessagesManager().send(sender, "unmute.success", "player", finalTargetName);
            } else {
                plugin.getMessagesManager().send(sender, "unmute.not-muted", "player", finalTargetName);
            }
        });
        
        return true;
    }
}
