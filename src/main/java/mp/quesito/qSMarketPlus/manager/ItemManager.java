package mp.quesito.qSMarketPlus.manager;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.shop.ShopCategory;
import mp.quesito.qSMarketPlus.shop.ShopItem;
import mp.quesito.qSMarketPlus.utils.ItemSerializer;
import mp.quesito.qSMarketPlus.utils.MenuItems;
import mp.quesito.qSMarketPlus.utils.MessageUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ItemManager {

    private final QSMarketPlus plugin;

    private final Map<String, Map<String, ShopItem>> itemsRawByCategory = new HashMap<>();
    private final Map<String, List<ShopItem>> itemsSortedByCategory = new HashMap<>();

    private FileConfiguration itemsMenuConfig;
    private File itemsMenuFile;

    public ItemManager(QSMarketPlus plugin) {
        this.plugin = plugin;

        File folder = new File(plugin.getDataFolder(), "items");
        if (!folder.exists()) folder.mkdirs();

        loadItemsMenuConfig();
    }

    // ================= MENU =================

    public void loadItemsMenuConfig() {
        itemsMenuFile = new File(plugin.getDataFolder(), "items-menu.yml");

        if (!itemsMenuFile.exists()) {
            plugin.saveResource("items-menu.yml", false);
        }

        itemsMenuConfig = YamlConfiguration.loadConfiguration(itemsMenuFile);
    }


    public boolean hasSortedList(String categoryId) {
        return itemsSortedByCategory.containsKey(categoryId)
                && !itemsSortedByCategory.get(categoryId).isEmpty();
    }


    public FileConfiguration getItemsMenuConfig() {
        return itemsMenuConfig;
    }

    public void reloadItemsMenuConfig() {
        loadItemsMenuConfig();
    }

    // ================= BUSCAR ITEM POR ID =================
    public ShopItem getItemById(String itemId) {
        if (itemId == null) return null;

        for (Map<String, ShopItem> categoryMap : itemsRawByCategory.values()) {
            for (ShopItem item : categoryMap.values()) {
                if (item.getId().equalsIgnoreCase(itemId)) {
                    return item;
                }
            }
        }
        return null;
    }

    // ================= ITEMS =================
    public void loadCategoryItems(ShopCategory category) {

        String categoryId = category.getId();
        String fileName = "items/" + categoryId + ".yml";
        File file = new File(plugin.getDataFolder(), fileName);

        // Limpiamos la lista ordenada para recargar
        itemsSortedByCategory.remove(categoryId);

        if (!file.exists()) {
            if (plugin.getResource(fileName) != null) {
                plugin.saveResource(fileName, false);
            } else {
                createEmptyYML(file);
            }
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("items");

        Map<String, ShopItem> rawMap = new LinkedHashMap<>();

        if (section == null) {
            config.createSection("items");
            saveYML(config, file);
            itemsRawByCategory.put(categoryId, rawMap);
            return;
        }

        for (String id : section.getKeys(false)) {

            String name = section.getString(id + ".name", "Item");
            String matName = section.getString(id + ".material", "STONE");
            Material mat = Material.matchMaterial(matName);
            if (mat == null) mat = Material.STONE;

            String texture = section.getString(id + ".texture");
            double buy = section.getDouble(id + ".buy", 1);
            double sell = section.getDouble(id + ".sell", 0);
            List<String> lore = section.getStringList(id + ".lore");

            // ================= ItemStack =================
            ItemStack itemStack;

            // Primero intentamos cargar desde Base64 si existe
            String base64 = section.getString(id + ".itemstack");
            if (base64 != null && !base64.isEmpty()) {
                itemStack = ItemSerializer.fromBase64(base64);
                if (itemStack == null) {
                    // fallback visual
                    itemStack = new ItemStack(mat);
                }
            } else {
                // fallback: crear desde material, nombre y lore
                if (mat == Material.PLAYER_HEAD && texture != null && !texture.isEmpty()) {
                    itemStack = MenuItems.customHead(name, texture);
                } else {
                    itemStack = new ItemStack(mat);
                }

                ItemMeta meta = itemStack.getItemMeta();
                meta.setDisplayName(MessageUtil.toLegacy(name));

                if (!lore.isEmpty()) {
                    List<String> coloredLore = new ArrayList<>();
                    for (String line : lore) {
                        coloredLore.add(MessageUtil.toLegacy(line));
                    }
                    meta.setLore(coloredLore);
                }

                itemStack.setItemMeta(meta);
            }

            // ================= ShopItem =================
            ShopItem shopItem = new ShopItem(
                    id,
                    name,
                    buy,
                    sell,
                    ItemSerializer.toBase64(itemStack) // Guardamos Base64 completo
            );

            // ================= PROPIEDADES =================
            shopItem.setOnlyOnce(section.getBoolean(id + ".only_once", false));
            shopItem.setPermission(section.getString(id + ".permission"));
            shopItem.setRequiredPermission(section.getString(id + ".required_permission"));
            shopItem.setRequiredGroup(section.getString(id + ".required_group"));
            shopItem.setCommands(section.getStringList(id + ".commands"));
            shopItem.setSellCommands(section.getStringList(id + ".sell_commands"));

            rawMap.put(id, shopItem);
        }

        itemsRawByCategory.put(categoryId, rawMap);
    }


    // ================= UTIL =================

    private void createEmptyYML(File file) {
        YamlConfiguration config = new YamlConfiguration();
        config.createSection("items");
        saveYML(config, file);
    }

    private void saveYML(YamlConfiguration config, File file) {
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ================= GETTERS =================

    public List<ShopItem> getOrderedItems(String categoryId) {

        if (itemsSortedByCategory.containsKey(categoryId)) {
            return new ArrayList<>(itemsSortedByCategory.get(categoryId));
        }

        Map<String, ShopItem> raw = itemsRawByCategory.get(categoryId);
        if (raw == null) return new ArrayList<>();

        return new ArrayList<>(raw.values());
    }

    public ShopItem getItem(String categoryId, String itemId) {
        Map<String, ShopItem> map = itemsRawByCategory.get(categoryId);
        return map != null ? map.get(itemId) : null;
    }

    public Map<String, ShopItem> getItems(String categoryId) {
        return itemsRawByCategory.getOrDefault(categoryId, new HashMap<>());
    }

    public void setSortedItems(String categoryId, List<ShopItem> sorted) {
        itemsSortedByCategory.put(categoryId, new ArrayList<>(sorted));
    }

    public void clearSortedItems(String categoryId) {
        itemsSortedByCategory.remove(categoryId);
    }
}
