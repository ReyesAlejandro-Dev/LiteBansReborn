package com.nuvik.litebansreborn;

import com.nuvik.litebansreborn.api.LiteBansRebornAPI;
import com.nuvik.litebansreborn.cache.CacheManager;
import com.nuvik.litebansreborn.commands.*;
import com.nuvik.litebansreborn.config.ConfigManager;
import com.nuvik.litebansreborn.config.MessagesManager;
import com.nuvik.litebansreborn.database.DatabaseManager;
import com.nuvik.litebansreborn.hooks.PlaceholderAPIHook;
import com.nuvik.litebansreborn.listeners.*;
import com.nuvik.litebansreborn.managers.*;
import com.nuvik.litebansreborn.notifications.DiscordNotifier;
import com.nuvik.litebansreborn.notifications.TelegramNotifier;
import com.nuvik.litebansreborn.utils.ColorUtil;
import com.nuvik.litebansreborn.utils.UpdateChecker;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * LiteBansReborn - Advanced Punishment Management System
 * A sophisticated and feature-rich alternative to LiteBans
 * 
 * @author Nuvik
 * @version 1.0.0
 */
public class LiteBansReborn extends JavaPlugin {

    private static LiteBansReborn instance;
    private static LiteBansRebornAPI api;
    
    // Managers
    private ConfigManager configManager;
    private MessagesManager messagesManager;
    private DatabaseManager databaseManager;
    private CacheManager cacheManager;
    
    // Punishment Managers
    private BanManager banManager;
    private MuteManager muteManager;
    private WarnManager warnManager;
    private KickManager kickManager;
    private FreezeManager freezeManager;
    
    // Additional Managers
    private ReportManager reportManager;
    private AppealManager appealManager;
    private NoteManager noteManager;
    private HistoryManager historyManager;
    private AltManager altManager;
    private PointManager pointManager;
    private TemplateManager templateManager;
    private GeoIPManager geoIPManager;
    
    // Notification Systems
    private DiscordNotifier discordNotifier;
    private TelegramNotifier telegramNotifier;
    
    // State
    private boolean chatMuted = false;
    
    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();
        instance = this;
        
        // Print startup banner
        printBanner();
        
        // Initialize configurations
        log(Level.INFO, "Loading configurations...");
        initializeConfigurations();
        
