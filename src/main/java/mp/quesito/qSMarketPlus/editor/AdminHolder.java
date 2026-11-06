package mp.quesito.qSMarketPlus.editor;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class AdminHolder implements InventoryHolder {

    public String mode;       // main | categories | category | items | item
    public String categoryId; // categoría de contexto
    public String itemId;     // ítem de contexto
    public int page;          // página en menús paginados

    private Inventory inventory;

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}