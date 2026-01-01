package com.nuvik.litebansreborn.commands;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.managers.TicketManager;
import com.nuvik.litebansreborn.managers.TicketManager.Ticket;
import com.nuvik.litebansreborn.managers.TicketManager.TicketCategory;
import com.nuvik.litebansreborn.managers.TicketManager.TicketStatus;
import com.nuvik.litebansreborn.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Ticket Command - Support ticket system
 * /ticket create <category> <subject> - Create ticket
 * /ticket list - View your tickets
 * /ticket view <id> - View ticket
 * /ticket respond <id> <message> - Respond to ticket
 * /ticket close <id> - Close ticket
 * /ticket claim <id> - Claim ticket (staff)
 * /tickets - View all open tickets (staff)
 */
public class TicketCommand implements CommandExecutor, TabCompleter {

    private final LiteBansReborn plugin;

    public TicketCommand(LiteBansReborn plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtil.translate("&cThis command can only be used by players."));
            return true;
        }

        TicketManager manager = plugin.getTicketManager();

        if (args.length < 1) {
            sendHelp(player);
            return true;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "create", "new" -> handleCreate(player, args);
            case "list", "my" -> handleList(player);
            case "view", "info" -> handleView(player, args);
            case "respond", "reply" -> handleRespond(player, args);
            case "close" -> handleClose(player, args);
            case "claim" -> handleClaim(player, args);
            case "gui", "menu" -> manager.openCategoryGUI(player);
            default -> sendHelp(player);
        }

        return true;
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ColorUtil.translate("&cUsage: /ticket create <category> <subject>"));
            player.sendMessage(ColorUtil.translate("&7Categories: " + 
                Arrays.stream(TicketCategory.values())
                    .map(c -> c.name().toLowerCase())
                    .collect(Collectors.joining(", "))));
            return;
        }

        TicketCategory category;
        try {
            category = TicketCategory.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage(ColorUtil.translate("&cInvalid category. Use: " +
                Arrays.stream(TicketCategory.values())
                    .map(c -> c.name().toLowerCase())
                    .collect(Collectors.joining(", "))));
            return;
        }

        String subject = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        
        if (subject.length() < 5) {
            player.sendMessage(ColorUtil.translate("&cSubject must be at least 5 characters."));
            return;
        }

        player.sendMessage(ColorUtil.translate("&7Creating ticket..."));

        plugin.getTicketManager().createTicket(
            player.getUniqueId(),
            player.getName(),
            category,
            subject
        ).thenAccept(ticket -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (ticket != null) {
                    player.sendMessage(ColorUtil.translate("&a&l✓ Ticket created successfully!"));
                    player.sendMessage(ColorUtil.translate("&7Ticket ID: &f#" + ticket.getId()));
                    player.sendMessage(ColorUtil.translate("&7Staff will respond as soon as possible."));
                    player.sendMessage(ColorUtil.translate("&7Use &e/ticket respond " + ticket.getId() + " <message> &7to add more info."));
                } else {
                    player.sendMessage(ColorUtil.translate("&cFailed to create ticket. Please try again."));
                }
            });
        });
    }

    private void handleList(Player player) {
        plugin.getTicketManager().getPlayerTickets(player.getUniqueId()).thenAccept(tickets -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (tickets.isEmpty()) {
                    player.sendMessage(ColorUtil.translate("&7You don't have any tickets."));
                    player.sendMessage(ColorUtil.translate("&7Use &e/ticket create <category> <subject> &7to create one."));
                    return;
                }

                player.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
                player.sendMessage(ColorUtil.translate("&6&lYour Tickets"));
                player.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));

                for (Ticket ticket : tickets) {
                    String statusColor = switch (ticket.getStatus()) {
                        case OPEN -> "&a";
                        case CLAIMED -> "&e";
                        case WAITING_RESPONSE -> "&b";
                        case CLOSED, RESOLVED -> "&7";
                    };
                    
                    player.sendMessage(ColorUtil.translate(
                        "&f#" + ticket.getId() + " " + statusColor + "[" + ticket.getStatus() + "] &f" + ticket.getSubject()
                    ));
                }

                player.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
            });
        });
    }

    private void handleView(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ColorUtil.translate("&cUsage: /ticket view <id>"));
            return;
        }

        int ticketId;
        try {
            ticketId = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(ColorUtil.translate("&cInvalid ticket ID."));
            return;
        }

        plugin.getTicketManager().getTicket(ticketId).thenAccept(ticket -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (ticket == null) {
                    player.sendMessage(ColorUtil.translate("&cTicket not found."));
                    return;
                }

                // Check permission
                if (!ticket.getPlayerUUID().equals(player.getUniqueId()) && 
                    !player.hasPermission("litebansreborn.tickets.view")) {
                    player.sendMessage(ColorUtil.translate("&cYou don't have permission to view this ticket."));
                    return;
                }

                player.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
                player.sendMessage(ColorUtil.translate("&6&lTicket #" + ticket.getId()));
                player.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
                player.sendMessage(ColorUtil.translate("&7Category: &f" + ticket.getCategory().getDisplayName()));
                player.sendMessage(ColorUtil.translate("&7Status: &f" + ticket.getStatus()));
                player.sendMessage(ColorUtil.translate("&7Player: &f" + ticket.getPlayerName()));
                player.sendMessage(ColorUtil.translate("&7Claimed by: &f" + 
                    (ticket.getClaimedByName() != null ? ticket.getClaimedByName() : "None")));
                player.sendMessage(ColorUtil.translate("&7Subject: &f" + ticket.getSubject()));
                player.sendMessage("");
                player.sendMessage(ColorUtil.translate("&7&lMessages:"));
                
                for (var msg : ticket.getMessages()) {
                    String prefix = msg.isStaff() ? "&c[Staff] " : "&a[Player] ";
                    player.sendMessage(ColorUtil.translate(prefix + "&f" + msg.getAuthorName() + "&7: " + msg.getMessage()));
                }

                player.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
            });
        });
    }

    private void handleRespond(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ColorUtil.translate("&cUsage: /ticket respond <id> <message>"));
            return;
        }

        int ticketId;
        try {
            ticketId = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(ColorUtil.translate("&cInvalid ticket ID."));
            return;
        }

        String message = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        plugin.getTicketManager().getTicket(ticketId).thenAccept(ticket -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (ticket == null) {
                    player.sendMessage(ColorUtil.translate("&cTicket not found."));
                    return;
                }

                // Check permission
                boolean isOwner = ticket.getPlayerUUID().equals(player.getUniqueId());
                boolean isStaff = player.hasPermission("litebansreborn.tickets.respond");
                
                if (!isOwner && !isStaff) {
                    player.sendMessage(ColorUtil.translate("&cYou don't have permission to respond to this ticket."));
                    return;
                }

                if (ticket.getStatus() == TicketStatus.CLOSED || ticket.getStatus() == TicketStatus.RESOLVED) {
                    player.sendMessage(ColorUtil.translate("&cThis ticket is closed."));
                    return;
                }

                plugin.getTicketManager().addMessage(
                    ticketId,
                    player.getUniqueId(),
                    player.getName(),
                    message,
                    isStaff && !isOwner
                ).thenAccept(success -> {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (success) {
                            player.sendMessage(ColorUtil.translate("&aMessage added to ticket #" + ticketId));
                        } else {
                            player.sendMessage(ColorUtil.translate("&cFailed to add message."));
                        }
                    });
                });
            });
        });
    }

    private void handleClose(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ColorUtil.translate("&cUsage: /ticket close <id>"));
            return;
        }

        int ticketId;
        try {
            ticketId = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(ColorUtil.translate("&cInvalid ticket ID."));
            return;
        }

        plugin.getTicketManager().getTicket(ticketId).thenAccept(ticket -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (ticket == null) {
                    player.sendMessage(ColorUtil.translate("&cTicket not found."));
                    return;
                }

                // Check permission
                boolean isOwner = ticket.getPlayerUUID().equals(player.getUniqueId());
                boolean isStaff = player.hasPermission("litebansreborn.tickets.close");
                
                if (!isOwner && !isStaff) {
                    player.sendMessage(ColorUtil.translate("&cYou don't have permission to close this ticket."));
                    return;
                }

                plugin.getTicketManager().closeTicket(ticketId, TicketStatus.CLOSED).thenAccept(success -> {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (success) {
                            player.sendMessage(ColorUtil.translate("&aTicket #" + ticketId + " has been closed."));
                        } else {
                            player.sendMessage(ColorUtil.translate("&cFailed to close ticket."));
                        }
                    });
                });
            });
        });
    }

    private void handleClaim(Player player, String[] args) {
        if (!player.hasPermission("litebansreborn.tickets.claim")) {
            plugin.getMessagesManager().send(player, "general.no-permission");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ColorUtil.translate("&cUsage: /ticket claim <id>"));
            return;
        }

        int ticketId;
        try {
            ticketId = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(ColorUtil.translate("&cInvalid ticket ID."));
            return;
        }

        plugin.getTicketManager().claimTicket(
            ticketId,
            player.getUniqueId(),
            player.getName()
        ).thenAccept(success -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (success) {
                    player.sendMessage(ColorUtil.translate("&aYou have claimed ticket #" + ticketId));
                } else {
                    player.sendMessage(ColorUtil.translate("&cFailed to claim ticket."));
                }
            });
        });
    }

    private void sendHelp(Player player) {
        player.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        player.sendMessage(ColorUtil.translate("&6&lTicket Commands"));
        player.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
        player.sendMessage(ColorUtil.translate("&e/ticket create <category> <subject> &8- &7Create ticket"));
        player.sendMessage(ColorUtil.translate("&e/ticket list &8- &7View your tickets"));
        player.sendMessage(ColorUtil.translate("&e/ticket view <id> &8- &7View ticket details"));
        player.sendMessage(ColorUtil.translate("&e/ticket respond <id> <message> &8- &7Respond to ticket"));
        player.sendMessage(ColorUtil.translate("&e/ticket close <id> &8- &7Close ticket"));
        player.sendMessage(ColorUtil.translate("&e/ticket gui &8- &7Open ticket GUI"));
        
        if (player.hasPermission("litebansreborn.tickets.claim")) {
            player.sendMessage(ColorUtil.translate("&c/ticket claim <id> &8- &7Claim ticket (Staff)"));
        }
        
        player.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(Arrays.asList("create", "list", "view", "respond", "close", "gui"));
            if (sender.hasPermission("litebansreborn.tickets.claim")) {
                options.add("claim");
            }
            return options.stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        if (args.length == 2 && args[0].equalsIgnoreCase("create")) {
            return Arrays.stream(TicketCategory.values())
                .map(c -> c.name().toLowerCase())
                .filter(s -> s.startsWith(args[1].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        return new ArrayList<>();
    }
}
