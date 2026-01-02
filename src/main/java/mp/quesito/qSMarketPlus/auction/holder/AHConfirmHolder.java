package mp.quesito.qSMarketPlus.auction.holder;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class AHConfirmHolder implements InventoryHolder {

    private final boolean bulk;
    private final double price;
    private final ItemStack[] items;

    private final ItemStack backup; // ítem original completo
    private final int sourceSlot;   // slot de donde se sacó

    // =====================================================
    // SUBASTA INDIVIDUAL — con backup + slot original
    // =====================================================
    public AHConfirmHolder(ItemStack item, double price, ItemStack backup, int sourceSlot) {
        this.bulk = false;
        this.price = price;
        this.items = new ItemStack[]{ item };
        this.backup = backup;
        this.sourceSlot = sourceSlot;
    }

    // =====================================================
    // SUBASTA MASIVA — no necesita backup ni slot
    // =====================================================
    public AHConfirmHolder(ItemStack[] bulkItems, double price) {
        this.bulk = true;
        this.price = price;
        this.items = bulkItems;
        this.backup = null;
        this.sourceSlot = -1;
    }

    // GETTERS
    public boolean isBulk() {
        return bulk;
    }

    public double getPrice() {
        return price;
    }

    public ItemStack[] getItems() {
        return items;
    }

    public ItemStack getBackup() {
        return backup;
    }

    public int getSourceSlot() {
        return sourceSlot;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
