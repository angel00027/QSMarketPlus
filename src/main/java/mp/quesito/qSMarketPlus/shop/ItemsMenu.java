package mp.quesito.qSMarketPlus.shop;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.holder.ItemsHolder;
import mp.quesito.qSMarketPlus.manager.ItemManager;
import mp.quesito.qSMarketPlus.manager.UniquePurchaseManager;
import mp.quesito.qSMarketPlus.utils.Lang;
import mp.quesito.qSMarketPlus.utils.MenuItems;
import mp.quesito.qSMarketPlus.utils.MessageUtil;
import mp.quesito.qSMarketPlus.utils.MetaUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.function.Consumer;

public final class ItemsMenu {

    private static final int[] ITEM_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    // =====================================================
    // OPEN MENU
    // =====================================================
    public static void open(Player player, ShopCategory category, ItemManager manager, int page) {

        FileConfiguration cfg = manager.getItemsMenuConfig();

        int rows = cfg.getInt("menu.rows", 6);
        int size = rows * 9;

        List<ShopItem> items = new ArrayList<>(manager.getOrderedItems(category.getId()));

        // =================================================
        // ORDENADO
        // =================================================
        if (!manager.hasSortedList(category.getId())) {

            String sort = cfg.getString("menu.default-sort", "name").toLowerCase();

            switch (sort) {
                case "price", "price_asc" ->
                        items.sort(Comparator.comparingDouble(ShopItem::getBuy));

                case "price_desc" ->
                        items.sort((a, b) -> Double.compare(b.getBuy(), a.getBuy()));

                case "none" -> {
                    // sin orden
                }

                default ->
                        items.sort(Comparator.comparing(i ->
                                ChatColor.stripColor(MessageUtil.toLegacy(i.getName()))
                        ));
            }

        }

        int totalItems = items.size();
        int perPage = ITEM_SLOTS.length;
        int maxPage = Math.max(1, (int) Math.ceil((double) totalItems / perPage));

        page = Math.max(1, Math.min(page, maxPage));

        // =================================================
        // TITLE
        // =================================================
        String title = cfg.getString("menu.title", "<green>{category}</green>")
                .replace("{category}", category.getName())
                .replace("{page}", String.valueOf(page))
                .replace("{max}", String.valueOf(maxPage))
                .replace("{items}", String.valueOf(totalItems));

        Inventory inv = Bukkit.createInventory(
                new ItemsHolder(player, category, page, size),
                size,
                MessageUtil.toLegacy(title)
        );

        // =================================================
        // DECORACIÓN
        // =================================================
        ItemStack glass = MenuItems.glass();
        for (int i = 0; i < size; i++) {
            if (i < 9 || i >= size - 9) {
                inv.setItem(i, glass);
            }
        }

        // =================================================
        // ITEMS
        // =================================================
        int start = (page - 1) * perPage;
        int end = Math.min(start + perPage, totalItems);
        int slotIndex = 0;

        for (int i = start; i < end; i++) {
            ShopItem item = items.get(i);
            ItemStack stack = buildMenuItem(player, item, manager);
            inv.setItem(ITEM_SLOTS[slotIndex++], stack);
        }

        // =================================================
        // BOTONES
        // =================================================
        applyButton(cfg, inv, "buttons.sort-asc", m -> m.put("btn", "sort_price_asc"));
        applyButton(cfg, inv, "buttons.sort-desc", m -> m.put("btn", "sort_price_desc"));
        applyButton(cfg, inv, "buttons.sort-name", m -> m.put("btn", "sort_name"));
        applyButton(cfg, inv, "buttons.back", m -> m.put("btn", "back"));

        if (page < maxPage)
            applyButton(cfg, inv, "buttons.next-page", m -> m.put("btn", "next_page"));

        if (page > 1)
            applyButton(cfg, inv, "buttons.prev-page", m -> m.put("btn", "prev_page"));

        player.openInventory(inv);
    }

    // =====================================================
    // BUILD ITEM
    // =====================================================
    private static ItemStack buildMenuItem(Player player, ShopItem item, ItemManager manager) {

        ItemStack stack = item.getRealItem();
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;

        // Nombre del ítem
        meta.setDisplayName(MessageUtil.toLegacy(item.getName()));

        List<String> lore = new ArrayList<>();

        // =========================
        // Lore original del item
        // =========================
        if (meta.hasLore()) {
            meta.getLore().forEach(line -> {
                if (line != null) lore.add(MessageUtil.toLegacy(line));
            });
            lore.add(""); // Separador
        }

        // =========================
        // Lore definido en ShopItem
        // =========================
        for (String line : item.getLore()) {
            if (line != null && !line.trim().isEmpty()) {
                lore.add(MessageUtil.toLegacy(line));
            }
        }

        // =========================
        // Verificar acceso
        // =========================
        boolean blocked = false;
        if (!item.canAccess(player)) {
            String msg = Lang.get("item_unavailable"); // usa YAML
            if (msg == null) msg = "<red>⛔ No disponible</red>";
            lore.add(MessageUtil.toLegacy(msg));
            blocked = true;
        }

        // =========================
        // Verificar si ya compró (item único)
        // =========================
        UniquePurchaseManager upManager = QSMarketPlus.getInstance().getUniquePurchaseManager();
        if (item.isOnlyOnce() && upManager.hasPurchased(player, item)) {
            String msg = Lang.get("item_already_bought");
            if (msg == null) msg = "<gray>✅ Ya comprado</gray>";
            lore.add(MessageUtil.toLegacy(msg));
            blocked = true;
        }

        // =========================
        // Mostrar precios solo si el ítem es accesible y no está bloqueado
        // =========================
        if (!blocked) {
            String buy = MessageUtil.priceFormat("buy", item.getBuy(), item.getSell());
            String sell = MessageUtil.priceFormat("sell", item.getBuy(), item.getSell());

            if (!buy.isEmpty()) lore.add(buy);
            if (!sell.isEmpty()) lore.add(sell);
        }

        // =========================
        // Tag interno para identificar el item en el menú
        // =========================
        MetaUtil.setTag(meta, "shop_item", item.getId());

        meta.setLore(lore);
        stack.setItemMeta(meta);

        return stack;
    }



    // =====================================================
    // BOTONES
    // =====================================================
    private static void applyButton(
            FileConfiguration cfg,
            Inventory inv,
            String path,
            Consumer<Map<String, String>> tagSetter
    ) {

        if (!cfg.isConfigurationSection(path)) return;

        ConfigurationSection sec = cfg.getConfigurationSection(path);
        ItemStack button = MenuItems.buttonFromConfig(sec);
        if (button == null) return;

        ItemMeta meta = button.getItemMeta();
        Map<String, String> tags = new HashMap<>();

        tagSetter.accept(tags);
        tags.forEach((k, v) -> MetaUtil.setTag(meta, k, v));

        button.setItemMeta(meta);

        int slot = sec.getInt("slot", -1);
        if (slot >= 0) inv.setItem(slot, button);
    }

    // =====================================================
    // REFRESH
    // =====================================================
    public static void refreshPage(
            Player player,
            ShopCategory category,
            ItemManager manager,
            int page,
            List<ShopItem> sorted
    ) {
        manager.setSortedItems(category.getId(), sorted);
        open(player, category, manager, page);
    }
}
