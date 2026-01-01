# 📋 Changelog

All notable changes to LiteBansReborn.

## [6.0.0] - 2026-01-01

### 🎉 Stability & Compatibility Overhaul
This major release is focused on making LiteBansReborn rock-solid for production environments, with a complete rewrite of threading models and database interactions.

### Added
- **Cross-Database Compatibility**: Native support for MySQL, MariaDB, PostgreSQL, SQLite, and H2 with dialect-aware queries (UPSERTs).
- **Thread Safety 2.0**: rewritten async task handling logic.
  - All Bukkit API calls in async callbacks are now correctly scheduled to the main thread.
  - Replaced unsafe `SimpleDateFormat` with thread-safe `DateTimeFormatter`.
  - Removed blocking `future.get()` calls from the main thread.
- **Leak-Free Connection Pooling**: Implemented `try-with-resources` across all 40+ database interaction points to guarantee connection closure.
- **SQLite Synchronization**: Added mutex locks to SQLite transactions to prevent database corruption under load.

### Fixed
- **AntiCheat**: Fixed logic error where auto-mute threshold would warn instead of mute.
- **GeoIP**: Corrected country name vs country code mapping for ipinfo.io provider.
- **GhostMute**: Fixed crash when player messages contained `%` characters.
- **Web Panel**: 
  - Patched potential SQL injection vectors by whitelisting inputs.
  - Fixed command routing for `ipban` actions.
- **Commands**: Fixed `Integer.parseInt` crashes in pagination commands (MuteList, etc).
- **Point System**: Replaced MySQL-specific `GREATEST()` function with cross-db `CASE WHEN` logic.
- **Build System**: Fixed UTF-8 encoding issues that caused compilation errors on some systems.

### Changed
- **Database**: All managers now strictly adhere to the `table-prefix` setting from config.
- **Performance**: Optimized `SocialNetworkManager` queries to use efficient upserts.

---

## [5.9.0] - 2025-12-31

### Added
- **GeoIP Map**: Interactive world map in Web Panel showing player locations.
- **Anti-VPN 2.0**: Dedicated thread pool and circuit breaker pattern.
- **HWID 2.0**: JSON storage and debounced saves for performance.

### Fixed
- **Startup**: Fixed race condition in manager initialization.
- **shutdown**: Proper cancellation of async tasks on disable.

---

## [5.8.0] - 2025-12-30

### Added
- **Web Panel Redesign**: Complete overhaul with glassmorphism UI.
- **One-Click Moderation**: Instant actions from the web dashboard.
- **Public IP Auto-Detection**: Panel now shows the correct public URL.

### Fixed
- **Player Lookup**: Case-insensitive search fixes.
- **HikariCP**: Tuning to prevent thread starvation.

---
... old versions ...
