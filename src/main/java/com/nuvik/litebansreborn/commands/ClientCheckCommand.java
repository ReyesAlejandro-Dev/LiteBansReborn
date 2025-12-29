package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.antivpn.ClientDetector;
import com.nuvik.litebansreborn.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Client Check Command - Check what client/launcher a player is using
 * 
 * /clientcheck <player> - Check player's client
 * /clientcheck list - List all online players and their clients
 */
public class ClientCheckCommand implements CommandExecutor, TabCompleter {

    private final LiteBansReborn plugin;

    public ClientCheckCommand(LiteBansReborn plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("litebansreborn.clientcheck")) {
            plugin.getMessagesManager().send(sender, "general.no-permission");
            return true;
        }

        ClientDetector detector = plugin.getClientDetector();
        if (detector == null) {
            sender.sendMessage(ColorUtil.translate("&cClient detection is not enabled!"));
            return true;
        }

        if (args.length == 0) {
            if (sender instanceof Player player) {
                showClientInfo(sender, player);
            } else {
                sendHelp(sender);
            }
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "list" -> handleList(sender);
            case "modded" -> handleModded(sender);
            case "safe" -> handleSafe(sender);
            case "suspicious" -> handleSuspicious(sender);
            default -> handleCheck(sender, args[0]);
        }

