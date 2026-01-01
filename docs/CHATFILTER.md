# 💬 Chat Filter Guide

Complete guide to the chat filtering system.

## Table of Contents
- [Overview](#overview)
- [Configuration](#configuration)
- [Filter Types](#filter-types)
- [Blocked Words](#blocked-words)
- [Regex Patterns](#regex-patterns)
- [Whitelists](#whitelists)
- [Auto-Punishment](#auto-punishment)
- [Commands](#commands)

---

## Overview

The Chat Filter protects your server from:
- 🚫 Profanity and inappropriate language
- 📢 Spam and message flooding
- 🔗 Unwanted advertisements
- 🔤 Excessive caps
- 📋 Repeated messages

### How It Works
1. Player sends message
2. Filter checks against all rules
3. Message is blocked, filtered, or allowed
4. Violations are tracked
5. Auto-punishment after threshold

---

## Configuration

```yaml
chat-filter:
  # Enable chat filter
  enabled: true
  
  # Block advertisements (IPs, domains)
  block-ads: true
  
  # Block all links
  block-links: false
  
  # Block excessive caps
  block-caps: true
  caps-threshold: 70
  min-caps-length: 5
  
  # Block spam (rapid messages)
  block-spam: true
  spam-message-limit: 5
  spam-time-window: 10
  
  # Block flood (repeated messages)
  block-flood: true
  flood-similarity: 80
  
  # Block character spam
  block-char-spam: true
  char-spam-threshold: 4
  
  # Auto-mute settings
  auto-mute-threshold: 5
  auto-mute-duration: "10m"
  
  # Blocked words
  blocked-words:
    - "badword1"
    - "badword2"
  
  # Blocked patterns (regex)
  blocked-patterns:
    - "discord\\.gg\\/[a-zA-Z0-9]+"
  
  # Whitelisted domains
  whitelist-domains:
    - "youtube.com"
    - "youtu.be"
    - "imgur.com"
```

---

## Filter Types

### 1. Anti-Spam 🚫
Prevents rapid message sending.

**Settings:**
```yaml
block-spam: true
spam-message-limit: 5    # Max messages
spam-time-window: 10     # In X seconds
```

**Example:**
```
Player: Hi
Player: Hello
Player: Hey
Player: Hola
Player: Yo
[BLOCKED] Player tried to spam
```

### 2. Anti-Flood 📋
Prevents repeated similar messages.

**Settings:**
```yaml
block-flood: true
flood-similarity: 80    # % similarity to count as repeat
```

**Example:**
```
Player: Anyone want to trade?
Player: Anyone wanna trade?    <- 85% similar, blocked
```

### 3. Anti-Caps 🔤
Reduces excessive caps lock.

**Settings:**
```yaml
block-caps: true
caps-threshold: 70       # Max % caps
min-caps-length: 5       # Min message length
```

**Example:**
```
Player: THIS IS ALL CAPS
Filtered to: this is all caps
```

**Note:** Caps messages are filtered (converted to lowercase), not blocked.

### 4. Anti-Advertisement 📢
Blocks server IPs and domain advertisements.

**Settings:**
```yaml
block-ads: true
```

**Blocks:**
- IP addresses: `192.168.0.1`, `mc.server.com:25565`
- Domains: `join.myserver.com`
- Discord invites: `discord.gg/abc123`

### 5. Anti-Link 🔗
Blocks all links (optional).

**Settings:**
```yaml
block-links: false    # Off by default
```

**Blocks (when enabled):**
- `https://example.com`
- `www.example.com`
- Any URL

### 6. Anti-Character Spam 🔡
Blocks repeated characters.

**Settings:**
```yaml
block-char-spam: true
char-spam-threshold: 4    # Max repeated chars
```

**Example:**
```
Player: Hiiiiiiiii    <- Blocked (i repeated 10 times)
```

### 7. Blocked Words 🚫
Filters/blocks specific words.

**Settings:**
```yaml
blocked-words:
  - "badword"
  - "inappropriate"
```

**Behavior:** Words are censored with asterisks.
```
Player: That's a badword
Filtered: That's a *******
```

---

## Blocked Words

### Adding Words

In config:
```yaml
blocked-words:
  - "badword1"
  - "badword2"
  - "inappropriate"
```

### Case Insensitive

Words are matched regardless of case:
- `badword` matches `BADWORD`, `BadWord`, `bAdWoRd`

### Partial Matching

Words are matched even in larger words:
- `bad` in `badword` is caught

### Censoring

Blocked words are replaced with asterisks:
```
Input: "This is badword"
Output: "This is *******"
```

---

## Regex Patterns

For advanced filtering, use regex patterns.

### Common Patterns

**Discord Invites:**
```yaml
blocked-patterns:
  - "discord\\.gg\\/[a-zA-Z0-9]+"
  - "discord\\.com\\/invite\\/[a-zA-Z0-9]+"
```

**Phone Numbers:**
```yaml
blocked-patterns:
  - "\\b\\d{3}[-.]?\\d{3}[-.]?\\d{4}\\b"
```

**Email Addresses:**
```yaml
blocked-patterns:
  - "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
```

**Obfuscated Discord:**
```yaml
blocked-patterns:
  - "d[i1]sc[o0]rd\\s*\\.\\s*gg"
```

### Testing Patterns

Test regex at [regex101.com](https://regex101.com) before adding.

---

## Whitelists

### Domain Whitelist

Allow specific domains even with link blocking:

```yaml
whitelist-domains:
  - "youtube.com"
  - "youtu.be"
  - "imgur.com"
  - "twitter.com"
  - "twitch.tv"
  - "yourserver.com"
```

### Bypass Permission

Staff can bypass filters:
```
litebansreborn.chatfilter.bypass
```

---

## Auto-Punishment

### Warning System

Each violation adds a warning. After threshold, auto-mute:

```yaml
auto-mute-threshold: 5     # Violations before mute
auto-mute-duration: "10m"  # Mute duration
```

**Flow:**
```
Violation 1 → Warning
Violation 2 → Warning
Violation 3 → Warning
Violation 4 → Warning
Violation 5 → AUTO-MUTE 10 minutes
```

### Violation Reasons

| Reason | Description |
|--------|-------------|
| SPAM | Too many messages |
| FLOOD | Repeated messages |
| CAPS | Excessive caps |
| ADVERTISEMENT | IP/domain ad |
| LINK | Blocked link |
| PROFANITY | Blocked word |
| CHAR_SPAM | Character spam |

---

## Commands

### Admin Commands

| Command | Description |
|---------|-------------|
| `/lbr chatfilter on` | Enable chat filter |
| `/lbr chatfilter off` | Disable chat filter |
| `/lbr chatfilter status` | View current status |
| `/lbr chatfilter reload` | Reload filter config |

### Adding Words at Runtime

```bash
/lbr chatfilter addword <word>
/lbr chatfilter removeword <word>
```

---

## Messages

Player warnings:

| Filter | Message |
|--------|---------|
| Spam | "Please don't spam!" |
| Flood | "Please don't repeat messages!" |
| Caps | "Please don't use excessive caps!" |
| Ads | "Advertising is not allowed!" |
| Links | "Links are not allowed!" |
| Profanity | "Please watch your language!" |
| Char Spam | "Please don't spam characters!" |

Customize in `messages.yml`:
```yaml
chat-filter:
  spam: "&cPlease don't spam!"
  flood: "&cPlease don't repeat messages!"
  caps: "&cPlease don't use excessive caps!"
  ads: "&cAdvertising is not allowed!"
  links: "&cLinks are not allowed!"
  profanity: "&cPlease watch your language!"
  char-spam: "&cPlease don't spam characters!"
  auto-muted: "&cYou have been muted for chat violations."
```

---

## Best Practices

1. **Start conservative** - Don't block too much initially
2. **Whitelist your domains** - Allow your website/Discord
3. **Review regularly** - Check for false positives
4. **Use bypass wisely** - Give to trusted staff only
5. **Test patterns** - Test regex before deploying
6. **Balance warnings** - 5 warnings is reasonable

---

## Troubleshooting

### Filter Too Aggressive

Lower thresholds:
```yaml
caps-threshold: 80      # Was 70
char-spam-threshold: 5  # Was 4
```

### Filter Not Working

1. Check filter is enabled
2. Check player doesn't have bypass
3. Check blocked words are in config
4. Reload: `/lbr reload`

### False Positives

Add to whitelist:
```yaml
whitelist-domains:
  - "falsepositive.com"
```

Or remove from blocked words.
