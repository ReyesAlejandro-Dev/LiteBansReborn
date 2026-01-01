# 🔮 Predictive Moderation Guide

LiteBansReborn v5.1 introduces **Predictive Moderation** - an intelligent system that analyzes player behavior and predicts who is likely to get banned.

## Features

### 📊 Risk Scoring
Every player gets a **Risk Score** (0-100) based on:
- Warning history
- Mute history
- Previous bans
- Chat toxicity ratio
- Playtime (veterans get bonus trust)
- Connections to banned players

### 🔮 Ban Prediction
The system predicts the probability of a player being banned in the next 7 days.

### 📈 Behavior Learning
When you ban a player, the system learns their behavior patterns to improve future predictions.

## Commands

| Command | Description | Permission |
|---|---|---|
| `/risk check <player>` | View player's risk profile | `litebansreborn.risk` |
| `/risk analyze <player>` | Force re-analyze a player | `litebansreborn.risk` |
| `/risk top` | Show highest-risk online players | `litebansreborn.risk` |

## Risk Levels

| Score | Level | Color | Description |
|---|---|---|---|
| 0-29 | MINIMAL | Green | Trusted player |
| 30-49 | LOW | Light Green | Normal player |
| 50-69 | MEDIUM | Yellow | Worth monitoring |
| 70-89 | HIGH | Red | Likely to cause problems |
| 90-100 | CRITICAL | Dark Red | Almost certain to be banned |

## Configuration

```yaml
predictive:
  enabled: true
  alert-high-risk: true
  alert-threshold: 70
  learn-from-bans: true
```

## Scoring Factors

| Factor | Impact |
|---|---|
| Warnings per hour | +20 per warning/hour |
| Each mute | +10 |
| Each previous ban | +25 |
| Toxic message ratio | +30 max |
| Banned associates | +20 max |
| 100+ hours playtime | -15 (trust bonus) |
| New player (<1 hour) | +10 (caution) |

## Staff Alerts

Staff with `litebansreborn.alerts.predictive` permission see:
- When high-risk players join
- When a player's risk score increases significantly
- Prediction warnings for problematic players

## Use Cases

1. **Prioritize Moderation**: Focus on high-risk players during busy times
2. **Preventive Action**: Talk to medium-risk players before they escalate
3. **Staff Allocation**: See the `/risk top` to know who needs watching
4. **Pattern Recognition**: Identify behavior patterns that lead to bans
