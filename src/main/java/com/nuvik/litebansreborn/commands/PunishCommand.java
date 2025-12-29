package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.gui.PunishGUI;
import com.nuvik.litebansreborn.utils.ColorUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PunishCommand implements CommandExecutor, TabCompleter {
    
    private final LiteBansReborn plugin;
    
    public PunishCommand(LiteBansReborn plugin) { 
        this.plugin = plugin; 
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ColorUtil.translate("&cThis command can only be used by players."));
            return true;
        }
        
        if (!sender.hasPermission("litebansreborn.punish")) {
            plugin.getMessagesManager().send(sender, "general.no-permission");
            return true;
        }
        
        if (args.length < 1) {
            sender.sendMessage(ColorUtil.translate("&cUsage: /punish <player>"));
            return true;
        }
        
        String targetName = args[0];
        Player player = (Player) sender;
        
        new PunishGUI(plugin, player, targetName).open();
        
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String partialName = args[0].toLowerCase();
            return plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partialName))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
