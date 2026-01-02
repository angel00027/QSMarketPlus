package mp.quesito.qSMarketPlus.holder;

import mp.quesito.qSMarketPlus.shop.ShopCategory;
import mp.quesito.qSMarketPlus.shop.ShopItem;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class ConfirmHolder implements InventoryHolder {

    private final ShopItem item;
    private final ShopCategory category;
    private int amount;

    private final boolean buying;

    public ConfirmHolder(ShopItem item, ShopCategory category, int amount, boolean buying) {
        this.item = item;
        this.category = category;
        this.amount = amount;
        this.buying = buying;
    }

    public ShopItem getItem() { return item; }
    public ShopCategory getCategory() { return category; }
    public int getAmount() { return amount; }
    public boolean isBuying() { return buying; }
    public void setAmount(int amount) {
        this.amount = amount;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
