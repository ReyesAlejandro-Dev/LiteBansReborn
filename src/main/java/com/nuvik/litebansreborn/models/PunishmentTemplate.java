package com.nuvik.litebansreborn.models;

import java.util.List;

/**
 * Represents a punishment template/ladder for escalating punishments
 */
public class PunishmentTemplate {
    
    private final String id;
    private final String name;
    private final String description;
    private final List<TemplateStep> steps;
    
    public PunishmentTemplate(String id, String name, String description, List<TemplateStep> steps) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.steps = steps;
    }
    
    /**
     * Get the next punishment step for a player based on their offense count
     */
    public TemplateStep getStep(int offenseCount) {
        if (offenseCount <= 0 || steps.isEmpty()) {
            return steps.isEmpty() ? null : steps.get(0);
        }
        
        // Offense count is 1-indexed, array is 0-indexed
        int index = offenseCount - 1;
        
        // If offense count exceeds steps, use the last step
        if (index >= steps.size()) {
            return steps.get(steps.size() - 1);
        }
        
        return steps.get(index);
    }
    
    /**
     * Get the maximum step number
     */
    public int getMaxSteps() {
        return steps.size();
    }
    
    // Getters
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public List<TemplateStep> getSteps() {
        return steps;
    }
    
    /**
     * Represents a single step in a punishment template
     */
    public static class TemplateStep {
        
        private final PunishmentType type;
        private final String duration; // null for permanent
        private final String reason;
        
        public TemplateStep(PunishmentType type, String duration, String reason) {
            this.type = type;
            this.duration = duration;
            this.reason = reason;
        }
        
        public PunishmentType getType() {
            return type;
        }
        
        public String getDuration() {
            return duration;
        }
        
        public boolean isPermanent() {
            return duration == null || duration.equalsIgnoreCase("permanent");
        }
        
        public String getReason() {
            return reason;
        }
    }
}
