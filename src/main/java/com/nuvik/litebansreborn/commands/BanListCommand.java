package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.models.Punishment;
import com.nuvik.litebansreborn.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * BanListCommand - Display active bans with pagination
 * 
 * Usage: /banlist [page]
 * Permission: litebansreborn.banlist
 * 
 * @author Nuvik
 * @version 5.4.0
 */
public class BanListCommand implements CommandExecutor, TabCompleter {
    
    private final LiteBansReborn plugin;
    private static final int BANS_PER_PAGE = 10;
    
    public BanListCommand(@NotNull LiteBansReborn plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, 
                            @NotNull String label, @NotNull String[] args) {
        
        if (!sender.hasPermission("litebansreborn.banlist")) {
            plugin.getMessagesManager().send(sender, "general.no-permission");
            return true;
        }
        
        // Parse page number with validation
        int page = 1;
        if (args.length > 0) {
            try {
                page = Math.max(1, Integer.parseInt(args[0]));
            } catch (NumberFormatException e) {
                plugin.getMessagesManager().send(sender, "general.invalid-number");
                return true;
            }
        }
        
        final int currentPage = page;
        
        // Fetch bans and count concurrently for performance
        plugin.getBanManager().getActiveBansCount().thenCompose(totalCount -> 
            plugin.getBanManager().getActiveBans(currentPage, BANS_PER_PAGE).thenApply(bans -> 
                new BanListResult(bans, totalCount, currentPage)))
            .thenAccept(result -> Bukkit.getScheduler().runTask(plugin, () -> 
                displayBanList(sender, result)))
            .exceptionally(ex -> {
                plugin.getMessagesManager().send(sender, "general.error");
                return null;
            });
        
        return true;
    }
    
    private void displayBanList(CommandSender sender, BanListResult result) {
        int totalPages = (int) Math.ceil((double) result.totalCount / BANS_PER_PAGE);
        
        // Header
        plugin.getMessagesManager().send(sender, "banlist.header",
            "page", String.valueOf(result.page),
            "total_pages", String.valueOf(Math.max(1, totalPages)),
            "total_bans", String.valueOf(result.totalCount));
        
        if (result.bans.isEmpty()) {
            plugin.getMessagesManager().send(sender, "banlist.empty");
        } else {
            // Display each ban
            for (Punishment ban : result.bans) {
                String duration = ban.isPermanent() ? 
                    plugin.getMessagesManager().get("banlist.permanent") : 
                    TimeUtil.formatDuration(ban.getRemainingTime());
                    
                plugin.getMessagesManager().send(sender, "banlist.entry",
                    "player", ban.getTargetName(),
                    "reason", ban.getReason() != null ? ban.getReason() : "No reason",
                    "executor", ban.getExecutorName(),
                    "duration", duration,
                    "id", String.valueOf(ban.getId()));
            }
        }
        
        // Footer with navigation hints
        if (totalPages > 1) {
            plugin.getMessagesManager().send(sender, "banlist.footer",
                "page", String.valueOf(result.page),
                "total_pages", String.valueOf(totalPages));
        }
    }
    
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("1", "2", "3");
        }
        return Collections.emptyList();
    }
    
    /**
     * Internal record for ban list result
     */
    private record BanListResult(List<Punishment> bans, int totalCount, int page) {}
}
