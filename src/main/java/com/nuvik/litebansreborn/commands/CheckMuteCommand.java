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
 * CheckMuteCommand - Elegantly check if a player is muted
 * 
 * Usage: /checkmute <player>
 * Permission: litebansreborn.checkmute
 * 
 * @author Nuvik
 * @version 5.4.0
 */
public class CheckMuteCommand implements CommandExecutor, TabCompleter {
    
    private final LiteBansReborn plugin;
    
    public CheckMuteCommand(@NotNull LiteBansReborn plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, 
                            @NotNull String label, @NotNull String[] args) {
        
        if (!sender.hasPermission("litebansreborn.checkmute")) {
            plugin.getMessagesManager().send(sender, "general.no-permission");
            return true;
        }
        
        if (args.length < 1) {
            plugin.getMessagesManager().send(sender, "checkmute.usage");
            return true;
        }
        
        final String targetName = args[0];
        final OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        
        plugin.getMessagesManager().send(sender, "checkmute.checking", "player", targetName);
        
        plugin.getMuteManager().getActiveMute(target.getUniqueId())
            .thenAccept(mute -> Bukkit.getScheduler().runTask(plugin, () -> 
                displayResult(sender, targetName, mute)))
            .exceptionally(ex -> {
                plugin.getMessagesManager().send(sender, "general.error");
                return null;
            });
        
        return true;
    }
    
    private void displayResult(CommandSender sender, String targetName, @Nullable Punishment mute) {
        if (mute != null && mute.isActiveAndValid()) {
            plugin.getMessagesManager().send(sender, "checkmute.muted.header", "player", targetName);
            plugin.getMessagesManager().send(sender, "checkmute.muted.reason", "reason", mute.getReason());
            plugin.getMessagesManager().send(sender, "checkmute.muted.by", "executor", mute.getExecutorName());
            
            if (mute.isPermanent()) {
                plugin.getMessagesManager().send(sender, "checkmute.muted.permanent");
            } else {
                plugin.getMessagesManager().send(sender, "checkmute.muted.expires",
                    "duration", TimeUtil.formatDuration(mute.getRemainingTime()));
            }
        } else {
            plugin.getMessagesManager().send(sender, "checkmute.not-muted", "player", targetName);
        }
    }
    
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("litebansreborn.checkmute")) return Collections.emptyList();
        return args.length == 1 ? PlayerUtil.getOnlineAndOfflinePlayerNames(args[0]) : Collections.emptyList();
    }
}
