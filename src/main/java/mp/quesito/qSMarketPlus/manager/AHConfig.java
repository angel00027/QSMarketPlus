package mp.quesito.qSMarketPlus.manager;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class AHConfig {

    private static FileConfiguration cfg;

    public static void load(QSMarketPlus plugin) {
        File file = new File(plugin.getDataFolder(), "ah_menu.yml");

        if (!file.exists()) {
            plugin.saveResource("ah_menu.yml", false);
        }

        cfg = YamlConfiguration.loadConfiguration(file);
    }

    public static FileConfiguration get() {
        return cfg;
    }

    public static void reload(QSMarketPlus plugin) {
        load(plugin); // simplemente vuelve a cargar igual que load()
    }

}
