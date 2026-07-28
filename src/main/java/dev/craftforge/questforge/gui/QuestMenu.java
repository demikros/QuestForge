package dev.craftforge.questforge.gui;

import dev.craftforge.questforge.QuestForge;
import dev.craftforge.questforge.quest.*;
import dev.craftforge.questforge.quest.objective.Objective;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class QuestMenu implements Listener {

    private static final int PAGE_SIZE = 45;
    private static final int PREV_SLOT = 45;
    private static final int NEXT_SLOT = 53;
    private static final int INV_SIZE = 54;

    private final QuestForge plugin;
    private final QuestRegistry registry;
    private final QuestService questService;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public QuestMenu(final QuestForge plugin, final QuestRegistry registry, final QuestService questService) {
        this.plugin = plugin;
        this.registry = registry;
        this.questService = questService;
    }

    public void open(final Player player, final int page) {
        final List<Quest> allQuests = new ArrayList<>(registry.all());
        final int totalPages = Math.max(1, (int) Math.ceil((double) allQuests.size() / PAGE_SIZE));
        final int currentPage = Math.max(1, Math.min(page, totalPages));

        final String titleTemplate = plugin.getConfig().getString("messages.gui-title", "<dark_gray>QuestForge <gray>- Page {page}/{total}</gray></dark_gray>");
        final String titleStr = titleTemplate
                .replace("{page}", String.valueOf(currentPage))
                .replace("{total}", String.valueOf(totalPages));

        final QuestHolder holder = new QuestHolder(currentPage);
        final Inventory inv = Bukkit.createInventory(holder, INV_SIZE, miniMessage.deserialize(titleStr));
        holder.setInventory(inv);

        final PlayerQuestData playerData = questService.getOrCreateData(player.getUniqueId());
        final int startIndex = (currentPage - 1) * PAGE_SIZE;
        final int endIndex = Math.min(startIndex + PAGE_SIZE, allQuests.size());

        for (int i = startIndex; i < endIndex; i++) {
            final Quest quest = allQuests.get(i);
            inv.setItem(i - startIndex, buildQuestItem(quest, playerData));
        }

        if (currentPage > 1) {
            inv.setItem(PREV_SLOT, buildNavItem(Material.ARROW, "<gray>Previous Page</gray>"));
        }
        if (currentPage < totalPages) {
            inv.setItem(NEXT_SLOT, buildNavItem(Material.ARROW, "<gray>Next Page</gray>"));
        }

        player.openInventory(inv);
    }

    private ItemStack buildQuestItem(final Quest quest, final PlayerQuestData playerData) {
        final boolean completed = playerData.getCompletedQuestIds().contains(quest.getId());
        final boolean active = playerData.getProgress(quest.getId()) != null;
        final boolean unlocked = quest.isUnlockedFor(playerData);

        final Material icon;
        final String statusTag;

        if (completed && !quest.isRepeatable()) {
            icon = Material.LIME_DYE;
            statusTag = plugin.getConfig().getString("messages.gui-completed", "<aqua>Completed</aqua>");
        } else if (active) {
            icon = Material.YELLOW_DYE;
            statusTag = plugin.getConfig().getString("messages.gui-in-progress", "<yellow>In Progress</yellow>");
        } else if (!unlocked) {
            icon = Material.RED_DYE;
            statusTag = plugin.getConfig().getString("messages.gui-locked", "<red>Locked</red>");
        } else {
            icon = Material.LIME_DYE;
            statusTag = plugin.getConfig().getString("messages.gui-available", "<green>Available</green>");
        }

        final ItemStack item = new ItemStack(icon);
        final ItemMeta meta = item.getItemMeta();
        meta.displayName(quest.getDisplayName());

        final List<Component> lore = new ArrayList<>();
        lore.add(miniMessage.deserialize(statusTag));
        lore.add(Component.empty());
        lore.addAll(quest.getDescriptionLines());
        lore.add(Component.empty());

        final QuestProgress progress = playerData.getProgress(quest.getId());
        for (int i = 0; i < quest.getObjectives().size(); i++) {
            final Objective obj = quest.getObjectives().get(i);
            final int current = progress != null ? Math.min(progress.getCounter(i), obj.getRequiredAmount()) : 0;
            final String progressStr = "<gray>" + current + "/" + obj.getRequiredAmount() + " </gray>";
            lore.add(miniMessage.deserialize(progressStr).append(obj.description()));
        }

        if (!unlocked && quest.getRequiredQuestId() != null) {
            lore.add(Component.empty());
            registry.byId(quest.getRequiredQuestId()).ifPresent(required -> {
                lore.add(miniMessage.deserialize("<red>Requires: </red>").append(required.getDisplayName()));
            });
        }

        if (active) {
            lore.add(Component.empty());
            lore.add(miniMessage.deserialize("<red>Click to abandon</red>"));
        } else if (unlocked && (!completed || quest.isRepeatable())) {
            lore.add(Component.empty());
            lore.add(miniMessage.deserialize("<green>Click to start</green>"));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildNavItem(final Material material, final String label) {
        final ItemStack item = new ItemStack(material);
        final ItemMeta meta = item.getItemMeta();
        meta.displayName(miniMessage.deserialize(label));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(final InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof QuestHolder holder)) {
            return;
        }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        final int slot = event.getRawSlot();

        if (slot == PREV_SLOT && holder.getPage() > 1) {
            open(player, holder.getPage() - 1);
            return;
        }

        final List<Quest> allQuests = new ArrayList<>(registry.all());
        final int totalPages = Math.max(1, (int) Math.ceil((double) allQuests.size() / PAGE_SIZE));
        if (slot == NEXT_SLOT && holder.getPage() < totalPages) {
            open(player, holder.getPage() + 1);
            return;
        }

        if (slot < 0 || slot >= PAGE_SIZE) {
            return;
        }

        final int questIndex = (holder.getPage() - 1) * PAGE_SIZE + slot;
        if (questIndex >= allQuests.size()) {
            return;
        }

        final Quest quest = allQuests.get(questIndex);
        final PlayerQuestData playerData = questService.getOrCreateData(player.getUniqueId());

        if (playerData.getProgress(quest.getId()) != null) {
            final QuestService.AbandonResult result = questService.abandon(player, quest);
            if (result == QuestService.AbandonResult.SUCCESS) {
                final String template = plugin.getConfig().getString("messages.quest-abandoned", "<red>Abandoned quest: <yellow>{quest}</yellow></red>");
                final String name = miniMessage.serialize(quest.getDisplayName());
                player.sendMessage(miniMessage.deserialize(template.replace("{quest}", name)));
            }
        } else {
            final QuestService.StartResult result = questService.start(player, quest);
            handleStartResult(player, result, quest);
        }

        open(player, holder.getPage());
    }

    private void handleStartResult(final Player player, final QuestService.StartResult result, final Quest quest) {
        final String name = miniMessage.serialize(quest.getDisplayName());
        final String message = switch (result) {
            case SUCCESS -> plugin.getConfig().getString("messages.quest-started", "<green>Quest started: <yellow>{quest}</yellow></green>").replace("{quest}", name);
            case ALREADY_ACTIVE -> plugin.getConfig().getString("messages.quest-already-active", "<red>You already have that quest active.</red>");
            case ALREADY_COMPLETED -> plugin.getConfig().getString("messages.quest-already-completed", "<red>You have already completed that quest.</red>");
            case NOT_UNLOCKED -> {
                final String template = plugin.getConfig().getString("messages.quest-not-unlocked", "<red>You must complete <yellow>{required}</yellow> first.</red>");
                final String reqName = quest.getRequiredQuestId() != null
                        ? registry.byId(quest.getRequiredQuestId()).map(q -> miniMessage.serialize(q.getDisplayName())).orElse(quest.getRequiredQuestId())
                        : "";
                yield template.replace("{required}", reqName);
            }
            case LIMIT_REACHED -> plugin.getConfig().getString("messages.active-limit-reached", "<red>You have reached the active quest limit.</red>")
                    .replace("{limit}", String.valueOf(plugin.getConfig().getInt("active-quest-limit", 3)));
        };
        player.sendMessage(miniMessage.deserialize(message));
    }
}
