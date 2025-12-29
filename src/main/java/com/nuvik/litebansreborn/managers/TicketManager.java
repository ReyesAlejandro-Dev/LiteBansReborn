package com.nuvik.litebansreborn.managers;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Ticket Manager - Support ticket system
 * Features:
 * - Create tickets for support/appeals
 * - Staff can claim and respond
 * - Discord integration
 * - Categories (Appeal, Report, Support)
 */
public class TicketManager {

    private final LiteBansReborn plugin;
    private final Map<Integer, Ticket> ticketCache = new ConcurrentHashMap<>();

    public TicketManager(LiteBansReborn plugin) {
        this.plugin = plugin;
        initializeDatabase();
    }

    /**
     * Ticket status
     */
    public enum TicketStatus {
        OPEN,
        CLAIMED,
        WAITING_RESPONSE,
        CLOSED,
        RESOLVED
    }

    /**
     * Ticket category
     */
    public enum TicketCategory {
        APPEAL("Appeal", Material.BOOK, "Appeal a punishment"),
        REPORT("Report", Material.PAPER, "Report a player"),
        SUPPORT("Support", Material.COMPASS, "General support"),
        BUG("Bug Report", Material.BARRIER, "Report a bug"),
        OTHER("Other", Material.NAME_TAG, "Other inquiries");

        private final String displayName;
        private final Material icon;
        private final String description;

        TicketCategory(String displayName, Material icon, String description) {
            this.displayName = displayName;
            this.icon = icon;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public Material getIcon() { return icon; }
        public String getDescription() { return description; }
    }

    /**
     * Ticket class
     */
    public static class Ticket {
        private int id;
        private UUID playerUUID;
        private String playerName;
        private TicketCategory category;
        private TicketStatus status;
        private String subject;
        private UUID claimedBy;
        private String claimedByName;
        private long createdAt;
        private long updatedAt;
        private long closedAt;
        private Long punishmentId;  // For appeals
        private List<TicketMessage> messages = new ArrayList<>();

        // Getters and setters
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public UUID getPlayerUUID() { return playerUUID; }
        public void setPlayerUUID(UUID playerUUID) { this.playerUUID = playerUUID; }
        public String getPlayerName() { return playerName; }
        public void setPlayerName(String playerName) { this.playerName = playerName; }
        public TicketCategory getCategory() { return category; }
        public void setCategory(TicketCategory category) { this.category = category; }
        public TicketStatus getStatus() { return status; }
        public void setStatus(TicketStatus status) { this.status = status; }
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        public UUID getClaimedBy() { return claimedBy; }
        public void setClaimedBy(UUID claimedBy) { this.claimedBy = claimedBy; }
        public String getClaimedByName() { return claimedByName; }
        public void setClaimedByName(String claimedByName) { this.claimedByName = claimedByName; }
        public long getCreatedAt() { return createdAt; }
        public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
        public long getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
        public long getClosedAt() { return closedAt; }
        public void setClosedAt(long closedAt) { this.closedAt = closedAt; }
        public Long getPunishmentId() { return punishmentId; }
        public void setPunishmentId(Long punishmentId) { this.punishmentId = punishmentId; }
        public List<TicketMessage> getMessages() { return messages; }
        public void addMessage(TicketMessage message) { messages.add(message); }
    }

    /**
     * Ticket message
     */
    public static class TicketMessage {
        private int id;
        private int ticketId;
        private UUID authorUUID;
        private String authorName;
        private String message;
        private long timestamp;
        private boolean isStaff;

        // Getters and setters
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public int getTicketId() { return ticketId; }
        public void setTicketId(int ticketId) { this.ticketId = ticketId; }
        public UUID getAuthorUUID() { return authorUUID; }
        public void setAuthorUUID(UUID authorUUID) { this.authorUUID = authorUUID; }
        public String getAuthorName() { return authorName; }
        public void setAuthorName(String authorName) { this.authorName = authorName; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        public boolean isStaff() { return isStaff; }
        public void setStaff(boolean staff) { isStaff = staff; }
    }

    // ==================== DATABASE ====================

