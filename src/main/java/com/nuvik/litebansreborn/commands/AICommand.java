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
        
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        sender.sendMessage(ColorUtil.translate("&6🤖 AI Status"));
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        sender.sendMessage(ColorUtil.translate("  &7Status: " + (ai.isEnabled() ? "&aEnabled" : "&cDisabled")));
        sender.sendMessage(ColorUtil.translate("  &7Provider: &f" + ai.getProvider()));
        sender.sendMessage(ColorUtil.translate("  &7Model: &f" + ai.getModel()));
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
    }

    private void handleToxicity(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.translate("&cUsage: /ai toxicity <message>"));
            return;
        }

        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        String playerName = sender instanceof Player ? sender.getName() : "Console";
        
        sender.sendMessage(ColorUtil.translate("&7Analyzing message for toxicity..."));
        
        plugin.getAIManager().analyzeToxicity(message, playerName).thenAccept(result -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
                sender.sendMessage(ColorUtil.translate("&6🔍 Toxicity Analysis"));
                sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
                sender.sendMessage(ColorUtil.translate("  &7Message: &f\"" + message + "\""));
                sender.sendMessage(ColorUtil.translate("  &7Toxic: " + (result.toxic() ? "&cYES" : "&aNO")));
                sender.sendMessage(ColorUtil.translate("  &7Score: " + getScoreColor(result.score()) + result.score() + "%"));
                sender.sendMessage(ColorUtil.translate("  &7Reason: &f" + result.reason()));
                sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
            });
        });
    }

    private void handleAnalyze(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.translate("&cUsage: /ai analyze <player>"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ColorUtil.translate("&cPlayer not found or not online."));
            return;
        }
        
        sender.sendMessage(ColorUtil.translate("&7Analyzing &e" + target.getName() + "&7's behavior..."));
        
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
                sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
                sender.sendMessage(ColorUtil.translate("&6🤖 AI Behavior Analysis: &f" + target.getName()));
                sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
                sender.sendMessage(ColorUtil.translate("  &7Risk Level: " + getRiskColor(result.riskLevel()) + result.riskLevel().toUpperCase()));
                sender.sendMessage(ColorUtil.translate("  &7Risk Score: " + getScoreColor(result.riskScore()) + result.riskScore() + "%"));
                sender.sendMessage(ColorUtil.translate("  &7Summary: &f" + result.summary()));
                sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
            });
        });
    }

    private void handleAppeal(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ColorUtil.translate("&cUsage: /ai appeal <player> <punishment_reason> <appeal_text>"));
            sender.sendMessage(ColorUtil.translate("&7Example: /ai appeal Steve \"Cheating\" \"I wasn't cheating, I use OptiFine\""));
            return;
        }

        String playerName = args[1];
        String reason = args[2];
        String appealText = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
        
        sender.sendMessage(ColorUtil.translate("&7Reviewing appeal with AI..."));
        
        plugin.getAIManager().reviewAppeal(playerName, reason, appealText).thenAccept(result -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                String recColor = switch (result.recommendation().toLowerCase()) {
                    case "accept" -> "&a";
                    case "deny" -> "&c";
                    default -> "&e";
                };
                
                sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
                sender.sendMessage(ColorUtil.translate("&6⚖️ AI Appeal Review"));
                sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
                sender.sendMessage(ColorUtil.translate("  &7Player: &f" + playerName));
                sender.sendMessage(ColorUtil.translate("  &7Punishment: &f" + reason));
                sender.sendMessage(ColorUtil.translate("  &7Appeal: &f\"" + appealText + "\""));
                sender.sendMessage(ColorUtil.translate(""));
                sender.sendMessage(ColorUtil.translate("  &7AI Recommendation: " + recColor + result.recommendation().toUpperCase()));
                sender.sendMessage(ColorUtil.translate("  &7Confidence: &f" + result.confidence() + "%"));
                sender.sendMessage(ColorUtil.translate("  &7Reasoning: &f" + result.reasoning()));
                sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
                sender.sendMessage(ColorUtil.translate("&8Note: This is AI-assisted. Final decision is yours."));
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
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        sender.sendMessage(ColorUtil.translate("&6🤖 AI Commands"));
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        sender.sendMessage(ColorUtil.translate("  &e/ai status &7- Check AI status"));
        sender.sendMessage(ColorUtil.translate("  &e/ai toxicity <message> &7- Analyze toxicity"));
        sender.sendMessage(ColorUtil.translate("  &e/ai analyze <player> &7- Analyze player"));
        sender.sendMessage(ColorUtil.translate("  &e/ai appeal <player> <reason> <text> &7- Review appeal"));
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
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
