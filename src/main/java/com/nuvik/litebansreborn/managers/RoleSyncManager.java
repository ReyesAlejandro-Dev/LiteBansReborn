package com.nuvik.litebansreborn.managers;

import com.nuvik.litebansreborn.LiteBansReborn;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Role Sync Manager - Sync Discord roles with LuckPerms
 * Features:
 * - Bidirectional sync (Discord ↔ Minecraft)
 * - Configurable role mappings
 * - Auto-sync on join
 * - Manual sync commands
 */
public class RoleSyncManager {

    private final LiteBansReborn plugin;
    private LuckPerms luckPerms;
    private boolean enabled = false;
    
    // Role mappings: Discord Role ID -> LuckPerms Group
    private final Map<String, String> discordToMinecraft = new ConcurrentHashMap<>();
    // Role mappings: LuckPerms Group -> Discord Role ID
    private final Map<String, String> minecraftToDiscord = new ConcurrentHashMap<>();

    public RoleSyncManager(LiteBansReborn plugin) {
        this.plugin = plugin;
        loadConfig();
        hookLuckPerms();
    }

    private void loadConfig() {
        enabled = plugin.getConfigManager().getBoolean("role-sync.enabled", false);
        
        if (!enabled) return;
        
        // Load role mappings from config
        var mappings = plugin.getConfig().getConfigurationSection("role-sync.mappings");
        if (mappings != null) {
            for (String discordRoleId : mappings.getKeys(false)) {
                String luckpermsGroup = mappings.getString(discordRoleId);
                if (luckpermsGroup != null) {
                    discordToMinecraft.put(discordRoleId, luckpermsGroup);
                    minecraftToDiscord.put(luckpermsGroup, discordRoleId);
                }
            }
        }
        
        plugin.log(Level.INFO, "Loaded " + discordToMinecraft.size() + " role mappings.");
    }

    private void hookLuckPerms() {
        if (!enabled) return;
        
        try {
            if (Bukkit.getPluginManager().getPlugin("LuckPerms") != null) {
                luckPerms = LuckPermsProvider.get();
                plugin.log(Level.INFO, "§aHooked into LuckPerms for role sync!");
            } else {
                enabled = false;
                plugin.log(Level.WARNING, "LuckPerms not found. Role sync disabled.");
            }
        } catch (Exception e) {
            enabled = false;
            plugin.log(Level.WARNING, "Failed to hook LuckPerms: " + e.getMessage());
        }
    }

    // ==================== SYNC OPERATIONS ====================

