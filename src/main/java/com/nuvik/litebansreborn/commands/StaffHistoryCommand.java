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

import java.util.Collections;
import java.util.List;

/**
 * StaffHistoryCommand - View punishment history for a staff member
 * 
 * @author Nuvik
 * @version 5.4.0
 */
public class StaffHistoryCommand implements CommandExecutor, TabCompleter {
    
    private final LiteBansReborn plugin;
    
    public StaffHistoryCommand(@NotNull LiteBansReborn plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, 
                            @NotNull String label, @NotNull String[] args) {
        
        if (!sender.hasPermission("litebansreborn.staffhistory")) {
            plugin.getMessagesManager().send(sender, "general.no-permission");
            return true;
        }
        
        if (args.length < 1) {
            plugin.getMessagesManager().send(sender, "staffhistory.usage");
            return true;
        }
        
        final String staffName = args[0];
        final OfflinePlayer staff = Bukkit.getOfflinePlayer(staffName);
        
        plugin.getHistoryManager().getStaffHistory(staff.getUniqueId(), 1, 15)
            .thenAccept(punishments -> Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getMessagesManager().send(sender, "staffhistory.header", "staff", staffName);
                
                if (punishments.isEmpty()) {
                    plugin.getMessagesManager().send(sender, "staffhistory.empty");
                } else {
                    for (var p : punishments) {
                        plugin.getMessagesManager().send(sender, "staffhistory.entry",
                            "type", p.getType().getId().toUpperCase(),
                            "target", p.getTargetName(),
                            "reason", p.getReason() != null ? p.getReason() : "No reason");
                    }
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
        if (!sender.hasPermission("litebansreborn.staffhistory")) return Collections.emptyList();
        return args.length == 1 ? PlayerUtil.getOnlineAndOfflinePlayerNames(args[0]) : Collections.emptyList();
    }
}
