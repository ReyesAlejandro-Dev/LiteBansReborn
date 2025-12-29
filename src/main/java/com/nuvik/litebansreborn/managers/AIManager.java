package com.nuvik.litebansreborn.managers;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * AI Manager - External AI API integration
 * Supports: Venice, OpenRouter, DeepSeek, OpenAI, Claude
 * 
 * Features:
 * - Player behavior analysis
 * - Chat toxicity detection
 * - Appeal review assistance
 * - No local resources used - all API calls
 */
public class AIManager {

    private final LiteBansReborn plugin;
    private final HttpClient httpClient;
    private final Gson gson = new Gson();
    
    private boolean enabled = false;
    private String provider = "openrouter";
    private String apiKey = "";
    private String model = "";
    private String baseUrl = "";

    // Provider URLs
    private static final Map<String, String> PROVIDER_URLS = Map.of(
        "openrouter", "https://openrouter.ai/api/v1/chat/completions",
        "venice", "https://api.venice.ai/api/v1/chat/completions",
        "openai", "https://api.openai.com/v1/chat/completions",
        "deepseek", "https://api.deepseek.com/v1/chat/completions",
        "claude", "https://api.anthropic.com/v1/messages"
    );

    public AIManager(LiteBansReborn plugin) {
        this.plugin = plugin;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
        loadConfig();
    }

    private void loadConfig() {
        enabled = plugin.getConfigManager().getBoolean("ai.enabled", false);
        provider = plugin.getConfigManager().getString("ai.provider", "openrouter");
        apiKey = plugin.getConfigManager().getString("ai.api-key", "");
        model = plugin.getConfigManager().getString("ai.model", "deepseek/deepseek-chat");
        
        // Get base URL for provider
        baseUrl = PROVIDER_URLS.getOrDefault(provider.toLowerCase(), PROVIDER_URLS.get("openrouter"));
        
        if (enabled && !apiKey.isEmpty() && !apiKey.equals("YOUR_API_KEY_HERE")) {
            plugin.log(Level.INFO, "§aAI Manager initialized with " + provider);
        } else if (enabled) {
            enabled = false;
            plugin.log(Level.WARNING, "AI Manager disabled - API key not configured");
        }
    }

    // ==================== CHAT ANALYSIS ====================

    /**
     * Analyze chat message for toxicity
     */
    public CompletableFuture<ToxicityResult> analyzeToxicity(String message, String playerName) {
        if (!enabled) {
            return CompletableFuture.completedFuture(new ToxicityResult(false, 0, "AI disabled"));
        }

        String prompt = """
            Analyze this Minecraft chat message for toxicity. Respond with JSON only.
            Player: %s
            Message: "%s"
            
            Respond in this exact JSON format:
            {"toxic": true/false, "score": 0-100, "reason": "brief reason", "category": "spam/harassment/hate/profanity/clean"}
            """.formatted(playerName, message);

        return sendRequest(prompt).thenApply(response -> {
            try {
                JsonObject json = gson.fromJson(response, JsonObject.class);
                return new ToxicityResult(
                    json.get("toxic").getAsBoolean(),
                    json.get("score").getAsInt(),
                    json.get("reason").getAsString()
                );
            } catch (Exception e) {
                return new ToxicityResult(false, 0, "Parse error");
            }
        });
    }

    /**
     * Analyze player behavior pattern
     */
    public CompletableFuture<BehaviorAnalysis> analyzeBehavior(String playerName, List<String> recentActions) {
        if (!enabled) {
            return CompletableFuture.completedFuture(new BehaviorAnalysis("unknown", 0, "AI disabled"));
        }

        String prompt = """
            Analyze this Minecraft player's recent behavior. Respond with JSON only.
            Player: %s
            Recent actions:
            %s
            
            Respond in this exact JSON format:
            {"risk_level": "low/medium/high/critical", "risk_score": 0-100, "summary": "brief 1-2 sentence summary", "recommendation": "none/watch/warn/mute/ban"}
            """.formatted(playerName, String.join("\n", recentActions));

        return sendRequest(prompt).thenApply(response -> {
            try {
                JsonObject json = gson.fromJson(response, JsonObject.class);
                return new BehaviorAnalysis(
                    json.get("risk_level").getAsString(),
                    json.get("risk_score").getAsInt(),
                    json.get("summary").getAsString()
                );
            } catch (Exception e) {
                return new BehaviorAnalysis("unknown", 0, "Parse error");
            }
        });
    }

