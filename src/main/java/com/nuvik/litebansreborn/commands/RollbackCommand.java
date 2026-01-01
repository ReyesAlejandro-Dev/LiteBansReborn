package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.utils.PlayerUtil;
import com.nuvik.litebansreborn.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.jetbrains.annotations.NotNull;
import java.util.*;

public class RollbackCommand implements CommandExecutor, TabCompleter {
    private final LiteBansReborn plugin;
    public RollbackCommand(@NotNull LiteBansReborn plugin) { this.plugin = plugin; }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("litebansreborn.rollback")) {
            plugin.getMessagesManager().send(sender, "general.no-permission");
            return true;
        }
        if (args.length < 1) {
            plugin.getMessagesManager().send(sender, "rollback.usage");
            return true;
        }
        
        OfflinePlayer staff = Bukkit.getOfflinePlayer(args[0]);
        Long time = args.length > 1 ? TimeUtil.parseDuration(args[1]) : null;
        
        plugin.getHistoryManager().rollbackStaff(staff.getUniqueId(), PlayerUtil.getExecutorUUID(sender), sender.getName(), time)
            .thenAccept(count -> Bukkit.getScheduler().runTask(plugin, () -> {
                if (count > 0) {
                    plugin.getMessagesManager().send(sender, "rollback.success", "count", String.valueOf(count), "staff", args[0]);
                } else {
                    plugin.getMessagesManager().send(sender, "rollback.empty");
                }
            }));
        return true;
    }
    
    @Override
    public List<String> onTabComplete(@NotNull CommandSender s, @NotNull Command c, @NotNull String a, @NotNull String[] args) {
        if (args.length == 1) return PlayerUtil.getOnlineAndOfflinePlayerNames(args[0]);
        if (args.length == 2) return List.of("1h", "6h", "1d", "7d");
        return Collections.emptyList();
    }
}
