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
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 * LiteBansReborn - Advanced Punishment Management System
 * A sophisticated and feature-rich alternative to LiteBans
 * 
 * v5.9 Improvements:
 * - Fixed startTime field usage
 * - Precompiled regex pattern for log color stripping
 * - Safe command registration with null checks
 * - Proper shutdown order (managers → DB)
 * - Tracked scheduled tasks for proper reload
 * - Removed duplicate manager initialization
 * - API cleanup on disable
 * 
 * @author Nuvik
 * @version 6.0.0
 */
public class LiteBansReborn extends JavaPlugin {

    // Precompiled pattern for color stripping (performance)
    private static final Pattern COLOR_PATTERN = Pattern.compile("§[0-9a-fk-or]");

    private static LiteBansReborn instance;
    private static LiteBansRebornAPI api;
    
    // Startup time (field used by getter)
    private long startTime;
    
    // Tracked scheduled tasks for proper reload
    private final List<BukkitTask> scheduledTasks = new ArrayList<>();
    
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
        startTime = System.currentTimeMillis(); // Use field, not local variable
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
        
        // Initialize ALL managers in one place
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
        if (configManager.getBoolean("general.check-updates", true)) {
            new UpdateChecker(this, 131216);
        }

        if (configManager.getBoolean("general.metrics")) {
            new org.bstats.bukkit.Metrics(this, 20468);
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
        
        // FIRST: Cancel all scheduled tasks
        stopScheduledTasks();
        
        // SECOND: Shutdown services that depend on DB
        if (hwidManager != null) {
            hwidManager.shutdown();
        }
        if (vpnManager != null) {
            vpnManager.shutdown();
        }
        if (clientDetector != null) {
            clientDetector.shutdown();
        }
        if (webPanelServer != null) {
            webPanelServer.stop();
        }
        if (discordBotManager != null) {
            discordBotManager.stop();
        }
        
        // THIRD: Shutdown notifiers
        if (discordNotifier != null) {
            discordNotifier.shutdown();
        }
        if (telegramNotifier != null) {
            telegramNotifier.shutdown();
        }
        
        // FOURTH: Save cache data
        if (cacheManager != null) {
            cacheManager.saveAll();
        }
        
        // LAST: Close database connection
        if (databaseManager != null) {
            databaseManager.close();
        }
        
        log(Level.INFO, "§cLiteBansReborn has been disabled!");
        
        // Clean up static references
        api = null;
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
        saveDefaultConfig();
        saveResource("messages.yml", false);
        
        configManager = new ConfigManager(this);
        messagesManager = new MessagesManager(this);
        
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
    
    /**
     * Initialize ALL managers in one centralized place
     * Avoids duplicate initialization
     */
    private void initializeManagers() {
        // Punishment managers
        banManager = new BanManager(this);
        muteManager = new MuteManager(this);
        warnManager = new WarnManager(this);
        kickManager = new KickManager(this);
        freezeManager = new FreezeManager(this);
        
        // Additional managers
        reportManager = new ReportManager(this);
        ghostMuteManager = new GhostMuteManager(this);
        snapshotManager = new SnapshotManager(this);
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
    
    /**
     * Safe command registration helper
     * Logs error instead of NPE if command not in plugin.yml
     */
    private void registerCmd(String name, CommandExecutor executor, TabCompleter tabCompleter) {
        PluginCommand cmd = getCommand(name);
        if (cmd == null) {
            log(Level.SEVERE, "Command missing in plugin.yml: " + name);
            return;
        }
        cmd.setExecutor(executor);
        if (tabCompleter != null) {
            cmd.setTabCompleter(tabCompleter);
        }
    }
    
    private void registerCommands() {
        // Ban commands
        registerCmd("ban", new BanCommand(this), null);
        registerCmd("tempban", new TempBanCommand(this), null);
        registerCmd("ipban", new IPBanCommand(this), null);
        registerCmd("unban", new UnbanCommand(this), null);
        registerCmd("unbanip", new UnbanIPCommand(this), null);
        
        // Mute commands
        registerCmd("mute", new MuteCommand(this), null);
        registerCmd("tempmute", new TempMuteCommand(this), null);
        registerCmd("ipmute", new IPMuteCommand(this), null);
        registerCmd("unmute", new UnmuteCommand(this), null);
        registerCmd("unmuteip", new UnmuteIPCommand(this), null);
        
        // Kick commands
        registerCmd("kick", new KickCommand(this), null);
        registerCmd("kickall", new KickAllCommand(this), null);
        
        // Warn commands
        registerCmd("warn", new WarnCommand(this), null);
        registerCmd("unwarn", new UnwarnCommand(this), null);
        registerCmd("warnings", new WarningsCommand(this), null);
        
        // Freeze commands
        registerCmd("freeze", new FreezeCommand(this), null);
        registerCmd("unfreeze", new UnfreezeCommand(this), null);
        
        // History commands
        registerCmd("history", new HistoryCommand(this), null);
        registerCmd("staffhistory", new StaffHistoryCommand(this), null);
        registerCmd("banlist", new BanListCommand(this), null);
        registerCmd("mutelist", new MuteListCommand(this), null);
        
        // Check commands
        registerCmd("checkban", new CheckBanCommand(this), null);
        registerCmd("checkmute", new CheckMuteCommand(this), null);
        
        // Utility commands
        registerCmd("clearchat", new ClearChatCommand(this), null);
        registerCmd("mutechat", new MuteChatCommand(this), null);
        registerCmd("staffchat", new StaffChatCommand(this), null);
        
        // Report commands
        registerCmd("report", new ReportCommand(this), null);
        registerCmd("reports", new ReportsCommand(this), null);
        registerCmd("handlereport", new HandleReportCommand(this), null);
        
        // Appeal commands
        registerCmd("appeal", new AppealCommand(this), null);
        registerCmd("appeals", new AppealsCommand(this), null);
        registerCmd("handleappeal", new HandleAppealCommand(this), null);
        
        // Alt detection commands
        registerCmd("alts", new AltsCommand(this), null);
        registerCmd("dupeip", new DupeIPCommand(this), null);
        
        // Note commands
        registerCmd("note", new NoteCommand(this), null);
        registerCmd("notes", new NotesCommand(this), null);
        registerCmd("delnote", new DelNoteCommand(this), null);
        
        // Other commands
        registerCmd("rollback", new RollbackCommand(this), null);
        registerCmd("geoip", new GeoIPCommand(this), null);
        registerCmd("allowplayer", new AllowPlayerCommand(this), null);
        registerCmd("punish", new PunishCommand(this), null);
        registerCmd("ghostmute", new GhostMuteCommand(this), null);
        registerCmd("litebansreborn", new MainCommand(this), null);
        
        // Anti-VPN commands
        VPNCheckCommand vpnCmd = new VPNCheckCommand(this);
        registerCmd("vpncheck", vpnCmd, vpnCmd);
        
        // Client detection commands
        ClientCheckCommand clientCmd = new ClientCheckCommand(this);
        registerCmd("clientcheck", clientCmd, clientCmd);
        
        // V4.0 Commands
        EvidenceCommand evidenceCmd = new EvidenceCommand(this);
        registerCmd("evidence", evidenceCmd, evidenceCmd);
        
        RedemptionCommand redemptionCmd = new RedemptionCommand(this);
        registerCmd("redemption", redemptionCmd, redemptionCmd);
        
        HWIDCommand hwidCmd = new HWIDCommand(this);
        registerCmd("hwid", hwidCmd, hwidCmd);
        
        // V4.5 Commands
        TicketCommand ticketCmd = new TicketCommand(this);
        registerCmd("ticket", ticketCmd, ticketCmd);
        
        VerifyCommand verifyCmd = new VerifyCommand(this);
        registerCmd("verify", verifyCmd, verifyCmd);
        registerCmd("unlink", verifyCmd, null);
        registerCmd("discordinfo", verifyCmd, null);
        registerCmd("whois", verifyCmd, null);
        
        // V5.0 Commands
        MaintenanceCommand maintenanceCmd = new MaintenanceCommand(this);
        registerCmd("maintenance", maintenanceCmd, maintenanceCmd);
        
        if (roleSyncManager != null) {
            RoleSyncCommand roleSyncCmd = new RoleSyncCommand(this);
            registerCmd("rolesync", roleSyncCmd, roleSyncCmd);
        }
        
        // V5.1 Commands
        NetworkCommand networkCmd = new NetworkCommand(this);
        registerCmd("network", networkCmd, networkCmd);
        
        CaseCommand caseCmd = new CaseCommand(this);
        registerCmd("case", caseCmd, caseCmd);
        
        RiskCommand riskCmd = new RiskCommand(this);
        registerCmd("risk", riskCmd, riskCmd);
        
        AICommand aiCmd = new AICommand(this);
        registerCmd("ai", aiCmd, aiCmd);
    }
    
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new FreezeListener(this), this);
        getServer().getPluginManager().registerEvents(new CommandListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
    }
    
    private void hookIntoPlugins() {
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PlaceholderAPIHook(this).register();
            log(Level.INFO, "§aHooked into PlaceholderAPI!");
        }
    }
    
