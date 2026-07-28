package dev.craftforge.questforge.listener;

import dev.craftforge.questforge.QuestForge;
import dev.craftforge.questforge.quest.QuestService;
import dev.craftforge.questforge.quest.objective.ObjectiveEvent;
import dev.craftforge.questforge.quest.objective.ObjectiveType;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ObjectiveListener implements Listener {

    private final QuestForge plugin;
    private final QuestService questService;
    private final Set<Location> playerPlacedBlocks = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public ObjectiveListener(final QuestForge plugin, final QuestService questService) {
        this.plugin = plugin;
        this.questService = questService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(final EntityDeathEvent event) {
        final Player killer = event.getEntity().getKiller();
        if (killer == null || killer.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        final String entityKey = event.getEntityType().name();
        questService.handleEvent(new ObjectiveEvent(ObjectiveType.KILL_MOB, killer, entityKey, 1));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(final BlockBreakEvent event) {
        final Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        final Block block = event.getBlock();
        final boolean antiFarm = plugin.getConfig().getBoolean("anti-farm", true);
        if (antiFarm && playerPlacedBlocks.remove(block.getLocation())) {
            return;
        }

        final String materialKey = block.getType().name();
        questService.handleEvent(new ObjectiveEvent(ObjectiveType.BREAK_BLOCK, player, materialKey, 1));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(final BlockPlaceEvent event) {
        final Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        final boolean antiFarm = plugin.getConfig().getBoolean("anti-farm", true);
        if (antiFarm) {
            playerPlacedBlocks.add(event.getBlock().getLocation());
        }

        final String materialKey = event.getBlock().getType().name();
        questService.handleEvent(new ObjectiveEvent(ObjectiveType.PLACE_BLOCK, player, materialKey, 1));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityPickupItem(final EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        final ItemStack item = event.getItem().getItemStack();
        final String materialKey = item.getType().name();
        questService.handleEvent(new ObjectiveEvent(ObjectiveType.COLLECT_ITEM, player, materialKey, item.getAmount()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftItem(final CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        final ItemStack result = event.getRecipe().getResult();
        if (result.getType().isAir()) {
            return;
        }

        final int craftedAmount = switch (event.getClick()) {
            case SHIFT_LEFT, SHIFT_RIGHT -> {
                int maxCraft = 64;
                for (final ItemStack ingredient : event.getInventory().getMatrix()) {
                    if (ingredient != null && !ingredient.getType().isAir()) {
                        maxCraft = Math.min(maxCraft, ingredient.getAmount());
                    }
                }
                yield maxCraft * result.getAmount();
            }
            default -> result.getAmount();
        };

        final String materialKey = result.getType().name();
        questService.handleEvent(new ObjectiveEvent(ObjectiveType.CRAFT_ITEM, player, materialKey, craftedAmount));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerFish(final PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        final Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        if (!(event.getCaught() instanceof org.bukkit.entity.Item caughtItem)) {
            return;
        }
        final ItemStack item = caughtItem.getItemStack();
        final String materialKey = item.getType().name();
        questService.handleEvent(new ObjectiveEvent(ObjectiveType.FISH_ITEM, player, materialKey, item.getAmount()));
    }

    public void clearPlacedBlocks() {
        playerPlacedBlocks.clear();
    }
}
