package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.config.MessagesManager;
import com.nuvik.litebansreborn.managers.HistoryManager;
import com.nuvik.litebansreborn.utils.ColorUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Main plugin command - /litebansreborn
 * Now with TabCompleter and configurable messages!
 */
public class MainCommand implements CommandExecutor, TabCompleter {

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
                plugin.getMessagesManager().send(sender, "main-command.reload-success");
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
                plugin.getMessagesManager().send(sender, "main-command.import-coming-soon");
                break;
                
            case "export":
                if (!sender.hasPermission("litebansreborn.admin")) {
                    plugin.getMessagesManager().send(sender, "general.no-permission");
                    return true;
                }
                plugin.getMessagesManager().send(sender, "main-command.export-coming-soon");
                break;
                
            case "antivpn":
                if (!sender.hasPermission("litebansreborn.admin")) {
                    plugin.getMessagesManager().send(sender, "general.no-permission");
                    return true;
                }
                handleAntiVPN(sender, args);
                break;
                
            case "web":
                handleWeb(sender, args);
                break;
                
            case "debug":
                if (!sender.hasPermission("litebansreborn.admin")) {
                    plugin.getMessagesManager().send(sender, "general.no-permission");
                    return true;
                }
                handleDebug(sender);
                break;
                
            case "help":
                int page = 1;
                if (args.length > 1) {
                    try {
                        page = Integer.parseInt(args[1]);
                    } catch (NumberFormatException ignored) {}
                }
                sendHelp(sender, page);
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
                // Try to parse as page number
                try {
                    int pageNum = Integer.parseInt(args[0]);
                    if (pageNum >= 1 && pageNum <= 4) {
                        sendHelp(sender, pageNum);
                        return true;
                    }
                } catch (NumberFormatException ignored) {}
                sendHelp(sender, 1);
                break;
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Main subcommands
            List<String> subcommands = new ArrayList<>(Arrays.asList("help", "info", "web"));
            
            // Admin-only subcommands
            if (sender.hasPermission("litebansreborn.admin")) {
                subcommands.addAll(Arrays.asList("reload", "stats", "import", "export", "antivpn", "debug"));
            }
            
            // Page numbers
            subcommands.addAll(Arrays.asList("1", "2", "3", "4"));
            
