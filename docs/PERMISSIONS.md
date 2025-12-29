# 🔑 Permissions Reference

Complete list of all permissions in LiteBansReborn.

## Table of Contents
- [Permission Structure](#permission-structure)
- [Punishment Permissions](#punishment-permissions)
- [Information Permissions](#information-permissions)
- [Utility Permissions](#utility-permissions)
- [V4.0 Permissions](#v40-permissions)
- [Admin Permissions](#admin-permissions)
- [Wildcards](#wildcards)
- [LuckPerms Examples](#luckperms-examples)

---

## Permission Structure

All permissions follow the pattern:
```
litebansreborn.<category>.<action>
```

---

## Punishment Permissions

### Ban Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `litebansreborn.ban` | Ban players | op |
| `litebansreborn.ban.temp` | Temporary bans | op |
| `litebansreborn.ban.permanent` | Permanent bans | op |
| `litebansreborn.ban.exempt` | Cannot be banned | false |
| `litebansreborn.unban` | Unban players | op |
| `litebansreborn.ipban` | IP ban | op |
| `litebansreborn.unbanip` | Unban IP | op |

### Mute Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `litebansreborn.mute` | Mute players | op |
| `litebansreborn.mute.temp` | Temporary mutes | op |
| `litebansreborn.mute.permanent` | Permanent mutes | op |
| `litebansreborn.mute.exempt` | Cannot be muted | false |
| `litebansreborn.unmute` | Unmute players | op |
| `litebansreborn.ipmute` | IP mute | op |
| `litebansreborn.unmuteip` | Unmute IP | op |
| `litebansreborn.mute.bypass` | Bypass mute | false |

### Kick Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `litebansreborn.kick` | Kick players | op |
| `litebansreborn.kick.exempt` | Cannot be kicked | false |

### Warn Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `litebansreborn.warn` | Warn players | op |
| `litebansreborn.unwarn` | Remove warnings | op |
| `litebansreborn.warnings` | View warnings | op |
| `litebansreborn.warn.exempt` | Cannot be warned | false |

### Freeze Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `litebansreborn.freeze` | Freeze players | op |
| `litebansreborn.unfreeze` | Unfreeze players | op |
| `litebansreborn.freeze.bypass` | Bypass freeze | false |

### Ghost Mute

| Permission | Description | Default |
|------------|-------------|---------|
| `litebansreborn.ghostmute` | Ghost mute players | op |
| `litebansreborn.ghostmute.view` | See ghost muted messages | op |

---

## Information Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `litebansreborn.history` | View punishment history | op |
| `litebansreborn.history.own` | View own history | true |
| `litebansreborn.staffhistory` | View staff's punishments | op |
| `litebansreborn.checkban` | Check ban status | op |
| `litebansreborn.checkmute` | Check mute status | op |
| `litebansreborn.banlist` | View ban list | op |
| `litebansreborn.mutelist` | View mute list | op |

---

## Utility Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `litebansreborn.clearchat` | Clear server chat | op |
| `litebansreborn.clearchat.bypass` | Bypass chat clear | op |
| `litebansreborn.mutechat` | Toggle chat mute | op |
| `litebansreborn.mutechat.bypass` | Bypass chat mute | op |
| `litebansreborn.staffchat` | Use staff chat | op |
| `litebansreborn.punish` | Use punishment GUI | op |
| `litebansreborn.geoip` | Use GeoIP lookup | op |
| `litebansreborn.alts` | Check for alts | op |
| `litebansreborn.dupeip` | Find duplicate IPs | op |
| `litebansreborn.allowplayer` | Allow during lockdown | op |

### Report System

| Permission | Description | Default |
|------------|-------------|---------|
| `litebansreborn.report` | Report players | true |
| `litebansreborn.report.cooldown.bypass` | Bypass cooldown | op |
| `litebansreborn.reports.view` | View reports | op |
| `litebansreborn.reports.handle` | Handle reports | op |

### Appeal System

| Permission | Description | Default |
|------------|-------------|---------|
| `litebansreborn.appeal` | Submit appeals | true |
| `litebansreborn.appeals.view` | View appeals | op |
| `litebansreborn.appeals.handle` | Handle appeals | op |

### Notes System

| Permission | Description | Default |
|------------|-------------|---------|
| `litebansreborn.note` | Add notes | op |
| `litebansreborn.notes` | View notes | op |
| `litebansreborn.delnote` | Delete notes | op |

---

## V4.0 Permissions

### Evidence System

| Permission | Description | Default |
|------------|-------------|---------|
| `litebansreborn.evidence` | Manage evidence | op |
| `litebansreborn.evidence.add` | Add evidence | op |
| `litebansreborn.evidence.view` | View evidence | op |
| `litebansreborn.evidence.capture` | Capture inventory | op |
| `litebansreborn.evidence.delete` | Delete evidence | op |

### Hardware ID

| Permission | Description | Default |
|------------|-------------|---------|
| `litebansreborn.hwid` | HWID commands | op |
| `litebansreborn.hwid.check` | Check HWID | op |
| `litebansreborn.hwid.ban` | Ban by HWID | op |
| `litebansreborn.hwid.alts` | Find linked accounts | op |

### Anti-VPN

| Permission | Description | Default |
|------------|-------------|---------|
| `litebansreborn.vpncheck` | VPN check commands | op |
| `litebansreborn.vpncheck.bypass` | Bypass VPN check | false |
| `litebansreborn.antivpn.manage` | Manage Anti-VPN | op |

### Web Panel

| Permission | Description | Default |
|------------|-------------|---------|
| `litebansreborn.webpanel` | Web panel access | op |

---

## Admin Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `litebansreborn.admin` | Full admin access | op |
| `litebansreborn.reload` | Reload plugin | op |
| `litebansreborn.import` | Import data | op |
| `litebansreborn.export` | Export data | op |

### Notification Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `litebansreborn.notify` | Receive all notifications | op |
| `litebansreborn.notify.ban` | Ban notifications | op |
| `litebansreborn.notify.mute` | Mute notifications | op |
| `litebansreborn.notify.kick` | Kick notifications | op |
| `litebansreborn.notify.warn` | Warn notifications | op |
| `litebansreborn.notify.vpn` | VPN detection alerts | op |
| `litebansreborn.notify.client` | Client detection alerts | op |

### Silent Punishments

| Permission | Description | Default |
|------------|-------------|---------|
| `litebansreborn.silent` | Use silent punishments | op |
| `litebansreborn.silent.view` | See silent punishments | op |

---

## Wildcards

### Staff Wildcard
```
litebansreborn.staff
```
Includes:
- All punishment commands
- History viewing
- Notifications
- Alt detection
- VPN checking
- GUI access

### Full Admin Wildcard
```
litebansreborn.*
```
Includes everything.

---

## LuckPerms Examples

### Setup Staff Group
```bash
# Create group
/lp creategroup moderator

# Add basic permissions
/lp group moderator permission set litebansreborn.ban true
/lp group moderator permission set litebansreborn.ban.temp true
/lp group moderator permission set litebansreborn.mute true
/lp group moderator permission set litebansreborn.kick true
/lp group moderator permission set litebansreborn.warn true
/lp group moderator permission set litebansreborn.freeze true
/lp group moderator permission set litebansreborn.history true
/lp group moderator permission set litebansreborn.notify true
/lp group moderator permission set litebansreborn.punish true
/lp group moderator permission set litebansreborn.staffchat true

# Or use wildcard
/lp group moderator permission set litebansreborn.staff true
```

### Setup Admin Group
```bash
# Full access
/lp group admin permission set litebansreborn.* true

# Or inherit from moderator
/lp group admin parent add moderator
/lp group admin permission set litebansreborn.admin true
/lp group admin permission set litebansreborn.ban.permanent true
/lp group admin permission set litebansreborn.evidence true
/lp group admin permission set litebansreborn.hwid true
```

### Exempt Players
```bash
# Exempt owner from punishments
/lp group owner permission set litebansreborn.ban.exempt true
/lp group owner permission set litebansreborn.mute.exempt true
/lp group owner permission set litebansreborn.kick.exempt true
```

### Bypass Permissions
```bash
# VIPs bypass mute
/lp group vip permission set litebansreborn.mute.bypass true

# Staff bypass VPN check
/lp group staff permission set litebansreborn.vpncheck.bypass true
```

---

## Permission Hierarchy

```
litebansreborn.*
├── litebansreborn.admin
│   ├── litebansreborn.reload
│   ├── litebansreborn.import
│   └── litebansreborn.export
├── litebansreborn.staff
│   ├── litebansreborn.ban
│   ├── litebansreborn.mute
│   ├── litebansreborn.kick
│   ├── litebansreborn.warn
│   ├── litebansreborn.freeze
│   ├── litebansreborn.history
│   ├── litebansreborn.notify
│   └── ...
└── litebansreborn.player
    ├── litebansreborn.report
    └── litebansreborn.appeal
```
