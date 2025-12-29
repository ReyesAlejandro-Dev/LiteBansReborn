package com.nuvik.litebansreborn.database;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Database Manager - Handles all database connections and operations
 * Supports: MySQL, MariaDB, PostgreSQL, SQLite, H2
 */
public class DatabaseManager {

    private final LiteBansReborn plugin;
    private HikariDataSource dataSource;
    private DatabaseType databaseType;
    private String tablePrefix;
    
    public DatabaseManager(LiteBansReborn plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Connect to the database
     */
    public void connect() throws Exception {
        String type = plugin.getConfigManager().getString("database.type").toUpperCase();
        this.databaseType = DatabaseType.valueOf(type);
        this.tablePrefix = plugin.getConfigManager().getString("database.table-prefix");
        
        HikariConfig config = new HikariConfig();
        
        switch (databaseType) {
            case MYSQL:
            case MARIADB:
                configureMySQLOrMariaDB(config);
                break;
            case POSTGRESQL:
                configurePostgreSQL(config);
                break;
            case SQLITE:
                configureSQLite(config);
                break;
            case H2:
            default:
                configureH2(config);
                break;
        }
        
        // Common pool settings
        config.setMaximumPoolSize(plugin.getConfigManager().getInt("database.pool.maximum-pool-size"));
        config.setMinimumIdle(plugin.getConfigManager().getInt("database.pool.minimum-idle"));
        config.setIdleTimeout(plugin.getConfigManager().getInt("database.pool.idle-timeout"));
        config.setMaxLifetime(plugin.getConfigManager().getInt("database.pool.max-lifetime"));
        config.setConnectionTimeout(plugin.getConfigManager().getInt("database.pool.connection-timeout"));
        
        config.setPoolName("LiteBansReborn-Pool");
        
        dataSource = new HikariDataSource(config);
        
        plugin.log(Level.INFO, "Connected to " + databaseType.name() + " database successfully!");
    }
    
    private void configureMySQLOrMariaDB(HikariConfig config) {
        String host = plugin.getConfigManager().getString("database.host");
        int port = plugin.getConfigManager().getInt("database.port");
        String database = plugin.getConfigManager().getString("database.database");
        String username = plugin.getConfigManager().getString("database.username");
        String password = plugin.getConfigManager().getString("database.password");
        boolean ssl = plugin.getConfigManager().getBoolean("database.ssl");
        
        String driverClass = databaseType == DatabaseType.MARIADB 
            ? "org.mariadb.jdbc.Driver" 
            : "com.mysql.cj.jdbc.Driver";
        
        config.setDriverClassName(driverClass);
        config.setJdbcUrl(String.format("jdbc:%s://%s:%d/%s?useSSL=%b&autoReconnect=true&serverTimezone=UTC",
            databaseType == DatabaseType.MARIADB ? "mariadb" : "mysql",
            host, port, database, ssl));
        config.setUsername(username);
        config.setPassword(password);
        
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
    }
    
    private void configurePostgreSQL(HikariConfig config) {
        String host = plugin.getConfigManager().getString("database.host");
        int port = plugin.getConfigManager().getInt("database.port");
        String database = plugin.getConfigManager().getString("database.database");
        String username = plugin.getConfigManager().getString("database.username");
        String password = plugin.getConfigManager().getString("database.password");
        boolean ssl = plugin.getConfigManager().getBoolean("database.ssl");
        
        config.setDriverClassName("org.postgresql.Driver");
        config.setJdbcUrl(String.format("jdbc:postgresql://%s:%d/%s?ssl=%b",
            host, port, database, ssl));
        config.setUsername(username);
        config.setPassword(password);
    }
    
    private void configureSQLite(HikariConfig config) {
        File dataFolder = plugin.getDataFolder();
        File databaseFile = new File(dataFolder, "database.db");
        
        config.setDriverClassName("org.sqlite.JDBC");
        config.setJdbcUrl("jdbc:sqlite:" + databaseFile.getAbsolutePath());
        
        // SQLite specific settings
        config.setMaximumPoolSize(1); // SQLite doesn't support multiple connections well
        config.addDataSourceProperty("journal_mode", "WAL");
        config.addDataSourceProperty("synchronous", "NORMAL");
    }
    
    private void configureH2(HikariConfig config) {
        File dataFolder = plugin.getDataFolder();
        File databaseFile = new File(dataFolder, "database");
        
        config.setDriverClassName("org.h2.Driver");
        config.setJdbcUrl("jdbc:h2:" + databaseFile.getAbsolutePath() + ";MODE=MySQL");
        config.setUsername("sa");
        config.setPassword("");
    }
    
    /**
     * Create all required database tables
     */
    public void createTables() throws SQLException {
        try (Connection conn = getConnection()) {
            // Punishments table
            execute(conn, """
                CREATE TABLE IF NOT EXISTS %spunishments (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    type VARCHAR(32) NOT NULL,
                    target_uuid VARCHAR(36) NOT NULL,
                    target_name VARCHAR(32) NOT NULL,
                    target_ip VARCHAR(45),
                    executor_uuid VARCHAR(36) NOT NULL,
                    executor_name VARCHAR(32) NOT NULL,
                    reason TEXT,
                    server VARCHAR(64),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    expires_at TIMESTAMP NULL,
                    active BOOLEAN DEFAULT TRUE,
                    removed_at TIMESTAMP NULL,
                    removed_by_uuid VARCHAR(36),
                    removed_by_name VARCHAR(32),
                    remove_reason TEXT,
                    silent BOOLEAN DEFAULT FALSE,
                    ip_based BOOLEAN DEFAULT FALSE,
                    INDEX idx_target_uuid (target_uuid),
                    INDEX idx_target_ip (target_ip),
                    INDEX idx_executor_uuid (executor_uuid),
                    INDEX idx_type (type),
                    INDEX idx_active (active)
                )
                """.formatted(tablePrefix));
            
            // Players table
            execute(conn, """
                CREATE TABLE IF NOT EXISTS %splayers (
                    uuid VARCHAR(36) PRIMARY KEY,
                    last_known_name VARCHAR(32) NOT NULL,
                    last_known_ip VARCHAR(45),
                    first_join TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    punishment_points DOUBLE DEFAULT 0,
                    ip_ban_exempt BOOLEAN DEFAULT FALSE,
                    INDEX idx_last_known_name (last_known_name),
                    INDEX idx_last_known_ip (last_known_ip)
                )
                """.formatted(tablePrefix));
            
            // Player IPs table (for alt detection)
            execute(conn, """
                CREATE TABLE IF NOT EXISTS %splayer_ips (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    uuid VARCHAR(36) NOT NULL,
                    ip VARCHAR(45) NOT NULL,
                    first_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE KEY unique_uuid_ip (uuid, ip),
                    INDEX idx_ip (ip)
                )
                """.formatted(tablePrefix));
            
            // Player names table (for name history)
            execute(conn, """
                CREATE TABLE IF NOT EXISTS %splayer_names (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    uuid VARCHAR(36) NOT NULL,
                    name VARCHAR(32) NOT NULL,
                    first_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE KEY unique_uuid_name (uuid, name),
                    INDEX idx_name (name)
                )
                """.formatted(tablePrefix));
            
            // Reports table
            execute(conn, """
                CREATE TABLE IF NOT EXISTS %sreports (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    reporter_uuid VARCHAR(36) NOT NULL,
                    reporter_name VARCHAR(32) NOT NULL,
                    reported_uuid VARCHAR(36) NOT NULL,
                    reported_name VARCHAR(32) NOT NULL,
                    reason TEXT NOT NULL,
                    category VARCHAR(32),
                    server VARCHAR(64),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    status VARCHAR(16) DEFAULT 'pending',
                    handled_at TIMESTAMP NULL,
                    handled_by_uuid VARCHAR(36),
                    handled_by_name VARCHAR(32),
                    resolution TEXT,
                    INDEX idx_reported_uuid (reported_uuid),
                    INDEX idx_status (status)
                )
                """.formatted(tablePrefix));
            
            // Appeals table
            execute(conn, """
                CREATE TABLE IF NOT EXISTS %sappeals (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    punishment_id BIGINT NOT NULL,
                    punishment_type VARCHAR(32) NOT NULL,
                    player_uuid VARCHAR(36) NOT NULL,
                    player_name VARCHAR(32) NOT NULL,
                    message TEXT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    status VARCHAR(16) DEFAULT 'pending',
                    handled_at TIMESTAMP NULL,
                    handled_by_uuid VARCHAR(36),
                    handled_by_name VARCHAR(32),
                    response TEXT,
                    INDEX idx_punishment_id (punishment_id),
                    INDEX idx_player_uuid (player_uuid),
                    INDEX idx_status (status)
                )
                """.formatted(tablePrefix));
            
            // Notes table
            execute(conn, """
                CREATE TABLE IF NOT EXISTS %snotes (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    target_uuid VARCHAR(36) NOT NULL,
                    target_name VARCHAR(32) NOT NULL,
                    author_uuid VARCHAR(36) NOT NULL,
                    author_name VARCHAR(32) NOT NULL,
                    content TEXT NOT NULL,
                    server VARCHAR(64),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_target_uuid (target_uuid)
                )
                """.formatted(tablePrefix));
            
            // Template offense counts (for punishment ladders)
            execute(conn, """
                CREATE TABLE IF NOT EXISTS %stemplate_offenses (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    player_uuid VARCHAR(36) NOT NULL,
                    template_id VARCHAR(64) NOT NULL,
                    offense_count INT DEFAULT 0,
                    last_offense TIMESTAMP,
                    UNIQUE KEY unique_player_template (player_uuid, template_id)
                )
                """.formatted(tablePrefix));
            
            // Allowed players (IP ban bypass)
            execute(conn, """
                CREATE TABLE IF NOT EXISTS %sallowed_players (
                    uuid VARCHAR(36) PRIMARY KEY,
                    added_by_uuid VARCHAR(36) NOT NULL,
                    added_by_name VARCHAR(32) NOT NULL,
                    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """.formatted(tablePrefix));
            
            plugin.log(Level.INFO, "Database tables created successfully!");
        }
    }
    
    /**
     * Execute a SQL statement
     */
    private void execute(Connection conn, String sql) throws SQLException {
        // Adapt SQL for different database types
        sql = adaptSQL(sql);
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        }
    }
    
