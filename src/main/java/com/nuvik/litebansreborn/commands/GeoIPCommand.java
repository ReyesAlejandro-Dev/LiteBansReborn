package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.utils.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.command.*;
import org.jetbrains.annotations.NotNull;
import java.util.*;

public class GeoIPCommand implements CommandExecutor, TabCompleter {
    private final LiteBansReborn plugin;
    public GeoIPCommand(@NotNull LiteBansReborn plugin) { this.plugin = plugin; }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("litebansreborn.geoip")) {
            plugin.getMessagesManager().send(sender, "general.no-permission");
            return true;
        }
        if (args.length < 1) {
            plugin.getMessagesManager().send(sender, "geoip.usage");
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            plugin.getMessagesManager().send(sender, "geoip.not-online");
            return true;
        }
        
        String ip = PlayerUtil.getPlayerIP(target);
        if (ip == null || plugin.getGeoIPManager() == null) {
            plugin.getMessagesManager().send(sender, "geoip.not-available");
            return true;
        }
        
        plugin.getGeoIPManager().lookup(ip).thenAccept(geo -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (geo != null) {
                    plugin.getMessagesManager().send(sender, "geoip.header", "player", target.getName());
                    plugin.getMessagesManager().send(sender, "geoip.country", "country", geo.getCountry());
                    plugin.getMessagesManager().send(sender, "geoip.city", "city", geo.getCity());
                    plugin.getMessagesManager().send(sender, "geoip.isp", "isp", geo.getIsp());
                } else {
                    plugin.getMessagesManager().send(sender, "geoip.not-available");
                }
            });
        });
        return true;
    }
    
    @Override
    public List<String> onTabComplete(@NotNull CommandSender s, @NotNull Command c, @NotNull String a, @NotNull String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase())).toList();
        }
        return Collections.emptyList();
    }
}
