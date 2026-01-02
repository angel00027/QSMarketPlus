package mp.quesito.qSMarketPlus.manager;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class ConfirmMenuConfig {

    private final QSMarketPlus plugin;
    private File file;
    private FileConfiguration config;

    public ConfirmMenuConfig(QSMarketPlus plugin) {
        this.plugin = plugin;
        create();
    }

    public void create() {
        file = new File(plugin.getDataFolder(), "confirm-menu.yml");

        if (!file.exists()) {
            plugin.saveResource("confirm-menu.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reload() {
        config = YamlConfiguration.loadConfiguration(file);
    }
}