package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.jetbrains.annotations.NotNull;
import java.util.*;

public class AppealsCommand implements CommandExecutor, TabCompleter {
    private final LiteBansReborn plugin;
    public AppealsCommand(@NotNull LiteBansReborn plugin) { this.plugin = plugin; }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("litebansreborn.appeals")) {
            plugin.getMessagesManager().send(sender, "general.no-permission");
            return true;
        }
        int page = 1;
        if (args.length > 0) {
            try {
                page = Math.max(1, Integer.parseInt(args[0]));
            } catch (NumberFormatException e) {
                plugin.getMessagesManager().send(sender, "general.invalid-number");
                return true;
            }
        }
        
        final int finalPage = page; // Make effectively final for lambda
        plugin.getAppealManager().getPendingAppeals(finalPage, 10)
            .thenAccept(appeals -> Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getMessagesManager().send(sender, "appeals.header", "page", String.valueOf(finalPage));
                if (appeals.isEmpty()) {
                    plugin.getMessagesManager().send(sender, "appeals.empty");
                } else {
                    for (var a : appeals) {
                        plugin.getMessagesManager().send(sender, "appeals.entry",
                            "id", String.valueOf(a.getId()),
                            "player", a.getPlayerName(),
                            "type", a.getPunishmentType().getId(),
                            "message", truncate(a.getMessage(), 50));
                    }
                }
            }));
        return true;
    }
    
    private String truncate(String text, int max) {
        return text != null && text.length() > max ? text.substring(0, max) + "..." : (text != null ? text : "");
    }
    
    @Override
    public List<String> onTabComplete(@NotNull CommandSender s, @NotNull Command c, @NotNull String a, @NotNull String[] args) {
        return args.length == 1 ? List.of("1", "2", "3") : Collections.emptyList();
    }
}
