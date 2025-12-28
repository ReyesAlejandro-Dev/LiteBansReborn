package com.nuvik.litebansreborn.notifications;

import com.google.gson.JsonObject;
import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.models.Punishment;
import com.nuvik.litebansreborn.utils.TimeUtil;
import okhttp3.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;

/**
 * Telegram Notifier - Sends punishment notifications to Telegram
 */
public class TelegramNotifier {

    private final LiteBansReborn plugin;
    private final OkHttpClient httpClient;
    private String botToken;
    private String chatId;
    private boolean enabled;
    
    public TelegramNotifier(LiteBansReborn plugin) {
        this.plugin = plugin;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
        
        reload();
    }
    
    public void reload() {
        enabled = plugin.getConfigManager().getBoolean("telegram.enabled");
        botToken = plugin.getConfigManager().getString("telegram.bot-token");
        chatId = plugin.getConfigManager().getString("telegram.chat-id");
    }
    
    public void sendBanNotification(Punishment ban) {
        if (!enabled || !isConfigured()) return;
        
        String emoji = ban.isPermanent() ? "🔒" : "⏳";
        String title = ban.isPermanent() ? "Permanent Ban" : "Temporary Ban";
        
        sendMessage(formatMessage(emoji, title, ban));
    }
    
    public void sendMuteNotification(Punishment mute) {
        if (!enabled || !isConfigured()) return;
        
        String emoji = mute.isPermanent() ? "🔇" : "⏰";
        String title = mute.isPermanent() ? "Permanent Mute" : "Temporary Mute";
        
        sendMessage(formatMessage(emoji, title, mute));
    }
    
    public void sendKickNotification(Punishment kick) {
        if (!enabled || !isConfigured()) return;
        
        sendMessage(formatMessage("👢", "Kick", kick));
    }
    
    public void sendWarnNotification(Punishment warn) {
        if (!enabled || !isConfigured()) return;
        
        sendMessage(formatMessage("⚠️", "Warning", warn));
    }
    
    private String formatMessage(String emoji, String title, Punishment punishment) {
        StringBuilder message = new StringBuilder();
        
        message.append(emoji).append(" *").append(title).append("*\n\n");
        message.append("👤 *Player:* `").append(escapeMarkdown(punishment.getTargetName())).append("`\n");
        message.append("⚔️ *Staff:* `").append(escapeMarkdown(punishment.getExecutorName())).append("`\n");
        message.append("📝 *Reason:* ").append(escapeMarkdown(punishment.getReason())).append("\n");
        
        if (!punishment.isPermanent() && punishment.getExpiresAt() != null) {
            message.append("⏱️ *Duration:* ").append(TimeUtil.formatDuration(punishment.getRemainingTime())).append("\n");
            message.append("📅 *Expires:* ").append(TimeUtil.formatDate(punishment.getExpiresAt())).append("\n");
        } else if (punishment.isPermanent()) {
            message.append("⏱️ *Duration:* Permanent\n");
        }
        
        message.append("🖥️ *Server:* ").append(escapeMarkdown(punishment.getServer())).append("\n");
        message.append("🔢 *ID:* #").append(punishment.getId());
        
        return message.toString();
    }
    
    private void sendMessage(String message) {
        try {
            String url = "https://api.telegram.org/bot" + botToken + "/sendMessage" +
                    "?chat_id=" + URLEncoder.encode(chatId, "UTF-8") +
                    "&text=" + URLEncoder.encode(message, "UTF-8") +
                    "&parse_mode=Markdown";
            
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();
            
            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    plugin.debug("Failed to send Telegram notification: " + e.getMessage());
                }
                
                @Override
                public void onResponse(Call call, Response response) {
                    if (!response.isSuccessful()) {
                        plugin.debug("Telegram API returned error: " + response.code());
                    }
                    response.close();
                }
            });
        } catch (UnsupportedEncodingException e) {
            plugin.debug("Failed to encode Telegram message: " + e.getMessage());
        }
    }
    
    /**
     * Shutdown the notifier
     */
    public void shutdown() {
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
    }
    
    private boolean isConfigured() {
        return botToken != null && !botToken.isEmpty() && chatId != null && !chatId.isEmpty();
    }
    
    private String escapeMarkdown(String text) {
        if (text == null) return "";
        return text.replace("_", "\\_")
                   .replace("*", "\\*")
                   .replace("[", "\\[")
                   .replace("`", "\\`");
    }
}
