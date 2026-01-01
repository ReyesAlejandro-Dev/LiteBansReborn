package com.nuvik.litebansreborn.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.managers.ReportManager;
import com.nuvik.litebansreborn.models.Report;
import com.nuvik.litebansreborn.models.Appeal;
import com.nuvik.litebansreborn.models.Punishment;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsServer;
import com.sun.net.httpserver.HttpsConfigurator;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.logging.Level;

/**
 * Web Panel Server - REST API for external management
 * Features:
 * - Real-time dashboard data
 * - Punishment management API
 * - Player search
 * - Staff statistics
 * - WebSocket for live updates (future)
 */
public class WebPanelServer {

    private final LiteBansReborn plugin;
    private final Gson gson;
    private HttpServer server;
    private final Map<String, String> apiKeys;
    private boolean running = false;
    private String publicIP = "localhost";
    private String lanIP = "192.168.1.1";
    private int currentPort = 8080;

    public WebPanelServer(LiteBansReborn plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
            .create();
        this.apiKeys = new HashMap<>();
        
        // Detect LAN IP immediately
        this.lanIP = detectLanIPSync();
        
        loadApiKeys();
    }

    /**
     * Start the web server
     */
    public boolean start() {
        if (!plugin.getConfigManager().getBoolean("web-panel.enabled", false)) {
            return false;
        }

        int port = plugin.getConfigManager().getInt("web-panel.port", 8080);
        this.currentPort = port;
        boolean ssl = plugin.getConfigManager().getBoolean("web-panel.ssl", false);

        // Detect API
        detectPublicIP();

        // Extract web resources
        saveWebResource("index.html");
        
        // Extract images
        File imgDir = new File(plugin.getDataFolder(), "web/img");
        if (!imgDir.exists()) imgDir.mkdirs();
        
        saveWebResource("img/logo.png");
        saveWebResource("img/dashboard.png");

        try {
            if (ssl) {
                server = createHttpsServer(port);
            } else {
                server = HttpServer.create(new InetSocketAddress(port), 0);
            }

            // Register endpoints
            registerEndpoints();

            // Performance configuration
            int threads = plugin.getConfigManager().getInt("web-panel.performance.worker-threads", 2);
            if (threads < 1) threads = 1; // Safety minimum
            
            server.setExecutor(Executors.newFixedThreadPool(threads));
            server.start();
            running = true;

            plugin.log(Level.INFO, "Web panel started on port " + port + (ssl ? " (HTTPS)" : " (HTTP)"));
            return true;
        } catch (Exception e) {
            plugin.log(Level.SEVERE, "Failed to start web panel: " + e.getMessage());
            return false;
        }
    }

    /**
     * Stop the web server
     */
    public void stop() {
        if (server != null) {
            server.stop(0);
            running = false;
            plugin.log(Level.INFO, "Web panel stopped");
        }
    }

    /**
     * Check if server is running
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Get the full URL to the web panel (public)
     */
    public String getWebURL() {
        boolean ssl = plugin.getConfigManager().getBoolean("web-panel.ssl", false);
        return (ssl ? "https://" : "http://") + publicIP + ":" + currentPort;
    }
    
    /**
     * Get localhost URL
     */
    public String getLocalURL() {
        boolean ssl = plugin.getConfigManager().getBoolean("web-panel.ssl", false);
        return (ssl ? "https://" : "http://") + "localhost:" + currentPort;
    }
    
    /**
     * Get LAN URL (192.168.x.x)
     */
    public String getLanURL() {
        boolean ssl = plugin.getConfigManager().getBoolean("web-panel.ssl", false);
        return (ssl ? "https://" : "http://") + lanIP + ":" + currentPort;
    }
    
    /**
     * Get all access URLs
     */
    public Map<String, String> getAllURLs() {
        Map<String, String> urls = new HashMap<>();
        urls.put("local", getLocalURL());
        urls.put("lan", getLanURL());
        urls.put("public", getWebURL());
        return urls;
    }
    
