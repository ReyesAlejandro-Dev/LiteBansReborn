# 🕸️ Social Network Analysis Guide

LiteBansReborn v5.1 introduces **Social Network Analysis** - a revolutionary feature that tracks player relationships and detects alt accounts automatically.

## Features

### 🔗 Alt Detection
- Automatically detects players sharing the same IP
- Tracks HWID matches (if enabled)
- Links accounts that have connected from the same network

### 👥 Relationship Tracking
- Records which players play together frequently
- Builds a social graph of player connections
- Updates relationship strength over time

### ⚠️ Banned Associates
- Alerts when a player connected to banned players joins
- Shows the type of relationship (alt, frequent partner, etc.)
- Calculates a "banned connection score"

## Commands

| Command | Description | Permission |
|---|---|---|
| `/network alts <player>` | Find alt accounts | `litebansreborn.network` |
| `/network connections <player>` | View all connections | `litebansreborn.network` |
| `/network check <player>` | Full network analysis | `litebansreborn.network` |
| `/network banned <player>` | Show banned associates | `litebansreborn.network` |

## Configuration

```yaml
social-network:
  enabled: true
  alert-banned-associates: true
  alert-threshold: 50
```

## How It Works

1. **On Join**: Records player IP and checks for matches
2. **During Play**: Tracks who plays with whom
3. **On Quit**: Saves session partnerships
4. **On Analysis**: Calculates relationship strengths

## Relationship Types

| Type | Description | Weight |
|---|---|---|
| `ALT_ACCOUNT` | Same IP/HWID | 100 |
| `FREQUENT_PARTNER` | Played together often | 70 |
| `SAME_SESSION` | Online at same time | 30 |
| `BANNED_ASSOCIATE` | Connected to banned player | 50 |

## Staff Alerts

Staff with `litebansreborn.alerts.network` permission will see:
- When a potential alt of a banned player joins
- When a player with many banned associates joins
- Network analysis results in real-time
