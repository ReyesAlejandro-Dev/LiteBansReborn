package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.utils.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Warn command - /warn <player> [reason]
 */
public class WarnCommand implements CommandExecutor {

    private final LiteBansReborn plugin;
    
    public WarnCommand(LiteBansReborn plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("litebansreborn.warn")) {
            plugin.getMessagesManager().send(sender, "general.no-permission");
            return true;
        }
        
        if (args.length < 1) {
            plugin.getMessagesManager().send(sender, "warn.usage");
            return true;
        }
        
        String targetName = args[0];
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
        
        OfflinePlayer target = PlayerUtil.getOfflinePlayer(targetName);
        if (target == null || !target.hasPlayedBefore()) {
            plugin.getMessagesManager().send(sender, "general.player-not-found", "player", targetName);
            return true;
        }
        
        UUID targetUUID = target.getUniqueId();
        targetName = target.getName() != null ? target.getName() : targetName;
        
        if (sender instanceof Player && ((Player) sender).getUniqueId().equals(targetUUID)) {
            plugin.getMessagesManager().send(sender, "general.cannot-punish-self");
            return true;
        }
        
        UUID executorUUID = PlayerUtil.getExecutorUUID(sender);
        String executorName = PlayerUtil.getExecutorName(sender);
        String finalTargetName = targetName;
        
        plugin.getWarnManager().warn(targetUUID, finalTargetName, executorUUID, executorName, reason, silent)
            .thenCompose(warn -> plugin.getWarnManager().getActiveWarningCount(targetUUID))
            .thenAccept(count -> {
                plugin.getMessagesManager().send(sender, "warn.success",
                    "player", finalTargetName,
                    "count", String.valueOf(count));
            });
        
        return true;
    }
}
