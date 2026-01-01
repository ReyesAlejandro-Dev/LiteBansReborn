package com.nuvik.litebansreborn.services;

import com.nuvik.litebansreborn.cache.CacheManager;
import com.nuvik.litebansreborn.config.ConfigManager;
import com.nuvik.litebansreborn.config.MessagesManager;
import com.nuvik.litebansreborn.database.DatabaseManager;
import com.nuvik.litebansreborn.managers.*;
import com.nuvik.litebansreborn.notifications.DiscordNotifier;
import com.nuvik.litebansreborn.notifications.TelegramNotifier;

import java.util.logging.Level;

/**
 * Service Locator Pattern Implementation
 * 
 * Provides centralized access to plugin services with
 * support for dependency injection and easier testing.
 * 
 * Usage:
 * - For production: Use ServiceLocator.getInstance().initialize(plugin)
 * - For testing: Create mocks and inject via setters
 * 
 * @author Nuvik
 * @version 6.0.0
 */
public class ServiceLocator {
    
    private static ServiceLocator instance;
    
    // Core services
    private ConfigManager configManager;
    private MessagesManager messagesManager;
    private DatabaseManager databaseManager;
    private CacheManager cacheManager;
    
    // Punishment managers
    private BanManager banManager;
    private MuteManager muteManager;
    private WarnManager warnManager;
    private FreezeManager freezeManager;
    private HistoryManager historyManager;
    
    // Feature managers
    private AIManager aiManager;
    private ChatFilterManager chatFilterManager;
    private CaseFileManager caseFileManager;
    private PredictiveManager predictiveManager;
    private CrossServerManager crossServerManager;
    
    // Notifiers
    private DiscordNotifier discordNotifier;
    private TelegramNotifier telegramNotifier;
    
    // Logging callback
    private LoggingCallback loggingCallback;
    
    private ServiceLocator() {
        // Private constructor for singleton
    }
    
    /**
     * Get the singleton instance
     */
    public static synchronized ServiceLocator getInstance() {
        if (instance == null) {
            instance = new ServiceLocator();
        }
        return instance;
    }
    
    /**
     * Reset instance (for testing)
     */
    public static synchronized void reset() {
        instance = null;
    }
    
    // ==================== SETTERS (for initialization and testing) ====================
    
    public void setConfigManager(ConfigManager configManager) {
        this.configManager = configManager;
    }
    
    public void setMessagesManager(MessagesManager messagesManager) {
        this.messagesManager = messagesManager;
    }
    
    public void setDatabaseManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }
    
    public void setCacheManager(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }
    
    public void setBanManager(BanManager banManager) {
        this.banManager = banManager;
    }
    
    public void setMuteManager(MuteManager muteManager) {
        this.muteManager = muteManager;
    }
    
    public void setWarnManager(WarnManager warnManager) {
        this.warnManager = warnManager;
    }
    
    public void setFreezeManager(FreezeManager freezeManager) {
        this.freezeManager = freezeManager;
    }
    
    public void setHistoryManager(HistoryManager historyManager) {
        this.historyManager = historyManager;
    }
    
    public void setAIManager(AIManager aiManager) {
        this.aiManager = aiManager;
    }
    
    public void setChatFilterManager(ChatFilterManager chatFilterManager) {
        this.chatFilterManager = chatFilterManager;
    }
    
    public void setCaseFileManager(CaseFileManager caseFileManager) {
        this.caseFileManager = caseFileManager;
    }
    
    public void setPredictiveManager(PredictiveManager predictiveManager) {
        this.predictiveManager = predictiveManager;
    }
    
    public void setCrossServerManager(CrossServerManager crossServerManager) {
        this.crossServerManager = crossServerManager;
    }
    
    public void setDiscordNotifier(DiscordNotifier discordNotifier) {
        this.discordNotifier = discordNotifier;
    }
    
    public void setTelegramNotifier(TelegramNotifier telegramNotifier) {
        this.telegramNotifier = telegramNotifier;
    }
    
    public void setLoggingCallback(LoggingCallback loggingCallback) {
        this.loggingCallback = loggingCallback;
    }
    
    // ==================== GETTERS ====================
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public MessagesManager getMessagesManager() {
        return messagesManager;
    }
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public CacheManager getCacheManager() {
        return cacheManager;
    }
    
    public BanManager getBanManager() {
        return banManager;
    }
    
    public MuteManager getMuteManager() {
        return muteManager;
    }
    
    public WarnManager getWarnManager() {
        return warnManager;
    }
    
    public FreezeManager getFreezeManager() {
        return freezeManager;
    }
    
    public HistoryManager getHistoryManager() {
        return historyManager;
    }
    
    public AIManager getAIManager() {
        return aiManager;
    }
    
    public ChatFilterManager getChatFilterManager() {
        return chatFilterManager;
    }
    
    public CaseFileManager getCaseFileManager() {
        return caseFileManager;
    }
    
    public PredictiveManager getPredictiveManager() {
        return predictiveManager;
    }
    
    public CrossServerManager getCrossServerManager() {
        return crossServerManager;
    }
    
    public DiscordNotifier getDiscordNotifier() {
        return discordNotifier;
    }
    
    public TelegramNotifier getTelegramNotifier() {
        return telegramNotifier;
    }
    
    // ==================== LOGGING ====================
    
    public void log(Level level, String message) {
        if (loggingCallback != null) {
            loggingCallback.log(level, message);
        }
    }
    
    public void debug(String message) {
        if (loggingCallback != null && configManager != null && configManager.isDebugEnabled()) {
            loggingCallback.log(Level.INFO, "[DEBUG] " + message);
        }
    }
    
    // ==================== CALLBACK INTERFACE ====================
    
    @FunctionalInterface
    public interface LoggingCallback {
        void log(Level level, String message);
    }
}
