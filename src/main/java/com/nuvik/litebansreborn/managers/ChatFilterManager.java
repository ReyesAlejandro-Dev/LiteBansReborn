package com.nuvik.litebansreborn.managers;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.utils.ColorUtil;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Level;

/**
 * Chat Filter Manager - Anti-spam, profanity filter, and chat protection
 * Features:
 * - Blocked words/regex patterns
 * - Anti-spam (message rate limiting)
 * - Anti-caps (excessive caps lock)
 * - Anti-flood (repeated messages)
 * - Anti-advertisement (IPs, domains)
 * - Link blocking
 * - Character spam detection
 * 
 * @author Nuvik
 * @version 6.0.0
 */
public class ChatFilterManager {

    private final LiteBansReborn plugin;
    
    // ==================== CONSTANTS ====================
    
    /** Maximum time (ms) to keep message history for anti-spam detection */
    private static final long MESSAGE_HISTORY_RETENTION_MS = 60000; // 1 minute
    
    /** Maximum number of recent messages to keep per player */
    private static final int MAX_RECENT_MESSAGES = 10;
    
    /** Default caps percentage threshold */
    private static final int DEFAULT_CAPS_THRESHOLD = 70;
    
    /** Default minimum message length for caps check */
    private static final int DEFAULT_MIN_CAPS_LENGTH = 5;
    
    /** Default spam message limit */
    private static final int DEFAULT_SPAM_MESSAGE_LIMIT = 5;
    
    /** Default spam time window in seconds */
    private static final int DEFAULT_SPAM_TIME_WINDOW_SECONDS = 10;
    
    /** Default flood similarity threshold (percentage) */
    private static final int DEFAULT_FLOOD_SIMILARITY_THRESHOLD = 80;
    
    /** Default character spam threshold (consecutive same chars) */
    private static final int DEFAULT_CHAR_SPAM_THRESHOLD = 4;
    
    // ==================== FIELDS ====================
    
    // Blocked patterns
    private final List<Pattern> blockedPatterns = new ArrayList<>();
    private final List<String> blockedWords = new ArrayList<>();
    private final List<Pattern> whitelistDomains = new ArrayList<>();
    
    // Player tracking for anti-spam
    private final Map<UUID, PlayerChatData> playerChatData = new ConcurrentHashMap<>();
    
    // Configuration flags
    private boolean enabled = true;
    private boolean blockAds = true;
    private boolean blockLinks = false;
    private boolean blockCaps = true;
    private boolean blockSpam = true;
    private boolean blockFlood = true;
    private boolean blockCharSpam = true;
    
    // Configuration thresholds (loaded from config)
    private int capsPercentThreshold = DEFAULT_CAPS_THRESHOLD;
    private int minCapsLength = DEFAULT_MIN_CAPS_LENGTH;
    private int spamMessageLimit = DEFAULT_SPAM_MESSAGE_LIMIT;
    private int spamTimeWindow = DEFAULT_SPAM_TIME_WINDOW_SECONDS * 1000;
    private int floodSimilarityThreshold = DEFAULT_FLOOD_SIMILARITY_THRESHOLD;
    private int charSpamThreshold = DEFAULT_CHAR_SPAM_THRESHOLD;

