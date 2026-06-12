package mp.quesito.qSMarketPlus.holder;

import mp.quesito.qSMarketPlus.shop.ShopCategory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class ProShopHolder implements InventoryHolder {

    private final int categoryIndex;
    private final int page;

    public ProShopHolder(int categoryIndex, int page) {
        this.categoryIndex = categoryIndex;
        this.page = page;
    }

    public int getCategoryIndex() {
        return categoryIndex;
    }

    public int getPage() {
        return page;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}