package com.nuvik.litebansreborn;

import com.nuvik.litebansreborn.api.LiteBansRebornAPI;
import com.nuvik.litebansreborn.antivpn.ClientDetector;
import com.nuvik.litebansreborn.antivpn.VPNManager;
import com.nuvik.litebansreborn.cache.CacheManager;
import com.nuvik.litebansreborn.commands.*;
import com.nuvik.litebansreborn.config.ConfigManager;
import com.nuvik.litebansreborn.config.MessagesManager;
import com.nuvik.litebansreborn.database.DatabaseManager;
import com.nuvik.litebansreborn.discord.DiscordBotManager;
import com.nuvik.litebansreborn.hooks.PlaceholderAPIHook;
import com.nuvik.litebansreborn.listeners.*;
import com.nuvik.litebansreborn.managers.*;
import com.nuvik.litebansreborn.notifications.DiscordNotifier;
import com.nuvik.litebansreborn.notifications.TelegramNotifier;
import com.nuvik.litebansreborn.web.WebPanelServer;
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
 * @version 5.1.0
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
    private GhostMuteManager ghostMuteManager;
    private SnapshotManager snapshotManager;
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
    
    // Anti-VPN & Client Detection
    private VPNManager vpnManager;
    private ClientDetector clientDetector;
    
    // V4.0 Features
    private EvidenceManager evidenceManager;
    private RateLimitManager rateLimitManager;
    private RedemptionManager redemptionManager;
    private HWIDManager hwidManager;
    private WebPanelServer webPanelServer;
    
    // V4.5 Features
    private DiscordBotManager discordBotManager;
    private TicketManager ticketManager;
    private VerificationManager verificationManager;
    private ChatFilterManager chatFilterManager;
    
    // V5.0 Features
    private MaintenanceManager maintenanceManager;
    private RoleSyncManager roleSyncManager;
    private AIManager aiManager;
    
    // V5.1 Features
    private SocialNetworkManager socialNetworkManager;
    private CaseFileManager caseFileManager;
    private CrossServerManager crossServerManager;
    private PredictiveManager predictiveManager;
    
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
        
        // Initialize Anti-VPN
        if (configManager.getBoolean("anti-vpn.enabled", false)) {
            log(Level.INFO, "Initializing Anti-VPN system...");
            vpnManager = new VPNManager(this);
        }
        
        // Initialize Client Detection
        if (configManager.getBoolean("client-detection.enabled", true)) {
            log(Level.INFO, "Initializing Client Detection...");
            clientDetector = new ClientDetector(this);
        }
        
        // Initialize Report Manager
        reportManager = new ReportManager(this);
        ghostMuteManager = new GhostMuteManager(this);
        snapshotManager = new SnapshotManager(this);
        
        // V4.0 Features
        log(Level.INFO, "Initializing v4.0 features...");
        evidenceManager = new EvidenceManager(this);
        rateLimitManager = new RateLimitManager(this);
        redemptionManager = new RedemptionManager(this);
        
        if (configManager.getBoolean("hardware-ban.enabled", false)) {
            hwidManager = new HWIDManager(this);
        }
        
        if (configManager.getBoolean("web-panel.enabled", false)) {
            webPanelServer = new WebPanelServer(this);
            webPanelServer.start();
        }
        
        // V4.5 Features
        log(Level.INFO, "Initializing v4.5 features...");
        ticketManager = new TicketManager(this);
        verificationManager = new VerificationManager(this);
        chatFilterManager = new ChatFilterManager(this);
        
        // Discord Bot (JDA)
        discordBotManager = new DiscordBotManager(this);
        if (configManager.getBoolean("discord-bot.enabled", false)) {
            log(Level.INFO, "Starting Discord bot...");
            discordBotManager.start();
        }
        
        // V5.0 Features
        log(Level.INFO, "Initializing v5.0 features...");
        maintenanceManager = new MaintenanceManager(this);
        aiManager = new AIManager(this);
        
        try {
            if (getServer().getPluginManager().getPlugin("LuckPerms") != null) {
                roleSyncManager = new RoleSyncManager(this);
            } else {
                log(Level.WARNING, "LuckPerms not found. Role Sync disabled.");
            }
        } catch (Throwable e) {
            log(Level.WARNING, "Could not initialize Role Sync (LuckPerms missing?): " + e.getMessage());
        }
        
        // V5.1 Features
        log(Level.INFO, "Initializing v5.1 features...");
        socialNetworkManager = new SocialNetworkManager(this);
        caseFileManager = new CaseFileManager(this);
        predictiveManager = new PredictiveManager(this);
        
        if (configManager.getBoolean("cross-server.enabled", false)) {
            crossServerManager = new CrossServerManager(this);
        }
        
        // Check for updates
        if (configManager.getBoolean("general.metrics")) {
            // Initialize bStats here
        }
        
        // Start scheduled tasks
        startScheduledTasks();
        
        long loadTime = System.currentTimeMillis() - startTime;
        log(Level.INFO, "§aLiteBansReborn has been enabled! §7(Loaded in " + loadTime + "ms)");
        
        // Show Web Panel info if enabled
        if (webPanelServer != null && configManager.getBoolean("web-panel.enabled", false)) {
            int port = configManager.getInt("web-panel.port", 8080);
            log(Level.INFO, "§b✦ Web Panel: §fhttp://localhost:" + port);
        }
        
        // Show Discord Bot status
        if (discordBotManager != null && discordBotManager.isEnabled()) {
            log(Level.INFO, "§b✦ Discord Bot: §aConnected!");
        }
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
        
        // Shutdown Anti-VPN
        if (vpnManager != null) {
            vpnManager.shutdown();
        }
        
        // Shutdown Client Detector
        if (clientDetector != null) {
            clientDetector.shutdown();
        }
        
        // V4.0 Shutdown
        if (hwidManager != null) {
            hwidManager.shutdown();
        }
        if (webPanelServer != null) {
            webPanelServer.stop();
        }
        
        // V4.5 Shutdown
        if (discordBotManager != null) {
            discordBotManager.stop();
        }
        
        log(Level.INFO, "§cLiteBansReborn has been disabled!");
        instance = null;
    }
    
    private void printBanner() {
        String[] banner = {
            "",
            "§6  _     _ _       ____                  ____      _                     ",
            "§6 | |   (_) |_ ___| __ )  __ _ _ __  ___|  _ \\ ___| |__   ___  _ __ _ __ ",
            "§6 | |   | | __/ _ \\  _ \\ / _` | '_ \\/ __| |_) / _ \\ '_ \\ / _ \\| '__| '_ \\",
            "§6 | |___| | ||  __/ |_) | (_| | | | \\__ \\  _ <  __/ |_) | (_) | |  | | | |",
            "§6 |_____|_|\\__\\___|____/ \\__,_|_| |_|___/_| \\_\\___|_.__/ \\___/|_|  |_| |_|",
            "",
            "§7                    Version §e" + getDescription().getVersion() + " §7| Author: §aNuvik",
            "§7                    Advanced Punishment Management System",
            "",
            "§8━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
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
        
        // Ghost Mute
        getCommand("ghostmute").setExecutor(new GhostMuteCommand(this));
        
        // Main plugin command
        getCommand("litebansreborn").setExecutor(new MainCommand(this));
        
        // Anti-VPN commands
        VPNCheckCommand vpnCmd = new VPNCheckCommand(this);
        getCommand("vpncheck").setExecutor(vpnCmd);
        getCommand("vpncheck").setTabCompleter(vpnCmd);
        
        // Client detection commands
        ClientCheckCommand clientCmd = new ClientCheckCommand(this);
        getCommand("clientcheck").setExecutor(clientCmd);
        getCommand("clientcheck").setTabCompleter(clientCmd);
        
        // V4.0 Commands
        EvidenceCommand evidenceCmd = new EvidenceCommand(this);
        getCommand("evidence").setExecutor(evidenceCmd);
        getCommand("evidence").setTabCompleter(evidenceCmd);
        
        RedemptionCommand redemptionCmd = new RedemptionCommand(this);
        getCommand("redemption").setExecutor(redemptionCmd);
        getCommand("redemption").setTabCompleter(redemptionCmd);
        
        HWIDCommand hwidCmd = new HWIDCommand(this);
        getCommand("hwid").setExecutor(hwidCmd);
        getCommand("hwid").setTabCompleter(hwidCmd);
        
        // V4.5 Commands
        TicketCommand ticketCmd = new TicketCommand(this);
        getCommand("ticket").setExecutor(ticketCmd);
        getCommand("ticket").setTabCompleter(ticketCmd);
        
        VerifyCommand verifyCmd = new VerifyCommand(this);
        getCommand("verify").setExecutor(verifyCmd);
        getCommand("verify").setTabCompleter(verifyCmd);
        getCommand("unlink").setExecutor(verifyCmd);
        getCommand("discordinfo").setExecutor(verifyCmd);
        getCommand("whois").setExecutor(verifyCmd);
        
        // V5.0 Commands
        MaintenanceCommand maintenanceCmd = new MaintenanceCommand(this);
        getCommand("maintenance").setExecutor(maintenanceCmd);
        getCommand("maintenance").setTabCompleter(maintenanceCmd);
        
        if (roleSyncManager != null) {
            RoleSyncCommand roleSyncCmd = new RoleSyncCommand(this);
            getCommand("rolesync").setExecutor(roleSyncCmd);
            getCommand("rolesync").setTabCompleter(roleSyncCmd);
        }
        
        // V5.1 Commands
        NetworkCommand networkCmd = new NetworkCommand(this);
        getCommand("network").setExecutor(networkCmd);
        getCommand("network").setTabCompleter(networkCmd);
        
        CaseCommand caseCmd = new CaseCommand(this);
        getCommand("case").setExecutor(caseCmd);
        getCommand("case").setTabCompleter(caseCmd);
        
        RiskCommand riskCmd = new RiskCommand(this);
        getCommand("risk").setExecutor(riskCmd);
        getCommand("risk").setTabCompleter(riskCmd);
        
        AICommand aiCmd = new AICommand(this);
        getCommand("ai").setExecutor(aiCmd);
        getCommand("ai").setTabCompleter(aiCmd);
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
    
    public GhostMuteManager getGhostMuteManager() {
        return ghostMuteManager;
    }
    
    public SnapshotManager getSnapshotManager() {
        return snapshotManager;
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
    
    public VPNManager getVPNManager() {
        return vpnManager;
    }
    
    public ClientDetector getClientDetector() {
        return clientDetector;
    }
    
    public boolean isChatMuted() {
        return chatMuted;
    }
    
    public void setChatMuted(boolean chatMuted) {
        this.chatMuted = chatMuted;
    }
    
    // V4.0 Getters
    public EvidenceManager getEvidenceManager() {
        return evidenceManager;
    }
    
    public RateLimitManager getRateLimitManager() {
        return rateLimitManager;
    }
    
    public RedemptionManager getRedemptionManager() {
        return redemptionManager;
    }
    
    public HWIDManager getHWIDManager() {
        return hwidManager;
    }
    
    public WebPanelServer getWebPanelServer() {
        return webPanelServer;
    }
    
    // V4.5 Getters
    public DiscordBotManager getDiscordBotManager() {
        return discordBotManager;
    }
    
    public TicketManager getTicketManager() {
        return ticketManager;
    }
    
    public VerificationManager getVerificationManager() {
        return verificationManager;
    }
    
    public ChatFilterManager getChatFilterManager() {
        return chatFilterManager;
    }
    
    // V5.0 Getters
    public MaintenanceManager getMaintenanceManager() {
        return maintenanceManager;
    }
    
    public RoleSyncManager getRoleSyncManager() {
        return roleSyncManager;
    }
    
    public AIManager getAIManager() {
        return aiManager;
    }
    
    // V5.1 Getters
    public SocialNetworkManager getSocialNetworkManager() {
        return socialNetworkManager;
    }
    
    public CaseFileManager getCaseFileManager() {
        return caseFileManager;
    }
    
    public CrossServerManager getCrossServerManager() {
        return crossServerManager;
    }
    
    public PredictiveManager getPredictiveManager() {
        return predictiveManager;
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    private long startTime = System.currentTimeMillis();
}
