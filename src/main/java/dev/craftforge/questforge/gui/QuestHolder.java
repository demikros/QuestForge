package dev.craftforge.questforge.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public final class QuestHolder implements InventoryHolder {

    private final int page;
    private Inventory inventory;

    public QuestHolder(final int page) {
        this.page = page;
    }

    public int getPage() {
        return page;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public void setInventory(final Inventory inventory) {
        this.inventory = inventory;
    }
}