    public ChatFilterManager(LiteBansReborn plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    /**
     * Filter result
     */
    public static class FilterResult {
        private final boolean blocked;
        private final FilterReason reason;
        private final String message;
        private final String filteredMessage;

        public FilterResult(boolean blocked, FilterReason reason, String message, String filteredMessage) {
            this.blocked = blocked;
            this.reason = reason;
            this.message = message;
            this.filteredMessage = filteredMessage;
        }

        public static FilterResult allow(String message) {
            return new FilterResult(false, null, message, message);
        }

        public static FilterResult block(FilterReason reason, String message) {
            return new FilterResult(true, reason, message, null);
        }

        public static FilterResult filter(String original, String filtered) {
            return new FilterResult(false, FilterReason.FILTERED, original, filtered);
        }

        public boolean isBlocked() { return blocked; }
        public FilterReason getReason() { return reason; }
        public String getMessage() { return message; }
        public String getFilteredMessage() { return filteredMessage; }
    }

    /**
     * Filter reason
     */
    public enum FilterReason {
        PROFANITY("Profanity"),
        ADVERTISEMENT("Advertisement"),
        LINK("Link/URL"),
        SPAM("Spam"),
        FLOOD("Message flood"),
        CAPS("Excessive caps"),
        CHAR_SPAM("Character spam"),
        BLOCKED_WORD("Blocked word"),
        REGEX_MATCH("Pattern match"),
        FILTERED("Content filtered");

        private final String displayName;

        FilterReason(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() { return displayName; }
    }

    /**
     * Player chat tracking data
     */
    private static class PlayerChatData {
        final List<Long> messageTimes = new ArrayList<>();
        final List<String> recentMessages = new ArrayList<>();
        long lastMessageTime = 0;
        int warnCount = 0;

        void addMessage(String message) {
            long now = System.currentTimeMillis();
            messageTimes.add(now);
            recentMessages.add(message.toLowerCase());
            lastMessageTime = now;

            // Keep only recent entries using defined constants
            messageTimes.removeIf(t -> now - t > MESSAGE_HISTORY_RETENTION_MS);
            while (recentMessages.size() > MAX_RECENT_MESSAGES) {
                recentMessages.remove(0);
            }
        }

        int getMessageCount(int windowMs) {
            long threshold = System.currentTimeMillis() - windowMs;
            return (int) messageTimes.stream().filter(t -> t >= threshold).count();
        }
    }

    // ==================== CONFIGURATION ====================

    private void loadConfig() {
        enabled = plugin.getConfigManager().getBoolean("chat-filter.enabled", true);
        
        // Load blocked words
        blockedWords.clear();
        List<String> words = plugin.getConfigManager().getStringList("chat-filter.blocked-words");
        blockedWords.addAll(words);

        // Load blocked patterns (regex)
        blockedPatterns.clear();
        List<String> patterns = plugin.getConfigManager().getStringList("chat-filter.blocked-patterns");
        for (String pattern : patterns) {
            try {
                blockedPatterns.add(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE));
            } catch (Exception e) {
                plugin.log(Level.WARNING, "Invalid chat filter pattern: " + pattern);
            }
        }

        // Load whitelist domains
        whitelistDomains.clear();
        List<String> domains = plugin.getConfigManager().getStringList("chat-filter.whitelist-domains");
        for (String domain : domains) {
            whitelistDomains.add(Pattern.compile(Pattern.quote(domain), Pattern.CASE_INSENSITIVE));
        }

        // Load settings
        blockAds = plugin.getConfigManager().getBoolean("chat-filter.block-ads", true);
        blockLinks = plugin.getConfigManager().getBoolean("chat-filter.block-links", false);
        blockCaps = plugin.getConfigManager().getBoolean("chat-filter.block-caps", true);
        blockSpam = plugin.getConfigManager().getBoolean("chat-filter.block-spam", true);
        blockFlood = plugin.getConfigManager().getBoolean("chat-filter.block-flood", true);
        blockCharSpam = plugin.getConfigManager().getBoolean("chat-filter.block-char-spam", true);

        capsPercentThreshold = plugin.getConfigManager().getInt("chat-filter.caps-threshold", 70);
        minCapsLength = plugin.getConfigManager().getInt("chat-filter.min-caps-length", 5);
        spamMessageLimit = plugin.getConfigManager().getInt("chat-filter.spam-message-limit", 5);
        spamTimeWindow = plugin.getConfigManager().getInt("chat-filter.spam-time-window", 10) * 1000;
        floodSimilarityThreshold = plugin.getConfigManager().getInt("chat-filter.flood-similarity", 80);
        charSpamThreshold = plugin.getConfigManager().getInt("chat-filter.char-spam-threshold", 4);

        plugin.log(Level.INFO, "Loaded chat filter with " + blockedWords.size() + " words and " + 
            blockedPatterns.size() + " patterns.");
    }

    public void reload() {
        loadConfig();
    }

    // ==================== FILTERING ====================

    /**
     * Filter a chat message
     */
    public FilterResult filterMessage(Player player, String message) {
        if (!enabled) {
            return FilterResult.allow(message);
        }

        // Bypass permission
        if (player.hasPermission("litebansreborn.chatfilter.bypass")) {
            return FilterResult.allow(message);
        }

        // Get or create player data
        PlayerChatData data = playerChatData.computeIfAbsent(player.getUniqueId(), k -> new PlayerChatData());

        // Check spam (message rate)
        if (blockSpam && isSpamming(data)) {
            return FilterResult.block(FilterReason.SPAM, message);
        }

        // Check flood (repeated messages)
        if (blockFlood && isFlooding(data, message)) {
            return FilterResult.block(FilterReason.FLOOD, message);
        }

        // Check character spam (aaaaaaaa)
        if (blockCharSpam && hasCharacterSpam(message)) {
            return FilterResult.block(FilterReason.CHAR_SPAM, message);
        }

        // Check caps
        if (blockCaps && hasExcessiveCaps(message)) {
            // Convert to lowercase instead of blocking
            return FilterResult.filter(message, message.toLowerCase());
        }

        // Check blocked words
        FilterResult wordResult = checkBlockedWords(message);
        if (wordResult.isBlocked()) {
            return wordResult;
        }
        message = wordResult.getFilteredMessage(); // May have been censored

        // Check regex patterns
        FilterResult patternResult = checkPatterns(message);
        if (patternResult.isBlocked()) {
            return patternResult;
        }

        // Check advertisements (IPs, domains)
        if (blockAds && containsAdvertisement(message)) {
            return FilterResult.block(FilterReason.ADVERTISEMENT, message);
        }

        // Check links
        if (blockLinks && containsLink(message)) {
            return FilterResult.block(FilterReason.LINK, message);
        }

        // Record message
        data.addMessage(message);

        return FilterResult.allow(message);
    }

    // ==================== CHECKS ====================

    private boolean isSpamming(PlayerChatData data) {
        return data.getMessageCount(spamTimeWindow) >= spamMessageLimit;
    }

    private boolean isFlooding(PlayerChatData data, String message) {
        String lowerMessage = message.toLowerCase();
        
        for (String recent : data.recentMessages) {
            if (similarity(lowerMessage, recent) >= floodSimilarityThreshold) {
                return true;
            }
        }
        
        return false;
    }

    private boolean hasCharacterSpam(String message) {
        char lastChar = 0;
        int count = 0;
        
        for (char c : message.toCharArray()) {
            if (c == lastChar && !Character.isWhitespace(c)) {
                count++;
                if (count >= charSpamThreshold) {
                    return true;
                }
            } else {
                count = 1;
                lastChar = c;
            }
        }
        
        return false;
    }

    private boolean hasExcessiveCaps(String message) {
        if (message.length() < minCapsLength) {
            return false;
        }

        int caps = 0;
        int letters = 0;

        for (char c : message.toCharArray()) {
            if (Character.isLetter(c)) {
                letters++;
                if (Character.isUpperCase(c)) {
                    caps++;
                }
            }
        }

        if (letters == 0) return false;

        double capsPercent = (caps * 100.0) / letters;
        return capsPercent >= capsPercentThreshold;
    }

    private FilterResult checkBlockedWords(String message) {
        String lowerMessage = message.toLowerCase();
        String filteredMessage = message;
        boolean found = false;

        for (String word : blockedWords) {
            String lowerWord = word.toLowerCase();
            if (lowerMessage.contains(lowerWord)) {
                found = true;
                // Censor the word
                String censor = "*".repeat(word.length());
                filteredMessage = filteredMessage.replaceAll("(?i)" + Pattern.quote(word), censor);
            }
        }

        if (found) {
            return FilterResult.filter(message, filteredMessage);
        }
        
        return FilterResult.allow(message);
    }

    private FilterResult checkPatterns(String message) {
        for (Pattern pattern : blockedPatterns) {
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                return FilterResult.block(FilterReason.REGEX_MATCH, message);
            }
        }
        
        return FilterResult.allow(message);
    }

