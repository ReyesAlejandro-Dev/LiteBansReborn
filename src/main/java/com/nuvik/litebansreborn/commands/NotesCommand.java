package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.utils.PlayerUtil;
import com.nuvik.litebansreborn.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.jetbrains.annotations.NotNull;
import java.util.*;

public class NotesCommand implements CommandExecutor, TabCompleter {
    private final LiteBansReborn plugin;
    public NotesCommand(@NotNull LiteBansReborn plugin) { this.plugin = plugin; }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("litebansreborn.notes")) {
            plugin.getMessagesManager().send(sender, "general.no-permission");
            return true;
        }
        if (args.length < 1) {
            plugin.getMessagesManager().send(sender, "notes.usage");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        
        plugin.getNoteManager().getNotes(target.getUniqueId())
            .thenAccept(notes -> Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getMessagesManager().send(sender, "notes.header", "player", args[0], "count", String.valueOf(notes.size()));
                if (notes.isEmpty()) {
                    plugin.getMessagesManager().send(sender, "notes.empty");
                } else {
                    for (var n : notes) {
                        plugin.getMessagesManager().send(sender, "notes.entry",
                            "id", String.valueOf(n.getId()),
                            "author", n.getAuthorName(),
                            "date", TimeUtil.formatDate(n.getCreatedAt()),
                            "content", n.getContent());
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
