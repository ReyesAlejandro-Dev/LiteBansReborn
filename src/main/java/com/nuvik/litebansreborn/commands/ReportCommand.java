package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ReportCommand implements CommandExecutor, TabCompleter {
    
    private final LiteBansReborn plugin;
    
    public ReportCommand(LiteBansReborn plugin) { 
        this.plugin = plugin; 
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ColorUtil.translate("&cThis command can only be used by players."));
            return true;
        }
        
        Player reporter = (Player) sender;
        
        if (!reporter.hasPermission("litebansreborn.report")) {
            plugin.getMessagesManager().send(reporter, "general.no-permission");
            return true;
        }
        
        if (args.length < 2) {
            plugin.getMessagesManager().send(reporter, "reports.usage");
            return true;
        }
        
        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);
        
        if (target == null) {
            plugin.getMessagesManager().send(reporter, "general.player-not-found", "player", targetName);
            return true;
        }
        
        if (target.getUniqueId().equals(reporter.getUniqueId())) {
            plugin.getMessagesManager().send(reporter, "reports.cannot-report-self");
            return true;
        }
        
        String reason = Arrays.stream(args).skip(1).collect(Collectors.joining(" "));
        
        // Create Report
        if (plugin.getReportManager() != null) {
            plugin.getReportManager().createReport(reporter, target, reason).thenAccept(v -> {
                plugin.getMessagesManager().send(reporter, "reports.success");
                plugin.getMessagesManager().send(reporter, "reports.confirm", "player", target.getName());
                
                // Notify Staff
                notifyStaff(reporter.getName(), target.getName(), reason);
            }).exceptionally(ex -> {
                reporter.sendMessage(ColorUtil.translate("&cError submitting report: " + ex.getMessage()));
                return null;
            });
        } else {
            reporter.sendMessage(ColorUtil.translate("&cReport system is currently unavailable."));
        }
        
        return true;
    }
    
    private void notifyStaff(String reporter, String reported, String reason) {
        String message = plugin.getMessagesManager().get("reports.notify", 
            "reporter", reporter,
            "reported", reported,
            "reason", reason
        );
        
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("litebansreborn.reports.view") || p.hasPermission("litebansreborn.notify")) {
                p.sendMessage(message);
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String partialName = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partialName))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            // Suggest some common reasons
            return Arrays.asList("Cheating", "Hacking", "Toxic", "Spam", "Advertising", "Griefing", "Teaming")
                    .stream()
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
