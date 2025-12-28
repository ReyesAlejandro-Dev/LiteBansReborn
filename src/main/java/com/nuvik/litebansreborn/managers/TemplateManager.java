package com.nuvik.litebansreborn.managers;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.models.PunishmentTemplate;
import com.nuvik.litebansreborn.models.PunishmentType;
import org.bukkit.configuration.ConfigurationSection;

import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;



/**
 * Template Manager - Handles punishment templates/ladders
 */
public class TemplateManager {

    private final LiteBansReborn plugin;
    private final Map<String, PunishmentTemplate> templates = new HashMap<>();
    
    public TemplateManager(LiteBansReborn plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Load templates from configuration
     */
    public void loadTemplates() {
        templates.clear();
        
        ConfigurationSection templatesSection = plugin.getConfigManager().getConfig()
                .getConfigurationSection("templates");
        
        if (templatesSection == null) {
            plugin.debug("No templates found in configuration");
            return;
        }
        
        for (String templateId : templatesSection.getKeys(false)) {
            ConfigurationSection section = templatesSection.getConfigurationSection(templateId);
            if (section == null) continue;
            
            String name = section.getString("name", templateId);
            String description = section.getString("description", "");
            
            List<PunishmentTemplate.TemplateStep> steps = new ArrayList<>();
            
            var punishments = section.getMapList("punishments");
            for (var punishment : punishments) {
                String typeStr = String.valueOf(punishment.get("type"));
                String duration = punishment.containsKey("duration") ? 
                        String.valueOf(punishment.get("duration")) : null;
                String reason = String.valueOf(punishment.get("reason"));
                
                PunishmentType type = PunishmentType.fromId(typeStr);
                if (type == null) {
                    plugin.debug("Unknown punishment type in template: " + typeStr);
                    continue;
                }
                
                steps.add(new PunishmentTemplate.TemplateStep(type, duration, reason));
            }
            
            if (!steps.isEmpty()) {
                templates.put(templateId, new PunishmentTemplate(templateId, name, description, steps));
                plugin.debug("Loaded template: " + templateId + " with " + steps.size() + " steps");
            }
        }
        
        plugin.debug("Loaded " + templates.size() + " templates");
    }
    
    /**
     * Get a template by ID
     */
    public PunishmentTemplate getTemplate(String id) {
        return templates.get(id.toLowerCase());
    }
    
    /**
     * Get all templates
     */
    public Collection<PunishmentTemplate> getTemplates() {
        return templates.values();
    }
    
    /**
     * Get template IDs
     */
    public Set<String> getTemplateIds() {
        return templates.keySet();
    }
    
    /**
     * Get offense count for a player and template
     */
    public CompletableFuture<Integer> getOffenseCount(UUID uuid, String templateId) {
        return plugin.getDatabaseManager().queryAsync(conn -> {
            String sql = "SELECT offense_count FROM " + 
                    plugin.getDatabaseManager().getTable("template_offenses") +
                    " WHERE player_uuid = ? AND template_id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, templateId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("offense_count");
                    }
                }
            }
            
            return 0;
        });
    }
    
    /**
     * Increment offense count for a player and template
     */
    public CompletableFuture<Integer> incrementOffenseCount(UUID uuid, String templateId) {
        return plugin.getDatabaseManager().queryAsync(conn -> {
            // Try to insert or update
            String sql = "INSERT INTO " + plugin.getDatabaseManager().getTable("template_offenses") +
                    " (player_uuid, template_id, offense_count, last_offense) VALUES (?, ?, 1, ?) " +
                    "ON DUPLICATE KEY UPDATE offense_count = offense_count + 1, last_offense = ?";
            
            Timestamp now = Timestamp.from(Instant.now());
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, templateId);
                stmt.setTimestamp(3, now);
                stmt.setTimestamp(4, now);
                
                stmt.executeUpdate();
            }
            
            // Get the new count
            String selectSql = "SELECT offense_count FROM " + 
                    plugin.getDatabaseManager().getTable("template_offenses") +
                    " WHERE player_uuid = ? AND template_id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, templateId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("offense_count");
                    }
                }
            }
            
            return 1;
        });
    }
    
    /**
     * Reset offense count for a player and template
     */
    public CompletableFuture<Void> resetOffenseCount(UUID uuid, String templateId) {
        return plugin.getDatabaseManager().executeAsync(conn -> {
            String sql = "DELETE FROM " + plugin.getDatabaseManager().getTable("template_offenses") +
                    " WHERE player_uuid = ? AND template_id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, templateId);
                stmt.executeUpdate();
            }
        });
    }
    
    /**
     * Get the next punishment step from a template for a player
     */
    public CompletableFuture<PunishmentTemplate.TemplateStep> getNextStep(UUID uuid, String templateId) {
        PunishmentTemplate template = getTemplate(templateId);
        if (template == null) {
            return CompletableFuture.completedFuture(null);
        }
        
        return getOffenseCount(uuid, templateId).thenApply(count -> {
            return template.getStep(count + 1);
        });
    }
}
