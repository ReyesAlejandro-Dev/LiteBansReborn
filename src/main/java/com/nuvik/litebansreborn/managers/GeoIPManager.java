package com.nuvik.litebansreborn.managers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nuvik.litebansreborn.LiteBansReborn;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * GeoIP Manager - Handles geolocation lookups for player IPs
 */
public class GeoIPManager {

    private final LiteBansReborn plugin;
    private final OkHttpClient httpClient;
    private final Map<String, GeoIPResult> cache = new ConcurrentHashMap<>();
    
    public GeoIPManager(LiteBansReborn plugin) {
        this.plugin = plugin;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build();
    }
    
    /**
     * Look up geolocation for an IP address
     */
    public CompletableFuture<GeoIPResult> lookup(String ip) {
        if (!plugin.getConfigManager().getBoolean("geoip.enabled")) {
            return CompletableFuture.completedFuture(null);
        }
        
        // Check cache
        GeoIPResult cached = cache.get(ip);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                String provider = plugin.getConfigManager().getString("geoip.provider", "ip-api");
                GeoIPResult result = null;
                
                switch (provider.toLowerCase()) {
                    case "ip-api":
                        result = lookupIPAPI(ip);
                        break;
                    case "ipinfo":
                        result = lookupIPInfo(ip);
                        break;
                    case "ipdata":
                        result = lookupIPData(ip);
                        break;
                    default:
                        result = lookupIPAPI(ip);
                }
                
                if (result != null) {
                    cache.put(ip, result);
                    
                    // Schedule cache expiry
                    int cacheDuration = plugin.getConfigManager().getInt("geoip.cache-duration", 60);
                    plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                        cache.remove(ip);
                    }, cacheDuration * 60L * 20L);
                }
                
                return result;
            } catch (Exception e) {
                plugin.debug("GeoIP lookup failed for " + ip + ": " + e.getMessage());
                return null;
            }
        });
    }
    
    /**
     * Lookup using ip-api.com (free, no API key required)
     */
    private GeoIPResult lookupIPAPI(String ip) throws Exception {
        Request request = new Request.Builder()
                .url("http://ip-api.com/json/" + ip + "?fields=status,country,countryCode,regionName,city,isp")
                .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                return null;
            }
            
            JsonObject json = JsonParser.parseString(response.body().string()).getAsJsonObject();
            
            if (!json.get("status").getAsString().equals("success")) {
                return null;
            }
            
            return new GeoIPResult(
                    json.has("country") ? json.get("country").getAsString() : "Unknown",
                    json.has("countryCode") ? json.get("countryCode").getAsString() : "??",
                    json.has("regionName") ? json.get("regionName").getAsString() : "",
                    json.has("city") ? json.get("city").getAsString() : "",
                    json.has("isp") ? json.get("isp").getAsString() : ""
            );
        }
    }
    
    /**
     * Lookup using ipinfo.io
     */
    private GeoIPResult lookupIPInfo(String ip) throws Exception {
        String apiKey = plugin.getConfigManager().getString("geoip.api-key");
        
        Request.Builder builder = new Request.Builder()
                .url("https://ipinfo.io/" + ip + "/json");
        
        if (apiKey != null && !apiKey.isEmpty()) {
            builder.header("Authorization", "Bearer " + apiKey);
        }
        
        try (Response response = httpClient.newCall(builder.build()).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                return null;
            }
            
            JsonObject json = JsonParser.parseString(response.body().string()).getAsJsonObject();
            
            return new GeoIPResult(
                    json.has("country") ? json.get("country").getAsString() : "Unknown",
                    json.has("country") ? json.get("country").getAsString() : "??",
                    json.has("region") ? json.get("region").getAsString() : "",
                    json.has("city") ? json.get("city").getAsString() : "",
                    json.has("org") ? json.get("org").getAsString() : ""
            );
        }
    }
    
    /**
     * Lookup using ipdata.co
     */
    private GeoIPResult lookupIPData(String ip) throws Exception {
        String apiKey = plugin.getConfigManager().getString("geoip.api-key");
        
        String url = "https://api.ipdata.co/" + ip;
        if (apiKey != null && !apiKey.isEmpty()) {
            url += "?api-key=" + apiKey;
        }
        
        Request request = new Request.Builder().url(url).build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                return null;
            }
            
            JsonObject json = JsonParser.parseString(response.body().string()).getAsJsonObject();
            
            return new GeoIPResult(
                    json.has("country_name") ? json.get("country_name").getAsString() : "Unknown",
                    json.has("country_code") ? json.get("country_code").getAsString() : "??",
                    json.has("region") ? json.get("region").getAsString() : "",
                    json.has("city") ? json.get("city").getAsString() : "",
                    json.has("asn") && json.getAsJsonObject("asn").has("name") ?
                            json.getAsJsonObject("asn").get("name").getAsString() : ""
            );
        }
    }
    
    /**
     * Clear the cache
     */
    public void clearCache() {
        cache.clear();
    }
    
    /**
     * GeoIP result data class
     */
    public static class GeoIPResult {
        private final String country;
        private final String countryCode;
        private final String region;
        private final String city;
        private final String isp;
        
        public GeoIPResult(String country, String countryCode, String region, String city, String isp) {
            this.country = country;
            this.countryCode = countryCode;
            this.region = region;
            this.city = city;
            this.isp = isp;
        }
        
        public String getCountry() { return country; }
        public String getCountryCode() { return countryCode; }
        public String getRegion() { return region; }
        public String getCity() { return city; }
        public String getIsp() { return isp; }
        
        public String getLocation() {
            StringBuilder sb = new StringBuilder();
            if (!city.isEmpty()) {
                sb.append(city).append(", ");
            }
            if (!region.isEmpty()) {
                sb.append(region).append(", ");
            }
            sb.append(country);
            return sb.toString();
        }
    }
}
