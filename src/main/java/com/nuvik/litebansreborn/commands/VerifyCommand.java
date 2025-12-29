package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.managers.VerificationManager;
import com.nuvik.litebansreborn.managers.VerificationManager.VerifiedPlayer;
import com.nuvik.litebansreborn.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Verification Command - Link Minecraft and Discord accounts
 * /verify - Generate verification code
 * /unlink - Unlink Discord account
 * /whois <player> - View player's linked Discord (staff)
 * /discordinfo - View your linked account
 */
public class VerifyCommand implements CommandExecutor, TabCompleter {

    private final LiteBansReborn plugin;

    public VerifyCommand(LiteBansReborn plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtil.translate("&cThis command can only be used by players."));
            return true;
        }

        VerificationManager manager = plugin.getVerificationManager();
        
        // Handle different commands based on label/alias
        if (label.equalsIgnoreCase("unlink")) {
            handleUnlink(player);
            return true;
        }
        
        if (label.equalsIgnoreCase("discordinfo")) {
            handleInfo(player);
            return true;
        }
        
        if (label.equalsIgnoreCase("whois") && args.length >= 1) {
            handleWhois(player, args[0]);
            return true;
        }

        // Default /verify command
        if (args.length < 1) {
            handleVerify(player);
            return true;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "code" -> handleVerify(player);
            case "info", "status" -> handleInfo(player);
            case "unlink" -> handleUnlink(player);
            case "whois" -> {
                if (args.length >= 2) {
                    handleWhois(player, args[1]);
                } else {
                    player.sendMessage(ColorUtil.translate("&cUsage: /verify whois <player>"));
                }
            }
            default -> sendHelp(player);
        }

        return true;
    }

    private void handleVerify(Player player) {
        VerificationManager manager = plugin.getVerificationManager();

        // Check if already verified
        if (manager.isVerified(player.getUniqueId())) {
            VerifiedPlayer vp = manager.getVerifiedPlayer(player.getUniqueId());
            player.sendMessage(ColorUtil.translate("&cYou are already verified!"));
            player.sendMessage(ColorUtil.translate("&7Linked to: &f" + vp.getDiscordName()));
            player.sendMessage(ColorUtil.translate("&7Use &e/unlink &7to unlink your account."));
            return;
        }

        // Check if Discord bot is enabled
        if (!plugin.getDiscordBotManager().isEnabled()) {
            player.sendMessage(ColorUtil.translate("&cDiscord verification is not available."));
            player.sendMessage(ColorUtil.translate("&7The Discord bot is not configured."));
            return;
        }

        // Generate code
        String code = manager.generateVerificationCode(player);
        
        if (code == null) {
            return; // Error message already sent
        }

        player.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        player.sendMessage(ColorUtil.translate("&a&l✦ DISCORD VERIFICATION ✦"));
        player.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        player.sendMessage(ColorUtil.translate(""));
        player.sendMessage(ColorUtil.translate("&7Your verification code is:"));
        player.sendMessage(ColorUtil.translate(""));
        player.sendMessage(ColorUtil.translate("  &e&l" + code));
        player.sendMessage(ColorUtil.translate(""));
        player.sendMessage(ColorUtil.translate("&7Go to our Discord server and use:"));
        player.sendMessage(ColorUtil.translate("  &b/verify " + code));
        player.sendMessage(ColorUtil.translate(""));
        player.sendMessage(ColorUtil.translate("&7This code expires in &f10 minutes&7."));
        player.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
    }

    private void handleInfo(Player player) {
        VerificationManager manager = plugin.getVerificationManager();
        VerifiedPlayer vp = manager.getVerifiedPlayer(player.getUniqueId());

        player.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        player.sendMessage(ColorUtil.translate("&b&lDiscord Account Info"));
        player.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));

        if (vp != null) {
            player.sendMessage(ColorUtil.translate("&7Status: &aVerified ✓"));
            player.sendMessage(ColorUtil.translate("&7Discord: &f" + vp.getDiscordName()));
            player.sendMessage(ColorUtil.translate("&7Discord ID: &7" + vp.getDiscordId()));
            player.sendMessage(ColorUtil.translate("&7Linked: &f" + formatDate(vp.getLinkedAt())));
        } else {
            player.sendMessage(ColorUtil.translate("&7Status: &cNot Verified"));
            player.sendMessage(ColorUtil.translate("&7Use &e/verify &7to link your Discord."));
        }

        player.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
    }

    private void handleUnlink(Player player) {
        VerificationManager manager = plugin.getVerificationManager();

        if (!manager.isVerified(player.getUniqueId())) {
            player.sendMessage(ColorUtil.translate("&cYou are not verified."));
            return;
        }

        manager.unlinkAccounts(player.getUniqueId()).thenAccept(success -> {
            if (success) {
                player.sendMessage(ColorUtil.translate("&aYour Discord account has been unlinked."));
                player.sendMessage(ColorUtil.translate("&7Use &e/verify &7to link a new account."));
            } else {
                player.sendMessage(ColorUtil.translate("&cFailed to unlink account. Please try again."));
            }
        });
    }

    private void handleWhois(Player player, String targetName) {
        if (!player.hasPermission("litebansreborn.verify.whois")) {
            plugin.getMessagesManager().send(player, "general.no-permission");
            return;
        }

        // Find target
        @SuppressWarnings("deprecation")
        org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            player.sendMessage(ColorUtil.translate("&cPlayer not found."));
            return;
        }

        VerificationManager manager = plugin.getVerificationManager();
        VerifiedPlayer vp = manager.getVerifiedPlayer(target.getUniqueId());

        player.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        player.sendMessage(ColorUtil.translate("&b&lWho Is: &f" + target.getName()));
        player.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));

        if (vp != null) {
            player.sendMessage(ColorUtil.translate("&7Discord Status: &aVerified ✓"));
            player.sendMessage(ColorUtil.translate("&7Discord Name: &f" + vp.getDiscordName()));
            player.sendMessage(ColorUtil.translate("&7Discord ID: &7" + vp.getDiscordId()));
            player.sendMessage(ColorUtil.translate("&7Linked At: &f" + formatDate(vp.getLinkedAt())));
        } else {
            player.sendMessage(ColorUtil.translate("&7Discord Status: &cNot Verified"));
        }

        player.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
    }

    private void sendHelp(Player player) {
        player.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        player.sendMessage(ColorUtil.translate("&b&lVerification Commands"));
        player.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        player.sendMessage(ColorUtil.translate("&e/verify &8- &7Generate verification code"));
        player.sendMessage(ColorUtil.translate("&e/verify info &8- &7View your linked Discord"));
        player.sendMessage(ColorUtil.translate("&e/unlink &8- &7Unlink Discord account"));
        
        if (player.hasPermission("litebansreborn.verify.whois")) {
            player.sendMessage(ColorUtil.translate("&c/verify whois <player> &8- &7View player's Discord"));
        }
        
        player.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
    }

    private String formatDate(long timestamp) {
        return new Date(timestamp).toString();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(Arrays.asList("code", "info", "unlink"));
            if (sender.hasPermission("litebansreborn.verify.whois")) {
                options.add("whois");
            }
            return options.stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (args.length == 2 && args[0].equalsIgnoreCase("whois")) {
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        return new ArrayList<>();
    }
}
