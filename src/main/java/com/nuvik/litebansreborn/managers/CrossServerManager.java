package com.nuvik.litebansreborn.managers;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.nuvik.litebansreborn.LiteBansReborn;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;

/**
 * Cross-Server Intelligence Manager - v5.1
 * 
 * Syncs punishment data across BungeeCord/Velocity networks:
 * - Real-time ban synchronization
 * - Cross-server alerts
 * - Unified player tracking
 * - Network-wide statistics
 * 
 * @author Nuvik
 * @version 5.4.0
 */
public class CrossServerManager implements PluginMessageListener {

    private final LiteBansReborn plugin;
    private final String CHANNEL = "litebansreborn:sync";
    
    // Network sync settings
    private boolean enabled = false;
    private String syncMode = "plugin-messaging"; // plugin-messaging, redis, mysql
    private String serverName;
    
    // Connected servers cache
    private final Set<String> knownServers = ConcurrentHashMap.newKeySet();
    private final Map<String, Long> serverLastSeen = new ConcurrentHashMap<>();
    
    // Pending sync queue
    private final BlockingQueue<SyncMessage> syncQueue = new LinkedBlockingQueue<>();
    
    public CrossServerManager(LiteBansReborn plugin) {
        this.plugin = plugin;
        loadConfig();
        
        if (enabled) {
            registerChannels();
            startSyncWorker();
            plugin.log(Level.INFO, "§aCross-Server sync enabled! Mode: " + syncMode);
        }
    }
    
    private void loadConfig() {
        enabled = plugin.getConfigManager().getBoolean("cross-server.enabled", false);
        syncMode = plugin.getConfigManager().getString("cross-server.mode", "plugin-messaging");
        serverName = plugin.getConfigManager().getString("cross-server.server-name", "server1");
    }
    
    private void registerChannels() {
        // Register BungeeCord/Velocity plugin messaging channel
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, CHANNEL, this);
        
