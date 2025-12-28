package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

// Stub commands for remaining functionality

public class UnwarnCommand implements CommandExecutor {
    private final LiteBansReborn plugin;
    public UnwarnCommand(LiteBansReborn plugin) { this.plugin = plugin; }
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        sender.sendMessage("§eUnwarn command - Coming soon!");
        return true;
    }
}
