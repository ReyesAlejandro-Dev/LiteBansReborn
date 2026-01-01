# 💾 Database Setup Guide

Complete guide to database configuration for LiteBansReborn v6.0.0+.

## Table of Contents
- [Supported Databases](#supported-databases)
- [Configuration](#configuration)
- [Connection Pool](#connection-pool)
- [Troubleshooting](#troubleshooting)

---

## Supported Databases

LiteBansReborn v6.0+ features a **Universal Database Driver** layer that automatically adjusts SQL queries (UPSERTs, pagination, etc.) to match your specific database dialect.

| Database | Supported? | Notes |
|----------|------------|-------|
| **MySQL / MariaDB** | ✅ **Recommended** | Best for networks. Uses `ON DUPLICATE KEY UPDATE`. |
| **PostgreSQL** | ✅ **Supported** | High performance. Uses `ON CONFLICT DO UPDATE`. |
| **SQLite** | ✅ **Supported** | Best for single servers. Uses `INSERT OR REPLACE` with thread locking. |
| **H2** | ✅ **Supported** | Zero-conf embedded DB. Uses `MERGE INTO` or `REPLACE`. |
| **MongoDB** | ⚠️ Experimental | Not fully feature-complete yet. |

---

## Configuration

### 1. MySQL / MariaDB (Recommended)
Add this to your `config.yml`:

```yaml
database:
  type: "MYSQL"  # or "MARIADB"
  host: "localhost"
  port: 3306
  database: "litebans"
  username: "root"
  password: "password"
  table-prefix: "lbr_"
  ssl: false
```

### 2. PostgreSQL
```yaml
database:
  type: "POSTGRESQL"
  host: "localhost"
  port: 5432
  database: "litebans"
  username: "postgres"
  password: "password"
  schema: "public"
```

### 3. SQLite / H2 (No Setup)
Simply set the type. The plugin will create the file in `/plugins/LiteBansReborn/database/`.

```yaml
database:
  type: "SQLITE" # or "H2"
```

**Note on SQLite:**
> In v6.0.0+, SQLite access is synchronized to prevent "Database Locked" errors, making it much more stable for medium-sized servers than before.

---

## Connection Pool (HikariCP)

We use HikariCP for high-performance connection pooling.
**Important:** v6.0.0 fixes connection leaks throughout the plugin. You should rarely see "Connection is not available" errors unless your database is genuinely down.

Recommended settings in `config.yml`:

```yaml
database:
  pool:
    maximum-pool-size: 10   # 10 is enough for most servers (< 500 players)
    minimum-idle: 2
    connection-timeout: 30000
    idle-timeout: 600000
    max-lifetime: 1800000
```

---

## Troubleshooting

### "Database is locked" (SQLite)
This should be fixed in v6.0.0. If you still see this, ensure no other external program is opening the `.db` file while the server is running.

### "Connection refused"
1. Check if your database server is running.
2. Verify IP/Port and Firewall rules.
3. Ensure the user has permissions: `GRANT ALL ON litebans.* TO 'user'@'%'`.

### "Packet for query is too large"
Increase `max_allowed_packet` in your MySQL/MariaDB configuration (`my.cnf`) to at least `16M`.
