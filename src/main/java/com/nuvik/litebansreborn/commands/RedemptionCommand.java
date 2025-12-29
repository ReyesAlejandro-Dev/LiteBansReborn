package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.managers.RedemptionManager;
import com.nuvik.litebansreborn.utils.ColorUtil;
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
 * Redemption Command - Allow players to participate in redemption challenges
 * /redemption start <type> - Start a challenge
 * /redemption answer <answer> - Submit answer
 * /redemption status - Check redemption status
 */
public class RedemptionCommand implements CommandExecutor, TabCompleter {

    private final LiteBansReborn plugin;

    public RedemptionCommand(LiteBansReborn plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtil.translate("&cThis command can only be used by players."));
            return true;
        }

        if (!plugin.getRedemptionManager().isEnabled()) {
            player.sendMessage(ColorUtil.translate("&cRedemption system is disabled."));
            return true;
        }

        if (args.length < 1) {
            sendHelp(player);
            return true;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "start" -> handleStart(player, args);
            case "answer" -> handleAnswer(player, args);
            case "status" -> handleStatus(player);
            case "cancel" -> handleCancel(player);
            default -> sendHelp(player);
        }

        return true;
    }

    private void handleStart(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ColorUtil.translate("&cUsage: /redemption start <math|typing|quiz>"));
            return;
        }

        RedemptionManager manager = plugin.getRedemptionManager();
        String type = args[1].toLowerCase();
        RedemptionManager.RedemptionSession session;

        switch (type) {
            case "math", "captcha" -> {
                if (!manager.canStartChallenge(player.getUniqueId(), RedemptionManager.ChallengeType.MATH_CAPTCHA)) {
                    player.sendMessage(ColorUtil.translate("&cYou cannot start this challenge right now."));
                    return;
                }
                session = manager.startMathChallenge(player.getUniqueId());
            }
            case "typing" -> {
                if (!manager.canStartChallenge(player.getUniqueId(), RedemptionManager.ChallengeType.TYPING_TEST)) {
                    player.sendMessage(ColorUtil.translate("&cYou cannot start this challenge right now."));
                    return;
                }
                session = manager.startTypingChallenge(player.getUniqueId());
            }
            case "quiz" -> {
                if (!manager.canStartChallenge(player.getUniqueId(), RedemptionManager.ChallengeType.QUIZ)) {
                    player.sendMessage(ColorUtil.translate("&cYou cannot start this challenge right now."));
                    return;
                }
                session = manager.startQuizChallenge(player.getUniqueId());
            }
            default -> {
                player.sendMessage(ColorUtil.translate("&cUnknown challenge type. Use: math, typing, quiz"));
                return;
            }
        }

        manager.sendChallengePrompt(player, session);
    }

    private void handleAnswer(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ColorUtil.translate("&cUsage: /redemption answer <your_answer>"));
            return;
        }

        RedemptionManager manager = plugin.getRedemptionManager();
        RedemptionManager.RedemptionSession session = manager.getActiveSession(player.getUniqueId());

        if (session == null) {
            player.sendMessage(ColorUtil.translate("&cYou don't have an active challenge. Use /redemption start"));
            return;
        }

        // Combine all args as answer
        String answer = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        
        RedemptionManager.ChallengeResult result = manager.submitAnswer(player.getUniqueId(), answer);

        if (result.success) {
            player.sendMessage(ColorUtil.translate("&a&l✓ " + result.message));
            player.sendMessage(ColorUtil.translate("&7Your punishment has been reduced by &a" + result.reductionPercent + "%"));
            
            // Apply the reduction
            manager.applyReduction(player.getUniqueId(), result.reductionPercent);
        } else {
            player.sendMessage(ColorUtil.translate("&c&l✗ " + result.message));
        }
    }

    private void handleStatus(Player player) {
        RedemptionManager manager = plugin.getRedemptionManager();
        RedemptionManager.RedemptionSession session = manager.getActiveSession(player.getUniqueId());

        player.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        player.sendMessage(ColorUtil.translate("&a&lRedemption Status"));
        player.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));

        if (session != null) {
            player.sendMessage(ColorUtil.translate("&7Active Challenge: &e" + session.type.name()));
            player.sendMessage(ColorUtil.translate("&7Attempts Left: &f" + session.attemptsLeft));
            player.sendMessage("");
            player.sendMessage(ColorUtil.translate("&7Use &e/redemption answer <answer> &7to submit"));
        } else {
            player.sendMessage(ColorUtil.translate("&7No active challenge."));
            player.sendMessage("");
            player.sendMessage(ColorUtil.translate("&7Available challenges:"));
            player.sendMessage(ColorUtil.translate("&e  - math &7(10% reduction)"));
            player.sendMessage(ColorUtil.translate("&e  - typing &7(15% reduction)"));
            player.sendMessage(ColorUtil.translate("&e  - quiz &7(20% reduction)"));
            player.sendMessage("");
            player.sendMessage(ColorUtil.translate("&7Use &e/redemption start <type> &7to begin"));
        }

        player.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
    }

    private void handleCancel(Player player) {
        RedemptionManager manager = plugin.getRedemptionManager();
        
        if (manager.getActiveSession(player.getUniqueId()) != null) {
            manager.cancelSession(player.getUniqueId());
            player.sendMessage(ColorUtil.translate("&cChallenge cancelled."));
        } else {
            player.sendMessage(ColorUtil.translate("&7You don't have an active challenge."));
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        player.sendMessage(ColorUtil.translate("&a&lRedemption System"));
        player.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        player.sendMessage(ColorUtil.translate("&7Complete challenges to reduce your punishment!"));
        player.sendMessage("");
        player.sendMessage(ColorUtil.translate("&a/redemption start <type> &8- &7Start challenge"));
        player.sendMessage(ColorUtil.translate("&a/redemption answer <answer> &8- &7Submit answer"));
        player.sendMessage(ColorUtil.translate("&a/redemption status &8- &7View status"));
        player.sendMessage(ColorUtil.translate("&a/redemption cancel &8- &7Cancel challenge"));
        player.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("start", "answer", "status", "cancel").stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
            return Arrays.asList("math", "typing", "quiz").stream()
                .filter(s -> s.startsWith(args[1].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        return new ArrayList<>();
    }
}
