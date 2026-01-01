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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * MuteCommand - Mute players temporarily or permanently
 * 
 * Usage: /mute <player> [duration] [reason] [-s]
 * Permission: litebansreborn.mute
 * 
 * @author Nuvik
 * @version 6.0.0
 */
public class MuteCommand implements CommandExecutor, TabCompleter {

    private final LiteBansReborn plugin;

    public MuteCommand(@NotNull LiteBansReborn plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                            @NotNull String label, @NotNull String[] args) {

        if (!sender.hasPermission("litebansreborn.mute")) {
            plugin.getMessagesManager().send(sender, "general.no-permission");
            return true;
        }

        if (args.length < 1) {
            plugin.getMessagesManager().send(sender, "mute.usage");
            return true;
        }

        final String targetName = args[0];

        // Check for silent flag
        boolean silent = false;
        for (String arg : args) {
            if (arg.equalsIgnoreCase("-s") && sender.hasPermission("litebansreborn.silent")) {
                silent = true;
                break;
            }
        }

        // Parse duration (if present)
        Long duration = null;
        int reasonIndex = 1;

        if (args.length > 1 && !args[1].equalsIgnoreCase("-s")) {
            long parsed = TimeUtil.parseDuration(args[1]);
            if (parsed > 0) {
                duration = parsed;
                reasonIndex = 2;
            } else if (parsed == -1) {
                // Permanent
                duration = null;
                reasonIndex = 2;
            }
        }

        // Build reason from remaining args
        StringBuilder reasonBuilder = new StringBuilder();
        for (int i = reasonIndex; i < args.length; i++) {
            if (!args[i].equalsIgnoreCase("-s")) {
                reasonBuilder.append(args[i]).append(" ");
            }
        }
        String reason = reasonBuilder.toString().trim();
        if (reason.isEmpty()) {
            reason = plugin.getConfigManager().getDefaultMuteReason();
        }

        // Prevent self-mute
        if (sender instanceof Player && ((Player) sender).getName().equalsIgnoreCase(targetName)) {
            plugin.getMessagesManager().send(sender, "mute.cannot-mute-self");
            return true;
        }

        // Get target player (online or offline)
        final OfflinePlayer target = PlayerUtil.getOfflinePlayer(targetName);
        final UUID targetUUID = target.getUniqueId();

        // Check if target has mute exemption
        Player onlineTarget = Bukkit.getPlayer(targetUUID);
        if (onlineTarget != null && onlineTarget.hasPermission("litebansreborn.exempt.mute")) {
            if (!sender.hasPermission("litebansreborn.exempt.bypass")) {
                plugin.getMessagesManager().send(sender, "mute.target-exempt");
                return true;
            }
        }

        final String finalReason = reason;
        final Long finalDuration = duration;
        final boolean finalSilent = silent;

        // Get target IP if online
        String targetIP = null;
        if (onlineTarget != null) {
            targetIP = PlayerUtil.getPlayerIP(onlineTarget);
        }
        final String finalTargetIP = targetIP;

        // Execute mute asynchronously
        plugin.getMuteManager().mute(
                targetUUID,
                target.getName() != null ? target.getName() : targetName,
                finalTargetIP,
                PlayerUtil.getExecutorUUID(sender),
                PlayerUtil.getExecutorName(sender),
                finalReason,
                finalDuration,
                finalSilent,
                false
        ).thenAccept(mute -> {
            // Success handled by MuteManager notifications
            plugin.debug("Mute executed successfully for " + targetName);
        }).exceptionally(ex -> {
            plugin.getMessagesManager().send(sender, "general.error");
            ex.printStackTrace();
            return null;
        });

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("litebansreborn.mute")) {
            return Collections.emptyList();
        }

        switch (args.length) {
            case 1:
                // Player names
                return PlayerUtil.getOnlineAndOfflinePlayerNames(args[0]);
            case 2:
                // Duration suggestions
                List<String> suggestions = new ArrayList<>();
                suggestions.add("10m");
                suggestions.add("1h");
                suggestions.add("1d");
                suggestions.add("7d");
                suggestions.add("30d");
                suggestions.add("perm");
                return suggestions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            default:
                // Reason or silent flag
                if (!args[args.length - 1].startsWith("-")) {
                    List<String> flags = new ArrayList<>();
                    if (sender.hasPermission("litebansreborn.silent")) {
                        flags.add("-s");
                    }
                    return flags.stream()
                        .filter(s -> s.startsWith(args[args.length - 1].toLowerCase()))
                        .collect(Collectors.toList());
                }
                return Collections.emptyList();
        }
    }
}
