package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.managers.HWIDManager;
import com.nuvik.litebansreborn.utils.ColorUtil;
import com.nuvik.litebansreborn.utils.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * HWID Command - Hardware ID management
 * /hwid check <player> - Check player's HWID/fingerprint
 * /hwid ban <hwid> <reason> - Ban by HWID
 * /hwid alts <player> - Find linked accounts
 * /hwid status - HWID system status
 */
public class HWIDCommand implements CommandExecutor, TabCompleter {

    private final LiteBansReborn plugin;

    public HWIDCommand(LiteBansReborn plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("litebansreborn.hwid")) {
            plugin.getMessagesManager().send(sender, "general.no-permission");
            return true;
        }

        HWIDManager manager = plugin.getHWIDManager();
        if (manager == null || !manager.isEnabled()) {
            sender.sendMessage(ColorUtil.translate("&cHardware ID system is disabled."));
            sender.sendMessage(ColorUtil.translate("&7Enable it in config.yml under 'hardware-ban.enabled'"));
            return true;
        }

        if (args.length < 1) {
            sendHelp(sender);
            return true;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "check" -> handleCheck(sender, args);
            case "ban" -> handleBan(sender, args);
            case "unban" -> handleUnban(sender, args);
            case "alts" -> handleAlts(sender, args);
            case "fingerprint" -> handleFingerprint(sender, args);
            case "status" -> handleStatus(sender);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void handleCheck(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getMessagesManager().get("hwid.check-usage", "prefix", plugin.getMessagesManager().getPrefix()));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getMessagesManager().get("general.player-not-found"));
            return;
        }

        HWIDManager manager = plugin.getHWIDManager();
        HWIDManager.PlayerFingerprint fp = manager.getFingerprint(target.getUniqueId());

        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        sender.sendMessage(ColorUtil.translate("&c&lHWID Info: &f" + target.getName()));
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));

        if (fp != null) {
            sender.sendMessage(ColorUtil.translate("&7HWID: &f" + (fp.hwid != null ? fp.hwid.substring(0, Math.min(16, fp.hwid.length())) + "..." : "&cNot registered")));
            sender.sendMessage(ColorUtil.translate("&7Client: &f" + (fp.clientBrand != null ? fp.clientBrand : "Unknown")));
            sender.sendMessage(ColorUtil.translate("&7Locale: &f" + (fp.locale != null ? fp.locale : "Unknown")));
            sender.sendMessage(ColorUtil.translate("&7View Distance: &f" + fp.viewDistance));
            sender.sendMessage(ColorUtil.translate("&7Known IPs: &f" + fp.knownIPs.size()));
            sender.sendMessage(ColorUtil.translate("&7First Seen: &f" + new Date(fp.firstSeen)));
            sender.sendMessage(ColorUtil.translate("&7Last Seen: &f" + new Date(fp.lastSeen)));
        } else {
            sender.sendMessage(ColorUtil.translate("&7No fingerprint data available yet."));
        }

        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));

        // Check for HWID ban
        manager.checkBanned(target.getUniqueId()).thenAccept(ban -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (ban != null) {
                    sender.sendMessage(ColorUtil.translate("&c⚠ This HWID is BANNED!"));
                    sender.sendMessage(ColorUtil.translate("&7Reason: &f" + ban.reason));
                }
            });
        });
    }

    private void handleBan(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(plugin.getMessagesManager().get("hwid.ban-usage", "prefix", plugin.getMessagesManager().getPrefix()));
            return;
        }

        String hwid = args[1];
        String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        HWIDManager manager = plugin.getHWIDManager();
        manager.banHWID(hwid, reason, PlayerUtil.getExecutorUUID(sender), PlayerUtil.getExecutorName(sender))
            .thenAccept(success -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (success) {
                        sender.sendMessage(plugin.getMessagesManager().get("hwid.banned", "hwid", hwid.substring(0, Math.min(16, hwid.length())) + "...", "reason", reason));
                    } else {
                        sender.sendMessage(plugin.getMessagesManager().get("hwid.ban-failed"));
                    }
                });
            });
    }

    private void handleUnban(CommandSender sender, String[] args) {
        if (args.length < 2) {
             sender.sendMessage(plugin.getMessagesManager().get("hwid.unban-usage", "prefix", plugin.getMessagesManager().getPrefix()));
             return;
        }
        String hwid = args[1];
        plugin.getHWIDManager().unbanHWID(hwid).thenAccept(success -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (success) {
                    sender.sendMessage(plugin.getMessagesManager().get("hwid.unbanned", "hwid", hwid));
                } else {
                    sender.sendMessage(plugin.getMessagesManager().get("hwid.unban-failed"));
                }
            });
        });
    }

    private void handleAlts(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getMessagesManager().get("hwid.alts-usage", "prefix", plugin.getMessagesManager().getPrefix()));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        UUID targetUUID;
        String targetName;

        if (target != null) {
            targetUUID = target.getUniqueId();
            targetName = target.getName();
        } else {
            @SuppressWarnings("deprecation")
            org.bukkit.OfflinePlayer offline = Bukkit.getOfflinePlayer(args[1]);
            if (!offline.hasPlayedBefore()) {
                sender.sendMessage(plugin.getMessagesManager().get("general.player-not-found"));
                return;
            }
            targetUUID = offline.getUniqueId();
            targetName = args[1];
        }

        HWIDManager manager = plugin.getHWIDManager();

        sender.sendMessage(ColorUtil.translate("&7Searching for linked accounts..."));

        // Check by HWID
        manager.findLinkedAccounts(targetUUID).thenAccept(hwIdLinked -> {
            // Check by fingerprint similarity
            manager.findSimilarFingerprints(targetUUID, 60).thenAccept(similar -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
                    sender.sendMessage(ColorUtil.translate("&c&lAlt Detection: &f" + targetName));
                    sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));

                    if (!hwIdLinked.isEmpty()) {
                        sender.sendMessage(ColorUtil.translate("&7&lSame HWID:"));
                        for (UUID uuid : hwIdLinked) {
                            String name = Bukkit.getOfflinePlayer(uuid).getName();
                            sender.sendMessage(ColorUtil.translate("  &c⚠ &f" + (name != null ? name : uuid.toString())));
                        }
                        sender.sendMessage("");
                    }

                    if (!similar.isEmpty()) {
                        sender.sendMessage(ColorUtil.translate("&7&lSimilar Fingerprints:"));
                        for (Map.Entry<UUID, Integer> entry : similar) {
                            String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                            int score = entry.getValue();
                            String color = score >= 80 ? "&c" : (score >= 60 ? "&e" : "&a");
                            sender.sendMessage(ColorUtil.translate("  " + color + score + "% &f" + (name != null ? name : entry.getKey().toString())));
                        }
                    }

                    if (hwIdLinked.isEmpty() && similar.isEmpty()) {
                        sender.sendMessage(ColorUtil.translate("&aNo potential alts found."));
                    }

                    sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
                });
            });
        });
    }

    private void handleFingerprint(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getMessagesManager().get("hwid.fingerprint-usage", "prefix", plugin.getMessagesManager().getPrefix()));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getMessagesManager().get("general.player-not-found"));
            return;
        }

        HWIDManager manager = plugin.getHWIDManager();
        manager.updateFingerprint(target);
        sender.sendMessage(ColorUtil.translate("&aFingerprint updated for " + target.getName()));
    }

    private void handleStatus(CommandSender sender) {
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        sender.sendMessage(ColorUtil.translate("&c&lHWID System Status"));
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        sender.sendMessage(ColorUtil.translate("&7Status: &aENABLED"));
        sender.sendMessage(ColorUtil.translate("&7Required Client Mod: &f" + 
            plugin.getConfigManager().getString("hardware-ban.client-mod", "LiteBansClient")));
        sender.sendMessage(ColorUtil.translate("&7Alt Detection Threshold: &f" + 
            plugin.getConfigManager().getInt("hardware-ban.alt-detection-threshold", 70) + "%"));
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        sender.sendMessage(ColorUtil.translate("&c&lHardware ID Commands"));
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        sender.sendMessage(ColorUtil.translate("&c/hwid check <player> &8- &7View HWID info"));
        sender.sendMessage(ColorUtil.translate("&c/hwid ban <hwid> <reason> &8- &7Ban by HWID"));
         sender.sendMessage(ColorUtil.translate("&c/hwid unban <hwid> &8- &7Unban HWID"));
        sender.sendMessage(ColorUtil.translate("&c/hwid alts <player> &8- &7Find linked accounts"));
        sender.sendMessage(ColorUtil.translate("&c/hwid fingerprint <player> &8- &7Update fingerprint"));
        sender.sendMessage(ColorUtil.translate("&c/hwid status &8- &7System status"));
        sender.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("check", "ban", "unban", "alts", "fingerprint", "status").stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (args.length == 2 && (args[0].equalsIgnoreCase("check") || 
            args[0].equalsIgnoreCase("alts") || args[0].equalsIgnoreCase("fingerprint"))) {
            return PlayerUtil.getOnlineAndOfflinePlayerNames(args[1]);
        }
        
        return new ArrayList<>();
    }
}
