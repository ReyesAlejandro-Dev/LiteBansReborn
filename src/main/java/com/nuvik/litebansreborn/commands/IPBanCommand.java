package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

// Stub commands - basic implementations

public class IPBanCommand implements CommandExecutor, org.bukkit.command.TabCompleter {
    private final LiteBansReborn plugin;

    public IPBanCommand(LiteBansReborn plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("litebansreborn.ban.ip")) {
            plugin.getMessagesManager().send(sender, "general.no-permission");
            return true;
        }

        if (args.length < 1) {
            plugin.getMessagesManager().send(sender, "ban.ip.usage", "command", label);
            return true;
        }

        String targetStr = args[0];
        long duration = -1;
        String reason = null;
        int reasonIndex = 1;

        // Check if second arg is duration
        if (args.length > 1) {
            try {
                duration = com.nuvik.litebansreborn.utils.TimeUtil.parseDuration(args[1]);
                if (duration > 0) {
                    reasonIndex = 2; // args[1] was duration, so reason starts at 2
                }
            } catch (IllegalArgumentException ignored) {
                // Not a valid duration, assume it's part of the reason
            }
        }

        if (args.length > reasonIndex) {
            StringBuilder sb = new StringBuilder();
            for (int i = reasonIndex; i < args.length; i++) {
                sb.append(args[i]).append(" ");
            }
            reason = sb.toString().trim();
        }

        // Silent flag check
        boolean silent = false;
        if (reason != null && reason.contains("-s")) {
            silent = true;
            reason = reason.replace("-s", "").trim();
        }

        final String finalReason = reason;
        final long finalDuration = duration;
        final boolean finalSilent = silent;

        sender.sendMessage(com.nuvik.litebansreborn.utils.ColorUtil.translate("&7Processing IP ban for &e" + targetStr + "&7..."));

        // Determine if target is IP or Player
        if (com.nuvik.litebansreborn.utils.PlayerUtil.isIP(targetStr)) {
            // It's an IP
            executeIPBan(sender, null, targetStr, targetStr, finalDuration, finalReason, finalSilent);
        } else {
            // It's a player name - resolve IP
            org.bukkit.entity.Player targetPlayer = org.bukkit.Bukkit.getPlayer(targetStr);
            if (targetPlayer != null) {
                String ip = com.nuvik.litebansreborn.utils.PlayerUtil.getPlayerIP(targetPlayer);
                executeIPBan(sender, targetPlayer.getUniqueId(), targetPlayer.getName(), ip, finalDuration, finalReason, finalSilent);
            } else {
                // Offline player lookup - try to get IP from database
                org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    org.bukkit.OfflinePlayer offlinePlayer = org.bukkit.Bukkit.getOfflinePlayer(targetStr);
                    if (!offlinePlayer.hasPlayedBefore()) {
                        plugin.getMessagesManager().send(sender, "general.player-not-found");
                        return;
                    }
                    java.util.UUID uuid = offlinePlayer.getUniqueId();
                    
                    // Try to get IP from player's history (last punishment that recorded their IP)
                    plugin.getHistoryManager().getPlayerHistory(uuid).thenAccept(history -> {
                         String ip = null;
                         for (var p : history) {
                             if (p.getTargetIP() != null && !p.getTargetIP().isEmpty()) {
                                 ip = p.getTargetIP();
                                 break;
                             }
                         }
                         if (ip != null) {
                             final String finalIP = ip;
                             org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> 
                                 executeIPBan(sender, uuid, offlinePlayer.getName() != null ? offlinePlayer.getName() : targetStr, finalIP, finalDuration, finalReason, finalSilent)
                             );
                         } else {
                             plugin.getMessagesManager().send(sender, "ban.ip.no-ip-found");
                         }
                    });
                });
            }
        }

        return true;
    }

    private void executeIPBan(CommandSender sender, java.util.UUID targetUUID, String targetName, String ip, long duration, String reason, boolean silent) {
        if (ip == null) {
            plugin.getMessagesManager().send(sender, "ban.ip.invalid-ip");
            return;
        }

        java.util.UUID executorUUID = (sender instanceof org.bukkit.entity.Player) ? ((org.bukkit.entity.Player) sender).getUniqueId() : null;
        String executorName = sender.getName();

        plugin.getBanManager().ban(targetUUID, targetName, ip, executorUUID, executorName, reason, duration, silent, true)
            .thenAccept(ban -> {
                // Success message handled by notification system usually, but showing confirmation to sender is good
                if (ban.isSilent() && sender.hasPermission("litebansreborn.notify")) {
                     // Already notified via silent broadcast
                }
            });
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
         if (args.length == 1) {
             return com.nuvik.litebansreborn.utils.PlayerUtil.getOnlineAndOfflinePlayerNames(args[0]);
         }
         if (args.length == 2) {
             java.util.List<String> suggestions = new java.util.ArrayList<>();
             suggestions.add("7d");
             suggestions.add("30d");
             suggestions.add("perm");
             return suggestions;
         }
         return java.util.List.of();
    }
}
