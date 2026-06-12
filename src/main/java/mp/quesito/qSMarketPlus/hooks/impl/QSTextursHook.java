package mp.quesito.qSMarketPlus.hooks.impl;

import mp.quesito.qSMarketPlus.hooks.ItemProvider;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.qs.qSTexturs.api.QSTextursAPI;

public class QSTextursHook implements ItemProvider {

    @Override
    public boolean isAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("QSTexturs");
    }

    @Override
    public ItemStack getItem(String id) {
        return QSTextursAPI.getItem(id);
    }
}