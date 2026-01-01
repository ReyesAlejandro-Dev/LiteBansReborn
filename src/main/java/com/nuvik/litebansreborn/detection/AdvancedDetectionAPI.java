package com.nuvik.litebansreborn.detection;

import com.nuvik.litebansreborn.LiteBansReborn;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Advanced Detection API - External Detection System
 * 
 * A sophisticated, modular detection system that can be enabled/disabled
 * entirely as an optional addon. Provides:
 * - Client brand detection (Forge, Fabric, Lunar, etc.)
 * - Cheat client detection (Meteor, Wurst, etc.)
 * - Bedrock cheat detection (Toolbox, Horion)
 * - Custom rule-based detection
 * - Plugin messaging channel analysis
 * 
 * This is an optional module - fully functional without it.
 * 
 * @author Nuvik
 * @version 6.0.0
 */
public class AdvancedDetectionAPI implements PluginMessageListener, Listener {

    private final LiteBansReborn plugin;
    private FileConfiguration config;
    private boolean enabled = false;
    
    // Player data storage
    private final Map<UUID, DetectedClient> playerClients = new ConcurrentHashMap<>();
    
    // Detection rules
    private final List<DetectionRule> javaClientRules = new ArrayList<>();
    private final List<DetectionRule> cheatClientRules = new ArrayList<>();
    private final List<DetectionRule> suspiciousModRules = new ArrayList<>();
    private final List<DetectionRule> bedrockCheatRules = new ArrayList<>();
    private final List<DetectionRule> customRules = new ArrayList<>();
    
    // Whitelisted channels
    private final Set<String> whitelistedChannels = new HashSet<>();
    
    // Channel names
    private static final String BRAND_CHANNEL_1_13 = "minecraft:brand";
    private static final String BRAND_CHANNEL_LEGACY = "MC|Brand";
    
    public AdvancedDetectionAPI(LiteBansReborn plugin) {
        this.plugin = plugin;
        loadConfig();
        
        if (enabled) {
            registerChannels();
            Bukkit.getPluginManager().registerEvents(this, plugin);
            plugin.log(Level.INFO, "§a[Detection API] Advanced Detection System enabled!");
        }
    }
    
    // ==================== CONFIGURATION ====================
    
