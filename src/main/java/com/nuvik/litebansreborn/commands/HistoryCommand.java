package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.config.MessagesManager;
import com.nuvik.litebansreborn.models.Punishment;
import com.nuvik.litebansreborn.utils.PlayerUtil;
import com.nuvik.litebansreborn.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * History command - /history <player>
 */
public class HistoryCommand implements CommandExecutor, TabCompleter {

    private final LiteBansReborn plugin;
    
    public HistoryCommand(LiteBansReborn plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("litebansreborn.history")) {
            plugin.getMessagesManager().send(sender, "general.no-permission");
            return true;
        }
        
        if (args.length < 1) {
            plugin.getMessagesManager().send(sender, "history.usage");
            return true;
        }
        
        String targetName = args[0];
        int page = 1;
        
        if (args.length > 1) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {}
        }
        
        OfflinePlayer target = PlayerUtil.getOfflinePlayer(targetName);
        if (target == null) {
            plugin.getMessagesManager().send(sender, "general.player-not-found", "player", targetName);
            return true;
        }
        
        UUID targetUUID = target.getUniqueId();
        String finalTargetName = target.getName() != null ? target.getName() : targetName;
        int finalPage = page;
        
        plugin.getHistoryManager().getPlayerHistory(targetUUID).thenAccept(history -> {
            if (history.isEmpty()) {
                plugin.getMessagesManager().send(sender, "history.no-history", "player", finalTargetName);
                return;
            }
            
            sender.sendMessage(plugin.getMessagesManager().get("history.header", "player", finalTargetName));
            
            int perPage = 10;
            int totalPages = (int) Math.ceil(history.size() / (double) perPage);
            int start = (finalPage - 1) * perPage;
            int end = Math.min(start + perPage, history.size());
            
            for (int i = start; i < end; i++) {
                Punishment p = history.get(i);
                
                String status;
                if (p.isActive() && !p.hasExpired()) {
                    status = plugin.getMessagesManager().get("history.active");
                } else if (p.getRemovedAt() != null) {
                    status = plugin.getMessagesManager().get("history.removed");
                } else {
                    status = plugin.getMessagesManager().get("history.expired");
                }
                
                String duration = p.isPermanent() ? "Permanent" : 
                        (p.getExpiresAt() != null ? TimeUtil.formatDurationShort(
                                p.getExpiresAt().toEpochMilli() - p.getCreatedAt().toEpochMilli()) : "N/A");
                
                String entry = plugin.getMessagesManager().get("history.entry." + p.getType().getId().replace("temp", ""),
                        "date", TimeUtil.formatDate(p.getCreatedAt()),
                        "reason", p.getReason(),
                        "executor", p.getExecutorName(),
                        "duration", duration,
                        "status", status
                );
                
                sender.sendMessage(status + " " + entry);
            }
            
            if (totalPages > 1) {
                sender.sendMessage(plugin.getMessagesManager().get("history.footer",
                        "page", String.valueOf(finalPage),
                        "total", String.valueOf(totalPages)));
            }
            
            sender.sendMessage(plugin.getMessagesManager().get("history.footer"));
        });
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