    /**
     * Review an appeal with AI assistance
     */
    public CompletableFuture<AppealReview> reviewAppeal(String playerName, String punishmentReason, String appealText) {
        if (!enabled) {
            return CompletableFuture.completedFuture(new AppealReview("neutral", 50, "AI disabled"));
        }

        String prompt = """
            Review this Minecraft punishment appeal. Respond with JSON only.
            
            Player: %s
            Original punishment reason: %s
            Player's appeal: "%s"
            
            Analyze the appeal for:
            - Sincerity
            - Understanding of wrongdoing
            - Promise to improve
            - Any red flags
            
            Respond in this exact JSON format:
            {"recommendation": "accept/deny/neutral", "confidence": 0-100, "reasoning": "brief explanation", "red_flags": ["list", "of", "concerns"]}
            """.formatted(playerName, punishmentReason, appealText);

        return sendRequest(prompt).thenApply(response -> {
            try {
                JsonObject json = gson.fromJson(response, JsonObject.class);
                return new AppealReview(
                    json.get("recommendation").getAsString(),
                    json.get("confidence").getAsInt(),
                    json.get("reasoning").getAsString()
                );
            } catch (Exception e) {
                return new AppealReview("neutral", 50, "Parse error");
            }
        });
    }

    // ==================== API CALLS ====================

    private CompletableFuture<String> sendRequest(String prompt) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject requestBody = new JsonObject();
                
                // Build messages array
                JsonArray messages = new JsonArray();
                JsonObject systemMessage = new JsonObject();
                systemMessage.addProperty("role", "system");
                systemMessage.addProperty("content", "You are a Minecraft server moderation assistant. Always respond in valid JSON only.");
                messages.add(systemMessage);
                
                JsonObject userMessage = new JsonObject();
                userMessage.addProperty("role", "user");
                userMessage.addProperty("content", prompt);
                messages.add(userMessage);
                
                requestBody.add("messages", messages);
                requestBody.addProperty("model", model);
                requestBody.addProperty("max_tokens", 500);
                requestBody.addProperty("temperature", 0.3);

                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json");

                // Set authorization based on provider
                if (provider.equalsIgnoreCase("claude")) {
                    requestBuilder.header("x-api-key", apiKey);
                    requestBuilder.header("anthropic-version", "2023-06-01");
                } else {
                    requestBuilder.header("Authorization", "Bearer " + apiKey);
                }

                // OpenRouter specific headers
                if (provider.equalsIgnoreCase("openrouter")) {
                    requestBuilder.header("HTTP-Referer", "https://github.com/ReyesAlejandro-Dev/LiteBansReborn");
                    requestBuilder.header("X-Title", "LiteBansReborn");
                }

                HttpRequest request = requestBuilder
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                    .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    plugin.log(Level.WARNING, "AI API error: " + response.statusCode());
                    return "{}";
                }

                // Parse response
                JsonObject responseJson = gson.fromJson(response.body(), JsonObject.class);
                
                if (provider.equalsIgnoreCase("claude")) {
                    return responseJson.getAsJsonArray("content")
                        .get(0).getAsJsonObject()
                        .get("text").getAsString();
                } else {
                    return responseJson.getAsJsonArray("choices")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("message")
                        .get("content").getAsString();
                }
            } catch (Exception e) {
                plugin.log(Level.WARNING, "AI request failed: " + e.getMessage());
                return "{}";
            }
        });
    }

    // ==================== RESULT CLASSES ====================

    public record ToxicityResult(boolean toxic, int score, String reason) {}
    public record BehaviorAnalysis(String riskLevel, int riskScore, String summary) {}
    public record AppealReview(String recommendation, int confidence, String reasoning) {}

    // ==================== GETTERS ====================

    public boolean isEnabled() {
        return enabled;
    }

    public String getProvider() {
        return provider;
    }

    public String getModel() {
        return model;
    }
}
