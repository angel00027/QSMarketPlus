package mp.quesito.qSMarketPlus.utils;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Cuentas de inventario de la tienda. Solo usa el almacenamiento (36 slots):
 * la armadura y la segunda mano nunca se cuentan ni se venden.
 */
public final class ShopInventory {

    private ShopInventory() {
    }

    /** Cuantas unidades de este objeto caben en el inventario. */
    public static int spaceFor(Player player, ItemStack item) {
        int maxStack = item.getMaxStackSize();
        int free = 0;

        for (ItemStack slot : player.getInventory().getStorageContents()) {
            if (slot == null || slot.getType().isAir()) {
                free += maxStack;
            } else if (slot.isSimilar(item)) {
                free += Math.max(0, maxStack - slot.getAmount());
            }
        }
        return free;
    }

    /**
     * Si la tienda vende el objeto normal, un objeto con nombre, lore, modelo,
     * encantamientos o datos de otro plugin (Nexo, por ejemplo) no cuenta como ese objeto.
     */
    public static boolean matchesForSale(ItemStack template, ItemStack stack) {
        if (template == null || stack == null || stack.getType().isAir()) return false;

        if (isCustom(template)) {
            return template.isSimilar(stack);
        }
        return template.getType() == stack.getType() && !isCustom(stack);
    }

    private static boolean isCustom(ItemStack stack) {
        if (!stack.hasItemMeta()) return false;

        ItemMeta meta = stack.getItemMeta();
        return meta.hasDisplayName()
                || meta.hasLore()
                || meta.hasCustomModelData()
                || meta.hasEnchants()
                || !meta.getPersistentDataContainer().isEmpty();
    }

    public static int countForSale(Player player, ItemStack template) {
        int count = 0;
        for (ItemStack slot : player.getInventory().getStorageContents()) {
            if (matchesForSale(template, slot)) count += slot.getAmount();
        }
        return count;
    }

    /** Quita en orden de slot, el mismo que muestra la vista previa del menu. */
    public static void removeForSale(Player player, ItemStack template, int amount) {
        ItemStack[] storage = player.getInventory().getStorageContents();

        for (int i = 0; i < storage.length && amount > 0; i++) {
            ItemStack slot = storage[i];
            if (!matchesForSale(template, slot)) continue;

            int have = slot.getAmount();
            if (have <= amount) {
                player.getInventory().setItem(i, null);
                amount -= have;
            } else {
                slot.setAmount(have - amount);
                player.getInventory().setItem(i, slot);
                amount = 0;
            }
        }
    }

    /** Evita totales como 0.6400000000000001 con precios pequeños. */
    public static double money(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
