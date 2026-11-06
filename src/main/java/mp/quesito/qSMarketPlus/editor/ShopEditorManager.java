package mp.quesito.qSMarketPlus.editor;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.manager.CategoryManager;
import mp.quesito.qSMarketPlus.manager.ItemManager;
import mp.quesito.qSMarketPlus.shop.ShopCategory;
import mp.quesito.qSMarketPlus.shop.ShopItem;
import mp.quesito.qSMarketPlus.utils.ItemSerializer;
import mp.quesito.qSMarketPlus.utils.MessageUtil;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lógica del editor de la tienda: persistencia en YAML
 * y procesado de los valores escritos en el chat.
 */
public class ShopEditorManager {

    private static final long INPUT_TIMEOUT_MS = 45_000L;

    private final QSMarketPlus plugin;
    private final Map<UUID, PendingEdit> pending = new HashMap<>();

    public ShopEditorManager(QSMarketPlus plugin) {
        this.plugin = plugin;
    }

    // =====================================================
    //                  PENDIENTES DE CHAT
    // =====================================================
    public static class PendingEdit {
        public final String kind;
        public final String categoryId;
        public final String itemId;
        public final long expiresAt;

        public PendingEdit(String kind, String categoryId, String itemId) {
            this.kind = kind;
            this.categoryId = categoryId;
            this.itemId = itemId;
            this.expiresAt = System.currentTimeMillis() + INPUT_TIMEOUT_MS;
        }
    }

    public void setPending(Player player, String kind, String categoryId, String itemId) {
        pending.put(player.getUniqueId(), new PendingEdit(kind, categoryId, itemId));
    }

    public PendingEdit getPending(UUID uuid) {
        PendingEdit p = pending.get(uuid);
        if (p == null) return null;
        if (System.currentTimeMillis() > p.expiresAt) {
            pending.remove(uuid);
            return null;
        }
        return p;
    }

    public void clearPending(UUID uuid) {
        pending.remove(uuid);
    }

    // =====================================================
    //               ABRIR EL EDITOR
    // =====================================================
    public void openEditor(Player player) {
        ShopEditorMenu.main(player);
    }

    // =====================================================
    //              PROCESAR CHAT (hilo principal)
    // =====================================================
    public void handleChat(Player player, String raw) {
        PendingEdit p = pending.get(player.getUniqueId());
        if (p == null) return;

        String msg = raw.trim();

        if (msg.equalsIgnoreCase("cancelar") || msg.equalsIgnoreCase("cancel") || msg.equalsIgnoreCase("-")) {
            pending.remove(player.getUniqueId());
            msg(player, "<gray>Edición cancelada.</gray>");
            reopen(p, player);
            return;
        }

        switch (p.kind) {
            case "cat_new" -> createCategory(player, msg);
            case "cat_name" -> setCategoryName(player, p.categoryId, msg);
            case "cat_material" -> setCategoryMaterial(player, p.categoryId, msg);
            case "cat_slot" -> setCategorySlot(player, p.categoryId, msg);
            case "cat_perm" -> setCategoryPermission(player, p.categoryId, msg);
            case "cat_group" -> setCategoryGroup(player, p.categoryId, msg);
            case "item_new_buy" -> setNewItemBuy(player, p.categoryId, p.itemId, msg);
            case "item_new_sell" -> setNewItemSell(player, p.categoryId, p.itemId, msg);
            case "item_buy" -> setItemBuy(player, p.categoryId, p.itemId, msg, true);
            case "item_sell" -> setItemBuy(player, p.categoryId, p.itemId, msg, false);
            case "item_eco" -> setItemEconomy(player, p.categoryId, p.itemId, msg);
            case "item_name" -> setItemName(player, p.categoryId, p.itemId, msg);
            case "item_material" -> setItemMaterial(player, p.categoryId, p.itemId, msg);
        }

        pending.remove(player.getUniqueId());
    }

