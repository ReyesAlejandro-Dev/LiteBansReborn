package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.jetbrains.annotations.NotNull;
import java.util.*;

public class ReportsCommand implements CommandExecutor, TabCompleter {
    private final LiteBansReborn plugin;
    public ReportsCommand(@NotNull LiteBansReborn plugin) { this.plugin = plugin; }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("litebansreborn.reports")) {
            plugin.getMessagesManager().send(sender, "general.no-permission");
            return true;
        }
        int page = args.length > 0 ? Math.max(1, Integer.parseInt(args[0])) : 1;
        int offset = (page - 1) * 10;
        
        plugin.getReportManager().getPendingReports(10, offset)
            .thenAccept(reports -> Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getMessagesManager().send(sender, "reports.header", "page", String.valueOf(page));
                if (reports.isEmpty()) {
                    plugin.getMessagesManager().send(sender, "reports.empty");
                } else {
                    for (var r : reports) {
                        plugin.getMessagesManager().send(sender, "reports.entry",
                            "id", String.valueOf(r.getId()),
                            "reported", r.getReportedName(),
                            "reporter", r.getReporterName(),
                            "reason", r.getReason());
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