        // Also register BungeeCord channel for getting server list
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord");
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, "BungeeCord", this);
    }
    
    private void startSyncWorker() {
        // Worker thread to process sync queue
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            while (!syncQueue.isEmpty()) {
                try {
                    SyncMessage msg = syncQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (msg != null) {
                        broadcastMessage(msg);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, 20L, 20L);
        
        // Heartbeat to track server connectivity
        Bukkit.getScheduler().runTaskTimer(plugin, this::sendHeartbeat, 200L, 200L);
    }
    
    // ==================== MESSAGE HANDLING ====================
    
    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (channel.equals("BungeeCord")) {
            handleBungeeCordMessage(message);
            return;
        }
        
        if (!channel.equals(CHANNEL)) return;
        
        try {
            ByteArrayDataInput in = ByteStreams.newDataInput(message);
            String messageType = in.readUTF();
            String sourceServer = in.readUTF();
            
            // Don't process our own messages
            if (sourceServer.equals(serverName)) return;
            
            // Track server
            knownServers.add(sourceServer);
            serverLastSeen.put(sourceServer, System.currentTimeMillis());
            
            switch (messageType) {
                case "BAN" -> handleBanSync(in, sourceServer);
                case "UNBAN" -> handleUnbanSync(in, sourceServer);
                case "ALERT" -> handleAlertSync(in, sourceServer);
                case "HEARTBEAT" -> handleHeartbeat(sourceServer);
                case "PLAYER_JOIN" -> handlePlayerJoinSync(in, sourceServer);
            }
        } catch (Exception e) {
            plugin.log(Level.WARNING, "Failed to process sync message: " + e.getMessage());
        }
    }
    
    private void handleBungeeCordMessage(byte[] message) {
        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subchannel = in.readUTF();
        
        if (subchannel.equals("GetServers")) {
            String[] servers = in.readUTF().split(", ");
            knownServers.addAll(Arrays.asList(servers));
        }
    }
    
    private void handleBanSync(ByteArrayDataInput in, String sourceServer) {
        String playerUuid = in.readUTF();
        String playerName = in.readUTF();
        String reason = in.readUTF();
        String executor = in.readUTF();
        long duration = in.readLong();
        
        plugin.log(Level.INFO, "§e[CrossServer] Ban synced from " + sourceServer + 
                   ": " + playerName + " by " + executor);
        
        // Alert online staff
        alertStaff("§c[§e" + sourceServer + "§c] §f" + playerName + " §7was banned by §f" + 
                   executor + "§7: §f" + reason);
        
        // Kick if online on this server
        Player player = Bukkit.getPlayer(UUID.fromString(playerUuid));
        if (player != null && player.isOnline()) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.kickPlayer("§cYou have been banned on " + sourceServer + "\n§7Reason: " + reason);
            });
        }
    }
    
    private void handleUnbanSync(ByteArrayDataInput in, String sourceServer) {
        String playerUuid = in.readUTF();
        String playerName = in.readUTF();
        String executor = in.readUTF();
        
        plugin.log(Level.INFO, "§a[CrossServer] Unban synced from " + sourceServer + ": " + playerName);
        
        alertStaff("§a[§e" + sourceServer + "§a] §f" + playerName + " §7was unbanned by §f" + executor);
    }
    
    private void handleAlertSync(ByteArrayDataInput in, String sourceServer) {
        String alertType = in.readUTF();
        String message = in.readUTF();
        
        alertStaff("§b[§e" + sourceServer + "§b] " + message);
    }
    
    private void handleHeartbeat(String sourceServer) {
        knownServers.add(sourceServer);
        serverLastSeen.put(sourceServer, System.currentTimeMillis());
    }
    
    private void handlePlayerJoinSync(ByteArrayDataInput in, String sourceServer) {
        String playerUuid = in.readUTF();
        String playerName = in.readUTF();
        int riskScore = in.readInt();
        
        // If high risk player, alert
        if (riskScore >= 70) {
            alertStaff("§c[§e" + sourceServer + "§c] §eHigh-risk player joined: §f" + 
                       playerName + " §7(Risk: §c" + riskScore + "§7)");
        }
    }
    
    // ==================== SYNC BROADCASTING ====================
    
    /**
     * Broadcast a ban to all servers
     */
    public void broadcastBan(UUID playerUuid, String playerName, String reason, 
                             String executor, long duration) {
        if (!enabled) return;
        
        syncQueue.offer(new SyncMessage("BAN", writer -> {
            writer.writeUTF(playerUuid.toString());
            writer.writeUTF(playerName);
            writer.writeUTF(reason);
            writer.writeUTF(executor);
            writer.writeLong(duration);
        }));
    }
    
    /**
     * Broadcast an unban to all servers
     */
    public void broadcastUnban(UUID playerUuid, String playerName, String executor) {
        if (!enabled) return;
        
        syncQueue.offer(new SyncMessage("UNBAN", writer -> {
            writer.writeUTF(playerUuid.toString());
            writer.writeUTF(playerName);
            writer.writeUTF(executor);
        }));
    }
    
    /**
     * Broadcast an alert to all servers
     */
    public void broadcastAlert(String alertType, String message) {
        if (!enabled) return;
        
        syncQueue.offer(new SyncMessage("ALERT", writer -> {
            writer.writeUTF(alertType);
            writer.writeUTF(message);
        }));
    }
    
    /**
     * Broadcast player join with risk score
     */
    public void broadcastPlayerJoin(UUID playerUuid, String playerName, int riskScore) {
        if (!enabled) return;
        
        syncQueue.offer(new SyncMessage("PLAYER_JOIN", writer -> {
            writer.writeUTF(playerUuid.toString());
            writer.writeUTF(playerName);
            writer.writeInt(riskScore);
        }));
    }
    
    private void sendHeartbeat() {
        if (!enabled) return;
        
        syncQueue.offer(new SyncMessage("HEARTBEAT", writer -> {}));
    }
    
    private void broadcastMessage(SyncMessage syncMessage) {
        try {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF(syncMessage.type());
            out.writeUTF(serverName);
            syncMessage.writer().write(out);
            
            byte[] data = out.toByteArray();
            
            // Send via plugin messaging
            Player player = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
            if (player != null) {
                player.sendPluginMessage(plugin, CHANNEL, data);
            }
        } catch (Exception e) {
            plugin.log(Level.WARNING, "Failed to broadcast sync message: " + e.getMessage());
        }
    }
    
    // ==================== UTILITIES ====================
    
    private void alertStaff(String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("litebansreborn.alerts.crossserver")) {
                player.sendMessage(message);
            }
        }
    }
    
    /**
     * Get list of known servers
     */
    public Set<String> getKnownServers() {
        // Remove stale servers (not seen in 5 minutes)
        long now = System.currentTimeMillis();
        serverLastSeen.entrySet().removeIf(entry -> 
            now - entry.getValue() > 300000);
        knownServers.retainAll(serverLastSeen.keySet());
        
        return new HashSet<>(knownServers);
    }
    
    /**
     * Get this server's name
     */
    public String getServerName() {
        return serverName;
    }
    
    /**
     * Check if cross-server sync is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Request server list from BungeeCord
     */
    public void requestServerList() {
        Player player = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
        if (player != null) {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("GetServers");
            player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
        }
    }
    
    public void shutdown() {
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL);
        Bukkit.getMessenger().unregisterIncomingPluginChannel(plugin, CHANNEL);
    }
    
    // ==================== RECORD CLASSES ====================
    
    private record SyncMessage(String type, MessageWriter writer) {}
    
    @FunctionalInterface
    private interface MessageWriter {
        void write(ByteArrayDataOutput out) throws IOException;
    }
}