    // =====================================================
    //              REABRIR MENÚ SEGÚN CONTEXTO
    // =====================================================
    private void reopen(PendingEdit p, Player player) {
        if (p == null || player == null || !player.isOnline()) return;

        if (p.kind.startsWith("item_new_") || p.itemId == null) {
            ShopEditorMenu.items(player, p.categoryId, 0);
        } else if (p.kind.startsWith("item_")) {
            ShopEditorMenu.itemEdit(player, p.categoryId, p.itemId);
        } else if (p.kind.startsWith("cat_")) {
            if (p.categoryId == null) {
                ShopEditorMenu.categories(player, 0);
            } else {
                ShopEditorMenu.categoryEdit(player, p.categoryId);
            }
        } else {
            ShopEditorMenu.categories(player, 0);
        }
    }

    // =====================================================
    //                   CATEGORÍAS
    // =====================================================
    private void createCategory(Player player, String id) {
        id = id.toLowerCase();
        if (!id.matches("[a-z0-9_]{3,16}")) {
            msg(player, "<red>ID inválido: usa 3-16 letras, números o _.</red> <gray>Escíbelo de nuevo.</gray>");
            return;
        }
        if (catManager().getCategory(id) != null) {
            msg(player, "<red>La categoría <white>" + id + "</white> ya existe.</red> <gray>Escribe otro ID.</gray>");
            return;
        }

        FileConfiguration c = catManager().getConfig();
        c.set("categories." + id + ".name", "<white>" + id + "</white>");
        c.set("categories." + id + ".material", "STONE");
        c.set("categories." + id + ".slot", -1);
        c.set("categories." + id + ".lore", List.of("<gray>Nueva categoría.</gray>"));
        catManager().saveCategories();
        catManager().reload();

        try {
            File f = itemsFile(id);
            f.getParentFile().mkdirs();
            YamlConfiguration ic = new YamlConfiguration();
            ic.createSection("items");
            ic.save(f);
        } catch (IOException ex) {
            plugin.getLogger().warning("No se pudo crear items/" + id + ".yml");
        }

        itemManager().reloadAllItems();
        msg(player, "<green>✔ Categoría <white>" + id + "</white> creada.</green>");
        ShopEditorMenu.categoryEdit(player, id);
    }

    public void deleteCategory(Player player, String id) {
        ShopCategory cat = catManager().getCategory(id);
        if (cat == null) {
            msg(player, "<red>La categoría no existe.</red>");
            return;
        }

        FileConfiguration c = catManager().getConfig();
        c.set("categories." + id, null);
        catManager().saveCategories();
        catManager().reload();

        File f = itemsFile(id);
        if (f.exists()) f.delete();

        itemManager().reloadAllItems();
        msg(player, "<red>✔ Categoría <white>" + id + "</white> eliminada.</red>");
        ShopEditorMenu.categories(player, 0);
    }

    private void setCategoryName(Player player, String catId, String name) {
        if (catManager().getCategory(catId) == null) {
            msg(player, "<red>Categoría no encontrada.</red>");
            return;
        }
        if (name.contains("&") || name.contains("§")) {
            name = MessageUtil.legacyToMiniMessage(name);
        }
        catManager().getConfig().set("categories." + catId + ".name", name);
        saveCategoriesAndReload();
        msg(player, "<green>✔ Nombre actualizado.</green>");
        ShopEditorMenu.categoryEdit(player, catId);
    }

    private void setCategoryMaterial(Player player, String catId, String raw) {
        Material mat = Material.matchMaterial(raw.toUpperCase());
        if (mat == null) {
            msg(player, "<red>Material inválido: <white>" + raw + "</white>.</red> <gray>Escríbelo de nuevo.</gray>");
            return;
        }
        catManager().getConfig().set("categories." + catId + ".material", mat.name());
        catManager().getConfig().set("categories." + catId + ".head", null);
        saveCategoriesAndReload();
        msg(player, "<green>✔ Material actualizado: <white>" + mat.name() + "</white></green>");
        ShopEditorMenu.categoryEdit(player, catId);
    }

    private void setCategorySlot(Player player, String catId, String raw) {
        int slot;
        if (raw.equalsIgnoreCase("auto") || raw.equals("-1")) {
            slot = -1;
        } else {
            try {
                slot = Integer.parseInt(raw);
            } catch (NumberFormatException ex) {
                msg(player, "<red>Slot inválido. <gray>Escribe un número o <white>auto</white>.</gray></red>");
                return;
            }
        }
        if (slot > 108) {
            msg(player, "<red>Slot demasiado alto.</red>");
            return;
        }
        catManager().getConfig().set("categories." + catId + ".slot", slot);
        saveCategoriesAndReload();
        msg(player, "<green>✔ Slot actualizado: <white>" + (slot == -1 ? "auto" : slot) + "</white></green>");
        ShopEditorMenu.categoryEdit(player, catId);
    }

