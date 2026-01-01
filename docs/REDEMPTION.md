# 🎮 Redemption System Guide

Complete guide to the punishment redemption system.

## Table of Contents
- [Overview](#overview)
- [Configuration](#configuration)
- [Minigames](#minigames)
- [Commands](#commands)
- [How It Works](#how-it-works)
- [Customization](#customization)
- [Examples](#examples)

---

## Overview

The Redemption System allows banned players to reduce their punishment duration by completing challenges. This provides a path for players to demonstrate genuine intention to follow rules.

### Benefits
- 🎯 Gives players a second chance
- 📚 Reinforces server rules
- ⏱️ Reduces moderation workload
- 🤝 Improves player relations

### How Reductions Work
Each challenge has a percentage reduction:
- **10% reduction**: Remove 10% of remaining time
- Example: 10-day ban → Complete math → 9-day ban

---

## Configuration

### Enable Redemption
```yaml
redemption:
  enabled: true
```

### Configure Minigames
```yaml
redemption:
  minigames:
    # Math captcha
    captcha-math:
      enabled: true
      reduction: 10       # 10% reduction
      max-attempts: 3     # 3 tries
      one-time: false     # Can repeat
    
    # Typing test
    typing-test:
      enabled: true
      reduction: 15       # 15% reduction
      max-attempts: 2     # 2 tries
      one-time: true      # Once per ban
    
    # Quiz about rules
    quiz:
      enabled: true
      reduction: 20       # 20% reduction
      max-attempts: 1     # 1 try
      one-time: true      # Once per ban
```

### Who Can Use Redemption
- Only banned players
- Only temporary bans (not permanent)
- Must be online (on ban screen or appeal server)

---

## Minigames

### 1. Math Captcha
Solve simple math problems to prove you're human and engaged.

**Example:**
```
✦ REDEMPTION CHALLENGE ✦
----------------------------------------
Solve this math problem:

     25 + 18 = ?

Type your answer in chat.
Attempts remaining: 3
----------------------------------------
```

**Configuration:**
```yaml
captcha-math:
  enabled: true
  reduction: 10
  max-attempts: 3
  difficulty: "medium"  # easy, medium, hard
```

**Difficulty Levels:**
| Level | Example | Numbers |
|-------|---------|---------|
| Easy | 5 + 3 | 1-20 |
| Medium | 25 + 18 | 10-50 |
| Hard | 47 × 8 | Multiplication |

### 2. Typing Test
Type a phrase exactly to demonstrate you read and understand rules.

**Example:**
```
✦ REDEMPTION CHALLENGE ✦
----------------------------------------
Type the following phrase exactly:

"I will follow the server rules"

Type your answer in chat.
Attempts remaining: 2
----------------------------------------
```

**Phrases:**
- "I will follow the server rules"
- "I understand my actions have consequences"
- "I promise to be respectful to others"
- "Fair play makes games fun for everyone"

### 3. Quiz
Answer questions about server rules.

**Example:**
```
✦ REDEMPTION CHALLENGE ✦
----------------------------------------
Answer this question:

What should you do if you see someone hacking?

Type your answer in chat.
Attempts remaining: 1
----------------------------------------
```

**Default Questions:**
| Question | Answer |
|----------|--------|
| What should you do if you see someone hacking? | report |
| Is griefing allowed on this server? | no |
| Should you share your account with others? | no |

---

## Commands

### Start a Challenge
```bash
/redemption start <type>
```

Types:
- `math` - Math captcha
- `typing` - Typing test
- `quiz` - Rules quiz

### Submit Answer
```bash
/redemption answer <your_answer>
```

### Check Status
```bash
/redemption status
```

### Cancel Challenge
```bash
/redemption cancel
```

---

## How It Works

### Flow
```
1. Banned player connects
2. Shown ban screen with redemption option
3. Player types /redemption start math
4. Challenge displayed
5. Player answers /redemption answer 42
6. If correct:
   - Punishment reduced by X%
   - Challenge marked complete (if one-time)
7. If wrong:
   - Attempts decrease
   - Can try again or fail
```

### Reduction Calculation
```
Original: 10 days
Reduction: 10%
New duration: 10 - (10 × 0.10) = 9 days
```

### Multiple Challenges
Players can complete multiple different challenges:
```
Original: 10 days
→ Complete math (10%): 9 days
→ Complete typing (15%): 7.65 days
→ Complete quiz (20%): 6.12 days

Final: ~6 days
```

---

## Customization

### Custom Phrases (Typing Test)
Edit in code or config:
```yaml
redemption:
  typing-test:
    phrases:
      - "I will respect all players"
      - "Cheating ruins the game for everyone"
      - "I accept the server rules"
```

### Custom Questions (Quiz)
```yaml
redemption:
  quiz:
    questions:
      - question: "What is our Discord server?"
        answer: "discord.gg/example"
      - question: "Who should you report hackers to?"
        answer: "staff"
```

### Cooldown Between Challenges
```yaml
redemption:
  cooldown: 60  # 60 seconds between challenges
```

### Maximum Reduction
```yaml
redemption:
  max-total-reduction: 50  # Max 50% off total
```

---

## Examples

### Basic Usage
```bash
# Player is banned for 7 days
# They want to reduce their ban

/redemption status
# Shows: No active challenge, 3 types available

/redemption start math
# Shows: 25 + 18 = ?

/redemption answer 43
# Shows: ✓ Correct! Your punishment has been reduced by 10%
# New duration: 6 days 7 hours
```

### Failed Attempt
```bash
/redemption start quiz
# Shows: Is griefing allowed?

/redemption answer yes
# Shows: ✗ Incorrect. 0 attempts remaining.
# Challenge failed. Cannot retry (one-time).
```

### Status Check
```bash
/redemption status
----------------------------------------
Redemption Status
----------------------------------------
No active challenge.

Available challenges:
  - math (10% reduction)
  - typing (15% reduction) ✓ COMPLETED
  - quiz (20% reduction) ✗ FAILED

Use /redemption start <type> to begin
----------------------------------------
```

---

## Messages

Customize in `messages.yml`:
```yaml
redemption:
  challenge-start: "&a&l✦ REDEMPTION CHALLENGE ✦"
  correct: "&a&l✓ {message}"
  incorrect: "&c&l✗ {message}"
  reduced: "&7Your punishment has been reduced by &a{percent}%"
  no-challenge: "&cNo active challenge. Use /redemption start"
  already-completed: "&cYou have already completed this challenge."
  expired: "&cChallenge expired. Try again."
```

---

## Best Practices

1. **Balance reductions** - Don't make them too high or punishments become meaningless
2. **Use one-time for harder challenges** - Quiz should be once per ban
3. **Allow repeating easy ones** - Math can be repeated for small reductions
4. **Set max reduction** - Cap at 50% so punishments still matter
5. **Monitor abuse** - Check if players are using bots to solve
6. **Inform players** - Make sure they know about redemption
