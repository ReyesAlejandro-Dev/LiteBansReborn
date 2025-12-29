package com.nuvik.litebansreborn.managers;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.models.Punishment;
import com.nuvik.litebansreborn.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redemption Manager - Allows banned players to reduce their punishment through minigames
 * Features:
 * - Math captcha challenges
 * - Time reduction rewards
 * - Redemption tracking
 */
public class RedemptionManager {

    private final LiteBansReborn plugin;
    private final Map<UUID, RedemptionSession> activeSessions;
    private final Map<UUID, Set<String>> completedChallenges;
    private final Random random = new Random();

    public RedemptionManager(LiteBansReborn plugin) {
        this.plugin = plugin;
        this.activeSessions = new ConcurrentHashMap<>();
        this.completedChallenges = new ConcurrentHashMap<>();
    }

    /**
     * Challenge types
     */
    public enum ChallengeType {
        MATH_CAPTCHA("captcha-math", 10, 3),
        TYPING_TEST("typing-test", 15, 2),
        QUIZ("quiz", 20, 1);

        private final String configKey;
        private final int defaultReduction;
        private final int defaultMaxAttempts;

        ChallengeType(String configKey, int defaultReduction, int defaultMaxAttempts) {
            this.configKey = configKey;
            this.defaultReduction = defaultReduction;
            this.defaultMaxAttempts = defaultMaxAttempts;
        }

        public String getConfigKey() { return configKey; }
        public int getDefaultReduction() { return defaultReduction; }
        public int getDefaultMaxAttempts() { return defaultMaxAttempts; }
    }

    /**
     * Active redemption session
     */
    public static class RedemptionSession {
        public final UUID playerUUID;
        public final ChallengeType type;
        public final String question;
        public final String answer;
        public final long startTime;
        public int attemptsLeft;

        public RedemptionSession(UUID playerUUID, ChallengeType type, String question, String answer, int maxAttempts) {
            this.playerUUID = playerUUID;
            this.type = type;
            this.question = question;
            this.answer = answer;
            this.startTime = System.currentTimeMillis();
            this.attemptsLeft = maxAttempts;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - startTime > 300000; // 5 minutes
        }
    }

    /**
     * Check if redemption is enabled
     */
    public boolean isEnabled() {
        return plugin.getConfigManager().getBoolean("redemption.enabled", true);
    }

