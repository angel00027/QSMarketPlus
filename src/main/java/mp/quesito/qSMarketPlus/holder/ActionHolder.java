package mp.quesito.qSMarketPlus.holder;

import mp.quesito.qSMarketPlus.shop.ShopCategory;
import mp.quesito.qSMarketPlus.shop.ShopItem;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class ActionHolder implements InventoryHolder {
    private final ShopItem item;
    private final ShopCategory category;

    public ActionHolder(ShopItem item, ShopCategory category) {
        this.item = item;
        this.category = category;
    }

    public ShopItem getItem() { return item; }
    public ShopCategory getCategory() { return category; }

    @Override
    public Inventory getInventory() { return null; }
}
