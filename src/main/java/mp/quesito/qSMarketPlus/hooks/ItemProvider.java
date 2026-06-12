package mp.quesito.qSMarketPlus.hooks;

import org.bukkit.inventory.ItemStack;

public interface ItemProvider {

    boolean isAvailable();

    ItemStack getItem(String id);

}