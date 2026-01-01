# ⚙️ Configuration Guide

Complete guide to configuring LiteBansReborn.

## Table of Contents
- [File Structure](#file-structure)
- [config.yml](#configyml)
- [messages.yml](#messagesyml)
- [durations.yml](#durationsyml)
- [templates.yml](#templatesyml)

---

## File Structure

```
/plugins/LiteBansReborn/
├── config.yml           # Main configuration
├── messages.yml         # All plugin messages
├── durations.yml        # Duration presets for GUI
├── templates.yml        # Punishment templates
├── reasons.yml          # Predefined reasons
├── database/            # Local database files
├── evidence/            # Evidence attachments
├── snapshots/           # Chat snapshots
└── web/                 # Web panel files
```

---

## config.yml

### General Settings

```yaml
general:
  # Server name shown in punishments
  server-name: "MyServer"
  
  # Enable debug logging
  debug: false
  
  # Enable metrics (bStats)
  metrics: true
  
  # Date format for punishments
  date-format: "dd/MM/yyyy HH:mm:ss"
  
  # Default punishment reason
  default-reason: "No reason specified"
  
  # Broadcast punishments to all players
  broadcast: true
  
  # Notify staff of punishments
  notify-staff: true
```

### Database Configuration

```yaml
database:
  # Type: MYSQL, MARIADB, POSTGRESQL, SQLITE, H2, MONGODB
  type: "H2"
  
  # Connection settings (for remote databases)
  host: "localhost"
  port: 3306
  database: "litebans"
  username: "root"
  password: ""
  
  # Table prefix
  table-prefix: "lbr_"
  
  # Connection pool settings
  pool:
    maximum-pool-size: 10
    minimum-idle: 2
    connection-timeout: 30000
    idle-timeout: 600000
    max-lifetime: 1800000
```

### Anti-VPN System

```yaml
anti-vpn:
  # Enable Anti-VPN
  enabled: true
  
  # Action: KICK, WARN, ALLOW, NONE
  action: "KICK"
  
  # API providers (in order of priority)
  providers:
    - proxycheck
    - ip-api
    - vpnapi
    - iphub
  
  # API keys (optional, increases limits)
  api-keys:
    proxycheck: ""
    vpnapi: ""
    iphub: ""
    ipqualityscore: ""
  
  # Cache duration in minutes
  cache-duration: 60
  
  # Send alerts to staff
  alerts: true
  
  # Whitelist
  whitelist:
    ips:
      - "127.0.0.1"
    countries:
      - "US"
      - "CA"
```

### Ghost Mute

```yaml
ghost-mute:
  # Enable ghost mute feature
  enabled: true
  
  # Send message back to the muted player
  echo-to-player: true
  
  # Prefix shown to staff
  staff-prefix: "&8[&cGhost&8] "
```

### Chat Snapshots

```yaml
snapshots:
  # Enable chat snapshots
  enabled: true
  
  # Number of messages to capture
  message-count: 50
  
  # Directory for snapshots
  directory: "snapshots"
```

### Auto-Rollback

```yaml
rollback:
  # Enable auto-rollback
  enabled: true
  
  # Require CoreProtect
  require-coreprotect: true
  
  # Rollback time period
  time: "24h"
  
  # Show button in GUI
  gui-button: true
  
  # Command to execute
  command: "co rollback u:{player} t:{time} #silent"
```

### Evidence System

```yaml
evidence:
  # Enable evidence system
  enabled: true
  
  # Storage: local, s3, cloudinary
  storage: "local"
  
  # Auto-capture inventory when banning
  auto-capture-inventory: true
  
  # Allowed evidence types
  types:
    - screenshots
    - video-links
    - replay-links
    - chat-logs
```

### Smart Rate Limiting

```yaml
smart-limits:
  # Enable rate limiting
  enabled: true
  
  # Staff limits
  staff:
    max-bans-per-hour: 20
    max-permanent-per-day: 5
    require-2fa-for-perma: true
  
  # Player limits
  players:
    report-cooldown: "5m"
    appeal-cooldown: "24h"
    max-reports-per-day: 10
```

### Redemption System

```yaml
redemption:
  # Enable redemption
  enabled: true
  
  # Minigames configuration
  minigames:
    captcha-math:
      enabled: true
      reduction: 10
      max-attempts: 3
      one-time: false
    
    typing-test:
      enabled: true
      reduction: 15
      max-attempts: 2
      one-time: true
    
    quiz:
      enabled: true
      reduction: 20
      max-attempts: 1
      one-time: true
```

### Hardware ID Banning

```yaml
hardware-ban:
  # Enable HWID banning
  enabled: false
  
  # Client mod name
  client-mod: "LiteBansClient"
  
  # Track these identifiers
  track:
    - hardware-id
    - mac-address-hash
    - gpu-signature
  
  # Alt detection threshold (0-100)
  alt-detection-threshold: 70
```

### Web Panel

```yaml
web-panel:
  # Enable web panel
  enabled: false
  
  # Port number
  port: 8080
  
  # Enable SSL
  ssl: false
  
  # API keys for authentication
  api-keys:
    - "your-secret-api-key-here"
  
  # Features
  features:
    real-time-dashboard: true
    punishment-management: true
    player-search: true
    staff-statistics: true
    appeal-handling: true
```

### Punishment Settings

```yaml
punishments:
  ban:
    # Broadcast ban messages
    broadcast: true
    
    # Kick player when banned
    kick-on-ban: true
    
    # Data wipe on permanent ban
    wipe-data:
      enabled: false
      only-permanent: true
      delete-world-data: false
      commands:
        - "lp user {player} clear"
        - "eco take {player} *"
  
  mute:
    # Broadcast mute messages
    broadcast: true
    
    # Block commands while muted
    blocked-commands:
      - "msg"
      - "tell"
      - "whisper"
      - "me"
  
  kick:
    # Broadcast kick messages
    broadcast: true
  
  warn:
    # Broadcast warnings
    broadcast: true
    
    # Escalation (auto-punishment after X warns)
    escalation:
      enabled: true
      3: "mute {player} 1h Too many warnings"
      5: "tempban {player} 1d Exceeded warning limit"
```

---

## messages.yml

All messages are customizable. Supports color codes (`&c`, `&#FF0000`), MiniMessage, and placeholders.

```yaml
prefix: "&8[&c&lLiteBans&8] &7"

general:
  no-permission: "{prefix}&cYou don't have permission."
  player-not-found: "{prefix}&cPlayer not found."
  reload-success: "{prefix}&aConfiguration reloaded."

ban:
  message: "&c&lYou have been banned!\n\n&7Reason: &f{reason}\n&7Expires: &f{expires}\n&7Banned by: &f{operator}"
  broadcast: "{prefix}&c{player} &7has been banned by &c{operator}&7. Reason: &f{reason}"
  
mute:
  message: "{prefix}&cYou have been muted! Reason: &f{reason}"
  broadcast: "{prefix}&e{player} &7has been muted by &e{operator}&7."     
```

---

## durations.yml

Presets for the duration selection GUI.

```yaml
durations:
  - id: "1h"
    display: "&a1 Hour"
    material: CLOCK
    duration: "1h"
  
  - id: "1d"
    display: "&e1 Day"
    material: CLOCK
    duration: "1d"
  
  - id: "1w"
    display: "&61 Week"
    material: CLOCK
    duration: "7d"
  
  - id: "1m"
    display: "&c1 Month"
    material: CLOCK
    duration: "30d"
  
  - id: "perm"
    display: "&4&lPermanent"
    material: BARRIER
    duration: ""
```

---

## templates.yml

Predefined punishment templates.

```yaml
templates:
  hacking:
    name: "Hacking"
    reason: "Using unauthorized client modifications"
    duration: ""  # Permanent
    type: BAN
    
  xray:
    name: "X-Ray"
    reason: "Using X-Ray texture pack or mod"
    duration: "30d"
    type: BAN
    
  spam:
    name: "Spam"
    reason: "Spamming in chat"
    duration: "1h"
    type: MUTE
    
  advertising:
    name: "Advertising"
    reason: "Advertising other servers"
    duration: ""
    type: BAN
```

---

## Hot Reload

Most configuration changes can be applied without restarting:

```
/lbr reload
```

Changes that require a restart:
- Database type change
- Web panel port change
- Major feature toggles
