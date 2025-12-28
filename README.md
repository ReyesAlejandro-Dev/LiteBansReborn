<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.16%2B-brightgreen?style=for-the-badge&logo=minecraft" alt="Minecraft Version"/>
  <img src="https://img.shields.io/badge/License-GPL--3.0-blue?style=for-the-badge" alt="License"/>
  <img src="https://img.shields.io/badge/Status-Active-success?style=for-the-badge" alt="Status"/>
  <img src="https://img.shields.io/badge/Java-17+-orange?style=for-the-badge&logo=openjdk" alt="Java Version"/>
</p>

<h1 align="center">
  🔨 LiteBansReborn
</h1>

<p align="center">
  <strong>Advanced Punishment Management System for Minecraft Servers</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/✓_Free_&_Open_Source-22c55e?style=flat-square" alt="Free"/>
  <img src="https://img.shields.io/badge/✓_Actively_Maintained-22c55e?style=flat-square" alt="Maintained"/>
  <img src="https://img.shields.io/badge/✓_Premium_Features-22c55e?style=flat-square" alt="Premium"/>
</p>

---

## ⭐ Overview

**LiteBansReborn** is a powerful and feature-rich punishment management plugin designed to be a superior alternative to traditional ban plugins. It provides everything you need to moderate your server effectively, from simple bans to advanced alt detection and punishment templates.

---

## 🎯 Key Features

### 🛡️ Complete Punishment System
| Feature | Description |
|---------|-------------|
| **Bans** | Permanent & Temporary Bans |
| **IP Bans** | With bypass support |
| **Mutes** | Permanent & Temporary Mutes |
| **IP Mutes** | Block communication by IP |
| **Warnings** | With auto-action triggers |
| **Kicks** | Single & Mass kicks |
| **Freeze** | Immobilize players for inspection |

### 🔍 Advanced Alt Detection
- 🌐 Automatic IP tracking
- 🚨 Detect alt accounts on join
- 📢 Staff notifications
- ⚡ Auto-ban options for alts

### 📋 Punishment Templates
- 📈 Create punishment ladders
- ⬆️ Escalating punishments
- 🔄 Perfect for repeat offenders

### 💾 Multi-Database Support
| Database | Status |
|----------|--------|
| MySQL / MariaDB | ✅ Supported |
| PostgreSQL | ✅ Supported |
| SQLite | ✅ Supported |
| H2 (default) | ✅ Supported |

### 🔗 Integrations
- 💬 **Discord Webhooks** - Get notifications in your Discord
- 📱 **Telegram Notifications** - Stay updated on the go
- 🏷️ **PlaceholderAPI Support** - Use placeholders anywhere
- 🌍 **GeoIP Location Lookup** - See where players connect from

### ✨ Additional Features
- 📝 Player Reports System
- 📨 Appeal System
- 📌 Staff Notes
- 🎯 Punishment Points
- 💬 Staff Chat
- 📊 Full History Tracking
- 🔇 Silent Punishments
- 🎨 Fully Customizable Messages

---

## 📋 Commands

<details>
<summary><b>🔨 Ban Commands</b></summary>

```
/ban <player> [duration] [reason]    - Ban a player
/tempban <player> <duration> [reason] - Temporarily ban
/ipban <player|ip> [duration] [reason] - IP ban
/unban <player>                       - Unban a player
/unbanip <ip>                         - Unban an IP
/checkban <player>                    - Check ban status
/banlist                              - View active bans
```

</details>

<details>
<summary><b>🔇 Mute Commands</b></summary>

```
/mute <player> [duration] [reason]    - Mute a player
/tempmute <player> <duration> [reason] - Temporarily mute
/ipmute <player|ip> [duration] [reason] - IP mute
/unmute <player>                       - Unmute a player
/unmuteip <ip>                         - Unmute an IP
/checkmute <player>                    - Check mute status
/mutelist                              - View active mutes
```

</details>

<details>
<summary><b>⚠️ Other Punishment Commands</b></summary>

```
/kick <player> [reason]     - Kick a player
/kickall [reason]           - Kick all players
/warn <player> [reason]     - Warn a player
/unwarn <player> <id>       - Remove a warning
/warnings <player>          - View warnings
/freeze <player> [reason]   - Freeze a player
/unfreeze <player>          - Unfreeze a player
```

</details>

<details>
<summary><b>🔧 Utility Commands</b></summary>

```
/history <player>           - View punishment history
/staffhistory <staff>       - View staff actions
/alts <player>              - Check alt accounts
/dupeip <player>            - Find players with same IP
/note <player> <message>    - Add staff note
/notes <player>             - View notes
/delnote <id>               - Delete a note
/clearchat                  - Clear server chat
/mutechat                   - Toggle chat mute
/staffchat                  - Toggle staff chat
/punish <player>            - Open punishment GUI
/geoip <player>             - View player location
/litebansreborn reload      - Reload config
```

</details>

<details>
<summary><b>📩 Reports & Appeals</b></summary>

```
/report <player> <reason>              - Report a player
/reports                               - View pending reports
/handlereport <id> <action>            - Handle a report
/appeal <message>                      - Appeal your punishment
/appeals                               - View pending appeals
/handleappeal <id> <accept|deny> [response] - Handle appeal
```

</details>

---

## 🔐 Permissions

<details>
<summary><b>View All Permissions</b></summary>

