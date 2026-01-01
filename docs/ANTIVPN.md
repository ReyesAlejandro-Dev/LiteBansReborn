# 🌐 Anti-VPN Setup Guide

Complete guide to configuring the Anti-VPN system (v6.0.0+).

## Table of Contents
- [Overview & Architecture](#overview--architecture)
- [Quick Setup](#quick-setup)
- [API Providers](#api-providers)
- [Configuration](#configuration)
- [Commands](#commands)
- [Whitelisting](#whitelisting)
- [Troubleshooting](#troubleshooting)

---

## Overview & Architecture

LiteBansReborn v6.0.0 introduces **Anti-VPN 2.0**, a robust detection engine designed for performance.

### Key Features
- **Dedicated Thread Pool**: Anti-VPN checks run on isolated threads, ensuring they never impact server TPS or database performance.
- **Circuit Breaker**: Automatically disables failing API providers for 60 seconds to prevent lag.
- **Failover System**: Rotates through 6 providers intelligently if one is down or out of credits.
- **SQLite Synchronization**: v6.0.0 introduces thread-safe SQLite storage for VPN cache, preventing "Database Locked" errors.
- **Real IP Detection**: Advanced heuristics to find the real IP behind some proxies.

---

## Quick Setup

### Step 1: Enable Anti-VPN
```yaml
anti-vpn:
  enabled: true
```

### Step 2: Set Action
```yaml
anti-vpn:
  action: "KICK"  # KICK, WARN, ALLOW, or NONE
```

### Step 3: Restart
```bash
/lbr reload
```

---

## API Providers

### Supported Providers
LiteBansReborn supports the following providers out of the box. Order matters in `config.yml`.

| Provider | Needs Key? | Limits | Accuracy | Speed |
|----------|------------|--------|----------|-------|
| **ProxyCheck.io** | Optional | 100/day (free), 1000/day (key) | ⭐⭐⭐⭐ | Fast |
| **IP-API.com** | No | 45 req/min | ⭐⭐⭐ | Very Fast |
| **VPNAPI.io** | Yes (Free) | 1,000/day | ⭐⭐⭐⭐ | Fast |
| **IPHub.info** | Yes (Free) | 1,000/day | ⭐⭐⭐⭐ | Medium |
| **IPHunter.info** | Yes (Free) | 1,000/day | ⭐⭐⭐ | Medium |
| **IPQualityScore** | Yes (Free) | 5,000/month | ⭐⭐⭐⭐⭐ | Slow |

> **Pro Tip:** Use `ProxyCheck` and `IP-API` first as they are the fastest. Put `IPQualityScore` last for tough cases.

---

## Configuration

### Full Configuration
```yaml
anti-vpn:
  enabled: true
  action: "KICK"
  
  # Priority list (top to bottom)
  providers:
    - proxycheck
    - ip-api
    - vpnapi
  
  api-keys:
    proxycheck: "YOUR_KEY"
    vpnapi: "YOUR_KEY"
    ipqualityscore: "YOUR_KEY"
  
  # Cache results to save API calls (minutes)
  cache-duration: 60
  
  # Notify staff on join?
  alerts: true
  
  # Exceptions
  whitelist:
    ips:
      - "127.0.0.1"
      - "10.0.0.1"
    countries:
      - "US"
      - "DE"
      - "GB"
```

---

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/vpncheck <player/ip>` | Check if a player/IP is using a VPN | `litebansreborn.vpncheck` |
| `/vpncheck stats` | View detection statistics | `litebansreborn.admin` |
| `/vpncheck whitelist list` | View whitelisted IPs | `litebansreborn.admin` |
| `/vpncheck whitelist add <ip>` | Whitelist an IP | `litebansreborn.admin` |
| `/lbr antivpn on/off` | Toggle system at runtime | `litebansreborn.admin` |
| `/lbr antivpn clearcache` | Clear VPN cache DB | `litebansreborn.admin` |

---

## Troubleshooting

### "Database Locked" Errors
**Solution:** Upgrade to v6.0.0+. We implemented strict synchronization for SQLite access, fixing this issue permanently.

### "API Limit Reached"
**Solution:**
1. Register for free API keys (ProxyCheck, VPNAPI).
2. Add them to `config.yml`.
3. Enable more providers in the list for better rotation.

### Detecting Legitimate Players (False Positives)
Some mobile networks (4G/5G) share IPs that look like proxies.
**Solution:**
1. Whitelist the player's Country.
2. Or use `action: WARN` instead of KICK for a week to monitor.
3. Whitelist specific IPs using `/vpncheck whitelist add <ip>`.

### "Connection Refused" (API)
The circuit breaker will automatically skip this provider for 60 seconds. You don't need to do anything. If it persists, check your server's firewall/DNS.

---

## Performance Note

Anti-VPN checks are **fully asynchronous**. Even if all APIs time out, your main server thread will NEVER freeze. The join event handles the kick safely on the main thread after the check completes.
