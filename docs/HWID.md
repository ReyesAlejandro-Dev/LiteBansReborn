# 🔐 Hardware ID (HWID) Banning Guide

Complete guide to hardware ID banning in LiteBansReborn.

## Table of Contents
- [Overview](#overview)
- [How It Works](#how-it-works)
- [Configuration](#configuration)
- [Commands](#commands)
- [Alt Detection](#alt-detection)
- [Client Mod](#client-mod)
- [Best Practices](#best-practices)

---

## Overview

Hardware ID (HWID) banning is an advanced feature that identifies players by their computer's hardware, not just their Minecraft account. This makes ban evasion with alt accounts nearly impossible.

### Benefits
- 🛡️ Prevents alt account evasion
- 🔍 Automatically detects linked accounts
- 📊 Fingerprinting even without client mod
- 🎯 Target the player, not just the account

### Limitations
- ⚠️ Requires client mod for full HWID
- ⚠️ Fingerprinting is probabilistic
- ⚠️ Shared computers may cause false positives

---

## How It Works

### With Client Mod
1. Player installs LiteBansClient mod
2. Mod collects hardware identifiers
3. Sends HWID to server on join
4. Server stores and tracks HWID
5. If HWID is banned, player is rejected

### Without Client Mod (Fingerprinting)
1. Server collects behavioral data
2. IP patterns, client settings, play style
3. Creates a "fingerprint" profile
4. Compares with other players
5. Similarity score indicates potential alt

### Identifiers Tracked

| Identifier | Reliability | Notes |
|------------|-------------|-------|
| Hardware ID | ★★★★★ | Requires client mod |
| MAC Address Hash | ★★★★☆ | Can be spoofed |
| GPU Signature | ★★★★☆ | Unique per graphics card |
| Client Brand | ★★★☆☆ | Vanilla, Forge, Fabric |
| Locale | ★★☆☆☆ | Language settings |
| View Distance | ★★☆☆☆ | Client setting |
| IP Patterns | ★★★☆☆ | Shared IPs common |

---

## Configuration

### Basic Setup
```yaml
hardware-ban:
  # Enable HWID banning
  enabled: true
  
  # Client mod name (for plugin message channel)
  client-mod: "LiteBansClient"
  
  # What to track
  track:
    - hardware-id
    - mac-address-hash
    - gpu-signature
  
  # Alt detection threshold (0-100)
  # Higher = fewer false positives, but may miss alts
  alt-detection-threshold: 70
```

### Recommended Threshold Settings

| Setting | Use Case |
|---------|----------|
| 50 | Aggressive - catches more alts, more false positives |
| 70 | Balanced - good for most servers |
| 85 | Conservative - fewer false positives |
| 95 | Very strict - only definite matches |

---

## Commands

### Check Player HWID
```bash
/hwid check <player>
```
Shows:
- HWID (if available)
- Client brand
- Locale and settings
- Known IPs
- First/last seen

### Ban by HWID
```bash
/hwid ban <hwid> <reason>
```

Example:
```bash
/hwid ban abc123def456... Ban evasion
```

### Find Alt Accounts
```bash
/hwid alts <player>
```
Shows:
- Same HWID matches (definite alts)
- Similar fingerprints (potential alts)
- Similarity scores

### Update Fingerprint
```bash
/hwid fingerprint <player>
```
Manually refresh player's fingerprint.

### System Status
```bash
/hwid status
```
Shows system status and statistics.

---

## Alt Detection

### Same HWID Detection
When two accounts share the same HWID, they are **definitively** the same person:

```
[HWID] Alt Detection: Notch
----------------------------------------
Same HWID:
  ⚠ Herobrine (100% match)
  ⚠ Steve (100% match)
```

### Fingerprint Similarity
Without HWID, we use fingerprint similarity:

```
Similar Fingerprints:
  80% Herobrine
  65% Alex
  42% Steve
```

### Similarity Factors

| Factor | Weight | Description |
|--------|--------|-------------|
| Same IP | 30% | Shared IP address |
| Same HWID | 100% | Definitive match |
| Client Brand | 10% | Same Minecraft client |
| Locale | 10% | Same language |
| View Distance | 5% | Same setting |
| Skin | 15% | Same skin signature |

### Example Detection
```
Player 1 (Notch):
- IP: 1.2.3.4
- Client: Lunar Client
- Locale: en_US
- View: 12

Player 2 (Herobrine):
- IP: 1.2.3.5 (different)
- Client: Lunar Client (+10%)
- Locale: en_US (+10%)
- View: 12 (+5%)

Similarity: 25% - Probably not an alt
```

---

## Client Mod

### For Server Owners

The LiteBansClient mod is optional but provides the most reliable HWID detection.

**Requirements:**
- Fabric or Forge
- Minecraft 1.21+
- Server with HWID enabled

**Installation:**
1. Players download LiteBansClient
2. Install in mods folder
3. Connect to server
4. HWID is automatically sent

### For Players

Players are not required to install the mod, but servers may offer benefits for doing so.

### Technical Details

The mod communicates via plugin messaging:
- Channel: `litebans:hwid`
- Data: Encrypted hardware identifiers
- Timing: Sent on join

---

## Best Practices

### 1. Start with Fingerprinting
Don't require the client mod at first:
```yaml
hardware-ban:
  enabled: true
  # Fingerprinting works without mod
```

### 2. Set Appropriate Threshold
Start higher, lower if needed:
```yaml
alt-detection-threshold: 80
```

### 3. Investigate Before Banning
Use `/hwid alts` to check before acting:
```bash
/hwid alts SuspiciousPlayer
```

### 4. Don't Auto-Ban Alts
Review manually to avoid false positives:
```bash
# Don't do this automatically!
if altDetected:
    ban(player)

# Instead, alert staff:
if altDetected:
    alertStaff(player)
```

### 5. Whitelist Shared Computers
For internet cafes or shared IPs:
```yaml
whitelist-ips:
  - "1.2.3.4"  # Internet cafe
```

### 6. Monitor for False Positives
Check logs regularly for patterns.

---

## Technical Details

### HWID Collection (Client Mod)

The client mod collects:
```
- Windows: System UUID from WMI
- Mac: Hardware UUID from IOKit  
- Linux: Machine ID + DMI
- Hash: SHA-256 of combined identifiers
```

### Data Security
- HWIDs are hashed before storage
- Only hash comparisons are made
- Raw hardware data never stored
- Encrypted transmission

### Privacy Considerations
- Inform players in server rules
- HWID is one-way hash (irreversible)
- No personal information collected
- Complies with GDPR (hashed data)

---

## Troubleshooting

### No HWID Detected
- Player doesn't have client mod
- Fingerprinting is still active
- Check `/hwid check <player>`

### Too Many False Positives
- Increase threshold to 80-90
- Check for shared IPs (college, work)
- Review before acting

### Mod Not Working
- Check plugin channel is registered
- Verify mod version matches
- Check console for errors

### HWID Changed
- Possible hardware upgrade
- Possible spoofing attempt
- Track pattern of changes