    private void setCategoryPermission(Player player, String catId, String value) {
        catManager().getConfig().set("categories." + catId + ".required_permission",
                emptyValue(value) ? null : value);
        saveCategoriesAndReload();
        msg(player, "<green>✔ Permiso requerido actualizado.</green>");
        ShopEditorMenu.categoryEdit(player, catId);
    }

    private void setCategoryGroup(Player player, String catId, String value) {
        catManager().getConfig().set("categories." + catId + ".required_group",
                emptyValue(value) ? null : value);
        saveCategoriesAndReload();
        msg(player, "<green>✔ Grupo requerido actualizado.</green>");
        ShopEditorMenu.categoryEdit(player, catId);
    }

    private boolean emptyValue(String v) {
        return v.isEmpty() || v.equalsIgnoreCase("ninguno") || v.equalsIgnoreCase("none")
                || v.equals("-") || v.equals("0");
    }

    private void saveCategoriesAndReload() {
        catManager().saveCategories();
        catManager().reload();
    }

    // =====================================================
    //                    ÍTEMS
    // =====================================================
    public void addItemFromHand(Player player, String catId) {
        if (catManager().getCategory(catId) == null) {
            msg(player, "<red>Categoría no encontrada.</red>");
            return;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()) {
            msg(player, "<red>Necesitas tener un ítem en la mano.</red>");
            return;
        }

        String id = "item_" + System.currentTimeMillis();
        File file = itemsFile(catId);
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        String name;
        if (hand.hasItemMeta() && hand.getItemMeta().hasDisplayName()) {
            name = MessageUtil.legacyToMiniMessage(hand.getItemMeta().getDisplayName());
        } else {
            name = "<white>" + hand.getType().name().toLowerCase() + "</white>";
        }

        cfg.set("items." + id + ".name", name);
        cfg.set("items." + id + ".material", hand.getType().name());
        cfg.set("items." + id + ".buy", 0.0);
        cfg.set("items." + id + ".sell", 0.0);
        cfg.set("items." + id + ".economy", "vault");
        cfg.set("items." + id + ".itemstack", ItemSerializer.toBase64(hand));
        if (hand.hasItemMeta() && hand.getItemMeta().hasLore()) {
            cfg.set("items." + id + ".lore",
                    hand.getItemMeta().getLore().stream().map(MessageUtil::legacyToMiniMessage).toList());
        }

        try {
            cfg.save(file);
        } catch (IOException ex) {
            msg(player, "<red>Error al guardar el archivo de la categoría.</red>");
            return;
        }

        reloadCategory(catId);
        msg(player, "<yellow>Ítem creado.</yellow> <gray>Escribe el <green>precio de compra</green> (0 = gratis).</gray>");
        setPending(player, "item_new_buy", catId, id);
    }

    public void deleteItem(Player player, String catId, String itemId) {
        File file = itemsFile(catId);
        if (!file.exists()) {
            msg(player, "<red>La categoría no existe.</red>");
            return;
        }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        cfg.set("items." + itemId, null);
        try {
            cfg.save(file);
        } catch (IOException ex) {
            msg(player, "<red>Error al guardar.</red>");
            return;
        }

        reloadCategory(catId);
        msg(player, "<red>✔ Ítem <white>" + itemId + "</white> eliminado.</red>");
        ShopEditorMenu.items(player, catId, 0);
    }

    private void setNewItemBuy(Player player, String catId, String itemId, String raw) {
        Double buy = parseDouble(raw);
        if (buy == null || buy < 0) {
            msg(player, "<red>Número inválido.</red> <gray>Escribe el precio de compra.</gray>");
            return;
        }
        setItemField(player, catId, itemId, "buy", buy);
        msg(player, "<yellow>Precio de compra: <green>" + buy + "</green>.</yellow>");
        msg(player, "<yellow>Escribe el <red>precio de venta</red> (0 = no se compra).</yellow>");
        setPending(player, "item_new_sell", catId, itemId);
    }

