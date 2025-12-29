package com.nuvik.litebansreborn.utils;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for color code handling
 */
public class ColorUtil {
    
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern GRADIENT_PATTERN = Pattern.compile("<gradient:([A-Fa-f0-9]{6}):([A-Fa-f0-9]{6})>(.*?)</gradient>");
    private static final Pattern RAINBOW_PATTERN = Pattern.compile("<rainbow>(.*?)</rainbow>");
    
    /**
     * Translate color codes in a string
     * Supports: & codes, &#RRGGBB hex codes, <gradient:from:to>text</gradient>, <rainbow>text</rainbow>
     */
    public static String translate(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        
        // Apply gradient
        message = applyGradient(message);
        
        // Apply rainbow
        message = applyRainbow(message);
        
        // Apply hex colors
        message = applyHexColors(message);
        
        // Apply legacy color codes
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    /**
     * Translate multiple lines
     */
    public static List<String> translate(List<String> messages) {
        List<String> translated = new ArrayList<>();
        for (String message : messages) {
            translated.add(translate(message));
        }
        return translated;
    }
    
    /**
     * Strip all color codes from a string
     */
    public static String stripColor(String message) {
        if (message == null) {
            return null;
        }
        // Strip hex colors
        message = HEX_PATTERN.matcher(message).replaceAll("");
        // Strip legacy codes
        return ChatColor.stripColor(translate(message));
    }
    
    /**
     * Apply hex colors (&#RRGGBB format)
     */
    private static String applyHexColors(String message) {
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();
        
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                replacement.append("§").append(c);
            }
            matcher.appendReplacement(buffer, replacement.toString());
        }
        matcher.appendTail(buffer);
        
        return buffer.toString();
    }
    
    /**
     * Apply gradient colors
     */
    private static String applyGradient(String message) {
        Matcher matcher = GRADIENT_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();
        
        while (matcher.find()) {
            String fromHex = matcher.group(1);
            String toHex = matcher.group(2);
            String content = matcher.group(3);
            
            String gradientText = applyGradientToText(content, fromHex, toHex);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(gradientText));
        }
        matcher.appendTail(buffer);
        
        return buffer.toString();
    }
    
    /**
     * Apply gradient to text
     */
    private static String applyGradientToText(String text, String fromHex, String toHex) {
        // Parse colors
        int[] fromRGB = hexToRGB(fromHex);
        int[] toRGB = hexToRGB(toHex);
        
        StringBuilder result = new StringBuilder();
        int length = text.length();
        
        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);
            if (c == ' ') {
                result.append(c);
                continue;
            }
            
            // Calculate interpolated color
            float ratio = length > 1 ? (float) i / (length - 1) : 0;
            int r = (int) (fromRGB[0] + ratio * (toRGB[0] - fromRGB[0]));
            int g = (int) (fromRGB[1] + ratio * (toRGB[1] - fromRGB[1]));
            int b = (int) (fromRGB[2] + ratio * (toRGB[2] - fromRGB[2]));
            
            result.append(rgbToMinecraftHex(r, g, b)).append(c);
        }
        
        return result.toString();
    }
    
    /**
     * Apply rainbow colors
     */
    private static String applyRainbow(String message) {
        Matcher matcher = RAINBOW_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();
        
        while (matcher.find()) {
            String content = matcher.group(1);
            String rainbowText = applyRainbowToText(content);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(rainbowText));
        }
        matcher.appendTail(buffer);
        
        return buffer.toString();
    }
    
    /**
     * Apply rainbow to text
     */
    private static String applyRainbowToText(String text) {
        StringBuilder result = new StringBuilder();
        int length = text.length();
        
        // Rainbow colors
        int[][] rainbowColors = {
            {255, 0, 0},    // Red
            {255, 127, 0},  // Orange
            {255, 255, 0},  // Yellow
            {0, 255, 0},    // Green
            {0, 0, 255},    // Blue
            {75, 0, 130},   // Indigo
            {148, 0, 211}   // Violet
        };
        
        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);
            if (c == ' ') {
                result.append(c);
                continue;
            }
            
            // Get rainbow color
            float position = (float) i / length * (rainbowColors.length - 1);
            int colorIndex = (int) position;
            float ratio = position - colorIndex;
            
            int[] fromColor = rainbowColors[colorIndex];
            int[] toColor = rainbowColors[Math.min(colorIndex + 1, rainbowColors.length - 1)];
            
            int r = (int) (fromColor[0] + ratio * (toColor[0] - fromColor[0]));
            int g = (int) (fromColor[1] + ratio * (toColor[1] - fromColor[1]));
            int b = (int) (fromColor[2] + ratio * (toColor[2] - fromColor[2]));
            
            result.append(rgbToMinecraftHex(r, g, b)).append(c);
        }
        
        return result.toString();
    }
    
    /**
     * Convert hex string to RGB array
     */
    private static int[] hexToRGB(String hex) {
        return new int[] {
            Integer.parseInt(hex.substring(0, 2), 16),
            Integer.parseInt(hex.substring(2, 4), 16),
            Integer.parseInt(hex.substring(4, 6), 16)
        };
    }
    
    /**
     * Convert RGB to Minecraft hex format
     */
    private static String rgbToMinecraftHex(int r, int g, int b) {
        String hex = String.format("%02x%02x%02x", r, g, b);
        StringBuilder result = new StringBuilder("§x");
        for (char c : hex.toCharArray()) {
            result.append("§").append(c);
        }
        return result.toString();
    }
    
    /**
     * Get a color based on percentage (green -> yellow -> red)
     */
    public static String getProgressColor(double percentage) {
        if (percentage >= 75) {
            return "§a"; // Green
        } else if (percentage >= 50) {
            return "§e"; // Yellow
        } else if (percentage >= 25) {
            return "§6"; // Gold
        } else {
            return "§c"; // Red
        }
    }
    
    /**
     * Create a progress bar
     */
    public static String progressBar(double current, double max, int length, String filled, String empty) {
        double percentage = Math.min(1.0, current / max);
        int filledLength = (int) (length * percentage);
        int emptyLength = length - filledLength;
        
        StringBuilder bar = new StringBuilder();
        bar.append(getProgressColor(percentage * 100));
        for (int i = 0; i < filledLength; i++) {
            bar.append(filled);
        }
        bar.append("§7");
        for (int i = 0; i < emptyLength; i++) {
            bar.append(empty);
        }
        
        return bar.toString();
    }
}
