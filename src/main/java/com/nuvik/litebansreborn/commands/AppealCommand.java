package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class AppealCommand implements CommandExecutor {
    private final LiteBansReborn plugin;
    public AppealCommand(@NotNull LiteBansReborn plugin) { this.plugin = plugin; }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessagesManager().send(sender, "general.player-only");
            return true;
        }
        if (args.length < 1) {
            plugin.getMessagesManager().send(sender, "appeal.usage");
            return true;
        }
        String message = String.join(" ", args);
        
        // First check if player has active punishment
        plugin.getBanManager().getActiveBan(player.getUniqueId()).thenAccept(ban -> {
            if (ban != null) {
                plugin.getAppealManager().createAppeal(ban.getId(), ban.getType(), player.getUniqueId(), player.getName(), message)
                    .thenAccept(success -> Bukkit.getScheduler().runTask(plugin, () -> {
                        plugin.getMessagesManager().send(sender, "appeal.success");
                    }));
            } else {
                plugin.getMuteManager().getActiveMute(player.getUniqueId()).thenAccept(mute -> {
                    if (mute != null) {
                        plugin.getAppealManager().createAppeal(mute.getId(), mute.getType(), player.getUniqueId(), player.getName(), message)
                            .thenAccept(success -> Bukkit.getScheduler().runTask(plugin, () -> {
                                plugin.getMessagesManager().send(sender, "appeal.success");
                            }));
                    } else {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            plugin.getMessagesManager().send(sender, "appeal.no-punishment");
                        });
                    }
                });
            }
        });
        return true;
    }
}
