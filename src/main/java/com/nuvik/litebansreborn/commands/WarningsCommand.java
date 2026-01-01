package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.utils.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.jetbrains.annotations.NotNull;
import java.util.*;

public class WarningsCommand implements CommandExecutor, TabCompleter {
    private final LiteBansReborn plugin;
    public WarningsCommand(@NotNull LiteBansReborn plugin) { this.plugin = plugin; }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("litebansreborn.warnings")) {
            plugin.getMessagesManager().send(sender, "general.no-permission");
            return true;
        }
        if (args.length < 1) {
            plugin.getMessagesManager().send(sender, "warnings.usage");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        plugin.getWarnManager().getWarnings(target.getUniqueId())
            .thenAccept(warns -> Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getMessagesManager().send(sender, "warnings.header", "player", args[0], "count", String.valueOf(warns.size()));
                if (warns.isEmpty()) {
                    plugin.getMessagesManager().send(sender, "warnings.empty", "player", args[0]);
                } else {
                    for (var w : warns) {
                        plugin.getMessagesManager().send(sender, "warnings.entry",
                            "id", String.valueOf(w.getId()),
                            "reason", w.getReason() != null ? w.getReason() : "No reason",
                            "executor", w.getExecutorName(),
                            "date", com.nuvik.litebansreborn.utils.TimeUtil.formatDate(w.getCreatedAt()));
                    }
                }
            }));
        return true;
    }
    
    @Override
    public List<String> onTabComplete(@NotNull CommandSender s, @NotNull Command c, @NotNull String a, @NotNull String[] args) {
        return args.length == 1 ? PlayerUtil.getOnlineAndOfflinePlayerNames(args[0]) : Collections.emptyList();
    }
}
