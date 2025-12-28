package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.utils.PlayerUtil;
import com.nuvik.litebansreborn.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Ban command - /ban <player> [duration] [reason]
 */
public class BanCommand implements CommandExecutor, TabCompleter {

    private final LiteBansReborn plugin;
    
    public BanCommand(LiteBansReborn plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("litebansreborn.ban")) {
            plugin.getMessagesManager().send(sender, "general.no-permission");
            return true;
        }
        
        if (args.length < 1) {
            plugin.getMessagesManager().send(sender, "ban.usage");
            return true;
        }
        
        String targetName = args[0];
        boolean silent = false;
        Long duration = null;
        StringBuilder reasonBuilder = new StringBuilder();
        int startIndex = 1;
        
        // Check for silent flag
        if (args.length > 1 && args[1].equalsIgnoreCase("-s")) {
            if (!sender.hasPermission("litebansreborn.silent")) {
                plugin.getMessagesManager().send(sender, "general.no-permission");
                return true;
            }
            silent = true;
            startIndex = 2;
        }
        
        // Parse duration if provided
        if (args.length > startIndex) {
            long parsedDuration = TimeUtil.parseDuration(args[startIndex]);
            if (parsedDuration > 0) {
                duration = parsedDuration;
                startIndex++;
            } else if (parsedDuration == -1) {
                // Permanent
                duration = null;
                startIndex++;
            }
        }
        
        // Build reason from remaining args
        for (int i = startIndex; i < args.length; i++) {
            if (i > startIndex) reasonBuilder.append(" ");
            reasonBuilder.append(args[i]);
        }
        
        String reason = reasonBuilder.length() > 0 ? reasonBuilder.toString() : null;
        
        // Check for permanent permission
        if (duration == null && !sender.hasPermission("litebansreborn.ban.permanent")) {
            plugin.getMessagesManager().send(sender, "general.no-permission");
            return true;
        }
        
        // Get target
        OfflinePlayer target = PlayerUtil.getOfflinePlayer(targetName);
        if (target == null || !target.hasPlayedBefore()) {
            // Try anyway with the name
            UUID targetUUID = null;
            String ip = null;
            
            // If online, get their info
            Player onlineTarget = Bukkit.getPlayerExact(targetName);
            if (onlineTarget != null) {
                targetUUID = onlineTarget.getUniqueId();
                targetName = onlineTarget.getName();
                ip = PlayerUtil.getPlayerIP(onlineTarget);
            }
            
            if (targetUUID == null) {
                plugin.getMessagesManager().send(sender, "general.player-not-found", "player", targetName);
                return true;
            }
            
            executeBan(sender, targetUUID, targetName, ip, duration, reason, silent);
        } else {
            UUID targetUUID = target.getUniqueId();
            targetName = target.getName() != null ? target.getName() : targetName;
            
            // Get IP if online
            String ip = null;
            Player onlineTarget = target.getPlayer();
            if (onlineTarget != null) {
                ip = PlayerUtil.getPlayerIP(onlineTarget);
            }
            
            // Self check
            if (sender instanceof Player && ((Player) sender).getUniqueId().equals(targetUUID)) {
                plugin.getMessagesManager().send(sender, "general.cannot-punish-self");
                return true;
            }
            
            // Exempt check
            if (onlineTarget != null && onlineTarget.hasPermission("litebansreborn.bypass.ban")) {
                plugin.getMessagesManager().send(sender, "general.cannot-punish-exempt");
                return true;
            }
            
            executeBan(sender, targetUUID, targetName, ip, duration, reason, silent);
        }
        
        return true;
    }
    
    private void executeBan(CommandSender sender, UUID targetUUID, String targetName, String ip,
                           Long duration, String reason, boolean silent) {
        
        UUID executorUUID = PlayerUtil.getExecutorUUID(sender);
        String executorName = PlayerUtil.getExecutorName(sender);
        
        // Check if already banned
        plugin.getBanManager().getActiveBan(targetUUID).thenAccept(existingBan -> {
            if (existingBan != null && existingBan.isActiveAndValid()) {
                plugin.getMessagesManager().send(sender, "general.already-punished", "punishment", "banned");
                return;
            }
            
            // Execute the ban
            plugin.getBanManager().ban(targetUUID, targetName, ip, executorUUID, executorName,
                    reason, duration, silent, false).thenAccept(ban -> {
                
                // Send success message
                if (duration != null) {
                    plugin.getMessagesManager().send(sender, "ban.success-temp",
                            "player", targetName,
                            "duration", TimeUtil.formatDuration(duration));
                } else {
                    plugin.getMessagesManager().send(sender, "ban.success", "player", targetName);
                }
            });
        });
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            List<String> completions = new ArrayList<>();
            completions.addAll(Arrays.asList("1h", "1d", "7d", "30d", "permanent", "-s"));
            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
