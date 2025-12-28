package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.managers.HistoryManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * Main plugin command - /litebansreborn
 */
public class MainCommand implements CommandExecutor {

    private final LiteBansReborn plugin;
    
    public MainCommand(LiteBansReborn plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "reload":
                if (!sender.hasPermission("litebansreborn.admin")) {
                    plugin.getMessagesManager().send(sender, "general.no-permission");
                    return true;
                }
                plugin.reload();
                plugin.getMessagesManager().send(sender, "general.reload-success");
                break;
                
            case "info":
                sendInfo(sender);
                break;
                
            case "stats":
                if (!sender.hasPermission("litebansreborn.admin")) {
                    plugin.getMessagesManager().send(sender, "general.no-permission");
                    return true;
                }
                showStats(sender);
                break;
                
            case "import":
                if (!sender.hasPermission("litebansreborn.admin")) {
                    plugin.getMessagesManager().send(sender, "general.no-permission");
                    return true;
                }
                sender.sendMessage("§eImport functionality coming soon!");
                break;
                
            case "export":
                if (!sender.hasPermission("litebansreborn.admin")) {
                    plugin.getMessagesManager().send(sender, "general.no-permission");
                    return true;
                }
                sender.sendMessage("§eExport functionality coming soon!");
                break;
                
            case "help":
            default:
                sendHelp(sender);
                break;
        }
        
        return true;
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage("§c§lLiteBansReborn §8- §7v" + plugin.getDescription().getVersion());
        sender.sendMessage("§7Advanced Punishment Management System");
        sender.sendMessage("");
        sender.sendMessage("§e/ban <player> [duration] [reason] §8- §7Ban a player");
        sender.sendMessage("§e/tempban <player> <duration> [reason] §8- §7Temporarily ban");
        sender.sendMessage("§e/ipban <player|ip> [duration] [reason] §8- §7IP ban");
        sender.sendMessage("§e/unban <player> §8- §7Unban a player");
        sender.sendMessage("");
        sender.sendMessage("§e/mute <player> [duration] [reason] §8- §7Mute a player");
        sender.sendMessage("§e/tempmute <player> <duration> [reason] §8- §7Temporarily mute");
        sender.sendMessage("§e/unmute <player> §8- §7Unmute a player");
        sender.sendMessage("");
        sender.sendMessage("§e/kick <player> [reason] §8- §7Kick a player");
        sender.sendMessage("§e/warn <player> [reason] §8- §7Warn a player");
        sender.sendMessage("§e/freeze <player> §8- §7Freeze a player");
        sender.sendMessage("");
        sender.sendMessage("§e/history <player> §8- §7View punishment history");
        sender.sendMessage("§e/checkban <player> §8- §7Check if banned");
        sender.sendMessage("§e/checkmute <player> §8- §7Check if muted");
        sender.sendMessage("");
        sender.sendMessage("§e/report <player> <reason> §8- §7Report a player");
        sender.sendMessage("§e/punish <player> §8- §7Open punishment GUI");
        sender.sendMessage("");
        sender.sendMessage("§e/litebansreborn reload §8- §7Reload configuration");
        sender.sendMessage("§e/litebansreborn info §8- §7Show plugin info");
        sender.sendMessage("§e/litebansreborn stats §8- §7Show punishment stats");
        sender.sendMessage("");
    }
    
    private void sendInfo(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage("§c§lLiteBansReborn §8- §7Plugin Information");
        sender.sendMessage("");
        sender.sendMessage("§7Version: §e" + plugin.getDescription().getVersion());
        sender.sendMessage("§7Author: §eNuvik");
        sender.sendMessage("§7Server: §e" + plugin.getConfigManager().getServerName());
        sender.sendMessage("§7Database: §e" + plugin.getDatabaseManager().getDatabaseType().name());
        sender.sendMessage("");
        
        // Cache stats
        var cacheStats = plugin.getCacheManager().getStats();
        sender.sendMessage("§7Cache Statistics:");
        sender.sendMessage("§8  - §7Players: §e" + cacheStats.get("players"));
        sender.sendMessage("§8  - §7Bans: §e" + cacheStats.get("bans"));
        sender.sendMessage("§8  - §7Mutes: §e" + cacheStats.get("mutes"));
        sender.sendMessage("§8  - §7Frozen: §e" + cacheStats.get("frozen"));
        sender.sendMessage("");
    }
    
    private void showStats(CommandSender sender) {
        sender.sendMessage("§eLoading statistics...");
        
        plugin.getHistoryManager().getStats().thenAccept(stats -> {
            sender.sendMessage("");
            sender.sendMessage("§c§lLiteBansReborn §8- §7Punishment Statistics");
            sender.sendMessage("");
            sender.sendMessage("§7Total Bans: §c" + stats.getTotalBans() + " §8(§a" + stats.getActiveBans() + " active§8)");
            sender.sendMessage("§7Total Mutes: §e" + stats.getTotalMutes() + " §8(§a" + stats.getActiveMutes() + " active§8)");
            sender.sendMessage("§7Total Kicks: §6" + stats.getTotalKicks());
            sender.sendMessage("§7Total Warnings: §d" + stats.getTotalWarns());
            sender.sendMessage("");
            sender.sendMessage("§7Grand Total: §f" + stats.getTotal() + " punishments");
            sender.sendMessage("");
        });
    }
}
