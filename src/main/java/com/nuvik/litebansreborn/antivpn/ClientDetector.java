package com.nuvik.litebansreborn.antivpn;

import com.nuvik.litebansreborn.LiteBansReborn;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Client Brand Detector - Detects what client/launcher players are using
 * Detects: Forge, Fabric, Lunar Client, Badlion, LabyMod, Vanilla, etc.
 * 
 * Uses plugin channels that clients register to identify themselves.
 */
public class ClientDetector implements Listener, PluginMessageListener {

    private final LiteBansReborn plugin;
    private final Map<UUID, ClientInfo> playerClients;
    
    // Known plugin channels for different clients
    private static final Map<String, String> KNOWN_CHANNELS = new HashMap<>();
    private static final Map<String, String> BRAND_MAPPING = new HashMap<>();
    private static final Set<String> SUSPICIOUS_MODS = new HashSet<>();
    private static final Set<String> CHEAT_CLIENTS = new HashSet<>();

    static {
        // Plugin channels registered by different clients
        KNOWN_CHANNELS.put("minecraft:brand", "Brand Channel");
        KNOWN_CHANNELS.put("MC|Brand", "Brand Channel (Legacy)");
        
        // Forge channels
        KNOWN_CHANNELS.put("FML|HS", "Forge");
        KNOWN_CHANNELS.put("FML", "Forge");
        KNOWN_CHANNELS.put("FORGE", "Forge");
        KNOWN_CHANNELS.put("fml:handshake", "Forge");
        KNOWN_CHANNELS.put("fml:play", "Forge");
        KNOWN_CHANNELS.put("forge:handshake", "Forge");
        KNOWN_CHANNELS.put("forge:tier_sorting", "Forge");
        
        // Fabric channels
        KNOWN_CHANNELS.put("fabric:registry/sync", "Fabric");
        KNOWN_CHANNELS.put("fabric-screen-handler-api-v1:open_screen", "Fabric");
        KNOWN_CHANNELS.put("fabric:container/open", "Fabric");
        
        // Lunar Client
        KNOWN_CHANNELS.put("lunarclient:pm", "Lunar Client");
        KNOWN_CHANNELS.put("Lunar-Client", "Lunar Client");
        KNOWN_CHANNELS.put("apollo:v1", "Lunar Client");
        
        // Badlion Client
        KNOWN_CHANNELS.put("badlion:mods", "Badlion Client");
        KNOWN_CHANNELS.put("BLC|M", "Badlion Client");
        KNOWN_CHANNELS.put("badlion:timers", "Badlion Client");
        
        // LabyMod
        KNOWN_CHANNELS.put("labymod:neo", "LabyMod");
        KNOWN_CHANNELS.put("labymod3:main", "LabyMod");
        KNOWN_CHANNELS.put("LMC", "LabyMod");
        KNOWN_CHANNELS.put("labymod:watermark", "LabyMod");
        
        // Feather Client
        KNOWN_CHANNELS.put("feather:client", "Feather Client");
        
        // PvPLounge
        KNOWN_CHANNELS.put("pvplounge:mods", "PvPLounge");
        
        // CheatBreaker (old)
        KNOWN_CHANNELS.put("CB|C", "CheatBreaker");
        
        // Cosmic Client
        KNOWN_CHANNELS.put("cosmic:client", "Cosmic Client");
        
        // Brand mappings (from client brand string)
        BRAND_MAPPING.put("vanilla", "Vanilla");
        BRAND_MAPPING.put("fabric", "Fabric");
        BRAND_MAPPING.put("forge", "Forge");
        BRAND_MAPPING.put("lunarclient", "Lunar Client");
        BRAND_MAPPING.put("lunar", "Lunar Client");
        BRAND_MAPPING.put("badlion", "Badlion Client");
        BRAND_MAPPING.put("labymod", "LabyMod");
        BRAND_MAPPING.put("feather", "Feather Client");
        BRAND_MAPPING.put("optifine", "OptiFine");
        BRAND_MAPPING.put("fml", "Forge");
        BRAND_MAPPING.put("fml,forge", "Forge");
        BRAND_MAPPING.put("quilt", "Quilt");
        BRAND_MAPPING.put("liteloader", "LiteLoader");
        
        // Suspicious mods (potential cheat mods)
        SUSPICIOUS_MODS.add("fabric"); // Can load cheat mods easily
        SUSPICIOUS_MODS.add("forge");  // Can load cheat mods
        SUSPICIOUS_MODS.add("liteloader");
        SUSPICIOUS_MODS.add("rift");
        
        // Known cheat clients (should be warned/blocked)
        CHEAT_CLIENTS.add("wurst");
        CHEAT_CLIENTS.add("aristois");
        CHEAT_CLIENTS.add("impact");
        CHEAT_CLIENTS.add("future");
        CHEAT_CLIENTS.add("sigma");
        CHEAT_CLIENTS.add("liquidbounce");
        CHEAT_CLIENTS.add("inertia");
        CHEAT_CLIENTS.add("meteor");
        CHEAT_CLIENTS.add("rusherhack");
        CHEAT_CLIENTS.add("lambda");
        CHEAT_CLIENTS.add("kami");
        CHEAT_CLIENTS.add("salhack");
        CHEAT_CLIENTS.add("phobos");
        CHEAT_CLIENTS.add("konas");
    }

