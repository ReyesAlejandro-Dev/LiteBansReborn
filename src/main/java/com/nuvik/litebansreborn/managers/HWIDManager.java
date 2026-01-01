package com.nuvik.litebansreborn.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.database.DatabaseManager;
import org.bukkit.entity.Player;

import java.io.*;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * Hardware ID Manager - Tracks and bans by hardware identifiers
 * Features: HWID tracking, fingerprinting, alt detection, linked accounts
 * 
 * v5.9 Improvements:
 * - Dedicated thread pool
 * - Proper connection handling (try-with-resources)
 * - JSON storage instead of Java serialization
 * - Debounced saves
 * - Fixed NPE in IP handling
 * - Normalized similarity scoring
 * - Database queries for linked accounts
 * - LRU cache with size limit
 */
public class HWIDManager {

    // ==================== CONSTANTS ====================
    private static final int EXECUTOR_POOL_SIZE = 2;
    private static final int MAX_CACHE_SIZE = 1000;
    private static final long SAVE_DEBOUNCE_TICKS = 20L * 10; // 10 seconds
    private static final String DATA_FILE_NAME = "hwid_fingerprints.json";
    
    // ==================== FIELDS ====================
    private final LiteBansReborn plugin;
    private final ExecutorService hwidExecutor;
    private final Gson gson;
    private final File dataFile;
    
    // Caches with LRU eviction
    private final Map<UUID, String> hwidCache;
    private final Map<UUID, PlayerFingerprint> fingerprintCache;
    
    // Inverted index for fast HWID lookup
    private final Map<String, Set<UUID>> hwidToPlayers = new ConcurrentHashMap<>();
    
    // Debounce save flag
    private final AtomicBoolean saveQueued = new AtomicBoolean(false);

