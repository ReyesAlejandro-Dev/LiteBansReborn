# 📜 Commands Reference

Complete list of all commands in LiteBansReborn.

## Table of Contents
- [Punishment Commands](#punishment-commands)
- [Information Commands](#information-commands)
- [Utility Commands](#utility-commands)
- [V4.0 Commands](#v40-commands)
- [Admin Commands](#admin-commands)
- [Duration Format](#duration-format)
- [Flags](#flags)

---

## Punishment Commands

### Ban Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/ban <player> [duration] [reason]` | `litebansreborn.ban` | Ban a player |
| `/tempban <player> <duration> [reason]` | `litebansreborn.ban.temp` | Temporary ban |
| `/ipban <player\|ip> [duration] [reason]` | `litebansreborn.ipban` | Ban by IP |
| `/unban <player>` | `litebansreborn.unban` | Unban a player |
| `/unbanip <ip>` | `litebansreborn.unbanip` | Unban an IP |

**Examples:**
```bash
/ban Notch Hacking
/ban Notch 7d Using X-Ray
/tempban Notch 1h Spam
/ipban 192.168.1.1 Evading ban
/unban Notch
```

### Mute Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/mute <player> [duration] [reason]` | `litebansreborn.mute` | Mute a player |
| `/tempmute <player> <duration> [reason]` | `litebansreborn.mute.temp` | Temporary mute |
| `/ipmute <player\|ip> [duration] [reason]` | `litebansreborn.ipmute` | Mute by IP |
| `/unmute <player>` | `litebansreborn.unmute` | Unmute a player |
| `/unmuteip <ip>` | `litebansreborn.unmuteip` | Unmute an IP |

**Examples:**
```bash
/mute Notch Spam
/mute Notch 30m Excessive caps
/tempmute Notch 1d Toxicity
/unmute Notch
```

### Kick Command

| Command | Permission | Description |
|---------|------------|-------------|
| `/kick <player> [reason]` | `litebansreborn.kick` | Kick a player |

**Example:**
```bash
/kick Notch AFK check
```

### Warn Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/warn <player> [reason]` | `litebansreborn.warn` | Warn a player |
| `/unwarn <player> [id]` | `litebansreborn.unwarn` | Remove warning |
| `/warnings <player>` | `litebansreborn.warnings` | View warnings |

**Examples:**
```bash
/warn Notch Minor rule violation
/unwarn Notch 5
/warnings Notch
```

### Freeze Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/freeze <player> [reason]` | `litebansreborn.freeze` | Freeze a player |
| `/unfreeze <player>` | `litebansreborn.unfreeze` | Unfreeze a player |

**Examples:**
```bash
/freeze Notch Suspected hacking
/unfreeze Notch
```

### Ghost Mute

| Command | Permission | Description |
|---------|------------|-------------|
| `/ghostmute <player>` | `litebansreborn.ghostmute` | Shadow mute a player |

**Example:**
```bash
/ghostmute Notch
```

---

## Information Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/history <player>` | `litebansreborn.history` | View punishment history |
| `/staffhistory <staff>` | `litebansreborn.staffhistory` | View staff's punishments |
| `/checkban <player>` | `litebansreborn.checkban` | Check ban status |
| `/checkmute <player>` | `litebansreborn.checkmute` | Check mute status |
| `/banlist [page]` | `litebansreborn.banlist` | List all bans |
| `/mutelist [page]` | `litebansreborn.mutelist` | List all mutes |

**Examples:**
```bash
/history Notch
/staffhistory Admin
/checkban Notch
/banlist 2
```

---

## Utility Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/clearchat` | `litebansreborn.clearchat` | Clear server chat |
| `/mutechat` | `litebansreborn.mutechat` | Toggle chat mute |
| `/staffchat <message>` | `litebansreborn.staffchat` | Staff-only chat |
| `/punish <player>` | `litebansreborn.punish` | Open punishment GUI |
| `/report <player> <reason>` | `litebansreborn.report` | Report a player |
| `/reports [page]` | `litebansreborn.reports.view` | View reports |
| `/handlereport <id> <accept\|deny>` | `litebansreborn.reports.handle` | Handle report |
| `/geoip <player>` | `litebansreborn.geoip` | Lookup location |
| `/alts <player>` | `litebansreborn.alts` | Check for alts |
| `/dupeip <player\|ip>` | `litebansreborn.dupeip` | Find duplicate IPs |
| `/allowplayer <player>` | `litebansreborn.allowplayer` | Allow during lockdown |

**Examples:**
```bash
/staffchat Player might be hacking
/punish Notch
/report Cheater Flying around the map
/geoip Notch
/alts Notch
```

---

## V4.0 Commands

### Evidence System

| Command | Permission | Description |
|---------|------------|-------------|
| `/evidence add <id> <url>` | `litebansreborn.evidence` | Add evidence URL |
| `/evidence view <id>` | `litebansreborn.evidence` | View evidence GUI |
| `/evidence capture <player> <id>` | `litebansreborn.evidence` | Capture inventory |

**Examples:**
```bash
/evidence add 1234 https://youtube.com/watch?v=...
/evidence view 1234
/evidence capture Notch 1234
```

### Redemption System

| Command | Permission | Description |
|---------|------------|-------------|
| `/redemption start <type>` | - | Start challenge |
| `/redemption answer <answer>` | - | Submit answer |
| `/redemption status` | - | View status |
| `/redemption cancel` | - | Cancel challenge |

**Examples:**
```bash
/redemption start math
/redemption answer 42
/redemption status
```

### Hardware ID

| Command | Permission | Description |
|---------|------------|-------------|
| `/hwid check <player>` | `litebansreborn.hwid` | View HWID info |
| `/hwid ban <hwid> <reason>` | `litebansreborn.hwid` | Ban by HWID |
| `/hwid alts <player>` | `litebansreborn.hwid` | Find linked accounts |
| `/hwid fingerprint <player>` | `litebansreborn.hwid` | Update fingerprint |
| `/hwid status` | `litebansreborn.hwid` | System status |

**Examples:**
```bash
/hwid check Notch
/hwid alts Notch
```

### Anti-VPN

| Command | Permission | Description |
|---------|------------|-------------|
| `/vpncheck <player\|ip>` | `litebansreborn.vpncheck` | Check VPN status |
| `/vpncheck stats` | `litebansreborn.vpncheck` | VPN statistics |
| `/vpncheck recent [count]` | `litebansreborn.vpncheck` | Recent detections |
| `/vpncheck history <player>` | `litebansreborn.vpncheck` | IP history |
| `/vpncheck whitelist <action>` | `litebansreborn.vpncheck` | Manage whitelist |

**Examples:**
```bash
/vpncheck Notch
/vpncheck 1.2.3.4
/vpncheck whitelist add 1.2.3.4
```

---

## Admin Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/lbr reload` | `litebansreborn.admin` | Reload config |
| `/lbr info` | `litebansreborn.admin` | Plugin info |
| `/lbr stats` | `litebansreborn.admin` | Statistics |
| `/lbr import <source>` | `litebansreborn.admin` | Import data |
| `/lbr export <format>` | `litebansreborn.admin` | Export data |
| `/lbr antivpn <action>` | `litebansreborn.admin` | Anti-VPN control |

### Anti-VPN Control
```bash
/lbr antivpn on           # Enable
/lbr antivpn off          # Disable
/lbr antivpn status       # View status
/lbr antivpn alerts on    # Toggle alerts
/lbr antivpn action KICK  # Set action
/lbr antivpn whitelist    # View whitelist
/lbr antivpn clearcache   # Clear cache
```

---

## Duration Format

Durations can be specified in various formats:

| Format | Description | Example |
|--------|-------------|---------|
| `s` | Seconds | `30s` = 30 seconds |
| `m` | Minutes | `30m` = 30 minutes |
| `h` | Hours | `2h` = 2 hours |
| `d` | Days | `7d` = 7 days |
| `w` | Weeks | `2w` = 2 weeks |
| `mo` | Months | `3mo` = 3 months |
| `y` | Years | `1y` = 1 year |

**Combined durations:**
```
1d12h    = 1 day and 12 hours
2w3d     = 2 weeks and 3 days
1mo2w    = 1 month and 2 weeks
```

**No duration = Permanent**

---

## Flags

Commands support special flags:

| Flag | Description |
|------|-------------|
| `-s` or `-silent` | Silent punishment (no broadcast) |
| `-e <url>` | Attach evidence |
| `-o` | Override existing punishment |

**Examples:**
```bash
/ban Notch -s Hacking           # Silent ban
/ban Notch Hacking -e https://youtube.com/...  # With evidence
/mute Notch -s 1h Spam          # Silent temporary mute
```

---

## Tab Completion

All commands support intelligent tab completion:
- Player names
- Online players
- Subcommands
- Duration suggestions
- Template suggestions
