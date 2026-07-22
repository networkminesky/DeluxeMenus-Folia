package com.extendedclip.deluxemenus.hooks;

import com.extendedclip.deluxemenus.DeluxeMenus;
import com.extendedclip.deluxemenus.cache.SimpleCache;
import com.extendedclip.deluxemenus.utils.DebugLevel;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class MMOItemsHook implements ItemHook, SimpleCache {

    private final Map<String, ItemStack> cache = new ConcurrentHashMap<>();
    private final DeluxeMenus plugin;

    public MMOItemsHook(final @NotNull DeluxeMenus plugin) {
        this.plugin = plugin;
    }

    @Override
    public ItemStack getItem(@NotNull final String... arguments) {
        if (arguments.length == 0) {
            return new ItemStack(Material.STONE, 1);
        }

        final ItemStack cached = cache.get(arguments[0]);
        if (cached != null) {
            return cached.clone();
        }

        String[] splitArgs = arguments[0].split(":", 2);
        if (splitArgs.length != 2) {
            return new ItemStack(Material.STONE, 1);
        }

        final Type itemType = MMOItems.plugin.getTypes().get(splitArgs[0]);
        if (itemType == null) {
            return new ItemStack(Material.STONE, 1);
        }

        final CompletableFuture<ItemStack> future = new CompletableFuture<>();

        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> {
            try {
                ItemStack item = MMOItems.plugin.getItem(itemType, splitArgs[1]);

                if (item == null) {
                    future.complete(new ItemStack(Material.STONE, 1));
                    return;
                }

                cache.put(arguments[0], item);
                future.complete(item.clone());
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Erro ao obter item MMOItems: " + arguments[0], e);
                future.complete(new ItemStack(Material.STONE, 1));
            }
        });

        try {
            return future.get(3, TimeUnit.SECONDS);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Timeout ou erro ao aguardar item MMOItems: " + arguments[0], e);
            return new ItemStack(Material.STONE, 1);
        }
    }

    @Override
    public boolean itemMatchesIdentifiers(@NotNull ItemStack item, @NotNull String... arguments) {
        if (arguments.length == 0) return false;
        String[] splitArgs = arguments[0].split(":", 2);
        if (splitArgs.length != 2) return false;
        return splitArgs[0].equalsIgnoreCase(MMOItems.getTypeName(item))
            && splitArgs[1].equalsIgnoreCase(MMOItems.getID(item));
    }

    @Override
    public String getPrefix() {
        return "mmoitems-";
    }

    @Override
    public void clearCache() {
        cache.clear();
    }
}
