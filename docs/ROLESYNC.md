# 🔄 Role Synchronization Guide

Sync your Minecraft ranks (LuckPerms) with Discord roles automatically.

## Requirements
* **LuckPerms v5+** installed on your server.
* **Discord Bot** configured and running (see [DISCORD.md](DISCORD.md)).
* Players must have linked their accounts using `/verify`.

## Features
* **Minecraft → Discord**: Sync in-game ranks to Discord roles.
* **Discord → Minecraft**: Sync Discord roles to in-game ranks.
* **Bidirectional**: Sync both ways (be careful with loops!).
* **Auto-Sync**: Syncs automatically when a player joins.

## Commands

All commands require `litebansreborn.rolesync` permission.

| Command | Description |
|---|---|
| `/rolesync sync [player]` | Force sync a specific player (or all if no player specified) |
| `/rolesync add <role_id> <group>` | Add a mapping pair |
| `/rolesync remove <role_id>` | Remove a mapping pair |
| `/rolesync list` | List all mappings |
| `/rolesync status` | Check status |

## Configuration

Located in `config.yml`:

```yaml
role-sync:
  enabled: true
  direction: "discord-to-minecraft" # or minecraft-to-discord
  sync-on-join: true
  mappings:
    "123456789012345678": "vip"    # Discord Role ID : LP Group Name
    "987654321098765432": "staff"
```

## How to get Role IDs
1. Enable **Developer Mode** in Discord (User Settings > Advanced).
2. Right-click a role in Server Settings > Roles.
3. Click **Copy ID**.
