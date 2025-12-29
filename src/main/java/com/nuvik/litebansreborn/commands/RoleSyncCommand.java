package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Role Sync Command - Manage Discord ↔ LuckPerms role synchronization
 */
public class RoleSyncCommand implements CommandExecutor, TabCompleter {

    private final LiteBansReborn plugin;

    public RoleSyncCommand(LiteBansReborn plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("litebansreborn.rolesync")) {
            plugin.getMessagesManager().send(sender, "general.no-permission");
            return true;
        }

        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "sync" -> handleSync(sender, args);
            case "add" -> handleAddMapping(sender, args);
            case "remove" -> handleRemoveMapping(sender, args);
            case "list" -> handleListMappings(sender);
            case "status" -> handleStatus(sender);
            default -> showHelp(sender);
        }

        return true;
    }

    private void handleSync(CommandSender sender, String[] args) {
        if (plugin.getRoleSyncManager() == null || !plugin.getRoleSyncManager().isEnabled()) {
            sender.sendMessage(ColorUtil.translate("&cRole sync is not enabled."));
            return;
        }

        if (args.length < 2) {
            // Sync all online verified players
            sender.sendMessage(ColorUtil.translate("&7Syncing all verified players..."));
            
            int count = 0;
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (plugin.getVerificationManager().isVerified(player.getUniqueId())) {
                    plugin.getRoleSyncManager().fullSync(player.getUniqueId());
                    count++;
                }
            }
            
            sender.sendMessage(ColorUtil.translate("&aSynced &e" + count + " &aplayers."));
        } else {
            // Sync specific player
            @SuppressWarnings("deprecation")
            var target = Bukkit.getOfflinePlayer(args[1]);
            
            if (!plugin.getVerificationManager().isVerified(target.getUniqueId())) {
                sender.sendMessage(ColorUtil.translate("&cPlayer is not verified."));
                return;
            }

            plugin.getRoleSyncManager().fullSync(target.getUniqueId()).thenAccept(success -> {
                if (success) {
                    sender.sendMessage(ColorUtil.translate("&aSynced roles for &e" + args[1]));
                } else {
                    sender.sendMessage(ColorUtil.translate("&cFailed to sync roles."));
                }
            });
        }
    }

    private void handleAddMapping(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ColorUtil.translate("&cUsage: /rolesync add <discord_role_id> <luckperms_group>"));
            return;
        }

        String discordRoleId = args[1];
        String lpGroup = args[2];

        plugin.getRoleSyncManager().addMapping(discordRoleId, lpGroup);
        sender.sendMessage(ColorUtil.translate("&aAdded mapping: Discord &e" + discordRoleId + " &a→ LP &e" + lpGroup));
    }

    private void handleRemoveMapping(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.translate("&cUsage: /rolesync remove <discord_role_id>"));
            return;
        }

        plugin.getRoleSyncManager().removeMapping(args[1]);
        sender.sendMessage(ColorUtil.translate("&cRemoved mapping for Discord role &e" + args[1]));
    }

    private void handleListMappings(CommandSender sender) {
        var mappings = plugin.getRoleSyncManager().getDiscordToMinecraft();

        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        sender.sendMessage(ColorUtil.translate("&6Role Mappings &7(" + mappings.size() + ")"));
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));

        if (mappings.isEmpty()) {
            sender.sendMessage(ColorUtil.translate("&7No mappings configured."));
        } else {
            for (var entry : mappings.entrySet()) {
                sender.sendMessage(ColorUtil.translate("  &bDiscord &f" + entry.getKey() + " &7→ &aLP &f" + entry.getValue()));
            }
        }
    }

    private void handleStatus(CommandSender sender) {
        var rsm = plugin.getRoleSyncManager();

        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        sender.sendMessage(ColorUtil.translate("&6🔗 Role Sync Status"));
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        sender.sendMessage(ColorUtil.translate("  &7Enabled: " + (rsm.isEnabled() ? "&aYes" : "&cNo")));
        sender.sendMessage(ColorUtil.translate("  &7Direction: &f" + 
            plugin.getConfigManager().getString("role-sync.direction", "discord-to-minecraft")));
        sender.sendMessage(ColorUtil.translate("  &7Mappings: &e" + rsm.getDiscordToMinecraft().size()));
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        sender.sendMessage(ColorUtil.translate("&6🔗 Role Sync Commands"));
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        sender.sendMessage(ColorUtil.translate("  &e/rolesync sync [player] &7- Sync roles"));
        sender.sendMessage(ColorUtil.translate("  &e/rolesync add <role_id> <group> &7- Add mapping"));
        sender.sendMessage(ColorUtil.translate("  &e/rolesync remove <role_id> &7- Remove mapping"));
        sender.sendMessage(ColorUtil.translate("  &e/rolesync list &7- List mappings"));
        sender.sendMessage(ColorUtil.translate("  &e/rolesync status &7- Show status"));
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(List.of("sync", "add", "remove", "list", "status"));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("sync")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    completions.add(player.getName());
                }
            }
        }

        String input = args[args.length - 1].toLowerCase();
        completions.removeIf(s -> !s.toLowerCase().startsWith(input));
        return completions;
    }
}