            String input = args[0].toLowerCase();
            completions = subcommands.stream()
                .filter(s -> s.toLowerCase().startsWith(input))
                .collect(Collectors.toList());
                
        } else if (args.length == 2) {
            String subcommand = args[0].toLowerCase();
            
            if (subcommand.equals("antivpn") && sender.hasPermission("litebansreborn.admin")) {
                List<String> antivpnSubs = Arrays.asList(
                    "on", "off", "enable", "disable", "status", 
                    "alerts", "action", "whitelist", "clearcache", "providers"
                );
                String input = args[1].toLowerCase();
                completions = antivpnSubs.stream()
                    .filter(s -> s.startsWith(input))
                    .collect(Collectors.toList());
                    
            } else if (subcommand.equals("help")) {
                completions = Arrays.asList("1", "2", "3", "4").stream()
                    .filter(s -> s.startsWith(args[1]))
                    .collect(Collectors.toList());
                    
            } else if (subcommand.equals("web")) {
                completions = Arrays.asList("on", "off", "view", "url").stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }
            
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("antivpn")) {
                String action = args[1].toLowerCase();
                
                if (action.equals("alerts")) {
                    completions = Arrays.asList("on", "off").stream()
                        .filter(s -> s.startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
                        
                } else if (action.equals("action")) {
                    completions = Arrays.asList("KICK", "WARN", "ALLOW", "NONE").stream()
                        .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
                        
                } else if (action.equals("whitelist")) {
                    completions = Arrays.asList("add", "remove").stream()
                        .filter(s -> s.startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
                }
            }
        }
        
        return completions;
    }
    
    private void handleDebug(CommandSender sender) {
        boolean debugEnabled = plugin.getConfigManager().isDebugEnabled();
        
        sender.sendMessage(plugin.getMessagesManager().get("main-command.debug.header"));
        sender.sendMessage(plugin.getMessagesManager().get("main-command.debug.status", 
            "status", debugEnabled ? "&aEnabled" : "&cDisabled"));
        sender.sendMessage(plugin.getMessagesManager().get("main-command.debug.database", 
            "type", plugin.getDatabaseManager().getDatabaseType().name()));
        sender.sendMessage(plugin.getMessagesManager().get("main-command.debug.cache", 
            "players", String.valueOf(plugin.getCacheManager().getStats().get("players")),
            "bans", String.valueOf(plugin.getCacheManager().getStats().get("bans")),
            "mutes", String.valueOf(plugin.getCacheManager().getStats().get("mutes"))));
        
        // Check managers status
        sender.sendMessage(plugin.getMessagesManager().get("main-command.debug.managers-header"));
        
        checkManager(sender, "VPN Manager", plugin.getVPNManager() != null);
        checkManager(sender, "HWID Manager", plugin.getHWIDManager() != null);
        checkManager(sender, "Discord Bot", plugin.getDiscordBotManager() != null && plugin.getDiscordBotManager().isEnabled());
        checkManager(sender, "AI Manager", plugin.getAIManager() != null && plugin.getAIManager().isEnabled());
        checkManager(sender, "Cross Server", plugin.getCrossServerManager() != null);
        checkManager(sender, "Predictive", plugin.getPredictiveManager() != null);
        
        sender.sendMessage(plugin.getMessagesManager().get("main-command.debug.footer"));
    }
    
    private void checkManager(CommandSender sender, String name, boolean enabled) {
        String status = enabled ? "&a✓" : "&c✗";
        sender.sendMessage(ColorUtil.translate("  &7" + name + ": " + status));
    }
    
    private void handleAntiVPN(CommandSender sender, String[] args) {
        var vpnManager = plugin.getVPNManager();
        
        if (vpnManager == null) {
            plugin.getMessagesManager().send(sender, "main-command.antivpn.not-initialized");
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
                plugin.getMessagesManager().send(sender, "main-command.antivpn.enabled");
            }
            case "off", "disable" -> {
                vpnManager.setEnabled(false);
                plugin.getMessagesManager().send(sender, "main-command.antivpn.disabled");
            }
            case "alerts" -> {
                if (args.length < 3) {
                    boolean current = vpnManager.areAlertsEnabled();
                    plugin.getMessagesManager().send(sender, "main-command.antivpn.alerts-status",
                        "status", current ? "&aON" : "&cOFF");
                } else {
                    boolean enable = args[2].equalsIgnoreCase("on") || args[2].equalsIgnoreCase("true");
                    vpnManager.setAlertsEnabled(enable);
                    plugin.getMessagesManager().send(sender, "main-command.antivpn.alerts-changed",
                        "status", enable ? "&aENABLED" : "&cDISABLED");
                }
            }
            case "action" -> {
                if (args.length < 3) {
                    plugin.getMessagesManager().send(sender, "main-command.antivpn.action-current",
                        "action", vpnManager.getEffectiveAction().name());
                } else {
                    try {
                        var newAction = com.nuvik.litebansreborn.antivpn.VPNManager.VPNAction.valueOf(args[2].toUpperCase());
                        vpnManager.setAction(newAction);
                        plugin.getMessagesManager().send(sender, "main-command.antivpn.action-set",
                            "action", newAction.name());
                    } catch (IllegalArgumentException e) {
                        plugin.getMessagesManager().send(sender, "main-command.antivpn.action-invalid");
                    }
                }
            }
            case "status" -> {
                sendAntiVPNStatus(sender);
            }
            case "whitelist" -> {
                if (args.length < 3) {
                    var ips = vpnManager.getWhitelistedIPs();
                    plugin.getMessagesManager().send(sender, "main-command.antivpn.whitelist-header",
                        "count", String.valueOf(ips.size()));
                    for (String ip : ips) {
                        sender.sendMessage(ColorUtil.translate("  &8- &f" + ip));
                    }
                } else if (args.length >= 4) {
                    String subAction = args[2].toLowerCase();
                    String ip = args[3];
                    if (subAction.equals("add")) {
                        vpnManager.whitelistIP(ip);
                        plugin.getMessagesManager().send(sender, "main-command.antivpn.whitelist-added", "ip", ip);
                    } else if (subAction.equals("remove")) {
                        vpnManager.unwhitelistIP(ip);
                        plugin.getMessagesManager().send(sender, "main-command.antivpn.whitelist-removed", "ip", ip);
                    }
                }
            }
            case "clearcache" -> {
                vpnManager.clearCache();
                plugin.getMessagesManager().send(sender, "main-command.antivpn.cache-cleared");
            }
            case "providers" -> {
                plugin.getMessagesManager().send(sender, "main-command.antivpn.providers",
                    "count", String.valueOf(vpnManager.getProviderCount()));
            }
            default -> sendAntiVPNHelp(sender);
        }
    }
    
    private void sendAntiVPNStatus(CommandSender sender) {
        var vpnManager = plugin.getVPNManager();
        boolean enabled = vpnManager != null && vpnManager.isRuntimeEnabled();
        
        for (String line : plugin.getMessagesManager().getList("main-command.antivpn.status")) {
            String formatted = line
                .replace("%enabled%", enabled ? "&a&lENABLED" : "&c&lDISABLED")
                .replace("%alerts%", vpnManager != null && vpnManager.areAlertsEnabled() ? "&aON" : "&cOFF")
                .replace("%action%", vpnManager != null ? vpnManager.getEffectiveAction().name() : "N/A")
                .replace("%providers%", vpnManager != null ? String.valueOf(vpnManager.getProviderCount()) : "0")
                .replace("%cache_size%", vpnManager != null ? String.valueOf(vpnManager.getCacheSize()) : "0")
                .replace("%whitelisted%", vpnManager != null ? String.valueOf(vpnManager.getWhitelistedIPs().size()) : "0");
            sender.sendMessage(ColorUtil.translate(formatted));
        }
    }
    
    private void sendAntiVPNHelp(CommandSender sender) {
        for (String line : plugin.getMessagesManager().getList("main-command.antivpn.help")) {
            sender.sendMessage(ColorUtil.translate(line));
        }
    }
    
    private void sendHelp(CommandSender sender, int page) {
        final int TOTAL_PAGES = 4;
        page = Math.max(1, Math.min(page, TOTAL_PAGES));
        
        // Header
        for (String line : plugin.getMessagesManager().getList("main-command.help.header")) {
            String formatted = line
                .replace("%version%", plugin.getDescription().getVersion())
                .replace("%page%", String.valueOf(page))
                .replace("%total_pages%", String.valueOf(TOTAL_PAGES));
            sender.sendMessage(ColorUtil.translate(formatted));
        }
        
        // Page content
        String pagePath = "main-command.help.page-" + page;
        if (plugin.getMessagesManager().contains(pagePath)) {
            for (String line : plugin.getMessagesManager().getList(pagePath)) {
                sender.sendMessage(ColorUtil.translate(line));
            }
        } else {
            // Fallback to hardcoded if not configured yet
            sendHelpFallback(sender, page);
        }
        
        // Footer
        for (String line : plugin.getMessagesManager().getList("main-command.help.footer")) {
            String formatted = line
                .replace("%page%", String.valueOf(page))
                .replace("%total_pages%", String.valueOf(TOTAL_PAGES));
            sender.sendMessage(ColorUtil.translate(formatted));
        }
    }
    
    /**
     * Fallback help in case messages aren't configured
     */
    private void sendHelpFallback(CommandSender sender, int page) {
        switch (page) {
            case 1 -> {
                sender.sendMessage(ColorUtil.translate("&6&l▸ Page 1/4 - Core Punishments"));
                sender.sendMessage("");
                sender.sendMessage(ColorUtil.translate("&e/ban <player> [duration] [reason] &8- &7Ban a player"));
                sender.sendMessage(ColorUtil.translate("&e/tempban <player> <duration> [reason] &8- &7Temporarily ban"));
                sender.sendMessage(ColorUtil.translate("&e/ipban <player|ip> [duration] [reason] &8- &7IP ban"));
                sender.sendMessage(ColorUtil.translate("&e/unban <player> &8- &7Unban a player"));
                sender.sendMessage("");
                sender.sendMessage(ColorUtil.translate("&e/mute <player> [duration] [reason] &8- &7Mute a player"));
                sender.sendMessage(ColorUtil.translate("&e/tempmute <player> <duration> [reason] &8- &7Temporarily mute"));
                sender.sendMessage(ColorUtil.translate("&e/unmute <player> &8- &7Unmute a player"));
                sender.sendMessage("");
                sender.sendMessage(ColorUtil.translate("&e/kick <player> [reason] &8- &7Kick a player"));
                sender.sendMessage(ColorUtil.translate("&e/warn <player> [reason] &8- &7Warn a player"));
            }
            case 2 -> {
                sender.sendMessage(ColorUtil.translate("&6&l▸ Page 2/4 - Staff Tools"));
                sender.sendMessage("");
                sender.sendMessage(ColorUtil.translate("&e/freeze <player> [reason] &8- &7Freeze a player"));
                sender.sendMessage(ColorUtil.translate("&e/unfreeze <player> &8- &7Unfreeze a player"));
                sender.sendMessage(ColorUtil.translate("&e/staffchat <message> &8- &7Staff chat"));
                sender.sendMessage(ColorUtil.translate("&e/ghostmute <player> &8- &7Ghost mute"));
                sender.sendMessage("");
                sender.sendMessage(ColorUtil.translate("&e/history <player> &8- &7View punishment history"));
                sender.sendMessage(ColorUtil.translate("&e/alts <player> &8- &7Check player alts"));
                sender.sendMessage(ColorUtil.translate("&e/checkban <player> &8- &7Check if banned"));
                sender.sendMessage(ColorUtil.translate("&e/report <player> <reason> &8- &7Report a player"));
                sender.sendMessage(ColorUtil.translate("&e/punish <player> &8- &7Open punishment GUI"));
            }
            case 3 -> {
                sender.sendMessage(ColorUtil.translate("&6&l▸ Page 3/4 - Advanced Features (v4.0+)"));
                sender.sendMessage("");
                sender.sendMessage(ColorUtil.translate("&e/hwid <ban|unban|check|alts> <player> &8- &7HWID banning"));
                sender.sendMessage(ColorUtil.translate("&e/evidence <add|view|list> <player> &8- &7Evidence system"));
                sender.sendMessage(ColorUtil.translate("&e/redemption <start|info> <player> &8- &7Redemption games"));
                sender.sendMessage("");
                sender.sendMessage(ColorUtil.translate("&e/ticket <create|list|view|close> &8- &7Ticket system"));
                sender.sendMessage(ColorUtil.translate("&e/verify [code] &8- &7Discord verification"));
                sender.sendMessage(ColorUtil.translate("&e/note <player> <text> &8- &7Add staff note"));
                sender.sendMessage(ColorUtil.translate("&e/notes <player> &8- &7View staff notes"));
            }
            case 4 -> {
                sender.sendMessage(ColorUtil.translate("&6&l▸ Page 4/4 - v5.0+ Intelligence Features"));
                sender.sendMessage("");
                sender.sendMessage(ColorUtil.translate("&e/maintenance <on|off|add|remove|list> &8- &7Maintenance mode"));
                sender.sendMessage(ColorUtil.translate("&e/rolesync <sync|add|remove|list> &8- &7Discord role sync"));
                sender.sendMessage("");
                sender.sendMessage(ColorUtil.translate("&d/network <alts|connections|check|banned> &8- &7Social analysis"));
                sender.sendMessage(ColorUtil.translate("&d/case <view|list|evidence|create> &8- &7Case files"));
                sender.sendMessage(ColorUtil.translate("&d/risk <check|analyze|top> &8- &7Predictive moderation"));
                sender.sendMessage(ColorUtil.translate("&d/ai <status|toxicity|analyze|appeal> &8- &7AI moderation"));
                sender.sendMessage("");
                sender.sendMessage(ColorUtil.translate("&c/lbr reload &8- &7Reload configuration"));
                sender.sendMessage(ColorUtil.translate("&c/lbr info &8- &7Show plugin info"));
                sender.sendMessage(ColorUtil.translate("&c/lbr stats &8- &7Show punishment stats"));
                sender.sendMessage(ColorUtil.translate("&c/lbr antivpn &8- &7Anti-VPN management"));
            }
        }
    }
    
    private void sendInfo(CommandSender sender) {
        for (String line : plugin.getMessagesManager().getList("main-command.info")) {
            String formatted = line
                .replace("%version%", plugin.getDescription().getVersion())
                .replace("%server%", plugin.getConfigManager().getServerName())
                .replace("%database%", plugin.getDatabaseManager().getDatabaseType().name())
                .replace("%players_cached%", String.valueOf(plugin.getCacheManager().getStats().get("players")))
                .replace("%bans_cached%", String.valueOf(plugin.getCacheManager().getStats().get("bans")))
                .replace("%mutes_cached%", String.valueOf(plugin.getCacheManager().getStats().get("mutes")))
                .replace("%frozen_cached%", String.valueOf(plugin.getCacheManager().getStats().get("frozen")))
                .replace("%uptime%", getUptime());
            sender.sendMessage(ColorUtil.translate(formatted));
        }
    }
    
    private String getUptime() {
        long uptime = System.currentTimeMillis() - plugin.getStartTime();
        long seconds = uptime / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return days + "d " + (hours % 24) + "h " + (minutes % 60) + "m";
        } else if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m " + (seconds % 60) + "s";
        } else if (minutes > 0) {
            return minutes + "m " + (seconds % 60) + "s";
        } else {
            return seconds + "s";
        }
    }
    
    private void showStats(CommandSender sender) {
        plugin.getMessagesManager().send(sender, "main-command.stats.loading");
        
        plugin.getHistoryManager().getStats().thenAccept(stats -> {
            for (String line : plugin.getMessagesManager().getList("main-command.stats.result")) {
                String formatted = line
                    .replace("%total_bans%", String.valueOf(stats.getTotalBans()))
                    .replace("%active_bans%", String.valueOf(stats.getActiveBans()))
                    .replace("%total_mutes%", String.valueOf(stats.getTotalMutes()))
                    .replace("%active_mutes%", String.valueOf(stats.getActiveMutes()))
                    .replace("%total_kicks%", String.valueOf(stats.getTotalKicks()))
                    .replace("%total_warns%", String.valueOf(stats.getTotalWarns()))
                    .replace("%total%", String.valueOf(stats.getTotal()));
                sender.sendMessage(ColorUtil.translate(formatted));
            }
        });
    }

    private void handleWeb(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.translate("&cUsage: /lbr web <on|off|view|url>"));
            return;
        }
        
        String action = args[1].toLowerCase();
        var webPanel = plugin.getWebPanelServer();
        
        switch (action) {
            case "on", "enable" -> {
                if (!sender.hasPermission("litebansreborn.admin")) {
                    plugin.getMessagesManager().send(sender, "general.no-permission");
                    return;
                }
                if (webPanel != null && webPanel.isRunning()) {
                    sender.sendMessage(ColorUtil.translate("&cWeb Panel is already running!"));
                    return;
                }
                // Update config
                plugin.getConfigManager().getConfig().set("web-panel.enabled", true);
                plugin.getConfigManager().save();
                plugin.reload(); // Quick reload to start web server
                
                sender.sendMessage(ColorUtil.translate("&aWeb Panel enabled and started!"));
                if (plugin.getWebPanelServer() != null) {
                    sender.sendMessage(ColorUtil.translate("&7URL: &b" + plugin.getWebPanelServer().getWebURL()));
                }
            }
            case "off", "disable" -> {
                if (!sender.hasPermission("litebansreborn.admin")) {
                    plugin.getMessagesManager().send(sender, "general.no-permission");
                    return;
                }
                if (webPanel == null || !webPanel.isRunning()) {
                    sender.sendMessage(ColorUtil.translate("&cWeb Panel is not running!"));
                    return;
                }
                plugin.getConfigManager().getConfig().set("web-panel.enabled", false);
                plugin.getConfigManager().save();
                plugin.reload();
                sender.sendMessage(ColorUtil.translate("&cWeb Panel disabled and stopped."));
            }
            case "view", "url" -> {
                if (!sender.hasPermission("litebansreborn.web.view")) {
                    plugin.getMessagesManager().send(sender, "general.no-permission");
                    return;
                }
                if (webPanel == null || !webPanel.isRunning()) {
                    sender.sendMessage(ColorUtil.translate("&cWeb Panel is currently disabled. Ask an admin to enable it."));
                    return;
                }
                
                String url = webPanel.getWebURL();
                sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
                sender.sendMessage(ColorUtil.translate("&b&lWeb Panel Available!"));
                sender.sendMessage(ColorUtil.translate("&7Click to open:"));
                sender.sendMessage(ColorUtil.translate("&a" + url));
                sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
            }
            default -> sender.sendMessage(ColorUtil.translate("&cUnknown option. Use: on, off, view"));
        }
    }
}
