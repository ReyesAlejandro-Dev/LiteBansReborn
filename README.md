# 🛡️ LiteBansReborn v5.3.0

<p align="center">
  <img src="https://img.shields.io/badge/Version-5.3.0-red?style=for-the-badge" alt="Version">
  <img src="https://img.shields.io/badge/Minecraft-1.21+-green?style=for-the-badge" alt="Minecraft">
  <img src="https://img.shields.io/badge/Java-21+-orange?style=for-the-badge" alt="Java">
  <img src="https://img.shields.io/badge/License-MIT-blue?style=for-the-badge" alt="License">
  <img src="https://img.shields.io/badge/Spigot-Paper-yellow?style=for-the-badge" alt="Platform">
</p>

<p align="center">
  <b>🚀 The Most Advanced Free Punishment Management System for Minecraft</b>
</p>

<p align="center">
  <i>More features than LiteBans. More innovation than BanManager. Completely free.</i>
</p>

---

## 📋 Table of Contents

- [Features](#-features)
- [What's New in v5.2](#-whats-new-in-v52)
- [Installation](#-installation)
- [Commands](#-commands)
- [Permissions](#-permissions)
- [Configuration](#-configuration)
- [API](#-api)
- [Support](#-support)

---

## ✨ Features

### 🔨 Core Punishment System
| Feature | Description |
|---------|-------------|
| **Bans** | Permanent, temporary, IP bans with full history |
| **Mutes** | Permanent, temporary, IP mutes |
| **Kicks** | Kick players with logged reasons |
| **Warnings** | Warning system with escalation support |
| **Freeze** | Freeze players for investigation |

### 🎯 Exclusive Features (Not in LiteBans!)
| Feature | Description |
|---------|-------------|
| **🔐 HWID Banning** | Ban by hardware ID, not just account |
| **👻 Ghost Mute** | Shadow mute - player thinks they're chatting |
| **🎮 Redemption System** | Minigames to reduce punishment duration |
| **📸 Evidence System** | Attach screenshots, videos, replay links |
| **📊 Web Panel** | Full REST API with web dashboard |
| **💾 Chat Snapshots** | Auto-save chat context on punishment |
| **⏪ Auto-Rollback** | CoreProtect integration for griefing |
| **🗑️ Data Wipe** | Delete player data on permanent ban |
| **⏱️ Smart Rate Limiting** | Prevent staff abuse, 2FA for perma-bans |
| **🔍 Alt Detection** | Advanced fingerprinting and IP tracking |

### 🌐 Anti-VPN System
- **6 API Providers**: ProxyCheck, IP-API, VPNAPI, IPHub, IPHunter, IPQualityScore
- **Rotational System**: Auto-fallback when one API fails
- **Caching**: Reduce API calls
- **Real IP Detection**: Find actual IP behind VPN
- **Whitelist**: Country and IP whitelisting

### 💾 Database Support
- MySQL / MariaDB
- PostgreSQL
- SQLite
- H2 (Embedded)
- MongoDB

### 🔗 Integrations
- **PlaceholderAPI** - 50+ placeholders
- **Discord Webhooks** - Real-time notifications
- **Telegram Notifications** - Staff alerts
- **CoreProtect** - Auto-rollback integration
- **LuckPerms** - Permission integration
- **Geyser/Bedrock** - Full support

---

## 🆕 What's New in v5.2

### 🎉 Major Features

#### 💬 Fully Configurable Messages
Every single message in the plugin is now customizable!
- **Main Command**: `/lbr` help pages, info, and stats are now editable.
- **HWID Messages**: Customize ban/unban confirmations.
- **AI Feedback**: Rewrite the AI analysis reports to fit your server's tone.

#### 🔄 Auto-Updating Messages
Never worry about missing keys again!
- The plugin now **automatically detects** missing message keys in `messages.yml`.
- It injects the default values for any missing keys without overwriting your custom edits.
- Just run `/lbr reload` and it's done.

#### 🛠️ Critical Fixes & Stability
- **Database Safety**: Fixed a critical deadlock that could freeze player logins if the database was slow.
- **Chat Priority**: Fixed a race condition where chat filters would verify before mutes (now Mutes > Filters).
- **H2 Support**: Fixed an SQL syntax error that prevented the plugin from working with H2 (SQLite/MySQL/MariaDB/PostgreSQL fully supported).

#### 🔐 Enhanced HWID Management
- Added **/hwid unban <hwid>** command.
- Improved tab completion for all commands.

---

## 📦 Installation

1. **Download** the latest release
2. **Place** `LiteBansReborn-4.0.0.jar` in your `/plugins` folder
3. **Restart** your server
4. **Configure** `config.yml` and `messages.yml`
5. **Enjoy!** 🎉

### Requirements
- Spigot/Paper 1.21+
- Java 21+

### Optional Dependencies
- PlaceholderAPI
- LuckPerms
- CoreProtect

---

## 📜 Commands

### Punishment Commands
| Command | Description |
|---------|-------------|
| `/ban <player> [duration] [reason]` | Ban a player |
| `/tempban <player> <duration> [reason]` | Temporary ban |
| `/ipban <player\|ip> [duration] [reason]` | IP ban |
| `/unban <player>` | Unban a player |
| `/mute <player> [duration] [reason]` | Mute a player |
| `/tempmute <player> <duration> [reason]` | Temporary mute |
| `/ipmute <player\|ip> [duration] [reason]` | IP mute |
| `/unmute <player>` | Unmute a player |
| `/kick <player> [reason]` | Kick a player |
| `/warn <player> [reason]` | Warn a player |
| `/unwarn <player> [id]` | Remove warning |
| `/freeze <player>` | Freeze a player |
| `/unfreeze <player>` | Unfreeze a player |
| `/ghostmute <player>` | Shadow mute |

### Information Commands
| Command | Description |
|---------|-------------|
| `/history <player>` | View punishment history |
| `/checkban <player>` | Check ban status |
| `/checkmute <player>` | Check mute status |
| `/warnings <player>` | View warnings |
| `/banlist` | List all bans |
| `/mutelist` | List all mutes |
| `/staffhistory <staff>` | Staff's punishments |

### Utility Commands
| Command | Description |
|---------|-------------|
| `/clearchat` | Clear server chat |
| `/mutechat` | Toggle chat mute |
| `/staffchat <message>` | Staff-only chat |
| `/report <player> <reason>` | Report a player |
| `/punish <player>` | Open punishment GUI |
| `/geoip <player>` | Lookup player location |
| `/alts <player>` | Check for alt accounts |
| `/dupeip <player\|ip>` | Find duplicate IPs |

### V4.0 Commands
| Command | Description |
|---------|-------------|
| `/evidence <add\|view\|capture>` | Evidence system |
| `/redemption <start\|answer>` | Redemption minigames |
| `/hwid <check\|ban\|alts>` | Hardware ID management |
| `/vpncheck <player\|ip>` | VPN detection |
| `/lbr antivpn <on\|off\|status>` | Anti-VPN control |

### Admin Commands
| Command | Description |
|---------|-------------|
| `/lbr reload` | Reload configuration |
| `/lbr info` | Plugin information |
| `/lbr stats` | Punishment statistics |
| `/lbr antivpn` | Anti-VPN management |

---

## 🔑 Permissions

### Basic Permissions
| Permission | Description |
|------------|-------------|
| `litebansreborn.ban` | Ban players |
| `litebansreborn.mute` | Mute players |
| `litebansreborn.kick` | Kick players |
| `litebansreborn.warn` | Warn players |
| `litebansreborn.freeze` | Freeze players |
| `litebansreborn.history` | View history |
| `litebansreborn.notify` | Receive notifications |

### Admin Permissions
| Permission | Description |
|------------|-------------|
| `litebansreborn.admin` | Full admin access |
| `litebansreborn.silent` | Silent punishments |
| `litebansreborn.evidence` | Evidence system |
| `litebansreborn.hwid` | HWID management |
| `litebansreborn.vpncheck` | VPN checking |

### Wildcard
```
litebansreborn.staff - All staff commands
litebansreborn.* - All permissions
```

---

## ⚙️ Configuration

See the [Configuration Guide](docs/CONFIGURATION.md) for detailed documentation.

### Quick Config Example
```yaml
# Core Settings
general:
  server-name: "MyServer"
  debug: false

# Database
database:
  type: "MYSQL"
  host: "localhost"
  port: 3306
  database: "litebans"
  username: "root"
  password: "password"

# Anti-VPN
anti-vpn:
  enabled: true
  action: "KICK"
  providers:
    - proxycheck
    - ip-api
    - vpnapi

# Web Panel
web-panel:
  enabled: true
  port: 8080
```

---

## 🔌 API

LiteBansReborn provides a comprehensive API for developers.

### Maven
```xml
<dependency>
    <groupId>com.nuvik</groupId>
    <artifactId>LiteBansReborn</artifactId>
    <version>4.0.0</version>
    <scope>provided</scope>
</dependency>
```

### Example Usage
```java
LiteBansRebornAPI api = LiteBansReborn.getAPI();

// Ban a player
api.banPlayer(uuid, "Hacking", "7d", staffUUID);

// Check if banned
boolean banned = api.isBanned(uuid);

// Get punishment history
List<Punishment> history = api.getHistory(uuid);
```

See the [Developer API](docs/API.md) for complete documentation.

---

## 📊 Comparison

| Feature | LiteBansReborn | LiteBans ($10) | BanManager |
|---------|----------------|----------------|------------|
| Price | **FREE** | $10 | Free |
| HWID Banning | ✅ | ❌ | ❌ |
| Ghost Mute | ✅ | ❌ | ❌ |
| Redemption System | ✅ | ❌ | ❌ |
| Evidence System | ✅ | ❌ | ❌ |
| Web Panel | ✅ | Add-on | ❌ |
| Anti-VPN | ✅ (6 APIs) | Add-on | ❌ |
| Chat Snapshots | ✅ | ❌ | ❌ |
| Auto-Rollback | ✅ | ❌ | ❌ |
| Rate Limiting | ✅ | ❌ | ❌ |

---

## 📚 Documentation

Full documentation available in the `/docs` folder:

- [Installation Guide](docs/INSTALLATION.md)
- [Configuration Guide](docs/CONFIGURATION.md)
- [Commands Reference](docs/COMMANDS.md)
- [Permissions Reference](docs/PERMISSIONS.md)
- [Database Setup](docs/DATABASE.md)
- [Anti-VPN Setup](docs/ANTIVPN.md)
- [Web Panel Setup](docs/WEBPANEL.md)
- [Developer API](docs/API.md)
- [Placeholders](docs/PLACEHOLDERS.md)
- [Migration Guide](docs/MIGRATION.md)
- [FAQ](docs/FAQ.md)
- [Changelog](docs/CHANGELOG.md)

---

## 💬 Support

- **Discord**: [Join our Discord](https://discord.gg/nuvik)
- **Issues**: [GitHub Issues](https://github.com/nuvik/litebansreborn/issues)
- **Wiki**: [Documentation](docs/)

---

## 📜 License

LiteBansReborn is released under the MIT License. See [LICENSE](LICENSE) for details.

---

## 👨‍💻 Credits

**Developed by Nuvik**

Special thanks to all contributors and testers!

---

<p align="center">
  <b>⭐ If you like this plugin, give it a star! ⭐</b>
</p>

<p align="center">
  Made with ❤️ for the Minecraft community
</p>