    /**
     * Sync Discord roles to Minecraft (Discord -> MC)
     */
    public CompletableFuture<Boolean> syncDiscordToMinecraft(UUID minecraftUUID) {
        if (!enabled || luckPerms == null) {
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get linked Discord ID
                Long discordId = plugin.getVerificationManager().getDiscordId(minecraftUUID);
                if (discordId == null) {
                    return false; // Not verified
                }

                // Get Discord roles via JDA
                if (plugin.getDiscordBotManager() == null || !plugin.getDiscordBotManager().isEnabled()) {
                    return false;
                }

                var jda = plugin.getDiscordBotManager().getJDA();
                if (jda == null) return false;

                // Get member from all guilds
                Set<String> discordRoles = new HashSet<>();
                for (var guild : jda.getGuilds()) {
                    var member = guild.getMemberById(discordId);
                    if (member != null) {
                        for (var role : member.getRoles()) {
                            discordRoles.add(role.getId());
                        }
                    }
                }

                // Apply LuckPerms groups
                User user = luckPerms.getUserManager().loadUser(minecraftUUID).join();
                
                for (Map.Entry<String, String> mapping : discordToMinecraft.entrySet()) {
                    String discordRoleId = mapping.getKey();
                    String lpGroup = mapping.getValue();
                    
                    Group group = luckPerms.getGroupManager().getGroup(lpGroup);
                    if (group == null) continue;

                    InheritanceNode node = InheritanceNode.builder(group).build();

                    if (discordRoles.contains(discordRoleId)) {
                        // Add group
                        user.data().add(node);
                    } else {
                        // Remove group
                        user.data().remove(node);
                    }
                }

                luckPerms.getUserManager().saveUser(user);
                return true;
            } catch (Exception e) {
                plugin.log(Level.WARNING, "Failed to sync Discord -> MC: " + e.getMessage());
                return false;
            }
        });
    }

    /**
     * Sync Minecraft roles to Discord (MC -> Discord)
     */
    public CompletableFuture<Boolean> syncMinecraftToDiscord(UUID minecraftUUID) {
        if (!enabled || luckPerms == null) {
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get linked Discord ID
                Long discordId = plugin.getVerificationManager().getDiscordId(minecraftUUID);
                if (discordId == null) {
                    return false;
                }

                if (plugin.getDiscordBotManager() == null || !plugin.getDiscordBotManager().isEnabled()) {
                    return false;
                }

                var jda = plugin.getDiscordBotManager().getJDA();
                if (jda == null) return false;

                // Get LuckPerms groups
                User user = luckPerms.getUserManager().loadUser(minecraftUUID).join();
                Set<String> lpGroups = new HashSet<>();
                for (var node : user.getNodes()) {
                    if (node instanceof InheritanceNode inheritanceNode) {
                        lpGroups.add(inheritanceNode.getGroupName());
                    }
                }

                // Apply Discord roles
                for (var guild : jda.getGuilds()) {
                    var member = guild.getMemberById(discordId);
                    if (member == null) continue;

                    for (Map.Entry<String, String> mapping : minecraftToDiscord.entrySet()) {
                        String lpGroup = mapping.getKey();
                        String discordRoleId = mapping.getValue();
                        
                        var role = guild.getRoleById(discordRoleId);
                        if (role == null) continue;

                        if (lpGroups.contains(lpGroup)) {
                            // Add role
                            guild.addRoleToMember(member, role).queue();
                        } else {
                            // Remove role
                            guild.removeRoleFromMember(member, role).queue();
                        }
                    }
                }

                return true;
            } catch (Exception e) {
                plugin.log(Level.WARNING, "Failed to sync MC -> Discord: " + e.getMessage());
                return false;
            }
        });
    }

    /**
     * Full bidirectional sync
     */
    public CompletableFuture<Boolean> fullSync(UUID minecraftUUID) {
        String direction = plugin.getConfigManager().getString("role-sync.direction", "discord-to-minecraft");
        
        return switch (direction.toLowerCase()) {
            case "discord-to-minecraft" -> syncDiscordToMinecraft(minecraftUUID);
            case "minecraft-to-discord" -> syncMinecraftToDiscord(minecraftUUID);
            case "bidirectional" -> syncDiscordToMinecraft(minecraftUUID)
                    .thenCompose(result -> syncMinecraftToDiscord(minecraftUUID));
            default -> CompletableFuture.completedFuture(false);
        };
    }

    // ==================== MANAGEMENT ====================

    /**
     * Add a role mapping
     */
    public void addMapping(String discordRoleId, String luckpermsGroup) {
        discordToMinecraft.put(discordRoleId, luckpermsGroup);
        minecraftToDiscord.put(luckpermsGroup, discordRoleId);
        saveConfig();
    }

    /**
     * Remove a role mapping
     */
    public void removeMapping(String discordRoleId) {
        String group = discordToMinecraft.remove(discordRoleId);
        if (group != null) {
            minecraftToDiscord.remove(group);
        }
        saveConfig();
    }

    private void saveConfig() {
        for (Map.Entry<String, String> entry : discordToMinecraft.entrySet()) {
            plugin.getConfig().set("role-sync.mappings." + entry.getKey(), entry.getValue());
        }
        plugin.saveConfig();
    }

    // ==================== GETTERS ====================

    public boolean isEnabled() {
        return enabled;
    }

    public Map<String, String> getDiscordToMinecraft() {
        return new HashMap<>(discordToMinecraft);
    }

    public Map<String, String> getMinecraftToDiscord() {
        return new HashMap<>(minecraftToDiscord);
    }
}
