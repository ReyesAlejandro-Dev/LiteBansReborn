package com.nuvik.litebansreborn.antivpn;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nuvik.litebansreborn.LiteBansReborn;
import okhttp3.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * VPN Manager - Handles VPN/Proxy detection using multiple API providers
 * Features:
 * - Rotational API system
 * - Multiple provider support
 * - Caching
 * - Real IP detection
 * - SQLite logging
 */
public class VPNManager {

    private final LiteBansReborn plugin;
    private final OkHttpClient httpClient;
    private final VPNDatabase database;
    private final Map<String, VPNResult> cache;
    private final Set<String> whitelistedIPs;
    private final Set<String> whitelistedCountries;
    
    // API providers in order of priority
    private final List<VPNAPIProvider> providers;
    private int currentProviderIndex = 0;
    private final Object providerLock = new Object();

    public enum VPNAction {
        KICK,       // Kick the player
        WARN,       // Only warn staff
        ALLOW,      // Allow but log
        NONE        // Do nothing
    }

    public VPNManager(LiteBansReborn plugin) {
        this.plugin = plugin;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
        
        this.database = new VPNDatabase(plugin);
        this.cache = new ConcurrentHashMap<>();
        this.whitelistedIPs = ConcurrentHashMap.newKeySet();
        this.whitelistedCountries = ConcurrentHashMap.newKeySet();
        this.providers = new ArrayList<>();

        // Initialize database
        database.initialize();

        // Load configuration
        loadConfig();
    }

    /**
     * Load configuration from config.yml
     */
    private void loadConfig() {
        // Load whitelisted IPs
        whitelistedIPs.addAll(plugin.getConfigManager().getStringList("anti-vpn.whitelist.ips"));
        
        // Load whitelisted countries
        whitelistedCountries.addAll(plugin.getConfigManager().getStringList("anti-vpn.whitelist.countries"));

        // Initialize providers based on config
        initializeProviders();
    }

    /**
     * Initialize API providers from config
     */
    private void initializeProviders() {
        providers.clear();

        List<String> enabledProviders = plugin.getConfigManager().getStringList("anti-vpn.providers");
        if (enabledProviders.isEmpty()) {
            // Default providers
            enabledProviders = Arrays.asList("proxycheck", "ip-api", "vpnapi", "iphub");
        }

        for (String provider : enabledProviders) {
            switch (provider.toLowerCase()) {
                case "proxycheck" -> providers.add(new ProxyCheckProvider());
                case "ip-api" -> providers.add(new IPAPIProvider());
                case "vpnapi" -> providers.add(new VPNAPIProvider_VPNAPI());
                case "iphub" -> providers.add(new IPHubProvider());
                case "iphunter" -> providers.add(new IPHunterProvider());
                case "ipqualityscore" -> providers.add(new IPQualityScoreProvider());
            }
        }

        if (providers.isEmpty()) {
            providers.add(new IPAPIProvider()); // Fallback
        }

        plugin.log(Level.INFO, "§aAnti-VPN initialized with " + providers.size() + " providers");
    }

    /**
     * Check if Anti-VPN is enabled
     */
    public boolean isEnabled() {
        return plugin.getConfigManager().getBoolean("anti-vpn.enabled", false);
    }

    /**
     * Get the action to take when VPN is detected
     */
    public VPNAction getAction() {
        String action = plugin.getConfigManager().getString("anti-vpn.action", "warn").toUpperCase();
        try {
            return VPNAction.valueOf(action);
        } catch (IllegalArgumentException e) {
            return VPNAction.WARN;
        }
    }

    /**
     * Toggle Anti-VPN on/off in runtime (persists until reload)
     */
    private boolean runtimeEnabled = true;
    private boolean alertsEnabled = true;
    private VPNAction runtimeAction = null;

    public void setEnabled(boolean enabled) {
        this.runtimeEnabled = enabled;
    }

    public void setAlertsEnabled(boolean enabled) {
        this.alertsEnabled = enabled;
    }

    public boolean areAlertsEnabled() {
        return alertsEnabled && plugin.getConfigManager().getBoolean("anti-vpn.alerts", true);
    }

    public void setAction(VPNAction action) {
        this.runtimeAction = action;
    }

    public VPNAction getEffectiveAction() {
        return runtimeAction != null ? runtimeAction : getAction();
    }

    public boolean isRuntimeEnabled() {
        return runtimeEnabled && plugin.getConfigManager().getBoolean("anti-vpn.enabled", false);
    }

