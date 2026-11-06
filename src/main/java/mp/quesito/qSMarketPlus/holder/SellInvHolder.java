package mp.quesito.qSMarketPlus.holder;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class SellInvHolder implements InventoryHolder {

    private Inventory inventory;
    private final Player viewer;

    public SellInvHolder(Player viewer) {
        this.viewer = viewer;
    }

    public Player getViewer() {
        return viewer;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}