    private void setNewItemSell(Player player, String catId, String itemId, String raw) {
        Double sell = parseDouble(raw);
        if (sell == null || sell < 0) {
            msg(player, "<red>Número inválido.</red> <gray>Escribe el precio de venta.</gray>");
            return;
        }
        setItemField(player, catId, itemId, "sell", sell);
        msg(player, "<green>✔ Ítem configurado correctamente.</green>");
        ShopEditorMenu.items(player, catId, 0);
    }

    private void setItemBuy(Player player, String catId, String itemId, String raw, boolean buy) {
        Double value = parseDouble(raw);
        if (value == null || value < 0) {
            msg(player, "<red>Número inválido.</red>");
            return;
        }
        setItemField(player, catId, itemId, buy ? "buy" : "sell", value);
        msg(player, "<green>✔ Precio " + (buy ? "de compra" : "de venta") + " actualizado.</green>");
        ShopEditorMenu.itemEdit(player, catId, itemId);
    }

    private void setItemEconomy(Player player, String catId, String itemId, String value) {
        String eco = value.toLowerCase();
        if (!plugin.getEconomyManager().exists(eco)) {
            msg(player, "<red>Economía no registrada: <white>" + value + "</white>.</red> <gray>(vault, xp, levels, points)</gray>");
            return;
        }
        setItemField(player, catId, itemId, "economy", eco);
        msg(player, "<green>✔ Moneda actualizada: <white>" + eco + "</white></green>");
        ShopEditorMenu.itemEdit(player, catId, itemId);
    }

    private void setItemName(Player player, String catId, String itemId, String name) {
        if (name.contains("&") || name.contains("§")) {
            name = MessageUtil.legacyToMiniMessage(name);
        }
        setItemField(player, catId, itemId, "name", name);
        msg(player, "<green>✔ Nombre actualizado.</green>");
        ShopEditorMenu.itemEdit(player, catId, itemId);
    }

    private void setItemMaterial(Player player, String catId, String itemId, String raw) {
        Material mat = Material.matchMaterial(raw.toUpperCase());
        if (mat == null) {
            msg(player, "<red>Material inválido: <white>" + raw + "</white>.</red>");
            return;
        }
        File file = itemsFile(catId);
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        cfg.set("items." + itemId + ".material", mat.name());
        cfg.set("items." + itemId + ".itemstack", null);
        try {
            cfg.save(file);
        } catch (IOException ex) {
            msg(player, "<red>Error al guardar.</red>");
            return;
        }
        reloadCategory(catId);
        msg(player, "<green>✔ Material actualizado: <white>" + mat.name() + "</white></green>");
        ShopEditorMenu.itemEdit(player, catId, itemId);
    }

    private void setItemField(Player player, String catId, String itemId, String field, Object value) {
        File file = itemsFile(catId);
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        cfg.set("items." + itemId + "." + field, value);
        try {
            cfg.save(file);
        } catch (IOException ex) {
            msg(player, "<red>Error al guardar.</red>");
            return;
        }
        reloadCategory(catId);
    }

    // =====================================================
    //               RECARGAR TODO
    // =====================================================
    public void reloadAll(Player player) {
        catManager().reload();
        itemManager().reloadAllItems();
        itemManager().reloadItemsMenuConfig();
        msg(player, "<green>✔ Tienda recargada.</green>");
    }

    // =====================================================
    //                     UTILS
    // =====================================================
    private Double parseDouble(String s) {
        try {
            return Double.parseDouble(s.replace(',', '.').replace(" ", ""));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void reloadCategory(String catId) {
        ShopCategory cat = catManager().getCategory(catId);
        if (cat != null) {
            itemManager().loadCategoryItems(cat);
        } else {
            itemManager().reloadAllItems();
        }
    }

    private File itemsFile(String catId) {
        return new File(plugin.getDataFolder(), "items/" + catId + ".yml");
    }

    private CategoryManager catManager() {
        return plugin.getCategoryManager();
    }

    private ItemManager itemManager() {
        return plugin.getItemManager();
    }

    private void msg(Player player, String text) {
        MessageUtil.msg(player, text);
    }

    // Referencia para no perder el ítem recién creado en el flujo de precios
    public ShopItem getItem(String catId, String itemId) {
        return itemManager().getItem(catId, itemId);
    }
}