    private void initializeDatabase() {
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            
            // Tickets table
            String ticketTable = "CREATE TABLE IF NOT EXISTS " + plugin.getDatabaseManager().getTable("tickets") + " (" +
                "id INTEGER PRIMARY KEY AUTO_INCREMENT, " +
                "player_uuid VARCHAR(36), " +
                "player_name VARCHAR(16), " +
                "category VARCHAR(32), " +
                "status VARCHAR(32), " +
                "subject TEXT, " +
                "claimed_by_uuid VARCHAR(36), " +
                "claimed_by_name VARCHAR(16), " +
                "punishment_id BIGINT, " +
                "created_at BIGINT, " +
                "updated_at BIGINT, " +
                "closed_at BIGINT" +
                ")";
            
            // Messages table
            String messageTable = "CREATE TABLE IF NOT EXISTS " + plugin.getDatabaseManager().getTable("ticket_messages") + " (" +
                "id INTEGER PRIMARY KEY AUTO_INCREMENT, " +
                "ticket_id INTEGER, " +
                "author_uuid VARCHAR(36), " +
                "author_name VARCHAR(16), " +
                "message TEXT, " +
                "is_staff BOOLEAN, " +
                "timestamp BIGINT" +
                ")";
            
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(ticketTable);
                stmt.execute(messageTable);
            }
        } catch (Exception e) {
            plugin.log(Level.WARNING, "Failed to initialize ticket tables: " + e.getMessage());
        }
    }

    // ==================== TICKET OPERATIONS ====================

    /**
     * Create a new ticket
     */
    public CompletableFuture<Ticket> createTicket(UUID playerUUID, String playerName, TicketCategory category, String subject) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Ticket ticket = new Ticket();
                ticket.setPlayerUUID(playerUUID);
                ticket.setPlayerName(playerName);
                ticket.setCategory(category);
                ticket.setStatus(TicketStatus.OPEN);
                ticket.setSubject(subject);
                ticket.setCreatedAt(System.currentTimeMillis());
                ticket.setUpdatedAt(System.currentTimeMillis());

                Connection conn = plugin.getDatabaseManager().getConnection();
                String sql = "INSERT INTO " + plugin.getDatabaseManager().getTable("tickets") +
                    " (player_uuid, player_name, category, status, subject, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";

                try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setString(1, playerUUID.toString());
                    stmt.setString(2, playerName);
                    stmt.setString(3, category.name());
                    stmt.setString(4, TicketStatus.OPEN.name());
                    stmt.setString(5, subject);
                    stmt.setLong(6, ticket.getCreatedAt());
                    stmt.setLong(7, ticket.getUpdatedAt());
                    stmt.executeUpdate();

