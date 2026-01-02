package mp.quesito.qSMarketPlus.manager;


import mp.quesito.qSMarketPlus.QSMarketPlus;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class ConfigFile {

    private final QSMarketPlus plugin;
    private final File file;
    private FileConfiguration config;

    public ConfigFile(QSMarketPlus plugin, String fileName) {

        this.plugin = plugin;

        // Archivo dentro de la carpeta del plugin
        this.file = new File(plugin.getDataFolder(), fileName);

        // Si no existe → copiar del jar
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }

        // Cargar configuración
        this.config = YamlConfiguration.loadConfiguration(file);
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
        this.config = YamlConfiguration.loadConfiguration(file);
    }
}
