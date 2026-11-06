package mp.quesito.qSMarketPlus.shop.browser;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.List;

/** product = -1 en la vista principal; si no, el objeto que se esta mirando. */
public class ShopBrowserHolder implements InventoryHolder {

    private final List<ShopBrowser.Product> products;
    private final int product;
    private final int page;
    private Inventory inventory;

    public ShopBrowserHolder(List<ShopBrowser.Product> products, int product, int page) {
        this.products = products;
        this.product = product;
        this.page = page;
    }

    public List<ShopBrowser.Product> getProducts() { return products; }
    public int getProduct() { return product; }
    public int getPage() { return page; }

    public void setInventory(Inventory inventory) { this.inventory = inventory; }

    @Override
    public Inventory getInventory() { return inventory; }
}
