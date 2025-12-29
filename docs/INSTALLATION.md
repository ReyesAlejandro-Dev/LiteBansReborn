# 📦 Installation Guide

Welcome to LiteBansReborn! This guide will help you get started.

## Table of Contents
- [Requirements](#requirements)
- [Download](#download)
- [Installation Steps](#installation-steps)
- [First Run](#first-run)
- [Upgrading](#upgrading)
- [Troubleshooting](#troubleshooting)

---

## Requirements

### Minimum Requirements
| Requirement | Version |
|-------------|---------|
| **Minecraft Server** | 1.21+ |
| **Java** | 21+ |
| **RAM** | 512MB+ |

### Supported Platforms
- ✅ Spigot
- ✅ Paper (Recommended)
- ✅ Purpur
- ✅ Pufferfish
- ✅ Folia (Experimental)

### Optional Dependencies
| Plugin | Purpose |
|--------|---------|
| PlaceholderAPI | Placeholders in chat/scoreboard |
| LuckPerms | Permission integration |
| CoreProtect | Auto-rollback on ban |
| DiscordSRV | Enhanced Discord integration |

---

## Download

### From SpigotMC
1. Go to the [SpigotMC Resource Page](https://spigotmc.org/resources/litebansreborn)
2. Click "Download Now"
3. Save `LiteBansReborn-4.0.0.jar`

### From GitHub
```bash
git clone https://github.com/nuvik/LiteBansReborn.git
cd LiteBansReborn
mvn clean package
```

---

## Installation Steps

### Step 1: Stop Your Server
Always stop your server before installing new plugins.

```bash
stop
```

### Step 2: Upload the Plugin
Upload `LiteBansReborn-4.0.0.jar` to your `/plugins` folder.

```
/plugins/
├── LiteBansReborn-4.0.0.jar
└── ...
```

### Step 3: Start Your Server
Start your server to generate configuration files.

```bash
java -jar server.jar
```

### Step 4: Configure the Plugin
Edit the generated configuration files:

```
/plugins/LiteBansReborn/
├── config.yml          # Main configuration
├── messages.yml        # All messages
├── durations.yml       # Duration presets
├── templates.yml       # Punishment templates
└── database/           # Database files (if using H2/SQLite)
```

### Step 5: Reload (Optional)
Apply changes without restarting:

```
/lbr reload
```

---

## First Run

When you first run LiteBansReborn, it will:

1. **Create configuration files** in `/plugins/LiteBansReborn/`
2. **Initialize the database** (H2 by default)
3. **Load default templates** for punishments
4. **Register commands** and listeners

### Check Installation
Run these commands to verify:

```
/lbr info     # Shows plugin information
/lbr stats    # Shows database statistics
```

### Test Punishment
Test with a temporary ban:

```
/ban TestPlayer 1m Testing LiteBansReborn
/unban TestPlayer
```

---

## Upgrading

### From Previous Versions

1. **Backup your data**
   ```bash
   cp -r plugins/LiteBansReborn plugins/LiteBansReborn.backup
   ```

2. **Stop the server**

3. **Replace the JAR file**

4. **Start the server**

5. **Check for migration prompts**

### Migrating from LiteBans

LiteBansReborn can import data from LiteBans:

```
/lbr import litebans
```

See [Migration Guide](MIGRATION.md) for details.

---

## Troubleshooting

### Plugin Not Loading

**Check Java version:**
```bash
java -version
# Should show: openjdk version "21.x.x"
```

**Check server version:**
```
/version
# Should show: 1.21+
```

**Check console for errors:**
Look for red error messages in the console.

### Database Connection Failed

**MySQL/MariaDB:**
- Verify host, port, username, password
- Ensure the database exists
- Check user permissions

```yaml
database:
  type: "MYSQL"
  host: "localhost"
  port: 3306
  database: "litebans"
  username: "root"
  password: "your_password"
```

### Commands Not Working

**Check permissions:**
```
/lp user <player> permission check litebansreborn.ban
```

**Reload the plugin:**
```
/lbr reload
```

### Need Help?

- **Discord**: [Join our Discord](https://discord.gg/nuvik)
- **GitHub Issues**: [Report a bug](https://github.com/nuvik/litebansreborn/issues)
- **SpigotMC**: [Discussion forum](https://spigotmc.org/resources/litebansreborn)

---

## Next Steps

- [Configuration Guide](CONFIGURATION.md) - Customize the plugin
- [Commands Reference](COMMANDS.md) - All available commands
- [Database Setup](DATABASE.md) - Configure your database
