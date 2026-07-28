package dev.craftforge.questforge.quest;

import dev.craftforge.questforge.QuestForge;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public sealed interface Reward permits Reward.ItemReward, Reward.CommandReward, Reward.ExperienceReward, Reward.MessageReward {

    void grant(QuestForge plugin, Player player);

    record ItemReward(Material material, int amount) implements Reward {
        @Override
        public void grant(final QuestForge plugin, final Player player) {
            player.getInventory().addItem(new ItemStack(material, amount));
            final String template = plugin.getConfig().getString("messages.reward-item", "<green>Received: <yellow>{item}</yellow></green>");
            final String label = material.name().replace("_", " ").toLowerCase();
            player.sendMessage(MiniMessage.miniMessage().deserialize(template.replace("{item}", amount + "x " + label)));
        }
    }

    record CommandReward(String command) implements Reward {
        @Override
        public void grant(final QuestForge plugin, final Player player) {
            final String resolved = command.replace("{player}", player.getName());
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), resolved);
        }
    }

    record ExperienceReward(int amount) implements Reward {
        @Override
        public void grant(final QuestForge plugin, final Player player) {
            player.giveExp(amount);
            final String template = plugin.getConfig().getString("messages.reward-experience", "<green>+{amount} XP</green>");
            player.sendMessage(MiniMessage.miniMessage().deserialize(template.replace("{amount}", String.valueOf(amount))));
        }
    }

    record MessageReward(String miniMessageText) implements Reward {
        @Override
        public void grant(final QuestForge plugin, final Player player) {
            if (!miniMessageText.isEmpty()) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(miniMessageText));
            }
        }
    }
}
