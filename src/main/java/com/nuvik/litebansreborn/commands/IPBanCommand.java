package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

// Stub commands - basic implementations

public class IPBanCommand implements CommandExecutor {
    private final LiteBansReborn plugin;
    public IPBanCommand(LiteBansReborn plugin) { this.plugin = plugin; }
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        sender.sendMessage("§eIP Ban command - Coming soon!");
        return true;
    }
}
