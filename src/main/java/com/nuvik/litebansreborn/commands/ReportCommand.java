package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

// Stub commands for reports, appeals, alts, notes, etc.

public class ReportCommand implements CommandExecutor {
    private final LiteBansReborn plugin;
    public ReportCommand(LiteBansReborn plugin) { this.plugin = plugin; }
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        sender.sendMessage("§eReport command - Coming soon!");
        return true;
    }
}
