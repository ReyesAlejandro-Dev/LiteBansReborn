package com.nuvik.litebansreborn.managers;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Evidence Manager - Manages evidence attachments for punishments
 * Supports: Screenshots, video links, replay links, chat logs
 */
public class EvidenceManager {

    private final LiteBansReborn plugin;
    private final File evidenceFolder;
    private final Map<Long, List<Evidence>> evidenceCache;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

    public EvidenceManager(LiteBansReborn plugin) {
        this.plugin = plugin;
        this.evidenceFolder = new File(plugin.getDataFolder(), "evidence");
        this.evidenceCache = new ConcurrentHashMap<>();
        
        if (!evidenceFolder.exists()) {
            evidenceFolder.mkdirs();
        }
    }

    /**
     * Evidence types
     */
    public enum EvidenceType {
        SCREENSHOT("screenshot", Material.PAINTING),
        VIDEO_LINK("video", Material.JUKEBOX),
        REPLAY_LINK("replay", Material.MUSIC_DISC_CAT),
        CHAT_LOG("chat", Material.PAPER),
        INVENTORY_SNAPSHOT("inventory", Material.CHEST),
        NOTE("note", Material.BOOK);

        private final String id;
        private final Material icon;

        EvidenceType(String id, Material icon) {
            this.id = id;
            this.icon = icon;
        }

        public String getId() { return id; }
        public Material getIcon() { return icon; }
    }

    /**
     * Evidence record
     */
    public static class Evidence implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private final String id;
        private final EvidenceType type;
        private final String content; // URL or file path or text
        private final UUID addedBy;
        private final String addedByName;
        private final long timestamp;
        private final String description;

        public Evidence(EvidenceType type, String content, UUID addedBy, String addedByName, String description) {
            this.id = UUID.randomUUID().toString().substring(0, 8);
            this.type = type;
            this.content = content;
            this.addedBy = addedBy;
            this.addedByName = addedByName;
            this.timestamp = System.currentTimeMillis();
            this.description = description;
        }

