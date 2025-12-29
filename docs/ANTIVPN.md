# 🌐 Anti-VPN Setup Guide

Complete guide to configuring the Anti-VPN system.

## Table of Contents
- [Overview](#overview)
- [Quick Setup](#quick-setup)
- [API Providers](#api-providers)
- [Configuration](#configuration)
- [Commands](#commands)
- [Whitelisting](#whitelisting)
- [Troubleshooting](#troubleshooting)

---

## Overview

LiteBansReborn's Anti-VPN system:
- Detects VPNs, proxies, and datacenter IPs
- Uses **6 different API providers**
- Features **automatic rotation** if one fails
- **Caches results** to reduce API calls
- Supports **real IP detection**

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
```
/lbr reload
```

That's it! The system will start protecting your server.

---

## API Providers

### Free Providers (No API Key Required)

| Provider | Daily Limit | Accuracy | Speed |
|----------|-------------|----------|-------|
| **IP-API.com** | Unlimited (45/min) | ⭐⭐⭐ | Fast |
| **ProxyCheck.io** | 100/day | ⭐⭐⭐⭐ | Medium |

### Free with API Key (Higher Limits)

| Provider | Free Limit | With Key | Accuracy |
|----------|------------|----------|----------|
| **ProxyCheck.io** | 100/day | 1000/day | ⭐⭐⭐⭐ |
| **VPNAPI.io** | 1000/day | 10000/day | ⭐⭐⭐⭐ |
| **IPHub.info** | 1000/day | 10000/day | ⭐⭐⭐⭐ |
| **IPHunter.info** | 1000/day | Unlimited | ⭐⭐⭐ |

### Premium Providers

| Provider | Price | Accuracy | Best For |
|----------|-------|----------|----------|
| **IPQualityScore** | $20/mo | ⭐⭐⭐⭐⭐ | Large servers |

---

## Configuration

### Full Configuration
```yaml
anti-vpn:
  # Enable the system
  enabled: true
  
  # Action when VPN detected
  # KICK - Kick the player
  # WARN - Warn staff only
  # ALLOW - Allow but log
  # NONE - Do nothing
  action: "KICK"
  
  # API providers (in priority order)
  providers:
    - proxycheck    # Best free option
    - ip-api        # Unlimited but rate limited
    - vpnapi        # Good accuracy
    - iphub         # Requires API key
    - iphunter      # Requires API key
    - ipqualityscore # Premium, best accuracy
  
  # API keys (optional but recommended)
  api-keys:
    proxycheck: ""
    vpnapi: ""
    iphub: ""
    iphunter: ""
    ipqualityscore: ""
  
  # Cache duration (minutes)
  cache-duration: 60
  
  # Send alerts to staff
  alerts: true
  
  # Whitelist
  whitelist:
    # Whitelisted IPs
    ips:
      - "127.0.0.1"
    
    # Whitelisted countries (ISO codes)
    countries:
      - "US"
      - "CA"
      - "GB"
```

### Getting API Keys

#### ProxyCheck.io
1. Go to https://proxycheck.io/dashboard
2. Sign up for free
3. Copy your API key

#### VPNAPI.io
1. Go to https://vpnapi.io
2. Register for free
3. Get API key from dashboard

#### IPHub.info
1. Go to https://iphub.info
2. Sign up
3. Copy API key

#### IPQualityScore
1. Go to https://ipqualityscore.com
2. Create account
3. Get API key

---

## Commands

### Check IP/Player
```bash
/vpncheck <player>           # Check online player
/vpncheck <ip>               # Check specific IP
/vpncheck 1.2.3.4            # Example
```

### View Statistics
```bash
/vpncheck stats              # Detection statistics
/vpncheck recent             # Recent detections
/vpncheck recent 20          # Last 20 detections
```

### Manage Whitelist
```bash
/vpncheck whitelist list             # View whitelist
/vpncheck whitelist add 1.2.3.4      # Add IP
/vpncheck whitelist remove 1.2.3.4   # Remove IP
```

### Player IP History
```bash
/vpncheck history <player>   # View IP history
/vpncheck realip <player>    # Get likely real IP
```

### Admin Controls
```bash
/lbr antivpn on              # Enable
/lbr antivpn off             # Disable
/lbr antivpn status          # View status
/lbr antivpn alerts on       # Enable alerts
/lbr antivpn alerts off      # Disable alerts
/lbr antivpn action KICK     # Change action
/lbr antivpn clearcache      # Clear cache
/lbr antivpn providers       # List providers
```

---

## Whitelisting

### Whitelist an IP
```bash
# Via command
/vpncheck whitelist add 1.2.3.4

# Via config
anti-vpn:
  whitelist:
    ips:
      - "1.2.3.4"
```

### Whitelist a Country
```yaml
anti-vpn:
  whitelist:
    countries:
      - "US"   # United States
      - "CA"   # Canada
      - "GB"   # United Kingdom
      - "DE"   # Germany
      - "MX"   # Mexico
```

### Whitelist by Permission
Players with this permission bypass VPN check:
```
litebansreborn.vpncheck.bypass
```

### Automatic Whitelist
These are always whitelisted:
- `127.0.0.1` (localhost)
- `192.168.x.x` (local network)
- `10.x.x.x` (local network)

---

## Detection Details

When a VPN is detected, the system provides:

| Field | Description |
|-------|-------------|
| **isVPN** | Is it a VPN? |
| **isProxy** | Is it a proxy? |
| **isHosting** | Is it a datacenter/hosting IP? |
| **isTor** | Is it a Tor exit node? |
| **vpnProvider** | VPN service name (if known) |
| **ISP** | Internet Service Provider |
| **Country** | Country code and name |
| **City** | City (if available) |
| **riskScore** | Risk score (0-100) |
| **realIP** | Detected real IP (if available) |

### Example Detection
```
VPN Check Result
----------------
Player: Notch
IP: 1.2.3.4

⚠ VPN/Proxy Detected!
Type: VPN

VPN: ✗ Yes
Proxy: ✓ No
Hosting: ✗ Yes
Tor: ✓ No

Provider: NordVPN
ISP: DataCenter Inc.
Country: Netherlands (NL)
Risk Score: 85%
API Provider: proxycheck.io
```

---

## Troubleshooting

### All Providers Failing

**Check internet connectivity:**
```bash
# From server
ping proxycheck.io
ping ip-api.com
```

**Check API limits:**
- You may have hit rate limits
- Wait a few minutes or add API keys

### False Positives

**Some ISPs get flagged:**
- Add their IP ranges to whitelist
- Or whitelist the country

**Mobile data detected as VPN:**
- Common with cellular carriers
- Consider using "WARN" instead of "KICK"

### Not Detecting VPNs

**Enable more providers:**
```yaml
providers:
  - proxycheck
  - ip-api
  - vpnapi    # Add more
  - iphub
```

**Add API keys:**
- Free tiers have limited detection
- Premium providers are more accurate

### Cache Issues

**Clear the cache:**
```bash
/lbr antivpn clearcache
```

**Reduce cache time:**
```yaml
cache-duration: 30  # 30 minutes
```

---

## Best Practices

1. **Use multiple providers** - Redundancy and accuracy
2. **Add API keys** - Higher limits and better detection
3. **Start with WARN** - Test before blocking
4. **Whitelist staff** - Give them bypass permission
5. **Monitor logs** - Check for false positives
6. **Use caching** - Don't waste API calls
7. **Whitelist trusted countries** - Reduce API usage

---

## Messages

Customize VPN-related messages:

```yaml
# messages.yml
vpn:
  kick: |
    &c&lVPN/Proxy Detected!
    
    &7Please disable your VPN/Proxy to join.
    &7If you believe this is an error, contact staff.
    
    &7Your IP: &f{ip}
    &7Detected: &f{type}
  
  staff-alert: "&c[VPN] &f{player} &7tried to join with a &c{type}&7! IP: &f{ip}"
```