    public Set<String> getWhitelistedIPs() {
        return new HashSet<>(whitelistedIPs);
    }

    public int getCacheSize() {
        return cache.size();
    }

    public int getProviderCount() {
        return providers.size();
    }

    /**
     * Check if an IP is a VPN/Proxy
     */
    public CompletableFuture<VPNResult> checkIP(String ip) {
        return CompletableFuture.supplyAsync(() -> {
            // Check if IP is whitelisted
            if (isWhitelisted(ip)) {
                return VPNResult.clean(ip, "whitelisted");
            }

            // Check cache first
            VPNResult cached = cache.get(ip);
            if (cached != null && !isCacheExpired(cached)) {
                plugin.debug("VPN check for " + ip + " returned from cache");
                return cached;
            }

            // Check local database for known VPNs (fast path)
            try {
                if (database.isKnownVPN(ip).get()) {
                    plugin.debug("IP " + ip + " found in local VPN database");
                    // Still do full check but we know it's likely a VPN
                }
            } catch (Exception e) {
                // Ignore
            }

            // Query API providers
            VPNResult result = queryProviders(ip);

            // Cache the result
            if (result != null) {
                cache.put(ip, result);
            }

            return result != null ? result : VPNResult.unknown(ip);
        });
    }

    /**
     * Query providers with rotation/fallback
     */
    private VPNResult queryProviders(String ip) {
        int attempts = 0;
        int maxAttempts = providers.size();

        while (attempts < maxAttempts) {
            VPNAPIProvider provider;
            synchronized (providerLock) {
                provider = providers.get(currentProviderIndex);
            }

            try {
                VPNResult result = provider.check(ip);
                if (result != null) {
                    plugin.debug("VPN check successful using " + provider.getName());
                    return result;
                }
            } catch (Exception e) {
                plugin.debug("VPN provider " + provider.getName() + " failed: " + e.getMessage());
            }

            // Rotate to next provider
            synchronized (providerLock) {
                currentProviderIndex = (currentProviderIndex + 1) % providers.size();
            }
            attempts++;
        }

        plugin.log(Level.WARNING, "All VPN providers failed for IP: " + ip);
        return null;
    }

    /**
     * Check if IP is whitelisted
     */
    private boolean isWhitelisted(String ip) {
        return whitelistedIPs.contains(ip) || ip.equals("127.0.0.1") || ip.startsWith("192.168.") || ip.startsWith("10.");
    }

    /**
     * Check if cache entry is expired
     */
    private boolean isCacheExpired(VPNResult result) {
        long cacheMinutes = plugin.getConfigManager().getInt("anti-vpn.cache-duration", 60);
        return System.currentTimeMillis() - result.getCheckTime() > TimeUnit.MINUTES.toMillis(cacheMinutes);
    }

    /**
     * Log a VPN detection to database
     */
    public void logDetection(VPNResult result, java.util.UUID playerUUID, String playerName, VPNAction action) {
        database.logDetection(result, playerUUID, playerName, action.name());
        database.trackIP(playerUUID, playerName, result.getIp(), result.isDangerous());
    }

    /**
     * Get likely real IP for a player (based on history)
     */
    public CompletableFuture<String> getLikelyRealIP(java.util.UUID playerUUID) {
        return database.getLikelyRealIP(playerUUID);
    }

    /**
     * Get all IPs used by a player
     */
    public CompletableFuture<List<VPNDatabase.TrackedIP>> getPlayerIPs(java.util.UUID playerUUID) {
        return database.getPlayerIPs(playerUUID);
    }

    /**
     * Get VPN statistics
     */
    public CompletableFuture<VPNDatabase.VPNStats> getStats() {
        return database.getStats();
    }

    /**
     * Get recent detections
     */
    public CompletableFuture<List<VPNDatabase.VPNDetectionRecord>> getRecentDetections(int limit) {
        return database.getRecentDetections(limit);
    }

    /**
     * Whitelist an IP
     */
    public void whitelistIP(String ip) {
        whitelistedIPs.add(ip);
        cache.remove(ip);
    }

    /**
     * Remove IP from whitelist
     */
    public void unwhitelistIP(String ip) {
        whitelistedIPs.remove(ip);
    }

    /**
     * Clear the cache
     */
    public void clearCache() {
        cache.clear();
    }

    /**
     * Get database instance
     */
    public VPNDatabase getDatabase() {
        return database;
    }

    /**
     * Shutdown the manager
     */
    public void shutdown() {
        database.close();
        cache.clear();
    }

