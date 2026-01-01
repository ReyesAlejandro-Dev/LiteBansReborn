# 🤖 Discord Bot Setup Guide

Complete guide to setting up the native Discord bot.

## Table of Contents
- [Overview](#overview)
- [Bot Creation](#bot-creation)
- [Configuration](#configuration)
- [Slash Commands](#slash-commands)
- [Notifications](#notifications)
- [Permissions](#permissions)
- [Troubleshooting](#troubleshooting)

---

## Overview

LiteBansReborn v4.5 includes a **native Discord bot** powered by JDA (Java Discord API). No external bot needed!

### Features
- 🔨 **Punishment commands** - Ban, mute, kick from Discord
- 📊 **Statistics** - View server stats
- 🔍 **Lookups** - Check player history
- 🔗 **Verification** - Link MC and Discord accounts
- 📢 **Notifications** - Real-time punishment logs

---

## Bot Creation

### Step 1: Create Discord Application

1. Go to [Discord Developer Portal](https://discord.com/developers/applications)
2. Click **"New Application"**
3. Enter name: `LiteBansReborn` (or your server name)
4. Click **"Create"**

### Step 2: Create Bot User

1. Go to **"Bot"** tab on the left
2. Click **"Add Bot"**
3. Click **"Yes, do it!"**

### Step 3: Get Bot Token

1. In the Bot tab, click **"Reset Token"**
2. Copy the token (keep it SECRET!)
3. Save it for config.yml

### Step 4: Enable Intents

In the Bot tab, enable these **Privileged Gateway Intents**:
- ✅ **Server Members Intent**
- ✅ **Message Content Intent**

### Step 5: Invite Bot to Server

1. Go to **"OAuth2"** → **"URL Generator"**
2. Select scopes:
   - ✅ `bot`
   - ✅ `applications.commands`
3. Select permissions:
   - ✅ Send Messages
   - ✅ Embed Links
   - ✅ Read Message History
   - ✅ Manage Roles (for verification)
   - ✅ Use Slash Commands
4. Copy the generated URL
5. Open URL in browser and invite to your server

---

## Configuration

### Basic Setup

```yaml
discord-bot:
  # Enable the bot
  enabled: true
  
  # Your bot token (from Discord Developer Portal)
  token: "YOUR_BOT_TOKEN_HERE"
```

### Channel Configuration

```yaml
discord-bot:
  channels:
    # Channel for punishment logs
    logs: "1234567890123456789"
    
    # Channel for ticket notifications
    tickets: "1234567890123456790"
    
    # Channel for verification
    verification: "1234567890123456791"
```

**How to get Channel ID:**
1. Enable Developer Mode in Discord (Settings → Advanced)
2. Right-click channel → Copy ID

### Staff Roles

```yaml
discord-bot:
  # Role IDs that can use staff commands
  staff-roles:
    - "1234567890123456789"  # Moderator role
    - "1234567890123456790"  # Admin role
```

### Verified Role

```yaml
discord-bot:
  # Role to assign when player verifies
  verified-role: "1234567890123456789"
```

---

## Slash Commands

### Staff Commands
(Require staff role or Administrator permission)

| Command | Description |
|---------|-------------|
| `/ban <player> [duration] [reason]` | Ban a player |
| `/unban <player>` | Unban a player |
| `/mute <player> [duration] [reason]` | Mute a player |
| `/history <player>` | View punishment history |
| `/checkban <player>` | Check if player is banned |
| `/lookup <player>` | View player information |

### Public Commands

| Command | Description |
|---------|-------------|
| `/stats` | View server punishment statistics |
| `/verify <code>` | Link your Minecraft account |

### Examples

**Ban a player:**
```
/ban Notch 7d Hacking
```

**Check ban status:**
```
/checkban Notch
```

**View history:**
```
/history Notch
```

---

## Notifications

### Punishment Logs

When punishments occur, they're posted to the logs channel:

```
🔨 BAN
────────────────────
Player: Notch
Staff: Admin
Duration: 7 days
Reason: X-Ray hacking

Today at 2:30 PM
```

### Log Types

| Emoji | Type |
|-------|------|
| 🔨 | Ban |
| 🔇 | Mute |
| 👢 | Kick |
| ⚠️ | Warning |
| ✅ | Unban |
| 🔊 | Unmute |

### Enable/Disable Specific Notifications

```yaml
discord-bot:
  notifications:
    bans: true
    mutes: true
    kicks: true
    warnings: true
    unbans: true
    unmutes: true
```

---

## Permissions

### Discord Permissions Needed

| Permission | Required For |
|------------|--------------|
| Send Messages | Sending notifications |
| Embed Links | Rich embeds |
| Manage Roles | Assigning verified role |
| Use Slash Commands | Command registration |

### In-Game Staff Check

The bot checks if Discord users have staff roles:
```yaml
staff-roles:
  - "MODERATOR_ROLE_ID"
  - "ADMIN_ROLE_ID"
```

Or Discord Administrator permission.

---

## Verification Flow

### Step 1: Player runs `/verify` in-game
```
✦ DISCORD VERIFICATION ✦
────────────────────────
Your verification code is:

     ABC123

Go to our Discord server and use:
  /verify ABC123

This code expires in 10 minutes.
────────────────────────
```

### Step 2: Player runs `/verify ABC123` in Discord
```
✅ Account Verified!
Your Discord account is now linked to Notch
```

### Step 3: (Optional) Verified role assigned

---

## Troubleshooting

### Bot Not Starting

**Check console for errors:**
```
[LiteBansReborn] Failed to start Discord bot: Invalid token
```

**Solutions:**
- Verify token is correct
- Check token hasn't been regenerated
- Ensure no extra spaces in token

### Slash Commands Not Appearing

**Wait up to 1 hour** - Discord caches commands globally.

**Force refresh:**
- Kick and re-invite the bot
- Restart Discord client (Ctrl+R)

### Bot is Offline

1. Check token is valid
2. Check intents are enabled
3. Check bot wasn't kicked from server
4. Check console for errors

### "Missing Permissions"

Ensure bot has required permissions:
- Send Messages
- Embed Links
- Manage Roles (if using verification)

### Commands Return "No Permission"

Ensure your Discord role is in `staff-roles`:
```yaml
staff-roles:
  - "YOUR_ROLE_ID"
```

---

## Best Practices

1. **Keep token secret** - Never share your bot token
2. **Use dedicated channels** - Create #punishment-logs and #verification
3. **Set up roles** - Create staff roles for command access
4. **Test first** - Test commands before going live
5. **Monitor logs** - Check console for errors

---

## Full Configuration Example

```yaml
discord-bot:
  enabled: true
  token: "MTIzNDU2Nzg5MDEyMzQ1Njc4OQ.ABCDEF.xyz123..."
  
  channels:
    logs: "1234567890123456789"
    tickets: "1234567890123456790"
    verification: "1234567890123456791"
  
  staff-roles:
    - "1111111111111111111"  # Moderator
    - "2222222222222222222"  # Admin
  
  verified-role: "3333333333333333333"
  
  sync-punishments: true
  
  commands:
    ban: true
    unban: true
    mute: true
    unmute: true
    kick: true
    history: true
    checkban: true
    stats: true
    lookup: true
  
  notifications:
    bans: true
    mutes: true
    kicks: true
    warnings: true
    unbans: true
    unmutes: true
```