| Permission | Description |
|------------|-------------|
| `litebansreborn.ban` | Use /ban |
| `litebansreborn.ban.permanent` | Use permanent bans |
| `litebansreborn.tempban` | Use /tempban |
| `litebansreborn.ipban` | Use /ipban |
| `litebansreborn.unban` | Use /unban |
| `litebansreborn.mute` | Use /mute |
| `litebansreborn.tempmute` | Use /tempmute |
| `litebansreborn.ipmute` | Use /ipmute |
| `litebansreborn.unmute` | Use /unmute |
| `litebansreborn.kick` | Use /kick |
| `litebansreborn.kickall` | Use /kickall |
| `litebansreborn.warn` | Use /warn |
| `litebansreborn.unwarn` | Use /unwarn |
| `litebansreborn.freeze` | Use /freeze |
| `litebansreborn.unfreeze` | Use /unfreeze |
| `litebansreborn.history` | Use /history |
| `litebansreborn.staffhistory` | Use /staffhistory |
| `litebansreborn.alts` | Use /alts |
| `litebansreborn.notes` | Use notes commands |
| `litebansreborn.clearchat` | Use /clearchat |
| `litebansreborn.mutechat` | Use /mutechat |
| `litebansreborn.staffchat` | Use staff chat |
| `litebansreborn.report` | Use /report |
| `litebansreborn.reports` | View reports |
| `litebansreborn.appeal` | Use /appeal |
| `litebansreborn.appeals` | Manage appeals |
| `litebansreborn.notify` | Receive staff notifications |
| `litebansreborn.silent` | Use silent punishments (-s) |
| `litebansreborn.admin` | Full admin access |
| `litebansreborn.bypass.ban` | Cannot be banned |
| `litebansreborn.bypass.mute` | Cannot be muted |
| `litebansreborn.bypass.kick` | Cannot be kicked |
| `litebansreborn.bypass.freeze` | Cannot be frozen |
| `litebansreborn.bypass.clearchat` | See cleared chat |
| `litebansreborn.bypass.mutechat` | Chat when muted |

</details>

---

## ⏱️ Duration Format

| Symbol | Duration |
|--------|----------|
| `s` | Seconds |
| `m` | Minutes |
| `h` | Hours |
| `d` | Days |
| `w` | Weeks |
| `mo` | Months |
| `y` | Years |

**Examples:**
```
30m        = 30 minutes
7d         = 7 days
1mo        = 1 month
permanent  = forever
perm       = forever
```

---

## 💾 Installation

1. 📥 Download the plugin
2. 📁 Place in your `plugins` folder
3. 🔄 Restart your server
4. ⚙️ Edit `config.yml` and `messages.yml`
5. 🔃 Use `/litebansreborn reload` to apply changes

---

## ⚙️ Configuration

The plugin generates two main configuration files:

| File | Purpose |
|------|---------|
| `config.yml` | Main settings (database, punishments, integrations) |
| `messages.yml` | All customizable messages |

<details>
<summary><b>📦 Example Database Config</b></summary>

```yaml
database:
  type: H2  # Options: MYSQL, MARIADB, POSTGRESQL, SQLITE, H2
  host: localhost
  port: 3306
  database: litebansreborn
  username: root
  password: ""
  table-prefix: "lbr_"
```

</details>

<details>
<summary><b>💬 Discord Integration</b></summary>

```yaml
discord:
  enabled: true
  webhooks:
    bans: "https://discord.com/api/webhooks/..."
    mutes: "https://discord.com/api/webhooks/..."
    kicks: "https://discord.com/api/webhooks/..."
    warns: "https://discord.com/api/webhooks/..."
```

</details>

---

## 📊 PlaceholderAPI

| Placeholder | Description |
|-------------|-------------|
| `%litebansreborn_is_banned%` | Check if player is banned |
| `%litebansreborn_ban_reason%` | Get ban reason |
| `%litebansreborn_ban_remaining%` | Time remaining on ban |
| `%litebansreborn_is_muted%` | Check if player is muted |
| `%litebansreborn_mute_reason%` | Get mute reason |
| `%litebansreborn_mute_remaining%` | Time remaining on mute |
| `%litebansreborn_warning_count%` | Number of warnings |
| `%litebansreborn_is_frozen%` | Check if player is frozen |
| `%litebansreborn_points%` | Punishment points |
| `%litebansreborn_history_count%` | Total punishment count |

---

## 📝 API for Developers

```java
// Get API instance
LiteBansRebornAPI api = LiteBansReborn.getAPI();

// Check if player is banned
api.isBanned(uuid).thenAccept(banned -> {
    if (banned) {
        // Player is banned
    }
});

// Ban a player
api.ban(targetUUID, targetName, targetIP, 
        executorUUID, executorName, 
        "Hacking", 86400000L, false);

// Check mute status
api.isMuted(uuid).thenAccept(muted -> { ... });

// Get warning count
api.getWarningCount(uuid).thenAccept(count -> { ... });
```

---

## ❓ FAQ

**Q: What Minecraft versions are supported?**  
A: 1.16+ (tested on 1.20+)

**Q: Is BungeeCord/Velocity supported?**  
A: Sync support coming in future updates!

**Q: Can I migrate from LiteBans?**  
A: Migration tool coming soon!

**Q: How do I change the language?**  
A: Edit `messages.yml` - all text is fully customizable.

---

## 📞 Support

- 💬 [Discord Server](https://discord.gg/your-discord)
- 🐛 [GitHub Issues](https://github.com/nuvik/litebansreborn/issues)
- 📧 PM on SpigotMC

---

<p align="center">
  <b>⭐ If you enjoy this plugin, please leave a star! ⭐</b>
</p>

<p align="center">
  Made with ❤️ by <b>Nuvik</b>
</p>

---

<p align="center">
  <img src="https://img.shields.io/github/stars/nuvik/litebansreborn?style=social" alt="Stars"/>
  <img src="https://img.shields.io/github/forks/nuvik/litebansreborn?style=social" alt="Forks"/>
  <img src="https://img.shields.io/github/watchers/nuvik/litebansreborn?style=social" alt="Watchers"/>
</p>