    // ==================== API Provider Implementations ====================

    /**
     * Base interface for VPN API providers
     */
    private interface VPNAPIProvider {
        String getName();
        VPNResult check(String ip) throws Exception;
    }

    /**
     * ProxyCheck.io - Best free option (100 queries/day)
     */
    private class ProxyCheckProvider implements VPNAPIProvider {
        @Override
        public String getName() { return "proxycheck.io"; }

        @Override
        public VPNResult check(String ip) throws Exception {
            String apiKey = plugin.getConfigManager().getString("anti-vpn.api-keys.proxycheck", "");
            String url = "https://proxycheck.io/v2/" + ip + "?vpn=1&asn=1&risk=1&port=1";
            if (!apiKey.isEmpty()) {
                url += "&key=" + apiKey;
            }

            Request request = new Request.Builder().url(url).build();
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) return null;

                String body = response.body().string();
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();

                if (!json.has(ip)) return null;
                JsonObject ipData = json.getAsJsonObject(ip);

                boolean isVPN = "yes".equalsIgnoreCase(getJsonString(ipData, "proxy"));
                String type = getJsonString(ipData, "type");

                return new VPNResult.Builder(ip)
                        .isVPN(isVPN || "VPN".equalsIgnoreCase(type))
                        .isProxy("Proxy".equalsIgnoreCase(type))
                        .isHosting("Hosting".equalsIgnoreCase(type) || "Data Center".equalsIgnoreCase(type))
                        .isTor("TOR".equalsIgnoreCase(type))
                        .vpnProvider(getJsonString(ipData, "provider"))
                        .isp(getJsonString(ipData, "isp"))
                        .org(getJsonString(ipData, "organisation"))
                        .asn(getJsonString(ipData, "asn"))
                        .country(getJsonString(ipData, "country"))
                        .countryCode(getJsonString(ipData, "isocode"))
                        .city(getJsonString(ipData, "city"))
                        .riskScore(ipData.has("risk") ? ipData.get("risk").getAsDouble() : 0)
                        .apiProvider(getName())
                        .build();
            }
        }
    }

    /**
     * IP-API.com - Unlimited free queries (45/minute)
     */
    private class IPAPIProvider implements VPNAPIProvider {
        @Override
        public String getName() { return "ip-api.com"; }

        @Override
        public VPNResult check(String ip) throws Exception {
            String url = "http://ip-api.com/json/" + ip + "?fields=status,message,country,countryCode,region,city,isp,org,as,proxy,hosting,query";

            Request request = new Request.Builder().url(url).build();
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) return null;

                String body = response.body().string();
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();

                if (!"success".equals(getJsonString(json, "status"))) return null;

                boolean isProxy = json.has("proxy") && json.get("proxy").getAsBoolean();
                boolean isHosting = json.has("hosting") && json.get("hosting").getAsBoolean();

                return new VPNResult.Builder(ip)
                        .isVPN(isProxy)
                        .isProxy(isProxy)
                        .isHosting(isHosting)
                        .isp(getJsonString(json, "isp"))
                        .org(getJsonString(json, "org"))
                        .asn(getJsonString(json, "as"))
                        .country(getJsonString(json, "country"))
                        .countryCode(getJsonString(json, "countryCode"))
                        .city(getJsonString(json, "city"))
                        .apiProvider(getName())
                        .build();
            }
        }
    }

    /**
     * VPN API (vpnapi.io) - 1000 free queries/day
     */
    private class VPNAPIProvider_VPNAPI implements VPNAPIProvider {
        @Override
        public String getName() { return "vpnapi.io"; }

        @Override
        public VPNResult check(String ip) throws Exception {
            String apiKey = plugin.getConfigManager().getString("anti-vpn.api-keys.vpnapi", "");
            String url = "https://vpnapi.io/api/" + ip;
            if (!apiKey.isEmpty()) {
                url += "?key=" + apiKey;
            }

            Request request = new Request.Builder().url(url).build();
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) return null;

                String body = response.body().string();
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();

                if (!json.has("security")) return null;
                JsonObject security = json.getAsJsonObject("security");
                JsonObject location = json.has("location") ? json.getAsJsonObject("location") : new JsonObject();
                JsonObject network = json.has("network") ? json.getAsJsonObject("network") : new JsonObject();

                return new VPNResult.Builder(ip)
                        .isVPN(security.has("vpn") && security.get("vpn").getAsBoolean())
                        .isProxy(security.has("proxy") && security.get("proxy").getAsBoolean())
                        .isTor(security.has("tor") && security.get("tor").getAsBoolean())
                        .isHosting(security.has("relay") && security.get("relay").getAsBoolean())
                        .country(getJsonString(location, "country"))
                        .countryCode(getJsonString(location, "country_code"))
                        .city(getJsonString(location, "city"))
                        .isp(getJsonString(network, "autonomous_system_organization"))
                        .asn(getJsonString(network, "autonomous_system_number"))
                        .apiProvider(getName())
                        .build();
            }
        }
    }

    /**
     * IPHub.info - 1000 free queries/day
     */
    private class IPHubProvider implements VPNAPIProvider {
        @Override
        public String getName() { return "iphub.info"; }

        @Override
        public VPNResult check(String ip) throws Exception {
            String apiKey = plugin.getConfigManager().getString("anti-vpn.api-keys.iphub", "");
            if (apiKey.isEmpty()) return null;

            Request request = new Request.Builder()
                    .url("https://v2.api.iphub.info/ip/" + ip)
                    .addHeader("X-Key", apiKey)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) return null;

                String body = response.body().string();
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();

                int block = json.has("block") ? json.get("block").getAsInt() : 0;
                // 0 = residential, 1 = hosting/vpn, 2 = non-residential

                return new VPNResult.Builder(ip)
                        .isVPN(block == 1)
                        .isHosting(block >= 1)
                        .isp(getJsonString(json, "isp"))
                        .country(getJsonString(json, "countryName"))
                        .countryCode(getJsonString(json, "countryCode"))
                        .asn("AS" + (json.has("asn") ? json.get("asn").getAsInt() : 0))
                        .apiProvider(getName())
                        .build();
            }
        }
    }

    /**
     * IPHunter.info - Free tier available
     */
    private class IPHunterProvider implements VPNAPIProvider {
        @Override
        public String getName() { return "iphunter.info"; }

        @Override
        public VPNResult check(String ip) throws Exception {
            String apiKey = plugin.getConfigManager().getString("anti-vpn.api-keys.iphunter", "");
            if (apiKey.isEmpty()) return null;

            Request request = new Request.Builder()
                    .url("https://www.iphunter.info:8082/v1/ip/" + ip)
                    .addHeader("X-Key", apiKey)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) return null;

                String body = response.body().string();
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();

                if (!json.has("data")) return null;
                JsonObject data = json.getAsJsonObject("data");

                int block = data.has("block") ? data.get("block").getAsInt() : 0;

                return new VPNResult.Builder(ip)
                        .isVPN(block == 1)
                        .isHosting(block >= 1)
                        .country(getJsonString(data, "country_name"))
                        .countryCode(getJsonString(data, "country_code"))
                        .apiProvider(getName())
                        .build();
            }
        }
    }

    /**
     * IPQualityScore - Premium but very accurate
     */
    private class IPQualityScoreProvider implements VPNAPIProvider {
        @Override
        public String getName() { return "ipqualityscore.com"; }

        @Override
        public VPNResult check(String ip) throws Exception {
            String apiKey = plugin.getConfigManager().getString("anti-vpn.api-keys.ipqualityscore", "");
            if (apiKey.isEmpty()) return null;

            String url = "https://ipqualityscore.com/api/json/ip/" + apiKey + "/" + ip + "?strictness=0&allow_public_access_points=true";

            Request request = new Request.Builder().url(url).build();
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) return null;

                String body = response.body().string();
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();

                if (!json.has("success") || !json.get("success").getAsBoolean()) return null;

                return new VPNResult.Builder(ip)
                        .isVPN(json.has("vpn") && json.get("vpn").getAsBoolean())
                        .isProxy(json.has("proxy") && json.get("proxy").getAsBoolean())
                        .isTor(json.has("tor") && json.get("tor").getAsBoolean())
                        .isHosting(json.has("is_crawler") && json.get("is_crawler").getAsBoolean())
                        .isp(getJsonString(json, "ISP"))
                        .org(getJsonString(json, "organization"))
                        .asn("AS" + (json.has("ASN") ? json.get("ASN").getAsInt() : 0))
                        .country(getJsonString(json, "country_code"))
                        .city(getJsonString(json, "city"))
                        .riskScore(json.has("fraud_score") ? json.get("fraud_score").getAsDouble() : 0)
                        .realIP(getJsonString(json, "recent_abuse") != null ? null : getJsonString(json, "host"))
                        .apiProvider(getName())
                        .build();
            }
        }
    }

    // Helper method
    private String getJsonString(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : "";
    }
}
