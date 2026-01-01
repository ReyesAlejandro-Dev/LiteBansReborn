package com.nuvik.litebansreborn.config;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.utils.ColorUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Messages Manager - Handles all plugin messages
 */
public class MessagesManager {

    private final LiteBansReborn plugin;
    private FileConfiguration messages;
    private File messagesFile;
    private String prefix;
    
    public MessagesManager(LiteBansReborn plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Load the messages
     */
    public void load() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        
        // Merge with defaults to add missing messages
        mergeDefaults();
        
        // Load prefix
        prefix = ColorUtil.translate(messages.getString("prefix", "&8[&c&lLiteBans&8] "));
        
        plugin.log(Level.INFO, "Messages loaded successfully!");
    }
    
    /**
     * Reload the messages
     */
    public void reload() {
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        
        // Merge with defaults to add missing messages (auto-migration)
        mergeDefaults();
        
        prefix = ColorUtil.translate(messages.getString("prefix", "&8[&c&lLiteBans&8] "));
        plugin.log(Level.INFO, "Messages reloaded!");
    }
    
    /**
     * Merge defaults from the JAR file into messages
     * This adds missing keys without overwriting existing translations
     */
    private void mergeDefaults() {
        try {
            // Load defaults from JAR
            java.io.InputStream defaultStream = plugin.getResource("messages.yml");
            if (defaultStream == null) return;
            
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultStream, java.nio.charset.StandardCharsets.UTF_8)
            );
            
            boolean modified = false;
            int addedKeys = 0;
            
            // Add missing keys from defaults
            for (String key : defaults.getKeys(true)) {
                if (!messages.contains(key)) {
                    messages.set(key, defaults.get(key));
                    modified = true;
                    addedKeys++;
                }
            }
            
            // Save if modified
            if (modified) {
                try {
                    messages.save(messagesFile);
                    plugin.log(Level.INFO, "Messages updated with " + addedKeys + " new entries from plugin update!");
                } catch (Exception e) {
                    plugin.log(Level.WARNING, "Could not save updated messages: " + e.getMessage());
                }
            }
            
            defaultStream.close();
        } catch (Exception e) {
            plugin.log(Level.WARNING, "Could not merge message defaults: " + e.getMessage());
        }
    }
    
    /**
     * Get the plugin prefix
     */
    public String getPrefix() {
        return prefix;
    }
    
    /**
     * Get a raw message (not translated)
     */
    public String getRaw(String path) {
        return messages.getString(path, "Message not found: " + path);
    }
    
    /**
     * Get a message with color codes translated
     */
    public String get(String path) {
        return ColorUtil.translate(getRaw(path));
    }
    
    /**
     * Get a message with placeholders replaced
     */
    public String get(String path, Map<String, String> placeholders) {
        String message = getRaw(path);
        
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        
        return ColorUtil.translate(message);
    }
    
    /**
     * Get a message with placeholders replaced (varargs version)
     */
    public String get(String path, Object... args) {
        Map<String, String> placeholders = new HashMap<>();
        
        for (int i = 0; i < args.length - 1; i += 2) {
            placeholders.put(String.valueOf(args[i]), String.valueOf(args[i + 1]));
        }
        
        return get(path, placeholders);
    }
    
    /**
     * Get a message list
     */
    public List<String> getList(String path) {
        return messages.getStringList(path).stream()
                .map(ColorUtil::translate)
                .collect(Collectors.toList());
    }
    
    /**
     * Get a message list with placeholders
     */
    public List<String> getList(String path, Map<String, String> placeholders) {
        return messages.getStringList(path).stream()
                .map(line -> {
                    for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                        line = line.replace("%" + entry.getKey() + "%", entry.getValue());
                    }
                    return ColorUtil.translate(line);
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Send a message to a sender with prefix
     */
    public void send(CommandSender sender, String path) {
        sender.sendMessage(prefix + get(path));
    }
    
    /**
     * Send a message to a sender with placeholders
     */
    public void send(CommandSender sender, String path, Map<String, String> placeholders) {
        sender.sendMessage(prefix + get(path, placeholders));
    }
    
    /**
     * Send a message to a sender with placeholders (varargs)
     */
    public void send(CommandSender sender, String path, Object... args) {
        sender.sendMessage(prefix + get(path, args));
    }
    
    /**
     * Send a message list to a sender
     */
    public void sendList(CommandSender sender, String path) {
        getList(path).forEach(sender::sendMessage);
    }
    
    /**
     * Send a message list with placeholders
     */
    public void sendList(CommandSender sender, String path, Map<String, String> placeholders) {
        getList(path, placeholders).forEach(sender::sendMessage);
    }
    
    /**
     * Send a raw message (no prefix)
     */
    public void sendRaw(CommandSender sender, String path) {
        sender.sendMessage(get(path));
    }
    
    /**
     * Send a raw message with placeholders
     */
    public void sendRaw(CommandSender sender, String path, Map<String, String> placeholders) {
        sender.sendMessage(get(path, placeholders));
    }
    
    /**
     * Create a placeholder map easily
     */
    public static Map<String, String> placeholders(Object... args) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < args.length - 1; i += 2) {
            map.put(String.valueOf(args[i]), String.valueOf(args[i + 1]));
        }
        return map;
    }
    
    /**
     * Check if a path exists
     */
    public boolean contains(String path) {
        return messages.contains(path);
    }
}
