package com.nuvik.litebansreborn.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nuvik.litebansreborn.LiteBansReborn;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsServer;
import com.sun.net.httpserver.HttpsConfigurator;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.net.InetSocketAddress;
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

    public WebPanelServer(LiteBansReborn plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
            .create();
        this.apiKeys = new HashMap<>();
        
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
        boolean ssl = plugin.getConfigManager().getBoolean("web-panel.ssl", false);

        // Extract web resources
        saveWebResource("index.html");

        try {
            if (ssl) {
                server = createHttpsServer(port);
            } else {
                server = HttpServer.create(new InetSocketAddress(port), 0);
            }

            // Register endpoints
            registerEndpoints();

            server.setExecutor(Executors.newFixedThreadPool(4));
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

        // Staff
        server.createContext("/api/staff/stats", new AuthenticatedHandler(this::handleStaffStats));
        server.createContext("/api/staff/online", new AuthenticatedHandler(this::handleOnlineStaff));

        // Actions (POST only)
        server.createContext("/api/actions/ban", new AuthenticatedHandler(this::handleBanAction));
        server.createContext("/api/actions/unban", new AuthenticatedHandler(this::handleUnbanAction));

        // Static files (for web UI)
        server.createContext("/", this::handleStaticFiles);
    }

    // ==================== HANDLERS ====================

    private void handleDashboard(HttpExchange exchange) throws IOException {
        Map<String, Object> data = new HashMap<>();
        
        // Get stats synchronously for simplicity
        data.put("onlinePlayers", plugin.getServer().getOnlinePlayers().size());
        data.put("maxPlayers", plugin.getServer().getMaxPlayers());
        data.put("serverName", plugin.getConfigManager().getServerName());
        data.put("uptime", System.currentTimeMillis() - plugin.getStartTime());
        
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
        int limit = getQueryParam(query, "limit", 20);
        String type = getQueryParam(query, "type", "all");
        
        // Return placeholder - actual implementation would query database
        Map<String, Object> response = new HashMap<>();
        response.put("page", page);
        response.put("limit", limit);
        response.put("total", 0);
        response.put("punishments", new ArrayList<>());
        
        sendJson(exchange, response);
    }

    private void handleRecentPunishments(HttpExchange exchange) throws IOException {
        int limit = getQueryParam(exchange.getRequestURI().getQuery(), "limit", 10);
        
        List<Map<String, Object>> recent = new ArrayList<>();
        // Placeholder - would fetch from database
        
        sendJson(exchange, Map.of("punishments", recent, "count", recent.size()));
    }

    private void handlePlayerSearch(HttpExchange exchange) throws IOException {
        String query = getQueryParam(exchange.getRequestURI().getQuery(), "q", "");
        
        if (query.length() < 3) {
            sendError(exchange, 400, "Query must be at least 3 characters");
            return;
        }
        
        List<Map<String, Object>> results = new ArrayList<>();
        
        // Search online players
        for (var player : plugin.getServer().getOnlinePlayers()) {
            if (player.getName().toLowerCase().contains(query.toLowerCase())) {
                results.add(Map.of(
                    "name", player.getName(),
                    "uuid", player.getUniqueId().toString(),
                    "online", true
                ));
            }
        }
        
        sendJson(exchange, Map.of("results", results, "query", query));
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
            String[] pair = param.split("=");
            if (pair.length == 2 && pair[0].equals(key)) {
                return pair[1];
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

    private void loadApiKeys() {
        // Load from config
        List<String> keys = plugin.getConfigManager().getStringList("web-panel.api-keys");
        for (String key : keys) {
            apiKeys.put(key, "admin");
        }
        
        // Generate default key if none exist
        if (apiKeys.isEmpty()) {
            String defaultKey = UUID.randomUUID().toString();
            apiKeys.put(defaultKey, "admin");
            plugin.log(Level.INFO, "Generated default API key: " + defaultKey);
        }
    }

    private boolean validateApiKey(String key) {
        return apiKeys.containsKey(key);
    }

    private HttpsServer createHttpsServer(int port) throws Exception {
        HttpsServer httpsServer = HttpsServer.create(new InetSocketAddress(port), 0);
        
        // Load SSL certificate
        File keystoreFile = new File(plugin.getDataFolder(), "keystore.jks");
        String keystorePassword = plugin.getConfigManager().getString("web-panel.keystore-password", "changeit");
        
        if (keystoreFile.exists()) {
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
        }
        
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
