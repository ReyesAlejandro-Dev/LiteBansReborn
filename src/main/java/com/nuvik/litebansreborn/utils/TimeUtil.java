package com.nuvik.litebansreborn.utils;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for time parsing and formatting
 */
public class TimeUtil {
    
    private static final Pattern DURATION_PATTERN = Pattern.compile(
        "(?:(\\d+)y)?\\s*(?:(\\d+)mo)?\\s*(?:(\\d+)w)?\\s*(?:(\\d+)d)?\\s*(?:(\\d+)h)?\\s*(?:(\\d+)m)?\\s*(?:(\\d+)s)?",
        Pattern.CASE_INSENSITIVE
    );
    
    private static DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(ZoneId.systemDefault());
    
    /**
     * Parse a duration string (e.g., "1d2h30m", "7d", "1y")
     * @param input The duration string
     * @return Duration in milliseconds, or -1 if permanent, 0 if invalid
     */
    public static long parseDuration(String input) {
        if (input == null || input.isEmpty()) {
            return 0;
        }
        
        // Check for permanent indicators
        String lower = input.toLowerCase().trim();
        if (lower.equals("permanent") || lower.equals("perm") || lower.equals("forever") || lower.equals("-1")) {
            return -1;
        }
        
        Matcher matcher = DURATION_PATTERN.matcher(input);
        if (!matcher.find()) {
            return 0;
        }
        
        long total = 0;
        
        // Years
        if (matcher.group(1) != null) {
            total += Long.parseLong(matcher.group(1)) * 365L * 24L * 60L * 60L * 1000L;
        }
        // Months (approximate as 30 days)
        if (matcher.group(2) != null) {
            total += Long.parseLong(matcher.group(2)) * 30L * 24L * 60L * 60L * 1000L;
        }
        // Weeks
        if (matcher.group(3) != null) {
            total += Long.parseLong(matcher.group(3)) * 7L * 24L * 60L * 60L * 1000L;
        }
        // Days
        if (matcher.group(4) != null) {
            total += Long.parseLong(matcher.group(4)) * 24L * 60L * 60L * 1000L;
        }
        // Hours
        if (matcher.group(5) != null) {
            total += Long.parseLong(matcher.group(5)) * 60L * 60L * 1000L;
        }
        // Minutes
        if (matcher.group(6) != null) {
            total += Long.parseLong(matcher.group(6)) * 60L * 1000L;
        }
        // Seconds
        if (matcher.group(7) != null) {
            total += Long.parseLong(matcher.group(7)) * 1000L;
        }
        
        return total;
    }
    
    /**
     * Format a duration in milliseconds to a human-readable string
     */
    public static String formatDuration(long millis) {
        if (millis < 0) {
            return "Permanent";
        }
        
        if (millis == 0) {
            return "0 seconds";
        }
        
        long days = TimeUnit.MILLISECONDS.toDays(millis);
        millis -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
        
        StringBuilder sb = new StringBuilder();
        
        if (days > 0) {
            sb.append(days).append(days == 1 ? " day" : " days");
        }
        if (hours > 0) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(hours).append(hours == 1 ? " hour" : " hours");
        }
        if (minutes > 0) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(minutes).append(minutes == 1 ? " minute" : " minutes");
        }
        if (seconds > 0 && days == 0) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(seconds).append(seconds == 1 ? " second" : " seconds");
        }
        
        return sb.length() > 0 ? sb.toString() : "0 seconds";
    }
    
    /**
     * Format a duration in short form (e.g., "1d 2h 30m")
     */
    public static String formatDurationShort(long millis) {
        if (millis < 0) {
            return "Permanent";
        }
        
        if (millis == 0) {
            return "0s";
        }
        
        long days = TimeUnit.MILLISECONDS.toDays(millis);
        millis -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
        
        StringBuilder sb = new StringBuilder();
        
        if (days > 0) {
            sb.append(days).append("d ");
        }
        if (hours > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0) {
            sb.append(minutes).append("m ");
        }
        if (seconds > 0 && days == 0 && hours == 0) {
            sb.append(seconds).append("s");
        }
        
        return sb.toString().trim();
    }
    
    /**
     * Format an Instant to a date string
     */
    public static String formatDate(Instant instant) {
        if (instant == null) {
            return "Never";
        }
        return dateFormatter.format(instant);
    }
    
    /**
     * Set the date format pattern
     */
    public static void setDateFormat(String pattern, ZoneId zoneId) {
        dateFormatter = DateTimeFormatter.ofPattern(pattern).withZone(zoneId);
    }
    
    /**
     * Get the time until an Instant
     */
    public static String getTimeUntil(Instant instant) {
        if (instant == null) {
            return "Never";
        }
        
        Duration duration = Duration.between(Instant.now(), instant);
        if (duration.isNegative()) {
            return "Expired";
        }
        
        return formatDuration(duration.toMillis());
    }
    
    /**
     * Get the time since an Instant
     */
    public static String getTimeSince(Instant instant) {
        if (instant == null) {
            return "Never";
        }
        
        Duration duration = Duration.between(instant, Instant.now());
        if (duration.isNegative()) {
            return "In the future";
        }
        
        return formatDuration(duration.toMillis()) + " ago";
    }
    
    /**
     * Calculate expiry time from now
     */
    public static Instant calculateExpiry(long durationMillis) {
        if (durationMillis < 0) {
            return null; // Permanent
        }
        return Instant.now().plusMillis(durationMillis);
    }
    
    /**
     * Check if a duration string is valid
     */
    public static boolean isValidDuration(String input) {
        return parseDuration(input) != 0;
    }
    
    /**
     * Format remaining time as countdown (e.g., "23:59:59")
     */
    public static String formatCountdown(long millis) {
        if (millis < 0) {
            return "∞";
        }
        
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
        
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }
    
    /**
     * Format an Instant to ISO 8601 format for Discord/API
     */
    public static String formatISO(Instant instant) {
        if (instant == null) {
            return null;
        }
        return DateTimeFormatter.ISO_INSTANT.format(instant);
    }
}
