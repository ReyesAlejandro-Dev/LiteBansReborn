# 📁 Case Files Guide

LiteBansReborn v5.1 introduces **Automatic Case Files** - a system that automatically collects evidence when you freeze a player.

## Features

### 🎯 Automatic Evidence Collection
When you `/freeze` a player, the system automatically saves:
- **Last 200 chat messages**
- **Recent commands used**
- **Movement history** (coordinates over time)
- **Connection info** (IP, client, ping)
- **Player relationships** (alts, associates)

### 📂 Case Management
- Each case gets a unique ID (e.g., `#A7B3C2D1`)
- Cases are stored permanently in the database
- Multiple cases can exist for the same player

## Commands

| Command | Description | Permission |
|---|---|---|
| `/case view <id>` | View case file details | `litebansreborn.case` |
| `/case list <player>` | List all cases for a player | `litebansreborn.case` |
| `/case evidence <id> [type]` | View collected evidence | `litebansreborn.case` |
| `/case create <player>` | Manually create a case | `litebansreborn.case` |

## Evidence Types

| Type | Description |
|---|---|
| `CHAT_HISTORY` | Last 200 chat messages with timestamps |
| `COMMAND_HISTORY` | Recent commands executed |
| `MOVEMENT_HISTORY` | Position data with flying/sprint status |
| `CONNECTION_INFO` | IP, client brand, ping, protocol |
| `PLAYER_RELATIONSHIPS` | Known alts and associates |

## Configuration

```yaml
case-files:
  enabled: true
  auto-create-on-freeze: true
  track-chat: true
  track-movement: true
  max-chat-history: 200
```

## Example Workflow

1. **Player reports hacker**: Receive report about suspicious player
2. **Staff freezes player**: `/freeze SuspiciousPlayer`
3. **Case file auto-created**: System collects all evidence
4. **Review evidence**: `/case view A7B3C2D1`
5. **Check chat log**: `/case evidence A7B3C2D1 CHAT_HISTORY`
6. **Make decision**: Ban/unban based on evidence

## Privacy Note

Evidence is only collected for players who are being investigated (frozen). Regular players' data is stored temporarily in memory and cleared after logout.
