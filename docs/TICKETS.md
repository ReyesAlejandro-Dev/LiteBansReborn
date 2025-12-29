# 🎫 Ticket System Guide

Complete guide to the support ticket system.

## Table of Contents
- [Overview](#overview)
- [Configuration](#configuration)
- [Commands](#commands)
- [Categories](#categories)
- [Workflow](#workflow)
- [Discord Integration](#discord-integration)
- [Staff Guide](#staff-guide)

---

## Overview

The Ticket System provides a structured way for players to:
- 📝 Appeal punishments
- 🚨 Report players
- ❓ Request support
- 🐛 Report bugs

### Features
- Multiple ticket categories
- Staff claiming system
- Message history
- Discord notifications
- Auto-close on inactivity

---

## Configuration

```yaml
tickets:
  # Enable ticket system
  enabled: true
  
  # Max open tickets per player
  max-open-per-player: 3
  
  # Cooldown between tickets (minutes)
  cooldown: 5
  
  # Auto-close after inactivity (hours)
  auto-close-hours: 72
  
  # Notify staff on new ticket
  notify-staff: true
  
  # Create Discord thread for tickets
  discord-threads: true
  
  # Categories
  categories:
    appeal:
      enabled: true
      icon: "BOOK"
      description: "Appeal a punishment"
    report:
      enabled: true
      icon: "PAPER"
      description: "Report a player"
    support:
      enabled: true
      icon: "COMPASS"
      description: "General support"
    bug:
      enabled: true
      icon: "BARRIER"
      description: "Report a bug"
```

---

## Commands

### Player Commands

| Command | Description |
|---------|-------------|
| `/ticket create <category> <subject>` | Create a new ticket |
| `/ticket list` | View your tickets |
| `/ticket view <id>` | View ticket details |
| `/ticket respond <id> <message>` | Add a message to ticket |
| `/ticket close <id>` | Close your ticket |
| `/ticket gui` | Open ticket GUI |

### Staff Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/ticket claim <id>` | `litebansreborn.tickets.claim` | Claim a ticket |
| `/tickets` | `litebansreborn.tickets.view` | View all open tickets |

---

## Categories

### Appeal
For players appealing punishments.

```
/ticket create appeal I was banned for hacking but I wasn't using any mods
```

### Report
For reporting rule-breakers.

```
/ticket create report Player123 is using killaura
```

### Support
For general help requests.

```
/ticket create support I lost my items after server restart
```

### Bug
For reporting server bugs.

```
/ticket create bug The /home command teleports me underground
```

---

## Workflow

### Player Workflow

```
1. Player creates ticket
   /ticket create appeal I want to appeal my ban

2. System creates ticket #123
   Staff notified

3. Staff claims ticket
   /ticket claim 123

4. Staff responds
   /ticket respond 123 Please explain what happened

5. Player responds
   /ticket respond 123 I was banned for X-Ray but I found diamonds normally

6. Staff resolves
   /ticket close 123
```

### Ticket Status

| Status | Description | Color |
|--------|-------------|-------|
| OPEN | New ticket, unclaimed | 🟡 Yellow |
| CLAIMED | Staff is handling | 🟠 Orange |
| WAITING_RESPONSE | Waiting for player | 🔵 Blue |
| CLOSED | Resolved | 🟢 Green |
| RESOLVED | Successfully resolved | 🟢 Green |

---

## Discord Integration

### Notifications

When a ticket is created, staff get notified:
```
📋 New Ticket #123
Player: Notch
Category: Appeal
Subject: I want to appeal my ban
```

### Discord Threads (Planned)

Each ticket can create a Discord thread for discussion.

---

## Staff Guide

### Viewing Open Tickets

```
/tickets
```

Shows all tickets:
```
----------------------------------------
Open Tickets
----------------------------------------
#123 [OPEN] Appeal - Notch
#124 [CLAIMED] Report - Steve
#125 [WAITING] Support - Alex
----------------------------------------
```

### Claiming a Ticket

```
/ticket claim 123
```

This assigns you as the handler.

### Responding

```
/ticket respond 123 Thank you for your appeal. Can you explain...
```

### Viewing Ticket Details

```
/ticket view 123
```

Shows:
```
----------------------------------------
Ticket #123
----------------------------------------
Category: Appeal
Status: CLAIMED
Player: Notch
Claimed by: Admin
Subject: I want to appeal my ban

Messages:
[Player] Notch: I was banned for hacking but...
[Staff] Admin: Can you explain what happened?
[Player] Notch: I was just mining normally...
----------------------------------------
```

### Closing a Ticket

```
/ticket close 123
```

### GUI Management

```
/ticket gui
```

Opens a visual interface for ticket management.

---

## Permissions

| Permission | Description |
|------------|-------------|
| `litebansreborn.ticket` | Create tickets (default: true) |
| `litebansreborn.tickets.view` | View all tickets |
| `litebansreborn.tickets.claim` | Claim tickets |
| `litebansreborn.tickets.respond` | Respond to any ticket |
| `litebansreborn.tickets.close` | Close any ticket |

---

## Best Practices

### For Staff
1. **Claim tickets quickly** - Don't leave players waiting
2. **Be professional** - Represent your server well
3. **Follow up** - Check for player responses
4. **Close properly** - Close tickets when resolved

### For Server Owners
1. **Set categories** - Enable only relevant categories
2. **Train staff** - Ensure staff know the system
3. **Monitor metrics** - Track response times
4. **Enable notifications** - Use Discord integration

---

## Messages

Customize in `messages.yml`:
```yaml
tickets:
  created: "&aTicket #%id% created successfully!"
  claimed: "&aYou have claimed ticket #%id%"
  responded: "&aMessage added to ticket #%id%"
  closed: "&aTicket #%id% has been closed"
  staff-notify: "&e[Ticket] &f%player% &7created ticket #%id%"
```
