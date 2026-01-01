package com.nuvik.litebansreborn.antivpn;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nuvik.litebansreborn.LiteBansReborn;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;

/**
 * VPN Manager - Handles VPN/Proxy detection using multiple API providers
 * Features: Rotational API system, Multiple providers, Caching with TTL, Circuit breaker
 */
public class VPNManager {

    // ==================== CONSTANTS ====================
    private static final int HTTP_CONNECT_TIMEOUT = 10;
    private static final int HTTP_READ_TIMEOUT = 10;
    private static final int HTTP_CALL_TIMEOUT = 12;
    private static final int DEFAULT_CACHE_MINUTES = 60;
    private static final int PROVIDER_COOLDOWN_SECONDS = 60;
    private static final int EXECUTOR_POOL_SIZE = 4;
    private static final List<String> DEFAULT_PROVIDERS = List.of("proxycheck", "ip-api", "vpnapi", "iphub");

    // ==================== FIELDS ====================
    private final LiteBansReborn plugin;
    private final OkHttpClient httpClient;
    private final VPNDatabase database;
    private final ExecutorService vpnExecutor;
    
    // Cache with expiration
    private final Map<String, CachedResult> cache = new ConcurrentHashMap<>();
    
    // Whitelists
    private final Set<String> whitelistedIPs = ConcurrentHashMap.newKeySet();
    private final Set<String> whitelistedCountries = ConcurrentHashMap.newKeySet();
    
    // Providers with circuit breaker
    private final List<VPNAPIProvider> providers = new ArrayList<>();
    private final Map<String, Long> providerCooldownUntil = new ConcurrentHashMap<>();
    private int currentProviderIndex = 0;
    private final Object providerLock = new Object();

    // Runtime state
    private boolean runtimeEnabled = true;
    private boolean alertsEnabled = true;
    private VPNAction runtimeAction = null;

    // ==================== ENUMS ====================
    public enum VPNAction { KICK, WARN, ALLOW, NONE }

