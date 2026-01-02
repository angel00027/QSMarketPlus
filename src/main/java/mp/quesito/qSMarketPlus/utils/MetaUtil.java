package mp.quesito.qSMarketPlus.utils;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class MetaUtil {

    // Para evitar recrear claves miles de veces
    public static NamespacedKey key(String id) {
        return new NamespacedKey(QSMarketPlus.getInstance(), id);
    }

    // ================================
    //      ESTABLECER TAG
    // ================================
    public static void setTag(ItemMeta meta, String tag, String value) {
        meta.getPersistentDataContainer().set(
                key(tag),
                PersistentDataType.STRING,
                value
        );
    }

    // ================================
    //      OBTENER TAG
    // ================================
    public static String getTag(ItemMeta meta, String tag) {
        return meta.getPersistentDataContainer().get(
                key(tag),
                PersistentDataType.STRING
        );
    }
    public static void setInt(ItemMeta meta, String key, int value) {
        NamespacedKey k = new NamespacedKey(QSMarketPlus.getInstance(), key);
        meta.getPersistentDataContainer().set(k, PersistentDataType.INTEGER, value);
    }

    public static Integer getInt(ItemMeta meta, String key) {
        NamespacedKey k = new NamespacedKey(QSMarketPlus.getInstance(), key);
        if (!meta.getPersistentDataContainer().has(k, PersistentDataType.INTEGER))
            return null;
        return meta.getPersistentDataContainer().get(k, PersistentDataType.INTEGER);
    }

    // ================================
    //  VERIFICAR TAG (CUALQUIERA)
    // ================================
    public static boolean is(ItemStack item, String expectedValue) {

        if (item == null || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();

        // Buscar SOLO claves "btn"
        String btn = getTag(meta, "btn");

        return btn != null && btn.equalsIgnoreCase(expectedValue);
    }

    // ================================
    //  LIMPIAR CÓDIGOS DE COLOR
    // ================================
    public static String clean(String s) {
        return s == null ? "" : s.replaceAll("§[0-9A-FK-ORa-fk-or]", "");
    }
}
