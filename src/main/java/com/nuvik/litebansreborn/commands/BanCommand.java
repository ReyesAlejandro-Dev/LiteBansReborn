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
        
        String targetArg = args[0];
        boolean silent = false;
        Long duration = null;
        StringBuilder reasonBuilder = new StringBuilder();
        int startIndex = 1;
        
        // Check for silent flag (can be anywhere typically, but here we check arg 1 or as part of reason)
        // For simplicity and standard command structure, we check if arg[0] starts with -s (flag) but target is usually first.
        // Let's stick to standard <player> first.
        
        // Handle flags if passed as first arg (e.g. /ban -s player)
        if (targetArg.equalsIgnoreCase("-s")) {
             if (args.length < 2) {
                 plugin.getMessagesManager().send(sender, "ban.usage");
                 return true;
             }
             if (!sender.hasPermission("litebansreborn.silent")) {
                 plugin.getMessagesManager().send(sender, "general.no-permission");
                 return true;
             }
             silent = true;
             targetArg = args[1];
             startIndex = 2;
        } else if (args.length > 1 && args[1].equalsIgnoreCase("-s")) {
            // /ban player -s
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
                // Permanent explicit
                duration = null;
                startIndex++;
            }
        }
        
        // Build reason from remaining args
        for (int i = startIndex; i < args.length; i++) {
            if (i > startIndex) reasonBuilder.append(" ");
            reasonBuilder.append(args[i]);
        }
        
        // Check for silent in reason (-s flag at end)
        String reasonStr = reasonBuilder.toString();
        if (reasonStr.contains("-s")) {
             if (sender.hasPermission("litebansreborn.silent")) {
                 silent = true;
                 reasonStr = reasonStr.replace("-s", "").trim();
             }
        }
        
        final String reason = reasonStr.isEmpty() ? null : reasonStr;
        final boolean finalSilent = silent;
        final String finalTargetName = targetArg;
        final Long finalDuration = duration;
        
        // Check for permanent permission
        if (duration == null && !sender.hasPermission("litebansreborn.ban.permanent")) {
            plugin.getMessagesManager().send(sender, "general.no-permission");
            return true;
        }
        
        // Async processing for lookup
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            UUID targetUUID = null;
            String targetName = finalTargetName;
            String ip = null;
            
            // 1. Check if input is UUID
            try {
                targetUUID = UUID.fromString(finalTargetName);
                // It's a valid UUID. Try to get name.
                OfflinePlayer p = Bukkit.getOfflinePlayer(targetUUID);
                if (p.getName() != null) targetName = p.getName();
            } catch (IllegalArgumentException e) {
                // Not a UUID, proceed as name
            }
            
            // 2. If not UUID, try online player
            if (targetUUID == null) {
                Player onlineTarget = Bukkit.getPlayerExact(finalTargetName);
                if (onlineTarget != null) {
                    targetUUID = onlineTarget.getUniqueId();
                    targetName = onlineTarget.getName();
                    ip = PlayerUtil.getPlayerIP(onlineTarget);
                }
            }
            
            // 3. If still null, try OfflinePlayer (hasPlayedBefore)
            if (targetUUID == null) {
                OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(finalTargetName);
                if (offlineTarget.hasPlayedBefore()) {
                     targetUUID = offlineTarget.getUniqueId();
                     targetName = offlineTarget.getName();
                }
            }
            
            // 4. Force lookup (Mojang API) if still not found and configured to do so?
            // For now, if provided a Name that hasn't played, we can't easily get UUID without an API call.
            // But if user provided a UUID (step 1), we have it.
            
            if (targetUUID == null) {
                 // Try to resolve via internal cache/database history if possible
                 // Or just fail if we can't find them.
                 // NOTE: As requested, we support banning by direct UUID which is handled in step 1.
                 
                 // Final attempt: allow non-existent players if strict mode is off?
                 // For now, fail with message.
                 plugin.getMessagesManager().send(sender, "general.player-not-found", "player", finalTargetName);
                 return;
            }
            
            
            // Check self/exempt
            if (sender instanceof Player && ((Player) sender).getUniqueId().equals(targetUUID)) {
                plugin.getMessagesManager().send(sender, "general.cannot-punish-self");
                return;
            }
             
            // We need to check exempt permission. If offline, we can't easily check permissions unless using LuckPerms API.
            // Basic online check:
            Player onlineP = Bukkit.getPlayer(targetUUID);
            if (onlineP != null && onlineP.hasPermission("litebansreborn.bypass.ban")) {
                plugin.getMessagesManager().send(sender, "general.cannot-punish-exempt");
                return;
            }
            
            final UUID finalUUID = targetUUID;
            final String finalName = targetName;
            final String finalIP = ip;

            // Execute Ban logic
            plugin.getBanManager().getActiveBan(finalUUID).thenAccept(existingBan -> {
                if (existingBan != null && existingBan.isActiveAndValid()) {
                    plugin.getMessagesManager().send(sender, "general.already-punished", "punishment", "banned");
                    return;
                }
                
                plugin.getBanManager().ban(finalUUID, finalName, finalIP, PlayerUtil.getExecutorUUID(sender), PlayerUtil.getExecutorName(sender),
                        reason, finalDuration, finalSilent, false).thenAccept(ban -> {
                    
                    if (finalDuration != null) {
                        plugin.getMessagesManager().send(sender, "ban.success-temp",
                                "player", finalName,
                                "duration", TimeUtil.formatDuration(finalDuration));
                    } else {
                        plugin.getMessagesManager().send(sender, "ban.success", "player", finalName);
                    }
                });
            });
        });
        
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            List<String> matches = new ArrayList<>();
            
            // Online players
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(partial)) {
                    matches.add(p.getName());
                }
            }
            
            // Offline players (limit to 50 for performance)
            if (matches.size() < 50) {
                 for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {
                     String name = p.getName();
                     if (name != null && name.toLowerCase().startsWith(partial)) {
                         if (!matches.contains(name)) matches.add(name);
                     }
                     if (matches.size() >= 50) break;
                 }
            }
            
            return matches;
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
