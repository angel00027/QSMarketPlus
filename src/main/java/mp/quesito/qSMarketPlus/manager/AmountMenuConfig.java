package mp.quesito.qSMarketPlus.manager;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class AmountMenuConfig {

    private final File file;
    private FileConfiguration config;

    public AmountMenuConfig(QSMarketPlus plugin) {

        file = new File(plugin.getDataFolder(), "amount-menu.yml");

        // Crear archivo si no existe
        if (!file.exists()) {
            plugin.saveResource("amount-menu.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(file);
    }

    /** Obtener configuración */
    public FileConfiguration getConfig() {
        return config;
    }

    /** Guardar cambios */
    public void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Recargar desde el archivo */
    public void reload() {
        config = YamlConfiguration.loadConfiguration(file);
    }
}
