package com.nuvik.litebansreborn.antivpn;

import com.nuvik.litebansreborn.LiteBansReborn;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * SQLite database for storing VPN detections
 * Tracks IPs, VPN providers, and player associations
 */
public class VPNDatabase {

    private final LiteBansReborn plugin;
    private Connection connection;
    private final File databaseFile;
    private final Object dbLock = new Object(); // Lock for SQLite thread safety

    public VPNDatabase(LiteBansReborn plugin) {
        this.plugin = plugin;
        this.databaseFile = new File(plugin.getDataFolder(), "vpn_detections.db");
    }

    /**
     * Initialize the database connection and create tables
     */
    public void initialize() {
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }

            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());

            createTables();
            plugin.log(Level.INFO, "§aVPN Database initialized successfully!");
        } catch (Exception e) {
            plugin.log(Level.SEVERE, "Failed to initialize VPN database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Create the necessary tables
     */
    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Main VPN detections table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS vpn_detections (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    ip VARCHAR(45) NOT NULL,
                    player_uuid VARCHAR(36),
                    player_name VARCHAR(16),
                    is_vpn BOOLEAN DEFAULT FALSE,
                    is_proxy BOOLEAN DEFAULT FALSE,
                    is_hosting BOOLEAN DEFAULT FALSE,
                    is_tor BOOLEAN DEFAULT FALSE,
                    vpn_provider VARCHAR(255),
                    isp VARCHAR(255),
                    org VARCHAR(255),
                    asn VARCHAR(50),
                    country VARCHAR(100),
                    country_code VARCHAR(10),
                    city VARCHAR(100),
                    real_ip VARCHAR(45),
                    risk_score DOUBLE DEFAULT 0.0,
                    api_provider VARCHAR(50),
                    action_taken VARCHAR(20),
                    detected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE(ip, player_uuid)
                )
            """);

            // IP tracking table for detecting real IPs
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS ip_tracking (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_uuid VARCHAR(36) NOT NULL,
                    player_name VARCHAR(16),
                    ip VARCHAR(45) NOT NULL,
                    is_vpn BOOLEAN DEFAULT FALSE,
                    first_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    login_count INTEGER DEFAULT 1,
                    UNIQUE(player_uuid, ip)
                )
            """);

            // Known VPN providers table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS known_vpn_providers (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    provider_name VARCHAR(255) NOT NULL UNIQUE,
                    detection_count INTEGER DEFAULT 1,
                    first_detected TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    last_detected TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // IP range blocks (for datacenter/hosting ranges)
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS blocked_ip_ranges (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    start_ip VARCHAR(45) NOT NULL,
                    end_ip VARCHAR(45) NOT NULL,
                    reason VARCHAR(255),
                    added_by VARCHAR(36),
                    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // Create indexes for faster lookups
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_vpn_ip ON vpn_detections(ip)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_vpn_player ON vpn_detections(player_uuid)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_tracking_uuid ON ip_tracking(player_uuid)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_tracking_ip ON ip_tracking(ip)");
        }
    }

    /**
     * Log a VPN detection
     */
    public CompletableFuture<Void> logDetection(VPNResult result, UUID playerUUID, String playerName, String actionTaken) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT OR REPLACE INTO vpn_detections 
                (ip, player_uuid, player_name, is_vpn, is_proxy, is_hosting, is_tor, 
                 vpn_provider, isp, org, asn, country, country_code, city, real_ip, 
                 risk_score, api_provider, action_taken, detected_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            """;

            synchronized (dbLock) {
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setString(1, result.getIp());
                    ps.setString(2, playerUUID != null ? playerUUID.toString() : null);
                    ps.setString(3, playerName);
                    ps.setBoolean(4, result.isVPN());
                    ps.setBoolean(5, result.isProxy());
                    ps.setBoolean(6, result.isHosting());
                    ps.setBoolean(7, result.isTor());
                    ps.setString(8, result.getVpnProvider());
                    ps.setString(9, result.getIsp());
                    ps.setString(10, result.getOrg());
                    ps.setString(11, result.getAsn());
                    ps.setString(12, result.getCountry());
                    ps.setString(13, result.getCountryCode());
                    ps.setString(14, result.getCity());
                    ps.setString(15, result.getRealIP());
                    ps.setDouble(16, result.getRiskScore());
                    ps.setString(17, result.getApiProvider());
                    ps.setString(18, actionTaken);
                    ps.executeUpdate();
                } catch (SQLException e) {
                    plugin.log(Level.WARNING, "Failed to log VPN detection: " + e.getMessage());
                }
            }

            // Update VPN provider tracking
            if (result.isDangerous() && result.getServiceName() != null && !result.getServiceName().isEmpty()) {
                updateVPNProvider(result.getServiceName());
            }
        });
    }

    /**
     * Track player IP connection
     */
    public CompletableFuture<Void> trackIP(UUID playerUUID, String playerName, String ip, boolean isVPN) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT INTO ip_tracking (player_uuid, player_name, ip, is_vpn, first_seen, last_seen, login_count)
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1)
                ON CONFLICT(player_uuid, ip) DO UPDATE SET
                    player_name = excluded.player_name,
                    is_vpn = excluded.is_vpn,
                    last_seen = CURRENT_TIMESTAMP,
                    login_count = login_count + 1
            """;

            synchronized (dbLock) {
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setString(1, playerUUID.toString());
                    ps.setString(2, playerName);
                    ps.setString(3, ip);
                    ps.setBoolean(4, isVPN);
                    ps.executeUpdate();
                } catch (SQLException e) {
                    plugin.log(Level.WARNING, "Failed to track IP: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Update VPN provider statistics
     */
    private void updateVPNProvider(String providerName) {
        String sql = """
            INSERT INTO known_vpn_providers (provider_name, detection_count, first_detected, last_detected)
            VALUES (?, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT(provider_name) DO UPDATE SET
                detection_count = detection_count + 1,
                last_detected = CURRENT_TIMESTAMP
        """;

        synchronized (dbLock) {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, providerName);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.log(Level.WARNING, "Failed to update VPN provider: " + e.getMessage());
            }
        }
    }

    /**
     * Get player's likely real IP based on historical non-VPN connections
     */
    public CompletableFuture<String> getLikelyRealIP(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
                SELECT ip, login_count 
                FROM ip_tracking 
                WHERE player_uuid = ? AND is_vpn = FALSE 
                ORDER BY login_count DESC, last_seen DESC 
                LIMIT 1
            """;

            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, playerUUID.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return rs.getString("ip");
                }
            } catch (SQLException e) {
                plugin.log(Level.WARNING, "Failed to get likely real IP: " + e.getMessage());
            }
            return null;
        });
    }

    /**
     * Get all IPs used by a player
     */
    public CompletableFuture<List<TrackedIP>> getPlayerIPs(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            List<TrackedIP> ips = new ArrayList<>();
            String sql = """
                SELECT ip, is_vpn, first_seen, last_seen, login_count 
                FROM ip_tracking 
                WHERE player_uuid = ? 
                ORDER BY last_seen DESC
            """;

            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, playerUUID.toString());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    ips.add(new TrackedIP(
                            rs.getString("ip"),
                            rs.getBoolean("is_vpn"),
                            rs.getTimestamp("first_seen").getTime(),
                            rs.getTimestamp("last_seen").getTime(),
                            rs.getInt("login_count")
                    ));
                }
            } catch (SQLException e) {
                plugin.log(Level.WARNING, "Failed to get player IPs: " + e.getMessage());
            }
            return ips;
        });
    }

    /**
     * Check if an IP is a known VPN IP
     */
    public CompletableFuture<Boolean> isKnownVPN(String ip) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT is_vpn FROM vpn_detections WHERE ip = ? AND is_vpn = TRUE LIMIT 1";

            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, ip);
                ResultSet rs = ps.executeQuery();
                return rs.next();
            } catch (SQLException e) {
                plugin.log(Level.WARNING, "Failed to check known VPN: " + e.getMessage());
            }
            return false;
        });
    }

    /**
     * Get VPN detection statistics
     */
    public CompletableFuture<VPNStats> getStats() {
        return CompletableFuture.supplyAsync(() -> {
            VPNStats stats = new VPNStats();

            try (Statement stmt = connection.createStatement()) {
                // Total detections
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM vpn_detections WHERE is_vpn = TRUE OR is_proxy = TRUE");
                if (rs.next()) stats.totalDetections = rs.getInt(1);

                // Total unique IPs
                rs = stmt.executeQuery("SELECT COUNT(DISTINCT ip) FROM vpn_detections WHERE is_vpn = TRUE OR is_proxy = TRUE");
                if (rs.next()) stats.uniqueVPNIPs = rs.getInt(1);

                // Total kicks
                rs = stmt.executeQuery("SELECT COUNT(*) FROM vpn_detections WHERE action_taken = 'KICK'");
                if (rs.next()) stats.totalKicks = rs.getInt(1);

                // Total warnings
                rs = stmt.executeQuery("SELECT COUNT(*) FROM vpn_detections WHERE action_taken = 'WARN'");
                if (rs.next()) stats.totalWarnings = rs.getInt(1);

                // Top VPN providers
                rs = stmt.executeQuery("SELECT provider_name, detection_count FROM known_vpn_providers ORDER BY detection_count DESC LIMIT 10");
                while (rs.next()) {
                    stats.topProviders.add(new ProviderStat(rs.getString("provider_name"), rs.getInt("detection_count")));
                }

                // Detections today
                rs = stmt.executeQuery("SELECT COUNT(*) FROM vpn_detections WHERE date(detected_at) = date('now')");
                if (rs.next()) stats.detectionsToday = rs.getInt(1);

            } catch (SQLException e) {
                plugin.log(Level.WARNING, "Failed to get VPN stats: " + e.getMessage());
            }

            return stats;
        });
    }

    /**
     * Get recent VPN detections
     */
    public CompletableFuture<List<VPNDetectionRecord>> getRecentDetections(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<VPNDetectionRecord> records = new ArrayList<>();
            String sql = """
                SELECT * FROM vpn_detections 
                WHERE is_vpn = TRUE OR is_proxy = TRUE OR is_hosting = TRUE OR is_tor = TRUE
                ORDER BY detected_at DESC 
                LIMIT ?
            """;

            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, limit);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    records.add(new VPNDetectionRecord(
                            rs.getString("ip"),
                            rs.getString("player_uuid"),
                            rs.getString("player_name"),
                            rs.getString("vpn_provider"),
                            rs.getString("country"),
                            rs.getString("action_taken"),
                            rs.getTimestamp("detected_at").getTime()
                    ));
                }
            } catch (SQLException e) {
                plugin.log(Level.WARNING, "Failed to get recent detections: " + e.getMessage());
            }
            return records;
        });
    }

    /**
     * Close the database connection
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.log(Level.WARNING, "Failed to close VPN database: " + e.getMessage());
        }
    }

    // Inner classes for data

    public static class TrackedIP {
        public final String ip;
        public final boolean isVPN;
        public final long firstSeen;
        public final long lastSeen;
        public final int loginCount;

        public TrackedIP(String ip, boolean isVPN, long firstSeen, long lastSeen, int loginCount) {
            this.ip = ip;
            this.isVPN = isVPN;
            this.firstSeen = firstSeen;
            this.lastSeen = lastSeen;
            this.loginCount = loginCount;
        }
    }

    public static class VPNStats {
        public int totalDetections = 0;
        public int uniqueVPNIPs = 0;
        public int totalKicks = 0;
        public int totalWarnings = 0;
        public int detectionsToday = 0;
        public List<ProviderStat> topProviders = new ArrayList<>();
    }

    public static class ProviderStat {
        public final String name;
        public final int count;

        public ProviderStat(String name, int count) {
            this.name = name;
            this.count = count;
        }
    }

    public static class VPNDetectionRecord {
        public final String ip;
        public final String playerUUID;
        public final String playerName;
        public final String vpnProvider;
        public final String country;
        public final String actionTaken;
        public final long detectedAt;

        public VPNDetectionRecord(String ip, String playerUUID, String playerName, 
                                   String vpnProvider, String country, String actionTaken, long detectedAt) {
            this.ip = ip;
            this.playerUUID = playerUUID;
            this.playerName = playerName;
            this.vpnProvider = vpnProvider;
            this.country = country;
            this.actionTaken = actionTaken;
            this.detectedAt = detectedAt;
        }
    }
}
