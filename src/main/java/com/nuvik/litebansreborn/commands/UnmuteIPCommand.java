package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class UnmuteIPCommand implements CommandExecutor {
    private final LiteBansReborn plugin;
    public UnmuteIPCommand(LiteBansReborn plugin) { this.plugin = plugin; }
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        sender.sendMessage("§eUnmute IP command - Coming soon!");
        return true;
    }
}
