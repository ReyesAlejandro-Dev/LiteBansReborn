# 🔄 Migration Guide

Migrate from other punishment plugins to LiteBansReborn.

## Table of Contents
- [Supported Plugins](#supported-plugins)
- [Before You Start](#before-you-start)
- [From LiteBans](#from-litebans)
- [From BanManager](#from-banmanager)
- [From AdvancedBan](#from-advancedban)
- [From MaxBans](#from-maxbans)
- [Verify Migration](#verify-migration)
- [Rollback](#rollback)

---

## Supported Plugins

| Plugin | Support Level |
|--------|---------------|
| LiteBans | ✅ Full |
| BanManager | ✅ Full |
| AdvancedBan | ✅ Full |
| MaxBans | ⚠️ Partial |
| BanHammer | ⚠️ Partial |
| Essentials | ⚠️ Basic |

---

## Before You Start

### 1. Backup Everything!
```bash
# Backup database
mysqldump -u user -p database > backup.sql

# Backup plugin folder
cp -r plugins/LiteBans plugins/LiteBans.backup
```

### 2. Install Both Plugins
Keep the old plugin while migrating.

### 3. Check Database Access
LiteBansReborn needs read access to the old plugin's database.

### 4. Inform Staff
```
/broadcast Migration in progress - punishments may be delayed
```

---

## From LiteBans

LiteBans uses a similar database structure, making migration straightforward.

### Step 1: Configure Database Access

In LiteBansReborn's `config.yml`, point to LiteBans' database:
```yaml
import:
  source: "litebans"
  database:
    host: "localhost"
    port: 3306
    database: "litebans"
    username: "root"
    password: "password"
    table-prefix: "litebans_"
```

### Step 2: Run Import
```
/lbr import litebans
```

### Step 3: Verify
```
/lbr stats
```

### What Gets Imported

| Data | Imported |
|------|----------|
| Bans | ✅ |
| Mutes | ✅ |
| Kicks | ✅ |
| Warnings | ✅ |
| History | ✅ |
| IP Bans | ✅ |
| Player Cache | ✅ |

### Notes
- UUIDs are preserved
- Timestamps are preserved
- Operator info is preserved
- Silent flags are preserved

---

## From BanManager

### Step 1: Configure Import

```yaml
import:
  source: "banmanager"
  database:
    host: "localhost"
    port: 3306
    database: "banmanager"
    username: "root"
    password: "password"
```

### Step 2: Run Import
```
/lbr import banmanager
```

### Mapping

| BanManager | LiteBansReborn |
|------------|----------------|
| `bm_player_bans` | Bans |
| `bm_player_mutes` | Mutes |
| `bm_player_kicks` | Kicks |
| `bm_player_warnings` | Warnings |
| `bm_player_notes` | Notes |

### Notes
- BanManager uses different UUID format
- Some fields may not have direct equivalents
- Notes are imported separately

---

## From AdvancedBan

### Step 1: Configure Import

**If using MySQL:**
```yaml
import:
  source: "advancedban"
  database:
    host: "localhost"
    port: 3306
    database: "advancedban"
    username: "root"
    password: "password"
```

**If using YAML files:**
```yaml
import:
  source: "advancedban"
  type: "file"
  path: "plugins/AdvancedBan/data"
```

### Step 2: Run Import
```
/lbr import advancedban
```

### Mapping

| AdvancedBan | LiteBansReborn | Notes |
|-------------|----------------|-------|
| Punishment Type 1 | Ban | |
| Punishment Type 2 | Mute | |
| Punishment Type 3 | Kick | |
| Punishment Type 4 | Warning | |

### Notes
- AdvancedBan uses different duration format
- Some punishment types may differ
- Layout IDs are ignored

---

## From MaxBans

### Step 1: Configure Import

```yaml
import:
  source: "maxbans"
  database:
    type: "YAML"  # or "MYSQL"
    path: "plugins/MaxBans/bans.yml"
```

### Step 2: Run Import
```
/lbr import maxbans
```

### Limitations
- MaxBans uses older UUID system
- Some data may not transfer
- Temporary bans may lose precision

---

## Verify Migration

### Check Statistics
```
/lbr stats
```

Compare with old plugin's stats.

### Check Specific Players
```
/history <player>
```

Verify ban history matches.

### Check Active Punishments
```
/banlist
/mutelist
```

Ensure all active punishments transferred.

### Test Commands
```
/checkban <known-banned-player>
```

Verify bans work correctly.

---

## Rollback

If migration fails, you can rollback:

### Step 1: Stop Server

### Step 2: Clear LiteBansReborn Data
```bash
# Delete database
mysql -u root -p -e "DROP DATABASE litebansreborn; CREATE DATABASE litebansreborn;"

# Or delete H2 file
rm plugins/LiteBansReborn/database/*.db
```

### Step 3: Restore Old Plugin
```bash
# Remove LiteBansReborn
rm plugins/LiteBansReborn-4.0.0.jar

# Restore old plugin if removed
cp backup/LiteBans.jar plugins/
```

### Step 4: Start Server

---

## Post-Migration

### 1. Remove Old Plugin
Once verified, remove the old plugin:
```bash
rm plugins/LiteBans-*.jar
```

### 2. Update Permissions
If permission nodes changed:
```
# LiteBans: litebans.ban
# LiteBansReborn: litebansreborn.ban
```

### 3. Update Scripts
Update any scripts that reference old plugin commands.

### 4. Inform Staff
```
/broadcast Migration complete! All punishments have been transferred.
```

---

## Import Command Reference

```bash
# Import from specific plugin
/lbr import <plugin>

# Import with options
/lbr import <plugin> --dry-run    # Preview without importing
/lbr import <plugin> --force      # Override existing data
/lbr import <plugin> --bans-only  # Only import bans

# Export data
/lbr export sql                   # Export to SQL file
/lbr export json                  # Export to JSON
```

---

## Troubleshooting

### "Database connection failed"
- Verify credentials in config
- Check database server is running
- Verify network access

### "No data found"
- Check table prefix matches
- Verify database name is correct
- Check if tables exist

### "UUID mismatch"
- Some plugins use offline UUIDs
- Run UUID converter if needed

### Duplicates
- Clear LiteBansReborn data first
- Use `--force` flag

### Performance
- Large databases take time
- Consider off-peak migration
- Increase memory if needed
