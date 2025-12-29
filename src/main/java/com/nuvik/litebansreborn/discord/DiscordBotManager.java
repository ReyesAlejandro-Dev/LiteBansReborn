package com.nuvik.litebansreborn.discord;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.models.Punishment;
import com.nuvik.litebansreborn.utils.ColorUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

import java.awt.Color;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Discord Bot Manager - Native JDA Integration
 * Features:
 * - Punishment notifications
 * - Slash commands for moderation
 * - Player verification
 * - Statistics and lookups
 */
public class DiscordBotManager extends ListenerAdapter {

    private final LiteBansReborn plugin;
    private JDA jda;
    private boolean enabled = false;
    
    // Channels
    private TextChannel logChannel;
    private TextChannel ticketChannel;
    private TextChannel verificationChannel;
    
    // Verification pending
    private final Map<String, VerificationRequest> pendingVerifications = new ConcurrentHashMap<>();

    public DiscordBotManager(LiteBansReborn plugin) {
        this.plugin = plugin;
    }

    /**
     * Start the Discord bot
     */
    public boolean start() {
        String token = plugin.getConfigManager().getString("discord-bot.token", "");
        
        if (token.isEmpty() || token.equals("YOUR_BOT_TOKEN_HERE")) {
            plugin.log(Level.WARNING, "Discord bot token not configured. Bot disabled.");
            return false;
        }

        try {
            jda = JDABuilder.createDefault(token)
                .enableIntents(
                    GatewayIntent.GUILD_MESSAGES,
                    GatewayIntent.GUILD_MEMBERS,
                    GatewayIntent.MESSAGE_CONTENT,
                    GatewayIntent.DIRECT_MESSAGES
                )
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .addEventListeners(this)
                .build();

            jda.awaitReady();
            enabled = true;
            
            // Register slash commands
            registerCommands();
            
            // Get channels
            loadChannels();
            
            plugin.log(Level.INFO, "Discord bot connected as " + jda.getSelfUser().getName());
            return true;
        } catch (Exception e) {
            plugin.log(Level.SEVERE, "Failed to start Discord bot: " + e.getMessage());
            return false;
        }
    }

    /**
     * Stop the Discord bot
     */
    public void stop() {
        if (jda != null) {
            jda.shutdown();
            enabled = false;
            plugin.log(Level.INFO, "Discord bot disconnected.");
        }
    }

    /**
     * Register slash commands
     */
    private void registerCommands() {
        jda.updateCommands().addCommands(
            Commands.slash("ban", "Ban a player")
                .addOption(OptionType.STRING, "player", "Player name", true)
                .addOption(OptionType.STRING, "duration", "Duration (e.g., 7d)", false)
                .addOption(OptionType.STRING, "reason", "Ban reason", false),
            
            Commands.slash("unban", "Unban a player")
                .addOption(OptionType.STRING, "player", "Player name", true),
            
            Commands.slash("mute", "Mute a player")
                .addOption(OptionType.STRING, "player", "Player name", true)
                .addOption(OptionType.STRING, "duration", "Duration", false)
                .addOption(OptionType.STRING, "reason", "Mute reason", false),
            
            Commands.slash("history", "View player punishment history")
                .addOption(OptionType.STRING, "player", "Player name", true),
            
            Commands.slash("checkban", "Check if a player is banned")
                .addOption(OptionType.STRING, "player", "Player name", true),
            
            Commands.slash("stats", "View server punishment statistics"),
            
            Commands.slash("verify", "Verify your Minecraft account")
                .addOption(OptionType.STRING, "code", "Verification code", true),
            
            Commands.slash("lookup", "Lookup a player's info")
                .addOption(OptionType.STRING, "player", "Player name", true)
        ).queue();
    }

