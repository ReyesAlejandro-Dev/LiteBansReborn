package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.utils.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * UnwarnCommand - Remove a warning from a player
 * 
 * Usage: /unwarn <player> [id]
 * Permission: litebansreborn.unwarn
 * 
 * @author Nuvik
 * @version 5.4.0
 */
public class UnwarnCommand implements CommandExecutor, TabCompleter {
    
    private final LiteBansReborn plugin;
    
    public UnwarnCommand(@NotNull LiteBansReborn plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, 
                            @NotNull String label, @NotNull String[] args) {
        
        if (!sender.hasPermission("litebansreborn.unwarn")) {
            plugin.getMessagesManager().send(sender, "general.no-permission");
            return true;
        }
        
        if (args.length < 1) {
            plugin.getMessagesManager().send(sender, "unwarn.usage");
            return true;
        }
        
        final String targetName = args[0];
        final OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        
        // Parse optional warning ID
        long warnId = -1;
        if (args.length > 1) {
            try {
                warnId = Long.parseLong(args[1]);
            } catch (NumberFormatException e) {
                plugin.getMessagesManager().send(sender, "general.invalid-number");
                return true;
            }
        }
        
        final long finalWarnId = warnId;
        
        plugin.getWarnManager().unwarn(target.getUniqueId(), finalWarnId, 
                PlayerUtil.getExecutorUUID(sender), PlayerUtil.getExecutorName(sender))
            .thenAccept(success -> Bukkit.getScheduler().runTask(plugin, () -> {
                if (success) {
                    if (finalWarnId > 0) {
                        plugin.getMessagesManager().send(sender, "unwarn.success-id", 
                            "player", targetName, 
                            "id", String.valueOf(finalWarnId));
                    } else {
                        plugin.getMessagesManager().send(sender, "unwarn.success", "player", targetName);
                    }
                } else {
                    plugin.getMessagesManager().send(sender, "unwarn.not-found", "player", targetName);
                }
            }))
            .exceptionally(ex -> {
                plugin.getMessagesManager().send(sender, "general.error");
                return null;
            });
        
        return true;
    }
    
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("litebansreborn.unwarn")) return Collections.emptyList();
        return args.length == 1 ? PlayerUtil.getOnlineAndOfflinePlayerNames(args[0]) : Collections.emptyList();
    }
}