    // ==================== CONSTRUCTOR ====================
    public VPNManager(LiteBansReborn plugin) {
        this.plugin = plugin;
        
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(HTTP_CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(HTTP_READ_TIMEOUT, TimeUnit.SECONDS)
                .callTimeout(HTTP_CALL_TIMEOUT, TimeUnit.SECONDS)
                .build();
        
        this.vpnExecutor = Executors.newFixedThreadPool(EXECUTOR_POOL_SIZE, 
            r -> new Thread(r, "LiteBansReborn-VPN-" + System.currentTimeMillis() % 1000));
        
        this.database = new VPNDatabase(plugin);
        database.initialize();
        
        loadConfig();
    }

    // ==================== CONFIGURATION ====================
    private void loadConfig() {
        whitelistedIPs.clear();
        whitelistedIPs.addAll(plugin.getConfigManager().getStringList("anti-vpn.whitelist.ips"));
        
        whitelistedCountries.clear();
        plugin.getConfigManager().getStringList("anti-vpn.whitelist.countries")
            .stream()
            .map(String::toUpperCase)
            .forEach(whitelistedCountries::add);
        
        initializeProviders();
    }

    private void initializeProviders() {
        providers.clear();
        
        List<String> enabled = plugin.getConfigManager().getStringList("anti-vpn.providers");
        if (enabled.isEmpty()) {
            enabled = DEFAULT_PROVIDERS;
        }
        
        Map<String, VPNAPIProvider> providerMap = Map.of(
            "proxycheck", new ProxyCheckProvider(),
            "ip-api", new IPAPIProvider(),
            "vpnapi", new VPNAPIProvider_VPNAPI(),
            "iphub", new IPHubProvider(),
            "iphunter", new IPHunterProvider(),
            "ipqualityscore", new IPQualityScoreProvider()
        );
        
        enabled.stream()
            .map(String::toLowerCase)
            .filter(providerMap::containsKey)
            .map(providerMap::get)
            .forEach(providers::add);
        
        if (providers.isEmpty()) {
            providers.add(new IPAPIProvider());
        }
        
        plugin.log(Level.INFO, "Anti-VPN initialized with " + providers.size() + " providers");
    }

    // ==================== PUBLIC API ====================
    
    public boolean isEnabled() {
        return plugin.getConfigManager().getBoolean("anti-vpn.enabled", false);
    }

    public boolean isRuntimeEnabled() {
        return runtimeEnabled && isEnabled();
    }

    public VPNAction getAction() {
        String action = plugin.getConfigManager().getString("anti-vpn.action", "warn").toUpperCase();
        try {
            return VPNAction.valueOf(action);
        } catch (IllegalArgumentException e) {
            return VPNAction.WARN;
        }
    }

    public VPNAction getEffectiveAction() {
        return runtimeAction != null ? runtimeAction : getAction();
    }

    /**
     * Check if an IP is a VPN/Proxy (async, non-blocking)
     */
    public CompletableFuture<VPNResult> checkIP(String ip) {
        // Fast path: whitelisted
        if (isWhitelisted(ip)) {
            return CompletableFuture.completedFuture(VPNResult.clean(ip, "whitelisted"));
        }
        
        // Check cache
        CachedResult cached = cache.get(ip);
        if (cached != null && !cached.isExpired()) {
            plugin.debug("VPN cache hit for " + ip);
            return CompletableFuture.completedFuture(cached.result);
        }
        
        // Async check via database then providers
        return database.isKnownVPN(ip)
            .exceptionally(e -> false)
            .thenApplyAsync(knownVPN -> {
                if (knownVPN) {
                    plugin.debug("IP " + ip + " found in local VPN database");
                }
                
                VPNResult result = queryProviders(ip);
                
                // Apply country whitelist
                if (result != null && result.getCountryCode() != null) {
                    if (whitelistedCountries.contains(result.getCountryCode().toUpperCase())) {
                        result = VPNResult.clean(ip, "country_whitelisted:" + result.getCountryCode());
                    }
                }
                
                // Cache result
                if (result != null) {
                    long ttlMinutes = plugin.getConfigManager().getInt("anti-vpn.cache-duration", DEFAULT_CACHE_MINUTES);
                    cache.put(ip, new CachedResult(result, System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(ttlMinutes)));
                }
                
                return result != null ? result : VPNResult.unknown(ip);
            }, vpnExecutor);
    }

    // ==================== PROVIDER LOGIC ====================
    
    private VPNResult queryProviders(String ip) {
        for (int attempts = 0; attempts < providers.size(); attempts++) {
            VPNAPIProvider provider;
            synchronized (providerLock) {
                provider = providers.get(currentProviderIndex);
            }
            
            // Circuit breaker: skip if on cooldown
            if (isOnCooldown(provider)) {
                rotateProvider();
                continue;
            }
            
            try {
                VPNResult result = provider.check(ip);
                if (result != null) {
                    plugin.debug("VPN check via " + provider.getName() + " for " + ip);
                    return result;
                }
            } catch (Exception e) {
                plugin.debug("Provider " + provider.getName() + " failed: " + e.getMessage());
                setCooldown(provider, PROVIDER_COOLDOWN_SECONDS);
            }
            
            rotateProvider();
        }
        
        plugin.log(Level.WARNING, "All VPN providers failed for IP: " + ip);
        return null;
    }
    
    private void rotateProvider() {
        synchronized (providerLock) {
            currentProviderIndex = (currentProviderIndex + 1) % providers.size();
        }
    }
    
    private boolean isOnCooldown(VPNAPIProvider provider) {
        return providerCooldownUntil.getOrDefault(provider.getName(), 0L) > System.currentTimeMillis();
    }
    
    private void setCooldown(VPNAPIProvider provider, long seconds) {
        providerCooldownUntil.put(provider.getName(), System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(seconds));
    }

    // ==================== WHITELIST ====================
    
    private boolean isWhitelisted(String ip) {
        return whitelistedIPs.contains(ip) || isPrivateOrLocal(ip);
    }
    
    private boolean isPrivateOrLocal(String ip) {
        try {
            InetAddress addr = InetAddress.getByName(ip);
            return addr.isAnyLocalAddress() 
                || addr.isLoopbackAddress() 
                || addr.isLinkLocalAddress() 
                || addr.isSiteLocalAddress();
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== DATABASE DELEGATIONS ====================
    
    public void logDetection(VPNResult result, UUID playerUUID, String playerName, VPNAction action) {
        database.logDetection(result, playerUUID, playerName, action.name());
        database.trackIP(playerUUID, playerName, result.getIp(), result.isDangerous());
    }
    
    public CompletableFuture<String> getLikelyRealIP(UUID playerUUID) {
        return database.getLikelyRealIP(playerUUID);
    }
    
    public CompletableFuture<List<VPNDatabase.TrackedIP>> getPlayerIPs(UUID playerUUID) {
        return database.getPlayerIPs(playerUUID);
    }
    
    public CompletableFuture<VPNDatabase.VPNStats> getStats() {
        return database.getStats();
    }
    
    public CompletableFuture<List<VPNDatabase.VPNDetectionRecord>> getRecentDetections(int limit) {
        return database.getRecentDetections(limit);
    }
    
    public VPNDatabase getDatabase() { return database; }

    // ==================== RUNTIME CONTROLS ====================
    
    public void setEnabled(boolean enabled) { this.runtimeEnabled = enabled; }
    public void setAlertsEnabled(boolean enabled) { this.alertsEnabled = enabled; }
    public void setAction(VPNAction action) { this.runtimeAction = action; }
    
    public boolean areAlertsEnabled() {
        return alertsEnabled && plugin.getConfigManager().getBoolean("anti-vpn.alerts", true);
    }
    
    public void whitelistIP(String ip) {
        whitelistedIPs.add(ip);
        cache.remove(ip);
    }
    
    public void unwhitelistIP(String ip) {
        whitelistedIPs.remove(ip);
    }
    
    public void clearCache() { cache.clear(); }
    public Set<String> getWhitelistedIPs() { return Set.copyOf(whitelistedIPs); }
    public int getCacheSize() { return cache.size(); }
    public int getProviderCount() { return providers.size(); }

    // ==================== SHUTDOWN ====================
    
    public void shutdown() {
        database.close();
        cache.clear();
        vpnExecutor.shutdownNow();
    }

    // ==================== CACHE WRAPPER ====================
    
    private record CachedResult(VPNResult result, long expiresAt) {
        boolean isExpired() { return System.currentTimeMillis() > expiresAt; }
    }

    // ==================== JSON HELPERS ====================
    
    private static String jsonStr(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : "";
    }
    
    private static boolean jsonBool(JsonObject json, String key) {
        return json.has(key) && json.get(key).getAsBoolean();
    }
    
    private static double jsonDouble(JsonObject json, String key) {
        return json.has(key) ? json.get(key).getAsDouble() : 0.0;
    }
    
    private static int jsonInt(JsonObject json, String key) {
        return json.has(key) ? json.get(key).getAsInt() : 0;
    }
    
    private String getApiKey(String provider) {
        return plugin.getConfigManager().getString("anti-vpn.api-keys." + provider, "");
    }

    // ==================== PROVIDER INTERFACE ====================
    
    private interface VPNAPIProvider {
        String getName();
        VPNResult check(String ip) throws Exception;
    }

    // ==================== PROVIDER IMPLEMENTATIONS ====================

    /** ProxyCheck.io - Best free option (100 queries/day) */
    private class ProxyCheckProvider implements VPNAPIProvider {
        @Override public String getName() { return "proxycheck.io"; }

        @Override
        public VPNResult check(String ip) throws Exception {
            String apiKey = getApiKey("proxycheck");
            String url = "https://proxycheck.io/v2/" + ip + "?vpn=1&asn=1&risk=1&port=1" + 
                        (apiKey.isEmpty() ? "" : "&key=" + apiKey);

            try (Response response = httpClient.newCall(new Request.Builder().url(url).build()).execute()) {
                ResponseBody body = response.body();
                if (!response.isSuccessful() || body == null) return null;

                JsonObject json = JsonParser.parseString(body.string()).getAsJsonObject();
                if (!json.has(ip)) return null;
                
                JsonObject data = json.getAsJsonObject(ip);
                String type = jsonStr(data, "type");

                return new VPNResult.Builder(ip)
                    .isVPN("yes".equalsIgnoreCase(jsonStr(data, "proxy")) || "VPN".equalsIgnoreCase(type))
                    .isProxy("Proxy".equalsIgnoreCase(type))
                    .isHosting("Hosting".equalsIgnoreCase(type) || "Data Center".equalsIgnoreCase(type))
                    .isTor("TOR".equalsIgnoreCase(type))
                    .vpnProvider(jsonStr(data, "provider"))
                    .isp(jsonStr(data, "isp"))
                    .org(jsonStr(data, "organisation"))
                    .asn(jsonStr(data, "asn"))
                    .country(jsonStr(data, "country"))
                    .countryCode(jsonStr(data, "isocode"))
                    .city(jsonStr(data, "city"))
                    .riskScore(jsonDouble(data, "risk"))
                    .apiProvider(getName())
                    .build();
            }
        }
    }

    /** IP-API.com - Unlimited free (45/min) - Uses HTTPS */
    private class IPAPIProvider implements VPNAPIProvider {
        @Override public String getName() { return "ip-api.com"; }

        @Override
        public VPNResult check(String ip) throws Exception {
            String url = "https://ip-api.com/json/" + ip + "?fields=status,message,country,countryCode,region,city,isp,org,as,proxy,hosting,query";

            try (Response response = httpClient.newCall(new Request.Builder().url(url).build()).execute()) {
                ResponseBody body = response.body();
                if (!response.isSuccessful() || body == null) return null;

                JsonObject json = JsonParser.parseString(body.string()).getAsJsonObject();
                if (!"success".equals(jsonStr(json, "status"))) return null;

                return new VPNResult.Builder(ip)
                    .isVPN(jsonBool(json, "proxy"))
                    .isProxy(jsonBool(json, "proxy"))
                    .isHosting(jsonBool(json, "hosting"))
                    .isp(jsonStr(json, "isp"))
                    .org(jsonStr(json, "org"))
                    .asn(jsonStr(json, "as"))
                    .country(jsonStr(json, "country"))
                    .countryCode(jsonStr(json, "countryCode"))
                    .city(jsonStr(json, "city"))
                    .apiProvider(getName())
                    .build();
            }
        }
    }

    /** VPNAPI.io - 1000 free/day */
    private class VPNAPIProvider_VPNAPI implements VPNAPIProvider {
        @Override public String getName() { return "vpnapi.io"; }

        @Override
        public VPNResult check(String ip) throws Exception {
            String apiKey = getApiKey("vpnapi");
            String url = "https://vpnapi.io/api/" + ip + (apiKey.isEmpty() ? "" : "?key=" + apiKey);

            try (Response response = httpClient.newCall(new Request.Builder().url(url).build()).execute()) {
                ResponseBody body = response.body();
                if (!response.isSuccessful() || body == null) return null;

                JsonObject json = JsonParser.parseString(body.string()).getAsJsonObject();
                if (!json.has("security")) return null;
                
                JsonObject security = json.getAsJsonObject("security");
                JsonObject location = json.has("location") ? json.getAsJsonObject("location") : new JsonObject();
                JsonObject network = json.has("network") ? json.getAsJsonObject("network") : new JsonObject();

                return new VPNResult.Builder(ip)
                    .isVPN(jsonBool(security, "vpn"))
                    .isProxy(jsonBool(security, "proxy"))
                    .isTor(jsonBool(security, "tor"))
                    .isHosting(jsonBool(security, "relay"))
                    .country(jsonStr(location, "country"))
                    .countryCode(jsonStr(location, "country_code"))
                    .city(jsonStr(location, "city"))
                    .isp(jsonStr(network, "autonomous_system_organization"))
                    .asn(jsonStr(network, "autonomous_system_number"))
                    .apiProvider(getName())
                    .build();
            }
        }
    }

    /** IPHub.info - 1000 free/day */
    private class IPHubProvider implements VPNAPIProvider {
        @Override public String getName() { return "iphub.info"; }

        @Override
        public VPNResult check(String ip) throws Exception {
            String apiKey = getApiKey("iphub");
            if (apiKey.isEmpty()) return null;

            Request request = new Request.Builder()
                .url("https://v2.api.iphub.info/ip/" + ip)
                .addHeader("X-Key", apiKey)
                .build();

            try (Response response = httpClient.newCall(request).execute()) {
                ResponseBody body = response.body();
                if (!response.isSuccessful() || body == null) return null;

                JsonObject json = JsonParser.parseString(body.string()).getAsJsonObject();
                int block = jsonInt(json, "block"); // 0=residential, 1=hosting/vpn, 2=non-residential

                return new VPNResult.Builder(ip)
                    .isVPN(block == 1)
                    .isHosting(block >= 1)
                    .isp(jsonStr(json, "isp"))
                    .country(jsonStr(json, "countryName"))
                    .countryCode(jsonStr(json, "countryCode"))
                    .asn("AS" + jsonInt(json, "asn"))
                    .apiProvider(getName())
                    .build();
            }
        }
    }

    /** IPHunter.info - Free tier */
    private class IPHunterProvider implements VPNAPIProvider {
        @Override public String getName() { return "iphunter.info"; }

        @Override
        public VPNResult check(String ip) throws Exception {
            String apiKey = getApiKey("iphunter");
            if (apiKey.isEmpty()) return null;

            Request request = new Request.Builder()
                .url("https://www.iphunter.info:8082/v1/ip/" + ip)
                .addHeader("X-Key", apiKey)
                .build();

            try (Response response = httpClient.newCall(request).execute()) {
                ResponseBody body = response.body();
                if (!response.isSuccessful() || body == null) return null;

                JsonObject json = JsonParser.parseString(body.string()).getAsJsonObject();
                if (!json.has("data")) return null;
                
                JsonObject data = json.getAsJsonObject("data");
                int block = jsonInt(data, "block");

                return new VPNResult.Builder(ip)
                    .isVPN(block == 1)
                    .isHosting(block >= 1)
                    .country(jsonStr(data, "country_name"))
                    .countryCode(jsonStr(data, "country_code"))
                    .apiProvider(getName())
                    .build();
            }
        }
    }

    /** IPQualityScore - Premium, very accurate */
    private class IPQualityScoreProvider implements VPNAPIProvider {
        @Override public String getName() { return "ipqualityscore.com"; }

        @Override
        public VPNResult check(String ip) throws Exception {
            String apiKey = getApiKey("ipqualityscore");
            if (apiKey.isEmpty()) return null;

            String url = "https://ipqualityscore.com/api/json/ip/" + apiKey + "/" + ip + "?strictness=0&allow_public_access_points=true";

            try (Response response = httpClient.newCall(new Request.Builder().url(url).build()).execute()) {
                ResponseBody body = response.body();
                if (!response.isSuccessful() || body == null) return null;

                JsonObject json = JsonParser.parseString(body.string()).getAsJsonObject();
                if (!jsonBool(json, "success")) return null;

                boolean recentAbuse = jsonBool(json, "recent_abuse");
                String host = jsonStr(json, "host");

                return new VPNResult.Builder(ip)
                    .isVPN(jsonBool(json, "vpn"))
                    .isProxy(jsonBool(json, "proxy"))
                    .isTor(jsonBool(json, "tor"))
                    .isHosting(jsonBool(json, "is_crawler"))
                    .isp(jsonStr(json, "ISP"))
                    .org(jsonStr(json, "organization"))
                    .asn("AS" + jsonInt(json, "ASN"))
                    .country(jsonStr(json, "country_code"))
                    .city(jsonStr(json, "city"))
                    .riskScore(jsonDouble(json, "fraud_score"))
                    .realIP(recentAbuse ? "" : host)
                    .apiProvider(getName())
                    .build();
            }
        }
    }
}
