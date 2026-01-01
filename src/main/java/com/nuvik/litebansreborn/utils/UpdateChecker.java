package com.nuvik.litebansreborn.utils;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;

/**
 * Checks for updates on SpigotMC (via Spiget API)
 */
public class UpdateChecker {

    private final LiteBansReborn plugin;
    private final int resourceId;
    private boolean updateAvailable;
    private String latestVersion;

    public UpdateChecker(LiteBansReborn plugin, int resourceId) {
        this.plugin = plugin;
        this.resourceId = resourceId;
        this.updateAvailable = false;
        
        checkForUpdates();
    }

    public void checkForUpdates() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Use Spiget API to get resource info
                URL url = new URL("https://api.spiget.org/v2/resources/" + resourceId + "/versions/latest");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "LiteBansReborn-UpdateChecker");
                
                if (connection.getResponseCode() == 200) {
                    InputStreamReader reader = new InputStreamReader(connection.getInputStream());
                    JsonElement element = JsonParser.parseReader(reader);
                    
                    if (element.isJsonObject()) {
                        JsonObject jsonObj = element.getAsJsonObject();
                        latestVersion = jsonObj.get("name").getAsString();
                        String currentVersion = plugin.getDescription().getVersion();
                        
                        // Use semantic version comparison
                        int comparison = compareVersions(currentVersion, latestVersion);
                        
                        if (comparison < 0) {
                            updateAvailable = true;
                            plugin.log(Level.INFO, "");
                            plugin.log(Level.INFO, "§e============================================");
                            plugin.log(Level.INFO, "§e A new update is available!");
                            plugin.log(Level.INFO, "§e Latest: §f" + latestVersion + " §7(You have: " + currentVersion + ")");
                            plugin.log(Level.INFO, "§e Download: §bhttps://www.spigotmc.org/resources/" + resourceId + "/");
                            plugin.log(Level.INFO, "§e============================================");
                            plugin.log(Level.INFO, "");
                        } else if (comparison == 0) {
                            plugin.debug("Plugin is up to date (Version: " + currentVersion + ")");
                        } else {
                            plugin.debug("Running development version: " + currentVersion + " (Latest stable: " + latestVersion + ")");
                        }
                    }
                    reader.close();
                }
            } catch (Exception e) {
                plugin.log(Level.WARNING, "Failed to check for updates: " + e.getMessage());
            }
        });
    }
    
    /**
     * Compare two version strings using semantic versioning.
     * Returns: -1 if v1 < v2, 0 if v1 == v2, 1 if v1 > v2
     * 
     * Handles cases like:
     * - "5.7" vs "6.0.0" -> 0 (equal)
     * - "6.0.0" vs "6.0.0" -> -1 (v1 is older)
     * - "5.7.1" vs "6.0.0" -> 1 (v1 is newer)
     */
    private int compareVersions(String v1, String v2) {
        // Remove any non-numeric prefixes (like 'v')
        v1 = v1.replaceAll("^[^0-9]*", "");
        v2 = v2.replaceAll("^[^0-9]*", "");
        
        // Split by dot
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        
        int maxLength = Math.max(parts1.length, parts2.length);
        
        for (int i = 0; i < maxLength; i++) {
            int num1 = 0;
            int num2 = 0;
            
            if (i < parts1.length) {
                try {
                    // Remove any non-numeric suffixes (like -SNAPSHOT)
                    String part = parts1[i].replaceAll("[^0-9].*", "");
                    num1 = part.isEmpty() ? 0 : Integer.parseInt(part);
                } catch (NumberFormatException e) {
                    num1 = 0;
                }
            }
            
            if (i < parts2.length) {
                try {
                    String part = parts2[i].replaceAll("[^0-9].*", "");
                    num2 = part.isEmpty() ? 0 : Integer.parseInt(part);
                } catch (NumberFormatException e) {
                    num2 = 0;
                }
            }
            
            if (num1 < num2) return -1;
            if (num1 > num2) return 1;
        }
        
        return 0; // Versions are equal
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public String getLatestVersion() {
        return latestVersion;
    }
}
