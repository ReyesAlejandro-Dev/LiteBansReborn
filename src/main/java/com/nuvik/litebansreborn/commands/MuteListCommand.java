package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.jetbrains.annotations.NotNull;
import java.util.*;

public class MuteListCommand implements CommandExecutor, TabCompleter {
    private final LiteBansReborn plugin;
    public MuteListCommand(@NotNull LiteBansReborn plugin) { this.plugin = plugin; }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("litebansreborn.mutelist")) {
            plugin.getMessagesManager().send(sender, "general.no-permission");
            return true;
        }
        int parsedPage = 1;
        if (args.length > 0) {
            try {
                parsedPage = Math.max(1, Integer.parseInt(args[0]));
            } catch (NumberFormatException e) {
                parsedPage = 1; // Default to page 1 if invalid
            }
        }
        final int page = parsedPage;
        
        plugin.getMuteManager().getActiveMutes(page, 10)
            .thenAccept(mutes -> Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getMessagesManager().send(sender, "mutelist.header", "page", String.valueOf(page), "total_pages", "?", "total_mutes", String.valueOf(mutes.size()));
                if (mutes.isEmpty()) {
                    plugin.getMessagesManager().send(sender, "mutelist.empty");
                } else {
                    for (var m : mutes) {
                        String duration = m.isPermanent() ? "Permanent" : TimeUtil.formatDuration(m.getRemainingTime());
                        plugin.getMessagesManager().send(sender, "mutelist.entry",
                            "player", m.getTargetName(),
                            "reason", m.getReason() != null ? m.getReason() : "No reason",
                            "executor", m.getExecutorName(),
                            "duration", duration);
                    }
                }
            }));
        return true;
    }
    
    @Override
    public List<String> onTabComplete(@NotNull CommandSender s, @NotNull Command c, @NotNull String a, @NotNull String[] args) {
        return args.length == 1 ? List.of("1", "2", "3") : Collections.emptyList();
    }
}
