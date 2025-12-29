# 📸 Evidence System Guide

Complete guide to attaching evidence to punishments.

## Table of Contents
- [Overview](#overview)
- [Evidence Types](#evidence-types)
- [Commands](#commands)
- [Adding Evidence](#adding-evidence)
- [Viewing Evidence](#viewing-evidence)
- [Storage Options](#storage-options)
- [Best Practices](#best-practices)

---

## Overview

The Evidence System allows staff to attach proof to punishments. This helps with:
- 📋 Documentation for appeals
- 🔍 Review by senior staff
- 📊 Quality assurance
- ⚖️ Fair punishment process

---

## Evidence Types

### Supported Types

| Type | Description | Example |
|------|-------------|---------|
| **Screenshots** | Image proof | Imgur, Discord CDN |
| **Videos** | Recorded footage | YouTube, Streamable |
| **Replay Links** | Replay mods | Replay.gg, Medal.tv |
| **Chat Logs** | Saved chat | Auto-captured |
| **Inventory** | Player inventory | Auto-captured |

### Auto-Detection

The system automatically detects evidence type from URL:
```
youtube.com → VIDEO
imgur.com → SCREENSHOT
medal.tv → REPLAY
streamable.com → VIDEO
gyazo.com → SCREENSHOT
```

---

## Commands

### Add Evidence
```bash
/evidence add <punishment_id> <url>
```

Example:
```bash
/evidence add 1234 https://youtube.com/watch?v=abc123
```

### View Evidence
```bash
/evidence view <punishment_id>
```
Opens a GUI showing all attached evidence.

### Capture Inventory
```bash
/evidence capture <player> <punishment_id>
```
Saves the player's current inventory as evidence.

---

## Adding Evidence

### During Punishment
Add evidence while punishing:
```bash
/ban Notch 7d Hacking -e https://youtube.com/clip
```

### After Punishment
Add evidence to existing punishment:
```bash
/evidence add 1234 https://youtube.com/watch?v=abc123
```

### Auto-Capture

Some evidence is captured automatically:

#### Chat Logs
When enabled, the last N messages are saved:
```yaml
snapshots:
  enabled: true
  message-count: 50
```

#### Inventory
When enabled, player inventory is saved on ban:
```yaml
evidence:
  auto-capture-inventory: true
```

---

## Viewing Evidence

### In-Game GUI
```bash
/evidence view 1234
```

GUI shows:
```
┌─────────────────────────────────────┐
│  📸 Evidence for Punishment #1234   │
├─────────────────────────────────────┤
│ [Paper] 📹 Video - YouTube          │
│ Click to copy link                  │
│                                     │
│ [Book] 💬 Chat Log                  │
│ 50 messages saved                   │
│                                     │
│ [Chest] 🎒 Inventory                │
│ Auto-captured on ban                │
│                                     │
│ Added by: Admin                     │
│ Date: 2024-01-08 14:30              │
│                                     │
│     [Back]           [Delete]       │
└─────────────────────────────────────┘
```

### Via History
View evidence in punishment history:
```bash
/history Notch
```
Click on punishment to see evidence.

---

## Storage Options

### Local Storage (Default)
Evidence saved to plugin folder:
```yaml
evidence:
  storage: "local"
```

Files saved at:
```
/plugins/LiteBansReborn/evidence/
├── 1234/
│   ├── chat.txt
│   ├── inventory.json
│   └── metadata.json
└── 1235/
    └── ...
```

### Cloud Storage (Future)
Support planned for:
- Amazon S3
- Cloudinary
- Custom HTTP endpoint

```yaml
evidence:
  storage: "s3"
  s3:
    bucket: "my-evidence-bucket"
    region: "us-east-1"
    access-key: "..."
    secret-key: "..."
```

---

## Configuration

```yaml
evidence:
  # Enable evidence system
  enabled: true
  
  # Storage type
  storage: "local"
  
  # Auto-capture inventory when banning
  auto-capture-inventory: true
  
  # Allowed evidence types
  types:
    - screenshots
    - video-links
    - replay-links
    - chat-logs
  
  # Max evidence per punishment
  max-per-punishment: 10
  
  # Max file size (for uploads)
  max-file-size: "10MB"
```

---

## Evidence Viewer GUI

When clicking evidence items:

| Item | Action |
|------|--------|
| **Paper** (URL) | Copy link to chat |
| **Book** (Chat) | Open chat log viewer |
| **Chest** (Inv) | Open inventory viewer |
| **Barrier** | Delete evidence (admin) |

### Chat Log Viewer
```
┌─────────────────────────────────────┐
│  💬 Chat Log - Punishment #1234     │
├─────────────────────────────────────┤
│ [14:25] Player1: hey               │
│ [14:25] Player2: sup               │
│ [14:26] Notch: free stuff at spawn │
│ [14:26] Notch: /tpa me             │
│ [14:27] Player1: is this a scam?   │
│ [14:27] Notch: no trust me         │
│ [14:28] <BANNED BY ADMIN>          │
├─────────────────────────────────────┤
│     [◀ Previous]    [Next ▶]       │
└─────────────────────────────────────┘
```

### Inventory Viewer
```
┌─────────────────────────────────────┐
│  🎒 Inventory - Notch               │
├─────────────────────────────────────┤
│ [Diamond]x64 [Diamond]x64 [       ] │
│ [Diamond]x64 [Diamond]x64 [       ] │
│ [Pickaxe] [Sword]  [Bow]  [Arrows] │
│ ...                                 │
├─────────────────────────────────────┤
│    Note: 256 diamonds (suspicious)  │
└─────────────────────────────────────┘
```

---

## Best Practices

### 1. Always Add Evidence for Bans
```bash
# Good
/ban Notch 7d Hacking -e https://youtube.com/evidence

# Bad
/ban Notch 7d Hacking
```

### 2. Use Permanent Links
Use reliable hosts:
- ✅ YouTube (unlisted videos)
- ✅ Imgur
- ✅ Medal.tv
- ❌ Discord attachments (expire)

### 3. Describe Evidence
In ban reason:
```bash
/ban Notch 7d "Kill aura - see video evidence"
```

### 4. Capture Chat When Relevant
For toxicity/spam bans, always capture chat:
```bash
/evidence capture Notch 1234
```

### 5. Train Staff
Ensure all staff know how to:
- Add evidence during punishment
- Add evidence after punishment
- Review evidence for appeals

---

## Integration with Appeals

When a player appeals, reviewers can:

1. View the punishment
2. Click "View Evidence"
3. Review all attached proof
4. Make informed decision

```
Appeal #567 from Notch
Ban Reason: Hacking

[View Evidence (3 items)]

📹 Video - YouTube clip showing kill aura
📸 Screenshot - Player report
💬 Chat Log - 50 messages

[Accept] [Deny with reason]
```

---

## Troubleshooting

### Evidence Not Saving
- Check storage permissions
- Check disk space
- Verify URL is valid

### Can't View Evidence
- Check player has permission
- Verify punishment ID exists
- Check evidence wasn't deleted

### Chat Log Empty
- Ensure snapshots are enabled
- Check message-count setting
- Player must have sent messages
