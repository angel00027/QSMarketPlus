package mp.quesito.qSMarketPlus.shop;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.holder.ProShopHolder;
import mp.quesito.qSMarketPlus.manager.CategoryManager;
import mp.quesito.qSMarketPlus.manager.ItemManager;
import mp.quesito.qSMarketPlus.utils.MenuItems;
import mp.quesito.qSMarketPlus.utils.MessageUtil;
import mp.quesito.qSMarketPlus.utils.MetaUtil;
import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ProShopMenu {

    private static final int[] ITEM_SLOTS = {
            9,10,11,12,13,14,15,16,17,
            18,19,20,21,22,23,24,25,26,
            27,28,29,30,31,32,33,34,35,
            36,37,38,39,40,41,42,43,44
    };

    public static void open(
            Player player,
            CategoryManager categoryManager,
            ItemManager itemManager,
            int categoryIndex,
            int page
    ) {

        var plugin = QSMarketPlus.getInstance();

        List<ShopCategory> categories =
                new ArrayList<>(categoryManager.getCategories().values());

        if (categories.isEmpty()) return;

        categoryIndex = Math.max(0, Math.min(categoryIndex, categories.size() - 1));

        ShopCategory current = categories.get(categoryIndex);

        // =============================
        // CALCULAR PÁGINAS
        // =============================

        List<ShopItem> items =
                new ArrayList<>(itemManager.getOrderedItems(current.getId()));

        int perPage = ITEM_SLOTS.length;

        int maxPage = (int) Math.ceil((double) items.size() / perPage);

        if (maxPage <= 0) maxPage = 1;

        page = Math.max(0, Math.min(page, maxPage - 1));

        // =============================
        // TÍTULO DESDE CONFIG
        // =============================

        String title = plugin.getConfig()
                .getString("pro-menu.title", "<dark_gray>Shop");

        title = title
                .replace("%category%", current.getName())
                .replace("%page%", String.valueOf(page + 1))
                .replace("%max_page%", String.valueOf(maxPage));

        title = MessageUtil.toLegacy(title);

        // =============================
        // TAMAÑO CONFIGURABLE
        // =============================

        int size = plugin.getConfig().getInt("pro-menu.size", 54);

        Inventory inv = Bukkit.createInventory(
                new ProShopHolder(categoryIndex, page),
                size,
                title
        );

        // =============================
        // CARGAR CONTENIDO
        // =============================

        loadCategories(inv, categories, categoryIndex, itemManager);

        loadItems(player, inv, itemManager, current, page);

        loadNavigation(inv, itemManager, page, items.size());

        player.openInventory(inv);
    }

    // =====================================================
    // CATEGORÍAS
    // =====================================================

    private static void loadCategories(
            Inventory inv,
            List<ShopCategory> categories,
            int index,
            ItemManager itemManager
    ) {

        int start = Math.max(0, index - 3);
        int end = Math.min(categories.size(), start + 7);

        int slot = 1;

        for (int i = start; i < end; i++) {

            ShopCategory cat = categories.get(i);

            ItemStack item = buildCategoryIcon(cat, i == index);

            inv.setItem(slot++, item);
        }

        var config = itemManager.getItemsMenuConfig();

        // PREV CATEGORY
        if (index > 0) {

            int prevSlot = MenuItems.getButtonSlot(config, "prev-category");

            if (prevSlot >= 0) {
                inv.setItem(
                        prevSlot,
                        MenuItems.getButton(config, "prev-category")
                );
            }
        }

        // NEXT CATEGORY
        if (index < categories.size() - 1) {

            int nextSlot = MenuItems.getButtonSlot(config, "next-category");

            if (nextSlot >= 0) {
                inv.setItem(
                        nextSlot,
                        MenuItems.getButton(config, "next-category")
                );
            }
        }
    }
    // =====================================================
    // ICONO CATEGORÍA
    // =====================================================

    private static ItemStack buildCategoryIcon(ShopCategory cat, boolean selected) {

        ItemStack item;

        if (cat.getHeadTexture() != null && !cat.getHeadTexture().isEmpty()) {
            item = MenuItems.customHead(cat.getName(), cat.getHeadTexture());
        } else {
            item = new ItemStack(cat.getMaterial());
        }

        ItemMeta meta = item.getItemMeta();

        if (meta != null) {

            meta.setDisplayName(MessageUtil.toLegacy(cat.getName()));

            List<String> lore = new ArrayList<>();

            for (String line : cat.getLore()) {
                lore.add(MessageUtil.toLegacy(line));
            }

            meta.setLore(lore);

            MetaUtil.setTag(meta, "category", cat.getId());

            // ⭐ ENCANTAMIENTO PARA CATEGORÍA SELECCIONADA
            if (selected) {

                meta.setDisplayName("§e▶ " + MessageUtil.toLegacy(cat.getName()));
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    // =====================================================
    // ITEMS
    // =====================================================
    private static void loadItems(
            Player player,
            Inventory inv,
            ItemManager itemManager,
            ShopCategory category,
            int page
    ) {

        List<ShopItem> items =
                new ArrayList<>(itemManager.getOrderedItems(category.getId()));

        int perPage = ITEM_SLOTS.length;

        int start = page * perPage;
        int end = Math.min(start + perPage, items.size());

        int slotIndex = 0;

        for (int i = start; i < end; i++) {

            ShopItem item = items.get(i);

            // ⭐ USAR EL MISMO BUILDER DEL MENU NORMAL
            ItemStack stack = ItemsMenu.buildMenuItem(player, item, itemManager);

            inv.setItem(ITEM_SLOTS[slotIndex++], stack);
        }
    }
    // =====================================================
    // NAVEGACIÓN
    // =====================================================

    private static void loadNavigation(
            Inventory inv,
            ItemManager itemManager,
            int page,
            int totalItems
    ) {

        var config = itemManager.getItemsMenuConfig();

        int perPage = ITEM_SLOTS.length;
        int maxPage = (int) Math.ceil((double) totalItems / perPage) - 1;

        if (page > 0) {

            int slot = MenuItems.getButtonSlot(config, "prev-page");

            inv.setItem(
                    slot,
                    MenuItems.getButton(config, "prev-page")
            );
        }

        int backSlot = MenuItems.getButtonSlot(config, "back");

        inv.setItem(
                backSlot,
                MenuItems.getButton(config, "back")
        );

        if (page < maxPage) {

            int slot = MenuItems.getButtonSlot(config, "next-page");

            inv.setItem(
                    slot,
                    MenuItems.getButton(config, "next-page")
            );
        }
    }
}