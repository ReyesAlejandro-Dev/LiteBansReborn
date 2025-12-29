# 🌐 Cross-Server Intelligence Guide

LiteBansReborn v5.1 introduces **Cross-Server Intelligence** - sync punishments and player data across your entire BungeeCord/Velocity network.

## Features

### 🔄 Real-time Sync
- **Ban Sync**: Ban on one server = ban message on all servers
- **Unban Sync**: Unbans propagate across the network
- **Alert Sync**: High-risk player alerts reach all staff

### 📊 Network Intelligence
- Track player movement across servers
- Share risk scores network-wide
- Detect ban evasion attempts

## Requirements

- **BungeeCord** or **Velocity** proxy
- Plugin installed on **all** backend servers
- Unique `server-name` for each server

## Configuration

```yaml
cross-server:
  enabled: true
  mode: "plugin-messaging"  # Recommended
  server-name: "survival1"  # Must be unique!
  sync-bans: true
  sync-alerts: true
```

## Sync Modes

| Mode | Description | Setup |
|---|---|---|
| `plugin-messaging` | Uses BungeeCord plugin channels | No extra setup |
| `redis` | Uses Redis pub/sub (coming soon) | Requires Redis |
| `mysql` | Uses database polling (coming soon) | Shared database |

## How It Works

1. **Player banned on Server A**
2. **Message sent via plugin channel**
3. **All servers receive the ban info**
4. **Staff on all servers see the broadcast**
5. **Player kicked if online on any server**

## Staff Alerts

Staff with `litebansreborn.alerts.crossserver` permission see:
- Bans from other servers
- Unbans from other servers
- High-risk player joins on other servers

## Network Commands

Use these from any server:
- All punishment commands work normally
- Bans apply network-wide automatically
- History shows punishments from all servers
