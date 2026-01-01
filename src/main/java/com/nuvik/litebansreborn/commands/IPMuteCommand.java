package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.utils.PlayerUtil;
import com.nuvik.litebansreborn.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.command.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;

public class IPMuteCommand implements CommandExecutor, TabCompleter {
    private final LiteBansReborn plugin;
    
    public IPMuteCommand(@NotNull LiteBansReborn plugin) { this.plugin = plugin; }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, 
                            @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("litebansreborn.ipmute")) {
            plugin.getMessagesManager().send(sender, "general.no-permission");
            return true;
        }
        if (args.length < 1) {
            plugin.getMessagesManager().send(sender, "mute.ip.usage");
            return true;
        }
        
        String target = args[0];
        Long duration = args.length > 1 ? TimeUtil.parseDuration(args[1]) : -1L;
        String reason = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : null;
        
        String ip, name = target;
        java.util.UUID uuid = null;
        
        if (PlayerUtil.isIP(target)) {
            ip = target;
        } else {
            Player player = Bukkit.getPlayer(target);
            if (player != null) {
                ip = PlayerUtil.getPlayerIP(player);
                uuid = player.getUniqueId();
                name = player.getName();
            } else {
                plugin.getMessagesManager().send(sender, "general.player-not-found", "player", target);
                return true;
            }
        }
        
        plugin.getMuteManager().mute(uuid, name, ip, PlayerUtil.getExecutorUUID(sender), 
                sender.getName(), reason, duration, false, true);
        plugin.getMessagesManager().send(sender, "mute.ip.success", "target", ip);
        return true;
    }
    
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender s, @NotNull Command c,
                                                 @NotNull String a, @NotNull String[] args) {
        if (args.length == 1) return null;
        if (args.length == 2) return List.of("1h", "1d", "7d", "perm");
        return Collections.emptyList();
    }
}