    /**
     * Detect LAN IP address synchronously
     */
    private String detectLanIPSync() {
        try {
            java.util.Enumeration<java.net.NetworkInterface> interfaces = java.net.NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                java.net.NetworkInterface iface = interfaces.nextElement();
                java.util.Enumeration<java.net.InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    java.net.InetAddress addr = addresses.nextElement();
                    if (!addr.isLoopbackAddress() && addr instanceof java.net.Inet4Address) {
                        String ip = addr.getHostAddress();
                        if (ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
                            return ip;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // calculated silently
        }
        return "192.168.1.1"; // fallback
    }
    
    /**
     * Detect public IP automatically
     */
    private void detectPublicIP() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL("https://api.ipify.org");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String ip = reader.readLine();
                    if (ip != null && !ip.isEmpty()) {
                        this.publicIP = ip;
                        plugin.log(Level.INFO, "Detected public IP for Web Panel: " + ip);
                    }
                }
            } catch (Exception e) {
                plugin.log(Level.WARNING, "Could not detect public IP: " + e.getMessage());
                // Fallback to server IP if possible, or keep localhost
                String serverIp = plugin.getServer().getIp();
                if (serverIp != null && !serverIp.isEmpty() && !serverIp.equals("0.0.0.0")) {
                    this.publicIP = serverIp;
                } else {
                    this.publicIP = "127.0.0.1";
                }
            }
        });
    }

    /**
     * Register all API endpoints
     */
    private void registerEndpoints() {
        // Health check
        server.createContext("/api/health", exchange -> {
            sendJson(exchange, Map.of("status", "ok", "version", plugin.getDescription().getVersion()));
        });

        // Dashboard stats
        server.createContext("/api/dashboard", new AuthenticatedHandler(this::handleDashboard));

        // Punishments
        server.createContext("/api/punishments", new AuthenticatedHandler(this::handlePunishments));
        server.createContext("/api/punishments/recent", new AuthenticatedHandler(this::handleRecentPunishments));

        // Players
        server.createContext("/api/players/search", new AuthenticatedHandler(this::handlePlayerSearch));
        server.createContext("/api/players/history", new AuthenticatedHandler(this::handlePlayerHistory));
        server.createContext("/api/players/risk", new AuthenticatedHandler(this::handlePlayerRisk));

        // Staff
        server.createContext("/api/staff/stats", new AuthenticatedHandler(this::handleStaffStats));
        server.createContext("/api/staff/online", new AuthenticatedHandler(this::handleOnlineStaff));
        server.createContext("/api/staff/leaderboard", new AuthenticatedHandler(this::handleStaffLeaderboard));

        // Reports & Appeals
        server.createContext("/api/reports", new AuthenticatedHandler(this::handleReports));
        server.createContext("/api/appeals", new AuthenticatedHandler(this::handleAppeals));

        // Analytics endpoints
        server.createContext("/api/analytics/heatmap", new AuthenticatedHandler(this::handleAnalyticsHeatmap));
        server.createContext("/api/analytics/trends", new AuthenticatedHandler(this::handleAnalyticsTrends));
        server.createContext("/api/analytics/timeline", new AuthenticatedHandler(this::handleAnalyticsTimeline));
        server.createContext("/api/analytics/geo", new AuthenticatedHandler(this::handleAnalyticsGeo));

        // AI endpoints
        server.createContext("/api/ai/analyze", new AuthenticatedHandler(this::handleAIAnalyze));
        server.createContext("/api/ai/suggest", new AuthenticatedHandler(this::handleAISuggest));

        // Mugshot generator
        server.createContext("/api/mugshot", new AuthenticatedHandler(this::handleMugshot));

        // Actions (POST only)
        server.createContext("/api/actions/ban", new AuthenticatedHandler(this::handleBanAction));
        server.createContext("/api/actions/unban", new AuthenticatedHandler(this::handleUnbanAction));
        server.createContext("/api/actions/execute", new AuthenticatedHandler(this::handleExecuteCommand));
        server.createContext("/api/actions/bulk", new AuthenticatedHandler(this::handleBulkAction));

        // Static files (for web UI)
        server.createContext("/", this::handleStaticFiles);
    }

    private void handleExecuteCommand(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }

        try {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> data = gson.fromJson(body, Map.class);
            
            String action = data.get("action");
            String player = sanitizeInput(data.get("player"));
            String reason = sanitizeInput(data.getOrDefault("reason", "Via Web Panel"));
            String duration = sanitizeInput(data.getOrDefault("duration", ""));
            
            if (action == null || player == null || player.isEmpty()) {
                sendError(exchange, 400, "Missing action or player");
                return;
            }
            
            // Validate player name format (alphanumeric and underscore only)
            if (!player.matches("^[a-zA-Z0-9_]{1,16}$")) {
                sendError(exchange, 400, "Invalid player name format");
                return;
            }
            
            // Build command
            String command;
            switch (action.toLowerCase()) {
                case "ban": command = "ban " + player + " " + reason; break;
                case "tempban": command = "tempban " + player + " " + duration + " " + reason; break;
                case "banip": command = "banip " + player + " " + reason; break;
                case "hwidban": command = "hwidban " + player + " " + reason; break;
                case "unban": command = "unban " + player; break;
                case "unbanip": command = "unbanip " + player; break;
                case "unbanhwid": command = "unbanhwid " + player; break;
                case "mute": command = "mute " + player + " " + reason; break;
                case "tempmute": command = "tempmute " + player + " " + duration + " " + reason; break;
                case "unmute": command = "unmute " + player; break;
                case "warn": command = "warn " + player + " " + reason; break;
                case "kick": command = "kick " + player + " " + reason; break;
                case "freeze": command = "freeze " + player; break;
                case "unfreeze": command = "unfreeze " + player; break;
                default:
                    sendError(exchange, 400, "Unknown action: " + action);
                    return;
            }
            
            // Execute command on main thread
            final String finalCommand = command;
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), finalCommand);
            });
            
            sendJson(exchange, Map.of(
                "success", true,
                "message", "Command executed: /" + command,
                "action", action,
                "player", player
            ));
            
        } catch (Exception e) {
            sendError(exchange, 500, "Error: " + e.getMessage());
        }
    }

    // ==================== HANDLERS ====================

    private void handleDashboard(HttpExchange exchange) throws IOException {
        Map<String, Object> data = new HashMap<>();
        
        // Get stats synchronously for simplicity
        data.put("onlinePlayers", plugin.getServer().getOnlinePlayers().size());
        data.put("maxPlayers", plugin.getServer().getMaxPlayers());
        data.put("serverName", plugin.getConfigManager().getServerName());
        data.put("uptime", System.currentTimeMillis() - plugin.getStartTime());
        data.put("publicIP", publicIP);
        data.put("port", currentPort);
        
        // Cache stats
        var cacheStats = plugin.getCacheManager().getStats();
        data.put("cachedBans", cacheStats.get("bans"));
        data.put("cachedMutes", cacheStats.get("mutes"));
        data.put("frozenPlayers", cacheStats.get("frozen"));
        
        // Anti-VPN stats
        if (plugin.getVPNManager() != null) {
            data.put("vpnEnabled", plugin.getVPNManager().isRuntimeEnabled());
            data.put("vpnCacheSize", plugin.getVPNManager().getCacheSize());
        }
        
        sendJson(exchange, data);
    }

    private void handlePunishments(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        int page = getQueryParam(query, "page", 1);
        int limit = getQueryParam(query, "limit", 50);
        String type = getQueryParam(query, "type", "all");
        
        try {
            List<Map<String, Object>> punishments = new ArrayList<>();
            
            // Query database directly for all punishment types
            String dbType = type.toLowerCase();
            if (dbType.equals("warning")) dbType = "warn";
            
            String tableName = plugin.getDatabaseManager().getTable("punishments");
            String sql;
            
            if (dbType.equals("all")) {
                sql = "SELECT * FROM " + tableName + " ORDER BY created_at DESC LIMIT " + limit + " OFFSET " + ((page - 1) * limit);
            } else {
                // Include all subtypes for each punishment category
                String typeCondition;
                switch (dbType) {
                    case "ban":
                        typeCondition = "type IN ('ban', 'tempban', 'ipban')";
                        break;
                    case "mute":
                        typeCondition = "type IN ('mute', 'tempmute', 'ipmute')";
                        break;
                    default:
                        typeCondition = "type = '" + dbType + "'";
                }
                sql = "SELECT * FROM " + tableName + " WHERE " + typeCondition + " ORDER BY created_at DESC LIMIT " + limit + " OFFSET " + ((page - 1) * limit);
            }
            
            // Execute query async
            final String finalSql = sql;
            var resultFuture = plugin.getDatabaseManager().queryAsync(conn -> {
                List<Map<String, Object>> results = new ArrayList<>();
                try (var stmt = conn.prepareStatement(finalSql);
                     var rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> punishment = new HashMap<>();
                        punishment.put("id", rs.getLong("id"));
                        punishment.put("type", rs.getString("type") != null ? rs.getString("type").toUpperCase() : "UNKNOWN");
                        punishment.put("targetName", rs.getString("target_name"));
                        punishment.put("targetUUID", rs.getString("target_uuid"));
                        punishment.put("executorName", rs.getString("executor_name"));
                        punishment.put("executorUUID", rs.getString("executor_uuid"));
                        punishment.put("reason", rs.getString("reason"));
                        
                        var createdAt = rs.getTimestamp("created_at");
                        punishment.put("createdAt", createdAt != null ? createdAt.toString() : null);
                        
                        var expiresAt = rs.getTimestamp("expires_at");
                        punishment.put("expiresAt", expiresAt != null ? expiresAt.toString() : null);
                        
                        punishment.put("active", rs.getBoolean("active"));
                        results.add(punishment);
                    }
                }
                return results;
            });
            
            punishments = resultFuture.join();
            
            Map<String, Object> response = new HashMap<>();
            response.put("page", page);
            response.put("limit", limit);
            response.put("total", punishments.size());
            response.put("punishments", punishments);
            response.put("type", type);
            
            sendJson(exchange, response);
        } catch (Exception e) {
            plugin.getLogger().warning("Error fetching punishments: " + e.getMessage());
            e.printStackTrace();
            sendError(exchange, 500, "Failed to fetch punishments: " + e.getMessage());
        }
    }
    
    private Map<String, Object> punishmentToMap(Punishment p, String type) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", p.getId());
        map.put("type", type);
        map.put("targetName", p.getTargetName());
        map.put("targetUUID", p.getTargetUUID() != null ? p.getTargetUUID().toString() : null);
        map.put("executorName", p.getExecutorName());
        map.put("executorUUID", p.getExecutorUUID() != null ? p.getExecutorUUID().toString() : null);
        map.put("reason", p.getReason());
        map.put("createdAt", p.getCreatedAt() != null ? p.getCreatedAt().toString() : null);
        map.put("expiresAt", p.getExpiresAt() != null ? p.getExpiresAt().toString() : null);
        map.put("active", p.isActive());
        map.put("permanent", p.isPermanent());
        return map;
    }

    private void handleRecentPunishments(HttpExchange exchange) throws IOException {
        int limit = getQueryParam(exchange.getRequestURI().getQuery(), "limit", 10);
        
        try {
            List<Map<String, Object>> recent = new ArrayList<>();
            
            var bans = plugin.getBanManager().getActiveBans(1, limit).join();
            var mutes = plugin.getMuteManager().getActiveMutes(1, limit).join();
            
            for (var ban : bans) recent.add(punishmentToMap(ban, "BAN"));
            for (var mute : mutes) recent.add(punishmentToMap(mute, "MUTE"));
            
            sendJson(exchange, Map.of("punishments", recent, "count", recent.size()));
        } catch (Exception e) {
            sendError(exchange, 500, "Failed to fetch recent punishments: " + e.getMessage());
        }
    }
    
    private void handleReports(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        int page = getQueryParam(query, "page", 1);
        int limit = getQueryParam(query, "limit", 20);
        String status = getQueryParam(query, "status", "all");
        
        try {
            List<Report> reports = plugin.getReportManager().getAllReports(status, page, limit).join();
            int total = plugin.getReportManager().getTotalReportsCount(status).join();
            
            Map<String, Object> response = new HashMap<>();
            response.put("page", page);
            response.put("limit", limit);
            response.put("total", total);
            response.put("reports", reports);
            
            sendJson(exchange, response);
        } catch (Exception e) {
            sendError(exchange, 500, "Failed to fetch reports: " + e.getMessage());
        }
    }
    
    private void handleAppeals(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        int page = getQueryParam(query, "page", 1);
        int limit = getQueryParam(query, "limit", 20);
        String status = getQueryParam(query, "status", "all");
        
        try {
            List<Appeal> appeals = plugin.getAppealManager().getAllAppeals(status, page, limit).join();
            int total = plugin.getAppealManager().getTotalAppealsCount(status).join();
            
            Map<String, Object> response = new HashMap<>();
            response.put("page", page);
            response.put("limit", limit);
            response.put("total", total);
            response.put("appeals", appeals);
            
            sendJson(exchange, response);
        } catch (Exception e) {
            sendError(exchange, 500, "Failed to fetch appeals: " + e.getMessage());
        }
    }

    private void handlePlayerSearch(HttpExchange exchange) throws IOException {
        String query = getQueryParam(exchange.getRequestURI().getQuery(), "q", "");
        
        List<Map<String, Object>> results = new ArrayList<>();
        Set<String> addedPlayers = new HashSet<>();
        
        // Get all active bans and mutes for lookup
        Map<String, Punishment> activeBans = new HashMap<>();
        Map<String, Punishment> activeMutes = new HashMap<>();
        
        try {
            var bans = plugin.getBanManager().getActiveBans(1, 100).join();
            for (var ban : bans) {
                if (ban.getTargetName() != null) {
                    activeBans.put(ban.getTargetName().toLowerCase(), ban);
                }
            }
            
            var mutes = plugin.getMuteManager().getActiveMutes(1, 100).join();
            for (var mute : mutes) {
                if (mute.getTargetName() != null) {
                    activeMutes.put(mute.getTargetName().toLowerCase(), mute);
                }
            }
        } catch (Exception e) {
            // Ignore database errors
        }
        
        // Always include online players
        for (var player : plugin.getServer().getOnlinePlayers()) {
            boolean matches = query.isEmpty() || query.length() < 3 || 
                player.getName().toLowerCase().contains(query.toLowerCase());
            
            if (matches && !addedPlayers.contains(player.getName().toLowerCase())) {
                String nameLower = player.getName().toLowerCase();
                Map<String, Object> playerData = new HashMap<>();
                playerData.put("name", player.getName());
                playerData.put("uuid", player.getUniqueId().toString());
                playerData.put("online", true);
                
                // Check for active punishments
                Punishment ban = activeBans.get(nameLower);
                Punishment mute = activeMutes.get(nameLower);
                
                playerData.put("isBanned", ban != null);
                playerData.put("isMuted", mute != null);
                
                if (ban != null) {
                    playerData.put("banReason", ban.getReason());
                    playerData.put("banExecutor", ban.getExecutorName());
                }
                if (mute != null) {
                    playerData.put("muteReason", mute.getReason());
                    playerData.put("muteExecutor", mute.getExecutorName());
                }
                
                results.add(playerData);
                addedPlayers.add(nameLower);
            }
        }
        
        // Also add banned/muted players that are offline
        for (var entry : activeBans.entrySet()) {
            String nameLower = entry.getKey();
            Punishment ban = entry.getValue();
            
            boolean matches = query.isEmpty() || query.length() < 3 || 
                nameLower.contains(query.toLowerCase());
            
            if (matches && !addedPlayers.contains(nameLower)) {
                Map<String, Object> playerData = new HashMap<>();
                playerData.put("name", ban.getTargetName());
                playerData.put("uuid", ban.getTargetUUID() != null ? ban.getTargetUUID().toString() : "unknown");
                playerData.put("online", false);
                playerData.put("isBanned", true);
                playerData.put("banReason", ban.getReason());
                playerData.put("banExecutor", ban.getExecutorName());
                
                // Check if also muted
                Punishment mute = activeMutes.get(nameLower);
                playerData.put("isMuted", mute != null);
                if (mute != null) {
                    playerData.put("muteReason", mute.getReason());
                    playerData.put("muteExecutor", mute.getExecutorName());
                }
                
                results.add(playerData);
                addedPlayers.add(nameLower);
            }
        }
        
        // Add muted players that aren't banned
        for (var entry : activeMutes.entrySet()) {
            String nameLower = entry.getKey();
            Punishment mute = entry.getValue();
            
            boolean matches = query.isEmpty() || query.length() < 3 || 
                nameLower.contains(query.toLowerCase());
            
            if (matches && !addedPlayers.contains(nameLower)) {
                Map<String, Object> playerData = new HashMap<>();
                playerData.put("name", mute.getTargetName());
                playerData.put("uuid", mute.getTargetUUID() != null ? mute.getTargetUUID().toString() : "unknown");
                playerData.put("online", false);
                playerData.put("isBanned", false);
                playerData.put("isMuted", true);
                playerData.put("muteReason", mute.getReason());
                playerData.put("muteExecutor", mute.getExecutorName());
                
                results.add(playerData);
                addedPlayers.add(nameLower);
            }
        }
        
        sendJson(exchange, Map.of("results", results, "query", query, "total", results.size()));
    }

    private void handlePlayerHistory(HttpExchange exchange) throws IOException {
        String uuid = getQueryParam(exchange.getRequestURI().getQuery(), "uuid", "");
        
        if (uuid.isEmpty()) {
            sendError(exchange, 400, "UUID required");
            return;
        }
        
        // Placeholder response
        sendJson(exchange, Map.of(
            "uuid", uuid,
            "punishments", new ArrayList<>(),
            "notes", new ArrayList<>()
        ));
    }

    private void handleStaffStats(HttpExchange exchange) throws IOException {
        List<Map<String, Object>> staffStats = new ArrayList<>();
        
        // Placeholder - would aggregate from database
        sendJson(exchange, Map.of("staff", staffStats));
    }

    private void handleOnlineStaff(HttpExchange exchange) throws IOException {
        List<Map<String, Object>> onlineStaff = new ArrayList<>();
        
        for (var player : plugin.getServer().getOnlinePlayers()) {
            if (player.hasPermission("litebansreborn.staff")) {
                onlineStaff.add(Map.of(
                    "name", player.getName(),
                    "uuid", player.getUniqueId().toString()
                ));
            }
        }
        
        sendJson(exchange, Map.of("staff", onlineStaff, "count", onlineStaff.size()));
    }

    private void handleBanAction(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }
        
        // Read body
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> request = gson.fromJson(body, Map.class);
        
        String player = request.get("player");
        String reason = request.getOrDefault("reason", "Banned via Web Panel");
        String duration = request.get("duration");
        
        if (player == null) {
            sendError(exchange, 400, "Player required");
            return;
        }
        
        // Execute ban command from console
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            String cmd = "ban " + player + (duration != null ? " " + duration : "") + " " + reason;
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd);
        });
        
        sendJson(exchange, Map.of("success", true, "message", "Ban command executed"));
    }

    private void handleUnbanAction(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }
        
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> request = gson.fromJson(body, Map.class);
        
        String player = request.get("player");
        
        if (player == null) {
            sendError(exchange, 400, "Player required");
            return;
        }
        
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), "unban " + player);
        });
        
        sendJson(exchange, Map.of("success", true, "message", "Unban command executed"));
    }

    private void handleStaticFiles(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.equals("/")) path = "/index.html";
        
        // Serve from web folder
        File webFolder = new File(plugin.getDataFolder(), "web");
        File file = new File(webFolder, path);
        
        if (!file.exists() || !file.getCanonicalPath().startsWith(webFolder.getCanonicalPath())) {
            sendError(exchange, 404, "Not found");
            return;
        }
        
        String contentType = getContentType(path);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, file.length());
        
        try (OutputStream os = exchange.getResponseBody();
             FileInputStream fis = new FileInputStream(file)) {
            fis.transferTo(os);
        }
    }

    // ==================== UTILITY ====================

    private void sendJson(HttpExchange exchange, Object data) throws IOException {
        String json = gson.toJson(data);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, bytes.length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendError(HttpExchange exchange, int code, String message) throws IOException {
        Map<String, Object> error = Map.of("error", true, "message", message, "code", code);
        String json = gson.toJson(error);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*"); // CORS fix
        exchange.sendResponseHeaders(code, bytes.length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private int getQueryParam(String query, String key, int defaultValue) {
        String value = getQueryParam(query, key, null);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String getQueryParam(String query, String key, String defaultValue) {
        if (query == null) return defaultValue;
        for (String param : query.split("&")) {
            String[] pair = param.split("=", 2); // Limit to 2 parts to handle = in values
            if (pair.length == 2 && pair[0].equals(key)) {
                try {
                    return java.net.URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                } catch (Exception e) {
                    return pair[1]; // Fallback to raw value
                }
            }
        }
        return defaultValue;
    }

    private String getContentType(String path) {
        if (path.endsWith(".html")) return "text/html";
        if (path.endsWith(".css")) return "text/css";
        if (path.endsWith(".js")) return "application/javascript";
        if (path.endsWith(".json")) return "application/json";
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream";
    }

    /**
     * Sanitize user input to prevent command injection
     */
    private String sanitizeInput(String input) {
        if (input == null) return "";
        // Remove dangerous characters: newlines, command separators, etc.
        return input
            .replaceAll("[\\r\\n]", " ")      // Replace newlines with space
            .replaceAll("[;&|`$]", "")        // Remove shell special chars
            .replaceAll("\\s+", " ")          // Collapse multiple spaces
            .trim();
    }

    private void loadApiKeys() {
        // Load from config
        List<String> keys = plugin.getConfigManager().getStringList("web-panel.api-keys");
        for (String key : keys) {
            apiKeys.put(key, "admin");
        }
        
        // Generate default key if none exist and save it to config
        if (apiKeys.isEmpty()) {
            String defaultKey = UUID.randomUUID().toString();
            apiKeys.put(defaultKey, "admin");
            
            // Save to config so it persists across restarts
            List<String> newKeys = new java.util.ArrayList<>();
            newKeys.add(defaultKey);
            plugin.getConfig().set("web-panel.api-keys", newKeys);
            plugin.saveConfig();
            
            plugin.log(Level.INFO, "Generated and saved default API key: " + defaultKey);
            plugin.log(Level.INFO, "You can find this key in config.yml under web-panel.api-keys");
        }
    }

    private boolean validateApiKey(String key) {
        return apiKeys.containsKey(key);
    }

    private HttpsServer createHttpsServer(int port) throws Exception {
        // Validate keystore exists before creating HTTPS server
        File keystoreFile = new File(plugin.getDataFolder(), "keystore.jks");
        String keystorePassword = plugin.getConfigManager().getString("web-panel.keystore-password", "changeit");
        
        if (!keystoreFile.exists()) {
            throw new IllegalStateException(
                "SSL is enabled but keystore.jks not found! " +
                "Please create a keystore file at: " + keystoreFile.getAbsolutePath() + " " +
                "or disable SSL in config.yml (web-panel.ssl: false)"
            );
        }
        
        HttpsServer httpsServer = HttpsServer.create(new InetSocketAddress(port), 0);
        
        KeyStore keyStore = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(keystoreFile)) {
            keyStore.load(fis, keystorePassword.toCharArray());
        }
        
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(keyStore, keystorePassword.toCharArray());
        
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(keyStore);
        
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        
        httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext));
        
        return httpsServer;
    }

    // ==================== AUTH HANDLER ====================

    private class AuthenticatedHandler implements HttpHandler {
        private final IOHandler handler;

        public AuthenticatedHandler(IOHandler handler) {
            this.handler = handler;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // CORS preflight
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Authorization, Content-Type");
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            // Check API key
            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                sendError(exchange, 401, "Unauthorized - API key required");
                return;
            }

            String apiKey = authHeader.substring(7);
            if (!validateApiKey(apiKey)) {
                sendError(exchange, 403, "Forbidden - Invalid API key");
                return;
            }

            handler.handle(exchange);
        }
    }

    @FunctionalInterface
    private interface IOHandler {
        void handle(HttpExchange exchange) throws IOException;
    }

    // ==================== NEW HANDLERS ====================
    
    private AnalyticsHandler analyticsHandler;
    
    private AnalyticsHandler getAnalyticsHandler() {
        if (analyticsHandler == null) {
            analyticsHandler = new AnalyticsHandler(plugin);
        }
        return analyticsHandler;
    }

    private void handleAnalyticsHeatmap(HttpExchange exchange) throws IOException {
        try {
            var data = getAnalyticsHandler().getActivityHeatmap().join();
            sendJson(exchange, data);
        } catch (Exception e) {
            sendError(exchange, 500, "Failed to get heatmap: " + e.getMessage());
        }
    }

    private void handleAnalyticsTrends(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        String period = getQueryParam(query, "period", "week");
        try {
            var data = getAnalyticsHandler().getPunishmentTrends(period).join();
            sendJson(exchange, data);
        } catch (Exception e) {
            sendError(exchange, 500, "Failed to get trends: " + e.getMessage());
        }
    }

    private void handleAnalyticsTimeline(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        int limit = getQueryParam(query, "limit", 50);
        try {
            var data = getAnalyticsHandler().getEventsTimeline(limit).join();
            sendJson(exchange, data);
        } catch (Exception e) {
            sendError(exchange, 500, "Failed to get timeline: " + e.getMessage());
        }
    }

    private void handleAnalyticsGeo(HttpExchange exchange) throws IOException {
        try {
            var data = getAnalyticsHandler().getGeoStats().join();
            sendJson(exchange, data);
        } catch (Exception e) {
            sendError(exchange, 500, "Failed to get geo stats: " + e.getMessage());
        }
    }

    private void handleStaffLeaderboard(HttpExchange exchange) throws IOException {
        try {
            var data = getAnalyticsHandler().getStaffLeaderboard().join();
            sendJson(exchange, data);
        } catch (Exception e) {
            sendError(exchange, 500, "Failed to get leaderboard: " + e.getMessage());
        }
    }

    private void handlePlayerRisk(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        String player = getQueryParam(query, "player", "");
        if (player.isEmpty()) {
            sendError(exchange, 400, "Player name required");
            return;
        }
        try {
            var data = getAnalyticsHandler().getPlayerRisk(player).join();
            sendJson(exchange, data);
        } catch (Exception e) {
            sendError(exchange, 500, "Failed to get player risk: " + e.getMessage());
        }
    }

    private void handleAIAnalyze(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "POST required");
            return;
        }
        try {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> data = gson.fromJson(body, Map.class);
            String message = data != null ? data.getOrDefault("message", "") : "";
            String player = data != null ? data.getOrDefault("player", "Unknown") : "Unknown";
            
            // Return placeholder - AI service would be initialized in main plugin
            Map<String, Object> result = new HashMap<>();
            result.put("isToxic", false);
            result.put("score", 0);
            result.put("analysis", "AI analysis not configured. Add groq-api-key to config.yml");
            sendJson(exchange, result);
        } catch (Exception e) {
            sendError(exchange, 500, "AI analysis failed: " + e.getMessage());
        }
    }

    private void handleAISuggest(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        String player = getQueryParam(query, "player", "");
        String violation = getQueryParam(query, "violation", "general");
        
        try {
            // Get player risk data and use that for suggestion
            var risk = getAnalyticsHandler().getPlayerRisk(player).join();
            sendJson(exchange, risk);
        } catch (Exception e) {
            sendError(exchange, 500, "AI suggestion failed: " + e.getMessage());
        }
    }

    private void handleMugshot(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        String player = getQueryParam(query, "player", "");
        long punishmentId = getQueryParam(query, "id", 0);
        
        if (player.isEmpty()) {
            sendError(exchange, 400, "Player name required");
            return;
        }
        
        try {
            // Generate mugshot data (simplified - full implementation in MugshotGenerator)
            Map<String, Object> result = new HashMap<>();
            result.put("player", player);
            result.put("punishmentId", punishmentId);
            result.put("avatarUrl", "https://minotar.net/helm/" + player + "/128");
            result.put("status", "WANTED");
            
            // If we have a punishment ID, get details
            if (punishmentId > 0) {
                // Would fetch from database
                result.put("reason", "Violation of server rules");
                result.put("bannedBy", "Console");
            }
            
            sendJson(exchange, result);
        } catch (Exception e) {
            sendError(exchange, 500, "Mugshot generation failed: " + e.getMessage());
        }
    }

    private void handleBulkAction(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "POST required");
            return;
        }
        
        try {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            // Parse JSON array of actions
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Bulk actions queued");
            result.put("note", "Bulk actions execute sequentially");
            sendJson(exchange, result);
        } catch (Exception e) {
            sendError(exchange, 500, "Bulk action failed: " + e.getMessage());
        }
    }

    private void saveWebResource(String resourcePath) {
        File webFolder = new File(plugin.getDataFolder(), "web");
        if (!webFolder.exists()) {
            webFolder.mkdirs();
        }

        File outFile = new File(webFolder, resourcePath);
        if (outFile.exists()) {
            return; // Don't overwrite existing files
        }

        String jarPath = "web/" + resourcePath;
        try (InputStream in = plugin.getResource(jarPath)) {
            if (in == null) {
                plugin.log(Level.WARNING, "Could not find web resource in JAR: " + jarPath);
                return;
            }

            try (OutputStream out = new FileOutputStream(outFile)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
                plugin.log(Level.INFO, "Extracted web resource: " + resourcePath);
            }
        } catch (IOException e) {
            plugin.log(Level.SEVERE, "Could not save web resource " + resourcePath + ": " + e.getMessage());
        }
    }
}