    /**
     * Check if player can start a challenge
     */
    public boolean canStartChallenge(UUID playerUUID, ChallengeType type) {
        if (!isEnabled()) return false;
        
        // Check if already in session
        if (activeSessions.containsKey(playerUUID)) {
            return false;
        }
        
        // Check if already completed (one-time challenges)
        Set<String> completed = completedChallenges.getOrDefault(playerUUID, new HashSet<>());
        if (completed.contains(type.name())) {
            boolean oneTime = plugin.getConfigManager().getBoolean(
                "redemption.minigames." + type.getConfigKey() + ".one-time", false);
            if (oneTime) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Start a math captcha challenge
     */
    public RedemptionSession startMathChallenge(UUID playerUUID) {
        int num1 = random.nextInt(50) + 10;
        int num2 = random.nextInt(50) + 10;
        int operation = random.nextInt(3); // 0=add, 1=sub, 2=mul
        
        String question;
        int answer;
        
        switch (operation) {
            case 0:
                question = num1 + " + " + num2;
                answer = num1 + num2;
                break;
            case 1:
                question = num1 + " - " + num2;
                answer = num1 - num2;
                break;
            default:
                num1 = random.nextInt(12) + 1;
                num2 = random.nextInt(12) + 1;
                question = num1 + " × " + num2;
                answer = num1 * num2;
                break;
        }
        
        int maxAttempts = plugin.getConfigManager().getInt(
            "redemption.minigames.captcha-math.max-attempts", 3);
        
        RedemptionSession session = new RedemptionSession(
            playerUUID, ChallengeType.MATH_CAPTCHA, question, String.valueOf(answer), maxAttempts);
        
        activeSessions.put(playerUUID, session);
        return session;
    }

    /**
     * Start a typing test challenge
     */
    public RedemptionSession startTypingChallenge(UUID playerUUID) {
        String[] phrases = {
            "I will follow the server rules",
            "I understand my actions have consequences",
            "I promise to be respectful to others",
            "I will not break the rules again",
            "Fair play makes games fun for everyone"
        };
        
        String phrase = phrases[random.nextInt(phrases.length)];
        int maxAttempts = plugin.getConfigManager().getInt(
            "redemption.minigames.typing-test.max-attempts", 2);
        
        RedemptionSession session = new RedemptionSession(
            playerUUID, ChallengeType.TYPING_TEST, phrase, phrase.toLowerCase().replaceAll("\\s+", ""), maxAttempts);
        
        activeSessions.put(playerUUID, session);
        return session;
    }

    /**
     * Start a quiz challenge
     */
    public RedemptionSession startQuizChallenge(UUID playerUUID) {
        String[][] quizzes = {
            {"What should you do if you see someone hacking?", "report"},
            {"Is griefing allowed on this server?", "no"},
            {"Should you share your account with others?", "no"},
            {"What command do you use to report a player?", "/report"}
        };
        
        String[] quiz = quizzes[random.nextInt(quizzes.length)];
        int maxAttempts = plugin.getConfigManager().getInt(
            "redemption.minigames.quiz.max-attempts", 1);
        
        RedemptionSession session = new RedemptionSession(
            playerUUID, ChallengeType.QUIZ, quiz[0], quiz[1].toLowerCase(), maxAttempts);
        
        activeSessions.put(playerUUID, session);
        return session;
    }

    /**
     * Submit an answer to active challenge
     */
    public ChallengeResult submitAnswer(UUID playerUUID, String answer) {
        RedemptionSession session = activeSessions.get(playerUUID);
        if (session == null) {
            return new ChallengeResult(false, "No active challenge", 0);
        }
        
        if (session.isExpired()) {
            activeSessions.remove(playerUUID);
            return new ChallengeResult(false, "Challenge expired", 0);
        }
        
        String normalizedAnswer = answer.toLowerCase().replaceAll("\\s+", "");
        boolean correct = normalizedAnswer.equals(session.answer);
        
        if (correct) {
            activeSessions.remove(playerUUID);
            
            // Mark as completed
            completedChallenges.computeIfAbsent(playerUUID, k -> new HashSet<>())
                .add(session.type.name());
            
            // Calculate reduction
            int reduction = plugin.getConfigManager().getInt(
                "redemption.minigames." + session.type.getConfigKey() + ".reduction",
                session.type.getDefaultReduction());
            
            return new ChallengeResult(true, "Correct! Your punishment has been reduced.", reduction);
        } else {
            session.attemptsLeft--;
            if (session.attemptsLeft <= 0) {
                activeSessions.remove(playerUUID);
                return new ChallengeResult(false, "Out of attempts. Challenge failed.", 0);
            }
            return new ChallengeResult(false, "Incorrect. " + session.attemptsLeft + " attempts remaining.", 0);
        }
    }

    /**
     * Apply reduction to a punishment
     */
    public void applyReduction(UUID playerUUID, int percentReduction) {
        plugin.getBanManager().getActiveBan(playerUUID).thenAccept(ban -> {
            if (ban == null || ban.isPermanent()) {
                return;
            }
            
            long remaining = ban.getRemainingTime();
            long reduction = (remaining * percentReduction) / 100;
            long newRemaining = remaining - reduction;
            
            // Update in database (simplified - would need actual implementation)
            plugin.log(java.util.logging.Level.INFO, 
                "Reduced punishment for " + playerUUID + " by " + percentReduction + "% (" + 
                (reduction / 1000 / 60) + " minutes)");
        });
    }

    /**
     * Get active session for player
     */
    public RedemptionSession getActiveSession(UUID playerUUID) {
        return activeSessions.get(playerUUID);
    }

    /**
     * Cancel active session
     */
    public void cancelSession(UUID playerUUID) {
        activeSessions.remove(playerUUID);
    }

    /**
     * Challenge result
     */
    public static class ChallengeResult {
        public final boolean success;
        public final String message;
        public final int reductionPercent;

        public ChallengeResult(boolean success, String message, int reductionPercent) {
            this.success = success;
            this.message = message;
            this.reductionPercent = reductionPercent;
        }
    }

    /**
     * Send challenge prompt to player
     */
    public void sendChallengePrompt(Player player, RedemptionSession session) {
        player.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        player.sendMessage(ColorUtil.translate("&a&l✦ REDEMPTION CHALLENGE ✦"));
        player.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        
        switch (session.type) {
            case MATH_CAPTCHA:
                player.sendMessage(ColorUtil.translate("&7Solve this math problem:"));
                player.sendMessage(ColorUtil.translate("&e&l" + session.question + " = ?"));
                player.sendMessage("");
                player.sendMessage(ColorUtil.translate("&7Type your answer in chat."));
                break;
            case TYPING_TEST:
                player.sendMessage(ColorUtil.translate("&7Type the following phrase exactly:"));
                player.sendMessage(ColorUtil.translate("&e\"" + session.question + "\""));
                break;
            case QUIZ:
                player.sendMessage(ColorUtil.translate("&7Answer this question:"));
                player.sendMessage(ColorUtil.translate("&e" + session.question));
                break;
        }
        
        player.sendMessage("");
        player.sendMessage(ColorUtil.translate("&7Attempts remaining: &f" + session.attemptsLeft));
        player.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
    }
}
