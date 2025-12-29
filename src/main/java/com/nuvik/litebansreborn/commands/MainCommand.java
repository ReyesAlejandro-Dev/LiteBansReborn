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
            sendHelp(sender, 1);
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
                
            case "antivpn":
                if (!sender.hasPermission("litebansreborn.admin")) {
                    plugin.getMessagesManager().send(sender, "general.no-permission");
                    return true;
                }
                handleAntiVPN(sender, args);
                break;
                
            case "help":
                sendHelp(sender, 1);
                break;
            case "1":
                sendHelp(sender, 1);
                break;
            case "2":
                sendHelp(sender, 2);
                break;
            case "3":
                sendHelp(sender, 3);
                break;
            case "4":
                sendHelp(sender, 4);
                break;
            default:
                sendHelp(sender, 1);
                break;
        }
        
        return true;
    }
    
    private void handleAntiVPN(CommandSender sender, String[] args) {
        var vpnManager = plugin.getVPNManager();
        
        if (vpnManager == null) {
            sender.sendMessage("§cAnti-VPN is not initialized. Enable it in config.yml first.");
            return;
        }
        
        if (args.length < 2) {
            sendAntiVPNHelp(sender);
            return;
        }
        
        String action = args[1].toLowerCase();
        
        switch (action) {
            case "on", "enable" -> {
                vpnManager.setEnabled(true);
                sender.sendMessage("§a§lAnti-VPN §ahas been §2ENABLED");
            }
            case "off", "disable" -> {
                vpnManager.setEnabled(false);
                sender.sendMessage("§c§lAnti-VPN §chas been §4DISABLED");
            }
            case "alerts" -> {
                if (args.length < 3) {
                    boolean current = vpnManager.areAlertsEnabled();
                    sender.sendMessage("§7Alerts are currently: " + (current ? "§aON" : "§cOFF"));
                } else {
                    boolean enable = args[2].equalsIgnoreCase("on") || args[2].equalsIgnoreCase("true");
                    vpnManager.setAlertsEnabled(enable);
                    sender.sendMessage("§7Alerts have been " + (enable ? "§aENABLED" : "§cDISABLED"));
                }
            }
            case "action" -> {
                if (args.length < 3) {
                    sender.sendMessage("§7Current action: §e" + vpnManager.getEffectiveAction().name());
                    sender.sendMessage("§7Available: KICK, WARN, ALLOW, NONE");
                } else {
                    try {
                        var newAction = com.nuvik.litebansreborn.antivpn.VPNManager.VPNAction.valueOf(args[2].toUpperCase());
                        vpnManager.setAction(newAction);
                        sender.sendMessage("§aAction set to: §e" + newAction.name());
                    } catch (IllegalArgumentException e) {
                        sender.sendMessage("§cInvalid action. Use: KICK, WARN, ALLOW, NONE");
                    }
                }
            }
            case "status" -> {
                sendAntiVPNStatus(sender);
            }
            case "whitelist" -> {
                if (args.length < 3) {
                    var ips = vpnManager.getWhitelistedIPs();
                    sender.sendMessage("§7Whitelisted IPs (§f" + ips.size() + "§7):");
                    for (String ip : ips) {
                        sender.sendMessage("§8  - §f" + ip);
                    }
                } else if (args.length >= 4) {
                    String subAction = args[2].toLowerCase();
                    String ip = args[3];
                    if (subAction.equals("add")) {
                        vpnManager.whitelistIP(ip);
                        sender.sendMessage("§aAdded §f" + ip + " §ato whitelist");
                    } else if (subAction.equals("remove")) {
                        vpnManager.unwhitelistIP(ip);
                        sender.sendMessage("§cRemoved §f" + ip + " §cfrom whitelist");
                    }
                }
            }
            case "clearcache" -> {
                vpnManager.clearCache();
                sender.sendMessage("§aVPN cache cleared!");
            }
            case "providers" -> {
                sender.sendMessage("§7Active providers: §e" + vpnManager.getProviderCount());
            }
            default -> sendAntiVPNHelp(sender);
        }
    }
    
    private void sendAntiVPNStatus(CommandSender sender) {
        var vpnManager = plugin.getVPNManager();
        boolean enabled = vpnManager != null && vpnManager.isRuntimeEnabled();
        
        sender.sendMessage("§8§m----------------------------------------");
        sender.sendMessage("§c§lAnti-VPN Status");
        sender.sendMessage("§8§m----------------------------------------");
        sender.sendMessage("§7Status: " + (enabled ? "§a§lENABLED" : "§c§lDISABLED"));
        
        if (vpnManager != null) {
            sender.sendMessage("§7Alerts: " + (vpnManager.areAlertsEnabled() ? "§aON" : "§cOFF"));
            sender.sendMessage("§7Action: §e" + vpnManager.getEffectiveAction().name());
            sender.sendMessage("§7Providers: §f" + vpnManager.getProviderCount());
            sender.sendMessage("§7Cache Size: §f" + vpnManager.getCacheSize());
            sender.sendMessage("§7Whitelisted IPs: §f" + vpnManager.getWhitelistedIPs().size());
        }
        sender.sendMessage("§8§m----------------------------------------");
    }
    
    private void sendAntiVPNHelp(CommandSender sender) {
        sender.sendMessage("§8§m----------------------------------------");
        sender.sendMessage("§c§lAnti-VPN Administration");
        sender.sendMessage("§8§m----------------------------------------");
        sender.sendMessage("§c/lbr antivpn on §8- §7Enable Anti-VPN");
        sender.sendMessage("§c/lbr antivpn off §8- §7Disable Anti-VPN");
        sender.sendMessage("§c/lbr antivpn status §8- §7View current status");
        sender.sendMessage("§c/lbr antivpn alerts <on|off> §8- §7Toggle alerts");
        sender.sendMessage("§c/lbr antivpn action <KICK|WARN|ALLOW|NONE> §8- §7Set action");
        sender.sendMessage("§c/lbr antivpn whitelist §8- §7View whitelist");
        sender.sendMessage("§c/lbr antivpn whitelist add <ip> §8- §7Add to whitelist");
        sender.sendMessage("§c/lbr antivpn whitelist remove <ip> §8- §7Remove from whitelist");
        sender.sendMessage("§c/lbr antivpn clearcache §8- §7Clear VPN cache");
        sender.sendMessage("§c/lbr antivpn providers §8- §7Show active providers");
        sender.sendMessage("§8§m----------------------------------------");
    }
    
    private void sendHelp(CommandSender sender, int page) {
        final int TOTAL_PAGES = 4;
        page = Math.max(1, Math.min(page, TOTAL_PAGES));
        
        sender.sendMessage("§8§m----------------------------------------");
        sender.sendMessage("§c§lLiteBansReborn §8- §7v" + plugin.getDescription().getVersion());
        sender.sendMessage("§7Advanced Punishment Management System");
        sender.sendMessage("§8§m----------------------------------------");
        
        switch (page) {
            case 1 -> {
                sender.sendMessage("§6§l▸ Page 1/4 - Core Punishments");
                sender.sendMessage("");
                sender.sendMessage("§e/ban <player> [duration] [reason] §8- §7Ban a player");
                sender.sendMessage("§e/tempban <player> <duration> [reason] §8- §7Temporarily ban");
                sender.sendMessage("§e/ipban <player|ip> [duration] [reason] §8- §7IP ban");
                sender.sendMessage("§e/unban <player> §8- §7Unban a player");
                sender.sendMessage("");
                sender.sendMessage("§e/mute <player> [duration] [reason] §8- §7Mute a player");
                sender.sendMessage("§e/tempmute <player> <duration> [reason] §8- §7Temporarily mute");
                sender.sendMessage("§e/ipmute <player|ip> [duration] [reason] §8- §7IP mute");
                sender.sendMessage("§e/unmute <player> §8- §7Unmute a player");
                sender.sendMessage("");
                sender.sendMessage("§e/kick <player> [reason] §8- §7Kick a player");
                sender.sendMessage("§e/kickall [reason] §8- §7Kick all players");
                sender.sendMessage("§e/warn <player> [reason] §8- §7Warn a player");
                sender.sendMessage("§e/unwarn <player> [id] §8- §7Remove a warning");
            }
            case 2 -> {
                sender.sendMessage("§6§l▸ Page 2/4 - Staff Tools");
                sender.sendMessage("");
                sender.sendMessage("§e/freeze <player> [reason] §8- §7Freeze a player");
                sender.sendMessage("§e/unfreeze <player> §8- §7Unfreeze a player");
                sender.sendMessage("§e/vanish §8- §7Toggle vanish mode");
                sender.sendMessage("§e/staffchat <message> §8- §7Staff chat");
                sender.sendMessage("§e/ghostmute <player> §8- §7Ghost mute (they don't know)");
                sender.sendMessage("");
                sender.sendMessage("§e/history <player> §8- §7View punishment history");
                sender.sendMessage("§e/staffhistory <staff> §8- §7View staff's punishments");
                sender.sendMessage("§e/alts <player> §8- §7Check player alts");
                sender.sendMessage("§e/checkban <player> §8- §7Check if banned");
                sender.sendMessage("§e/checkmute <player> §8- §7Check if muted");
                sender.sendMessage("");
                sender.sendMessage("§e/report <player> <reason> §8- §7Report a player");
                sender.sendMessage("§e/reports §8- §7View pending reports");
                sender.sendMessage("§e/punish <player> §8- §7Open punishment GUI");
            }
            case 3 -> {
                sender.sendMessage("§6§l▸ Page 3/4 - Advanced Features (v4.0+)");
                sender.sendMessage("");
                sender.sendMessage("§e/hwid <ban|unban|check|alts> <player> §8- §7HWID banning");
                sender.sendMessage("§e/evidence <add|view|list> <player> §8- §7Evidence system");
                sender.sendMessage("§e/redemption <start|info> <player> §8- §7Redemption games");
                sender.sendMessage("");
                sender.sendMessage("§e/ticket <create|list|view|close> §8- §7Ticket system");
                sender.sendMessage("§e/verify [code] §8- §7Discord verification");
                sender.sendMessage("§e/whois <player> §8- §7Check Discord link");
                sender.sendMessage("");
                sender.sendMessage("§e/note <player> <text> §8- §7Add staff note");
                sender.sendMessage("§e/notes <player> §8- §7View staff notes");
                sender.sendMessage("§e/mutechat §8- §7Toggle global chat mute");
                sender.sendMessage("§e/slowmode <seconds> §8- §7Set chat slowmode");
            }
            case 4 -> {
                sender.sendMessage("§6§l▸ Page 4/4 - v5.0+ Intelligence Features");
                sender.sendMessage("");
                sender.sendMessage("§e/maintenance <on|off|add|remove|list> §8- §7Maintenance mode");
                sender.sendMessage("§e/rolesync <sync|add|remove|list> §8- §7Discord role sync");
                sender.sendMessage("");
                sender.sendMessage("§d/network <alts|connections|check|banned> §8- §7Social analysis");
                sender.sendMessage("§d/case <view|list|evidence|create> §8- §7Case files");
                sender.sendMessage("§d/risk <check|analyze|top> §8- §7Predictive moderation");
                sender.sendMessage("§d/ai <status|toxicity|analyze|appeal> §8- §7AI moderation");
                sender.sendMessage("");
                sender.sendMessage("§c/lbr reload §8- §7Reload configuration");
                sender.sendMessage("§c/lbr info §8- §7Show plugin info");
                sender.sendMessage("§c/lbr stats §8- §7Show punishment stats");
                sender.sendMessage("§c/lbr antivpn §8- §7Anti-VPN management");
            }
        }
        
        sender.sendMessage("§8§m----------------------------------------");
        sender.sendMessage("§7Use §e/lbr <1-4> §7to navigate pages");
        sender.sendMessage("§8§m----------------------------------------");
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
