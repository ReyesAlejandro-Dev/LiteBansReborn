package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.utils.PlayerUtil;
import org.bukkit.command.*;
import org.jetbrains.annotations.NotNull;
import java.util.*;

public class UnmuteIPCommand implements CommandExecutor, TabCompleter {
    private final LiteBansReborn plugin;
    public UnmuteIPCommand(@NotNull LiteBansReborn plugin) { this.plugin = plugin; }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("litebansreborn.unmuteip")) {
            plugin.getMessagesManager().send(sender, "general.no-permission");
            return true;
        }
        if (args.length < 1) {
            plugin.getMessagesManager().send(sender, "unmuteip.usage");
            return true;
        }
        String ip = args[0];
        if (!PlayerUtil.isIP(ip)) {
            plugin.getMessagesManager().send(sender, "unmuteip.invalid-ip");
            return true;
        }
        
        plugin.getMuteManager().unmuteIP(ip, PlayerUtil.getExecutorUUID(sender), sender.getName(), "Unmuted")
            .thenAccept(success -> {
                if (success) {
                    plugin.getMessagesManager().send(sender, "unmuteip.success", "ip", ip);
                } else {
                    plugin.getMessagesManager().send(sender, "unmuteip.not-found", "ip", ip);
                }
            });
        return true;
    }
    
    @Override
    public List<String> onTabComplete(@NotNull CommandSender s, @NotNull Command c, @NotNull String a, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
