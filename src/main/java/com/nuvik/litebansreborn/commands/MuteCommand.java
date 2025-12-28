package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.utils.PlayerUtil;
import com.nuvik.litebansreborn.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Mute command - /mute <player> [duration] [reason]
 */
public class MuteCommand implements CommandExecutor {

    private final LiteBansReborn plugin;
    
    public MuteCommand(LiteBansReborn plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("litebansreborn.mute")) {
            plugin.getMessagesManager().send(sender, "general.no-permission");
            return true;
        }
        
        if (args.length < 1) {
            plugin.getMessagesManager().send(sender, "mute.usage");
            return true;
        }
        
        String targetName = args[0];
        boolean silent = false;
        Long duration = null;
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
        
        if (args.length > startIndex) {
            long parsedDuration = TimeUtil.parseDuration(args[startIndex]);
            if (parsedDuration > 0) {
                duration = parsedDuration;
                startIndex++;
            } else if (parsedDuration == -1) {
                duration = null;
                startIndex++;
            }
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
        
        Player onlineTarget = target.getPlayer();
        if (onlineTarget != null && onlineTarget.hasPermission("litebansreborn.bypass.mute")) {
            plugin.getMessagesManager().send(sender, "general.cannot-punish-exempt");
            return true;
        }
        
        String ip = onlineTarget != null ? PlayerUtil.getPlayerIP(onlineTarget) : null;
        UUID executorUUID = PlayerUtil.getExecutorUUID(sender);
        String executorName = PlayerUtil.getExecutorName(sender);
        
        Long finalDuration = duration;
        String finalTargetName = targetName;
        boolean finalSilent = silent;
        
        plugin.getMuteManager().getActiveMute(targetUUID).thenAccept(existingMute -> {
            if (existingMute != null && existingMute.isActiveAndValid()) {
                plugin.getMessagesManager().send(sender, "general.already-punished", "punishment", "muted");
                return;
            }
            
            plugin.getMuteManager().mute(targetUUID, finalTargetName, ip, executorUUID, executorName,
                    reason, finalDuration, finalSilent, false).thenAccept(mute -> {
                
                if (finalDuration != null) {
                    plugin.getMessagesManager().send(sender, "mute.success-temp",
                            "player", finalTargetName,
                            "duration", TimeUtil.formatDuration(finalDuration));
                } else {
                    plugin.getMessagesManager().send(sender, "mute.success", "player", finalTargetName);
                }
            });
        });
        
        return true;
    }
}
