package com.nuvik.litebansreborn.managers;

import com.nuvik.litebansreborn.LiteBansReborn;
import org.bukkit.entity.Player;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Hardware ID Manager - Tracks and bans by hardware identifiers
 * Works with optional client-side mod that reports HWID
 * Also supports alternative fingerprinting methods
 */
public class HWIDManager {

    private final LiteBansReborn plugin;
    private final Map<UUID, String> hwidCache;
    private final Map<UUID, PlayerFingerprint> fingerprintCache;
    private final File dataFile;

    public HWIDManager(LiteBansReborn plugin) {
        this.plugin = plugin;
        this.hwidCache = new ConcurrentHashMap<>();
        this.fingerprintCache = new ConcurrentHashMap<>();
        this.dataFile = new File(plugin.getDataFolder(), "hwid_data.dat");
        
        loadData();
        initializeDatabase();
    }

    /**
     * Player fingerprint - alternative to HWID using behavioral patterns
     */
    public static class PlayerFingerprint implements Serializable {
        private static final long serialVersionUID = 1L;
        
        public final UUID playerUUID;
        public String lastIP;
        public String clientBrand; // e.g., "vanilla", "fabric", "forge"
        public int clientProtocol;
        public String locale;
        public int viewDistance;
        public String skinSignature;
        public long firstSeen;
        public long lastSeen;
        public Set<String> knownIPs = new HashSet<>();
        public String hwid; // From client mod if available

        public PlayerFingerprint(UUID playerUUID) {
            this.playerUUID = playerUUID;
            this.firstSeen = System.currentTimeMillis();
            this.lastSeen = System.currentTimeMillis();
        }

        /**
         * Calculate similarity score with another fingerprint (0-100)
         */
        public int similarityTo(PlayerFingerprint other) {
            if (other == null) return 0;
            
            int score = 0;
            int factors = 0;
            
            // HWID match is definitive
            if (hwid != null && hwid.equals(other.hwid)) {
                return 100;
            }
            
            // IP overlap
            factors++;
            for (String ip : knownIPs) {
                if (other.knownIPs.contains(ip)) {
                    score += 30;
                    break;
                }
            }
            
            // Client brand match
            if (clientBrand != null && clientBrand.equals(other.clientBrand)) {
                score += 10;
            }
            factors++;
            
            // Locale match
            if (locale != null && locale.equals(other.locale)) {
                score += 10;
            }
            factors++;
            
            // View distance match
            if (viewDistance == other.viewDistance && viewDistance != 0) {
                score += 5;
            }
            factors++;
            
            // Skin signature match (same skin)
            if (skinSignature != null && skinSignature.equals(other.skinSignature)) {
                score += 15;
            }
            factors++;
            
            return Math.min(100, score);
        }
    }

    /**
     * HWID Ban record
     */
    public static class HWIDBan {
        public final String hwid;
        public final String reason;
        public final UUID bannedBy;
        public final String bannedByName;
        public final long timestamp;
        public final List<UUID> linkedAccounts = new ArrayList<>();

        public HWIDBan(String hwid, String reason, UUID bannedBy, String bannedByName) {
            this.hwid = hwid;
            this.reason = reason;
            this.bannedBy = bannedBy;
            this.bannedByName = bannedByName;
            this.timestamp = System.currentTimeMillis();
        }
    }

    /**
     * Check if HWID banning is enabled
     */
    public boolean isEnabled() {
        return plugin.getConfigManager().getBoolean("hardware-ban.enabled", false);
    }

    /**
     * Register HWID from client mod
     */
    public void registerHWID(UUID playerUUID, String hwid) {
        if (hwid == null || hwid.isEmpty()) return;
        
        hwidCache.put(playerUUID, hwid);
        
        PlayerFingerprint fp = fingerprintCache.computeIfAbsent(playerUUID, PlayerFingerprint::new);
        fp.hwid = hwid;
        fp.lastSeen = System.currentTimeMillis();
        
        // Save to database
        saveHWID(playerUUID, hwid);
        
        plugin.debug("Registered HWID for " + playerUUID + ": " + hwid.substring(0, 8) + "...");
    }

