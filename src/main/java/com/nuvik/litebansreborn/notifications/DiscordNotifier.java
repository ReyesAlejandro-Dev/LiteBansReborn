package com.nuvik.litebansreborn.notifications;

import com.google.gson.JsonObject;
import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.models.Punishment;
import com.nuvik.litebansreborn.utils.TimeUtil;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Discord Notifier - Sends punishment notifications to Discord webhooks
 */
public class DiscordNotifier {

    private final LiteBansReborn plugin;
    private final OkHttpClient httpClient;
    private String banWebhook;
    private String muteWebhook;
    private String kickWebhook;
    private String warnWebhook;
    private boolean enabled;
    
    public DiscordNotifier(LiteBansReborn plugin) {
        this.plugin = plugin;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
        
        reload();
    }
    
    public void reload() {
        enabled = plugin.getConfigManager().getBoolean("discord.enabled");
        banWebhook = plugin.getConfigManager().getString("discord.webhooks.bans");
        muteWebhook = plugin.getConfigManager().getString("discord.webhooks.mutes");
        kickWebhook = plugin.getConfigManager().getString("discord.webhooks.kicks");
        warnWebhook = plugin.getConfigManager().getString("discord.webhooks.warns");
    }
    
    public void sendBanNotification(Punishment ban) {
        if (!enabled || banWebhook == null || banWebhook.isEmpty()) return;
        
        String title = ban.isPermanent() ? "Permanent Ban" : "Temporary Ban";
        int color = 0xFF0000; // Red
        
        JsonObject embed = createEmbed(title, ban, color);
        sendWebhook(banWebhook, embed);
    }
    
    public void sendMuteNotification(Punishment mute) {
        if (!enabled || muteWebhook == null || muteWebhook.isEmpty()) return;
        
        String title = mute.isPermanent() ? "Permanent Mute" : "Temporary Mute";
        int color = 0xFFA500; // Orange
        
        JsonObject embed = createEmbed(title, mute, color);
        sendWebhook(muteWebhook, embed);
    }
    
    public void sendKickNotification(Punishment kick) {
        if (!enabled || kickWebhook == null || kickWebhook.isEmpty()) return;
        
        int color = 0xFFFF00; // Yellow
        JsonObject embed = createEmbed("Kick", kick, color);
        sendWebhook(kickWebhook, embed);
    }
    
    public void sendWarnNotification(Punishment warn) {
        if (!enabled || warnWebhook == null || warnWebhook.isEmpty()) return;
        
        int color = 0xFFD700; // Gold
        JsonObject embed = createEmbed("Warning", warn, color);
        sendWebhook(warnWebhook, embed);
    }
    
    /**
     * Shutdown the notifier
     */
    public void shutdown() {
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
    }
    
    private JsonObject createEmbed(String title, Punishment punishment, int color) {
        JsonObject embed = new JsonObject();
        embed.addProperty("title", title);
        embed.addProperty("color", color);
        
        StringBuilder description = new StringBuilder();
        description.append("**Player:** ").append(punishment.getTargetName()).append("\n");
        description.append("**Staff:** ").append(punishment.getExecutorName()).append("\n");
        description.append("**Reason:** ").append(punishment.getReason()).append("\n");
        
        if (!punishment.isPermanent() && punishment.getExpiresAt() != null) {
            description.append("**Duration:** ").append(TimeUtil.formatDuration(punishment.getRemainingTime())).append("\n");
            description.append("**Expires:** ").append(TimeUtil.formatDate(punishment.getExpiresAt())).append("\n");
        } else if (punishment.isPermanent()) {
            description.append("**Duration:** Permanent\n");
        }
        
        description.append("**Server:** ").append(punishment.getServer()).append("\n");
        description.append("**ID:** #").append(punishment.getId());
        
        embed.addProperty("description", description.toString());
        
        // Footer with timestamp
        JsonObject footer = new JsonObject();
        footer.addProperty("text", plugin.getDescription().getName() + " v" + plugin.getDescription().getVersion());
        embed.add("footer", footer);
        
        // Timestamp
        embed.addProperty("timestamp", TimeUtil.formatISO(punishment.getCreatedAt()));
        
        return embed;
    }
    
    private void sendWebhook(String url, JsonObject embed) {
        JsonObject payload = new JsonObject();
        payload.addProperty("username", plugin.getConfigManager().getString("discord.bot-name", "LiteBansReborn"));
        
        String avatarUrl = plugin.getConfigManager().getString("discord.avatar-url");
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            payload.addProperty("avatar_url", avatarUrl);
        }
        
        // Create embeds array
        com.google.gson.JsonArray embeds = new com.google.gson.JsonArray();
        embeds.add(embed);
        payload.add("embeds", embeds);
        
        RequestBody body = RequestBody.create(
            payload.toString(),
            MediaType.parse("application/json")
        );
        
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                plugin.debug("Failed to send Discord notification: " + e.getMessage());
            }
            
            @Override
            public void onResponse(Call call, Response response) {
                if (!response.isSuccessful()) {
                    plugin.debug("Discord webhook returned error: " + response.code());
                }
                response.close();
            }
        });
    }
}
