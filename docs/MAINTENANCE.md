# 🚧 Maintenance Mode Guide

The maintenance mode allows you to restrict server access to staff or whitelisted players while you work on updates or fixes.

## Features
* **Lockdown**: Block all non-staff logins.
* **Whitelist**: Allow specific players to bypass maintenance.
* **Scheduled**: Set a duration for maintenance to auto-disable.
* **Custom Message**: Set a custom kick message/reason.
* **Discord Integration**: Notifications sent to Discord (if configured).

## Commands

All commands require `litebansreborn.maintenance` permission.

| Command | Description | Example |
|---|---|---|
| `/maintenance on [duration] [reason]` | Enable maintenance mode | `/maintenance on 2h Fixing bugs` |
| `/maintenance off` | Disable maintenance mode | `/maintenance off` |
| `/maintenance add <player>` | Add player to whitelist | `/maintenance add notch` |
| `/maintenance remove <player>` | Remove player from whitelist | `/maintenance remove notch` |
| `/maintenance list` | List whitelisted players | `/maintenance list` |
| `/maintenance status` | Check current status | `/maintenance status` |

## Configuration

Located in `config.yml`:

```yaml
maintenance:
  enabled: false
  reason: "Server maintenance in progress"
  whitelist:
    - "uuid-here"
```

## Permissions

* `litebansreborn.maintenance` - Full access to commands
* `litebansreborn.maintenance.bypass` - Bypass maintenance mode (can join while enabled)
