package com.nuvik.litebansreborn.utils;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * Utility class for player-related operations
 */
public class PlayerUtil {
    
    private static final Pattern UUID_PATTERN = Pattern.compile(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );
    
    private static final Pattern IP_PATTERN = Pattern.compile(
        "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$"
    );
    
    // Console UUID - represents console/server
    public static final UUID CONSOLE_UUID = new UUID(0, 0);
    public static final String CONSOLE_NAME = "Console";
    
    /**
     * Get a player's IP address
     */
    public static String getPlayerIP(Player player) {
        if (player == null) {
            return null;
        }
        
        InetSocketAddress address = player.getAddress();
        if (address == null) {
            return null;
        }
        
        InetAddress inetAddress = address.getAddress();
        if (inetAddress == null) {
            return null;
        }
        
        return inetAddress.getHostAddress();
    }
    
    /**
     * Get a player by name (online or offline)
     */
    @SuppressWarnings("deprecation")
    public static OfflinePlayer getOfflinePlayer(String name) {
        // First check online players
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online;
        }
        
        // Get offline player
        return Bukkit.getOfflinePlayer(name);
    }
    
    /**
     * Get a player by UUID (online or offline)
     */
    public static OfflinePlayer getOfflinePlayer(UUID uuid) {
        return Bukkit.getOfflinePlayer(uuid);
    }
    
    /**
     * Check if a player is online
     */
    public static boolean isOnline(UUID uuid) {
        return Bukkit.getPlayer(uuid) != null;
    }
    
    /**
     * Check if a player is online
     */
    public static boolean isOnline(String name) {
        return Bukkit.getPlayerExact(name) != null;
    }
    
    /**
     * Get online player by name
     */
    public static Player getOnlinePlayer(String name) {
        return Bukkit.getPlayerExact(name);
    }
    
    /**
     * Get online player by UUID
     */
    public static Player getOnlinePlayer(UUID uuid) {
        return Bukkit.getPlayer(uuid);
    }
    
    /**
     * Check if a string is a valid UUID
     */
    public static boolean isUUID(String input) {
        if (input == null) {
            return false;
        }
        return UUID_PATTERN.matcher(input).matches();
    }
    
    /**
     * Parse a UUID from string
     */
    public static UUID parseUUID(String input) {
        if (input == null) {
            return null;
        }
        try {
            return UUID.fromString(input);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    /**
     * Check if a string is a valid IP address
     */
    public static boolean isIP(String input) {
        if (input == null) {
            return false;
        }
        return IP_PATTERN.matcher(input).matches();
    }
    
    /**
     * Get the executor UUID (console or player)
     */
    public static UUID getExecutorUUID(org.bukkit.command.CommandSender sender) {
        if (sender instanceof Player) {
            return ((Player) sender).getUniqueId();
        }
        return CONSOLE_UUID;
    }
    
    /**
     * Get the executor name (console or player)
     */
    public static String getExecutorName(org.bukkit.command.CommandSender sender) {
        if (sender instanceof Player) {
            return sender.getName();
        }
        return CONSOLE_NAME;
    }
    
    /**
     * Check if sender is console
     */
    public static boolean isConsole(org.bukkit.command.CommandSender sender) {
        return !(sender instanceof Player);
    }
    
    /**
     * Get display name or regular name
     */
    public static String getDisplayName(Player player) {
        if (player == null) {
            return "Unknown";
        }
        String displayName = player.getDisplayName();
        return displayName != null ? displayName : player.getName();
    }
    
    /**
     * Check if a player has ever played on the server
     */
    public static boolean hasPlayed(String name) {
        OfflinePlayer player = getOfflinePlayer(name);
        return player != null && player.hasPlayedBefore();
    }
    
    /**
     * Check if a player has ever played on the server
     */
    public static boolean hasPlayed(UUID uuid) {
        OfflinePlayer player = getOfflinePlayer(uuid);
        return player != null && player.hasPlayedBefore();
    }
    
    /**
     * Get UUID from player name (async - uses Mojang API for offline players)
     */
    public static CompletableFuture<UUID> getUUID(String name) {
        return CompletableFuture.supplyAsync(() -> {
            // First check if player is online
            Player online = Bukkit.getPlayerExact(name);
            if (online != null) {
                return online.getUniqueId();
            }
            
            // Check cached offline player
            OfflinePlayer offline = getOfflinePlayer(name);
            if (offline.hasPlayedBefore()) {
                return offline.getUniqueId();
            }
            
            // TODO: Implement Mojang API lookup for players who haven't played before
            return null;
        });
    }
    
    /**
     * Mask an IP address for privacy (e.g., 192.168.1.100 -> 192.168.*.*)
     */
    public static String maskIP(String ip) {
        if (ip == null || !isIP(ip)) {
            return ip;
        }
        
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return ip;
        }
        
        return parts[0] + "." + parts[1] + ".*.*";
    }
    
    /**
     * Get the first two octets of an IP (for alt detection)
     */
    public static String getIPRange(String ip) {
        if (ip == null || !isIP(ip)) {
            return null;
        }
        
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return null;
        }
        
        return parts[0] + "." + parts[1] + "." + parts[2];
    }
}
