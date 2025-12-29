package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.antivpn.VPNDatabase;
import com.nuvik.litebansreborn.antivpn.VPNManager;
import com.nuvik.litebansreborn.antivpn.VPNResult;
import com.nuvik.litebansreborn.config.MessagesManager;
import com.nuvik.litebansreborn.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * VPN Check Command - Check VPN status and manage Anti-VPN system
 * 
 * /vpncheck <player|ip> - Check VPN status
 * /vpncheck stats - View VPN statistics
 * /vpncheck recent [count] - View recent detections
 * /vpncheck history <player> - View player's IP history
 * /vpncheck whitelist <add|remove|list> [ip] - Manage whitelist
 * /vpncheck reload - Reload Anti-VPN config
 */
public class VPNCheckCommand implements CommandExecutor, TabCompleter {

    private final LiteBansReborn plugin;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public VPNCheckCommand(LiteBansReborn plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("litebansreborn.vpncheck")) {
            plugin.getMessagesManager().send(sender, "general.no-permission");
            return true;
        }

        VPNManager vpnManager = plugin.getVPNManager();
        if (vpnManager == null || !vpnManager.isEnabled()) {
            sender.sendMessage(ColorUtil.translate("&cAnti-VPN is not enabled! Enable it in config.yml"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "stats" -> handleStats(sender);
            case "recent" -> handleRecent(sender, args);
            case "history" -> handleHistory(sender, args);
            case "whitelist" -> handleWhitelist(sender, args);
            case "realip" -> handleRealIP(sender, args);
            case "reload" -> handleReload(sender);
            case "clearcache" -> handleClearCache(sender);
            default -> handleCheck(sender, args[0]);
        }

        return true;
    }

    /**
     * Check VPN status for player or IP
     */
    private void handleCheck(CommandSender sender, String target) {
        sender.sendMessage(ColorUtil.translate("&7Checking VPN status for &e" + target + "&7..."));

        // Check if it's a player name or IP
        String ip;
        String playerName = target;
        UUID playerUUID = null;

        Player targetPlayer = Bukkit.getPlayer(target);
        if (targetPlayer != null) {
            ip = targetPlayer.getAddress().getAddress().getHostAddress();
            playerUUID = targetPlayer.getUniqueId();
        } else if (isValidIP(target)) {
            ip = target;
            playerName = "Unknown";
        } else {
            sender.sendMessage(ColorUtil.translate("&cPlayer not found or invalid IP address."));
            return;
        }

        final String finalPlayerName = playerName;
        final CommandSender finalSender = sender;
        
        plugin.getVPNManager().checkIP(ip).thenAccept(result -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                sendVPNResult(finalSender, result, finalPlayerName);
            });
        }).exceptionally(ex -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                finalSender.sendMessage(ColorUtil.translate("&cFailed to check VPN status: " + ex.getMessage()));
            });
            return null;
        });
    }

    /**
     * Send VPN result to sender
     */
    private void sendVPNResult(CommandSender sender, VPNResult result, String playerName) {
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        sender.sendMessage(ColorUtil.translate("&c&lVPN Check Result"));
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        sender.sendMessage(ColorUtil.translate("&7Player: &f" + playerName));
        sender.sendMessage(ColorUtil.translate("&7IP: &f" + result.getIp()));
        sender.sendMessage("");
        
        if (result.isDangerous()) {
            sender.sendMessage(ColorUtil.translate("&c⚠ &eVPN/Proxy Detected!"));
            sender.sendMessage(ColorUtil.translate("&7Type: &c" + result.getType()));
        } else {
            sender.sendMessage(ColorUtil.translate("&a✓ &aNo VPN/Proxy Detected"));
        }
        
        sender.sendMessage("");
        sender.sendMessage(ColorUtil.translate("&7VPN: " + (result.isVPN() ? "&c✗ Yes" : "&a✓ No")));
        sender.sendMessage(ColorUtil.translate("&7Proxy: " + (result.isProxy() ? "&c✗ Yes" : "&a✓ No")));
        sender.sendMessage(ColorUtil.translate("&7Hosting: " + (result.isHosting() ? "&c✗ Yes" : "&a✓ No")));
        sender.sendMessage(ColorUtil.translate("&7Tor: " + (result.isTor() ? "&c✗ Yes" : "&a✓ No")));
        sender.sendMessage("");
        sender.sendMessage(ColorUtil.translate("&7Provider: &f" + (result.getServiceName().isEmpty() ? "Unknown" : result.getServiceName())));
        sender.sendMessage(ColorUtil.translate("&7ISP: &f" + (result.getIsp().isEmpty() ? "Unknown" : result.getIsp())));
        sender.sendMessage(ColorUtil.translate("&7Country: &f" + result.getCountry() + " &7(&f" + result.getCountryCode() + "&7)"));
        sender.sendMessage(ColorUtil.translate("&7City: &f" + (result.getCity().isEmpty() ? "Unknown" : result.getCity())));
        
        if (result.getRiskScore() > 0) {
            String riskColor = result.getRiskScore() > 80 ? "&c" : (result.getRiskScore() > 50 ? "&e" : "&a");
            sender.sendMessage(ColorUtil.translate("&7Risk Score: " + riskColor + String.format("%.1f%%", result.getRiskScore())));
        }
        
        if (!result.getRealIP().isEmpty()) {
            sender.sendMessage(ColorUtil.translate("&7Real IP: &e" + result.getRealIP()));
        }
        
        sender.sendMessage(ColorUtil.translate("&7API Provider: &f" + result.getApiProvider()));
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
    }

    /**
     * Show VPN statistics
     */
    private void handleStats(CommandSender sender) {
        plugin.getVPNManager().getStats().thenAccept(stats -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
                sender.sendMessage(ColorUtil.translate("&c&lAnti-VPN Statistics"));
                sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
                sender.sendMessage(ColorUtil.translate("&7Total Detections: &f" + stats.totalDetections));
                sender.sendMessage(ColorUtil.translate("&7Unique VPN IPs: &f" + stats.uniqueVPNIPs));
                sender.sendMessage(ColorUtil.translate("&7Detections Today: &f" + stats.detectionsToday));
                sender.sendMessage(ColorUtil.translate("&7Total Kicks: &c" + stats.totalKicks));
                sender.sendMessage(ColorUtil.translate("&7Total Warnings: &e" + stats.totalWarnings));
                sender.sendMessage("");
                sender.sendMessage(ColorUtil.translate("&7Top VPN Providers:"));
                
                int i = 1;
                for (VPNDatabase.ProviderStat provider : stats.topProviders) {
                    sender.sendMessage(ColorUtil.translate("  &f" + i + ". &e" + provider.name + " &7(" + provider.count + " detections)"));
                    i++;
                    if (i > 5) break;
                }
                
                sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
            });
        });
    }

    /**
     * Show recent VPN detections
     */
    private void handleRecent(CommandSender sender, String[] args) {
        int limit = 10;
        if (args.length > 1) {
            try {
                limit = Math.min(50, Math.max(1, Integer.parseInt(args[1])));
            } catch (NumberFormatException e) {
                limit = 10;
            }
        }

        int finalLimit = limit;
        plugin.getVPNManager().getRecentDetections(limit).thenAccept(detections -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
                sender.sendMessage(ColorUtil.translate("&c&lRecent VPN Detections &7(Last " + finalLimit + ")"));
                sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
                
                if (detections.isEmpty()) {
                    sender.sendMessage(ColorUtil.translate("&7No VPN detections found."));
                } else {
                    for (VPNDatabase.VPNDetectionRecord record : detections) {
                        String time = dateFormat.format(new Date(record.detectedAt));
                        String action = record.actionTaken != null ? record.actionTaken : "LOG";
                        String actionColor = action.equals("KICK") ? "&c" : (action.equals("WARN") ? "&e" : "&7");
                        
                        sender.sendMessage(ColorUtil.translate(
                                "&7[&f" + time + "&7] &f" + record.playerName + 
                                " &7(" + record.ip + ") &7- &e" + 
                                (record.vpnProvider != null ? record.vpnProvider : "Unknown") +
                                " " + actionColor + "[" + action + "]"
                        ));
                    }
                }
                
                sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
            });
        });
    }

    /**
     * Show player's IP history
     */
    private void handleHistory(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.translate("&cUsage: /vpncheck history <player>"));
            return;
        }

        String playerName = args[1];
        Player target = Bukkit.getPlayer(playerName);
        
        if (target == null) {
            // Try to get UUID from offline player
            @SuppressWarnings("deprecation")
            org.bukkit.OfflinePlayer offline = Bukkit.getOfflinePlayer(playerName);
            if (!offline.hasPlayedBefore()) {
                sender.sendMessage(ColorUtil.translate("&cPlayer not found."));
                return;
            }
            showPlayerIPHistory(sender, offline.getUniqueId(), playerName);
        } else {
            showPlayerIPHistory(sender, target.getUniqueId(), target.getName());
        }
    }

    private void showPlayerIPHistory(CommandSender sender, UUID uuid, String playerName) {
        plugin.getVPNManager().getPlayerIPs(uuid).thenAccept(ips -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
                sender.sendMessage(ColorUtil.translate("&c&lIP History: &f" + playerName));
                sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
                
                if (ips.isEmpty()) {
                    sender.sendMessage(ColorUtil.translate("&7No IP history found."));
                } else {
                    // Also get likely real IP
                    plugin.getVPNManager().getLikelyRealIP(uuid).thenAccept(realIP -> {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            if (realIP != null) {
                                sender.sendMessage(ColorUtil.translate("&7Likely Real IP: &a" + realIP));
                                sender.sendMessage("");
                            }
                            
                            sender.sendMessage(ColorUtil.translate("&7All IPs:"));
                            for (VPNDatabase.TrackedIP ip : ips) {
                                String vpnStatus = ip.isVPN ? "&c[VPN]" : "&a[Clean]";
                                String lastSeen = dateFormat.format(new Date(ip.lastSeen));
                                sender.sendMessage(ColorUtil.translate(
                                        "  &f" + ip.ip + " " + vpnStatus + 
                                        " &7- " + ip.loginCount + " logins, last: " + lastSeen
                                ));
                            }
                            
                            sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
                        });
                    });
                }
            });
        });
    }

    /**
     * Handle whitelist commands
     */
    private void handleWhitelist(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.translate("&cUsage: /vpncheck whitelist <add|remove|list> [ip]"));
            return;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "add" -> {
                if (args.length < 3) {
                    sender.sendMessage(ColorUtil.translate("&cUsage: /vpncheck whitelist add <ip>"));
                    return;
                }
                String ip = args[2];
                plugin.getVPNManager().whitelistIP(ip);
                sender.sendMessage(ColorUtil.translate("&aIP &f" + ip + " &ahas been whitelisted."));
            }
            case "remove" -> {
                if (args.length < 3) {
                    sender.sendMessage(ColorUtil.translate("&cUsage: /vpncheck whitelist remove <ip>"));
                    return;
                }
                String ip = args[2];
                plugin.getVPNManager().unwhitelistIP(ip);
                sender.sendMessage(ColorUtil.translate("&cIP &f" + ip + " &chas been removed from whitelist."));
            }
            case "list" -> {
                sender.sendMessage(ColorUtil.translate("&7Whitelisted IPs are stored in config.yml"));
            }
        }
    }

    /**
     * Handle real IP lookup
     */
    private void handleRealIP(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.translate("&cUsage: /vpncheck realip <player>"));
            return;
        }

        String playerName = args[1];
        Player target = Bukkit.getPlayer(playerName);
        UUID uuid;
        
        if (target != null) {
            uuid = target.getUniqueId();
        } else {
            @SuppressWarnings("deprecation")
            org.bukkit.OfflinePlayer offline = Bukkit.getOfflinePlayer(playerName);
            if (!offline.hasPlayedBefore()) {
                sender.sendMessage(ColorUtil.translate("&cPlayer not found."));
                return;
            }
            uuid = offline.getUniqueId();
        }

        plugin.getVPNManager().getLikelyRealIP(uuid).thenAccept(realIP -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (realIP != null) {
                    sender.sendMessage(ColorUtil.translate("&7Likely real IP for &f" + playerName + "&7: &a" + realIP));
                } else {
                    sender.sendMessage(ColorUtil.translate("&cCould not determine real IP for " + playerName));
                }
            });
        });
    }

    /**
     * Reload Anti-VPN config
     */
    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("litebansreborn.admin")) {
            plugin.getMessagesManager().send(sender, "general.no-permission");
            return;
        }

        plugin.getVPNManager().clearCache();
        sender.sendMessage(ColorUtil.translate("&aAnti-VPN cache cleared and configuration will reload."));
    }

    /**
     * Clear cache
     */
    private void handleClearCache(CommandSender sender) {
        if (!sender.hasPermission("litebansreborn.admin")) {
            plugin.getMessagesManager().send(sender, "general.no-permission");
            return;
        }

        plugin.getVPNManager().clearCache();
        sender.sendMessage(ColorUtil.translate("&aAnti-VPN cache cleared."));
    }

    /**
     * Send help message
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        sender.sendMessage(ColorUtil.translate("&c&lAnti-VPN Commands"));
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        sender.sendMessage(ColorUtil.translate("&c/vpncheck <player|ip> &7- Check VPN status"));
        sender.sendMessage(ColorUtil.translate("&c/vpncheck stats &7- View statistics"));
        sender.sendMessage(ColorUtil.translate("&c/vpncheck recent [count] &7- Recent detections"));
        sender.sendMessage(ColorUtil.translate("&c/vpncheck history <player> &7- Player IP history"));
        sender.sendMessage(ColorUtil.translate("&c/vpncheck realip <player> &7- Get likely real IP"));
        sender.sendMessage(ColorUtil.translate("&c/vpncheck whitelist <add|remove> <ip> &7- Manage whitelist"));
        sender.sendMessage(ColorUtil.translate("&c/vpncheck clearcache &7- Clear VPN cache"));
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
    }

    /**
     * Check if string is valid IP address
     */
    private boolean isValidIP(String ip) {
        String[] parts = ip.split("\\.");
        if (parts.length != 4) return false;
        
        try {
            for (String part : parts) {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) return false;
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("stats", "recent", "history", "whitelist", "realip", "clearcache"));
            // Add online players
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("whitelist")) {
                completions.addAll(Arrays.asList("add", "remove", "list"));
            } else if (args[0].equalsIgnoreCase("history") || args[0].equalsIgnoreCase("realip")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    completions.add(player.getName());
                }
            }
        }

        String current = args[args.length - 1].toLowerCase();
        completions.removeIf(s -> !s.toLowerCase().startsWith(current));
        return completions;
    }
}
