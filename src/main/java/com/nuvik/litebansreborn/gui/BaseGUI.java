package com.nuvik.litebansreborn.gui;

import com.nuvik.litebansreborn.LiteBansReborn;
import com.nuvik.litebansreborn.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public abstract class BaseGUI implements InventoryHolder, Listener {

    protected final LiteBansReborn plugin;
    protected final Inventory inventory;
    protected final Player player;

    public BaseGUI(LiteBansReborn plugin, Player player, String title, int size) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, size, ColorUtil.translate(title));
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        // initializeItems() removed from here to prevent null pointer on subclass fields
    }

    public abstract void initializeItems();

    public void open() {
        // Check if inventory needs initialization (first slot is empty/null)
        if (inventory.getItem(0) == null) {
            initializeItems();
        }
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) return;
        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        handleClick(event);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() != this) return;
        event.setCancelled(true);
    }

    protected abstract void handleClick(InventoryClickEvent event);

    protected ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.translate(name));
            List<String> translatedLore = new ArrayList<>();
            for (String l : lore) {
                translatedLore.add(ColorUtil.translate(l));
            }
            meta.setLore(translatedLore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    protected ItemStack createGuiItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.translate(name));
            meta.setLore(lore.stream().map(ColorUtil::translate).collect(Collectors.toList()));
            item.setItemMeta(meta);
        }
        return item;
    }
    
    protected ItemStack createSkullItem(String playerName, String name, String... lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD, 1); // 1.13+
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            meta.setOwner(playerName);
            meta.setDisplayName(ColorUtil.translate(name));
            List<String> translatedLore = new ArrayList<>();
            for (String l : lore) {
                translatedLore.add(ColorUtil.translate(l));
            }
            meta.setLore(translatedLore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
