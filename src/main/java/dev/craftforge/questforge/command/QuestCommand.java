package dev.craftforge.questforge.command;

import dev.craftforge.questforge.QuestForge;
import dev.craftforge.questforge.gui.QuestMenu;
import dev.craftforge.questforge.quest.*;
import dev.craftforge.questforge.quest.objective.Objective;
import dev.craftforge.questforge.storage.ProgressStorage;
import dev.craftforge.questforge.util.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class QuestCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("list", "info", "start", "abandon", "gui", "reload");

    private final QuestForge plugin;
    private final QuestRegistry registry;
    private final QuestService questService;
    private final ProgressStorage progressStorage;
    private final QuestMenu questMenu;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public QuestCommand(
            final QuestForge plugin,
            final QuestRegistry registry,
            final QuestService questService,
            final ProgressStorage progressStorage,
            final QuestMenu questMenu
    ) {
        this.plugin = plugin;
        this.registry = registry;
        this.questService = questService;
        this.progressStorage = progressStorage;
        this.questMenu = questMenu;
    }

    @Override
    public boolean onCommand(
            final @NotNull CommandSender sender,
            final @NotNull Command command,
            final @NotNull String label,
            final @NotNull String[] args
    ) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list" -> handleList(sender);
            case "info" -> {
                if (args.length < 2) {
                    sendUsage(sender);
                    return true;
                }
                handleInfo(sender, args[1]);
            }
            case "start" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(miniMessage.deserialize("<red>This command can only be used by a player.</red>"));
                    return true;
                }
                if (args.length < 2) {
                    sendUsage(sender);
                    return true;
                }
                handleStart(player, args[1]);
            }
            case "abandon" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(miniMessage.deserialize("<red>This command can only be used by a player.</red>"));
                    return true;
                }
                if (args.length < 2) {
                    sendUsage(sender);
                    return true;
                }
                handleAbandon(player, args[1]);
            }
            case "gui" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(miniMessage.deserialize("<red>This command can only be used by a player.</red>"));
                    return true;
                }
                questMenu.open(player, 1);
            }
            case "reload" -> {
                if (!sender.hasPermission("questforge.reload")) {
                    sender.sendMessage(miniMessage.deserialize(Messages.prefix(plugin) + Messages.get(plugin, "no-permission", "<red>You do not have permission.</red>")));
                    return true;
                }
                handleReload(sender);
            }
            default -> sendUsage(sender);
        }

        return true;
    }

    private void handleList(final CommandSender sender) {
        final Player player = sender instanceof Player p ? p : null;
        final PlayerQuestData playerData = player != null ? questService.getOrCreateData(player.getUniqueId()) : null;

        sender.sendMessage(miniMessage.deserialize("<gold><bold>--- Quest List ---</bold></gold>"));
        for (final Quest quest : registry.all()) {
            final String status;
            if (playerData != null) {
                if (playerData.getProgress(quest.getId()) != null) {
                    status = "<yellow>[In Progress]</yellow>";
                } else if (playerData.getCompletedQuestIds().contains(quest.getId())) {
                    status = "<aqua>[Completed]</aqua>";
                } else if (!quest.isUnlockedFor(playerData)) {
                    status = "<red>[Locked]</red>";
                } else {
                    status = "<green>[Available]</green>";
                }
            } else {
                status = "";
            }
            sender.sendMessage(miniMessage.deserialize(
                    status + " " + miniMessage.serialize(quest.getDisplayName()) + " <dark_gray>(" + quest.getId() + ")</dark_gray>"
            ));
        }
    }

    private void handleInfo(final CommandSender sender, final String questId) {
        final Optional<Quest> questOpt = registry.byId(questId);
        if (questOpt.isEmpty()) {
            final String template = Messages.get(plugin, "quest-not-found", "<red>Quest not found: <yellow>{id}</yellow></red>");
            sender.sendMessage(miniMessage.deserialize(Messages.prefix(plugin) + template.replace("{id}", questId)));
            return;
        }

        final Quest quest = questOpt.get();
        sender.sendMessage(miniMessage.deserialize("<gold><bold>--- " + miniMessage.serialize(quest.getDisplayName()) + " <bold>---</bold></gold>"));
        for (final Component line : quest.getDescriptionLines()) {
            sender.sendMessage(line);
        }
        sender.sendMessage(miniMessage.deserialize("<gray>Objectives:</gray>"));
        for (final Objective obj : quest.getObjectives()) {
            sender.sendMessage(miniMessage.deserialize("  <dark_gray>- </dark_gray>").append(obj.description()));
        }
        if (quest.getRequiredQuestId() != null) {
            final String reqId = quest.getRequiredQuestId();
            final String reqName = registry.byId(reqId)
                    .map(q -> miniMessage.serialize(q.getDisplayName()))
                    .orElse(reqId);
            sender.sendMessage(miniMessage.deserialize("<gray>Requires: </gray>" + reqName));
        }
        sender.sendMessage(miniMessage.deserialize("<gray>Repeatable: <white>" + quest.isRepeatable() + "</white></gray>"));
    }

    private void handleStart(final Player player, final String questId) {
        final Optional<Quest> questOpt = registry.byId(questId);
        if (questOpt.isEmpty()) {
            final String template = Messages.get(plugin, "quest-not-found", "<red>Quest not found: <yellow>{id}</yellow></red>");
            player.sendMessage(miniMessage.deserialize(Messages.prefix(plugin) + template.replace("{id}", questId)));
            return;
        }

        final Quest quest = questOpt.get();
        final QuestService.StartResult result = questService.start(player, quest);
        final String name = miniMessage.serialize(quest.getDisplayName());

        final String message = switch (result) {
            case SUCCESS -> Messages.get(plugin, "quest-started", "<green>Quest started: <yellow>{quest}</yellow></green>").replace("{quest}", name);
            case ALREADY_ACTIVE -> Messages.get(plugin, "quest-already-active", "<red>You already have that quest active.</red>");
            case ALREADY_COMPLETED -> Messages.get(plugin, "quest-already-completed", "<red>You have already completed that quest.</red>");
            case NOT_UNLOCKED -> {
                final String template = Messages.get(plugin, "quest-not-unlocked", "<red>You must complete <yellow>{required}</yellow> first.</red>");
                final String reqName = quest.getRequiredQuestId() != null
                        ? registry.byId(quest.getRequiredQuestId()).map(q -> miniMessage.serialize(q.getDisplayName())).orElse(quest.getRequiredQuestId())
                        : "";
                yield template.replace("{required}", reqName);
            }
            case LIMIT_REACHED -> Messages.get(plugin, "active-limit-reached", "<red>Active quest limit reached.</red>")
                    .replace("{limit}", String.valueOf(plugin.getConfig().getInt("active-quest-limit", 3)));
        };

        player.sendMessage(miniMessage.deserialize(Messages.prefix(plugin) + message));
    }

    private void handleAbandon(final Player player, final String questId) {
        final Optional<Quest> questOpt = registry.byId(questId);
        if (questOpt.isEmpty()) {
            final String template = Messages.get(plugin, "quest-not-found", "<red>Quest not found: <yellow>{id}</yellow></red>");
            player.sendMessage(miniMessage.deserialize(Messages.prefix(plugin) + template.replace("{id}", questId)));
            return;
        }

        final Quest quest = questOpt.get();
        final QuestService.AbandonResult result = questService.abandon(player, quest);
        final String name = miniMessage.serialize(quest.getDisplayName());

        final String message = switch (result) {
            case SUCCESS -> Messages.get(plugin, "quest-abandoned", "<red>Abandoned quest: <yellow>{quest}</yellow></red>").replace("{quest}", name);
            case NOT_ACTIVE -> Messages.get(plugin, "quest-not-active", "<red>That quest is not active.</red>");
        };

        player.sendMessage(miniMessage.deserialize(Messages.prefix(plugin) + message));
    }

    private void handleReload(final CommandSender sender) {
        plugin.reloadConfig();
        registry.reload();
        progressStorage.reload();
        final String message = Messages.get(plugin, "reload-success", "<green>QuestForge reloaded successfully.</green>");
        sender.sendMessage(miniMessage.deserialize(Messages.prefix(plugin) + message));
    }

    private void sendUsage(final CommandSender sender) {
        final String usage = Messages.get(plugin, "usage-quest", "<red>Usage: /quest <list|info <id>|start <id>|abandon <id>|gui|reload></red>");
        sender.sendMessage(miniMessage.deserialize(usage));
    }

    @Override
    public List<String> onTabComplete(
            final @NotNull CommandSender sender,
            final @NotNull Command command,
            final @NotNull String alias,
            final @NotNull String[] args
    ) {
        if (args.length == 1) {
            return SUBCOMMANDS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "info", "start", "abandon" -> registry.all().stream()
                        .map(Quest::getId)
                        .filter(id -> id.startsWith(args[1].toLowerCase()))
                        .toList();
                default -> Collections.emptyList();
            };
        }

        return Collections.emptyList();
    }
}
