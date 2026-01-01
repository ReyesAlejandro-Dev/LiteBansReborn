package com.nuvik.litebansreborn.ai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nuvik.litebansreborn.LiteBansReborn;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Groq AI Integration for intelligent moderation
 * Provides: Chat moderation, toxicity detection, behavior analysis, smart suggestions
 */
public class GroqAIService {
    
    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String DEFAULT_MODEL = "llama-3.3-70b-versatile";
    
    private final LiteBansReborn plugin;
    private final String apiKey;
    private final boolean enabled;
    private final Gson gson;
    
    public GroqAIService(LiteBansReborn plugin) {
        this.plugin = plugin;
        this.apiKey = plugin.getConfig().getString("ai.groq-api-key", "");
        this.enabled = plugin.getConfig().getBoolean("ai.enabled", false) && !apiKey.isEmpty();
        this.gson = new Gson();
        
        if (enabled) {
            plugin.getLogger().info("[AI] Groq AI Service initialized with model: " + DEFAULT_MODEL);
        }
    }
    
    /**
     * Analyze chat message for toxicity
     */
    public CompletableFuture<ToxicityResult> analyzeToxicity(String playerName, String message) {
        if (!enabled) {
            return CompletableFuture.completedFuture(new ToxicityResult(false, 0, "AI disabled"));
        }
        
        String prompt = String.format("""
            Analyze this Minecraft chat message for toxicity.
            Player: %s
            Message: "%s"
            
            Respond ONLY in JSON format:
            {
              "isToxic": true/false,
              "toxicityScore": 0-100,
              "category": "none/spam/insult/hate/threat/advertising",
              "reason": "brief explanation",
              "suggestedAction": "none/warn/mute/ban"
            }
            """, playerName, message);
        
        return callGroqAPI(prompt).thenApply(response -> {
            try {
                JsonObject json = gson.fromJson(response, JsonObject.class);
                boolean isToxic = json.get("isToxic").getAsBoolean();
                int score = json.get("toxicityScore").getAsInt();
                String reason = json.get("reason").getAsString();
                String category = json.has("category") ? json.get("category").getAsString() : "unknown";
                String action = json.has("suggestedAction") ? json.get("suggestedAction").getAsString() : "none";
                
                return new ToxicityResult(isToxic, score, reason, category, action);
            } catch (Exception e) {
                return new ToxicityResult(false, 0, "Parse error: " + e.getMessage());
            }
        });
    }
    
    /**
     * Get smart ban duration suggestion based on player history
     */
    public CompletableFuture<BanSuggestion> suggestBanDuration(String playerName, int previousBans, 
                                                               int previousWarns, String currentViolation) {
        if (!enabled) {
            return CompletableFuture.completedFuture(new BanSuggestion("1d", "AI disabled", 50));
        }
        
        String prompt = String.format("""
            Suggest appropriate punishment for this Minecraft player.
            
            Player: %s
            Previous bans: %d
            Previous warnings: %d
            Current violation: %s
            
            Respond ONLY in JSON format:
            {
              "suggestedDuration": "1h/1d/7d/30d/permanent",
              "reasoning": "brief explanation",
              "confidence": 0-100,
              "escalation": true/false,
              "alternativeAction": "warn/mute/kick/ban"
            }
            """, playerName, previousBans, previousWarns, currentViolation);
        
        return callGroqAPI(prompt).thenApply(response -> {
            try {
                JsonObject json = gson.fromJson(response, JsonObject.class);
                String duration = json.get("suggestedDuration").getAsString();
                String reasoning = json.get("reasoning").getAsString();
                int confidence = json.get("confidence").getAsInt();
                
                return new BanSuggestion(duration, reasoning, confidence);
            } catch (Exception e) {
                return new BanSuggestion("1d", "Error: " + e.getMessage(), 0);
            }
        });
    }
    
