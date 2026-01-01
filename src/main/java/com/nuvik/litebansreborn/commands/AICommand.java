package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.managers.AIManager;
import com.nuvik.litebansreborn.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * AI Command - Manual AI analysis for staff
 * 
 * Commands:
 * /ai analyze <player> - Analyze player behavior
 * /ai toxicity <message> - Check message toxicity
 * /ai appeal <player> <reason> <appeal_text> - Review an appeal
 * /ai status - Check AI status
 */
public class AICommand implements CommandExecutor, TabCompleter {

    private final LiteBansReborn plugin;

    public AICommand(LiteBansReborn plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("litebansreborn.ai")) {
            plugin.getMessagesManager().send(sender, "general.no-permission");
            return true;
        }

        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        AIManager ai = plugin.getAIManager();
        if (ai == null || !ai.isEnabled()) {
            sender.sendMessage(ColorUtil.translate("&cAI is not enabled. Configure it in config.yml"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "status" -> handleStatus(sender);
            case "toxicity" -> handleToxicity(sender, args);
            case "analyze" -> handleAnalyze(sender, args);
            case "appeal" -> handleAppeal(sender, args);
            default -> showHelp(sender);
        }

        return true;
    }

    private void handleStatus(CommandSender sender) {
        AIManager ai = plugin.getAIManager();
        
        List<String> statusMsg = plugin.getMessagesManager().getList("ai.status.info", 
             com.nuvik.litebansreborn.config.MessagesManager.placeholders(
                 "enabled", ai.isEnabled() ? "&aEnabled" : "&cDisabled",
                 "provider", ai.getProvider(),
                 "model", ai.getModel()
             ));
        
        if (statusMsg == null || statusMsg.isEmpty()) {
             // Fallback if message key missing
             sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
             sender.sendMessage(ColorUtil.translate("&6🤖 AI Status"));
             sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
             sender.sendMessage(ColorUtil.translate("  &7Status: " + (ai.isEnabled() ? "&aEnabled" : "&cDisabled")));
             sender.sendMessage(ColorUtil.translate("  &7Provider: &f" + ai.getProvider()));
             sender.sendMessage(ColorUtil.translate("  &7Model: &f" + ai.getModel()));
             sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        } else {
             for (String line : statusMsg) {
                 sender.sendMessage(line);
             }
        }
    }

    private void handleToxicity(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getMessagesManager().get("ai.usage.toxicity", "prefix", plugin.getMessagesManager().getPrefix()));
            return;
        }

        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        String playerName = sender instanceof Player ? sender.getName() : "Console";
        
        sender.sendMessage(plugin.getMessagesManager().get("ai.analyzing"));
        
