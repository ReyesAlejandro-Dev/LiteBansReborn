package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.utils.PlayerUtil;
import org.bukkit.command.*;
import org.jetbrains.annotations.NotNull;
import java.util.*;

public class UnbanIPCommand implements CommandExecutor, TabCompleter {
    private final LiteBansReborn plugin;
    public UnbanIPCommand(@NotNull LiteBansReborn plugin) { this.plugin = plugin; }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("litebansreborn.unbanip")) {
            plugin.getMessagesManager().send(sender, "general.no-permission");
            return true;
        }
        if (args.length < 1) {
            plugin.getMessagesManager().send(sender, "unbanip.usage");
            return true;
        }
        String ip = args[0];
        if (!PlayerUtil.isIP(ip)) {
            plugin.getMessagesManager().send(sender, "unbanip.invalid-ip");
            return true;
        }
        
        plugin.getBanManager().unbanIP(ip, PlayerUtil.getExecutorUUID(sender), sender.getName(), "Unbanned")
            .thenAccept(success -> {
                if (success) {
                    plugin.getMessagesManager().send(sender, "unbanip.success", "ip", ip);
                } else {
                    plugin.getMessagesManager().send(sender, "unbanip.not-found", "ip", ip);
                }
            });
        return true;
    }
    
    @Override
    public List<String> onTabComplete(@NotNull CommandSender s, @NotNull Command c, @NotNull String a, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
