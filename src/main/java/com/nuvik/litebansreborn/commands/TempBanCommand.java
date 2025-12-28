package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

// Stub commands - they all follow the same pattern as BanCommand
// These are placeholder implementations

public class TempBanCommand implements CommandExecutor {
    private final LiteBansReborn plugin;
    public TempBanCommand(LiteBansReborn plugin) { this.plugin = plugin; }
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // Redirect to ban command with duration
        return plugin.getCommand("ban").getExecutor().onCommand(sender, cmd, label, args);
    }
}
