# 🔗 Discord Verification Guide

Complete guide to linking Minecraft and Discord accounts.

## Table of Contents
- [Overview](#overview)
- [Configuration](#configuration)
- [Player Guide](#player-guide)
- [Commands](#commands)
- [Benefits](#benefits)
- [Staff Tools](#staff-tools)
- [Security](#security)

---

## Overview

Discord Verification links Minecraft accounts with Discord accounts. This provides:
- 🔒 **Account security** - Verify player identity
- 🎁 **Benefits** - Grant perks to verified players
- 📊 **Tracking** - Know who is who
- 🛡️ **Protection** - Reduce imposters

---

## Configuration

```yaml
verification:
  # Enable account linking
  enabled: true
  
  # Code expiry time (minutes)
  code-expiry: 10
  
  # Require verification to play
  required: false
  
  # Kick message if not verified (when required: true)
  kick-message: "&cYou must verify your Discord account to play!"
  
  # Benefits for verified players
  benefits:
    # Extra homes (if using Essentials)
    extra-homes: 0
    # Permissions to add on verification
    permissions:
      - "essentials.nick"
  
  # Update Minecraft name on join
  sync-names: true
```

### Require Verification

To make verification mandatory:
```yaml
verification:
  required: true
  kick-message: |
    &c&lVerification Required!
    
    &7Join our Discord and verify your account:
    &bdiscord.gg/yourserver
    
    &7Then use &e/verify &7in-game
```

---

## Player Guide

### Step 1: Get Verification Code

Run in Minecraft:
```
/verify
```

You'll see:
```
✦ DISCORD VERIFICATION ✦
────────────────────────────
Your verification code is:

     ABC123

Go to our Discord server and use:
  /verify ABC123

This code expires in 10 minutes.
────────────────────────────
```

### Step 2: Verify in Discord

Join your server's Discord and run:
```
/verify ABC123
```

You'll see:
```
✅ Account Verified!
Your Discord account is now linked to Notch
```

### Step 3: Enjoy Benefits!

You now have verified status.

---

## Commands

### In Minecraft

| Command | Description |
|---------|-------------|
| `/verify` | Generate verification code |
| `/verify info` | View your linked Discord |
| `/unlink` | Unlink your Discord account |
| `/discordinfo` | Same as `/verify info` |

### In Discord

| Command | Description |
|---------|-------------|
| `/verify <code>` | Complete verification |

### Staff Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/verify whois <player>` | `litebansreborn.verify.whois` | View player's Discord |
| `/whois <player>` | `litebansreborn.verify.whois` | Same as above |

---

## Benefits

### Automatic Role

Verified players receive a Discord role:
```yaml
discord-bot:
  verified-role: "1234567890123456789"
```

### Permissions

Grant permissions to verified players:
```yaml
verification:
  benefits:
    permissions:
      - "essentials.nick"
      - "essentials.hat"
      - "server.verified"
```

### LuckPerms Integration

Use with LuckPerms for advanced perks:
```bash
/lp group verified permission set essentials.nick true
```

Then track verified players who have the permission.

---

## Staff Tools

### Who Is This Player?

```
/whois Notch
```

Shows:
```
----------------------------------------
Who Is: Notch
----------------------------------------
Discord Status: Verified ✓
Discord Name: Notch#1234
Discord ID: 123456789012345678
Linked At: 2024-01-08 14:30:00
----------------------------------------
```

### Unverified Players

If a player is not verified:
```
----------------------------------------
Who Is: Steve
----------------------------------------
Discord Status: Not Verified
Use /verify in-game to link Discord.
----------------------------------------
```

---

## Security

### Code Expiry

Verification codes expire after 10 minutes:
```yaml
verification:
  code-expiry: 10  # minutes
```

### One-to-One Linking

- Each Minecraft account can only link to ONE Discord account
- Each Discord account can only link to ONE Minecraft account
- Prevents duplicate/fake verifications

### Unlink Cooldown

Consider adding a cooldown for unlinking to prevent abuse.

### Require Verification

For extra security, require verification:
```yaml
verification:
  required: true
```

Players must verify to play.

---

## Database

Verified accounts are stored in:
```
lbr_verified_players
├── minecraft_uuid
├── minecraft_name
├── discord_id
├── discord_name
└── linked_at
```

---

## Messages

Customize in `messages.yml`:
```yaml
verification:
  code-generated: |
    &a&l✦ DISCORD VERIFICATION ✦
    &7Your code: &e&l%code%
    &7Use &b/verify %code% &7in Discord
    &7Expires in &f10 minutes
  
  already-verified: "&cYou are already verified!"
  verified-success: "&a&l✓ Verification successful!"
  invalid-code: "&cInvalid or expired code."
  unlinked: "&aYour Discord has been unlinked."
```

---

## Troubleshooting

### "Invalid or expired code"

- Code expires after 10 minutes
- Generate a new code with `/verify`
- Make sure you typed it correctly

### "Already linked to another account"

- Each Discord can only link to one MC account
- Unlink the other account first

### Bot command not working

- Ensure Discord bot is enabled and running
- Check bot has slash command permission
- Wait for commands to register (up to 1 hour)

### Verified role not assigned

- Check `verified-role` is set correctly
- Ensure bot has "Manage Roles" permission
- Verified role must be BELOW bot's role

---

## Use Cases

### Anti-Impersonation
Verify staff accounts to prevent imposters.

### VIP Access
Require verification for VIP perks.

### Reporting System
Know who made reports via Discord integration.

### Community Building
Connect your Minecraft and Discord communities.
