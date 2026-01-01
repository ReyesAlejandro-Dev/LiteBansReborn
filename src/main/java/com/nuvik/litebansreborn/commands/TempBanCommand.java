package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

// Stub commands - they all follow the same pattern as BanCommand
// These are placeholder implementations

public class TempBanCommand implements CommandExecutor, org.bukkit.command.TabCompleter {
    private final LiteBansReborn plugin;
    public TempBanCommand(LiteBansReborn plugin) { this.plugin = plugin; }
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // Redirect to ban command with duration
        return plugin.getCommand("ban").getExecutor().onCommand(sender, cmd, label, args);
    }
    
    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return com.nuvik.litebansreborn.utils.PlayerUtil.getOnlineAndOfflinePlayerNames(args[0]);
        } else if (args.length == 2) {
            java.util.List<String> completions = new java.util.ArrayList<>();
            completions.addAll(java.util.Arrays.asList("1h", "1d", "7d", "30d", "permanent", "-s"));
            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(java.util.stream.Collectors.toList());
        }
        return new java.util.ArrayList<>();
    }
}
