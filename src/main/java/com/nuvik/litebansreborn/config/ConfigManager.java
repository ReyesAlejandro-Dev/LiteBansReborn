package com.nuvik.litebansreborn.config;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.utils.TimeUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.ZoneId;
import java.util.List;
import java.util.logging.Level;

/**
 * Configuration Manager - Handles the main config.yml
 */
public class ConfigManager {

    private final LiteBansReborn plugin;
    private FileConfiguration config;
    private File configFile;
    
    public ConfigManager(LiteBansReborn plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Load the configuration
     */
    public void load() {
        configFile = new File(plugin.getDataFolder(), "config.yml");
        
        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
        
        // Merge with defaults to add missing keys
        mergeDefaults();
        
        // Apply settings
        applySettings();
        
        plugin.log(Level.INFO, "Configuration loaded successfully!");
    }
    
    /**
     * Reload the configuration
     */
    public void reload() {
        config = YamlConfiguration.loadConfiguration(configFile);
        
        // Merge with defaults to add missing keys (auto-migration)
        mergeDefaults();
        
        applySettings();
        plugin.log(Level.INFO, "Configuration reloaded!");
    }
    
    /**
     * Merge defaults from the JAR file into the config
     * This adds missing keys without overwriting existing values
     */
    private void mergeDefaults() {
        try {
            // Load defaults from JAR
            java.io.InputStream defaultStream = plugin.getResource("config.yml");
            if (defaultStream == null) return;
            
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                new java.io.InputStreamReader(defaultStream)
            );
            
            boolean modified = false;
            
            // Add missing keys from defaults
            for (String key : defaults.getKeys(true)) {
                if (!config.contains(key)) {
                    config.set(key, defaults.get(key));
                    modified = true;
                    plugin.debug("Added missing config key: " + key);
                }
            }
            
            // Save if modified
            if (modified) {
                save();
                plugin.log(Level.INFO, "Config updated with new options from plugin update!");
            }
            
            defaultStream.close();
        } catch (Exception e) {
            plugin.log(Level.WARNING, "Could not merge config defaults: " + e.getMessage());
        }
    }
    
    /**
     * Save the configuration
     */
    public void save() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.log(Level.SEVERE, "Failed to save configuration: " + e.getMessage());
        }
    }
    
    /**
     * Apply configuration settings
     */
    private void applySettings() {
        // Set date format
        String dateFormat = getString("general.date-format");
        String timezone = getString("general.timezone");
        try {
            TimeUtil.setDateFormat(dateFormat, ZoneId.of(timezone));
        } catch (Exception e) {
            plugin.log(Level.WARNING, "Invalid date format or timezone, using defaults.");
            TimeUtil.setDateFormat("dd/MM/yyyy HH:mm:ss", ZoneId.systemDefault());
        }
    }
    
    // Getters for various config types
    
    public String getString(String path) {
        return config.getString(path, "");
    }
    
    public String getString(String path, String defaultValue) {
        return config.getString(path, defaultValue);
    }
    
    public int getInt(String path) {
        return config.getInt(path, 0);
    }
    
    public int getInt(String path, int defaultValue) {
        return config.getInt(path, defaultValue);
    }
    
    public double getDouble(String path) {
        return config.getDouble(path, 0.0);
    }
    
    public double getDouble(String path, double defaultValue) {
        return config.getDouble(path, defaultValue);
    }
    
    public boolean getBoolean(String path) {
        return config.getBoolean(path, false);
    }
    
    public boolean getBoolean(String path, boolean defaultValue) {
        return config.getBoolean(path, defaultValue);
    }
    
    public List<String> getStringList(String path) {
        return config.getStringList(path);
    }
    
    public boolean contains(String path) {
        return config.contains(path);
    }
    
    public void set(String path, Object value) {
        config.set(path, value);
    }
    
    public FileConfiguration getConfig() {
        return config;
    }
    
    // Convenience methods for common settings
    
    public String getServerName() {
        return getString("general.server-name", "server");
    }
    
    public boolean isDebugEnabled() {
        return getBoolean("general.debug");
    }
    
    public String getLanguage() {
        return getString("general.language", "en");
    }
    
    public String getDefaultBanReason() {
        return getString("punishments.default-reasons.ban");
    }
    
    public String getDefaultMuteReason() {
        return getString("punishments.default-reasons.mute");
    }
    
    public String getDefaultKickReason() {
        return getString("punishments.default-reasons.kick");
    }
    
    public String getDefaultWarnReason() {
        return getString("punishments.default-reasons.warn");
    }
    
    public String getDefaultFreezeReason() {
        return getString("punishments.default-reasons.freeze");
    }
    
    public List<String> getMuteBlockedCommands() {
        return getStringList("punishments.mute.blocked-commands");
    }
    
    public int getMaxWarnings() {
        return getInt("punishments.warn.max-warnings", 3);
    }
    
    public boolean isWarnAutoActionEnabled() {
        return getBoolean("punishments.warn.auto-action.enabled");
    }
    
    public String getWarnAutoAction() {
        return getString("punishments.warn.auto-action.action", "ban");
    }
    
    public String getWarnAutoActionDuration() {
        return getString("punishments.warn.auto-action.duration", "7d");
    }
    
    public String getWarnAutoActionReason() {
        return getString("punishments.warn.auto-action.reason");
    }
    
    public int getWarnExpiry() {
        return getInt("punishments.warn.expiry", 30);
    }
}
