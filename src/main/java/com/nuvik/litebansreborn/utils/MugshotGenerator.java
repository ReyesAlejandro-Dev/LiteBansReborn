package com.nuvik.litebansreborn.utils;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.models.Punishment;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

/**
 * Mugshot Generator - Creates "WANTED" style posters for banned players
 */
public class MugshotGenerator {
    
    private static final int WIDTH = 400;
    private static final int HEIGHT = 500;
    private static final String AVATAR_API = "https://minotar.net/helm/";
    
    private final LiteBansReborn plugin;
    private final File outputDir;
    
    // Colors
    private static final Color BACKGROUND = new Color(45, 45, 45);
    private static final Color WANTED_RED = new Color(180, 30, 30);
    private static final Color GOLD = new Color(255, 215, 0);
    private static final Color TEXT_WHITE = new Color(255, 255, 255);
    private static final Color TEXT_GRAY = new Color(180, 180, 180);
    
    public MugshotGenerator(LiteBansReborn plugin) {
        this.plugin = plugin;
        this.outputDir = new File(plugin.getDataFolder(), "mugshots");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
    }
    
    /**
     * Generate a WANTED poster for a banned player
     */
    public CompletableFuture<String> generateMugshot(Punishment punishment) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                BufferedImage poster = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = poster.createGraphics();
                
                // Enable anti-aliasing
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                
                // Draw background with rounded corners
                g2d.setColor(BACKGROUND);
                g2d.fill(new RoundRectangle2D.Float(0, 0, WIDTH, HEIGHT, 20, 20));
                
                // Draw border
                g2d.setColor(WANTED_RED);
                g2d.setStroke(new BasicStroke(8));
                g2d.draw(new RoundRectangle2D.Float(4, 4, WIDTH - 8, HEIGHT - 8, 16, 16));
                
                // Draw "WANTED" text
                Font wantedFont = new Font("Impact", Font.BOLD, 60);
                g2d.setFont(wantedFont);
                g2d.setColor(WANTED_RED);
                String wantedText = "WANTED";
                FontMetrics fm = g2d.getFontMetrics();
                int wantedX = (WIDTH - fm.stringWidth(wantedText)) / 2;
                g2d.drawString(wantedText, wantedX, 65);
                
                // Draw player avatar
                BufferedImage avatar = getPlayerAvatar(punishment.getTargetName(), 128);
                if (avatar != null) {
                    // Draw avatar with border
                    int avatarX = (WIDTH - 128) / 2;
                    int avatarY = 90;
                    
                    // Shadow
                    g2d.setColor(new Color(0, 0, 0, 100));
                    g2d.fillRoundRect(avatarX + 5, avatarY + 5, 128, 128, 10, 10);
                    
                    // Border
                    g2d.setColor(GOLD);
                    g2d.fillRoundRect(avatarX - 4, avatarY - 4, 136, 136, 10, 10);
                    
                    g2d.drawImage(avatar, avatarX, avatarY, 128, 128, null);
                }
                
                // Player name
                Font nameFont = new Font("Arial", Font.BOLD, 28);
                g2d.setFont(nameFont);
                g2d.setColor(TEXT_WHITE);
                String playerName = punishment.getTargetName();
                fm = g2d.getFontMetrics();
                int nameX = (WIDTH - fm.stringWidth(playerName)) / 2;
                g2d.drawString(playerName, nameX, 255);
                
                // Divider line
                g2d.setColor(WANTED_RED);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawLine(40, 275, WIDTH - 40, 275);
                
                // Reason
                Font infoFont = new Font("Arial", Font.PLAIN, 16);
                g2d.setFont(infoFont);
                g2d.setColor(TEXT_GRAY);
                String reason = "REASON:";
                g2d.drawString(reason, 40, 305);
                
                g2d.setColor(TEXT_WHITE);
                String reasonText = truncateText(punishment.getReason(), 35);
                g2d.drawString(reasonText, 40, 325);
                
                // Banned by
                g2d.setColor(TEXT_GRAY);
                g2d.drawString("BANNED BY:", 40, 360);
                g2d.setColor(TEXT_WHITE);
                g2d.drawString(punishment.getExecutorName(), 40, 380);
                
                // Date
                g2d.setColor(TEXT_GRAY);
                g2d.drawString("DATE:", WIDTH / 2, 360);
                g2d.setColor(TEXT_WHITE);
                String date = punishment.getCreatedAt() != null ? 
                    DateTimeFormatter.ofPattern("yyyy-MM-dd").format(
                        punishment.getCreatedAt().atZone(java.time.ZoneId.systemDefault())
                    ) : "Unknown";
                g2d.drawString(date, WIDTH / 2, 380);
                
                // Duration / Status
                g2d.setColor(TEXT_GRAY);
                g2d.drawString("STATUS:", 40, 415);
                g2d.setColor(WANTED_RED);
                Font statusFont = new Font("Arial", Font.BOLD, 18);
                g2d.setFont(statusFont);
                String status = punishment.isPermanent() ? "PERMANENTLY BANNED" : 
                    (punishment.isActive() ? "BANNED" : "UNBANNED");
                g2d.drawString(status, 40, 435);
                
                // Ban ID
                g2d.setFont(infoFont);
                g2d.setColor(TEXT_GRAY);
                String banId = "ID: #" + punishment.getId();
                fm = g2d.getFontMetrics();
                g2d.drawString(banId, WIDTH - fm.stringWidth(banId) - 40, 435);
                
                // Footer
                g2d.setColor(new Color(100, 100, 100));
                Font footerFont = new Font("Arial", Font.ITALIC, 12);
                g2d.setFont(footerFont);
                String footer = "Generated by LiteBansReborn";
                fm = g2d.getFontMetrics();
                int footerX = (WIDTH - fm.stringWidth(footer)) / 2;
                g2d.drawString(footer, footerX, HEIGHT - 20);
                
                g2d.dispose();
                
                // Save to file
                String filename = punishment.getTargetName() + "_" + punishment.getId() + ".png";
                File outputFile = new File(outputDir, filename);
                ImageIO.write(poster, "PNG", outputFile);
                
                // Also return as base64 for web
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(poster, "PNG", baos);
                String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
                
                plugin.getLogger().info("[Mugshot] Generated poster for " + punishment.getTargetName());
                
                return "data:image/png;base64," + base64;
                
            } catch (Exception e) {
                plugin.getLogger().warning("[Mugshot] Failed to generate: " + e.getMessage());
                return null;
            }
        });
    }
    
    /**
     * Get player avatar from Minotar API
     */
    private BufferedImage getPlayerAvatar(String playerName, int size) {
        try {
            URL url = new URL(AVATAR_API + playerName + "/" + size);
            return ImageIO.read(url);
        } catch (Exception e) {
            // Return Steve head as fallback
            try {
                URL url = new URL(AVATAR_API + "MHF_Steve/" + size);
                return ImageIO.read(url);
            } catch (Exception ex) {
                return null;
            }
        }
    }
    
    /**
     * Truncate text if too long
     */
    private String truncateText(String text, int maxLength) {
        if (text == null) return "Unknown";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
    
    /**
     * Get mugshot file path
     */
    public File getMugshotFile(String playerName, long punishmentId) {
        String filename = playerName + "_" + punishmentId + ".png";
        return new File(outputDir, filename);
    }
    
    /**
     * Check if mugshot exists
     */
    public boolean mugshotExists(String playerName, long punishmentId) {
        return getMugshotFile(playerName, punishmentId).exists();
    }
}