    private boolean containsAdvertisement(String message) {
        // IP pattern
        Pattern ipPattern = Pattern.compile(
            "\\b(?:\\d{1,3}\\.){3}\\d{1,3}(?::\\d{1,5})?\\b"
        );
        
        if (ipPattern.matcher(message).find()) {
            return true;
        }

        // Domain pattern (excluding whitelisted)
        Pattern domainPattern = Pattern.compile(
            "(?i)\\b(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+(?:com|net|org|gg|me|io|co|tv|xyz|info|biz|us|uk|eu|de|fr|es|it|nl|ru|br|jp|kr|cn|au|ca|in)\\b"
        );
        
        Matcher matcher = domainPattern.matcher(message);
        while (matcher.find()) {
            String domain = matcher.group();
            boolean whitelisted = false;
            
            for (Pattern whitelist : whitelistDomains) {
                if (whitelist.matcher(domain).find()) {
                    whitelisted = true;
                    break;
                }
            }
            
            if (!whitelisted) {
                return true;
            }
        }

        return false;
    }

    private boolean containsLink(String message) {
        Pattern urlPattern = Pattern.compile(
            "(?i)\\b(?:https?://|www\\.)[a-z0-9+&@#/%?=~_|!:,.;-]*[a-z0-9+&@#/%=~_|]"
        );
        
        Matcher matcher = urlPattern.matcher(message);
        while (matcher.find()) {
            String url = matcher.group();
            boolean whitelisted = false;
            
            for (Pattern whitelist : whitelistDomains) {
                if (whitelist.matcher(url).find()) {
                    whitelisted = true;
                    break;
                }
            }
            
            if (!whitelisted) {
                return true;
            }
        }

        return false;
    }

