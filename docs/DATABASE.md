# 💾 Database Setup Guide

Complete guide to database configuration for LiteBansReborn.

## Table of Contents
- [Supported Databases](#supported-databases)
- [H2 (Default)](#h2-default)
- [MySQL / MariaDB](#mysql--mariadb)
- [PostgreSQL](#postgresql)
- [SQLite](#sqlite)
- [MongoDB](#mongodb)
- [Connection Pool](#connection-pool)
- [Migration](#migration)
- [Backup](#backup)

---

## Supported Databases

| Database | Recommended For | Notes |
|----------|-----------------|-------|
| **H2** | Single server, testing | No setup required |
| **MySQL** | Networks, production | Most common choice |
| **MariaDB** | Networks, production | MySQL-compatible |
| **PostgreSQL** | Advanced setups | Best performance |
| **SQLite** | Small servers | Single file |
| **MongoDB** | NoSQL preference | Experimental |

---

## H2 (Default)

H2 is an embedded database that requires no setup. Perfect for:
- Single servers
- Testing
- Quick setup

### Configuration
```yaml
database:
  type: "H2"
```

### File Location
```
/plugins/LiteBansReborn/database/litebans.mv.db
```

### Pros & Cons
✅ No setup required
✅ Fast for small data
❌ Not for multi-server
❌ Not recommended for large scale

---

## MySQL / MariaDB

Recommended for production servers and BungeeCord networks.

### Prerequisites
1. MySQL/MariaDB server running
2. Database created
3. User with permissions

### Create Database
```sql
CREATE DATABASE litebans CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'litebans'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON litebans.* TO 'litebans'@'localhost';
FLUSH PRIVILEGES;
```

### Configuration
```yaml
database:
  type: "MYSQL"  # or "MARIADB"
  host: "localhost"
  port: 3306
  database: "litebans"
  username: "litebans"
  password: "your_password"
  
  # SSL (optional)
  ssl: false
  
  # Table prefix
  table-prefix: "lbr_"
  
  # Connection pool
  pool:
    maximum-pool-size: 10
    minimum-idle: 2
    connection-timeout: 30000
    idle-timeout: 600000
    max-lifetime: 1800000
```

### Troubleshooting

**Connection refused:**
```yaml
# Check if MySQL is running
# Check firewall settings
# Verify host/port
```

**Access denied:**
```sql
-- Check user permissions
SHOW GRANTS FOR 'litebans'@'localhost';

-- Reset password
ALTER USER 'litebans'@'localhost' IDENTIFIED BY 'new_password';
```

**SSL issues:**
```yaml
database:
  ssl: true
  ssl-mode: "REQUIRED"  # or "VERIFY_CA"
```

---

## PostgreSQL

Best performance for large-scale deployments.

### Create Database
```sql
CREATE DATABASE litebans;
CREATE USER litebans WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE litebans TO litebans;
\c litebans
GRANT ALL ON SCHEMA public TO litebans;
```

### Configuration
```yaml
database:
  type: "POSTGRESQL"
  host: "localhost"
  port: 5432
  database: "litebans"
  username: "litebans"
  password: "your_password"
  
  # Schema (optional)
  schema: "public"
  
  # SSL
  ssl: false
```

### Performance Tips
```sql
-- Enable connection pooling
-- Increase shared_buffers
-- Tune work_mem
```

---

## SQLite

Single-file database, no server required.

### Configuration
```yaml
database:
  type: "SQLITE"
  file: "database.db"  # Relative to plugin folder
```

### File Location
```
/plugins/LiteBansReborn/database.db
```

### Pros & Cons
✅ No setup required
✅ Simple backup (copy file)
❌ Not for multi-server
❌ Can lock under heavy load

---

## MongoDB

NoSQL option for advanced users.

### Configuration
```yaml
database:
  type: "MONGODB"
  connection-string: "mongodb://localhost:27017"
  database: "litebans"
  
  # Authentication (optional)
  auth-database: "admin"
  username: "litebans"
  password: "your_password"
```

### Collections
LiteBansReborn creates these collections:
- `bans`
- `mutes`
- `kicks`
- `warnings`
- `history`
- `players`

---

## Connection Pool

HikariCP is used for connection pooling.

### Configuration
```yaml
database:
  pool:
    # Maximum connections in pool
    maximum-pool-size: 10
    
    # Minimum idle connections
    minimum-idle: 2
    
    # Connection timeout (ms)
    connection-timeout: 30000
    
    # Idle timeout (ms)
    idle-timeout: 600000
    
    # Max connection lifetime (ms)
    max-lifetime: 1800000
    
    # Test query
    connection-test-query: "SELECT 1"
```

### Recommended Settings

**Small server (< 50 players):**
```yaml
pool:
  maximum-pool-size: 5
  minimum-idle: 1
```

**Medium server (50-200 players):**
```yaml
pool:
  maximum-pool-size: 10
  minimum-idle: 2
```

**Large network (200+ players):**
```yaml
pool:
  maximum-pool-size: 20
  minimum-idle: 5
```

---

## Migration

### From LiteBans
```bash
/lbr import litebans
```

### From BanManager
```bash
/lbr import banmanager
```

### From AdvancedBan
```bash
/lbr import advancedban
```

### Database to Database
```bash
# Export from current
/lbr export sql

# Change config to new database
# Restart server

# Import
/lbr import file exported_data.sql
```

---

## Backup

### MySQL
```bash
# Backup
mysqldump -u litebans -p litebans > backup.sql

# Restore
mysql -u litebans -p litebans < backup.sql
```

### PostgreSQL
```bash
# Backup
pg_dump -U litebans litebans > backup.sql

# Restore
psql -U litebans litebans < backup.sql
```

### SQLite / H2
```bash
# Just copy the file
cp plugins/LiteBansReborn/database.db backup/
```

### Automated Backups
```bash
#!/bin/bash
# backup.sh
DATE=$(date +%Y%m%d_%H%M%S)
mysqldump -u litebans -p'password' litebans > /backups/litebans_$DATE.sql

# Keep only last 7 days
find /backups -name "litebans_*.sql" -mtime +7 -delete
```

Add to crontab:
```
0 4 * * * /path/to/backup.sh
```

---

## Tables

LiteBansReborn creates these tables:

| Table | Description |
|-------|-------------|
| `lbr_bans` | Ban records |
| `lbr_mutes` | Mute records |
| `lbr_kicks` | Kick records |
| `lbr_warnings` | Warning records |
| `lbr_history` | Full punishment history |
| `lbr_players` | Player cache |
| `lbr_notes` | Staff notes |
| `lbr_reports` | Player reports |
| `lbr_appeals` | Ban appeals |
| `lbr_vpn_detections` | VPN detection log |
| `lbr_vpn_ips` | IP tracking |
| `lbr_hwid_data` | Hardware IDs |
| `lbr_hwid_bans` | HWID bans |
