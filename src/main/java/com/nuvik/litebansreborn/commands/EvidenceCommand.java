package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.managers.EvidenceManager;
import com.nuvik.litebansreborn.utils.ColorUtil;
import com.nuvik.litebansreborn.utils.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Evidence Command - Manage evidence for punishments
 * /evidence add <punishment_id> <url> - Add evidence
 * /evidence view <punishment_id> - View evidence
 * /evidence list <player> - List punishments with evidence
 */
public class EvidenceCommand implements CommandExecutor, TabCompleter {

    private final LiteBansReborn plugin;

    public EvidenceCommand(LiteBansReborn plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("litebansreborn.evidence")) {
            plugin.getMessagesManager().send(sender, "general.no-permission");
            return true;
        }

        if (args.length < 1) {
            sendHelp(sender);
            return true;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "add" -> handleAdd(sender, args);
            case "view" -> handleView(sender, args);
            case "capture" -> handleCapture(sender, args);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void handleAdd(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ColorUtil.translate("&cUsage: /evidence add <punishment_id> <url>"));
            return;
        }

        try {
            long punishmentId = Long.parseLong(args[1]);
            String url = args[2];

            EvidenceManager.Evidence evidence = plugin.getEvidenceManager().parseFromCommand(
                url,
                PlayerUtil.getExecutorUUID(sender),
                PlayerUtil.getExecutorName(sender)
            );

            plugin.getEvidenceManager().addEvidence(punishmentId, evidence).thenAccept(success -> {
                if (success) {
                    sender.sendMessage(ColorUtil.translate("&aEvidence added successfully to punishment #" + punishmentId));
                } else {
                    sender.sendMessage(ColorUtil.translate("&cFailed to add evidence."));
                }
            });
        } catch (NumberFormatException e) {
            sender.sendMessage(ColorUtil.translate("&cInvalid punishment ID."));
        }
    }

    private void handleView(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.translate("&cUsage: /evidence view <punishment_id>"));
            return;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtil.translate("&cThis command can only be used by players."));
            return;
        }

        try {
            long punishmentId = Long.parseLong(args[1]);
            plugin.getEvidenceManager().openViewer(player, punishmentId, "Punishment #" + punishmentId);
        } catch (NumberFormatException e) {
            sender.sendMessage(ColorUtil.translate("&cInvalid punishment ID."));
        }
    }

    private void handleCapture(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ColorUtil.translate("&cUsage: /evidence capture <player> <punishment_id>"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ColorUtil.translate("&cPlayer not found or not online."));
            return;
        }

        try {
            long punishmentId = Long.parseLong(args[2]);
            
            EvidenceManager.Evidence evidence = plugin.getEvidenceManager().captureInventory(
                target,
                PlayerUtil.getExecutorUUID(sender),
                PlayerUtil.getExecutorName(sender)
            );

            plugin.getEvidenceManager().addEvidence(punishmentId, evidence).thenAccept(success -> {
                if (success) {
                    sender.sendMessage(ColorUtil.translate("&aInventory captured and added to punishment #" + punishmentId));
                } else {
                    sender.sendMessage(ColorUtil.translate("&cFailed to capture inventory."));
                }
            });
        } catch (NumberFormatException e) {
            sender.sendMessage(ColorUtil.translate("&cInvalid punishment ID."));
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        sender.sendMessage(ColorUtil.translate("&c&lEvidence System"));
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        sender.sendMessage(ColorUtil.translate("&c/evidence add <id> <url> &8- &7Add evidence URL"));
        sender.sendMessage(ColorUtil.translate("&c/evidence view <id> &8- &7View evidence GUI"));
        sender.sendMessage(ColorUtil.translate("&c/evidence capture <player> <id> &8- &7Capture inventory"));
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("add", "view", "capture").stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (args.length == 2 && args[0].equalsIgnoreCase("capture")) {
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        return new ArrayList<>();
    }
}
