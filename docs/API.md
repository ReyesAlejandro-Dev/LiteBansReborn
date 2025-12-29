# 🔌 Developer API

Complete API documentation for developers integrating with LiteBansReborn.

## Table of Contents
- [Getting Started](#getting-started)
- [Maven Dependency](#maven-dependency)
- [Basic Usage](#basic-usage)
- [Ban API](#ban-api)
- [Mute API](#mute-api)
- [Warn API](#warn-api)
- [History API](#history-api)
- [Events](#events)
- [Placeholders](#placeholders)
- [Examples](#examples)

---

## Getting Started

### Accessing the API

```java
import com.nuvik.litebansreborn.api.LiteBansRebornAPI;
import com.nuvik.litebansreborn.LiteBansReborn;

// Get API instance
LiteBansRebornAPI api = LiteBansReborn.getAPI();
```

---

## Maven Dependency

### Repository
```xml
<repository>
    <id>nuvik-repo</id>
    <url>https://repo.nuvik.com/releases</url>
</repository>
```

### Dependency
```xml
<dependency>
    <groupId>com.nuvik</groupId>
    <artifactId>LiteBansReborn</artifactId>
    <version>4.0.0</version>
    <scope>provided</scope>
</dependency>
```

### Gradle
```groovy
repositories {
    maven { url 'https://repo.nuvik.com/releases' }
}

dependencies {
    compileOnly 'com.nuvik:LiteBansReborn:4.0.0'
}
```

---

## Basic Usage

### Check if Player is Banned
```java
LiteBansRebornAPI api = LiteBansReborn.getAPI();

// Sync check (cached)
boolean isBanned = api.isBanned(playerUUID);

// Async check (database)
api.isBannedAsync(playerUUID).thenAccept(banned -> {
    if (banned) {
        // Player is banned
    }
});
```

### Check if Player is Muted
```java
boolean isMuted = api.isMuted(playerUUID);

api.isMutedAsync(playerUUID).thenAccept(muted -> {
    if (muted) {
        // Player is muted
    }
});
```

---

## Ban API

### Ban a Player
```java
import com.nuvik.litebansreborn.models.Punishment;

// Permanent ban
api.banPlayer(targetUUID, "Hacking", null, staffUUID);

// Temporary ban (7 days)
api.banPlayer(targetUUID, "X-Ray", "7d", staffUUID);

// With callback
api.banPlayerAsync(targetUUID, "Hacking", "7d", staffUUID)
    .thenAccept(punishment -> {
        System.out.println("Ban ID: " + punishment.getId());
    });
```

### Unban a Player
```java
api.unbanPlayer(targetUUID, staffUUID);

// Async
api.unbanPlayerAsync(targetUUID, staffUUID).thenAccept(success -> {
    if (success) {
        // Player unbanned
    }
});
```

### Get Active Ban
```java
api.getActiveBan(playerUUID).thenAccept(ban -> {
    if (ban != null) {
        System.out.println("Reason: " + ban.getReason());
        System.out.println("Expires: " + ban.getExpiresAt());
        System.out.println("Staff: " + ban.getOperatorName());
    }
});
```

### IP Ban
```java
api.ipBanPlayer(targetUUID, "Ban evasion", "30d", staffUUID);
```

---

## Mute API

### Mute a Player
```java
// Permanent mute
api.mutePlayer(targetUUID, "Spam", null, staffUUID);

// Temporary mute (1 hour)
api.mutePlayer(targetUUID, "Spam", "1h", staffUUID);
```

### Unmute a Player
```java
api.unmutePlayer(targetUUID, staffUUID);
```

### Get Active Mute
```java
api.getActiveMute(playerUUID).thenAccept(mute -> {
    if (mute != null) {
        System.out.println("Remaining: " + mute.getRemainingTime());
    }
});
```

---

## Warn API

### Warn a Player
```java
api.warnPlayer(targetUUID, "Minor rule violation", staffUUID);
```

### Get Warnings
```java
api.getWarnings(playerUUID).thenAccept(warnings -> {
    for (Punishment warning : warnings) {
        System.out.println(warning.getReason());
    }
});
```

### Remove Warning
```java
api.removeWarning(warningId, staffUUID);
```

---

## History API

### Get Punishment History
```java
api.getHistory(playerUUID).thenAccept(history -> {
    for (Punishment p : history) {
        System.out.println(p.getType() + ": " + p.getReason());
    }
});
```

### Get History by Type
```java
import com.nuvik.litebansreborn.models.PunishmentType;

api.getHistoryByType(playerUUID, PunishmentType.BAN)
    .thenAccept(bans -> {
        System.out.println("Total bans: " + bans.size());
    });
```

### Get Staff History
```java
api.getStaffHistory(staffUUID).thenAccept(punishments -> {
    System.out.println("Punishments issued: " + punishments.size());
});
```

---

## Events

### Available Events

| Event | Description |
|-------|-------------|
| `PlayerBanEvent` | Fired when a player is banned |
| `PlayerUnbanEvent` | Fired when a player is unbanned |
| `PlayerMuteEvent` | Fired when a player is muted |
| `PlayerUnmuteEvent` | Fired when a player is unmuted |
| `PlayerKickEvent` | Fired when a player is kicked |
| `PlayerWarnEvent` | Fired when a player is warned |
| `PlayerFreezeEvent` | Fired when a player is frozen |
| `VPNDetectionEvent` | Fired when VPN is detected |

### Listening to Events

```java
import com.nuvik.litebansreborn.events.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class MyListener implements Listener {
    
    @EventHandler
    public void onBan(PlayerBanEvent event) {
        UUID target = event.getTargetUUID();
        String reason = event.getReason();
        UUID staff = event.getOperatorUUID();
        
        // Custom logic
        System.out.println(target + " was banned for: " + reason);
        
        // Cancel the ban
        if (someCondition) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onVPNDetection(VPNDetectionEvent event) {
        Player player = event.getPlayer();
        VPNResult result = event.getResult();
        
        if (result.getCountryCode().equals("US")) {
            event.setCancelled(true); // Allow US VPNs
        }
    }
}
```

### Event Priority

```java
@EventHandler(priority = EventPriority.HIGH)
public void onBan(PlayerBanEvent event) {
    // Runs after NORMAL priority listeners
}
```

---

## Placeholders

### PlaceholderAPI Placeholders

| Placeholder | Description |
|-------------|-------------|
| `%litebans_bans%` | Total bans |
| `%litebans_mutes%` | Total mutes |
| `%litebans_warnings%` | Player's warnings |
| `%litebans_isbanned%` | Is player banned? |
| `%litebans_ismuted%` | Is player muted? |
| `%litebans_banreason%` | Ban reason |
| `%litebans_banexpiry%` | Ban expiry |
| `%litebans_banoperator%` | Who banned |
| `%litebans_mutereason%` | Mute reason |
| `%litebans_muteexpiry%` | Mute expiry |

### Using in Your Plugin

```java
import me.clip.placeholderapi.PlaceholderAPI;

String parsed = PlaceholderAPI.setPlaceholders(player, "%litebans_bans%");
```

---

## Examples

### Custom Ban Command
```java
public class CustomBanCommand implements CommandExecutor {
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length < 2) {
            return false;
        }
        
        String targetName = args[0];
        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            sender.sendMessage("Player not found!");
            return true;
        }
        
        LiteBansRebornAPI api = LiteBansReborn.getAPI();
        
        UUID staffUUID = sender instanceof Player ? ((Player) sender).getUniqueId() : null;
        
        api.banPlayerAsync(target.getUniqueId(), reason, "7d", staffUUID)
            .thenAccept(ban -> {
                sender.sendMessage("Banned " + targetName + " for 7 days!");
            });
        
        return true;
    }
}
```

### Discord Integration
```java
@EventHandler
public void onBan(PlayerBanEvent event) {
    // Send to Discord webhook
    String json = String.format(
        "{\"content\": \"**%s** was banned for: %s\"}",
        event.getTargetName(),
        event.getReason()
    );
    
    // Send HTTP request to Discord webhook
    sendWebhook(WEBHOOK_URL, json);
}
```

### Warning Escalation
```java
@EventHandler
public void onWarn(PlayerWarnEvent event) {
    LiteBansRebornAPI api = LiteBansReborn.getAPI();
    
    api.getWarnings(event.getTargetUUID()).thenAccept(warnings -> {
        int count = warnings.size();
        
        if (count >= 5) {
            // Auto-ban after 5 warnings
            api.banPlayer(
                event.getTargetUUID(),
                "Exceeded warning limit",
                "1d",
                null
            );
        } else if (count >= 3) {
            // Auto-mute after 3 warnings
            api.mutePlayer(
                event.getTargetUUID(),
                "3 warnings received",
                "1h",
                null
            );
        }
    });
}
```

### Anti-VPN Integration
```java
@EventHandler
public void onVPNDetection(VPNDetectionEvent event) {
    VPNResult result = event.getResult();
    
    // Log to database
    logVPNDetection(
        event.getPlayer().getUniqueId(),
        result.getIp(),
        result.getVpnProvider(),
        result.getCountry()
    );
    
    // Custom action based on provider
    if ("NordVPN".equals(result.getVpnProvider())) {
        event.setCancelled(true); // Allow NordVPN
    }
}
```

---

## Best Practices

1. **Use async methods** - Don't block the main thread
2. **Cache results** - Avoid excessive API calls
3. **Handle null** - Punishments may not exist
4. **Check enabled** - Verify plugin is loaded
5. **Use events** - React to punishments properly

### Thread Safety
```java
// Always use async for database operations
api.getHistoryAsync(uuid).thenAccept(history -> {
    // Process on async thread
    
    // If you need to run on main thread:
    Bukkit.getScheduler().runTask(plugin, () -> {
        // Main thread code
    });
});
```

### Null Safety
```java
api.getActiveBan(uuid).thenAccept(ban -> {
    if (ban != null) {
        // Player is banned
    } else {
        // Player is not banned
    }
});
```