    // ==================== UTILITY ====================

    /**
     * Calculate similarity between two strings (Levenshtein-based)
     */
    private int similarity(String s1, String s2) {
        if (s1.equals(s2)) return 100;
        if (s1.isEmpty() || s2.isEmpty()) return 0;

        int maxLen = Math.max(s1.length(), s2.length());
        int distance = levenshteinDistance(s1, s2);
        
        return (int) ((1 - (distance / (double) maxLen)) * 100);
    }

    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                    dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                    );
                }
            }
        }

        return dp[s1.length()][s2.length()];
    }

    // ==================== MANAGEMENT ====================

    /**
     * Add a blocked word at runtime
     */
    public void addBlockedWord(String word) {
        if (!blockedWords.contains(word.toLowerCase())) {
            blockedWords.add(word.toLowerCase());
        }
    }

    /**
     * Remove a blocked word
     */
    public void removeBlockedWord(String word) {
        blockedWords.remove(word.toLowerCase());
    }

    /**
     * Add a blocked pattern
     */
    public void addBlockedPattern(String pattern) {
        try {
            blockedPatterns.add(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE));
        } catch (Exception e) {
            plugin.log(Level.WARNING, "Invalid pattern: " + pattern);
        }
    }

    /**
     * Handle filter violation
     */
    public void handleViolation(Player player, FilterReason reason) {
        PlayerChatData data = playerChatData.get(player.getUniqueId());
        if (data != null) {
            data.warnCount++;

            // Auto-punishment after too many warnings
            int autoMuteThreshold = plugin.getConfigManager().getInt("chat-filter.auto-mute-threshold", 5);
            if (data.warnCount >= autoMuteThreshold) {
                String muteDuration = plugin.getConfigManager().getString("chat-filter.auto-mute-duration", "10m");
                plugin.getServer().dispatchCommand(
                    plugin.getServer().getConsoleSender(),
                    "mute " + player.getName() + " " + muteDuration + " Chat filter violations"
                );
                data.warnCount = 0;
            }
        }
    }

    // ==================== GETTERS ====================

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public List<String> getBlockedWords() { return new ArrayList<>(blockedWords); }
    public int getBlockedPatternCount() { return blockedPatterns.size(); }
}
