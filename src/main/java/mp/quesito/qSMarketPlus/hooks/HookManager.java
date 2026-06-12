package mp.quesito.qSMarketPlus.hooks;

import mp.quesito.qSMarketPlus.hooks.impl.QSTextursHook;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class HookManager {

    private final List<ItemProvider> providers = new ArrayList<>();

    public HookManager() {

        QSTextursHook qst = new QSTextursHook();

        if (qst.isAvailable()) {
            providers.add(qst);
        }
    }

    public ItemStack getItem(String id) {

        for (ItemProvider provider : providers) {

            ItemStack item = provider.getItem(id);

            if (item != null)
                return item;

        }

        return null;
    }
}