package com.nuvik.litebansreborn.gui;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.managers.ReportManager;
import com.nuvik.litebansreborn.models.Report;
import com.nuvik.litebansreborn.utils.ColorUtil;
import com.nuvik.litebansreborn.utils.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Reports GUI - Beautiful and fully configurable reports management interface
 * 
 * @author Nuvik
 * @version 6.0.0
 */
public class ReportsGUI implements Listener {

    private final LiteBansReborn plugin;
    private FileConfiguration config;
    private boolean enabled = true;
    
    // Track open GUIs
    private final Map<UUID, ReportsGUISession> openSessions = new ConcurrentHashMap<>();
    
    public ReportsGUI(LiteBansReborn plugin) {
        this.plugin = plugin;
        loadConfig();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        
        // Start auto-refresh task if enabled
        if (config.getBoolean("auto-refresh.enabled", true)) {
            startAutoRefresh();
        }
    }
    
    private void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "reports.yml");
        
        if (!configFile.exists()) {
            plugin.saveResource("reports.yml", false);
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
        enabled = config.getBoolean("gui.enabled", true);
    }
    
    public void reload() {
        loadConfig();
    }
    
    /**
     * Open the reports GUI for a player
     */
    public void openReportsGUI(Player player) {
        openReportsGUI(player, 1, null, null);
    }
    
    /**
     * Open the reports GUI with specific page and filters
     */
    public void openReportsGUI(Player player, int page, String statusFilter, String priorityFilter) {
        if (!enabled) {
            plugin.getMessagesManager().send(player, "general.feature-disabled");
            return;
        }
        
        // Create session
        ReportsGUISession session = new ReportsGUISession(player.getUniqueId(), page, statusFilter, priorityFilter);
        openSessions.put(player.getUniqueId(), session);
        
        // Load reports asynchronously
        int offset = (page - 1) * getReportsPerPage();
        plugin.getReportManager().getPendingReports(getReportsPerPage(), offset).thenAccept(reports -> {
            plugin.getReportManager().getPendingReportsCount().thenAccept(total -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    buildAndOpenGUI(player, session, reports, total);
                });
            });
        });
    }
    
    private void buildAndOpenGUI(Player player, ReportsGUISession session, List<Report> reports, int totalReports) {
        int rows = config.getInt("gui.rows", 6);
        String title = ColorUtil.translate(config.getString("gui.title", "&c&l🛡️ REPORTS PANEL"));
        
        ReportsInventoryHolder holder = new ReportsInventoryHolder(session);
        Inventory inv = Bukkit.createInventory(holder, rows * 9, title);
        holder.setInventory(inv);
        
        // Fill background
        if (config.getBoolean("gui.background.enabled", true)) {
            fillBackground(inv);
        }
        
        // Add border
        if (config.getBoolean("gui.border.enabled", true)) {
            addBorder(inv, rows);
        }
        
        // Add reports
        int slot = 10; // Start after first row + 1
        int reportsAdded = 0;
        
        for (Report report : reports) {
            if (reportsAdded >= getReportsPerPage()) break;
            
            // Skip slots in rows that are part of border
            while (slot % 9 == 0 || slot % 9 == 8) {
                slot++;
            }
            
            if (slot >= (rows - 1) * 9) break; // Don't go into bottom row
            
            ItemStack reportItem = createReportItem(report);
            inv.setItem(slot, reportItem);
            session.getSlotToReportId().put(slot, (int) report.getId());
            
            slot++;
            reportsAdded++;
        }
        
        // Add navigation buttons
        addNavigationButtons(inv, session, totalReports);
        
        // Add filter buttons
        addFilterButtons(inv, session);
        
        // Play sound
        playSound(player, "open-menu");
        
        player.openInventory(inv);
    }
    
    private void fillBackground(Inventory inv) {
        Material material = Material.valueOf(config.getString("gui.background.material", "BLACK_STAINED_GLASS_PANE"));
        String name = config.getString("gui.background.name", " ");
        ItemStack background = createItem(material, name, null);
        
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, background);
        }
    }
    
    private void addBorder(Inventory inv, int rows) {
        Material material = Material.valueOf(config.getString("gui.border.material", "GRAY_STAINED_GLASS_PANE"));
        String name = config.getString("gui.border.name", " ");
        ItemStack border = createItem(material, name, null);
        
        // Top row
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, border);
        }
        
        // Bottom row
        for (int i = (rows - 1) * 9; i < rows * 9; i++) {
            inv.setItem(i, border);
        }
        
        // Sides
        for (int i = 1; i < rows - 1; i++) {
            inv.setItem(i * 9, border);
            inv.setItem(i * 9 + 8, border);
        }
    }
    
    private ItemStack createReportItem(Report report) {
        String status = report.getStatus() != null ? report.getStatus().getId() : "pending";
        String materialName = config.getString("report-item.materials." + status, "PAPER");
        Material material;
        try {
            material = Material.valueOf(materialName);
        } catch (Exception e) {
            material = Material.PAPER;
        }
        
        String name = config.getString("report-item.name", "&e&l⚠ Report #%id%")
            .replace("%id%", String.valueOf(report.getId()));
        
        List<String> lore = new ArrayList<>();
        for (String line : config.getStringList("report-item.lore")) {
            lore.add(ColorUtil.translate(replacePlaceholders(line, report)));
        }
        
        return createItem(material, name, lore);
    }
    
    private String replacePlaceholders(String text, Report report) {
        String status = report.getStatus() != null ? report.getStatus().getId() : "pending";
        String statusText = config.getString("report-item.status-colors." + status, "&7" + status);
        
        return text
            .replace("%id%", String.valueOf(report.getId()))
            .replace("%reported%", report.getReportedName() != null ? report.getReportedName() : "Unknown")
            .replace("%reporter%", report.getReporterName() != null ? report.getReporterName() : "Unknown")
            .replace("%reason%", report.getReason() != null ? report.getReason() : "No reason")
            .replace("%time%", report.getCreatedAt() != null ? TimeUtil.formatDate(report.getCreatedAt()) : "Unknown")
            .replace("%server%", "Main")
            .replace("%priority%", "Normal")
            .replace("%priority_color%", "&e")
            .replace("%status%", status)
            .replace("%status_color%", statusText);
    }
    
    private void addNavigationButtons(Inventory inv, ReportsGUISession session, int totalReports) {
        int totalPages = Math.max(1, (int) Math.ceil((double) totalReports / getReportsPerPage()));
        int currentPage = session.getPage();
        
        // Previous page
        if (currentPage > 1) {
            int slot = config.getInt("navigation.previous-page.slot", 45);
            Material material = Material.valueOf(config.getString("navigation.previous-page.material", "ARROW"));
            String name = config.getString("navigation.previous-page.name", "&a← Previous Page");
            List<String> lore = config.getStringList("navigation.previous-page.lore");
            lore = replacePaginationPlaceholders(lore, currentPage, totalPages);
            inv.setItem(slot, createItem(material, name, lore));
        }
        
        // Next page
        if (currentPage < totalPages) {
            int slot = config.getInt("navigation.next-page.slot", 53);
            Material material = Material.valueOf(config.getString("navigation.next-page.material", "ARROW"));
            String name = config.getString("navigation.next-page.name", "&a→ Next Page");
            List<String> lore = config.getStringList("navigation.next-page.lore");
            lore = replacePaginationPlaceholders(lore, currentPage, totalPages);
            inv.setItem(slot, createItem(material, name, lore));
        }
        
        // Refresh
        int refreshSlot = config.getInt("navigation.refresh.slot", 49);
        Material refreshMaterial = Material.valueOf(config.getString("navigation.refresh.material", "SUNFLOWER"));
        String refreshName = config.getString("navigation.refresh.name", "&e⟳ Refresh");
        inv.setItem(refreshSlot, createItem(refreshMaterial, refreshName, config.getStringList("navigation.refresh.lore")));
        
        // Close
        int closeSlot = config.getInt("navigation.close.slot", 47);
        Material closeMaterial = Material.valueOf(config.getString("navigation.close.material", "BARRIER"));
        String closeName = config.getString("navigation.close.name", "&c✖ Close");
        inv.setItem(closeSlot, createItem(closeMaterial, closeName, config.getStringList("navigation.close.lore")));
    }
    
    private void addFilterButtons(Inventory inv, ReportsGUISession session) {
        // All filter
        int allSlot = config.getInt("filters.all.slot", 46);
        Material allMaterial = Material.valueOf(config.getString("filters.all.material", "COMPASS"));
        inv.setItem(allSlot, createItem(allMaterial, 
            config.getString("filters.all.name", "&f🔍 All Reports"),
            config.getStringList("filters.all.lore")));
    }
    
    private List<String> replacePaginationPlaceholders(List<String> lore, int current, int total) {
        List<String> result = new ArrayList<>();
        for (String line : lore) {
            result.add(ColorUtil.translate(line
                .replace("%current%", String.valueOf(current))
                .replace("%total%", String.valueOf(total))));
        }
        return result;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        InventoryHolder holder = event.getInventory().getHolder();
        
        if (holder instanceof ReportsInventoryHolder reportsHolder) {
            event.setCancelled(true);
            handleReportsClick(player, reportsHolder.getSession(), event);
        }
    }
    
    private void handleReportsClick(Player player, ReportsGUISession session, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        ClickType click = event.getClick();
        
        // Check if it's a report slot
        if (session.getSlotToReportId().containsKey(slot)) {
            int reportId = session.getSlotToReportId().get(slot);
            
            if (click == ClickType.LEFT) {
                // Handle the report
                handleReport(player, reportId);
            } else if (click == ClickType.RIGHT) {
                // Quick dismiss
                dismissReport(player, reportId);
            }
            return;
        }
        
        // Navigation buttons
        int prevSlot = config.getInt("navigation.previous-page.slot", 45);
        int nextSlot = config.getInt("navigation.next-page.slot", 53);
        int refreshSlot = config.getInt("navigation.refresh.slot", 49);
        int closeSlot = config.getInt("navigation.close.slot", 47);
        
        if (slot == prevSlot && session.getPage() > 1) {
            playSound(player, "click");
            openReportsGUI(player, session.getPage() - 1, session.getStatusFilter(), session.getPriorityFilter());
        } else if (slot == nextSlot) {
            playSound(player, "click");
            openReportsGUI(player, session.getPage() + 1, session.getStatusFilter(), session.getPriorityFilter());
        } else if (slot == refreshSlot) {
            playSound(player, "click");
            openReportsGUI(player, session.getPage(), session.getStatusFilter(), session.getPriorityFilter());
        } else if (slot == closeSlot) {
            player.closeInventory();
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            if (event.getInventory().getHolder() instanceof ReportsInventoryHolder) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!(player.getOpenInventory().getTopInventory().getHolder() instanceof ReportsInventoryHolder)) {
                        openSessions.remove(player.getUniqueId());
                    }
                }, 5L);
            }
        }
    }
    
    private void handleReport(Player player, int reportId) {
        plugin.getReportManager().handleReport(reportId, "RESOLVED", player.getUniqueId(), player.getName(), "Handled via GUI");
        playSound(player, "success");
        player.sendMessage(ColorUtil.translate("&aReport #" + reportId + " has been handled."));
        openReportsGUI(player);
    }
    
    private void dismissReport(Player player, int reportId) {
        plugin.getReportManager().handleReport(reportId, "DISMISSED", player.getUniqueId(), player.getName(), "Dismissed via GUI");
        playSound(player, "click");
        player.sendMessage(ColorUtil.translate("&7Report #" + reportId + " has been dismissed."));
        openReportsGUI(player);
    }
    
    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(ColorUtil.translate(name));
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(lore.stream().map(ColorUtil::translate).toList());
            }
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    private void playSound(Player player, String soundKey) {
        try {
            String soundName = config.getString("sounds." + soundKey, "UI_BUTTON_CLICK");
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (Exception ignored) {}
    }
    
    private int getReportsPerPage() {
        int rows = config.getInt("gui.rows", 6);
        return (rows - 2) * 7; // Exclude border rows and columns
    }
    
    private void startAutoRefresh() {
        long interval = config.getLong("auto-refresh.interval-seconds", 30) * 20L;
        
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (UUID uuid : openSessions.keySet()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    ReportsGUISession session = openSessions.get(uuid);
                    if (session != null && player.getOpenInventory().getTopInventory().getHolder() instanceof ReportsInventoryHolder) {
                        openReportsGUI(player, session.getPage(), session.getStatusFilter(), session.getPriorityFilter());
                    }
                }
            }
        }, interval, interval);
    }
    
    // ==================== INNER CLASSES ====================
    
    public static class ReportsGUISession {
        private final UUID playerUuid;
        private int page;
        private String statusFilter;
        private String priorityFilter;
        private final Map<Integer, Integer> slotToReportId = new HashMap<>();
        
        public ReportsGUISession(UUID playerUuid, int page, String statusFilter, String priorityFilter) {
            this.playerUuid = playerUuid;
            this.page = page;
            this.statusFilter = statusFilter;
            this.priorityFilter = priorityFilter;
        }
        
        public UUID getPlayerUuid() { return playerUuid; }
        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }
        public String getStatusFilter() { return statusFilter; }
        public void setStatusFilter(String filter) { this.statusFilter = filter; }
        public String getPriorityFilter() { return priorityFilter; }
        public void setPriorityFilter(String filter) { this.priorityFilter = filter; }
        public Map<Integer, Integer> getSlotToReportId() { return slotToReportId; }
    }
    
    private static class ReportsInventoryHolder implements InventoryHolder {
        private final ReportsGUISession session;
        private Inventory inventory;
        
        public ReportsInventoryHolder(ReportsGUISession session) {
            this.session = session;
        }
        
        public ReportsGUISession getSession() { return session; }
        public void setInventory(Inventory inv) { this.inventory = inv; }
        
        @Override
        public @NotNull Inventory getInventory() { return inventory; }
    }
}
