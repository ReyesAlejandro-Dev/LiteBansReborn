package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.utils.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;

public class AllowPlayerCommand implements CommandExecutor, TabCompleter {
    private final LiteBansReborn plugin;
    
    public AllowPlayerCommand(@NotNull LiteBansReborn plugin) { this.plugin = plugin; }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, 
                            @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("litebansreborn.allowplayer")) {
            plugin.getMessagesManager().send(sender, "general.no-permission");
            return true;
        }
        if (args.length < 1) {
            plugin.getMessagesManager().send(sender, "allowplayer.usage");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        plugin.getAltManager().allowPlayer(target.getUniqueId(), 
                PlayerUtil.getExecutorUUID(sender), sender.getName())
            .thenAccept(s -> plugin.getMessagesManager().send(sender, "allowplayer.success", "player", args[0]));
        return true;
    }
    
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender s, @NotNull Command c,
                                                 @NotNull String a, @NotNull String[] args) {
        return args.length == 1 ? PlayerUtil.getOnlineAndOfflinePlayerNames(args[0]) : Collections.emptyList();
    }
}