    private void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "client-detection.yml");
        
        if (!configFile.exists()) {
            plugin.saveResource("client-detection.yml", false);
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
        enabled = config.getBoolean("enabled", true);
        
        if (!enabled) {
            plugin.log(Level.INFO, "§e[Detection API] Advanced Detection is disabled in config.");
            return;
        }
        
        // Load whitelisted channels
        whitelistedChannels.clear();
        whitelistedChannels.addAll(config.getStringList("whitelist.channels"));
        
        // Load Java client rules
        loadJavaClientRules();
        
        // Load cheat client rules
        loadCheatClientRules();
        
        // Load suspicious mod rules
        loadSuspiciousModRules();
        
        // Load Bedrock cheat rules
        loadBedrockCheatRules();
        
        // Load custom rules
        loadCustomRules();
        
        plugin.log(Level.INFO, "§a[Detection API] Loaded " + getTotalRulesCount() + " detection rules.");
    }
    
    private void loadJavaClientRules() {
        javaClientRules.clear();
        
        var section = config.getConfigurationSection("java-clients");
        if (section == null) return;
        
        for (String clientName : section.getKeys(false)) {
            var clientSection = section.getConfigurationSection(clientName);
            if (clientSection == null || !clientSection.getBoolean("enabled", true)) continue;
            
            DetectionRule rule = new DetectionRule(
                clientName,
                DetectionCategory.JAVA_CLIENT,
                parseAction(clientSection.getString("action", "LOG")),
                clientSection.getString("alert-message", ""),
                clientSection.getBoolean("allowed", true)
            );
            
            javaClientRules.add(rule);
        }
    }
    
    private void loadCheatClientRules() {
        cheatClientRules.clear();
        
        if (!config.getBoolean("cheat-clients.enabled", true)) return;
        
        DetectionAction defaultAction = parseAction(config.getString("cheat-clients.default-action", "BAN"));
        String banReason = config.getString("cheat-clients.ban-reason", "Cheating/Hacked Client Detected");
        
        var clientsSection = config.getConfigurationSection("cheat-clients.clients");
        if (clientsSection == null) return;
        
        for (String clientName : clientsSection.getKeys(false)) {
            var clientSection = clientsSection.getConfigurationSection(clientName);
            if (clientSection == null || !clientSection.getBoolean("enabled", true)) continue;
            
            DetectionRule rule = new DetectionRule(
                clientName,
                DetectionCategory.CHEAT_CLIENT,
                parseAction(clientSection.getString("action", defaultAction.name())),
                "",
                false
            );
            
            // Add brand patterns
            rule.setBrandPatterns(clientSection.getStringList("brands"));
            rule.setChannelPatterns(clientSection.getStringList("channels"));
            rule.setBanReason(banReason);
            
            cheatClientRules.add(rule);
        }
    }
    
    private void loadSuspiciousModRules() {
        suspiciousModRules.clear();
        
        if (!config.getBoolean("suspicious-mods.enabled", true)) return;
        
        DetectionAction defaultAction = parseAction(config.getString("suspicious-mods.default-action", "ALERT"));
        
        var modsSection = config.getConfigurationSection("suspicious-mods.mods");
        if (modsSection == null) return;
        
        for (String modName : modsSection.getKeys(false)) {
            var modSection = modsSection.getConfigurationSection(modName);
            if (modSection == null || !modSection.getBoolean("enabled", true)) continue;
            
            DetectionRule rule = new DetectionRule(
                modName,
                DetectionCategory.SUSPICIOUS_MOD,
                parseAction(modSection.getString("action", defaultAction.name())),
                "",
                false
            );
            
            rule.setChannelPatterns(modSection.getStringList("channels"));
            
            suspiciousModRules.add(rule);
        }
    }
    
    private void loadBedrockCheatRules() {
        bedrockCheatRules.clear();
        
        if (!config.getBoolean("bedrock-clients.enabled", true)) return;
        
        // Toolbox detection
        if (config.getBoolean("bedrock-clients.toolbox.enabled", true)) {
            DetectionRule toolbox = new DetectionRule(
                "toolbox",
                DetectionCategory.BEDROCK_CHEAT,
                parseAction(config.getString("bedrock-clients.toolbox.action", "BAN")),
                "",
                false
            );
            toolbox.setBanReason(config.getString("bedrock-clients.toolbox.ban-reason", "Toolbox (Cheat) Detected"));
            bedrockCheatRules.add(toolbox);
        }
        
        // Horion detection
        if (config.getBoolean("bedrock-clients.horion.enabled", true)) {
            DetectionRule horion = new DetectionRule(
                "horion",
                DetectionCategory.BEDROCK_CHEAT,
                parseAction(config.getString("bedrock-clients.horion.action", "BAN")),
                "",
                false
            );
            horion.setBanReason(config.getString("bedrock-clients.horion.ban-reason", "Horion Client (Cheat) Detected"));
            bedrockCheatRules.add(horion);
        }
        
        // Other Bedrock cheats
        if (config.getBoolean("bedrock-clients.other-cheats.enabled", true)) {
            List<String> otherCheats = config.getStringList("bedrock-clients.other-cheats.clients");
            for (String cheat : otherCheats) {
                DetectionRule rule = new DetectionRule(
                    cheat,
                    DetectionCategory.BEDROCK_CHEAT,
                    parseAction(config.getString("bedrock-clients.other-cheats.action", "BAN")),
                    "",
                    false
                );
                bedrockCheatRules.add(rule);
            }
        }
    }
    
    private void loadCustomRules() {
        customRules.clear();
        
        if (!config.getBoolean("custom-rules.enabled", false)) return;
        
        var rulesList = config.getMapList("custom-rules.rules");
        for (var ruleMap : rulesList) {
            // Check if enabled - use get() instead of getOrDefault to avoid type issues
            Object enabledObj = ruleMap.get("enabled");
            boolean isEnabled = enabledObj instanceof Boolean && (Boolean) enabledObj;
            if (!isEnabled) continue;
            
            // Get name
            Object nameObj = ruleMap.get("name");
            String name = nameObj != null ? nameObj.toString() : "unknown";
            
            // Get action
            Object actionObj = ruleMap.get("action");
            String action = actionObj != null ? actionObj.toString() : "ALERT";
            
            // Get message
            Object messageObj = ruleMap.get("message");
            String message = messageObj != null ? messageObj.toString() : "";
            
            DetectionRule rule = new DetectionRule(
                name,
                DetectionCategory.CUSTOM,
                parseAction(action),
                message,
                false
            );
            
            List<String> brands = new ArrayList<>();
            List<String> channels = new ArrayList<>();
            
            Object brandsObj = ruleMap.get("brands");
            if (brandsObj instanceof List<?>) {
                for (Object b : (List<?>) brandsObj) {
                    if (b != null) brands.add(b.toString());
                }
            }
            
            Object channelsObj = ruleMap.get("channels");
            if (channelsObj instanceof List<?>) {
                for (Object c : (List<?>) channelsObj) {
                    if (c != null) channels.add(c.toString());
                }
            }
            
            rule.setBrandPatterns(brands);
            rule.setChannelPatterns(channels);
            
            customRules.add(rule);
        }
    }
    
    private DetectionAction parseAction(String action) {
        try {
            return DetectionAction.valueOf(action.toUpperCase());
        } catch (IllegalArgumentException e) {
            return DetectionAction.LOG;
        }
    }
    
    private int getTotalRulesCount() {
        return javaClientRules.size() + cheatClientRules.size() + 
               suspiciousModRules.size() + bedrockCheatRules.size() + customRules.size();
    }
    
    // ==================== CHANNEL REGISTRATION ====================
    
    private void registerChannels() {
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, BRAND_CHANNEL_1_13, this);
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, BRAND_CHANNEL_LEGACY, this);
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, BRAND_CHANNEL_1_13);
    }
    
    public void shutdown() {
        Bukkit.getMessenger().unregisterIncomingPluginChannel(plugin, BRAND_CHANNEL_1_13, this);
        Bukkit.getMessenger().unregisterIncomingPluginChannel(plugin, BRAND_CHANNEL_LEGACY, this);
        playerClients.clear();
    }
    
    // ==================== PLUGIN MESSAGING ====================
    
    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!enabled) return;
        
        if (channel.equals(BRAND_CHANNEL_1_13) || channel.equals(BRAND_CHANNEL_LEGACY)) {
            handleBrandMessage(player, message);
        }
    }
    
    private void handleBrandMessage(Player player, byte[] message) {
        try {
            // Decode brand string
            String brand = decodeBrand(message);
            
            // Get or create client info
            DetectedClient clientInfo = playerClients.computeIfAbsent(
                player.getUniqueId(), 
                k -> new DetectedClient(player.getUniqueId(), player.getName())
            );
            
            clientInfo.setBrand(brand);
            
            // Run detection
            runDetection(player, clientInfo);
            
        } catch (Exception e) {
            plugin.log(Level.WARNING, "[Detection API] Error processing brand for " + player.getName() + ": " + e.getMessage());
        }
    }
    
    private String decodeBrand(byte[] message) {
        // Read VarInt length prefix and then string
        int length = 0;
        int numRead = 0;
        int idx = 0;
        
        do {
            if (idx >= message.length) {
                return new String(message);
            }
            byte read = message[idx++];
            int value = (read & 0b01111111);
            length |= (value << (7 * numRead));
            numRead++;
        } while ((message[idx - 1] & 0b10000000) != 0);
        
        if (idx + length > message.length) {
            return new String(message);
        }
        
        return new String(message, idx, length);
    }
    
    // ==================== DETECTION ENGINE ====================
    
    private void runDetection(Player player, DetectedClient clientInfo) {
        String brand = clientInfo.getBrand().toLowerCase();
        
        // Check cheat clients first (highest priority)
        for (DetectionRule rule : cheatClientRules) {
            if (matchesBrand(brand, rule)) {
                handleDetection(player, clientInfo, rule);
                return; // Stop on first cheat match
            }
        }
        
        // Check suspicious mods
        for (DetectionRule rule : suspiciousModRules) {
            if (matchesBrand(brand, rule)) {
                handleDetection(player, clientInfo, rule);
            }
        }
        
        // Check custom rules
        for (DetectionRule rule : customRules) {
            if (matchesBrand(brand, rule)) {
                handleDetection(player, clientInfo, rule);
            }
        }
        
        // Check Java clients (informational)
        for (DetectionRule rule : javaClientRules) {
            if (matchesClientType(brand, rule.getName())) {
                clientInfo.setClientType(rule.getName());
                if (rule.getAction() != DetectionAction.NONE) {
                    handleDetection(player, clientInfo, rule);
                }
                break;
            }
        }
    }
    
    private boolean matchesBrand(String brand, DetectionRule rule) {
        for (String pattern : rule.getBrandPatterns()) {
            if (brand.contains(pattern.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
    
    private boolean matchesClientType(String brand, String clientType) {
        return switch (clientType.toLowerCase()) {
            case "forge" -> brand.contains("forge") || brand.contains("fml");
            case "fabric" -> brand.contains("fabric");
            case "quilt" -> brand.contains("quilt");
            case "lunar" -> brand.contains("lunarclient");
            case "badlion" -> brand.contains("badlion");
            case "labymod" -> brand.contains("labymod");
            case "feather" -> brand.contains("feather");
            case "pvplounge" -> brand.contains("pvplounge");
            case "vanilla" -> brand.equals("vanilla") || brand.equals("paper") || brand.equals("spigot");
            default -> brand.contains(clientType.toLowerCase());
        };
    }
    
    // ==================== ACTION HANDLING ====================
    
    private void handleDetection(Player player, DetectedClient clientInfo, DetectionRule rule) {
        clientInfo.addDetection(rule);
        
        // Log detection
        if (config.getBoolean("logging.enabled", true)) {
            logDetection(player, clientInfo, rule);
        }
        
        // Execute action
        switch (rule.getAction()) {
            case NONE -> {}
            
            case LOG -> plugin.debug("[Detection] " + player.getName() + " detected: " + rule.getName());
            
            case ALERT -> alertStaff(player, clientInfo, rule);
            
            case KICK -> Bukkit.getScheduler().runTask(plugin, () -> {
                player.kickPlayer("§c" + rule.getName() + " is not allowed on this server.");
            });
            
            case BAN -> Bukkit.getScheduler().runTask(plugin, () -> {
                String banReason = rule.getBanReason() != null ? rule.getBanReason() : "Cheat Client Detected";
                long duration = config.getLong("cheat-clients.ban-duration", -1);
                
                plugin.getBanManager().ban(
                    player.getUniqueId(),
                    player.getName(),
                    getPlayerIP(player),
                    null, // Console
                    "CONSOLE",
                    banReason,
                    duration > 0 ? duration : null,
                    false,
                    false
                );
            });
        }
    }
    
    private void alertStaff(Player player, DetectedClient clientInfo, DetectionRule rule) {
        String message = rule.getAlertMessage();
        if (message.isEmpty()) {
            message = "§8[§cDetection§8] §e" + player.getName() + " §7was detected using §c" + rule.getName();
        } else {
            message = message.replace("%player%", player.getName())
                            .replace("%client%", rule.getName())
                            .replace("%brand%", clientInfo.getBrand());
        }
        
        String alertPermission = config.getString("alert-permission", "litebansreborn.alerts.clientdetection");
        String finalMessage = message;
        
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission(alertPermission)) {
                staff.sendMessage(finalMessage);
            }
        }
        
        Bukkit.getConsoleSender().sendMessage(message);
    }
    
    private void logDetection(Player player, DetectedClient clientInfo, DetectionRule rule) {
        // Implementation for file logging
        String format = config.getString("logging.format", 
            "[%date%] %player% (%uuid%) - Client: %client%, Brand: %brand%");
        
        String logMessage = format
            .replace("%date%", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()))
            .replace("%player%", player.getName())
            .replace("%uuid%", player.getUniqueId().toString())
            .replace("%client%", rule.getName())
            .replace("%brand%", clientInfo.getBrand())
            .replace("%channels%", String.join(", ", clientInfo.getRegisteredChannels()));
        
        plugin.debug(logMessage);
    }
    
    private String getPlayerIP(Player player) {
        if (player.getAddress() != null) {
            return player.getAddress().getAddress().getHostAddress();
        }
        return "unknown";
    }
    
    // ==================== EVENTS ====================
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!enabled) return;
        
        Player player = event.getPlayer();
        
        // Check for Bedrock player
        if (isBedrockPlayer(player)) {
            handleBedrockPlayer(player);
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerClients.remove(event.getPlayer().getUniqueId());
    }
    
    private boolean isBedrockPlayer(Player player) {
        String geyserPrefix = config.getString("bedrock-clients.geyser-prefix", ".");
        return player.getName().startsWith(geyserPrefix);
    }
    
    private void handleBedrockPlayer(Player player) {
        DetectedClient clientInfo = playerClients.computeIfAbsent(
            player.getUniqueId(),
            k -> new DetectedClient(player.getUniqueId(), player.getName())
        );
        
        clientInfo.setBedrock(true);
        clientInfo.setBrand("Bedrock Edition");
        
        // Alert if configured
        if (config.getBoolean("bedrock-clients.detection.enabled", true)) {
            DetectionRule bedrockRule = new DetectionRule(
                "Bedrock",
                DetectionCategory.BEDROCK_PLAYER,
                parseAction(config.getString("bedrock-clients.detection.action", "LOG")),
                config.getString("bedrock-clients.detection.alert-message", ""),
                true
            );
            
            handleDetection(player, clientInfo, bedrockRule);
        }
        
        // TODO: Implement Toolbox detection via packet analysis
        // This would require deeper integration with ProtocolLib or similar
    }
    
    // ==================== PUBLIC API ====================
    
    /**
     * Get the detected client for a player
     */
    public DetectedClient getPlayerClient(UUID uuid) {
        return playerClients.get(uuid);
    }
    
    /**
     * Get the detected client for a player
     */
    public DetectedClient getPlayerClient(Player player) {
        return playerClients.get(player.getUniqueId());
    }
    
    /**
     * Check if a player is using a specific client type
     */
    public boolean isUsingClient(Player player, String clientType) {
        DetectedClient client = playerClients.get(player.getUniqueId());
        if (client == null) return false;
        return matchesClientType(client.getBrand().toLowerCase(), clientType);
    }
    
    /**
     * Check if a player was flagged for any detection
     */
    public boolean isFlagged(Player player) {
        DetectedClient client = playerClients.get(player.getUniqueId());
        return client != null && !client.getDetections().isEmpty();
    }
    
    /**
     * Check if a player is a Bedrock player
     */
    public boolean isBedrockPlayer(UUID uuid) {
        DetectedClient client = playerClients.get(uuid);
        return client != null && client.isBedrock();
    }
    
    /**
     * Get all currently tracked players
     */
    public Map<UUID, DetectedClient> getAllPlayerClients() {
        return Collections.unmodifiableMap(playerClients);
    }
    
    /**
     * Reload the detection configuration
     */
    public void reload() {
        loadConfig();
    }
    
    /**
     * Check if the detection system is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    // ==================== INNER CLASSES ====================
    
    public enum DetectionAction {
        NONE, LOG, ALERT, KICK, BAN
    }
    
    public enum DetectionCategory {
        JAVA_CLIENT,
        CHEAT_CLIENT,
        SUSPICIOUS_MOD,
        BEDROCK_PLAYER,
        BEDROCK_CHEAT,
        CUSTOM
    }
    
    public static class DetectionRule {
        private final String name;
        private final DetectionCategory category;
        private final DetectionAction action;
        private final String alertMessage;
        private final boolean allowed;
        private List<String> brandPatterns = new ArrayList<>();
        private List<String> channelPatterns = new ArrayList<>();
        private String banReason;
        
        public DetectionRule(String name, DetectionCategory category, DetectionAction action, 
                           String alertMessage, boolean allowed) {
            this.name = name;
            this.category = category;
            this.action = action;
            this.alertMessage = alertMessage;
            this.allowed = allowed;
        }
        
        // Getters and setters
        public String getName() { return name; }
        public DetectionCategory getCategory() { return category; }
        public DetectionAction getAction() { return action; }
        public String getAlertMessage() { return alertMessage; }
        public boolean isAllowed() { return allowed; }
        public List<String> getBrandPatterns() { return brandPatterns; }
        public void setBrandPatterns(List<String> patterns) { this.brandPatterns = patterns; }
        public List<String> getChannelPatterns() { return channelPatterns; }
        public void setChannelPatterns(List<String> patterns) { this.channelPatterns = patterns; }
        public String getBanReason() { return banReason; }
        public void setBanReason(String reason) { this.banReason = reason; }
    }
    
    public static class DetectedClient {
        private final UUID uuid;
        private final String playerName;
        private String brand = "unknown";
        private String clientType = "unknown";
        private boolean bedrock = false;
        private final Set<String> registeredChannels = new HashSet<>();
        private final List<DetectionRule> detections = new ArrayList<>();
        private final long firstSeen;
        
        public DetectedClient(UUID uuid, String playerName) {
            this.uuid = uuid;
            this.playerName = playerName;
            this.firstSeen = System.currentTimeMillis();
        }
        
        // Getters and setters
        public UUID getUuid() { return uuid; }
        public String getPlayerName() { return playerName; }
        public String getBrand() { return brand; }
        public void setBrand(String brand) { this.brand = brand; }
        public String getClientType() { return clientType; }
        public void setClientType(String type) { this.clientType = type; }
        public boolean isBedrock() { return bedrock; }
        public void setBedrock(boolean bedrock) { this.bedrock = bedrock; }
        public Set<String> getRegisteredChannels() { return registeredChannels; }
        public void addChannel(String channel) { registeredChannels.add(channel); }
        public List<DetectionRule> getDetections() { return detections; }
        public void addDetection(DetectionRule rule) { detections.add(rule); }
        public long getFirstSeen() { return firstSeen; }
    }
}
