package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.models.Punishment;
import com.nuvik.litebansreborn.utils.PlayerUtil;
import com.nuvik.litebansreborn.utils.TimeUtil;
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
import java.util.concurrent.CompletableFuture;

/**
 * CheckBanCommand - Elegantly check if a player is banned
 * 
 * Usage: /checkban <player>
 * Permission: litebansreborn.checkban
 * 
 * @author Nuvik
 * @version 5.4.0
 */
public class CheckBanCommand implements CommandExecutor, TabCompleter {
    
    private final LiteBansReborn plugin;
    
    public CheckBanCommand(@NotNull LiteBansReborn plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, 
                            @NotNull String label, @NotNull String[] args) {
        
        // Permission check
        if (!sender.hasPermission("litebansreborn.checkban")) {
            plugin.getMessagesManager().send(sender, "general.no-permission");
            return true;
        }
        
        // Argument validation
        if (args.length < 1) {
            plugin.getMessagesManager().send(sender, "checkban.usage");
            return true;
        }
        
        final String targetName = args[0];
        final OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        
        // Send checking message
        plugin.getMessagesManager().send(sender, "checkban.checking", "player", targetName);
        
        // Async ban check with elegant callback
        checkBanStatus(target)
            .thenAccept(ban -> Bukkit.getScheduler().runTask(plugin, () -> 
                displayResult(sender, targetName, ban)))
            .exceptionally(ex -> {
                plugin.getMessagesManager().send(sender, "general.error");
                plugin.debug("Error checking ban for " + targetName + ": " + ex.getMessage());
                return null;
            });
        
        return true;
    }
    
    /**
     * Check ban status asynchronously
     */
    private CompletableFuture<Punishment> checkBanStatus(OfflinePlayer target) {
        return plugin.getBanManager().getActiveBan(target.getUniqueId());
    }
    
    /**
     * Display the ban check result with beautiful formatting
     */
    private void displayResult(CommandSender sender, String targetName, @Nullable Punishment ban) {
        if (ban != null && ban.isActiveAndValid()) {
            // Player is banned - show detailed info
            plugin.getMessagesManager().send(sender, "checkban.banned.header", "player", targetName);
            plugin.getMessagesManager().send(sender, "checkban.banned.reason", "reason", ban.getReason());
            plugin.getMessagesManager().send(sender, "checkban.banned.by", "executor", ban.getExecutorName());
            plugin.getMessagesManager().send(sender, "checkban.banned.date", 
                "date", TimeUtil.formatDate(ban.getCreatedAt()));
            
            if (ban.isPermanent()) {
                plugin.getMessagesManager().send(sender, "checkban.banned.permanent");
            } else {
                plugin.getMessagesManager().send(sender, "checkban.banned.expires",
                    "duration", TimeUtil.formatDuration(ban.getRemainingTime()),
                    "date", TimeUtil.formatDate(ban.getExpiresAt()));
            }
            
            plugin.getMessagesManager().send(sender, "checkban.banned.id", "id", String.valueOf(ban.getId()));
        } else {
            // Player is not banned
            plugin.getMessagesManager().send(sender, "checkban.not-banned", "player", targetName);
        }
    }
    
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("litebansreborn.checkban")) {
            return Collections.emptyList();
        }
        
        if (args.length == 1) {
            return PlayerUtil.getOnlineAndOfflinePlayerNames(args[0]);
        }
        
        return Collections.emptyList();
    }
}
