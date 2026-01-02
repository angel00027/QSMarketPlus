package mp.quesito.qSMarketPlus.listeners;

import mp.quesito.qSMarketPlus.holder.ItemsHolder;
import mp.quesito.qSMarketPlus.manager.ItemManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class ItemsMenuListener implements Listener {

    private final ItemManager manager;

    public ItemsMenuListener(ItemManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {

        if (!(e.getInventory().getHolder() instanceof ItemsHolder holder)) return;

        String categoryId = holder.getCategory().getId();

        // 🔥 limpiar el orden del shop SOLO al cerrar ItemsMenu
        manager.clearSortedItems(categoryId);
    }
}
