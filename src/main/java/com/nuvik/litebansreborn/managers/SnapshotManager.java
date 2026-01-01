package com.nuvik.litebansreborn.managers;

import com.nuvik.litebansreborn.LiteBansReborn;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

public class SnapshotManager {

    private final LiteBansReborn plugin;
    private final ConcurrentLinkedDeque<String> globalChatBuffer;
    private final int bufferSize;
    // Thread-safe formatters
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    public SnapshotManager(LiteBansReborn plugin) {
        this.plugin = plugin;
        this.bufferSize = plugin.getConfigManager().getInt("snapshots.buffer-size", 100);
        this.globalChatBuffer = new ConcurrentLinkedDeque<>();
    }

    public void addMessage(Player player, String message) {
        if (!plugin.getConfigManager().getBoolean("snapshots.enabled", true)) return;

        String entry = String.format("[%s] %s: %s", 
            LocalDateTime.now().format(TIME_FORMATTER),
            player.getName(), 
            message
        );
        
        globalChatBuffer.add(entry);
        
        // Trim buffer
        while (globalChatBuffer.size() > bufferSize) {
            globalChatBuffer.pollFirst(); // Remove oldest
        }
    }

    public void saveSnapshot(String targetName, String reason, String type) {
        if (!plugin.getConfigManager().getBoolean("snapshots.enabled", true)) return;

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                File folder = new File(plugin.getDataFolder(), "snapshots");
                if (!folder.exists()) folder.mkdirs();

                String timestamp = LocalDateTime.now().format(FILE_DATE_FORMATTER);
                String filename = String.format("%s_%s_%s.txt", timestamp, targetName, type);
                File file = new File(folder, filename);

                try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                    writer.println("=== Punishment Snapshot ===");
                    writer.println("Target: " + targetName);
                    writer.println("Type: " + type);
                    writer.println("Reason: " + reason);
                    writer.println("Time: " + new Date());
                    writer.println("=== Chat Context (Last " + globalChatBuffer.size() + " messages) ===");
                    writer.println("");
                    
                    for (String line : globalChatBuffer) {
                        writer.println(line);
                    }
                    
                    writer.println("");
                    writer.println("=== End of Snapshot ===");
                }
                
                plugin.log(java.util.logging.Level.INFO, "Saved chat snapshot to " + filename);
                
            } catch (IOException e) {
                plugin.log(java.util.logging.Level.SEVERE, "Failed to save chat snapshot: " + e.getMessage());
            }
        });
    }
}
