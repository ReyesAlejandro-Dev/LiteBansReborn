# 📊 Web Panel Setup Guide

Complete guide to setting up the LiteBansReborn Web Panel (v6.0.0+).

## Table of Contents
- [Overview](#overview)
- [Quick Setup](#quick-setup)
- [Configuration](#configuration)
- [API Endpoints](#api-endpoints)
- [Authentication](#authentication)
- [Security](#security)
- [Troubleshooting](#troubleshooting)

---

## Overview

The Web Panel provides:
- **Real-time dashboard** - Server stats at a glance
- **Punishment management** - View, search, and manage punishments
- **Player search** - Find players by name (now case-insensitive in v6.0)
- **Staff statistics** - Track moderator activity
- **REST API** - Integration with external tools

### v6.0.0 Updates
- **Security Hardened**: All punishment endpoints now strictly whitelist inputs to prevent SQL Injection.
- **Async Backend**: Dashboard metrics load instantly without lagging the main thread.
- **Smart IP**: The plugin now auto-detects your server's public IP for easier setup.

---

## Quick Setup

### Step 1: Enable in Config
```yaml
web-panel:
  enabled: true
  port: 8080
```

### Step 2: Set API Key
```yaml
web-panel:
  api-keys:
    - "your-secret-key-here"
```

### Step 3: Restart Server
```bash
/lbr reload
```

### Step 4: Access Panel
Open in browser: `http://your-server-ip:8080`

---

## Configuration

### Full Configuration
```yaml
web-panel:
  enabled: true
  port: 8080
  
  # Enable SSL/HTTPS (recommended for public panels)
  ssl: false
  keystore-password: "changeit"
  
  api-keys:
    - "admin-key-12345"
  
  features:
    real-time-dashboard: true
    punishment-management: true
    player-search: true
    staff-statistics: true
    appeal-handling: true
```

---

## Security

### SQL Injection Protection
In v6.0.0+, all API inputs are validated against a strict whitelist or sanitized before touching the database.

### Best Practices

1. **Use Strong API Keys**: 
   Generate a random key: `openssl rand -hex 32`
2. **Enable SSL**: 
   Use a reverse proxy (Nginx/Apache) or the built-in SSL support to encrypt traffic.
3. **Restrict Access**: 
   If possible, use a firewall to allow access only from your management IP or VPN.

### Nginx Reverse Proxy Example (Recommended)
This is the safest way to host the panel publicly.

```nginx
server {
    listen 80;
    server_name bans.myserver.com;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

---

## API Endpoints

All endpoints require `Authorization: Bearer <api-key>`.

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/dashboard` | GET | Server stats, online players |
| `/api/punishments` | GET | List bans, mutes, etc. |
| `/api/actions/execute` | POST | Run actions (ban, mute, etc.) |
| `/api/players/search` | GET | Fuzzy search for players |
| `/api/reports` | GET | View player reports |

---

## Troubleshooting

### "Address already in use"
Another program (or a stalled instance of the plugin) is using the port.
**Fix:** Change the port in `config.yml` to `8081` or kills the process using it.

### "Authentication Failed"
Ensure you are passing the API key in the header:
`Authorization: Bearer YOUR_KEY`

### "Database Locked" (SQLite)
If you are using SQLite, upgrade to LiteBansReborn v6.0.0+. Earlier versions had concurrency issues with the Web Panel reading while the server wrote data. v6.0.0 fixes this with synchronization.
