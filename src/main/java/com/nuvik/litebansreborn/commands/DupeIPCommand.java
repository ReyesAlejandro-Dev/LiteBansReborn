package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.utils.PlayerUtil;
import com.nuvik.litebansreborn.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.jetbrains.annotations.NotNull;
import java.util.*;

public class DupeIPCommand implements CommandExecutor, TabCompleter {
    private final LiteBansReborn plugin;
    public DupeIPCommand(@NotNull LiteBansReborn plugin) { this.plugin = plugin; }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("litebansreborn.dupeip")) {
            plugin.getMessagesManager().send(sender, "general.no-permission");
            return true;
        }
        if (args.length < 1) {
            plugin.getMessagesManager().send(sender, "dupeip.usage");
            return true;
        }
        String target = args[0];
        
        if (PlayerUtil.isIP(target)) {
            plugin.getAltManager().getPlayersWithIP(target)
                .thenAccept(alts -> Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.getMessagesManager().send(sender, "dupeip.header");
                    if (alts.isEmpty()) {
                        plugin.getMessagesManager().send(sender, "dupeip.empty");
                    } else {
                        for (var alt : alts) {
                            String status = alt.isBanned() ? "&c(Banned)" : "&a(Clean)";
                            plugin.getMessagesManager().send(sender, "dupeip.entry", 
                                "player", alt.getName(),
                                "name", alt.getName(),
                                "status", status,
                                "lastseen", TimeUtil.formatDate(alt.getLastSeen())
                            );
                        }
                    }
                }));
        } else {
            OfflinePlayer player = Bukkit.getOfflinePlayer(target);
            plugin.getAltManager().getAlts(player.getUniqueId())
                .thenAccept(alts -> Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.getMessagesManager().send(sender, "dupeip.header");
                    if (alts.isEmpty()) {
                        plugin.getMessagesManager().send(sender, "dupeip.empty");
                    } else {
                        for (var alt : alts) {
                             String status = alt.isBanned() ? "&c(Banned)" : "&a(Clean)";
                            plugin.getMessagesManager().send(sender, "dupeip.entry", 
                                "player", alt.getName(),
                                "name", alt.getName(),
                                "status", status,
                                "lastseen", TimeUtil.formatDate(alt.getLastSeen())
                            );
                        }
                    }
                }));
        }
        return true;
    }
    
    @Override
    public List<String> onTabComplete(@NotNull CommandSender s, @NotNull Command c, @NotNull String a, @NotNull String[] args) {
        return args.length == 1 ? PlayerUtil.getOnlineAndOfflinePlayerNames(args[0]) : Collections.emptyList();
    }
}