    /**
     * Adapt SQL for the current database type
     */
    private String adaptSQL(String sql) {
        switch (databaseType) {
            case SQLITE:
                // SQLite doesn't support AUTO_INCREMENT, uses AUTOINCREMENT
                sql = sql.replace("AUTO_INCREMENT", "");
                sql = sql.replace("BIGINT", "INTEGER");
                sql = sql.replace("TIMESTAMP DEFAULT CURRENT_TIMESTAMP", "TEXT DEFAULT (datetime('now'))");
                sql = sql.replace("TIMESTAMP NULL", "TEXT");
                sql = sql.replace("TIMESTAMP", "TEXT");
                sql = sql.replace("BOOLEAN", "INTEGER");
                // Remove INDEX definitions (SQLite uses different syntax)
                sql = sql.replaceAll(",\\s*INDEX [^,)]+", "");
                sql = sql.replaceAll(",\\s*UNIQUE KEY [^,)]+", "");
                break;
            case H2:
                // H2 doesn't support inline INDEX definitions well - remove them
                sql = sql.replaceAll(",\\s*INDEX [a-z_]+ \\([^)]+\\)", "");
                // Convert UNIQUE KEY to just UNIQUE
                sql = sql.replaceAll(",\\s*UNIQUE KEY [a-z_]+ (\\([^)]+\\))", ", UNIQUE $1");
                break;
            case POSTGRESQL:
                // PostgreSQL uses SERIAL instead of AUTO_INCREMENT
                sql = sql.replace("BIGINT AUTO_INCREMENT", "BIGSERIAL");
                sql = sql.replace("INT AUTO_INCREMENT", "SERIAL");
                sql = sql.replace("BOOLEAN", "BOOLEAN");
                // PostgreSQL uses different index syntax
                sql = sql.replaceAll(",\\s*INDEX [^,)]+", "");
                sql = sql.replaceAll(",\\s*UNIQUE KEY ([a-z_]+) \\(([^)]+)\\)", ", UNIQUE ($2)");
                break;
        }
        return sql;
    }
    
