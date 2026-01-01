package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.utils.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.jetbrains.annotations.NotNull;
import java.util.*;

public class HandleReportCommand implements CommandExecutor, TabCompleter {
    private final LiteBansReborn plugin;
    public HandleReportCommand(@NotNull LiteBansReborn plugin) { this.plugin = plugin; }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("litebansreborn.handlereport")) {
            plugin.getMessagesManager().send(sender, "general.no-permission");
            return true;
        }
        if (args.length < 2) {
            plugin.getMessagesManager().send(sender, "handlereport.usage");
            return true;
        }
        
        int id = Integer.parseInt(args[0]);
        String action = args[1].toLowerCase();
        String response = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "";
        
        plugin.getReportManager().handleReport(id, action, PlayerUtil.getExecutorUUID(sender), sender.getName(), response)
            .thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getMessagesManager().send(sender, "handlereport.success", "id", String.valueOf(id), "action", action);
            }));
        return true;
    }
    
    @Override
    public List<String> onTabComplete(@NotNull CommandSender s, @NotNull Command c, @NotNull String a, @NotNull String[] args) {
        return args.length == 2 ? List.of("accept", "deny", "ignore") : Collections.emptyList();
    }
}
