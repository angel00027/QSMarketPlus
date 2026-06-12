package mp.quesito.qSMarketPlus.manager;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.shop.ShopCategory;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class CategoryManager {

    private final QSMarketPlus plugin;
    private FileConfiguration categoriesConfig;
    private final Map<String, ShopCategory> categories = new LinkedHashMap<>();

    // CONFIG DEL MENÚ
    public String menuTitle = "<green>QSMarketPlus</green>";
    public int menuSize = 0; // 0 → dinámico
    public int closeSlot = 49;
    public int sellAllSlot = 51;

    public CategoryManager(QSMarketPlus plugin) {
        this.plugin = plugin;
        loadCategories();
    }

    public void reload() {
        loadCategories();
    }

    public void loadCategories() {

        File file = new File(plugin.getDataFolder(), "categories.yml");

        if (!file.exists()) {
            plugin.saveResource("categories.yml", false);
        }

        categoriesConfig = YamlConfiguration.loadConfiguration(file);
        categories.clear();

        // ====================================================
        //            LEER CONFIGURACIÓN DEL MENÚ
        // ====================================================
        if (categoriesConfig.isConfigurationSection("menu")) {
            menuTitle = categoriesConfig.getString("menu.title", "<green>QSMarketPlus</green>");
            menuSize = categoriesConfig.getInt("menu.size", 0);

            closeSlot   = categoriesConfig.getInt("menu.close-slot", 49);
            sellAllSlot = categoriesConfig.getInt("menu.sell-all-slot", 51);
        }

        // ====================================================
        //                LEER CATEGORÍAS
        // ====================================================
        ConfigurationSection section = categoriesConfig.getConfigurationSection("categories");

        if (section == null) {
            plugin.getLogger().warning("No se encontró la sección 'categories:' en categories.yml");
            return;
        }

        List<ShopCategory> autoSlot = new ArrayList<>();

        for (String id : section.getKeys(false)) {

            String name = section.getString(id + ".name", "<white>" + id + "</white>");
            String materialName = section.getString(id + ".material", "STONE");
            String texture = section.getString(id + ".head", null);
            String requiredPermission = section.getString(id + ".required_permission", null);
            String requiredGroup = section.getString(id + ".required_group", null);

            Material mat = Material.matchMaterial(materialName);
            if (mat == null) mat = Material.STONE;

            int slot = section.getInt(id + ".slot", -1);
            List<String> lore = section.getStringList(id + ".lore");

            // ==========================================
            // Crear categoría usando el constructor correcto
            // ==========================================
            ShopCategory cat = new ShopCategory(
                    id.toLowerCase(),
                    name,
                    mat,
                    slot,
                    lore,
                    texture,
                    requiredPermission,
                    requiredGroup
            );

            // Slot manual o automático
            if (slot == -1) autoSlot.add(cat);
            else categories.put(id.toLowerCase(), cat);
        }

        // ====================================================
        //     ASIGNACIÓN AUTOMÁTICA DE SLOTS CENTRADOS
        // ====================================================
        if (!autoSlot.isEmpty()) {

            int[] autoSlots = generateCenteredSlots(autoSlot.size());

            for (int i = 0; i < autoSlot.size(); i++) {
                ShopCategory oldCat = autoSlot.get(i);
                // Crear nueva instancia con slot actualizado
                ShopCategory cat = new ShopCategory(
                        oldCat.getId(),
                        oldCat.getName(),
                        oldCat.getMaterial(),
                        autoSlots[i],
                        oldCat.getLore(),
                        oldCat.getHeadTexture(),
                        oldCat.getRequiredPermission(),
                        oldCat.getRequiredGroup()
                );
                categories.put(cat.getId(), cat);
            }
        }
    }

    public FileConfiguration getConfig() {
        return categoriesConfig;
    }

    public ShopCategory getCategory(String id) {
        if (id == null) return null;
        return categories.get(id.toLowerCase());
    }

    public QSMarketPlus getPlugin() {
        return this.plugin;
    }

    private int[] generateCenteredSlots(int count) {

        int size = getInventorySize(count);
        int rows = size / 9;

        List<Integer> usable = new ArrayList<>();

        for (int row = 1; row < rows - 1; row++) {
            for (int col = 1; col <= 7; col++) {
                usable.add(row * 9 + col);
            }
        }

        return usable.stream().limit(count).mapToInt(i -> i).toArray();
    }

    public int getInventorySize(int count) {

        if (menuSize > 0) return menuSize;

        if (count <= 7) return 27;
        if (count <= 14) return 36;
        if (count <= 21) return 45;
        return 54;
    }

    public Map<String, ShopCategory> getCategories() {
        return categories;
    }
}
