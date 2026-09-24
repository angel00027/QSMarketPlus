package mp.quesito.qSMarketPlus.manager;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

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

        // Auto-fusión: si el archivo en disco es antiguo y le faltan claves
        // nuevas (p. ej. el botón "1 Pila"), se copian desde el jar sin tocar las existentes.
        try (InputStream in = plugin.getResource("amount-menu.yml")) {
            if (in != null) {
                YamlConfiguration def = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(in, StandardCharsets.UTF_8));
                boolean changed = false;
                for (String key : def.getKeys(true)) {
                    if (def.isConfigurationSection(key)) continue;
                    if (!config.isSet(key)) {
                        config.set(key, def.get(key));
                        changed = true;
                    }
                }

                // Claves "críticas" del layout que se fuerzan desde el jar:
                // tamaño del menú, slot del info y posición de TODOS los botones,
                // para que los servidores viejos adopten el layout correcto.
                if (config.getInt("menu.size", def.getInt("menu.size", 45)) != def.getInt("menu.size", 45)) {
                    config.set("menu.size", def.getInt("menu.size", 45));
                    changed = true;
                }
                if (config.getInt("buttons.info-slot", def.getInt("buttons.info-slot", 22))
                        != def.getInt("buttons.info-slot", 22)) {
                    config.set("buttons.info-slot", def.getInt("buttons.info-slot", 22));
                    changed = true;
                }
                if (def.isConfigurationSection("buttons")) {
                    for (String key : def.getConfigurationSection("buttons").getKeys(false)) {
                        if ("info-slot".equals(key)) continue;
                        String slotPath = "buttons." + key + ".slot";
                        int defSlot = def.getInt(slotPath, -1);
                        if (config.isSet(slotPath) && config.getInt(slotPath, -1) != defSlot) {
                            config.set(slotPath, defSlot);
                            changed = true;
                        }
                    }
                }

                // Borrar claves que ya no existen en el jar (botones antiguos
                // de pilas y la regleta) para no dejar basura en el menú.
                if (config.isConfigurationSection("buttons")) {
                    for (String key : new ArrayList<>(config.getConfigurationSection("buttons").getKeys(false))) {
                        if ("info-slot".equals(key)) continue;
                        if (!def.isConfigurationSection("buttons." + key)) {
                            config.set("buttons." + key, null);
                            changed = true;
                        }
                    }
                }
                if (config.isConfigurationSection("regleta") && !def.isConfigurationSection("regleta")) {
                    config.set("regleta", null);
                    changed = true;
                }

                if (changed) config.save(file);
            }
        } catch (Exception ignored) { }
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
