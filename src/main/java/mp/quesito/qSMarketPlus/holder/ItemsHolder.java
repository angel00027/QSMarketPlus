package mp.quesito.qSMarketPlus.holder;

import mp.quesito.qSMarketPlus.shop.ShopCategory;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class ItemsHolder implements InventoryHolder {

    private static final Inventory EMPTY = Bukkit.createInventory(null, 9);

    private final Player player;
    private final ShopCategory category;
    private final int page;
    private final int size;

    public ItemsHolder(Player player, ShopCategory category, int page, int size) {
        this.player = player;
        this.category = category;
        this.page = page;
        this.size = size;
    }

    @Override
    public Inventory getInventory() {
        return EMPTY; // Inventario vacío para cumplir API
    }

    public Player getPlayer() { return player; }
    public ShopCategory getCategory() { return category; }
    public int getPage() { return page; }
    public int getSize() { return size; }
}