    /**
     * Get a connection from the pool
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("Database not connected!");
        }
        return dataSource.getConnection();
    }
    
    /**
     * Execute an async query
     */
    public <T> CompletableFuture<T> queryAsync(DatabaseCallback<T> callback) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection()) {
                return callback.execute(conn);
            } catch (SQLException e) {
                plugin.log(Level.SEVERE, "Database query failed: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * Execute an async update
     */
    public CompletableFuture<Void> executeAsync(DatabaseRunnable runnable) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = getConnection()) {
                runnable.execute(conn);
            } catch (SQLException e) {
                plugin.log(Level.SEVERE, "Database update failed: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * Close the connection pool
     */
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.log(Level.INFO, "Database connection pool closed.");
        }
    }
    
    // Getters
    public DatabaseType getDatabaseType() {
        return databaseType;
    }
    
    public String getTablePrefix() {
        return tablePrefix;
    }
    
    public String getTable(String name) {
        return tablePrefix + name;
    }
    
    /**
     * Database types enum
     */
    public enum DatabaseType {
        MYSQL, MARIADB, POSTGRESQL, SQLITE, H2, MONGODB
    }
    
    /**
     * Callback interface for async queries
     */
    @FunctionalInterface
    public interface DatabaseCallback<T> {
        T execute(Connection connection) throws SQLException;
    }
    
    /**
     * Runnable interface for async updates
     */
    @FunctionalInterface
    public interface DatabaseRunnable {
        void execute(Connection connection) throws SQLException;
    }
}