                    try (ResultSet rs = stmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            ticket.setId(rs.getInt(1));
                        }
                    }
                }

                ticketCache.put(ticket.getId(), ticket);
                
                // Notify staff
                notifyStaff(ticket);
                
                return ticket;
            } catch (Exception e) {
                plugin.log(Level.SEVERE, "Failed to create ticket: " + e.getMessage());
                return null;
            }
        });
    }

    /**
     * Add message to ticket
     */
    public CompletableFuture<Boolean> addMessage(int ticketId, UUID authorUUID, String authorName, String message, boolean isStaff) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                TicketMessage msg = new TicketMessage();
                msg.setTicketId(ticketId);
                msg.setAuthorUUID(authorUUID);
                msg.setAuthorName(authorName);
                msg.setMessage(message);
                msg.setStaff(isStaff);
                msg.setTimestamp(System.currentTimeMillis());

                Connection conn = plugin.getDatabaseManager().getConnection();
                String sql = "INSERT INTO " + plugin.getDatabaseManager().getTable("ticket_messages") +
                    " (ticket_id, author_uuid, author_name, message, is_staff, timestamp) VALUES (?, ?, ?, ?, ?, ?)";

                try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setInt(1, ticketId);
                    stmt.setString(2, authorUUID.toString());
                    stmt.setString(3, authorName);
                    stmt.setString(4, message);
                    stmt.setBoolean(5, isStaff);
                    stmt.setLong(6, msg.getTimestamp());
                    stmt.executeUpdate();

                    try (ResultSet rs = stmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            msg.setId(rs.getInt(1));
                        }
                    }
                }

                // Update ticket
                updateTicketStatus(ticketId, isStaff ? TicketStatus.WAITING_RESPONSE : TicketStatus.OPEN);
                
                // Add to cache
                Ticket ticket = ticketCache.get(ticketId);
                if (ticket != null) {
                    ticket.addMessage(msg);
                }

                // Notify other party
                notifyParty(ticketId, authorUUID, isStaff);
                
                return true;
            } catch (Exception e) {
                plugin.log(Level.SEVERE, "Failed to add message: " + e.getMessage());
                return false;
            }
        });
    }

    /**
     * Claim a ticket
     */
    public CompletableFuture<Boolean> claimTicket(int ticketId, UUID staffUUID, String staffName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Connection conn = plugin.getDatabaseManager().getConnection();
                String sql = "UPDATE " + plugin.getDatabaseManager().getTable("tickets") +
                    " SET claimed_by_uuid = ?, claimed_by_name = ?, status = ?, updated_at = ? WHERE id = ?";

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, staffUUID.toString());
                    stmt.setString(2, staffName);
                    stmt.setString(3, TicketStatus.CLAIMED.name());
                    stmt.setLong(4, System.currentTimeMillis());
                    stmt.setInt(5, ticketId);
                    stmt.executeUpdate();
                }

                Ticket ticket = ticketCache.get(ticketId);
                if (ticket != null) {
                    ticket.setClaimedBy(staffUUID);
                    ticket.setClaimedByName(staffName);
                    ticket.setStatus(TicketStatus.CLAIMED);
                }

                return true;
            } catch (Exception e) {
                plugin.log(Level.SEVERE, "Failed to claim ticket: " + e.getMessage());
                return false;
            }
        });
    }

    /**
     * Close a ticket
     */
    public CompletableFuture<Boolean> closeTicket(int ticketId, TicketStatus status) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Connection conn = plugin.getDatabaseManager().getConnection();
                String sql = "UPDATE " + plugin.getDatabaseManager().getTable("tickets") +
                    " SET status = ?, closed_at = ?, updated_at = ? WHERE id = ?";

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, status.name());
                    stmt.setLong(2, System.currentTimeMillis());
                    stmt.setLong(3, System.currentTimeMillis());
                    stmt.setInt(4, ticketId);
                    stmt.executeUpdate();
                }

                Ticket ticket = ticketCache.get(ticketId);
                if (ticket != null) {
                    ticket.setStatus(status);
                    ticket.setClosedAt(System.currentTimeMillis());
                }

                return true;
            } catch (Exception e) {
                plugin.log(Level.SEVERE, "Failed to close ticket: " + e.getMessage());
                return false;
            }
        });
    }

    /**
     * Update ticket status
     */
    private void updateTicketStatus(int ticketId, TicketStatus status) {
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            String sql = "UPDATE " + plugin.getDatabaseManager().getTable("tickets") +
                " SET status = ?, updated_at = ? WHERE id = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, status.name());
                stmt.setLong(2, System.currentTimeMillis());
                stmt.setInt(3, ticketId);
                stmt.executeUpdate();
            }

            Ticket ticket = ticketCache.get(ticketId);
            if (ticket != null) {
                ticket.setStatus(status);
            }
        } catch (Exception e) {
            plugin.log(Level.WARNING, "Failed to update ticket status: " + e.getMessage());
        }
    }

    /**
     * Get open tickets
     */
    public CompletableFuture<List<Ticket>> getOpenTickets() {
        return CompletableFuture.supplyAsync(() -> {
            List<Ticket> tickets = new ArrayList<>();
            try {
                Connection conn = plugin.getDatabaseManager().getConnection();
                String sql = "SELECT * FROM " + plugin.getDatabaseManager().getTable("tickets") +
                    " WHERE status IN ('OPEN', 'CLAIMED', 'WAITING_RESPONSE') ORDER BY created_at DESC";

                try (PreparedStatement stmt = conn.prepareStatement(sql);
                     ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        tickets.add(loadTicketFromRS(rs));
                    }
                }
            } catch (Exception e) {
                plugin.log(Level.SEVERE, "Failed to get open tickets: " + e.getMessage());
            }
            return tickets;
        });
    }

    /**
     * Get player tickets
     */
    public CompletableFuture<List<Ticket>> getPlayerTickets(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            List<Ticket> tickets = new ArrayList<>();
            try {
                Connection conn = plugin.getDatabaseManager().getConnection();
                String sql = "SELECT * FROM " + plugin.getDatabaseManager().getTable("tickets") +
                    " WHERE player_uuid = ? ORDER BY created_at DESC";

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, playerUUID.toString());
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            tickets.add(loadTicketFromRS(rs));
                        }
                    }
                }
            } catch (Exception e) {
                plugin.log(Level.SEVERE, "Failed to get player tickets: " + e.getMessage());
            }
            return tickets;
        });
    }

    /**
     * Get ticket by ID
     */
    public CompletableFuture<Ticket> getTicket(int ticketId) {
        Ticket cached = ticketCache.get(ticketId);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                Connection conn = plugin.getDatabaseManager().getConnection();
                String sql = "SELECT * FROM " + plugin.getDatabaseManager().getTable("tickets") +
                    " WHERE id = ?";

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, ticketId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            Ticket ticket = loadTicketFromRS(rs);
                            loadTicketMessages(ticket);
                            ticketCache.put(ticketId, ticket);
                            return ticket;
                        }
                    }
                }
            } catch (Exception e) {
                plugin.log(Level.SEVERE, "Failed to get ticket: " + e.getMessage());
            }
            return null;
        });
    }

    private Ticket loadTicketFromRS(ResultSet rs) throws SQLException {
        Ticket ticket = new Ticket();
        ticket.setId(rs.getInt("id"));
        ticket.setPlayerUUID(UUID.fromString(rs.getString("player_uuid")));
        ticket.setPlayerName(rs.getString("player_name"));
        ticket.setCategory(TicketCategory.valueOf(rs.getString("category")));
        ticket.setStatus(TicketStatus.valueOf(rs.getString("status")));
        ticket.setSubject(rs.getString("subject"));
        
        String claimedBy = rs.getString("claimed_by_uuid");
        if (claimedBy != null) {
            ticket.setClaimedBy(UUID.fromString(claimedBy));
            ticket.setClaimedByName(rs.getString("claimed_by_name"));
        }
        
        ticket.setCreatedAt(rs.getLong("created_at"));
        ticket.setUpdatedAt(rs.getLong("updated_at"));
        ticket.setClosedAt(rs.getLong("closed_at"));
        
        long punishmentId = rs.getLong("punishment_id");
        if (!rs.wasNull()) {
            ticket.setPunishmentId(punishmentId);
        }
        
        return ticket;
    }

    private void loadTicketMessages(Ticket ticket) {
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            String sql = "SELECT * FROM " + plugin.getDatabaseManager().getTable("ticket_messages") +
                " WHERE ticket_id = ? ORDER BY timestamp ASC";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, ticket.getId());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        TicketMessage msg = new TicketMessage();
                        msg.setId(rs.getInt("id"));
                        msg.setTicketId(rs.getInt("ticket_id"));
                        msg.setAuthorUUID(UUID.fromString(rs.getString("author_uuid")));
                        msg.setAuthorName(rs.getString("author_name"));
                        msg.setMessage(rs.getString("message"));
                        msg.setStaff(rs.getBoolean("is_staff"));
                        msg.setTimestamp(rs.getLong("timestamp"));
                        ticket.addMessage(msg);
                    }
                }
            }
        } catch (Exception e) {
            plugin.log(Level.WARNING, "Failed to load ticket messages: " + e.getMessage());
        }
    }

    // ==================== NOTIFICATIONS ====================

    private void notifyStaff(Ticket ticket) {
        String msg = ColorUtil.translate("&8[&6Ticket&8] &e" + ticket.getPlayerName() + 
            " &7created a new ticket: &f#" + ticket.getId() + " &8- &7" + ticket.getCategory().getDisplayName());
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("litebansreborn.tickets.view")) {
                player.sendMessage(msg);
            }
        }
    }

    private void notifyParty(int ticketId, UUID authorUUID, boolean fromStaff) {
        getTicket(ticketId).thenAccept(ticket -> {
            if (ticket == null) return;
            
            if (fromStaff) {
                // Notify player
                Player player = Bukkit.getPlayer(ticket.getPlayerUUID());
                if (player != null) {
                    player.sendMessage(ColorUtil.translate(
                        "&8[&6Ticket&8] &7Staff responded to your ticket &f#" + ticketId));
                }
            } else {
                // Notify staff
                if (ticket.getClaimedBy() != null) {
                    Player staff = Bukkit.getPlayer(ticket.getClaimedBy());
                    if (staff != null) {
                        staff.sendMessage(ColorUtil.translate(
                            "&8[&6Ticket&8] &7New response on ticket &f#" + ticketId));
                    }
                }
            }
        });
    }

    // ==================== GUI ====================

    /**
     * Open ticket category selection GUI
     */
    public void openCategoryGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, ColorUtil.translate("&8Select Ticket Category"));

        int slot = 10;
        for (TicketCategory category : TicketCategory.values()) {
            ItemStack item = new ItemStack(category.getIcon());
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ColorUtil.translate("&e" + category.getDisplayName()));
            meta.setLore(Arrays.asList(
                ColorUtil.translate("&7" + category.getDescription()),
                "",
                ColorUtil.translate("&aClick to create ticket")
            ));
            item.setItemMeta(meta);
            inv.setItem(slot++, item);
        }

        player.openInventory(inv);
    }

    /**
     * Open tickets list GUI
     */
    public void openTicketsGUI(Player player, boolean staffView) {
        CompletableFuture<List<Ticket>> future = staffView ? 
            getOpenTickets() : getPlayerTickets(player.getUniqueId());

        future.thenAccept(tickets -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                int size = Math.min(54, ((tickets.size() / 9) + 1) * 9);
                size = Math.max(27, size);
                
                Inventory inv = Bukkit.createInventory(null, size, 
                    ColorUtil.translate(staffView ? "&8Open Tickets" : "&8Your Tickets"));

                int slot = 0;
                for (Ticket ticket : tickets) {
                    if (slot >= size - 9) break;
                    
                    Material material = switch (ticket.getStatus()) {
                        case OPEN -> Material.YELLOW_WOOL;
                        case CLAIMED -> Material.ORANGE_WOOL;
                        case WAITING_RESPONSE -> Material.LIGHT_BLUE_WOOL;
                        case CLOSED, RESOLVED -> Material.GREEN_WOOL;
                    };

                    ItemStack item = new ItemStack(material);
                    ItemMeta meta = item.getItemMeta();
                    meta.setDisplayName(ColorUtil.translate("&f#" + ticket.getId() + " &8- &e" + ticket.getSubject()));
                    meta.setLore(Arrays.asList(
                        ColorUtil.translate("&7Category: &f" + ticket.getCategory().getDisplayName()),
                        ColorUtil.translate("&7Status: &f" + ticket.getStatus().name()),
                        ColorUtil.translate("&7Player: &f" + ticket.getPlayerName()),
                        ColorUtil.translate("&7Claimed by: &f" + 
                            (ticket.getClaimedByName() != null ? ticket.getClaimedByName() : "None")),
                        "",
                        ColorUtil.translate("&aClick to view")
                    ));
                    item.setItemMeta(meta);
                    inv.setItem(slot++, item);
                }

                player.openInventory(inv);
            });
        });
    }

    public int getOpenTicketCount() {
        return (int) ticketCache.values().stream()
            .filter(t -> t.getStatus() == TicketStatus.OPEN || t.getStatus() == TicketStatus.CLAIMED)
            .count();
    }
}