        return true;
    }

    /**
     * Check a specific player's client
     */
    private void handleCheck(CommandSender sender, String playerName) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(ColorUtil.translate("&cPlayer &f" + playerName + " &cis not online."));
            return;
        }

        showClientInfo(sender, target);
    }

    /**
     * Show detailed client info for a player
     */
    private void showClientInfo(CommandSender sender, Player target) {
        ClientDetector.ClientInfo info = plugin.getClientDetector().getClientInfo(target);

        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        sender.sendMessage(ColorUtil.translate("&b&lClient Information: &f" + target.getName()));
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));

        if (info == null) {
            sender.sendMessage(ColorUtil.translate("&7Client: &fUnknown (No data yet)"));
            sender.sendMessage(ColorUtil.translate("&7Brand: &fUnknown"));
        } else {
            // Main client info
            sender.sendMessage(ColorUtil.translate("&7Detected Client: &f" + info.getDisplayName()));
            sender.sendMessage(ColorUtil.translate("&7Client Brand: &f" + (info.getBrand().isEmpty() ? "Unknown" : info.getBrand())));
            
            // Status indicators
            sender.sendMessage("");
            
            boolean isModded = info.hasModSupport();
            boolean isTrusted = info.isTrustedPvPClient();
            
            if (isTrusted) {
                sender.sendMessage(ColorUtil.translate("&a✓ Trusted PvP Client &7(Anti-cheat safe)"));
            } else if (isModded) {
                sender.sendMessage(ColorUtil.translate("&e⚠ Modded Client &7(Can load mods/cheats)"));
            } else {
                sender.sendMessage(ColorUtil.translate("&a✓ Vanilla-like Client"));
            }
            
            // Registered channels
            Set<String> channels = info.getChannels();
            if (!channels.isEmpty()) {
                sender.sendMessage("");
                sender.sendMessage(ColorUtil.translate("&7Registered Channels &7(&f" + channels.size() + "&7):"));
                
                int shown = 0;
                StringBuilder channelList = new StringBuilder();
                for (String channel : channels) {
                    if (shown > 0) channelList.append("&7, ");
                    channelList.append("&f").append(channel);
                    shown++;
                    
                    if (shown >= 5) {
                        if (channels.size() > 5) {
                            channelList.append(" &7... and ").append(channels.size() - 5).append(" more");
                        }
                        break;
                    }
                }
                sender.sendMessage(ColorUtil.translate("  " + channelList));
            }
        }

        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
    }

    /**
     * List all online players and their clients
     */
    private void handleList(CommandSender sender) {
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        sender.sendMessage(ColorUtil.translate("&b&lOnline Players - Client List"));
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));

        if (Bukkit.getOnlinePlayers().isEmpty()) {
            sender.sendMessage(ColorUtil.translate("&7No players online."));
        } else {
            for (Player player : Bukkit.getOnlinePlayers()) {
                ClientDetector.ClientInfo info = plugin.getClientDetector().getClientInfo(player);
                
                String clientName = info != null ? info.getDisplayName() : "Unknown";
                String statusColor;
                String statusIcon;
                
                if (info != null && info.isTrustedPvPClient()) {
                    statusColor = "&a";
                    statusIcon = "✓";
                } else if (info != null && info.hasModSupport()) {
                    statusColor = "&e";
                    statusIcon = "⚠";
                } else {
                    statusColor = "&f";
                    statusIcon = "•";
                }
                
                sender.sendMessage(ColorUtil.translate(
                        statusColor + statusIcon + " &f" + player.getName() + " &7- " + statusColor + clientName
                ));
            }
        }

        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        sender.sendMessage(ColorUtil.translate("&7Legend: &a✓ Trusted &e⚠ Modded &f• Vanilla"));
    }

    /**
     * List only modded clients
     */
    private void handleModded(CommandSender sender) {
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        sender.sendMessage(ColorUtil.translate("&e&lPlayers with Modded Clients"));
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));

        int count = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getClientDetector().isModdedClient(player.getUniqueId())) {
                ClientDetector.ClientInfo info = plugin.getClientDetector().getClientInfo(player);
                String client = info != null ? info.getDisplayName() : "Unknown";
                sender.sendMessage(ColorUtil.translate("&e⚠ &f" + player.getName() + " &7- &e" + client));
                count++;
            }
        }

        if (count == 0) {
            sender.sendMessage(ColorUtil.translate("&7No players with modded clients."));
        }

        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        sender.sendMessage(ColorUtil.translate("&7Total: &e" + count + " &7players with modded clients."));
    }

    /**
     * List only safe/trusted clients
     */
    private void handleSafe(CommandSender sender) {
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        sender.sendMessage(ColorUtil.translate("&a&lPlayers with Trusted Clients"));
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));

        int count = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getClientDetector().isSafeClient(player.getUniqueId())) {
                ClientDetector.ClientInfo info = plugin.getClientDetector().getClientInfo(player);
                String client = info != null ? info.getDisplayName() : "Vanilla";
                sender.sendMessage(ColorUtil.translate("&a✓ &f" + player.getName() + " &7- &a" + client));
                count++;
            }
        }

        if (count == 0) {
            sender.sendMessage(ColorUtil.translate("&7No players with trusted clients."));
        }

        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        sender.sendMessage(ColorUtil.translate("&7Total: &a" + count + " &7players with trusted clients."));
    }

    /**
     * List suspicious clients (modded but not trusted PvP clients)
     */
    private void handleSuspicious(CommandSender sender) {
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        sender.sendMessage(ColorUtil.translate("&c&lPotentially Suspicious Clients"));
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));

        int count = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            ClientDetector.ClientInfo info = plugin.getClientDetector().getClientInfo(player);
            if (info != null && info.hasModSupport() && !info.isTrustedPvPClient()) {
                sender.sendMessage(ColorUtil.translate("&c⚠ &f" + player.getName() + " &7- &c" + info.getDisplayName()));
                count++;
            }
        }

        if (count == 0) {
            sender.sendMessage(ColorUtil.translate("&7No suspicious clients detected."));
        }

        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        sender.sendMessage(ColorUtil.translate("&7Total: &c" + count + " &7potentially suspicious clients."));
        sender.sendMessage(ColorUtil.translate("&7These players can load mods/cheats."));
    }

    /**
     * Send help message
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        sender.sendMessage(ColorUtil.translate("&b&lClient Detection Commands"));
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        sender.sendMessage(ColorUtil.translate("&b/clientcheck <player> &7- Check player's client"));
        sender.sendMessage(ColorUtil.translate("&b/clientcheck list &7- List all players with clients"));
        sender.sendMessage(ColorUtil.translate("&b/clientcheck modded &7- List modded clients only"));
        sender.sendMessage(ColorUtil.translate("&b/clientcheck safe &7- List trusted clients only"));
        sender.sendMessage(ColorUtil.translate("&b/clientcheck suspicious &7- List suspicious clients"));
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("list");
            completions.add("modded");
            completions.add("safe");
            completions.add("suspicious");
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        }

        String current = args[args.length - 1].toLowerCase();
        completions.removeIf(s -> !s.toLowerCase().startsWith(current));
        return completions;
    }
}
