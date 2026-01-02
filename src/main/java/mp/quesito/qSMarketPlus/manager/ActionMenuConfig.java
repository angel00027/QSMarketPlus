package mp.quesito.qSMarketPlus.manager;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import org.bukkit.configuration.file.FileConfiguration;

public class ActionMenuConfig {

    private final ConfigFile file;

    public ActionMenuConfig(QSMarketPlus plugin) {
        this.file = new ConfigFile(plugin, "action-menu.yml");
    }

    public FileConfiguration getConfig() {
        return file.getConfig();
    }

    public void save() {
        file.save();
    }

    public void reload() {
        file.reload();
    }
}