        // Initialize database
        log(Level.INFO, "Connecting to database...");
        if (!initializeDatabase()) {
            log(Level.SEVERE, "Failed to connect to database! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Initialize cache
        log(Level.INFO, "Initializing cache system...");
        initializeCache();
        
        // Initialize managers
        log(Level.INFO, "Initializing managers...");
        initializeManagers();
        
        // Initialize API
        log(Level.INFO, "Initializing API...");
        api = new LiteBansRebornAPI(this);
        
        // Register commands
        log(Level.INFO, "Registering commands...");
        registerCommands();
        
        // Register listeners
        log(Level.INFO, "Registering listeners...");
        registerListeners();
        
        // Hook into other plugins
        log(Level.INFO, "Hooking into other plugins...");
        hookIntoPlugins();
        
        // Initialize notifiers
        log(Level.INFO, "Initializing notification systems...");
        initializeNotifiers();
        
        // Check for updates
        if (configManager.getBoolean("general.metrics")) {
            // Initialize bStats here
        }
        
        // Start scheduled tasks
        startScheduledTasks();
        
        long loadTime = System.currentTimeMillis() - startTime;
        log(Level.INFO, "§aLiteBansReborn has been enabled! §7(Loaded in " + loadTime + "ms)");
    }
    
    @Override
    public void onDisable() {
        log(Level.INFO, "Disabling LiteBansReborn...");
        
        // Save all cached data
        if (cacheManager != null) {
            cacheManager.saveAll();
        }
        
        // Close database connection
        if (databaseManager != null) {
            databaseManager.close();
        }
        
        // Shutdown notifiers
        if (discordNotifier != null) {
            discordNotifier.shutdown();
        }
        if (telegramNotifier != null) {
            telegramNotifier.shutdown();
        }
        
        log(Level.INFO, "§cLiteBansReborn has been disabled!");
        instance = null;
    }
    
    private void printBanner() {
        String[] banner = {
            "",
            "§c  _     _ _       ____                  ____      _                     ",
            "§c | |   (_) |_ ___| __ )  __ _ _ __  ___|  _ \\ ___| |__   ___  _ __ _ __ ",
            "§c | |   | | __/ _ \\  _ \\ / _` | '_ \\/ __| |_) / _ \\ '_ \\ / _ \\| '__| '_ \\",
            "§c | |___| | ||  __/ |_) | (_| | | | \\__ \\  _ <  __/ |_) | (_) | |  | | | |",
            "§c |_____|_|\\__\\___|____/ \\__,_|_| |_|___/_| \\_\\___|_.__/ \\___/|_|  |_| |_|",
            "",
            "§7                    Version §e" + getDescription().getVersion() + " §7| Author: §aNuvik",
            "§7                    Advanced Punishment Management System",
            ""
        };
        
        for (String line : banner) {
            Bukkit.getConsoleSender().sendMessage(ColorUtil.translate(line));
        }
    }
    
    private void initializeConfigurations() {
        // Save default configs if not present
        saveDefaultConfig();
        saveResource("messages.yml", false);
        
        // Initialize config managers
        configManager = new ConfigManager(this);
        messagesManager = new MessagesManager(this);
        
        // Load configurations
        configManager.load();
        messagesManager.load();
    }
    
    private boolean initializeDatabase() {
        try {
            databaseManager = new DatabaseManager(this);
            databaseManager.connect();
            databaseManager.createTables();
            return true;
        } catch (Exception e) {
            log(Level.SEVERE, "Database initialization failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private void initializeCache() {
        cacheManager = new CacheManager(this);
    }
    
    private void initializeManagers() {
        // Punishment managers
        banManager = new BanManager(this);
        muteManager = new MuteManager(this);
        warnManager = new WarnManager(this);
        kickManager = new KickManager(this);
        freezeManager = new FreezeManager(this);
        
        // Additional managers
        reportManager = new ReportManager(this);
        appealManager = new AppealManager(this);
        noteManager = new NoteManager(this);
        historyManager = new HistoryManager(this);
        altManager = new AltManager(this);
        pointManager = new PointManager(this);
        templateManager = new TemplateManager(this);
        geoIPManager = new GeoIPManager(this);
        
        // Load templates from config
        templateManager.loadTemplates();
    }
    
    private void registerCommands() {
        // Ban commands
        getCommand("ban").setExecutor(new BanCommand(this));
        getCommand("tempban").setExecutor(new TempBanCommand(this));
        getCommand("ipban").setExecutor(new IPBanCommand(this));
        getCommand("unban").setExecutor(new UnbanCommand(this));
        getCommand("unbanip").setExecutor(new UnbanIPCommand(this));
        
        // Mute commands
        getCommand("mute").setExecutor(new MuteCommand(this));
        getCommand("tempmute").setExecutor(new TempMuteCommand(this));
        getCommand("ipmute").setExecutor(new IPMuteCommand(this));
        getCommand("unmute").setExecutor(new UnmuteCommand(this));
        getCommand("unmuteip").setExecutor(new UnmuteIPCommand(this));
        
        // Kick commands
        getCommand("kick").setExecutor(new KickCommand(this));
        getCommand("kickall").setExecutor(new KickAllCommand(this));
        
        // Warn commands
        getCommand("warn").setExecutor(new WarnCommand(this));
        getCommand("unwarn").setExecutor(new UnwarnCommand(this));
        getCommand("warnings").setExecutor(new WarningsCommand(this));
        
        // Freeze commands
        getCommand("freeze").setExecutor(new FreezeCommand(this));
        getCommand("unfreeze").setExecutor(new UnfreezeCommand(this));
        
        // History commands
        getCommand("history").setExecutor(new HistoryCommand(this));
        getCommand("staffhistory").setExecutor(new StaffHistoryCommand(this));
        getCommand("banlist").setExecutor(new BanListCommand(this));
        getCommand("mutelist").setExecutor(new MuteListCommand(this));
        
        // Check commands
        getCommand("checkban").setExecutor(new CheckBanCommand(this));
        getCommand("checkmute").setExecutor(new CheckMuteCommand(this));
        
        // Utility commands
        getCommand("clearchat").setExecutor(new ClearChatCommand(this));
        getCommand("mutechat").setExecutor(new MuteChatCommand(this));
        getCommand("staffchat").setExecutor(new StaffChatCommand(this));
        
        // Report commands
        getCommand("report").setExecutor(new ReportCommand(this));
        getCommand("reports").setExecutor(new ReportsCommand(this));
        getCommand("handlereport").setExecutor(new HandleReportCommand(this));
        
        // Appeal commands
        getCommand("appeal").setExecutor(new AppealCommand(this));
        getCommand("appeals").setExecutor(new AppealsCommand(this));
        getCommand("handleappeal").setExecutor(new HandleAppealCommand(this));
        
        // Alt detection commands
        getCommand("alts").setExecutor(new AltsCommand(this));
        getCommand("dupeip").setExecutor(new DupeIPCommand(this));
        
        // Note commands
        getCommand("note").setExecutor(new NoteCommand(this));
        getCommand("notes").setExecutor(new NotesCommand(this));
        getCommand("delnote").setExecutor(new DelNoteCommand(this));
        
        // Other commands
        getCommand("rollback").setExecutor(new RollbackCommand(this));
        getCommand("geoip").setExecutor(new GeoIPCommand(this));
        getCommand("allowplayer").setExecutor(new AllowPlayerCommand(this));
        getCommand("punish").setExecutor(new PunishCommand(this));
        
        // Main plugin command
        getCommand("litebansreborn").setExecutor(new MainCommand(this));
    }
    
    private void registerListeners() {
        // Player join/quit listeners
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        
        // Chat listener for mutes and staff chat
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        
        // Freeze listener
        getServer().getPluginManager().registerEvents(new FreezeListener(this), this);
        
        // Command listener for muted players
        getServer().getPluginManager().registerEvents(new CommandListener(this), this);
        
        // GUI listener
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
    }
    
    private void hookIntoPlugins() {
        // PlaceholderAPI
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PlaceholderAPIHook(this).register();
            log(Level.INFO, "§aHooked into PlaceholderAPI!");
        }
    }
    
    private void initializeNotifiers() {
        // Discord
        if (configManager.getBoolean("discord.enabled")) {
            discordNotifier = new DiscordNotifier(this);
            log(Level.INFO, "§aDiscord notifications enabled!");
        }
        
        // Telegram
        if (configManager.getBoolean("telegram.enabled")) {
            telegramNotifier = new TelegramNotifier(this);
            log(Level.INFO, "§aTelegram notifications enabled!");
        }
    }
    
    private void startScheduledTasks() {
        // Point decay task (runs every hour)
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (configManager.getBoolean("points.enabled")) {
                pointManager.decayPoints();
            }
        }, 20L * 60 * 60, 20L * 60 * 60);
        
        // Cache cleanup task (runs every 5 minutes)
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            cacheManager.cleanup();
        }, 20L * 60 * 5, 20L * 60 * 5);
        
        // Warning expiry task (runs every 6 hours)
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            warnManager.expireOldWarnings();
        }, 20L * 60 * 60 * 6, 20L * 60 * 60 * 6);
    }
    
    /**
     * Reload the plugin configuration
     */
    public void reload() {
        configManager.reload();
        messagesManager.reload();
        templateManager.loadTemplates();
        cacheManager.clearAll();
        
        // Reinitialize notifiers
        if (discordNotifier != null) {
            discordNotifier.shutdown();
        }
        if (telegramNotifier != null) {
            telegramNotifier.shutdown();
        }
        initializeNotifiers();
    }
    
    /**
     * Log a message to the console
     */
    public void log(Level level, String message) {
        getLogger().log(level, ColorUtil.translate(message));
    }
    
    /**
     * Debug log (only if debug mode enabled)
     */
    public void debug(String message) {
        if (configManager != null && configManager.getBoolean("general.debug")) {
            log(Level.INFO, "§7[DEBUG] " + message);
        }
    }
    
    // Getters
    public static LiteBansReborn getInstance() {
        return instance;
    }
    
    public static LiteBansRebornAPI getAPI() {
        return api;
    }
    
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
    
    public KickManager getKickManager() {
        return kickManager;
    }
    
    public FreezeManager getFreezeManager() {
        return freezeManager;
    }
    
    public ReportManager getReportManager() {
        return reportManager;
    }
    
    public AppealManager getAppealManager() {
        return appealManager;
    }
    
    public NoteManager getNoteManager() {
        return noteManager;
    }
    
    public HistoryManager getHistoryManager() {
        return historyManager;
    }
    
    public AltManager getAltManager() {
        return altManager;
    }
    
    public PointManager getPointManager() {
        return pointManager;
    }
    
    public TemplateManager getTemplateManager() {
        return templateManager;
    }
    
    public GeoIPManager getGeoIPManager() {
        return geoIPManager;
    }
    
    public DiscordNotifier getDiscordNotifier() {
        return discordNotifier;
    }
    
    public TelegramNotifier getTelegramNotifier() {
        return telegramNotifier;
    }
    
    public boolean isChatMuted() {
        return chatMuted;
    }
    
    public void setChatMuted(boolean chatMuted) {
        this.chatMuted = chatMuted;
    }
}
