package mp.quesito.qSMarketPlus.editor;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.manager.CategoryManager;
import mp.quesito.qSMarketPlus.manager.ItemManager;
import mp.quesito.qSMarketPlus.shop.ShopCategory;
import mp.quesito.qSMarketPlus.shop.ShopItem;
import mp.quesito.qSMarketPlus.utils.MenuItems;
import mp.quesito.qSMarketPlus.utils.MessageUtil;
import mp.quesito.qSMarketPlus.utils.MetaUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ShopEditorMenu {

    private static final int ITEMS_PER_PAGE = 36; // filas 1-4 en un menú de 54

    // =====================================================
    //                    MENÚ PRINCIPAL
    // =====================================================
    public static void main(Player player) {
        AdminHolder holder = new AdminHolder();
        holder.mode = "main";

        Inventory inv = Bukkit.createInventory(holder, 9,
                MessageUtil.toLegacy("<dark_gray>Editor » <yellow>Tienda</yellow>"));

        fillGlassSafe(inv, new int[]{0, 1, 3, 5, 7, 8});

        inv.setItem(2, editButton(Material.CHEST, "<yellow>Categorías</yellow>",
                List.of("<gray>Crear, renombrar, cambiar", "<gray>material, slot y permisos."),
                "cat_list", null, null));
        inv.setItem(4, editButton(Material.GREEN_WOOL, "<green>Recargar tienda</green>",
                List.of("<gray>Vuelve a cargar categorías", "<gray>e ítems desde los archivos."),
                "reload", null, null));
        inv.setItem(6, editButton(Material.BARRIER, "<red>Cerrar</red>",
                List.of(), "close", null, null));

        player.openInventory(inv);
    }

    // =====================================================
    //                LISTA DE CATEGORÍAS
    // =====================================================
    public static void categories(Player player, int page) {
        AdminHolder holder = new AdminHolder();
        holder.mode = "categories";
        holder.page = page;

        Inventory inv = Bukkit.createInventory(holder, 54,
                MessageUtil.toLegacy("<dark_gray>Editor » <yellow>Categorías</yellow>"));

        fillGlassSafe(inv, null);

        List<ShopCategory> cats = new ArrayList<>(catManager().getCategories().values());
        int perPage = ITEMS_PER_PAGE;
        int maxPage = Math.max(1, (int) Math.ceil(cats.size() / (double) perPage));
        page = Math.max(0, Math.min(page, maxPage - 1));
        holder.page = page;

        int start = page * perPage;
        int end = Math.min(start + perPage, cats.size());
        int slot = 9;
        for (int i = start; i < end; i++) {
            ShopCategory cat = cats.get(i);
            inv.setItem(slot++, categoryIcon(cat));
        }

        // Botones
        inv.setItem(45, editButton(Material.ARROW, "<yellow>← Volver</yellow>",
                List.of("<gray>Menú principal"), "main_back", null, null));
        inv.setItem(49, editButton(Material.EMERALD, "<green>+ Añadir categoría</green>",
                List.of("<gray>Crea una categoría nueva.", "<gray>Se te pedirá el ID en el chat."),
                "cat_add", null, null));
        inv.setItem(52, navButton(page > 0, true, "categories"));
        inv.setItem(53, navButton(page < maxPage - 1, false, "categories"));

        player.openInventory(inv);
    }

    // =====================================================
    //              EDITAR UNA CATEGORÍA
    // =====================================================
    public static void categoryEdit(Player player, String catId) {
        ShopCategory cat = catManager().getCategory(catId);
        if (cat == null) {
            MessageUtil.msg(player, "<red>La categoría no existe.</red>");
            return;
        }

        AdminHolder holder = new AdminHolder();
        holder.mode = "category";
        holder.categoryId = catId;

        Inventory inv = Bukkit.createInventory(holder, 27,
                MessageUtil.toLegacy("<dark_gray>Editor » <yellow>" + cat.getName() + "</yellow>"));

        fillGlassSafe(inv, null);

        inv.setItem(10, editButton(Material.NAME_TAG, "<yellow>Renombrar</yellow>",
                List.of("<gray>Actual: <white>" + cat.getName() + "</white>", "<gray>Escribe el nombre en el chat."),
                "cat_name", catId, null));
        inv.setItem(11, editButton(Material.STONE, "<yellow>Material</yellow>",
                List.of("<gray>Actual: <white>" + cat.getMaterial().name() + "</white>"),
                "cat_material", catId, null));
        inv.setItem(12, editButton(Material.CHEST, "<yellow>Slot</yellow>",
                List.of("<gray>Actual: <white>" + (cat.getSlot() == -1 ? "auto" : cat.getSlot()) + "</white>"),
                "cat_slot", catId, null));
        inv.setItem(13, categoryInfo(cat));
        inv.setItem(14, editButton(Material.PAPER, "<yellow>Permiso requerido</yellow>",
                List.of("<gray>Actual: <white>" + or(cat.getRequiredPermission(), "ninguno")
                        + "</white>", "<gray>Vacío = todos."),
                "cat_perm", catId, null));
        inv.setItem(15, editButton(Material.PLAYER_HEAD, "<yellow>Grupo requerido</yellow>",
                List.of("<gray>Actual: <white>" + or(cat.getRequiredGroup(), "ninguno")
                        + "</white>", "<gray>Vacío = todos."),
                "cat_group", catId, null));
        inv.setItem(16, editButton(Material.DIAMOND, "<green>Ítems de la categoría</green>",
                List.of("<gray>Ver, añadir, editar", "<gray>y eliminar ítems."),
                "cat_items", catId, null));

        inv.setItem(18, editButton(Material.ARROW, "<yellow>← Volver</yellow>",
                List.of("<gray>Categorías"), "cat_back", null, null));
        inv.setItem(22, editButton(Material.RED_WOOL, "<red>Eliminar categoría</red>",
                List.of("<red>Borra la categoría y su", "<red>archivo de ítems.</red>"),
                "cat_delete", catId, null));

        player.openInventory(inv);
    }

    // =====================================================
    //               LISTA DE ÍTEMS (paginar)
    // =====================================================
    public static void items(Player player, String catId, int page) {
        if (catManager().getCategory(catId) == null) {
            MessageUtil.msg(player, "<red>La categoría no existe.</red>");
            return;
        }

        AdminHolder holder = new AdminHolder();
        holder.mode = "items";
        holder.categoryId = catId;
        holder.page = page;

        Inventory inv = Bukkit.createInventory(holder, 54,
                MessageUtil.toLegacy("<dark_gray>Editor » <gold>Ítems</gold> <white>(" + catId + ")</white>"));

        fillGlassSafe(inv, null);

        List<ShopItem> items = itemManager().getOrderedItems(catId);
        int perPage = ITEMS_PER_PAGE;
        int maxPage = Math.max(1, (int) Math.ceil(items.size() / (double) perPage));
        page = Math.max(0, Math.min(page, maxPage - 1));
        holder.page = page;

        int start = page * perPage;
        int end = Math.min(start + perPage, items.size());
        int slot = 9;
        for (int i = start; i < end; i++) {
            inv.setItem(slot++, itemIcon(catId, items.get(i), itemManager()));
        }

        inv.setItem(45, editButton(Material.ARROW, "<yellow>← Volver</yellow>",
                List.of("<gray>Editar categoría"), "items_back", catId, null));
        inv.setItem(49, editButton(Material.EMERALD, "<green>+ Añadir ítem (mano)</green>",
                List.of("<gray>Añade el ítem que tienes", "<gray>en la mano a esta categoría."),
                "item_add", catId, null));
        inv.setItem(52, navButton(page > 0, true, "items"));
        inv.setItem(53, navButton(page < maxPage - 1, false, "items"));

        player.openInventory(inv);
    }

    // =====================================================
    //              EDITAR UN ÍTEM
    // =====================================================
    public static void itemEdit(Player player, String catId, String itemId) {
        ShopItem item = itemManager().getItem(catId, itemId);
        if (item == null) {
            MessageUtil.msg(player, "<red>El ítem no existe.</red>");
            return;
        }

        AdminHolder holder = new AdminHolder();
        holder.mode = "item";
        holder.categoryId = catId;
        holder.itemId = itemId;

        Inventory inv = Bukkit.createInventory(holder, 27,
                MessageUtil.toLegacy("<dark_gray>Editor » <white>" + item.getName() + "</white>"));

        fillGlassSafe(inv, null);

        inv.setItem(10, editButton(Material.GOLD_INGOT, "<green>Precio de compra</green>",
                List.of("<gray>Actual: <white>" + fmt(item.getBuy()) + "</white>"),
                "it_buy", catId, itemId));
        inv.setItem(11, editButton(Material.REDSTONE, "<red>Precio de venta</red>",
                List.of("<gray>Actual: <white>" + fmt(item.getSell()) + "</white>"),
                "it_sell", catId, itemId));
        inv.setItem(12, editButton(Material.EMERALD, "<yellow>Moneda</yellow>",
                List.of("<gray>Actual: <white>" + item.getEconomy() + "</white>"),
                "it_eco", catId, itemId));
        inv.setItem(13, itemInfoIcon(item, itemManager()));
        inv.setItem(14, editButton(Material.NAME_TAG, "<yellow>Renombrar</yellow>",
                List.of("<gray>Escribe un nuevo nombre."), "it_name", catId, itemId));
        inv.setItem(15, editButton(Material.STONE, "<yellow>Material</yellow>",
                List.of("<gray>Actual: <white>" + item.getMaterial().name() + "</white>",
                        "<gray>Restablece el ítem como un", "<gray>objeto simple de ese material."),
                "it_mat", catId, itemId));
        inv.setItem(16, editButton(Material.RED_WOOL, "<red>Eliminar ítem</red>",
                List.of("<red>Borra este ítem de la tienda."), "it_delete", catId, itemId));

        inv.setItem(18, editButton(Material.ARROW, "<yellow>← Volver</yellow>",
                List.of("<gray>Ítems de la categoría"), "it_back", catId, itemId));

        player.openInventory(inv);
    }

    // =====================================================
    //                     HELPERS
    // =====================================================
    private static void fillGlassSafe(Inventory inv, int[] except) {
        for (int i = 0; i < inv.getSize(); i++) {
            if (contains(except, i)) continue;
            inv.setItem(i, MenuItems.glass());
        }
    }

    private static boolean contains(int[] arr, int i) {
        if (arr == null) return false;
        for (int v : arr) if (v == i) return true;
        return false;
    }

    private static ItemStack categoryIcon(ShopCategory cat) {
        ItemStack icon;
        if (cat.getHeadTexture() != null && !cat.getHeadTexture().isEmpty()) {
            icon = MenuItems.customHead(" ", cat.getHeadTexture());
        } else {
            icon = new ItemStack(cat.getMaterial());
        }

        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(MessageUtil.toLegacy("<yellow>" + cat.getName() + "</yellow>"));

        List<String> lore = new ArrayList<>();
        lore.add(MessageUtil.toLegacy("<gray>ID: <white>" + cat.getId() + "</white>"));
        lore.add(MessageUtil.toLegacy("<gray>Slot: <white>" + (cat.getSlot() == -1 ? "auto" : cat.getSlot()) + "</white>"));
        if (cat.getRequiredPermission() != null && !cat.getRequiredPermission().isEmpty()) {
            lore.add(MessageUtil.toLegacy("<gray>Permiso: <yellow>" + cat.getRequiredPermission() + "</yellow>"));
        }
        if (cat.getRequiredGroup() != null && !cat.getRequiredGroup().isEmpty()) {
            lore.add(MessageUtil.toLegacy("<gray>Grupo: <aqua>" + cat.getRequiredGroup() + "</aqua>"));
        }
        lore.add("");
        lore.add(MessageUtil.toLegacy("<green>Click para editar</green>"));
        meta.setLore(lore);

        MetaUtil.setTag(meta, "btn", "cat_item");
        MetaUtil.setTag(meta, "value", cat.getId());
        icon.setItemMeta(meta);
        return icon;
    }

    private static ItemStack categoryInfo(ShopCategory cat) {
        ItemStack icon;
        if (cat.getHeadTexture() != null && !cat.getHeadTexture().isEmpty()) {
            icon = MenuItems.customHead(" ", cat.getHeadTexture());
        } else {
            icon = new ItemStack(cat.getMaterial());
        }
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(MessageUtil.toLegacy("<green>" + cat.getName() + "</green>"));
        List<String> lore = new ArrayList<>();
        lore.add(MessageUtil.toLegacy("<gray>ID: <white>" + cat.getId() + "</white>"));
        lore.add(MessageUtil.toLegacy("<gray>Slot: <white>" + (cat.getSlot() == -1 ? "auto" : cat.getSlot()) + "</white>"));
        lore.add(MessageUtil.toLegacy("<gray>Material: <white>" + cat.getMaterial().name() + "</white>"));
        if (cat.getRequiredPermission() != null && !cat.getRequiredPermission().isEmpty()) {
            lore.add(MessageUtil.toLegacy("<gray>Permiso: <yellow>" + cat.getRequiredPermission() + "</yellow>"));
        }
        if (cat.getRequiredGroup() != null && !cat.getRequiredGroup().isEmpty()) {
            lore.add(MessageUtil.toLegacy("<gray>Grupo: <aqua>" + cat.getRequiredGroup() + "</aqua>"));
        }
        meta.setLore(lore);
        icon.setItemMeta(meta);
        return icon;
    }

    private static ItemStack itemIcon(String catId, ShopItem item, ItemManager manager) {
        ItemStack icon = item.getRealItem().clone();
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(MessageUtil.toLegacy("<white>" + item.getName() + "</white>"));

        List<String> lore = new ArrayList<>();
        lore.add(MessageUtil.toLegacy("<gray>ID: <white>" + item.getId() + "</white>"));
        lore.add("");
        lore.add(MessageUtil.toLegacy("<green>Comprar: <white>" + fmt(item.getBuy()) + "</white>"));
        lore.add(MessageUtil.toLegacy("<red>Vender: <white>" + fmt(item.getSell()) + "</white>"));
        lore.add(MessageUtil.toLegacy("<gray>Moneda: <yellow>" + item.getEconomy() + "</yellow>"));
        lore.add("");
        lore.add(MessageUtil.toLegacy("<green>Click para editar</green>"));
        meta.setLore(lore);

        MetaUtil.setTag(meta, "btn", "item_item");
        MetaUtil.setTag(meta, "value", catId);
        MetaUtil.setTag(meta, "item", item.getId());
        icon.setItemMeta(meta);
        return icon;
    }

    private static ItemStack itemInfoIcon(ShopItem item, ItemManager manager) {
        ItemStack icon = item.getRealItem().clone();
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(MessageUtil.toLegacy("<green>" + item.getName() + "</green>"));
        List<String> lore = new ArrayList<>();
        lore.add(MessageUtil.toLegacy("<gray>ID: <white>" + item.getId() + "</white>"));
        lore.add(MessageUtil.toLegacy("<gray>Material: <white>" + item.getMaterial().name() + "</white>"));
        lore.add(MessageUtil.toLegacy("<green>Comprar: <white>" + fmt(item.getBuy()) + "</white>"));
        lore.add(MessageUtil.toLegacy("<red>Vender: <white>" + fmt(item.getSell()) + "</white>"));
        lore.add(MessageUtil.toLegacy("<gray>Moneda: <yellow>" + item.getEconomy() + "</yellow>"));
        meta.setLore(lore);
        icon.setItemMeta(meta);
        return icon;
    }

    private static ItemStack editButton(Material mat, String name, List<String> lore,
                                        String action, String value, String item) {
        ItemStack icon = new ItemStack(mat);
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(MessageUtil.toLegacy(name));
        if (lore != null && !lore.isEmpty()) {
            meta.setLore(MessageUtil.loreToLegacy(lore));
        }
        MetaUtil.setTag(meta, "btn", action);
        if (value != null) MetaUtil.setTag(meta, "value", value);
        if (item != null) MetaUtil.setTag(meta, "item", item);
        icon.setItemMeta(meta);
        return icon;
    }

    private static ItemStack navButton(boolean enabled, boolean prev, String context) {
        ItemStack icon = new ItemStack(Material.ARROW);
        ItemMeta meta = icon.getItemMeta();

        if (!enabled) {
            meta.setDisplayName(MessageUtil.toLegacy("<gray>Sin más páginas</gray>"));
        } else {
            meta.setDisplayName(MessageUtil.toLegacy(prev
                    ? "<green>← Anterior</green>"
                    : "<green>Siguiente →</green>"));
            MetaUtil.setTag(meta, "btn", "page_nav");
            MetaUtil.setTag(meta, "value", context);
            MetaUtil.setTag(meta, "item", prev ? "prev" : "next");
        }
        icon.setItemMeta(meta);
        return icon;
    }

    private static String fmt(double d) {
        return d == Math.floor(d)
                ? String.valueOf((long) d)
                : String.format("%.2f", d);
    }

    private static String or(String s, String def) {
        return s == null || s.isEmpty() ? def : s;
    }

    private static CategoryManager catManager() {
        return QSMarketPlus.getInstance().getCategoryManager();
    }

    private static ItemManager itemManager() {
        return QSMarketPlus.getInstance().getItemManager();
    }
}