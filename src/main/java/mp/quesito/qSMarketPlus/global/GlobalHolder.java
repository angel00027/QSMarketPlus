package mp.quesito.qSMarketPlus.global;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public class GlobalHolder implements InventoryHolder {

    private final int tab;                   // 0=Tienda, 1=Tradeos, 2=Subastas
    private final UUID playerFilter;         // null = todos
    private final String playerFilterName;   // nombre legible del filtro activo
    private final int timeIndex;             // 0=todos, 1=24h, 2=7d, 3=30d
    private int page;
    private Inventory inventory;

    public GlobalHolder(int tab, UUID playerFilter, String playerFilterName, int timeIndex, int page) {
        this.tab = tab;
        this.playerFilter = playerFilter;
        this.playerFilterName = playerFilterName;
        this.timeIndex = timeIndex;
        this.page = page;
    }

    public int getTab() {
        return tab;
    }

    public UUID getPlayerFilter() {
        return playerFilter;
    }

    public String getPlayerFilterName() {
        return playerFilterName;
    }

    public int getTimeIndex() {
        return timeIndex;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}