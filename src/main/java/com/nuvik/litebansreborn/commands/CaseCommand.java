package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.managers.CaseFileManager;
import com.nuvik.litebansreborn.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Case Command - View and manage case files
 */
public class CaseCommand implements CommandExecutor, TabCompleter {

    private final LiteBansReborn plugin;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public CaseCommand(LiteBansReborn plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("litebansreborn.case")) {
            plugin.getMessagesManager().send(sender, "general.no-permission");
            return true;
        }

        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "view" -> handleView(sender, args);
            case "list" -> handleList(sender, args);
            case "evidence" -> handleEvidence(sender, args);
            case "create" -> handleCreate(sender, args);
            default -> showHelp(sender);
        }

        return true;
    }

    private void handleView(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.translate("&cUsage: /case view <case_id>"));
            return;
        }

        String caseId = args[1].toUpperCase();
        
        plugin.getCaseFileManager().getCaseFile(caseId).thenAccept(caseFile -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (caseFile == null) {
                    sender.sendMessage(ColorUtil.translate("&cCase file not found: " + caseId));
                    return;
                }
                
                sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
                sender.sendMessage(ColorUtil.translate("&6📁 Case File: &f#" + caseFile.caseId()));
                sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
                sender.sendMessage(ColorUtil.translate("  &7Target: &f" + caseFile.targetName()));
                sender.sendMessage(ColorUtil.translate("  &7Created by: &f" + caseFile.creatorName()));
                sender.sendMessage(ColorUtil.translate("  &7Date: &f" + dateFormat.format(new Date(caseFile.createdAt()))));
                sender.sendMessage(ColorUtil.translate(""));
                sender.sendMessage(ColorUtil.translate("  &7Use &e/case evidence " + caseId + " &7to view evidence."));
                sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
            });
        });
    }

    private void handleList(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.translate("&cUsage: /case list <player>"));
            return;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        
        plugin.getCaseFileManager().getCasesForPlayer(target.getUniqueId()).thenAccept(cases -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
                sender.sendMessage(ColorUtil.translate("&6📁 Cases for &f" + target.getName()));
                sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
                
                if (cases.isEmpty()) {
                    sender.sendMessage(ColorUtil.translate("&7No case files found."));
                } else {
                    for (var caseFile : cases) {
                        sender.sendMessage(ColorUtil.translate("  &e#" + caseFile.caseId() + " &7- " + 
                            dateFormat.format(new Date(caseFile.createdAt())) + 
                            " by &f" + caseFile.creatorName()));
                    }
                }
                
                sender.sendMessage(ColorUtil.translate("&7Total: &e" + cases.size() + " &7case(s)"));
                sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
            });
        });
    }

    private void handleEvidence(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.translate("&cUsage: /case evidence <case_id> [type]"));
            return;
        }

        String caseId = args[1].toUpperCase();
        String filterType = args.length >= 3 ? args[2].toUpperCase() : null;
        
        plugin.getCaseFileManager().getCaseEvidence(caseId).thenAccept(evidence -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (evidence.isEmpty()) {
                    sender.sendMessage(ColorUtil.translate("&cNo evidence found for case: " + caseId));
                    return;
                }
                
                sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
                sender.sendMessage(ColorUtil.translate("&6🔍 Evidence for Case #" + caseId));
                sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
                
                for (Map.Entry<String, String> entry : evidence.entrySet()) {
                    if (filterType != null && !entry.getKey().equals(filterType)) continue;
                    
                    sender.sendMessage(ColorUtil.translate("&e▸ " + entry.getKey() + ":"));
                    
                    // Truncate long content
                    String content = entry.getValue();
                    String[] lines = content.split("\n");
                    int shown = 0;
                    for (String line : lines) {
                        if (shown++ >= 10) {
                            sender.sendMessage(ColorUtil.translate("  &8... (" + (lines.length - 10) + " more lines)"));
                            break;
                        }
                        sender.sendMessage(ColorUtil.translate("  &7" + line));
                    }
                    sender.sendMessage("");
                }
                
                sender.sendMessage(ColorUtil.translate("&7Types: " + String.join(", ", evidence.keySet())));
                sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
            });
        });
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtil.translate("&cThis command can only be used by players."));
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.translate("&cUsage: /case create <player>"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ColorUtil.translate("&cPlayer must be online to create a case file."));
            return;
        }
        
        sender.sendMessage(ColorUtil.translate("&7Creating case file for &e" + target.getName() + "&7..."));
        
        plugin.getCaseFileManager().createCaseFile(target, player).thenAccept(caseFile -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(ColorUtil.translate("&a✓ Case file created! ID: &e#" + caseFile.caseId()));
                sender.sendMessage(ColorUtil.translate("&7View with: &e/case view " + caseFile.caseId()));
            });
        });
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        sender.sendMessage(ColorUtil.translate("&6📁 Case File Commands"));
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        sender.sendMessage(ColorUtil.translate("  &e/case view <id> &7- View a case file"));
        sender.sendMessage(ColorUtil.translate("  &e/case list <player> &7- List player's cases"));
        sender.sendMessage(ColorUtil.translate("  &e/case evidence <id> [type] &7- View evidence"));
        sender.sendMessage(ColorUtil.translate("  &e/case create <player> &7- Create case manually"));
        sender.sendMessage(ColorUtil.translate(""));
        sender.sendMessage(ColorUtil.translate("  &8Note: Case files are auto-created on /freeze"));
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(List.of("view", "list", "evidence", "create"));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("list") || args[0].equalsIgnoreCase("create")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    completions.add(player.getName());
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("evidence")) {
            completions.addAll(List.of("CHAT_HISTORY", "COMMAND_HISTORY", "MOVEMENT_HISTORY", 
                                        "CONNECTION_INFO", "PLAYER_RELATIONSHIPS"));
        }

        String input = args[args.length - 1].toLowerCase();
        completions.removeIf(s -> !s.toLowerCase().startsWith(input));
        return completions;
    }
}
