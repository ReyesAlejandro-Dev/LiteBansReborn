package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * DelNoteCommand - Delete a staff note
 * 
 * @author Nuvik
 * @version 5.4.0
 */
public class DelNoteCommand implements CommandExecutor, TabCompleter {
    
    private final LiteBansReborn plugin;
    
    public DelNoteCommand(@NotNull LiteBansReborn plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, 
                            @NotNull String label, @NotNull String[] args) {
        
        if (!sender.hasPermission("litebansreborn.delnote")) {
            plugin.getMessagesManager().send(sender, "general.no-permission");
            return true;
        }
        
        if (args.length < 1) {
            plugin.getMessagesManager().send(sender, "delnote.usage");
            return true;
        }
        
        long id;
        try {
            id = Long.parseLong(args[0]);
        } catch (NumberFormatException e) {
            plugin.getMessagesManager().send(sender, "general.invalid-number");
            return true;
        }
        
        plugin.getNoteManager().deleteNote(id)
            .thenAccept(success -> Bukkit.getScheduler().runTask(plugin, () -> {
                if (success) {
                    plugin.getMessagesManager().send(sender, "delnote.success", "id", String.valueOf(id));
                } else {
                    plugin.getMessagesManager().send(sender, "delnote.not-found");
                }
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
        return Collections.emptyList();
    }
}
