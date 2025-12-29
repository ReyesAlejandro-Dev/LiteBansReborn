package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class GhostMuteCommand implements CommandExecutor, TabCompleter {

    private final LiteBansReborn plugin;

    public GhostMuteCommand(LiteBansReborn plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("litebansreborn.ghostmute")) {
            sender.sendMessage(ColorUtil.translate("&cYou don't have permission to use this command."));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ColorUtil.translate("&cUsage: /ghostmute <player>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ColorUtil.translate("&cPlayer not found."));
            return true;
        }
        
        UUID targetUUID = target.getUniqueId();
        boolean isMuted = plugin.getGhostMuteManager().toggleGhostMute(targetUUID);
        
        if (isMuted) {
            sender.sendMessage(ColorUtil.translate("&a&lGhost Mute Enabled &7for &e" + target.getName()));
            sender.sendMessage(ColorUtil.translate("&7They will now talk to themselves. No one else will hear them."));
        } else {
            sender.sendMessage(ColorUtil.translate("&c&lGhost Mute Disabled &7for &e" + target.getName()));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
