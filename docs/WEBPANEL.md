# 📊 Web Panel Setup Guide

Complete guide to setting up the LiteBansReborn Web Panel.

## Table of Contents
- [Overview](#overview)
- [Quick Setup](#quick-setup)
- [Configuration](#configuration)
- [API Endpoints](#api-endpoints)
- [Authentication](#authentication)
- [SSL Setup](#ssl-setup)
- [Customization](#customization)
- [Security](#security)

---

## Overview

The Web Panel provides:
- **Real-time dashboard** - Server stats at a glance
- **Punishment management** - View, search, and manage punishments
- **Player search** - Find players by name
- **Staff statistics** - Track moderator activity
- **REST API** - Integration with external tools

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
```
/lbr reload
```

### Step 4: Access Panel
Open in browser: `http://your-server-ip:8080`

---

## Configuration

### Full Configuration
```yaml
web-panel:
  # Enable web panel
  enabled: true
  
  # Port to run on
  port: 8080
  
  # Enable SSL/HTTPS
  ssl: false
  
  # Keystore for SSL (if enabled)
  keystore-password: "changeit"
  
  # API keys for authentication
  api-keys:
    - "admin-key-12345"
    - "readonly-key-67890"
  
  # Features to enable
  features:
    real-time-dashboard: true
    punishment-management: true
    player-search: true
    staff-statistics: true
    appeal-handling: true
```

### Port Selection

| Port | Notes |
|------|-------|
| `80` | HTTP default (requires root/admin) |
| `443` | HTTPS default (requires root/admin) |
| `8080` | Common alternative (recommended) |
| `8443` | HTTPS alternative |
| `3000-9999` | Safe range |

**Note**: Ensure the port is open in your firewall.

---

## API Endpoints

### Public Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/health` | Health check |

### Authenticated Endpoints

All require `Authorization: Bearer <api-key>` header.

#### Dashboard
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/dashboard` | Server statistics |

**Response:**
```json
{
  "onlinePlayers": 50,
  "maxPlayers": 100,
  "serverName": "MyServer",
  "uptime": 3600000,
  "publicIP": "200.110.106.56",
  "port": 8080,
  "cachedBans": 150,
  "cachedMutes": 45,
  "frozenPlayers": 2,
  "vpnEnabled": true,
  "vpnCacheSize": 234
}
```

#### Punishments
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/punishments` | List punishments |
| GET | `/api/punishments/recent` | Recent punishments |

**Query Parameters:**
- `page` - Page number (default: 1)
- `limit` - Items per page (default: 20)
- `type` - Filter by type (ban, mute, kick, warn)

#### Players
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/players/search?q=<query>` | Search players |
| GET | `/api/players/history?uuid=<uuid>` | Player history |

#### Staff
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/staff/stats` | Staff statistics |
| GET | `/api/staff/online` | Online staff |

#### Reports
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/reports` | List reports |

**Query Parameters:**
- `page`, `limit`
- `status` (pending, accepted, denied, all)

#### Appeals
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/appeals` | List appeals |

**Query Parameters:**
- `page`, `limit`
- `status` (pending, accepted, denied, all)

#### Actions
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/actions/execute` | Execute any punishment action |
| POST | `/api/actions/ban` | Ban a player (legacy) |
| POST | `/api/actions/unban` | Unban a player (legacy) |

**Execute Action Request:**
```json
{
  "action": "ban",
  "player": "Notch",
  "reason": "Hacking",
  "duration": "7d"
}
```

**Available Actions:**
| Action | Description | Requires Duration |
|--------|-------------|-------------------|
| `ban` | Permanent ban | No |
| `tempban` | Temporary ban | Yes |
| `banip` | Ban player's IP | No |
| `hwidban` | Hardware ID ban | No |
| `unban` | Remove ban | No |
| `unbanip` | Remove IP ban | No |
| `unbanhwid` | Remove HWID ban | No |
| `mute` | Permanent mute | No |
| `tempmute` | Temporary mute | Yes |
| `unmute` | Remove mute | No |
| `warn` | Issue warning | No |
| `kick` | Kick player | No |
| `freeze` | Freeze player | No |
| `unfreeze` | Unfreeze player | No |

**Success Response:**
```json
{
  "success": true,
  "message": "Command executed: /ban Notch Hacking",
  "action": "ban",
  "player": "Notch"
}
```

---

## Authentication

### API Keys

Generate secure API keys:
```bash
# Linux/Mac
openssl rand -hex 32

# Or use UUID
uuidgen
```

Add to config:
```yaml
web-panel:
  api-keys:
    - "a1b2c3d4e5f6..."
```

### Using the API

#### cURL Example
```bash
curl -H "Authorization: Bearer your-api-key" \
     http://localhost:8080/api/dashboard
```

#### JavaScript Example
```javascript
fetch('http://localhost:8080/api/dashboard', {
  headers: {
    'Authorization': 'Bearer your-api-key'
  }
})
.then(res => res.json())
.then(data => console.log(data));
```

#### Python Example
```python
import requests

response = requests.get(
    'http://localhost:8080/api/dashboard',
    headers={'Authorization': 'Bearer your-api-key'}
)
print(response.json())
```

---

## SSL Setup

### Step 1: Generate Keystore
```bash
keytool -genkeypair -alias litebans \
        -keyalg RSA -keysize 2048 \
        -validity 365 \
        -keystore keystore.jks \
        -storepass changeit
```

### Step 2: Copy to Plugin Folder
```bash
cp keystore.jks plugins/LiteBansReborn/
```

### Step 3: Configure
```yaml
web-panel:
  enabled: true
  port: 8443
  ssl: true
  keystore-password: "changeit"
```

### Step 4: Restart
```
/lbr reload
```

### Using Let's Encrypt

```bash
# Get certificate
certbot certonly --standalone -d your-domain.com

# Convert to PKCS12
openssl pkcs12 -export \
    -in /etc/letsencrypt/live/your-domain.com/fullchain.pem \
    -inkey /etc/letsencrypt/live/your-domain.com/privkey.pem \
    -out keystore.p12 \
    -name litebans

# Convert to JKS
keytool -importkeystore \
    -srckeystore keystore.p12 \
    -srcstoretype PKCS12 \
    -destkeystore keystore.jks \
    -deststoretype JKS
```

---

## Customization

### Custom Web Files

Place custom files in:
```
/plugins/LiteBansReborn/web/
├── index.html
├── style.css
├── script.js
└── assets/
```

### Modifying the Dashboard

Edit `web/index.html` to customize:
- Colors and branding
- Layout
- Additional features

### Adding Pages

Create new HTML files:
```
/plugins/LiteBansReborn/web/
├── index.html
├── players.html
├── bans.html
└── settings.html
```

---

## Security

### Best Practices

1. **Use strong API keys**
   ```bash
   openssl rand -hex 32
   ```

2. **Enable SSL in production**
   ```yaml
   ssl: true
   ```

3. **Restrict network access**
   - Use firewall rules
   - Only allow trusted IPs

4. **Rotate API keys regularly**
   - Change keys monthly
   - Remove unused keys

5. **Use reverse proxy**
   - Nginx or Apache
   - Additional security layer

### Nginx Reverse Proxy
```nginx
server {
    listen 443 ssl;
    server_name bans.yourserver.com;
    
    ssl_certificate /etc/ssl/certs/your-cert.pem;
    ssl_certificate_key /etc/ssl/private/your-key.pem;
    
    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### Firewall Rules

**UFW (Ubuntu):**
```bash
# Allow only specific IPs
ufw allow from 1.2.3.4 to any port 8080

# Or allow all (less secure)
ufw allow 8080
```

**iptables:**
```bash
iptables -A INPUT -p tcp --dport 8080 -s 1.2.3.4 -j ACCEPT
iptables -A INPUT -p tcp --dport 8080 -j DROP
```

---

## Troubleshooting

### Port Already in Use
```
Error: Address already in use
```

**Solution:**
- Change port in config
- Kill process using port:
  ```bash
  lsof -i :8080
  kill <PID>
  ```

### Cannot Access Panel
1. Check port is open in firewall
2. Check server IP is correct
3. Verify plugin is enabled
4. Check console for errors

### API Returns 401
- Verify API key is correct
- Check `Authorization` header format
- Ensure key is in config

### SSL Certificate Errors
- Verify keystore path
- Check password is correct
- Ensure certificate is valid