    /**
     * Load configured channels
     */
    private void loadChannels() {
        String logChannelId = plugin.getConfigManager().getString("discord-bot.channels.logs", "");
        String ticketChannelId = plugin.getConfigManager().getString("discord-bot.channels.tickets", "");
        String verifyChannelId = plugin.getConfigManager().getString("discord-bot.channels.verification", "");

        if (!logChannelId.isEmpty()) {
            logChannel = jda.getTextChannelById(logChannelId);
        }
        if (!ticketChannelId.isEmpty()) {
            ticketChannel = jda.getTextChannelById(ticketChannelId);
        }
        if (!verifyChannelId.isEmpty()) {
            verificationChannel = jda.getTextChannelById(verifyChannelId);
        }
    }

    // ==================== SLASH COMMANDS ====================

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!hasPermission(event.getMember(), "staff")) {
            // Check if it's a player command
            if (!event.getName().equals("verify") && !event.getName().equals("stats")) {
                event.reply("❌ You don't have permission to use this command.").setEphemeral(true).queue();
                return;
            }
        }

        switch (event.getName()) {
            case "ban" -> handleBan(event);
            case "unban" -> handleUnban(event);
            case "mute" -> handleMute(event);
            case "history" -> handleHistory(event);
            case "checkban" -> handleCheckban(event);
            case "stats" -> handleStats(event);
            case "verify" -> handleVerify(event);
            case "lookup" -> handleLookup(event);
        }
    }

    private void handleBan(SlashCommandInteractionEvent event) {
        String player = event.getOption("player").getAsString();
        String duration = event.getOption("duration") != null ? event.getOption("duration").getAsString() : null;
        String reason = event.getOption("reason") != null ? event.getOption("reason").getAsString() : "Banned via Discord";

        event.deferReply().queue();

        // Execute ban on main thread
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            String cmd = "ban " + player + (duration != null ? " " + duration : "") + " " + reason;
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd);
            
            EmbedBuilder embed = new EmbedBuilder()
                .setColor(Color.RED)
                .setTitle("🔨 Player Banned")
                .addField("Player", player, true)
                .addField("Duration", duration != null ? duration : "Permanent", true)
                .addField("Reason", reason, false)
                .addField("Banned by", event.getUser().getAsTag(), true)
                .setTimestamp(Instant.now());
            
            event.getHook().editOriginalEmbeds(embed.build()).queue();
        });
    }

    private void handleUnban(SlashCommandInteractionEvent event) {
        String player = event.getOption("player").getAsString();
        
        event.deferReply().queue();
        
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), "unban " + player);
            
            EmbedBuilder embed = new EmbedBuilder()
                .setColor(Color.GREEN)
                .setTitle("✅ Player Unbanned")
                .addField("Player", player, true)
                .addField("Unbanned by", event.getUser().getAsTag(), true)
                .setTimestamp(Instant.now());
            
            event.getHook().editOriginalEmbeds(embed.build()).queue();
        });
    }

    private void handleMute(SlashCommandInteractionEvent event) {
        String player = event.getOption("player").getAsString();
        String duration = event.getOption("duration") != null ? event.getOption("duration").getAsString() : null;
        String reason = event.getOption("reason") != null ? event.getOption("reason").getAsString() : "Muted via Discord";

        event.deferReply().queue();

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            String cmd = "mute " + player + (duration != null ? " " + duration : "") + " " + reason;
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd);
            
            EmbedBuilder embed = new EmbedBuilder()
                .setColor(Color.ORANGE)
                .setTitle("🔇 Player Muted")
                .addField("Player", player, true)
                .addField("Duration", duration != null ? duration : "Permanent", true)
                .addField("Reason", reason, false)
                .addField("Muted by", event.getUser().getAsTag(), true)
                .setTimestamp(Instant.now());
            
            event.getHook().editOriginalEmbeds(embed.build()).queue();
        });
    }

    private void handleHistory(SlashCommandInteractionEvent event) {
        String player = event.getOption("player").getAsString();
        event.deferReply().queue();

        // Get player UUID from name
        @SuppressWarnings("deprecation")
        org.bukkit.OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(player);
        
        plugin.getHistoryManager().getPlayerHistory(offlinePlayer.getUniqueId()).thenAccept(history -> {
            EmbedBuilder embed = new EmbedBuilder()
                .setColor(Color.CYAN)
                .setTitle("📋 Punishment History: " + player);

            if (history.isEmpty()) {
                embed.setDescription("No punishments found.");
            } else {
                StringBuilder sb = new StringBuilder();
                int count = 0;
                for (Punishment p : history) {
                    if (count++ >= 10) {
                        sb.append("\n*...and ").append(history.size() - 10).append(" more*");
                        break;
                    }
                    sb.append("**").append(p.getType()).append("** - ")
                      .append(p.getReason()).append("\n");
                }
                embed.setDescription(sb.toString());
                embed.setFooter("Total: " + history.size() + " punishments");
            }
            
            event.getHook().editOriginalEmbeds(embed.build()).queue();
        });
    }

    private void handleCheckban(SlashCommandInteractionEvent event) {
        String player = event.getOption("player").getAsString();
        event.deferReply().queue();

        // Get player UUID from name
        @SuppressWarnings("deprecation")
        org.bukkit.OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(player);
        
        plugin.getBanManager().getActiveBan(offlinePlayer.getUniqueId()).thenAccept(ban -> {
            EmbedBuilder embed = new EmbedBuilder();
            
            if (ban != null) {
                String expiresText = ban.isPermanent() ? "Never (Permanent)" : 
                    (ban.getExpiresAt() != null ? ban.getExpiresAt().toString() : "Unknown");
                    
                embed.setColor(Color.RED)
                    .setTitle("🚫 Player is Banned")
                    .addField("Player", player, true)
                    .addField("Reason", ban.getReason(), false)
                    .addField("Expires", expiresText, true)
                    .addField("Banned by", ban.getExecutorName(), true);
            } else {
                embed.setColor(Color.GREEN)
                    .setTitle("✅ Player is Not Banned")
                    .setDescription(player + " is not currently banned.");
            }
            
            event.getHook().editOriginalEmbeds(embed.build()).queue();
        });
    }

    private void handleStats(SlashCommandInteractionEvent event) {
        var cacheStats = plugin.getCacheManager().getStats();
        
        EmbedBuilder embed = new EmbedBuilder()
            .setColor(Color.BLUE)
            .setTitle("📊 Server Statistics")
            .addField("Online Players", String.valueOf(plugin.getServer().getOnlinePlayers().size()), true)
            .addField("Active Bans", String.valueOf(cacheStats.get("bans")), true)
            .addField("Active Mutes", String.valueOf(cacheStats.get("mutes")), true)
            .addField("Frozen Players", String.valueOf(cacheStats.get("frozen")), true)
            .setTimestamp(Instant.now());
        
        event.replyEmbeds(embed.build()).queue();
    }

    private void handleVerify(SlashCommandInteractionEvent event) {
        String code = event.getOption("code").getAsString();
        
        VerificationRequest request = pendingVerifications.get(code.toUpperCase());
        if (request == null || request.isExpired()) {
            pendingVerifications.remove(code.toUpperCase());
            event.reply("❌ Invalid or expired verification code.").setEphemeral(true).queue();
            return;
        }

        // Link accounts
        plugin.getVerificationManager().linkAccounts(request.minecraftUUID, event.getUser().getIdLong());
        pendingVerifications.remove(code.toUpperCase());

        EmbedBuilder embed = new EmbedBuilder()
            .setColor(Color.GREEN)
            .setTitle("✅ Account Verified!")
            .setDescription("Your Discord account is now linked to **" + request.minecraftName + "**")
            .setTimestamp(Instant.now());
        
        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
        
        // Add verified role if configured
        String verifiedRoleId = plugin.getConfigManager().getString("discord-bot.verified-role", "");
        if (!verifiedRoleId.isEmpty() && event.getGuild() != null) {
            Role role = event.getGuild().getRoleById(verifiedRoleId);
            if (role != null && event.getMember() != null) {
                event.getGuild().addRoleToMember(event.getMember(), role).queue();
            }
        }
    }

    private void handleLookup(SlashCommandInteractionEvent event) {
        String player = event.getOption("player").getAsString();
        event.deferReply().queue();

        // Get player info
        @SuppressWarnings("deprecation")
        org.bukkit.OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(player);
        
        EmbedBuilder embed = new EmbedBuilder()
            .setColor(Color.CYAN)
            .setTitle("🔍 Player Lookup: " + player)
            .addField("UUID", offlinePlayer.getUniqueId().toString(), false)
            .addField("Has Played Before", offlinePlayer.hasPlayedBefore() ? "Yes" : "No", true)
            .addField("Is Online", offlinePlayer.isOnline() ? "Yes" : "No", true);
        
        if (offlinePlayer.hasPlayedBefore()) {
            embed.addField("First Played", new Date(offlinePlayer.getFirstPlayed()).toString(), false);
            embed.addField("Last Played", new Date(offlinePlayer.getLastPlayed()).toString(), false);
        }
        
        event.getHook().editOriginalEmbeds(embed.build()).queue();
    }

    // ==================== NOTIFICATIONS ====================

    /**
     * Send punishment notification to Discord
     */
    public void sendPunishmentNotification(Punishment punishment) {
        if (!enabled || logChannel == null) return;

        Color color = switch (punishment.getType().name().toUpperCase()) {
            case "BAN", "IPBAN" -> Color.RED;
            case "MUTE", "IPMUTE" -> Color.ORANGE;
            case "KICK" -> Color.YELLOW;
            case "WARN" -> new Color(255, 165, 0);
            default -> Color.GRAY;
        };

        String durationText = punishment.isPermanent() ? "Permanent" : 
            (punishment.getExpiresAt() != null ? formatDuration(punishment.getRemainingTime()) : "Unknown");
            
        EmbedBuilder embed = new EmbedBuilder()
            .setColor(color)
            .setTitle(getEmoji(punishment.getType().name()) + " " + punishment.getType().name())
            .addField("Player", punishment.getTargetName(), true)
            .addField("Staff", punishment.getExecutorName(), true)
            .addField("Duration", durationText, true)
            .addField("Reason", punishment.getReason(), false)
            .setTimestamp(Instant.now());

        logChannel.sendMessageEmbeds(embed.build()).queue();
    }

    private String getEmoji(String type) {
        return switch (type.toUpperCase()) {
            case "BAN", "IPBAN" -> "🔨";
            case "MUTE", "IPMUTE" -> "🔇";
            case "KICK" -> "👢";
            case "WARN" -> "⚠️";
            case "UNBAN" -> "✅";
            case "UNMUTE" -> "🔊";
            default -> "📋";
        };
    }
    
    private String formatDuration(long millis) {
        if (millis <= 0) return "Expired";
        
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return days + "d " + (hours % 24) + "h";
        } else if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m";
        } else if (minutes > 0) {
            return minutes + "m";
        } else {
            return seconds + "s";
        }
    }

    // ==================== VERIFICATION ====================

    /**
     * Create verification request
     */
    public String createVerificationCode(UUID minecraftUUID, String minecraftName) {
        String code = generateCode();
        pendingVerifications.put(code, new VerificationRequest(minecraftUUID, minecraftName));
        
        // Clean up expired codes
        pendingVerifications.entrySet().removeIf(e -> e.getValue().isExpired());
        
        return code;
    }

    private String generateCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }

    // ==================== UTILITY ====================

    private boolean hasPermission(Member member, String permission) {
        if (member == null) return false;
        
        // Check for admin
        if (member.hasPermission(net.dv8tion.jda.api.Permission.ADMINISTRATOR)) {
            return true;
        }
        
        // Check for configured roles
        List<String> staffRoles = plugin.getConfigManager().getStringList("discord-bot.staff-roles");
        for (Role role : member.getRoles()) {
            if (staffRoles.contains(role.getId())) {
                return true;
            }
        }
        
        return false;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public JDA getJDA() {
        return jda;
    }

    // ==================== INNER CLASSES ====================

    private static class VerificationRequest {
        final UUID minecraftUUID;
        final String minecraftName;
        final long createdAt;

        VerificationRequest(UUID uuid, String name) {
            this.minecraftUUID = uuid;
            this.minecraftName = name;
            this.createdAt = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - createdAt > TimeUnit.MINUTES.toMillis(10);
        }
    }
}
