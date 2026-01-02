package mp.quesito.qSMarketPlus.manager;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.shop.ShopItem;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;

public class UniquePurchaseManager {

    private final QSMarketPlus plugin;
    private final File file;
    private final FileConfiguration data;

    public UniquePurchaseManager(QSMarketPlus plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "unique_purchases.yml");

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        this.data = YamlConfiguration.loadConfiguration(file);
    }

    public boolean hasPurchased(Player player, ShopItem item) {
        return data.getBoolean("unique_purchases." + player.getUniqueId() + "." + item.getId(), false);
    }

    public void markPurchased(Player player, ShopItem item) {
        data.set("unique_purchases." + player.getUniqueId() + "." + item.getId(), true);
        save();
    }

    public void save() {
        try {
            data.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