    public ClientDetector(LiteBansReborn plugin) {
        this.plugin = plugin;
        this.playerClients = new ConcurrentHashMap<>();

        // Register plugin channels
        registerChannels();

        // Register as listener
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Register all known plugin channels
     */
    private void registerChannels() {
        // Register brand channel
        try {
            Bukkit.getMessenger().registerIncomingPluginChannel(plugin, "minecraft:brand", this);
        } catch (Exception e) {
            plugin.debug("Could not register minecraft:brand channel");
        }

        // Try legacy brand channel
        try {
            Bukkit.getMessenger().registerIncomingPluginChannel(plugin, "MC|Brand", this);
        } catch (Exception e) {
            plugin.debug("Could not register MC|Brand channel");
        }

        // Register known mod channels for detection
        for (String channel : KNOWN_CHANNELS.keySet()) {
            try {
                if (!channel.equals("minecraft:brand") && !channel.equals("MC|Brand")) {
                    Bukkit.getMessenger().registerIncomingPluginChannel(plugin, channel, this);
                }
            } catch (Exception e) {
                // Some channels may not be valid, ignore
            }
        }
    }

    /**
     * Handle plugin channel messages
     */
    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] data) {
        UUID uuid = player.getUniqueId();
        ClientInfo info = playerClients.computeIfAbsent(uuid, k -> new ClientInfo(player.getName()));

        // Record channel registration
        info.addChannel(channel);

        // Parse brand if it's the brand channel
        if (channel.equals("minecraft:brand") || channel.equals("MC|Brand")) {
            String brand = parseBrand(data);
            if (brand != null && !brand.isEmpty()) {
                info.setBrand(brand);
                plugin.debug("Player " + player.getName() + " brand: " + brand);
                
                // Check for suspicious/cheat clients
                checkClientSuspicion(player, info);
            }
        }

        // Detect client type from channel
        if (KNOWN_CHANNELS.containsKey(channel)) {
            String clientType = KNOWN_CHANNELS.get(channel);
            info.setDetectedClient(clientType);
            plugin.debug("Player " + player.getName() + " detected client: " + clientType + " (from channel: " + channel + ")");
        }
    }

    /**
     * Parse brand from plugin message data
     */
    private String parseBrand(byte[] data) {
        try {
            if (data.length == 0) return null;

            // Try reading as a string directly
            String brandDirect = new String(data, StandardCharsets.UTF_8).trim();
            
            // Remove any length prefix if present
            if (data.length > 1 && data[0] == (byte)(data.length - 1)) {
                brandDirect = new String(data, 1, data.length - 1, StandardCharsets.UTF_8).trim();
            }
            
            // Clean up the brand string
            brandDirect = brandDirect.replaceAll("[^a-zA-Z0-9,/\\- ]", "").trim();
            
            return brandDirect.isEmpty() ? null : brandDirect;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Check if client is suspicious and notify staff
     */
    private void checkClientSuspicion(Player player, ClientInfo info) {
        if (!plugin.getConfigManager().getBoolean("client-detection.notify-suspicious", true)) {
            return;
        }

        String brand = info.getBrand().toLowerCase();
        boolean suspicious = false;
        String reason = "";

        // Check for known cheat clients
        for (String cheat : CHEAT_CLIENTS) {
            if (brand.contains(cheat)) {
                suspicious = true;
                reason = "Known cheat client detected: " + cheat;
                break;
            }
        }

        // Check for modded clients (if configured to warn)
        if (!suspicious && plugin.getConfigManager().getBoolean("client-detection.warn-modded", true)) {
            for (String mod : SUSPICIOUS_MODS) {
                if (brand.contains(mod)) {
                    suspicious = true;
                    reason = "Modded client detected: " + getFriendlyClientName(brand);
                    break;
                }
            }
        }

        // Notify staff if suspicious
        if (suspicious) {
            String finalReason = reason;
            Bukkit.getScheduler().runTask(plugin, () -> {
                String message = plugin.getMessagesManager().get("client-detection.suspicious",
                        "player", player.getName(),
                        "client", info.getDisplayName(),
                        "brand", info.getBrand(),
                        "reason", finalReason
                );

                for (Player staff : Bukkit.getOnlinePlayers()) {
                    if (staff.hasPermission("litebansreborn.notify.client")) {
                        staff.sendMessage(message);
                    }
                }

                // Log to console
                plugin.log(Level.WARNING, "§e[Client Detection] §f" + player.getName() + " §7- " + finalReason);
            });
        }
    }

    /**
     * Handle channel registration event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onChannelRegister(PlayerRegisterChannelEvent event) {
        Player player = event.getPlayer();
        String channel = event.getChannel();
        UUID uuid = player.getUniqueId();

        ClientInfo info = playerClients.computeIfAbsent(uuid, k -> new ClientInfo(player.getName()));
        info.addChannel(channel);

        // Detect client from channel
        if (KNOWN_CHANNELS.containsKey(channel)) {
            String clientType = KNOWN_CHANNELS.get(channel);
            info.setDetectedClient(clientType);
            plugin.debug("Player " + player.getName() + " registered channel: " + channel + " (Client: " + clientType + ")");
        }
    }

    /**
     * Initialize client info on join
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Create client info if not exists
        playerClients.computeIfAbsent(uuid, k -> new ClientInfo(player.getName()));

        // Schedule a delayed check to ensure all channels are registered
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                ClientInfo info = playerClients.get(uuid);
                if (info != null && plugin.getConfigManager().getBoolean("client-detection.log-on-join", true)) {
                    plugin.debug("Player " + player.getName() + " client summary: " + info.getDisplayName() + 
                            " (Brand: " + info.getBrand() + ", Channels: " + info.getChannels().size() + ")");
                }
            }
        }, 40L); // 2 seconds delay
    }

    /**
     * Clean up on quit
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Keep info for a while in case they rejoin
        // playerClients.remove(event.getPlayer().getUniqueId());
    }

    /**
     * Get client info for a player
     */
    public ClientInfo getClientInfo(UUID uuid) {
        return playerClients.get(uuid);
    }

    /**
     * Get client info for a player
     */
    public ClientInfo getClientInfo(Player player) {
        return getClientInfo(player.getUniqueId());
    }

    /**
     * Get friendly client name from brand
     */
    public String getFriendlyClientName(String brand) {
        if (brand == null || brand.isEmpty()) {
            return "Unknown";
        }

        String lowerBrand = brand.toLowerCase();
        
        for (Map.Entry<String, String> entry : BRAND_MAPPING.entrySet()) {
            if (lowerBrand.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return brand;
    }

    /**
     * Check if player is using a modded client
     */
    public boolean isModdedClient(UUID uuid) {
        ClientInfo info = playerClients.get(uuid);
        if (info == null) return false;

        String brand = info.getBrand().toLowerCase();
        return brand.contains("forge") || brand.contains("fabric") || 
               brand.contains("quilt") || brand.contains("liteloader");
    }

    /**
     * Check if player is using a known anticheat-safe client
     */
    public boolean isSafeClient(UUID uuid) {
        ClientInfo info = playerClients.get(uuid);
        if (info == null) return true; // Assume safe if unknown

        String brand = info.getBrand().toLowerCase();
        
        // These clients have built-in anticheat or mod blocking
        return brand.contains("lunar") || brand.contains("badlion") || 
               brand.equals("vanilla") || brand.isEmpty();
    }

    /**
     * Shutdown the detector
     */
    public void shutdown() {
        playerClients.clear();
        
        // Unregister channels
        try {
            Bukkit.getMessenger().unregisterIncomingPluginChannel(plugin);
        } catch (Exception e) {
            // Ignore
        }
    }

    /**
     * Client information holder
     */
    public static class ClientInfo {
        private final String playerName;
        private String brand = "";
        private String detectedClient = "Unknown";
        private final Set<String> channels = ConcurrentHashMap.newKeySet();
        private final long firstSeen;
        private long lastUpdated;

        public ClientInfo(String playerName) {
            this.playerName = playerName;
            this.firstSeen = System.currentTimeMillis();
            this.lastUpdated = this.firstSeen;
        }

        public String getPlayerName() { return playerName; }
        public String getBrand() { return brand; }
        public String getDetectedClient() { return detectedClient; }
        public Set<String> getChannels() { return Collections.unmodifiableSet(channels); }
        public long getFirstSeen() { return firstSeen; }
        public long getLastUpdated() { return lastUpdated; }

        public void setBrand(String brand) {
            this.brand = brand != null ? brand : "";
            this.lastUpdated = System.currentTimeMillis();
        }

        public void setDetectedClient(String client) {
            this.detectedClient = client != null ? client : "Unknown";
            this.lastUpdated = System.currentTimeMillis();
        }

        public void addChannel(String channel) {
            this.channels.add(channel);
            this.lastUpdated = System.currentTimeMillis();
        }

        /**
         * Get a display-friendly name for the client
         */
        public String getDisplayName() {
            if (!detectedClient.equals("Unknown")) {
                return detectedClient;
            }
            if (!brand.isEmpty()) {
                return brand;
            }
            return "Vanilla";
        }

        /**
         * Check if client has specific forge/fabric mods loaded
         */
        public boolean hasModSupport() {
            return channels.stream().anyMatch(c -> 
                c.contains("fml") || c.contains("forge") || 
                c.contains("fabric") || c.contains("quilt"));
        }

        /**
         * Check if using a trusted PvP client
         */
        public boolean isTrustedPvPClient() {
            return detectedClient.contains("Lunar") || 
                   detectedClient.contains("Badlion") ||
                   detectedClient.contains("LabyMod");
        }
    }
}
