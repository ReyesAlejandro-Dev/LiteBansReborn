# 📝 Placeholders Reference

Complete list of PlaceholderAPI placeholders.

## Table of Contents
- [Requirements](#requirements)
- [Player Placeholders](#player-placeholders)
- [Punishment Placeholders](#punishment-placeholders)
- [Statistics Placeholders](#statistics-placeholders)
- [Staff Placeholders](#staff-placeholders)
- [Server Placeholders](#server-placeholders)
- [Formatting](#formatting)
- [Examples](#examples)

---

## Requirements

- PlaceholderAPI installed
- LiteBansReborn 4.0.0+

Placeholders are automatically registered when both plugins are present.

---

## Player Placeholders

### Ban Status

| Placeholder | Description | Example |
|-------------|-------------|---------|
| `%litebans_isbanned%` | Is player banned? | `true` / `false` |
| `%litebans_banreason%` | Ban reason | `Hacking` |
| `%litebans_banoperator%` | Who banned | `Admin` |
| `%litebans_banexpiry%` | Ban expiry date | `2024-01-15 14:30` |
| `%litebans_banremaining%` | Time remaining | `2d 5h` |
| `%litebans_bantype%` | Ban type | `Temporary` / `Permanent` |
| `%litebans_banid%` | Ban ID | `12345` |
| `%litebans_bandate%` | Ban date | `2024-01-08 14:30` |

### Mute Status

| Placeholder | Description | Example |
|-------------|-------------|---------|
| `%litebans_ismuted%` | Is player muted? | `true` / `false` |
| `%litebans_mutereason%` | Mute reason | `Spam` |
| `%litebans_muteoperator%` | Who muted | `Moderator` |
| `%litebans_muteexpiry%` | Mute expiry | `2024-01-09 10:00` |
| `%litebans_muteremaining%` | Time remaining | `1h 30m` |
| `%litebans_mutetype%` | Mute type | `Temporary` |
| `%litebans_muteid%` | Mute ID | `12346` |

### Warning Status

| Placeholder | Description | Example |
|-------------|-------------|---------|
| `%litebans_warnings%` | Warning count | `3` |
| `%litebans_haswarnings%` | Has warnings? | `true` / `false` |
| `%litebans_lastwarning_reason%` | Last warning reason | `Minor violation` |
| `%litebans_lastwarning_date%` | Last warning date | `2024-01-07` |

### Freeze Status

| Placeholder | Description | Example |
|-------------|-------------|---------|
| `%litebans_isfrozen%` | Is player frozen? | `true` / `false` |
| `%litebans_freezereason%` | Freeze reason | `Investigation` |
| `%litebans_freezeoperator%` | Who froze | `Admin` |

---

## Punishment Placeholders

### History

| Placeholder | Description | Example |
|-------------|-------------|---------|
| `%litebans_totalbans%` | Player's total bans | `5` |
| `%litebans_totalmutes%` | Player's total mutes | `3` |
| `%litebans_totalkicks%` | Player's total kicks | `2` |
| `%litebans_totalwarnings%` | Player's total warnings | `8` |
| `%litebans_totalpunishments%` | All punishments | `18` |
| `%litebans_activepunishments%` | Active punishments | `2` |

### First/Last Punishment

| Placeholder | Description |
|-------------|-------------|
| `%litebans_firstban_date%` | First ban date |
| `%litebans_lastban_date%` | Last ban date |
| `%litebans_lastban_reason%` | Last ban reason |
| `%litebans_lastban_operator%` | Last ban operator |
| `%litebans_lastpunishment_type%` | Last punishment type |
| `%litebans_lastpunishment_date%` | Last punishment date |

---

## Statistics Placeholders

### Global Statistics

| Placeholder | Description | Example |
|-------------|-------------|---------|
| `%litebans_global_bans%` | Total bans on server | `1523` |
| `%litebans_global_mutes%` | Total mutes | `892` |
| `%litebans_global_kicks%` | Total kicks | `3421` |
| `%litebans_global_warnings%` | Total warnings | `2156` |
| `%litebans_global_total%` | Total punishments | `7992` |

### Active Statistics

| Placeholder | Description | Example |
|-------------|-------------|---------|
| `%litebans_active_bans%` | Currently active bans | `234` |
| `%litebans_active_mutes%` | Currently active mutes | `56` |
| `%litebans_frozen_players%` | Currently frozen | `3` |

### Today Statistics

| Placeholder | Description | Example |
|-------------|-------------|---------|
| `%litebans_today_bans%` | Bans today | `12` |
| `%litebans_today_mutes%` | Mutes today | `8` |
| `%litebans_today_kicks%` | Kicks today | `23` |
| `%litebans_today_warnings%` | Warnings today | `15` |

---

## Staff Placeholders

### Staff Performance

| Placeholder | Description | Example |
|-------------|-------------|---------|
| `%litebans_staff_bans%` | Bans by this staff | `45` |
| `%litebans_staff_mutes%` | Mutes by this staff | `23` |
| `%litebans_staff_kicks%` | Kicks by this staff | `89` |
| `%litebans_staff_warnings%` | Warnings by this staff | `112` |
| `%litebans_staff_total%` | Total by this staff | `269` |

### Staff Today

| Placeholder | Description | Example |
|-------------|-------------|---------|
| `%litebans_staff_today_bans%` | Staff's bans today | `3` |
| `%litebans_staff_today_mutes%` | Staff's mutes today | `2` |
| `%litebans_staff_today_total%` | Staff's total today | `8` |

---

## Server Placeholders

### Anti-VPN

| Placeholder | Description | Example |
|-------------|-------------|---------|
| `%litebans_vpn_enabled%` | Is Anti-VPN enabled? | `true` |
| `%litebans_vpn_detections%` | Total VPN detections | `156` |
| `%litebans_vpn_detections_today%` | VPN detections today | `12` |
| `%litebans_vpn_action%` | Current VPN action | `KICK` |

### Server Info

| Placeholder | Description | Example |
|-------------|-------------|---------|
| `%litebans_version%` | Plugin version | `4.0.0` |
| `%litebans_server%` | Server name | `Survival` |
| `%litebans_database%` | Database type | `MYSQL` |

---

## Formatting

### Date Formatting

Default format: `dd/MM/yyyy HH:mm:ss`

Configure in `config.yml`:
```yaml
general:
  date-format: "yyyy-MM-dd HH:mm"
```

### Duration Formatting

| Original | Formatted |
|----------|-----------|
| 3600000 | `1h` |
| 86400000 | `1d` |
| 604800000 | `7d` |

### Colors in Placeholders

Some placeholders support color based on value:

| Placeholder | If True | If False |
|-------------|---------|----------|
| `%litebans_isbanned_colored%` | `&cYes` | `&aNo` |
| `%litebans_ismuted_colored%` | `&cYes` | `&aNo` |

---

## Examples

### Scoreboard
```
&c&lPunishment Info
&7Bans: &f%litebans_totalbans%
&7Mutes: &f%litebans_totalmutes%
&7Warnings: &f%litebans_warnings%

&e&lServer Stats
&7Active Bans: &c%litebans_active_bans%
&7Active Mutes: &e%litebans_active_mutes%
```

### Tab Header
```
&7You have &c%litebans_warnings% &7warnings
%litebans_ismuted%
```

### Join Message
```
# DeluxeJoin
&8[&c!&8] &f{player} &7has &c%litebans_totalbans% &7previous bans!
```

### Chat Format
```
# EssentialsChat / ChatControl
[%litebans_warnings% warns] {player}: {message}
```

### Hologram
```
# HolographicDisplays / DecentHolograms
&c&lBan Statistics
&7Total Bans: &f%litebans_global_bans%
&7Active Bans: &c%litebans_active_bans%
&7Bans Today: &e%litebans_today_bans%
```

### NPC
```
# Citizens + PlaceholderAPI
%litebans_active_bans% players are currently banned.
```

### Discord Bot
```
# DiscordSRV
embed:
  title: "Ban Statistics"
  fields:
    - name: "Total Bans"
      value: "%litebans_global_bans%"
    - name: "Active"
      value: "%litebans_active_bans%"
```

---

## Troubleshooting

### Placeholder Not Working

1. **Check PlaceholderAPI installed:**
   ```
   /papi list
   ```

2. **Check LiteBansReborn registered:**
   ```
   /papi list litebans
   ```

3. **Reload placeholders:**
   ```
   /papi reload
   ```

### Wrong Values

- Check player has data in database
- Verify placeholder is spelled correctly
- Ensure player is online (for player placeholders)

### Performance

- Placeholders are cached
- Refresh interval: 60 seconds
- Heavy placeholders (history) may have delay
