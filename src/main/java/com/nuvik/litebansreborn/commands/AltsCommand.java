package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.utils.PlayerUtil;
import com.nuvik.litebansreborn.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.jetbrains.annotations.NotNull;
import java.util.*;

public class AltsCommand implements CommandExecutor, TabCompleter {
    private final LiteBansReborn plugin;
    public AltsCommand(@NotNull LiteBansReborn plugin) { this.plugin = plugin; }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("litebansreborn.alts")) {
            plugin.getMessagesManager().send(sender, "general.no-permission");
            return true;
        }
        if (args.length < 1) {
            plugin.getMessagesManager().send(sender, "alts.usage");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        plugin.getMessagesManager().send(sender, "alts.searching", "player", args[0]);
        
        plugin.getAltManager().getAlts(target.getUniqueId())
            .thenAccept(alts -> Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getMessagesManager().send(sender, "alts.header", "player", args[0], "count", String.valueOf(alts.size()));
                if (alts.isEmpty()) {
                    plugin.getMessagesManager().send(sender, "alts.empty");
                } else {
                    for (var alt : alts) {
                        String status = alt.isBanned() ? 
                            plugin.getMessagesManager().get("alts.status.banned") :
                            plugin.getMessagesManager().get("alts.status.clean");
                        
                        // Support both %name% and %alt%, and %lastseen%
                        plugin.getMessagesManager().send(sender, "alts.entry",
                            "name", alt.getName(),
                            "alt", alt.getName(),
                            "status", status,
                            "lastseen", TimeUtil.formatDate(alt.getLastSeen())
                        );
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
