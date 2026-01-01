package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.models.Appeal.AppealStatus;
import com.nuvik.litebansreborn.utils.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.jetbrains.annotations.NotNull;
import java.util.*;

public class HandleAppealCommand implements CommandExecutor, TabCompleter {
    private final LiteBansReborn plugin;
    public HandleAppealCommand(@NotNull LiteBansReborn plugin) { this.plugin = plugin; }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("litebansreborn.handleappeal")) {
            plugin.getMessagesManager().send(sender, "general.no-permission");
            return true;
        }
        if (args.length < 2) {
            plugin.getMessagesManager().send(sender, "handleappeal.usage");
            return true;
        }
        
        long id = Long.parseLong(args[0]);
        AppealStatus status = args[1].equalsIgnoreCase("accept") ? AppealStatus.ACCEPTED : AppealStatus.DENIED;
        String response = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : null;
        
        // handleAppeal(long id, UUID handledByUUID, String handledByName, AppealStatus status, String response)
        plugin.getAppealManager().handleAppeal(id, PlayerUtil.getExecutorUUID(sender), sender.getName(), status, response)
            .thenAccept(success -> Bukkit.getScheduler().runTask(plugin, () -> {
                if (success) {
                    if (status == AppealStatus.ACCEPTED) {
                        plugin.getMessagesManager().send(sender, "handleappeal.accepted", "id", String.valueOf(id));
                    } else {
                        plugin.getMessagesManager().send(sender, "handleappeal.denied", "id", String.valueOf(id));
                    }
                } else {
                    plugin.getMessagesManager().send(sender, "handleappeal.not-found");
                }
            }));
        return true;
    }
    
    @Override
    public List<String> onTabComplete(@NotNull CommandSender s, @NotNull Command c, @NotNull String a, @NotNull String[] args) {
        return args.length == 2 ? List.of("accept", "deny") : Collections.emptyList();
    }
}
