package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.utils.ColorUtil;
import com.nuvik.litebansreborn.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Maintenance Command - Control server maintenance mode
 */
public class MaintenanceCommand implements CommandExecutor, TabCompleter {

    private final LiteBansReborn plugin;

    public MaintenanceCommand(LiteBansReborn plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("litebansreborn.maintenance")) {
            plugin.getMessagesManager().send(sender, "general.no-permission");
            return true;
        }

        if (args.length == 0) {
            showStatus(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "on", "enable" -> handleEnable(sender, args);
            case "off", "disable" -> handleDisable(sender);
            case "add" -> handleAddWhitelist(sender, args);
            case "remove" -> handleRemoveWhitelist(sender, args);
            case "list" -> handleListWhitelist(sender);
            case "status" -> showStatus(sender);
            default -> showHelp(sender);
        }

        return true;
    }

    private void handleEnable(CommandSender sender, String[] args) {
        String reason = "Server maintenance in progress";
        Long duration = null;

        if (args.length >= 2) {
            // Check if first arg is duration
            if (args[1].matches("\\d+[smhdw]")) {
                duration = TimeUtil.parseDuration(args[1]);
                if (args.length >= 3) {
                    reason = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
                }
            } else {
                reason = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
            }
        }

        plugin.getMaintenanceManager().enable(reason, duration);
        
        sender.sendMessage(ColorUtil.translate(plugin.getMessagesManager().getPrefix() + 
            "&c⚠ Maintenance mode ENABLED!"));
        sender.sendMessage(ColorUtil.translate("  &7Reason: &f" + reason));
        if (duration != null) {
            sender.sendMessage(ColorUtil.translate("  &7Duration: &e" + TimeUtil.formatDuration(duration)));
        }
    }

    private void handleDisable(CommandSender sender) {
        plugin.getMaintenanceManager().disable();
        sender.sendMessage(ColorUtil.translate(plugin.getMessagesManager().getPrefix() + 
            "&a✓ Maintenance mode DISABLED!"));
    }

    private void handleAddWhitelist(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.translate("&cUsage: /maintenance add <player>"));
            return;
        }

        @SuppressWarnings("deprecation")
        var target = Bukkit.getOfflinePlayer(args[1]);
        plugin.getMaintenanceManager().addToWhitelist(target.getUniqueId());
        
        sender.sendMessage(ColorUtil.translate(plugin.getMessagesManager().getPrefix() + 
            "&aAdded &e" + args[1] + " &ato maintenance whitelist."));
    }

    private void handleRemoveWhitelist(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.translate("&cUsage: /maintenance remove <player>"));
            return;
        }

        @SuppressWarnings("deprecation")
        var target = Bukkit.getOfflinePlayer(args[1]);
        plugin.getMaintenanceManager().removeFromWhitelist(target.getUniqueId());
        
        sender.sendMessage(ColorUtil.translate(plugin.getMessagesManager().getPrefix() + 
            "&cRemoved &e" + args[1] + " &cfrom maintenance whitelist."));
    }

    private void handleListWhitelist(CommandSender sender) {
        var whitelist = plugin.getMaintenanceManager().getWhitelist();
        
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        sender.sendMessage(ColorUtil.translate("&6Maintenance Whitelist &7(" + whitelist.size() + ")"));
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        
        if (whitelist.isEmpty()) {
            sender.sendMessage(ColorUtil.translate("&7No players whitelisted."));
        } else {
            for (UUID uuid : whitelist) {
                var player = Bukkit.getOfflinePlayer(uuid);
                sender.sendMessage(ColorUtil.translate("  &7- &f" + player.getName()));
            }
        }
    }

    private void showStatus(CommandSender sender) {
        var mm = plugin.getMaintenanceManager();
        
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        sender.sendMessage(ColorUtil.translate("&6⚠ Maintenance Status"));
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        
        if (mm.isEnabled()) {
            sender.sendMessage(ColorUtil.translate("  &7Status: &c&lENABLED"));
            sender.sendMessage(ColorUtil.translate("  &7Reason: &f" + mm.getReason()));
            
            if (mm.getScheduledEnd() != null) {
                long remaining = mm.getScheduledEnd() - System.currentTimeMillis();
                sender.sendMessage(ColorUtil.translate("  &7Time left: &e" + TimeUtil.formatDuration(remaining)));
            }
            
            sender.sendMessage(ColorUtil.translate("  &7Whitelisted: &f" + mm.getWhitelist().size() + " players"));
        } else {
            sender.sendMessage(ColorUtil.translate("  &7Status: &a&lDISABLED"));
        }
        
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        sender.sendMessage(ColorUtil.translate("&6⚠ Maintenance Commands"));
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        sender.sendMessage(ColorUtil.translate("  &e/maintenance on [duration] [reason] &7- Enable"));
        sender.sendMessage(ColorUtil.translate("  &e/maintenance off &7- Disable"));
        sender.sendMessage(ColorUtil.translate("  &e/maintenance add <player> &7- Add to whitelist"));
        sender.sendMessage(ColorUtil.translate("  &e/maintenance remove <player> &7- Remove from whitelist"));
        sender.sendMessage(ColorUtil.translate("  &e/maintenance list &7- List whitelisted players"));
        sender.sendMessage(ColorUtil.translate("  &e/maintenance status &7- Show status"));
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(List.of("on", "off", "add", "remove", "list", "status"));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    completions.add(player.getName());
                }
            } else if (args[0].equalsIgnoreCase("on")) {
                completions.addAll(List.of("1h", "30m", "2h", "1d"));
            }
        }

        String input = args[args.length - 1].toLowerCase();
        completions.removeIf(s -> !s.toLowerCase().startsWith(input));
        return completions;
    }
}
