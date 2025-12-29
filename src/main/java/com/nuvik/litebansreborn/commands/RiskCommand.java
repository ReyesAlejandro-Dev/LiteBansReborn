package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.managers.PredictiveManager;
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

/**
 * Risk Command - View player risk scores and predictions
 */
public class RiskCommand implements CommandExecutor, TabCompleter {

    private final LiteBansReborn plugin;

    public RiskCommand(LiteBansReborn plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("litebansreborn.risk")) {
            plugin.getMessagesManager().send(sender, "general.no-permission");
            return true;
        }

        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "check" -> handleCheck(sender, args);
            case "analyze" -> handleAnalyze(sender, args);
            case "top" -> handleTop(sender);
            default -> showHelp(sender);
        }

        return true;
    }

    private void handleCheck(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.translate("&cUsage: /risk check <player>"));
            return;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        
        PredictiveManager.PlayerRiskProfile profile = plugin.getPredictiveManager().getRiskProfile(target.getUniqueId());
        
        if (profile == null) {
            sender.sendMessage(ColorUtil.translate("&cNo risk profile found for " + target.getName()));
            sender.sendMessage(ColorUtil.translate("&7Use &e/risk analyze " + target.getName() + " &7to create one."));
            return;
        }
        
        String riskLevel = plugin.getPredictiveManager().getRiskLevel(profile.riskScore());
        String predictionColor = profile.banPrediction() >= 70 ? "&c" : profile.banPrediction() >= 40 ? "&e" : "&a";
        
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        sender.sendMessage(ColorUtil.translate("&6🔮 Risk Profile: &f" + target.getName()));
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        sender.sendMessage(ColorUtil.translate("  &7Risk Score: " + riskLevel + " &7(" + profile.riskScore() + "%)"));
        sender.sendMessage(ColorUtil.translate("  &7Ban Prediction: " + predictionColor + profile.banPrediction() + "% &7chance in 7 days"));
        sender.sendMessage(ColorUtil.translate(""));
        sender.sendMessage(ColorUtil.translate("  &7📊 Statistics:"));
        sender.sendMessage(ColorUtil.translate("    &7Total Messages: &f" + profile.totalMessages()));
        sender.sendMessage(ColorUtil.translate("    &7Toxic Messages: &c" + profile.toxicMessages()));
        sender.sendMessage(ColorUtil.translate("    &7Warnings: &e" + profile.totalWarnings()));
        sender.sendMessage(ColorUtil.translate("    &7Mutes: &6" + profile.totalMutes()));
        sender.sendMessage(ColorUtil.translate("    &7Previous Bans: &c" + profile.totalBans()));
        sender.sendMessage(ColorUtil.translate("    &7Playtime: &a" + String.format("%.1f", profile.playtimeHours()) + "h"));
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
    }

    private void handleAnalyze(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.translate("&cUsage: /risk analyze <player>"));
            return;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        
        sender.sendMessage(ColorUtil.translate("&7Analyzing &e" + target.getName() + "&7..."));
        
        plugin.getPredictiveManager().analyzePlayer(target.getUniqueId()).thenAccept(profile -> {
            String riskLevel = plugin.getPredictiveManager().getRiskLevel(profile.riskScore());
            
            sender.sendMessage(ColorUtil.translate("&a✓ Analysis complete!"));
            sender.sendMessage(ColorUtil.translate("  &7Risk: " + riskLevel + " &7(" + profile.riskScore() + "%)"));
            sender.sendMessage(ColorUtil.translate("  &7Ban Prediction: &e" + profile.banPrediction() + "%"));
            
            if (profile.riskScore() >= 70) {
                sender.sendMessage(ColorUtil.translate("  &c⚠ HIGH RISK - Recommend increased monitoring!"));
            }
        });
    }

    private void handleTop(CommandSender sender) {
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        sender.sendMessage(ColorUtil.translate("&6🔮 Highest Risk Online Players"));
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        
        List<PlayerRiskEntry> entries = new ArrayList<>();
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            PredictiveManager.PlayerRiskProfile profile = 
                plugin.getPredictiveManager().getRiskProfile(player.getUniqueId());
            if (profile != null) {
                entries.add(new PlayerRiskEntry(player.getName(), profile.riskScore(), profile.banPrediction()));
            }
        }
        
        entries.sort((a, b) -> b.risk() - a.risk());
        
        int shown = 0;
        for (PlayerRiskEntry entry : entries) {
            if (shown++ >= 10) break;
            String riskLevel = plugin.getPredictiveManager().getRiskLevel(entry.risk());
            sender.sendMessage(ColorUtil.translate("  " + riskLevel + " &f" + entry.name() + 
                " &8(Risk: " + entry.risk() + "%, Pred: " + entry.prediction() + "%)"));
        }
        
        if (entries.isEmpty()) {
            sender.sendMessage(ColorUtil.translate("&7No players with risk profiles online."));
        }
        
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        sender.sendMessage(ColorUtil.translate("&6🔮 Predictive Moderation Commands"));
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        sender.sendMessage(ColorUtil.translate("  &e/risk check <player> &7- View risk profile"));
        sender.sendMessage(ColorUtil.translate("  &e/risk analyze <player> &7- Force reanalysis"));
        sender.sendMessage(ColorUtil.translate("  &e/risk top &7- Show highest risk players online"));
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(List.of("check", "analyze", "top"));
        } else if (args.length == 2) {
            if (!args[0].equalsIgnoreCase("top")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    completions.add(player.getName());
                }
            }
        }

        String input = args[args.length - 1].toLowerCase();
        completions.removeIf(s -> !s.toLowerCase().startsWith(input));
        return completions;
    }
    
    private record PlayerRiskEntry(String name, int risk, int prediction) {}
}