        plugin.getAIManager().analyzeToxicity(message, playerName).thenAccept(result -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                List<String> msg = plugin.getMessagesManager().getList("ai.toxicity-result",
                    com.nuvik.litebansreborn.config.MessagesManager.placeholders(
                        "message", message,
                        "is_toxic", result.toxic() ? "&cYES" : "&aNO",
                        "score", getScoreColor(result.score()) + result.score() + "%",
                        "reason", result.reason()
                    )
                );
                for (String line : msg) sender.sendMessage(line);
            });
        });
    }

    private void handleAnalyze(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getMessagesManager().get("ai.usage.analyze", "prefix", plugin.getMessagesManager().getPrefix()));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getMessagesManager().get("general.player-not-found"));
            return;
        }
        
        sender.sendMessage(plugin.getMessagesManager().get("ai.analyzing"));
        
        // Get recent chat from case file manager
        List<String> recentActions = new ArrayList<>();
        recentActions.add("Player online for current session");
        recentActions.add("Location: " + target.getWorld().getName() + " " + 
                          (int)target.getLocation().getX() + ", " + 
                          (int)target.getLocation().getY() + ", " + 
                          (int)target.getLocation().getZ());
        recentActions.add("Game mode: " + target.getGameMode());
        recentActions.add("Health: " + (int)target.getHealth() + "/" + (int)target.getMaxHealth());
        
        // Add punishment history
        if (plugin.getPredictiveManager() != null) {
            var profile = plugin.getPredictiveManager().getRiskProfile(target.getUniqueId());
            if (profile != null) {
                recentActions.add("Warnings: " + profile.totalWarnings());
                recentActions.add("Mutes: " + profile.totalMutes());
                recentActions.add("Bans: " + profile.totalBans());
            }
        }
        
        plugin.getAIManager().analyzeBehavior(target.getName(), recentActions).thenAccept(result -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                List<String> msg = plugin.getMessagesManager().getList("ai.behavior-result",
                    com.nuvik.litebansreborn.config.MessagesManager.placeholders(
                        "player", target.getName(),
                        "risk_level", getRiskColor(result.riskLevel()) + result.riskLevel().toUpperCase(),
                        "score", getScoreColor(result.riskScore()) + result.riskScore() + "%",
                        "summary", result.summary()
                    )
                );
                for (String line : msg) sender.sendMessage(line);
            });
        });
    }

    private void handleAppeal(CommandSender sender, String[] args) {
        if (args.length < 4) {
             sender.sendMessage(plugin.getMessagesManager().get("ai.usage.appeal", "prefix", plugin.getMessagesManager().getPrefix()));
             sender.sendMessage(ColorUtil.translate("&7Example: /ai appeal Steve \"Cheating\" \"I wasn't cheating, I use OptiFine\""));
             return;
        }

        String playerName = args[1];
        String reason = args[2];
        String appealText = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
        
        sender.sendMessage(plugin.getMessagesManager().get("ai.analyzing"));
        
        plugin.getAIManager().reviewAppeal(playerName, reason, appealText).thenAccept(result -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                String recColor = switch (result.recommendation().toLowerCase()) {
                    case "accept" -> "&a";
                    case "deny" -> "&c";
                    default -> "&e";
                };
                
                List<String> msg = plugin.getMessagesManager().getList("ai.appeal-result",
                    com.nuvik.litebansreborn.config.MessagesManager.placeholders(
                        "player", playerName,
                        "reason", reason,
                        "appeal_text", appealText,
                        "recommendation", recColor + result.recommendation().toUpperCase(),
                        "confidence", String.valueOf(result.confidence()),
                        "reasoning", result.reasoning()
                    )
                );
                for (String line : msg) sender.sendMessage(line);
            });
        });
    }

    private String getScoreColor(int score) {
        if (score >= 70) return "&c";
        if (score >= 40) return "&e";
        return "&a";
    }

    private String getRiskColor(String level) {
        return switch (level.toLowerCase()) {
            case "critical" -> "&4";
            case "high" -> "&c";
            case "medium" -> "&e";
            case "low" -> "&a";
            default -> "&7";
        };
    }

    private void showHelp(CommandSender sender) {
        List<String> help = plugin.getMessagesManager().getList("ai.help");
        if (help == null || help.isEmpty()) {
             sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
             sender.sendMessage(ColorUtil.translate("&6🤖 AI Commands"));
             sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
             sender.sendMessage(ColorUtil.translate("  &e/ai status &7- Check AI status"));
             sender.sendMessage(ColorUtil.translate("  &e/ai toxicity <message> &7- Analyze toxicity"));
             sender.sendMessage(ColorUtil.translate("  &e/ai analyze <player> &7- Analyze player"));
             sender.sendMessage(ColorUtil.translate("  &e/ai appeal <player> <reason> <text> &7- Review appeal"));
             sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        } else {
             for (String line : help) sender.sendMessage(line);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(List.of("status", "toxicity", "analyze", "appeal"));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("analyze") || args[0].equalsIgnoreCase("appeal")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    completions.add(player.getName());
                }
            }
        }

        String input = args[args.length - 1].toLowerCase();
        completions.removeIf(s -> !s.toLowerCase().startsWith(input));
        return completions;
    }
}
