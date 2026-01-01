package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.utils.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * NoteCommand - Add a staff note to a player
 * 
 * @author Nuvik
 * @version 5.4.0
 */
public class NoteCommand implements CommandExecutor, TabCompleter {
    
    private final LiteBansReborn plugin;
    
    public NoteCommand(@NotNull LiteBansReborn plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, 
                            @NotNull String label, @NotNull String[] args) {
        
        if (!sender.hasPermission("litebansreborn.note")) {
            plugin.getMessagesManager().send(sender, "general.no-permission");
            return true;
        }
        
        if (args.length < 2) {
            plugin.getMessagesManager().send(sender, "note.usage");
            return true;
        }
        
        final String targetName = args[0];
        final OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        final String note = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        
        plugin.getNoteManager().addNote(target.getUniqueId(), targetName, 
                PlayerUtil.getExecutorUUID(sender), PlayerUtil.getExecutorName(sender), note)
            .thenAccept(success -> Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getMessagesManager().send(sender, "note.success", "player", targetName);
            }))
            .exceptionally(ex -> {
                plugin.getMessagesManager().send(sender, "general.error");
                return null;
            });
        
        return true;
    }
    
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("litebansreborn.note")) return Collections.emptyList();
        return args.length == 1 ? PlayerUtil.getOnlineAndOfflinePlayerNames(args[0]) : Collections.emptyList();
    }
}
