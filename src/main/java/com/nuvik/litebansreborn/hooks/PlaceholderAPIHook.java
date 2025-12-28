package com.nuvik.litebansreborn.hooks;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.models.Punishment;
import com.nuvik.litebansreborn.utils.TimeUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * PlaceholderAPI expansion for LiteBansReborn
 */
public class PlaceholderAPIHook extends PlaceholderExpansion {

    private final LiteBansReborn plugin;
    
    public PlaceholderAPIHook(LiteBansReborn plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public @NotNull String getIdentifier() {
        return "litebansreborn";
    }
    
    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }
    
    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }
    
    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";
        
        try {
            switch (params.toLowerCase()) {
                
                // Ban placeholders
                case "is_banned":
                    return String.valueOf(plugin.getBanManager().getActiveBan(player.getUniqueId())
                            .get(1, TimeUnit.SECONDS) != null);
                
                case "ban_reason":
                    Punishment ban = plugin.getBanManager().getActiveBan(player.getUniqueId())
                            .get(1, TimeUnit.SECONDS);
                    return ban != null ? ban.getReason() : "";
                
                case "ban_executor":
                    Punishment banE = plugin.getBanManager().getActiveBan(player.getUniqueId())
                            .get(1, TimeUnit.SECONDS);
                    return banE != null ? banE.getExecutorName() : "";
                
                case "ban_remaining":
                    Punishment banR = plugin.getBanManager().getActiveBan(player.getUniqueId())
                            .get(1, TimeUnit.SECONDS);
                    if (banR == null) return "";
                    return banR.isPermanent() ? "Permanent" : TimeUtil.formatDuration(banR.getRemainingTime());
                
                // Mute placeholders
                case "is_muted":
                    return String.valueOf(plugin.getMuteManager().getActiveMute(player.getUniqueId())
                            .get(1, TimeUnit.SECONDS) != null);
                
                case "mute_reason":
                    Punishment mute = plugin.getMuteManager().getActiveMute(player.getUniqueId())
                            .get(1, TimeUnit.SECONDS);
                    return mute != null ? mute.getReason() : "";
                
                case "mute_remaining":
                    Punishment muteR = plugin.getMuteManager().getActiveMute(player.getUniqueId())
                            .get(1, TimeUnit.SECONDS);
                    if (muteR == null) return "";
                    return muteR.isPermanent() ? "Permanent" : TimeUtil.formatDuration(muteR.getRemainingTime());
                
                // Warning placeholders
                case "warning_count":
                    return String.valueOf(plugin.getWarnManager().getActiveWarningCount(player.getUniqueId())
                            .get(1, TimeUnit.SECONDS));
                
                // Freeze placeholders
                case "is_frozen":
                    return String.valueOf(plugin.getFreezeManager().isFrozen(player.getUniqueId()));
                
                case "freeze_reason":
                    String freezeReason = plugin.getCacheManager().getFreezeReason(player.getUniqueId());
                    return freezeReason != null ? freezeReason : "";
                
                // Points placeholders
                case "points":
                    return String.valueOf(plugin.getPointManager().getPoints(player.getUniqueId())
                            .get(1, TimeUnit.SECONDS).intValue());
                
                // History placeholders
                case "history_count":
                    return String.valueOf(plugin.getHistoryManager().getPlayerHistoryCount(player.getUniqueId())
                            .get(1, TimeUnit.SECONDS));
                
                // Stats placeholders
                case "stats_total_bans":
                    return String.valueOf(plugin.getHistoryManager().getStats()
                            .get(1, TimeUnit.SECONDS).getTotalBans());
                
                case "stats_active_bans":
                    return String.valueOf(plugin.getHistoryManager().getStats()
                            .get(1, TimeUnit.SECONDS).getActiveBans());
                
                case "stats_total_mutes":
                    return String.valueOf(plugin.getHistoryManager().getStats()
                            .get(1, TimeUnit.SECONDS).getTotalMutes());
                
                case "stats_active_mutes":
                    return String.valueOf(plugin.getHistoryManager().getStats()
                            .get(1, TimeUnit.SECONDS).getActiveMutes());
                
                default:
                    return null;
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            plugin.debug("Placeholder request timed out: " + params);
            return "";
        }
    }
}
