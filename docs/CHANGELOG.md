# 📋 Changelog

All notable changes to LiteBansReborn.

## [4.0.0] - 2024-12-28

### 🎉 Major Release - "Punishment Intelligence"

This release introduces groundbreaking features not found in any other punishment plugin.

### Added

#### 🔐 Hardware ID Banning
- Ban players by their computer's hardware ID
- Prevents ban evasion with alt accounts
- Player fingerprinting system
- Alt detection with similarity scoring
- Commands: `/hwid check`, `/hwid ban`, `/hwid alts`

#### 📸 Evidence System
- Attach proof to any punishment
- Support for screenshots, YouTube, replay links
- Auto-capture player inventory on ban
- Evidence viewer GUI
- Commands: `/evidence add`, `/evidence view`, `/evidence capture`

#### 🎮 Redemption System
- Banned players can reduce punishment through minigames
- Math captcha challenges (10% reduction)
- Typing test challenges (15% reduction)
- Quiz challenges (20% reduction)
- Configurable reduction percentages
- Commands: `/redemption start`, `/redemption answer`

#### 📊 Web Panel
- Full REST API for external management
- Beautiful dark-theme web dashboard
- Real-time server statistics
- Player search
- Staff statistics
- API key authentication
- SSL support

#### ⏱️ Smart Rate Limiting
- Prevent staff abuse with limits
- Max bans per hour
- Max permanent bans per day
- 2FA confirmation for permanent bans
- Report cooldowns for players
- `/lbr confirm` command

#### 👻 Ghost Mute (Shadow Mute)
- Muted player thinks they're chatting normally
- Only staff can see their messages
- Perfect for catching rule-breakers
- Command: `/ghostmute`

#### 💾 Chat Snapshots
- Automatically save chat context when punishing
- Configurable message count
- Saved as text files for review

#### ⏪ Auto-Rollback
- CoreProtect integration
- Automatically rollback griefer's actions when banned
- Configurable time period
- GUI button option

#### 🗑️ Data Wipe
- Delete player data on permanent ban
- Configurable commands to run
- Delete world data (playerdata, stats, advancements)
- Optional: only on permanent bans

#### 🌐 Anti-VPN Runtime Controls
- Enable/disable Anti-VPN without restart
- Toggle alerts
- Change action (KICK/WARN/ALLOW)
- Manage whitelist
- Clear cache
- View providers
- Commands: `/lbr antivpn on/off/status/alerts`

### Improved
- UUID support for offline players (pre-ban)
- Ban players who have never joined
- GUI system enhancements
- Alt detection algorithm
- Database performance
- Error handling

### Fixed
- Various bug fixes from v3.0.0
- Memory leak in cache system
- Race condition in freeze
- Tab completion issues

---

## [3.0.0] - 2024-12-15

### Added
- Initial public release
- Complete punishment system (ban, mute, kick, warn, freeze)
- Anti-VPN system with 6 providers
- Discord webhook notifications
- Telegram notifications
- PlaceholderAPI support
- GeoIP lookup
- Alt detection
- Report system
- Appeal system
- Notes system
- Punishment templates
- GUI system
- Multi-database support

---

## [2.0.0] - 2024-11-01

### Added
- Beta testing release
- Core punishment features
- Basic database support

---

## [1.0.0] - 2024-10-01

### Added
- Initial development version
- Proof of concept

---

## Future Plans

### v4.5.0 (Planned)
- [ ] Discord Bot with JDA (slash commands)
- [ ] Ticket system
- [ ] Discord-Minecraft verification
- [ ] Chat filter system
- [ ] Maintenance mode

### v5.0.0 (Planned)
- [ ] Role synchronization (Discord ↔ LuckPerms)
- [ ] AI behavior analysis
- [ ] Full React web panel
- [ ] Mobile app
- [ ] Advanced analytics

---

## Versioning

LiteBansReborn uses [Semantic Versioning](https://semver.org/):

- **MAJOR** (X.0.0): Breaking changes
- **MINOR** (0.X.0): New features, backward compatible
- **PATCH** (0.0.X): Bug fixes

---

## Reporting Issues

Found a bug? Please report it:

1. Check [existing issues](https://github.com/nuvik/litebansreborn/issues)
2. Include:
   - Server version
   - Plugin version
   - Error log
   - Steps to reproduce
3. Submit issue

---

## Contributors

Thank you to all contributors!

- **Nuvik** - Lead Developer
- Community testers and reporters

Want to contribute? See our [contributing guide](CONTRIBUTING.md).
