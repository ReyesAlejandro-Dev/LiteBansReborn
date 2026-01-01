# ❓ Frequently Asked Questions

Common questions and answers about LiteBansReborn.

## Table of Contents
- [General](#general)
- [Installation](#installation)
- [Commands](#commands)
- [Database](#database)
- [Anti-VPN](#anti-vpn)
- [Permissions](#permissions)
- [Troubleshooting](#troubleshooting)
- [Features](#features)

---

## General

### What is LiteBansReborn?
LiteBansReborn is a free, advanced punishment management plugin for Minecraft servers. It provides ban, mute, kick, warn, and freeze functionality with extensive features like HWID banning, evidence system, and web panel.

### Is it really free?
Yes! LiteBansReborn is 100% free and open source. No hidden costs, no premium versions.

### How does it compare to LiteBans?
LiteBansReborn has more features than LiteBans (which costs $10), including:
- HWID banning
- Evidence system
- Redemption minigames
- Web panel
- Ghost mute
- And more!

See the [comparison table](../README.md#comparison) for details.

### What Minecraft versions are supported?
LiteBansReborn supports Minecraft 1.21+. We focus on the latest versions for best performance and features.

### Does it work with BungeeCord/Velocity?
Yes! Configure MySQL database and all servers will share punishments.

### Does it support Bedrock players?
Yes! Full support for Geyser/Floodgate players.

---

## Installation

### How do I install?
1. Download the JAR file
2. Put in `/plugins` folder
3. Restart server
4. Configure `config.yml`

See [Installation Guide](INSTALLATION.md) for details.

### What Java version do I need?
Java 21 or higher is required.

### Do I need MySQL?
No! By default, LiteBansReborn uses H2 (embedded database). But MySQL is recommended for networks.

### Can I migrate from another plugin?
Yes! We support importing from:
- LiteBans
- BanManager
- AdvancedBan
- And more

See [Migration Guide](MIGRATION.md).

---

## Commands

### What's the main command?
`/lbr` or `/litebansreborn`

### How do I ban permanently?
Just omit the duration:
```
/ban Notch Hacking
```

### How do I ban temporarily?
Include a duration:
```
/ban Notch 7d Hacking
```

### What duration formats are supported?
- `s` - seconds (30s)
- `m` - minutes (30m)
- `h` - hours (24h)
- `d` - days (7d)
- `w` - weeks (2w)
- `mo` - months (1mo)
- `y` - years (1y)

### How do I make a silent punishment?
Add `-s` flag:
```
/ban Notch -s Hacking
```

### How do I unban someone?
```
/unban Notch
```

See [Commands Reference](COMMANDS.md) for all commands.

---

## Database

### What databases are supported?
- MySQL / MariaDB
- PostgreSQL
- SQLite
- H2 (default)
- MongoDB

### How do I switch to MySQL?
Edit `config.yml`:
```yaml
database:
  type: "MYSQL"
  host: "localhost"
  port: 3306
  database: "litebans"
  username: "root"
  password: "password"
```

### Will I lose data switching databases?
Yes, unless you export and import:
```
/lbr export sql
# Change config
/lbr import file export.sql
```

### How do I backup?
For MySQL:
```bash
mysqldump -u user -p litebans > backup.sql
```

See [Database Guide](DATABASE.md).

---

## Anti-VPN

### How do I enable Anti-VPN?
In `config.yml`:
```yaml
anti-vpn:
  enabled: true
  action: "KICK"
```

### Is it free to use?
Yes! Basic detection is free. For higher limits, some providers offer free API keys.

### How do I allow certain VPNs?
Add to whitelist:
```
/vpncheck whitelist add 1.2.3.4
```

Or in config:
```yaml
anti-vpn:
  whitelist:
    ips:
      - "1.2.3.4"
```

### How do I bypass VPN check for staff?
Give permission:
```
litebansreborn.vpncheck.bypass
```

See [Anti-VPN Guide](ANTIVPN.md).

---

## Permissions

### What's the staff permission?
```
litebansreborn.staff
```
This gives access to all staff commands.

### How do I give ban permission only?
```
litebansreborn.ban
```

### How do I exempt someone from being banned?
```
litebansreborn.ban.exempt
```

### Where's the full permission list?
See [Permissions Reference](PERMISSIONS.md).

---

## Troubleshooting

### Commands not working
1. Check permissions
2. Check plugin is loaded: `/plugins`
3. Reload: `/lbr reload`

### Database connection failed
1. Check credentials
2. Check MySQL is running
3. Check firewall

### Players can still chat while muted
1. Check mute is active: `/checkmute Player`
2. Check blocked commands in config
3. Check for conflicting plugins

### VPN not being detected
1. Add more providers
2. Add API keys
3. Clear cache: `/lbr antivpn clearcache`

### Placeholders not working
1. Check PlaceholderAPI is installed
2. Reload: `/papi reload`
3. Check placeholder name is correct

### Web panel not loading
1. Check port is open
2. Check firewall rules
3. Check SSL configuration

---

## Features

### What is Ghost Mute?
A shadow mute where the player thinks they're chatting normally, but only staff can see their messages.
```
/ghostmute Notch
```

### What is HWID Banning?
Hardware ID banning identifies players by their computer's hardware, not just their account. This prevents ban evasion with alt accounts.

### What is the Evidence System?
Attach proof to punishments:
```
/evidence add 1234 https://youtube.com/clip
```

### What is the Redemption System?
Banned players can complete minigames (math problems, typing tests) to reduce their ban time.
```
/redemption start math
```

### What is the Web Panel?
A browser-based dashboard for managing punishments:
- View statistics
- Search players
- Manage punishments
- Staff activity

### Does it support Discord?
Yes! Discord webhooks for notifications. Full Discord bot is planned for future versions.

### Does it support PlaceholderAPI?
Yes! 50+ placeholders available. See [Placeholders](PLACEHOLDERS.md).

---

## Getting Help

### Where can I get support?
- **Discord**: [Join Server](https://discord.gg/nuvik)
- **GitHub Issues**: [Report Bug](https://github.com/nuvik/litebansreborn/issues)
- **SpigotMC**: [Discussion](https://spigotmc.org/resources/litebansreborn)

### How do I report a bug?
1. Check if it's already reported
2. Include server version
3. Include plugin version
4. Include error logs
5. Include steps to reproduce

### How do I request a feature?
Open a GitHub issue or ask on Discord!

---

## Contributing

### Can I contribute?
Yes! We welcome contributions:
- Bug fixes
- New features
- Documentation
- Translations

### How do I contribute?
1. Fork the repository
2. Make changes
3. Submit pull request

### Is there a development guide?
See [Developer API](API.md) for coding guidelines.