    // ==================== CONSTRUCTOR ====================
    public HWIDManager(LiteBansReborn plugin) {
        this.plugin = plugin;
        
        this.hwidExecutor = Executors.newFixedThreadPool(EXECUTOR_POOL_SIZE,
            r -> new Thread(r, "LiteBansReborn-HWID-" + System.currentTimeMillis() % 1000));
        
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.dataFile = new File(plugin.getDataFolder(), DATA_FILE_NAME);
        
        // LRU cache implementation
        this.hwidCache = Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<UUID, String> eldest) {
                return size() > MAX_CACHE_SIZE;
            }
        });
        
        this.fingerprintCache = new ConcurrentHashMap<>();
        
        loadData();
        initializeDatabase();
    }

    // ==================== FINGERPRINT CLASS ====================
    
    public static class PlayerFingerprint {
        public final UUID playerUUID;
        public String lastIP;
        public String clientBrand;
        public int clientProtocol;
        public String locale;
        public int viewDistance;
        public String skinSignature;
        public long firstSeen;
        public long lastSeen;
        public Set<String> knownIPs = new HashSet<>();
        public String hwid;

        public PlayerFingerprint(UUID playerUUID) {
            this.playerUUID = playerUUID;
            this.firstSeen = System.currentTimeMillis();
            this.lastSeen = System.currentTimeMillis();
        }

        /**
         * Calculate normalized similarity score (0-100)
         * Uses weighted factors with proper normalization
         */
        public int similarityTo(PlayerFingerprint other) {
            if (other == null) return 0;
            
            // HWID match is definitive
            if (hwid != null && !hwid.isEmpty() && hwid.equals(other.hwid)) {
                return 100;
            }
            
            int score = 0;
            int maxPossible = 0;
            
            // IP overlap (weight: 30)
            maxPossible += 30;
            if (hasIPOverlap(other)) {
                score += 30;
            }
            
            // Client brand match (weight: 10)
            if (clientBrand != null && other.clientBrand != null) {
                maxPossible += 10;
                if (clientBrand.equals(other.clientBrand)) {
                    score += 10;
                }
            }
            
            // Locale match (weight: 10)
            if (locale != null && other.locale != null) {
                maxPossible += 10;
                if (locale.equals(other.locale)) {
                    score += 10;
                }
            }
            
            // View distance match (weight: 5)
            if (viewDistance > 0 && other.viewDistance > 0) {
                maxPossible += 5;
                if (viewDistance == other.viewDistance) {
                    score += 5;
                }
            }
            
            // Skin signature match (weight: 15)
            if (skinSignature != null && other.skinSignature != null) {
                maxPossible += 15;
                if (skinSignature.equals(other.skinSignature)) {
                    score += 15;
                }
            }
            
            // Normalize to 0-100 scale
            return maxPossible == 0 ? 0 : Math.min(100, (int) Math.round((score * 100.0) / maxPossible));
        }
        
        private boolean hasIPOverlap(PlayerFingerprint other) {
            if (knownIPs == null || other.knownIPs == null) return false;
            for (String ip : knownIPs) {
                if (ip != null && other.knownIPs.contains(ip)) {
                    return true;
                }
            }
            return false;
        }
    }

    // ==================== HWID BAN CLASS ====================
    
    public static class HWIDBan {
        public final String hwid;
        public final String reason;
        public final UUID bannedBy;
        public final String bannedByName;
        public final long timestamp;
        public final List<UUID> linkedAccounts;

        public HWIDBan(String hwid, String reason, UUID bannedBy, String bannedByName, List<UUID> linkedAccounts) {
            this.hwid = hwid;
            this.reason = reason;
            this.bannedBy = bannedBy;
            this.bannedByName = bannedByName;
            this.timestamp = System.currentTimeMillis();
            this.linkedAccounts = linkedAccounts != null ? linkedAccounts : new ArrayList<>();
        }
    }

    // ==================== PUBLIC API ====================
    
    public boolean isEnabled() {
        return plugin.getConfigManager().getBoolean("hardware-ban.enabled", false);
    }

    /**
     * Register HWID from client mod (with hashed logging for privacy)
     */
    public void registerHWID(UUID playerUUID, String hwid) {
        if (hwid == null || hwid.isBlank()) return;
        
        hwidCache.put(playerUUID, hwid);
        
        // Update inverted index
        hwidToPlayers.computeIfAbsent(hwid, k -> ConcurrentHashMap.newKeySet()).add(playerUUID);
        
        PlayerFingerprint fp = fingerprintCache.computeIfAbsent(playerUUID, PlayerFingerprint::new);
        fp.hwid = hwid;
        fp.lastSeen = System.currentTimeMillis();
        
        saveHWID(playerUUID, hwid);
        
        plugin.debug("Registered HWID for " + playerUUID + ": " + maskHWID(hwid));
    }

    /**
     * Update player fingerprint on join (with null-safe IP handling)
     */
    public void updateFingerprint(Player player) {
        PlayerFingerprint fp = fingerprintCache.computeIfAbsent(player.getUniqueId(), PlayerFingerprint::new);
        
        // Null-safe IP extraction
        String ip = extractPlayerIP(player);
        fp.lastIP = ip;
        if (ip != null && !ip.isBlank()) {
            fp.knownIPs.add(ip);
        }
        
        fp.locale = player.getLocale();
        fp.viewDistance = player.getClientViewDistance();
        fp.lastSeen = System.currentTimeMillis();
        
        saveFingerprint(fp);
    }
    
    private String extractPlayerIP(Player player) {
        InetSocketAddress address = player.getAddress();
        if (address == null || address.getAddress() == null) {
            return null;
        }
        return address.getAddress().getHostAddress();
    }

    /**
     * Check if player's HWID is banned (async, non-blocking)
     */
    public CompletableFuture<HWIDBan> checkBanned(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            String hwid = hwidCache.get(playerUUID);
            if (hwid == null) {
                hwid = loadHWID(playerUUID);
                if (hwid != null) {
                    hwidCache.put(playerUUID, hwid);
                    hwidToPlayers.computeIfAbsent(hwid, k -> ConcurrentHashMap.newKeySet()).add(playerUUID);
                }
            }
            
            if (hwid == null) return null;
            
            return getHWIDBan(hwid);
        }, hwidExecutor);
    }

    /**
     * Ban by HWID (with proper connection handling)
     */
    public CompletableFuture<Boolean> banHWID(String hwid, String reason, UUID bannedBy, String bannedByName) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "INSERT INTO " + plugin.getDatabaseManager().getTable("hwid_bans") +
                " (hwid, reason, banned_by_uuid, banned_by_name, timestamp) VALUES (?, ?, ?, ?, ?)";
            
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, hwid);
                stmt.setString(2, reason);
                stmt.setString(3, bannedBy.toString());
                stmt.setString(4, bannedByName);
                stmt.setLong(5, System.currentTimeMillis());
                stmt.executeUpdate();
                
                plugin.log(Level.INFO, "HWID banned: " + maskHWID(hwid) + " by " + bannedByName);
                return true;
            } catch (Exception e) {
                plugin.log(Level.SEVERE, "Failed to ban HWID: " + e.getMessage());
                return false;
            }
        }, hwidExecutor);
    }

    /**
     * Unban HWID (with proper connection handling)
     */
    public CompletableFuture<Boolean> unbanHWID(String hwid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM " + plugin.getDatabaseManager().getTable("hwid_bans") + " WHERE hwid = ?";
            
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, hwid);
                boolean result = stmt.executeUpdate() > 0;
                
                if (result) {
                    plugin.log(Level.INFO, "HWID unbanned: " + maskHWID(hwid));
                }
                return result;
            } catch (Exception e) {
                plugin.log(Level.WARNING, "Failed to unban HWID: " + e.getMessage());
                return false;
            }
        }, hwidExecutor);
    }

    /**
     * Find accounts linked to same HWID (queries database for completeness)
     */
    public CompletableFuture<List<UUID>> findLinkedAccounts(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            String hwid = hwidCache.get(playerUUID);
            if (hwid == null) {
                hwid = loadHWID(playerUUID);
            }
            
            if (hwid == null) return Collections.emptyList();
            
            // First check inverted index (fast path)
            Set<UUID> cached = hwidToPlayers.get(hwid);
            if (cached != null && cached.size() > 1) {
                List<UUID> result = new ArrayList<>(cached);
                result.remove(playerUUID);
                return result;
            }
            
            // Query database for complete results
            return findLinkedAccountsFromDB(hwid, playerUUID);
        }, hwidExecutor);
    }
    
    private List<UUID> findLinkedAccountsFromDB(String hwid, UUID excludeUUID) {
        List<UUID> linked = new ArrayList<>();
        String sql = "SELECT player_uuid FROM " + plugin.getDatabaseManager().getTable("hwid_data") + 
                     " WHERE hwid = ? AND player_uuid != ?";
        
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, hwid);
            stmt.setString(2, excludeUUID.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    linked.add(UUID.fromString(rs.getString("player_uuid")));
                }
            }
        } catch (Exception e) {
            plugin.log(Level.WARNING, "Failed to find linked accounts: " + e.getMessage());
        }
        return linked;
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
            
            results.sort((a, b) -> b.getValue().compareTo(a.getValue()));
            return results;
        }, hwidExecutor);
    }

    public PlayerFingerprint getFingerprint(UUID playerUUID) {
        return fingerprintCache.get(playerUUID);
    }

    // ==================== DATABASE OPERATIONS ====================

    private void initializeDatabase() {
        String hwidTable = "CREATE TABLE IF NOT EXISTS " + plugin.getDatabaseManager().getTable("hwid_data") + " (" +
            "player_uuid VARCHAR(36) PRIMARY KEY, " +
            "hwid VARCHAR(128), " +
            "first_seen BIGINT, " +
            "last_seen BIGINT" +
            ")";
        
        String autoInc = getAutoIncrementSyntax();
        String banTable = "CREATE TABLE IF NOT EXISTS " + plugin.getDatabaseManager().getTable("hwid_bans") + " (" +
            "id " + autoInc + ", " +
            "hwid VARCHAR(128) UNIQUE, " +
            "reason TEXT, " +
            "banned_by_uuid VARCHAR(36), " +
            "banned_by_name VARCHAR(16), " +
            "timestamp BIGINT" +
            ")";
        
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.execute(hwidTable);
            stmt.execute(banTable);
            
            // Create index for faster HWID lookups
            try {
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_hwid ON " + 
                    plugin.getDatabaseManager().getTable("hwid_data") + " (hwid)");
            } catch (Exception ignored) {
                // Index might already exist
            }
        } catch (Exception e) {
            plugin.log(Level.WARNING, "Failed to initialize HWID tables: " + e.getMessage());
        }
    }
    
    private String getAutoIncrementSyntax() {
        DatabaseManager.DatabaseType dbType = plugin.getDatabaseManager().getDatabaseType();
        return switch (dbType) {
            case POSTGRESQL -> "SERIAL PRIMARY KEY";
            case SQLITE -> "INTEGER PRIMARY KEY AUTOINCREMENT";
            default -> "INTEGER PRIMARY KEY AUTO_INCREMENT";
        };
    }

    private void saveHWID(UUID playerUUID, String hwid) {
        hwidExecutor.execute(() -> {
            String table = plugin.getDatabaseManager().getTable("hwid_data");
            long now = System.currentTimeMillis();
            
            // Use UPSERT based on database type
            String sql = getUpsertSQL(table);
            
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                DatabaseManager.DatabaseType dbType = plugin.getDatabaseManager().getDatabaseType();
                
                if (dbType == DatabaseManager.DatabaseType.MYSQL || dbType == DatabaseManager.DatabaseType.MARIADB) {
                    // MySQL: INSERT ... ON DUPLICATE KEY UPDATE
                    stmt.setString(1, playerUUID.toString());
                    stmt.setString(2, hwid);
                    stmt.setLong(3, now);
                    stmt.setLong(4, now);
                    stmt.setString(5, hwid);
                    stmt.setLong(6, now);
                } else {
                    // SQLite/H2/PostgreSQL: standard params
                    stmt.setString(1, playerUUID.toString());
                    stmt.setString(2, hwid);
                    stmt.setLong(3, now);
                    stmt.setLong(4, now);
                }
                
                stmt.executeUpdate();
            } catch (Exception e) {
                plugin.log(Level.WARNING, "Failed to save HWID: " + e.getMessage());
            }
        });
    }
    
    private String getUpsertSQL(String table) {
        DatabaseManager.DatabaseType dbType = plugin.getDatabaseManager().getDatabaseType();
        
        return switch (dbType) {
            case MYSQL, MARIADB -> 
                "INSERT INTO " + table + " (player_uuid, hwid, first_seen, last_seen) VALUES (?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE hwid = ?, last_seen = ?";
            case POSTGRESQL ->
                "INSERT INTO " + table + " (player_uuid, hwid, first_seen, last_seen) VALUES (?, ?, ?, ?) " +
                "ON CONFLICT(player_uuid) DO UPDATE SET hwid = EXCLUDED.hwid, last_seen = EXCLUDED.last_seen";
            default -> // SQLite, H2
                "INSERT OR REPLACE INTO " + table + " (player_uuid, hwid, first_seen, last_seen) VALUES (?, ?, ?, ?)";
        };
    }

    private String loadHWID(UUID playerUUID) {
        String sql = "SELECT hwid FROM " + plugin.getDatabaseManager().getTable("hwid_data") +
            " WHERE player_uuid = ?";
        
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, playerUUID.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("hwid");
                }
            }
        } catch (Exception e) {
            plugin.log(Level.WARNING, "Failed to load HWID: " + e.getMessage());
        }
        return null;
    }

    private HWIDBan getHWIDBan(String hwid) {
        String sql = "SELECT * FROM " + plugin.getDatabaseManager().getTable("hwid_bans") + " WHERE hwid = ?";
        
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, hwid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    List<UUID> linked = findLinkedAccountsFromDB(hwid, null);
                    return new HWIDBan(
                        hwid,
                        rs.getString("reason"),
                        UUID.fromString(rs.getString("banned_by_uuid")),
                        rs.getString("banned_by_name"),
                        linked
                    );
                }
            }
        } catch (Exception e) {
            plugin.log(Level.WARNING, "Failed to check HWID ban: " + e.getMessage());
        }
        return null;
    }

    // ==================== FINGERPRINT PERSISTENCE (JSON) ====================
    
    /**
     * Debounced save to prevent disk spam
     */
    private void saveFingerprint(PlayerFingerprint fp) {
        if (saveQueued.compareAndSet(false, true)) {
            plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                try {
                    saveData();
                } finally {
                    saveQueued.set(false);
                }
            }, SAVE_DEBOUNCE_TICKS);
        }
    }

    private void loadData() {
        if (!dataFile.exists()) return;
        
        try (Reader reader = new InputStreamReader(new FileInputStream(dataFile), StandardCharsets.UTF_8)) {
            Type type = new TypeToken<Map<String, PlayerFingerprint>>(){}.getType();
            Map<String, PlayerFingerprint> data = gson.fromJson(reader, type);
            
            if (data != null) {
                for (Map.Entry<String, PlayerFingerprint> entry : data.entrySet()) {
                    UUID uuid = UUID.fromString(entry.getKey());
                    PlayerFingerprint fp = entry.getValue();
                    fingerprintCache.put(uuid, fp);
                    
                    // Rebuild inverted index
                    if (fp.hwid != null && !fp.hwid.isBlank()) {
                        hwidToPlayers.computeIfAbsent(fp.hwid, k -> ConcurrentHashMap.newKeySet()).add(uuid);
                    }
                }
                plugin.log(Level.INFO, "Loaded " + data.size() + " fingerprints");
            }
        } catch (Exception e) {
            plugin.log(Level.WARNING, "Failed to load fingerprint data: " + e.getMessage());
        }
    }

    private synchronized void saveData() {
        // Convert to string-keyed map for JSON
        Map<String, PlayerFingerprint> data = new HashMap<>();
        for (Map.Entry<UUID, PlayerFingerprint> entry : fingerprintCache.entrySet()) {
            data.put(entry.getKey().toString(), entry.getValue());
        }
        
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(dataFile), StandardCharsets.UTF_8)) {
            gson.toJson(data, writer);
        } catch (Exception e) {
            plugin.log(Level.WARNING, "Failed to save fingerprint data: " + e.getMessage());
        }
    }

    // ==================== UTILITIES ====================
    
    /**
     * Mask HWID for logging (privacy)
     */
    private String maskHWID(String hwid) {
        if (hwid == null || hwid.length() < 8) return "***";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(hwid.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                sb.append(String.format("%02x", hash[i]));
            }
            return sb + "...";
        } catch (Exception e) {
            return hwid.substring(0, 4) + "****";
        }
    }

    // ==================== SHUTDOWN ====================
    
    public void shutdown() {
        saveData();
        hwidExecutor.shutdownNow();
        try {
            hwidExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
