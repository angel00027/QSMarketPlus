package mp.quesito.qSMarketPlus.auction.menu;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.auction.AuctionItem;
import mp.quesito.qSMarketPlus.auction.AuctionManager;
import mp.quesito.qSMarketPlus.auction.holder.AHHolder;
import mp.quesito.qSMarketPlus.manager.AHConfig;
import mp.quesito.qSMarketPlus.utils.MenuItems;
import mp.quesito.qSMarketPlus.utils.MetaUtil;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class AHMenu {

    private static int[] GRID_SLOTS;

    public static void open(Player player) {
        open(player, 1);
    }

    public static void open(Player player, int page) {

        AuctionManager manager = QSMarketPlus.getInstance().getAuctionManager();
        List<AuctionItem> auctions = new ArrayList<>(manager.getAuctions());

        // No se ocultan subastas con ítem corrupto: se muestran con marcador para
        // que no parezcan "desaparecidas" y el admin pueda gestionarlas.
        applyFilters(player, auctions);
        loadGridSlots();

        int perPage = GRID_SLOTS.length;
        int maxPage = Math.max(1, (int) Math.ceil((double) auctions.size() / perPage));

        page = Math.max(1, Math.min(page, maxPage));

        int start = (page - 1) * perPage;
        int end = Math.min(start + perPage, auctions.size());

        List<AuctionItem> pageItems = new ArrayList<>(auctions.subList(start, end));

        String title = MenuItems.miniToLegacy(mp.quesito.qSMarketPlus.utils.NexoGui.title(
                AHConfig.get().getString("ah.title", "<gold>Casa de Subasta"))
                .replace("{page}", String.valueOf(page))
                .replace("{max}", String.valueOf(maxPage)));
        int size = AHConfig.get().getInt("ah.size", 54);

        // NUEVO HOLDER, sin inventario en el constructor
        AHHolder holder = new AHHolder(player, pageItems, page);

        // CREAR inventario CON EL HOLDER, el correcto
        Inventory inv = Bukkit.createInventory(holder, size, title);

        // Vincular inventario al holder
        holder.setInventory(inv);

        // Rellenar GUI
        fillBackground(inv);
        loadFilters(inv, holder);
        loadNavigation(inv, holder, maxPage);

        for (int i = 0; i < pageItems.size(); i++) {
            AuctionItem auc = pageItems.get(i);
            ItemStack display = createAuctionDisplay(auc, i, player);
            inv.setItem(GRID_SLOTS[i], display);
        }

        player.openInventory(inv);
    }

    // ------------------ FILTROS ------------------
    private static void applyFilters(Player p, List<AuctionItem> auctions) {
        AHHolder.FilterMode mode = AHHolder.getFilter(p);

        switch (mode) {
            case PRICE_ASC -> auctions.sort(Comparator.comparingDouble(a -> a.price));
            case PRICE_DESC -> auctions.sort((a, b) -> Double.compare(b.price, a.price));
            case ONLY_ITEMS -> auctions.removeIf(AuctionItem::isBulk);
            case ONLY_BULK -> auctions.removeIf(a -> !a.isBulk());
        }
    }

    private static void loadFilters(Inventory inv, AHHolder holder) {
        var sec = AHConfig.get().getConfigurationSection("ah.filters");
        if (sec == null) return;

        for (String key : sec.getKeys(false)) {

            var itemSec = sec.getConfigurationSection(key);
            if (itemSec == null) continue;

            ItemStack item = MenuItems.buttonFromConfig(itemSec);

            String tag = itemSec.getString("tag");
            ItemMeta meta = item.getItemMeta();
            MetaUtil.setTag(meta, "filter", tag);

            if (holder.getFilterMode().name().equals(tag)) {
                meta.setDisplayName(MenuItems.miniToLegacy(
                        itemSec.getString("name") + " <green>(Activo)"
                ));
            }

            item.setItemMeta(meta);
            inv.setItem(itemSec.getInt("slot"), item);
        }
    }

    // ------------------ NAVEGACIÓN ------------------
    private static void loadNavigation(Inventory inv, AHHolder holder, int maxPage) {

        var sec = AHConfig.get().getConfigurationSection("ah.navigation");
        if (sec == null) return;

        for (String key : sec.getKeys(false)) {

            var itemSec = sec.getConfigurationSection(key);
            if (itemSec == null) continue;

            int slot = itemSec.getInt("slot");

            if (key.equalsIgnoreCase("prev") && holder.getPage() <= 1) continue;
            if (key.equalsIgnoreCase("next") && holder.getPage() >= maxPage) continue;

            ItemStack item = MenuItems.buttonFromConfig(itemSec);

            ItemMeta meta = item.getItemMeta();
            MetaUtil.setTag(meta, "btn", key);
            item.setItemMeta(meta);

            inv.setItem(slot, item);
        }
    }

    // ------------------ FONDO ------------------
    private static void fillBackground(Inventory inv) {

        var sec = AHConfig.get().getConfigurationSection("ah.background");
        if (sec == null) return;

        Material mat = Material.matchMaterial(sec.getString("material", "GRAY_STAINED_GLASS_PANE"));
        if (mat == null || mat.isAir()) return; // AIR: se ve el fondo de Nexo
        String name = MenuItems.miniToLegacy(sec.getString("name", "<gray> "));

        ItemStack filler = new ItemStack(mat);
        ItemMeta meta = filler.getItemMeta();
        meta.setDisplayName(name);
        filler.setItemMeta(meta);

        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }
    }

    // ------------------ ITEM DE SUBASTA ------------------
    private static ItemStack createAuctionDisplay(AuctionItem auc, int index, Player viewer) {

        boolean isOwner = auc.seller.equals(viewer.getUniqueId());
        int packCount = auc.isBulk() ? AHUi.countItems(auc.container) : 0;

        ItemStack display = null;
        if (auc.isBulk() && packCount > 1) {
            display = new ItemStack(Material.CHEST);
        } else if (auc.isBulk() && auc.container != null) {
            for (ItemStack it : auc.container) {
                if (it != null && !it.getType().isAir()) {
                    display = it.clone();
                    break;
                }
            }
        } else if (auc.item != null && !auc.item.getType().isAir()) {
            display = auc.item.clone();
        }

        // Subasta corrupta/ilegible: se muestra como marcador, nunca se oculta
        if (display == null || display.getType().isAir()) {
            display = new ItemStack(Material.BARRIER);
            ItemMeta meta = display.getItemMeta();
            meta.setDisplayName(MenuItems.miniToLegacy("<red>Subasta corrupta</red>"));
            meta.setLore(List.of(
                    MenuItems.miniToLegacy("<gray>Base64 inválido o pack vacío."),
                    MenuItems.miniToLegacy("<yellow>Click: Opciones (Admin)")
            ));
            MetaUtil.setTag(meta, "auction_index", String.valueOf(index));
            display.setItemMeta(meta);
            return display;
        }

        ItemMeta meta = display.getItemMeta();

        if (packCount > 1) {
            meta.setDisplayName(MenuItems.miniToLegacy(isOwner
                    ? AHConfig.get().getString("ui.texts.own-pack", "<gold>Tu pack en subasta")
                    : AHConfig.get().getString("ui.texts.pack", "<gold>Pack de objetos")));
        }

        // Se conserva el lore original del objeto (encantamientos de libros, etc.)
        List<String> lore = new ArrayList<>(meta.hasLore() && packCount <= 1 ? meta.getLore() : List.of());
        if (!lore.isEmpty()) lore.add("");

        String key = isOwner ? "auction-own" : "auction";
        List<String> fallback = isOwner
                ? List.of("<gray>Precio: <green>$<price>", "<gray>Termina en: <white><time>", "",
                "<yellow>Click: Opciones (Cancelar)")
                : List.of("<gray>Vendedor: <yellow><seller>", "<gray>Precio: <green>$<price>",
                "<gray>Termina en: <white><time>", "", "<yellow>Click: Opciones (Comprar)");

        lore.addAll(AHUi.lore(key, fallback,
                "seller", AHUi.seller(auc),
                "price", AHUi.money(auc.price),
                "time", AHUi.timeLeft(auc.expiresAt)));

        if (packCount > 1) {
            lore.addAll(AHUi.lore("pack-count", List.of("<gray>Objetos en el pack: <white><count>"),
                    "count", String.valueOf(packCount)));
        }

        meta.setLore(lore);
        MetaUtil.setTag(meta, "auction_index", String.valueOf(index));
        display.setItemMeta(meta);
        return display;
    }


    private static void loadGridSlots() {
        List<Integer> raw = AHConfig.get().getIntegerList("ah.grid_slots");
        GRID_SLOTS = raw.stream().mapToInt(i -> i).toArray();
    }
}
