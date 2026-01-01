package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.managers.SocialNetworkManager;
import com.nuvik.litebansreborn.utils.ColorUtil;
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

/**
 * Network Command - View player relationships and connections
 */
public class NetworkCommand implements CommandExecutor, TabCompleter {

    private final LiteBansReborn plugin;

    public NetworkCommand(LiteBansReborn plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("litebansreborn.network")) {
            plugin.getMessagesManager().send(sender, "general.no-permission");
            return true;
        }

        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "alts" -> handleAlts(sender, args);
            case "connections" -> handleConnections(sender, args);
            case "check" -> handleCheck(sender, args);
            case "banned" -> handleBannedAssociates(sender, args);
            default -> showHelp(sender);
        }

        return true;
    }

    private void handleAlts(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.translate("&cUsage: /network alts <player>"));
            return;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        
        sender.sendMessage(ColorUtil.translate("&7Scanning for alt accounts..."));
        
        plugin.getSocialNetworkManager().getAlts(target.getUniqueId()).thenAccept(alts -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
                sender.sendMessage(ColorUtil.translate("&6🔗 Alt Accounts for &f" + target.getName()));
                sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
                
                if (alts.isEmpty()) {
                    sender.sendMessage(ColorUtil.translate("&7No alt accounts detected."));
                } else {
                    for (UUID altUuid : alts) {
                        OfflinePlayer alt = Bukkit.getOfflinePlayer(altUuid);
                        boolean isBanned = plugin.getBanManager().getActiveBan(altUuid).join() != null;
                        String status = isBanned ? "&c[BANNED]" : "&a[OK]";
                        sender.sendMessage(ColorUtil.translate("  &7- &f" + alt.getName() + " " + status));
                    }
                }
                
                sender.sendMessage(ColorUtil.translate("&7Total: &e" + alts.size() + " &7alt(s) found"));
                sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
            });
        });
    }

    private void handleConnections(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.translate("&cUsage: /network connections <player>"));
            return;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        
        plugin.getSocialNetworkManager().getRelationships(target.getUniqueId()).thenAccept(relations -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
                sender.sendMessage(ColorUtil.translate("&6🕸️ Connections for &f" + target.getName()));
                sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
                
                if (relations.isEmpty()) {
                    sender.sendMessage(ColorUtil.translate("&7No connections found."));
                } else {
                    int shown = 0;
                    for (var rel : relations) {
                        if (shown++ >= 15) {
                            sender.sendMessage(ColorUtil.translate("&7... and " + (relations.size() - 15) + " more"));
                            break;
                        }
                        OfflinePlayer other = Bukkit.getOfflinePlayer(rel.player());
                        String typeColor = switch (rel.type()) {
                            case ALT_ACCOUNT -> "&c";
                            case BANNED_ASSOCIATE -> "&4";
                            case FREQUENT_PARTNER -> "&e";
                            case SAME_SESSION -> "&7";
                        };
                        sender.sendMessage(ColorUtil.translate("  " + typeColor + "● &f" + other.getName() + 
                            " &8(" + rel.type().getDescription() + ", strength: " + rel.strength() + ")"));
                    }
                }
                
                sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
            });
        });
    }

    private void handleCheck(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.translate("&cUsage: /network check <player>"));
            return;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        
        sender.sendMessage(ColorUtil.translate("&7Running full network analysis..."));
        
        plugin.getSocialNetworkManager().getRelationships(target.getUniqueId()).thenAccept(relations -> {
            plugin.getSocialNetworkManager().getBannedConnectionScore(target.getUniqueId()).thenAccept(score -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    long alts = relations.stream()
                        .filter(r -> r.type() == SocialNetworkManager.RelationType.ALT_ACCOUNT)
                        .count();
                    long bannedAssociates = relations.stream()
                        .filter(r -> r.type() == SocialNetworkManager.RelationType.BANNED_ASSOCIATE)
                        .count();
                    
                    sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
                    sender.sendMessage(ColorUtil.translate("&6📊 Network Analysis: &f" + target.getName()));
                    sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
                    sender.sendMessage(ColorUtil.translate("  &7Alt Accounts: &e" + alts));
                    sender.sendMessage(ColorUtil.translate("  &7Connections: &e" + relations.size()));
                    sender.sendMessage(ColorUtil.translate("  &7Banned Associates: &c" + bannedAssociates));
                    sender.sendMessage(ColorUtil.translate("  &7Risk Score: " + getRiskColor(score) + score + "%"));
                    sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
                });
            });
        });
    }

    private void handleBannedAssociates(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.translate("&cUsage: /network banned <player>"));
            return;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        
        plugin.getSocialNetworkManager().getBannedAssociates(target.getUniqueId()).thenAccept(associates -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
                sender.sendMessage(ColorUtil.translate("&c⚠ Banned Associates of &f" + target.getName()));
                sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
                
                if (associates.isEmpty()) {
                    sender.sendMessage(ColorUtil.translate("&aNo banned associates found."));
                } else {
                    for (var assoc : associates) {
                        OfflinePlayer other = Bukkit.getOfflinePlayer(assoc.player());
                        sender.sendMessage(ColorUtil.translate("  &c● &f" + other.getName() + 
                            " &7(Relation: " + assoc.type().getDescription() + ")"));
                    }
                }
                
                sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
            });
        });
    }

    private String getRiskColor(int score) {
        if (score >= 70) return "&c";
        if (score >= 40) return "&e";
        return "&a";
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        sender.sendMessage(ColorUtil.translate("&6🕸️ Network Analysis Commands"));
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        sender.sendMessage(ColorUtil.translate("  &e/network alts <player> &7- Find alt accounts"));
        sender.sendMessage(ColorUtil.translate("  &e/network connections <player> &7- View all connections"));
        sender.sendMessage(ColorUtil.translate("  &e/network check <player> &7- Full network analysis"));
        sender.sendMessage(ColorUtil.translate("  &e/network banned <player> &7- Show banned associates"));
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(List.of("alts", "connections", "check", "banned"));
        } else if (args.length == 2) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        }

        String input = args[args.length - 1].toLowerCase();
        completions.removeIf(s -> !s.toLowerCase().startsWith(input));
        return completions;
    }
}
