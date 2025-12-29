package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class TempMuteCommand implements CommandExecutor {
    private final LiteBansReborn plugin;
    public TempMuteCommand(LiteBansReborn plugin) { this.plugin = plugin; }
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        return plugin.getCommand("mute").getExecutor().onCommand(sender, cmd, label, args);
    }
}
