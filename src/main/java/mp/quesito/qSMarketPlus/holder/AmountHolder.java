package mp.quesito.qSMarketPlus.holder;

import mp.quesito.qSMarketPlus.shop.ShopCategory;
import mp.quesito.qSMarketPlus.shop.ShopItem;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class AmountHolder implements InventoryHolder {

    private final ShopItem item;
    private final ShopCategory category;
    private final boolean buying;

    private int amount = 1; // cantidad inicial

    public AmountHolder(ShopItem item, ShopCategory category, boolean buying) {
        this.item = item;
        this.category = category;
        this.buying = buying;
    }

    public ShopItem getItem() { return item; }
    public ShopCategory getCategory() { return category; }
    public boolean isBuying() { return buying; }

    public int getAmount() { return amount; }
    public void setAmount(int amt) { this.amount = Math.max(1, Math.min(64, amt)); }

    @Override public Inventory getInventory() { return null; }
}