    /**
     * Update player fingerprint on join
     */
    public void updateFingerprint(Player player) {
        PlayerFingerprint fp = fingerprintCache.computeIfAbsent(player.getUniqueId(), PlayerFingerprint::new);
        
        fp.lastIP = player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : null;
        fp.knownIPs.add(fp.lastIP);
        fp.locale = player.getLocale();
        fp.viewDistance = player.getClientViewDistance();
        fp.lastSeen = System.currentTimeMillis();
        
        // Client brand requires ProtocolLib or packet listener
        // Skin signature requires Mojang API call
        
        saveFingerprint(fp);
    }

    /**
     * Check if player's HWID is banned
     */
    public CompletableFuture<HWIDBan> checkBanned(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            String hwid = hwidCache.get(playerUUID);
            if (hwid == null) {
                // Try to load from database
                hwid = loadHWID(playerUUID);
                if (hwid != null) {
                    hwidCache.put(playerUUID, hwid);
                }
            }
            
            if (hwid == null) return null;
            
            return getHWIDBan(hwid);
        });
    }

    /**
     * Ban by HWID
     */
    public CompletableFuture<Boolean> banHWID(String hwid, String reason, UUID bannedBy, String bannedByName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Connection conn = plugin.getDatabaseManager().getConnection();
                String sql = "INSERT INTO " + plugin.getDatabaseManager().getTable("hwid_bans") +
                    " (hwid, reason, banned_by_uuid, banned_by_name, timestamp) VALUES (?, ?, ?, ?, ?)";
                
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, hwid);
                    stmt.setString(2, reason);
                    stmt.setString(3, bannedBy.toString());
                    stmt.setString(4, bannedByName);
                    stmt.setLong(5, System.currentTimeMillis());
                    stmt.executeUpdate();
                }
                
                plugin.log(Level.INFO, "HWID banned: " + hwid.substring(0, 8) + "... by " + bannedByName);
                return true;
            } catch (Exception e) {
                plugin.log(Level.SEVERE, "Failed to ban HWID: " + e.getMessage());
                return false;
            }
        });
    }

    /**
     * Find accounts linked to same HWID
     */
    public CompletableFuture<List<UUID>> findLinkedAccounts(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            List<UUID> linked = new ArrayList<>();
            
            String hwid = hwidCache.get(playerUUID);
            if (hwid == null) return linked;
            
            // Find all accounts with same HWID
            for (Map.Entry<UUID, String> entry : hwidCache.entrySet()) {
                if (entry.getValue().equals(hwid) && !entry.getKey().equals(playerUUID)) {
                    linked.add(entry.getKey());
                }
            }
            
            return linked;
        });
    }

    /**
     * Find similar fingerprints (potential alts)
     */
    public CompletableFuture<List<Map.Entry<UUID, Integer>>> findSimilarFingerprints(UUID playerUUID, int minScore) {
        return CompletableFuture.supplyAsync(() -> {
            List<Map.Entry<UUID, Integer>> results = new ArrayList<>();
            
            PlayerFingerprint target = fingerprintCache.get(playerUUID);
            if (target == null) return results;
            
            for (Map.Entry<UUID, PlayerFingerprint> entry : fingerprintCache.entrySet()) {
                if (entry.getKey().equals(playerUUID)) continue;
                
                int score = target.similarityTo(entry.getValue());
                if (score >= minScore) {
                    results.add(new AbstractMap.SimpleEntry<>(entry.getKey(), score));
                }
            }
            
            // Sort by score descending
            results.sort((a, b) -> b.getValue().compareTo(a.getValue()));
            
            return results;
        });
    }

    /**
     * Get fingerprint for player
     */
    public PlayerFingerprint getFingerprint(UUID playerUUID) {
        return fingerprintCache.get(playerUUID);
    }

    // ==================== DATABASE OPERATIONS ====================

    private void initializeDatabase() {
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            
            // HWID storage table
            String hwidTable = "CREATE TABLE IF NOT EXISTS " + plugin.getDatabaseManager().getTable("hwid_data") + " (" +
                "player_uuid VARCHAR(36) PRIMARY KEY, " +
                "hwid VARCHAR(128), " +
                "first_seen BIGINT, " +
                "last_seen BIGINT" +
                ")";
            
            // HWID bans table
            String banTable = "CREATE TABLE IF NOT EXISTS " + plugin.getDatabaseManager().getTable("hwid_bans") + " (" +
                "id INTEGER PRIMARY KEY AUTO_INCREMENT, " +
                "hwid VARCHAR(128) UNIQUE, " +
                "reason TEXT, " +
                "banned_by_uuid VARCHAR(36), " +
                "banned_by_name VARCHAR(16), " +
                "timestamp BIGINT" +
                ")";
            
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(hwidTable);
                stmt.execute(banTable);
            }
        } catch (Exception e) {
            plugin.log(Level.WARNING, "Failed to initialize HWID tables: " + e.getMessage());
        }
    }

    private void saveHWID(UUID playerUUID, String hwid) {
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            String sql = "REPLACE INTO " + plugin.getDatabaseManager().getTable("hwid_data") +
                " (player_uuid, hwid, first_seen, last_seen) VALUES (?, ?, ?, ?)";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerUUID.toString());
                stmt.setString(2, hwid);
                stmt.setLong(3, System.currentTimeMillis());
                stmt.setLong(4, System.currentTimeMillis());
                stmt.executeUpdate();
            }
        } catch (Exception e) {
            plugin.log(Level.WARNING, "Failed to save HWID: " + e.getMessage());
        }
    }

    private String loadHWID(UUID playerUUID) {
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            String sql = "SELECT hwid FROM " + plugin.getDatabaseManager().getTable("hwid_data") +
                " WHERE player_uuid = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerUUID.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("hwid");
                    }
                }
            }
        } catch (Exception e) {
            plugin.log(Level.WARNING, "Failed to load HWID: " + e.getMessage());
        }
        return null;
    }

    private HWIDBan getHWIDBan(String hwid) {
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            String sql = "SELECT * FROM " + plugin.getDatabaseManager().getTable("hwid_bans") +
                " WHERE hwid = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, hwid);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return new HWIDBan(
                            hwid,
                            rs.getString("reason"),
                            UUID.fromString(rs.getString("banned_by_uuid")),
                            rs.getString("banned_by_name")
                        );
                    }
                }
            }
        } catch (Exception e) {
            plugin.log(Level.WARNING, "Failed to check HWID ban: " + e.getMessage());
        }
        return null;
    }

    private void saveFingerprint(PlayerFingerprint fp) {
        // Async save to reduce main thread impact
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::saveData);
    }

    @SuppressWarnings("unchecked")
    private void loadData() {
        if (!dataFile.exists()) return;
        
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(dataFile))) {
            Map<UUID, PlayerFingerprint> data = (Map<UUID, PlayerFingerprint>) ois.readObject();
            fingerprintCache.putAll(data);
            plugin.log(Level.INFO, "Loaded " + data.size() + " fingerprints");
        } catch (Exception e) {
            plugin.log(Level.WARNING, "Failed to load fingerprint data: " + e.getMessage());
        }
    }

    private void saveData() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(dataFile))) {
            oos.writeObject(new HashMap<>(fingerprintCache));
        } catch (Exception e) {
            plugin.log(Level.WARNING, "Failed to save fingerprint data: " + e.getMessage());
        }
    }

    /**
     * Shutdown - save all data
     */
    public void shutdown() {
        saveData();
    }
}