    private void initializeNotifiers() {
        if (configManager.getBoolean("discord.enabled")) {
            discordNotifier = new DiscordNotifier(this);
            log(Level.INFO, "§aDiscord notifications enabled!");
        }
        
        if (configManager.getBoolean("telegram.enabled")) {
            telegramNotifier = new TelegramNotifier(this);
            log(Level.INFO, "§aTelegram notifications enabled!");
        }
    }
    
    /**
     * Start scheduled tasks (tracked for proper reload)
     */
    private void startScheduledTasks() {
        // Point decay task (runs every hour)
        scheduledTasks.add(Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (configManager.getBoolean("points.enabled")) {
                pointManager.decayPoints();
            }
        }, 20L * 60 * 60, 20L * 60 * 60));
        
        // Cache cleanup task (runs every 5 minutes)
        scheduledTasks.add(Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            cacheManager.cleanup();
        }, 20L * 60 * 5, 20L * 60 * 5));
        
        // Warning expiry task (runs every 6 hours)
        scheduledTasks.add(Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            warnManager.expireOldWarnings();
        }, 20L * 60 * 60 * 6, 20L * 60 * 60 * 6));
    }
    
    /**
     * Stop all scheduled tasks
     */
    private void stopScheduledTasks() {
        scheduledTasks.forEach(BukkitTask::cancel);
        scheduledTasks.clear();
    }
    
    /**
     * Reload the plugin configuration
     */
    public void reload() {
        // Stop current scheduled tasks
        stopScheduledTasks();
        
        // Reload configurations
        configManager.reload();
        messagesManager.reload();
        templateManager.loadTemplates();
        cacheManager.clearAll();
        
        // Reinitialize notifiers
        if (discordNotifier != null) {
            discordNotifier.shutdown();
            discordNotifier = null;
        }
        if (telegramNotifier != null) {
            telegramNotifier.shutdown();
            telegramNotifier = null;
        }
        initializeNotifiers();
        
        // Reload HWID Manager
        if (configManager.getBoolean("hardware-ban.enabled", false)) {
            if (hwidManager == null) {
                hwidManager = new HWIDManager(this);
                log(Level.INFO, "Enabled HWID system");
            }
        } else {
            if (hwidManager != null) {
                hwidManager.shutdown();
                hwidManager = null;
                log(Level.INFO, "Disabled HWID system");
            }
        }
        
        // Reload Web Panel
        if (webPanelServer != null) {
            webPanelServer.stop();
            webPanelServer = null;
        }
        if (configManager.getBoolean("web-panel.enabled", false)) {
            webPanelServer = new WebPanelServer(this);
            webPanelServer.start();
        }
        
        // Reload Anti-VPN
        if (vpnManager != null) {
            vpnManager.shutdown();
            vpnManager = null;
        }
        if (configManager.getBoolean("anti-vpn.enabled", false)) {
            vpnManager = new VPNManager(this);
        }
        
        // Reload Client Detector
        if (clientDetector != null) {
            clientDetector.shutdown();
            clientDetector = null;
        }
        if (configManager.getBoolean("client-detection.enabled", true)) {
            clientDetector = new ClientDetector(this);
        }
        
        // Restart scheduled tasks with new config
        startScheduledTasks();
    }
    
    /**
     * Log a message to the console (with precompiled regex for performance)
     */
    public void log(Level level, String message) {
        String cleanMessage = COLOR_PATTERN.matcher(message).replaceAll("");
        getLogger().log(level, cleanMessage);
    }
    
    /**
     * Log a message with colors to console sender (supports colors)
     */
    public void logColored(String message) {
        Bukkit.getConsoleSender().sendMessage("[LiteBansReborn] " + ColorUtil.translate(message));
    }
    
    /**
     * Debug log (only if debug mode enabled)
     */
    public void debug(String message) {
        if (configManager != null && configManager.getBoolean("general.debug")) {
            log(Level.INFO, "[DEBUG] " + message);
        }
    }
    
    // ==================== GETTERS ====================
    
    public static LiteBansReborn getInstance() { return instance; }
    public static LiteBansRebornAPI getAPI() { return api; }
    
    public ConfigManager getConfigManager() { return configManager; }
    public MessagesManager getMessagesManager() { return messagesManager; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public CacheManager getCacheManager() { return cacheManager; }
    
    public BanManager getBanManager() { return banManager; }
    public MuteManager getMuteManager() { return muteManager; }
    public WarnManager getWarnManager() { return warnManager; }
    public KickManager getKickManager() { return kickManager; }
    public FreezeManager getFreezeManager() { return freezeManager; }
    
    public ReportManager getReportManager() { return reportManager; }
    public GhostMuteManager getGhostMuteManager() { return ghostMuteManager; }
    public SnapshotManager getSnapshotManager() { return snapshotManager; }
    public AppealManager getAppealManager() { return appealManager; }
    public NoteManager getNoteManager() { return noteManager; }
    public HistoryManager getHistoryManager() { return historyManager; }
    public AltManager getAltManager() { return altManager; }
    public PointManager getPointManager() { return pointManager; }
    public TemplateManager getTemplateManager() { return templateManager; }
    public GeoIPManager getGeoIPManager() { return geoIPManager; }
    
    public DiscordNotifier getDiscordNotifier() { return discordNotifier; }
    public TelegramNotifier getTelegramNotifier() { return telegramNotifier; }
    public VPNManager getVPNManager() { return vpnManager; }
    public ClientDetector getClientDetector() { return clientDetector; }
    
    public boolean isChatMuted() { return chatMuted; }
    public void setChatMuted(boolean chatMuted) { this.chatMuted = chatMuted; }
    
    // V4.0 Getters
    public EvidenceManager getEvidenceManager() { return evidenceManager; }
    public RateLimitManager getRateLimitManager() { return rateLimitManager; }
    public RedemptionManager getRedemptionManager() { return redemptionManager; }
    public HWIDManager getHWIDManager() { return hwidManager; }
    public WebPanelServer getWebPanelServer() { return webPanelServer; }
    
    // V4.5 Getters
    public DiscordBotManager getDiscordBotManager() { return discordBotManager; }
    public TicketManager getTicketManager() { return ticketManager; }
    public VerificationManager getVerificationManager() { return verificationManager; }
    public ChatFilterManager getChatFilterManager() { return chatFilterManager; }
    
    // V5.0 Getters
    public MaintenanceManager getMaintenanceManager() { return maintenanceManager; }
    public RoleSyncManager getRoleSyncManager() { return roleSyncManager; }
    public AIManager getAIManager() { return aiManager; }
    
    // V5.1 Getters
    public SocialNetworkManager getSocialNetworkManager() { return socialNetworkManager; }
    public CaseFileManager getCaseFileManager() { return caseFileManager; }
    public CrossServerManager getCrossServerManager() { return crossServerManager; }
    public PredictiveManager getPredictiveManager() { return predictiveManager; }
    
    public long getStartTime() { return startTime; }
}
