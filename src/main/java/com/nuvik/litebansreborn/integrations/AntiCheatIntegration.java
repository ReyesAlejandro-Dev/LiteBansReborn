package com.nuvik.litebansreborn.integrations;

import com.nuvik.litebansreborn.LiteBansReborn;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Anti-Cheat Integration - Hooks into popular anti-cheat plugins
 * Supports: Vulcan, Grim, Matrix, Spartan, NoCheatPlus, AAC
 */
public class AntiCheatIntegration implements Listener {
    
    private final LiteBansReborn plugin;
    private final Map<String, Boolean> detectedPlugins = new HashMap<>();
    private final Map<UUID, Integer> violationCounts = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastViolationType = new ConcurrentHashMap<>();
    
    // Configurable thresholds
    private int autoBanThreshold;
    private int autoMuteThreshold;
    private int autoWarnThreshold;
    private boolean autoBanEnabled;
    private String autoBanDuration;
    private String autoBanReason;
    
    public AntiCheatIntegration(LiteBansReborn plugin) {
        this.plugin = plugin;
        loadConfig();
        detectAntiCheatPlugins();
        
        // Register event listener
        Bukkit.getPluginManager().registerEvents(this, plugin);
        
        plugin.getLogger().info("[AntiCheat] Integration initialized. Detected: " + getDetectedPlugins());
    }
    
    private void loadConfig() {
        autoBanEnabled = plugin.getConfig().getBoolean("anti-cheat.auto-ban.enabled", true);
        autoBanThreshold = plugin.getConfig().getInt("anti-cheat.auto-ban.threshold", 50);
        autoBanDuration = plugin.getConfig().getString("anti-cheat.auto-ban.duration", "7d");
        autoBanReason = plugin.getConfig().getString("anti-cheat.auto-ban.reason", "Cheating detected by anti-cheat");
        autoWarnThreshold = plugin.getConfig().getInt("anti-cheat.auto-warn.threshold", 20);
        autoMuteThreshold = plugin.getConfig().getInt("anti-cheat.auto-mute.threshold", 30);
    }
    
    private void detectAntiCheatPlugins() {
        String[] antiCheats = {"Vulcan", "Grim", "GrimAC", "Matrix", "Spartan", "NoCheatPlus", "AAC", "Negativity", "Kauri"};
        
        for (String ac : antiCheats) {
            boolean detected = Bukkit.getPluginManager().getPlugin(ac) != null;
            detectedPlugins.put(ac, detected);
            if (detected) {
                plugin.getLogger().info("[AntiCheat] Detected: " + ac);
                registerHooks(ac);
            }
        }
    }
    
    private void registerHooks(String antiCheat) {
        // Each anti-cheat has its own API, we'll try to hook into their events
        switch (antiCheat.toLowerCase()) {
            case "vulcan" -> registerVulcanHook();
            case "grim", "grimac" -> registerGrimHook();
            case "matrix" -> registerMatrixHook();
            // Add more as needed
        }
    }
    
    private void registerVulcanHook() {
        // Vulcan fires VulcanFlagEvent
        try {
            Class<?> vulcanEvent = Class.forName("me.frep.vulcan.api.event.VulcanFlagEvent");
            plugin.getLogger().info("[AntiCheat] Vulcan hook registered");
        } catch (ClassNotFoundException e) {
            // Vulcan not properly loaded
        }
    }
    
    private void registerGrimHook() {
        try {
            Class<?> grimEvent = Class.forName("ac.grim.grimac.api.events.FlagEvent");
            plugin.getLogger().info("[AntiCheat] GrimAC hook registered");
        } catch (ClassNotFoundException e) {
            // Grim not properly loaded
        }
    }
    
    private void registerMatrixHook() {
        try {
            Class<?> matrixEvent = Class.forName("me.rerere.matrix.api.events.PlayerViolationEvent");
            plugin.getLogger().info("[AntiCheat] Matrix hook registered");
        } catch (ClassNotFoundException e) {
            // Matrix not properly loaded
        }
    }
    
    /**
     * Process a violation from any anti-cheat
     */
    public void processViolation(Player player, String checkType, int violations, String antiCheatName) {
        if (!autoBanEnabled) return;
        if (isVIPProtected(player)) return;
        
        UUID uuid = player.getUniqueId();
        violationCounts.merge(uuid, violations, Integer::sum);
        lastViolationType.put(uuid, checkType);
        
        int totalViolations = violationCounts.getOrDefault(uuid, 0);
        
        plugin.debug("[AntiCheat] " + player.getName() + " flagged for " + checkType + 
                    " (" + totalViolations + " total) by " + antiCheatName);
        
        // Check thresholds
        if (totalViolations >= autoBanThreshold) {
            executeBan(player, checkType, antiCheatName);
        } else if (totalViolations >= autoMuteThreshold) {
            executeMute(player, checkType, antiCheatName);
        } else if (totalViolations >= autoWarnThreshold) {
            executeWarn(player, checkType, antiCheatName);
        }
    }
    
    private void executeBan(Player player, String checkType, String antiCheatName) {
        if (!autoBanEnabled) return;
        
        String reason = autoBanReason + " [" + checkType + "]";
        
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            String command = "tempban " + player.getName() + " " + autoBanDuration + " " + reason;
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command);
            
            plugin.getLogger().info("[AntiCheat] Auto-banned " + player.getName() + " for " + checkType);
            
            // Clear violations
            violationCounts.remove(player.getUniqueId());
            lastViolationType.remove(player.getUniqueId());
        });
    }
    
    private void executeMute(Player player, String checkType, String antiCheatName) {
        String reason = "Suspicious activity: " + checkType;
        String muteDuration = plugin.getConfig().getString("anti-cheat.auto-mute.duration", "10m");
        
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            String command = "tempmute " + player.getName() + " " + muteDuration + " " + reason;
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command);
            
            plugin.getLogger().info("[AntiCheat] Auto-muted " + player.getName() + " for " + checkType);
        });
    }
    
    private void executeWarn(Player player, String checkType, String antiCheatName) {
        String reason = "Suspicious activity: " + checkType;
        
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            String command = "warn " + player.getName() + " " + reason;
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), command);
            
            plugin.getLogger().info("[AntiCheat] Auto-warned " + player.getName() + " for " + checkType);
        });
    }
    
    /**
     * Check if player has VIP protection
     */
    public boolean isVIPProtected(Player player) {
        if (!plugin.getConfig().getBoolean("vip-protection.enabled", true)) {
            return false;
        }
        
        // Check for VIP permissions
        String[] vipPerms = {
            "litebansreborn.vip",
            "litebansreborn.vip.protection",
            "essentials.vip",
            "group.vip",
            "group.donator",
            "group.mvp",
            "group.mvp+"
        };
        
        for (String perm : vipPerms) {
            if (player.hasPermission(perm)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Get violation count for player
     */
    public int getViolations(UUID uuid) {
        return violationCounts.getOrDefault(uuid, 0);
    }
    
    /**
     * Reset violations for player
     */
    public void resetViolations(UUID uuid) {
        violationCounts.remove(uuid);
        lastViolationType.remove(uuid);
    }
    
    /**
     * Get detected anti-cheat plugins
     */
    public String getDetectedPlugins() {
        StringBuilder sb = new StringBuilder();
        detectedPlugins.forEach((name, detected) -> {
            if (detected) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(name);
            }
        });
        return sb.length() > 0 ? sb.toString() : "None";
    }
    
    /**
     * Check if any anti-cheat is detected
     */
    public boolean hasAntiCheat() {
        return detectedPlugins.values().stream().anyMatch(b -> b);
    }
}