        public String getId() { return id; }
        public EvidenceType getType() { return type; }
        public String getContent() { return content; }
        public UUID getAddedBy() { return addedBy; }
        public String getAddedByName() { return addedByName; }
        public long getTimestamp() { return timestamp; }
        public String getDescription() { return description; }
    }

    /**
     * Add evidence to a punishment
     */
    public CompletableFuture<Boolean> addEvidence(long punishmentId, Evidence evidence) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Evidence> list = evidenceCache.computeIfAbsent(punishmentId, k -> new ArrayList<>());
                list.add(evidence);
                
                // Save to file
                saveEvidence(punishmentId);
                
                plugin.log(Level.INFO, "Evidence added to punishment #" + punishmentId + " by " + evidence.getAddedByName());
                return true;
            } catch (Exception e) {
                plugin.log(Level.SEVERE, "Failed to add evidence: " + e.getMessage());
                return false;
            }
        });
    }

    /**
     * Get all evidence for a punishment
     */
    public CompletableFuture<List<Evidence>> getEvidence(long punishmentId) {
        return CompletableFuture.supplyAsync(() -> {
            // Check cache first
            if (evidenceCache.containsKey(punishmentId)) {
                return new ArrayList<>(evidenceCache.get(punishmentId));
            }
            
            // Load from file
            return loadEvidence(punishmentId);
        });
    }

    /**
     * Capture inventory snapshot as evidence
     */
    public Evidence captureInventory(Player target, UUID staffUUID, String staffName) {
        StringBuilder inv = new StringBuilder();
        inv.append("=== Inventory Snapshot ===\n");
        inv.append("Player: ").append(target.getName()).append("\n");
        inv.append("Time: ").append(new Date()).append("\n");
        inv.append("Location: ").append(target.getLocation().toString()).append("\n\n");
        
        // Armor
        inv.append("Armor:\n");
        for (ItemStack item : target.getInventory().getArmorContents()) {
            if (item != null && item.getType() != Material.AIR) {
                inv.append("  - ").append(item.getType().name()).append(" x").append(item.getAmount()).append("\n");
            }
        }
        
        // Inventory
        inv.append("\nInventory:\n");
        for (ItemStack item : target.getInventory().getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                inv.append("  - ").append(item.getType().name()).append(" x").append(item.getAmount()).append("\n");
            }
        }
        
        // Ender Chest
        inv.append("\nEnder Chest:\n");
        for (ItemStack item : target.getEnderChest().getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                inv.append("  - ").append(item.getType().name()).append(" x").append(item.getAmount()).append("\n");
            }
        }
        
        // Save to file
        String filename = "inv_" + target.getName() + "_" + dateFormat.format(new Date()) + ".txt";
        File file = new File(evidenceFolder, filename);
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.print(inv.toString());
        } catch (IOException e) {
            plugin.log(Level.SEVERE, "Failed to save inventory snapshot: " + e.getMessage());
        }
        
        return new Evidence(EvidenceType.INVENTORY_SNAPSHOT, filename, staffUUID, staffName, "Auto-captured inventory");
    }

    /**
     * Parse evidence from command flag (-e URL)
     */
    public Evidence parseFromCommand(String url, UUID staffUUID, String staffName) {
        EvidenceType type = detectType(url);
        return new Evidence(type, url, staffUUID, staffName, "Attached via command");
    }

    /**
     * Detect evidence type from URL
     */
    private EvidenceType detectType(String url) {
        url = url.toLowerCase();
        if (url.contains("youtube.com") || url.contains("youtu.be") || url.contains("streamable") || url.contains("medal.tv")) {
            return EvidenceType.VIDEO_LINK;
        } else if (url.contains("replay") || url.contains(".mcpr")) {
            return EvidenceType.REPLAY_LINK;
        } else if (url.endsWith(".png") || url.endsWith(".jpg") || url.endsWith(".gif") || url.contains("imgur")) {
            return EvidenceType.SCREENSHOT;
        }
        return EvidenceType.NOTE;
    }

    /**
     * Open Evidence Viewer GUI
     */
    public void openViewer(Player viewer, long punishmentId, String targetName) {
        getEvidence(punishmentId).thenAccept(evidences -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Inventory gui = Bukkit.createInventory(null, 27, ColorUtil.translate("&8Evidence: &c" + targetName));
                
                // Fill background
                ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
                ItemMeta glassMeta = glass.getItemMeta();
                glassMeta.setDisplayName(" ");
                glass.setItemMeta(glassMeta);
                for (int i = 0; i < 27; i++) {
                    gui.setItem(i, glass);
                }
                
                // Add evidence items
                int slot = 10;
                for (Evidence ev : evidences) {
                    if (slot > 16) break;
                    
                    ItemStack item = new ItemStack(ev.getType().getIcon());
                    ItemMeta meta = item.getItemMeta();
                    meta.setDisplayName(ColorUtil.translate("&e&l" + ev.getType().name()));
                    
                    List<String> lore = new ArrayList<>();
                    lore.add(ColorUtil.translate("&7ID: &f" + ev.getId()));
                    lore.add(ColorUtil.translate("&7Added by: &f" + ev.getAddedByName()));
                    lore.add(ColorUtil.translate("&7Date: &f" + new Date(ev.getTimestamp())));
                    lore.add("");
                    
                    if (ev.getContent().startsWith("http")) {
                        lore.add(ColorUtil.translate("&7Click to copy URL"));
                        lore.add(ColorUtil.translate("&a" + ev.getContent()));
                    } else {
                        lore.add(ColorUtil.translate("&7Content: &f" + ev.getContent()));
                    }
                    
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                    gui.setItem(slot++, item);
                }
                
                // Info item
                ItemStack info = new ItemStack(Material.BOOK);
                ItemMeta infoMeta = info.getItemMeta();
                infoMeta.setDisplayName(ColorUtil.translate("&6&lEvidence Summary"));
                infoMeta.setLore(Arrays.asList(
                    ColorUtil.translate("&7Punishment ID: &f#" + punishmentId),
                    ColorUtil.translate("&7Total Evidence: &f" + evidences.size()),
                    "",
                    ColorUtil.translate("&eClick items to view details")
                ));
                info.setItemMeta(infoMeta);
                gui.setItem(4, info);
                
                viewer.openInventory(gui);
            });
        });
    }

    /**
     * Save evidence to file
     */
    private void saveEvidence(long punishmentId) {
        File file = new File(evidenceFolder, "punishment_" + punishmentId + ".dat");
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(evidenceCache.get(punishmentId));
        } catch (IOException e) {
            plugin.log(Level.SEVERE, "Failed to save evidence: " + e.getMessage());
        }
    }

    /**
     * Load evidence from file
     */
    @SuppressWarnings("unchecked")
    private List<Evidence> loadEvidence(long punishmentId) {
        File file = new File(evidenceFolder, "punishment_" + punishmentId + ".dat");
        if (!file.exists()) {
            return new ArrayList<>();
        }
        
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            List<Evidence> list = (List<Evidence>) ois.readObject();
            evidenceCache.put(punishmentId, list);
            return list;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * Get evidence count for a punishment
     */
    public int getEvidenceCount(long punishmentId) {
        if (evidenceCache.containsKey(punishmentId)) {
            return evidenceCache.get(punishmentId).size();
        }
        return loadEvidence(punishmentId).size();
    }
}