    /**
     * Analyze player behavior patterns for potential cheating
     */
    public CompletableFuture<BehaviorAnalysis> analyzeBehavior(String playerName, 
                                                                Map<String, Object> playerStats) {
        if (!enabled) {
            return CompletableFuture.completedFuture(new BehaviorAnalysis(0, "AI disabled", false));
        }
        
        String statsJson = gson.toJson(playerStats);
        
        String prompt = String.format("""
            Analyze this Minecraft player's behavior for potential cheating.
            
            Player: %s
            Stats: %s
            
            Consider: KDR, movement speed, accuracy, playtime consistency, etc.
            
            Respond ONLY in JSON format:
            {
              "suspicionLevel": 0-100,
              "analysis": "detailed analysis",
              "isLikelyCheating": true/false,
              "suspectedCheats": ["killaura", "speed", "fly", "etc"],
              "recommendation": "monitor/warn/spectate/ban"
            }
            """, playerName, statsJson);
        
        return callGroqAPI(prompt).thenApply(response -> {
            try {
                JsonObject json = gson.fromJson(response, JsonObject.class);
                int suspicion = json.get("suspicionLevel").getAsInt();
                String analysis = json.get("analysis").getAsString();
                boolean cheating = json.get("isLikelyCheating").getAsBoolean();
                
                return new BehaviorAnalysis(suspicion, analysis, cheating);
            } catch (Exception e) {
                return new BehaviorAnalysis(0, "Error: " + e.getMessage(), false);
            }
        });
    }
    
    /**
     * Generate punishment reason from violation type
     */
    public CompletableFuture<String> generatePunishmentReason(String violationType, String context) {
        if (!enabled) {
            return CompletableFuture.completedFuture(violationType);
        }
        
        String prompt = String.format("""
            Generate a professional, brief ban/mute reason for a Minecraft server.
            
            Violation type: %s
            Context: %s
            
            Requirements:
            - Keep it under 50 characters
            - Be professional but firm
            - Don't use profanity
            
            Respond with ONLY the reason text, nothing else.
            """, violationType, context);
        
        return callGroqAPI(prompt);
    }
    
    /**
     * Call Groq API with a prompt
     */
    private CompletableFuture<String> callGroqAPI(String prompt) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URL url = new URL(GROQ_API_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(30000);
                
                // Build request body
                JsonObject requestBody = new JsonObject();
                requestBody.addProperty("model", DEFAULT_MODEL);
                requestBody.addProperty("temperature", 0.3);
                requestBody.addProperty("max_tokens", 500);
                
                JsonArray messages = new JsonArray();
                JsonObject systemMsg = new JsonObject();
                systemMsg.addProperty("role", "system");
                systemMsg.addProperty("content", "You are a helpful Minecraft server moderation assistant. Always respond in valid JSON when requested.");
                messages.add(systemMsg);
                
                JsonObject userMsg = new JsonObject();
                userMsg.addProperty("role", "user");
                userMsg.addProperty("content", prompt);
                messages.add(userMsg);
                
                requestBody.add("messages", messages);
                
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
                
                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    plugin.getLogger().warning("[AI] Groq API error: " + responseCode);
                    return "Error: " + responseCode;
                }
                
                StringBuilder response = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                }
                
                // Parse response
                JsonObject jsonResponse = gson.fromJson(response.toString(), JsonObject.class);
                JsonArray choices = jsonResponse.getAsJsonArray("choices");
                if (choices != null && choices.size() > 0) {
                    JsonObject choice = choices.get(0).getAsJsonObject();
                    JsonObject message = choice.getAsJsonObject("message");
                    return message.get("content").getAsString().trim();
                }
                
                return "No response";
                
            } catch (Exception e) {
                plugin.getLogger().warning("[AI] Groq API call failed: " + e.getMessage());
                return "Error: " + e.getMessage();
            }
        });
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    // Result classes
    public record ToxicityResult(boolean isToxic, int score, String reason, String category, String action) {
        public ToxicityResult(boolean isToxic, int score, String reason) {
            this(isToxic, score, reason, "none", "none");
        }
    }
    
    public record BanSuggestion(String duration, String reasoning, int confidence) {}
    
    public record BehaviorAnalysis(int suspicionLevel, String analysis, boolean likelyCheating) {}
}
