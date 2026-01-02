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

        auctions.removeIf(a ->
                (!a.isBulk() && (a.item == null || a.item.getType().isAir()))
        );

        applyFilters(player, auctions);
        loadGridSlots();

        int perPage = GRID_SLOTS.length;
        int maxPage = Math.max(1, (int) Math.ceil((double) auctions.size() / perPage));

        page = Math.max(1, Math.min(page, maxPage));

        int start = (page - 1) * perPage;
        int end = Math.min(start + perPage, auctions.size());

        List<AuctionItem> pageItems = new ArrayList<>(auctions.subList(start, end));

        String title = MenuItems.miniToLegacy(AHConfig.get().getString("ah.title"));
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

        ItemStack display;
        ItemMeta meta;

        boolean isOwner = auc.seller.equals(viewer.getUniqueId());

        // Contar cuántos items reales hay en el container (solo para bulk)
        long itemCount = 0;
        if (auc.isBulk() && auc.container != null) {
            itemCount = Arrays.stream(auc.container)
                    .filter(it -> it != null && it.getType() != Material.AIR)
                    .count();
        }

        boolean multipleItems = auc.isBulk() && itemCount > 1;

        if (multipleItems) {
            // Mostrar cofre si hay más de un item en el pack
            display = new ItemStack(Material.CHEST);
            meta = display.getItemMeta();

            int totalItems = (int) Arrays.stream(auc.container)
                    .filter(it -> it != null && it.getType() != Material.AIR)
                    .count();

            if (isOwner) {
                meta.setDisplayName(MenuItems.miniToLegacy("<gold>Tus Items Subastados"));
                meta.setLore(List.of(
                        MenuItems.miniToLegacy("<gray>Precio total: <green>$" + auc.price),
                        MenuItems.miniToLegacy("<gray>Items incluidos: <white>" + totalItems),
                        "",
                        MenuItems.miniToLegacy("<yellow>Click: Opciones (Cancelar / Ver)")
                ));
            } else {
                meta.setDisplayName(MenuItems.miniToLegacy("<gold>Inventario Subastado"));
                meta.setLore(List.of(
                        MenuItems.miniToLegacy("<gray>Vendedor: <yellow>" + Bukkit.getOfflinePlayer(auc.seller).getName()),
                        MenuItems.miniToLegacy("<gray>Precio total: <green>$" + auc.price),
                        MenuItems.miniToLegacy("<gray>Items incluidos: <white>" + totalItems),
                        "",
                        MenuItems.miniToLegacy("<yellow>Click: Opciones (Comprar / Ver)")
                ));
            }

        } else {
            // Mostrar item real si solo hay un item (aunque sea un stack grande)
            if (auc.isBulk() && auc.container != null) {
                // Tomar el primer item no nulo
                display = Arrays.stream(auc.container)
                        .filter(it -> it != null && it.getType() != Material.AIR)
                        .findFirst()
                        .orElse(new ItemStack(Material.AIR))
                        .clone();
            } else {
                display = auc.item.clone();
            }

            meta = display.getItemMeta();

            if (isOwner) {
                meta.setDisplayName(MenuItems.miniToLegacy("<yellow>Tu Subasta"));
                meta.setLore(List.of(
                        MenuItems.miniToLegacy("<gray>Precio: <green>$" + auc.price),
                        "",
                        MenuItems.miniToLegacy("<yellow>Click: Opciones (Cancelar)")
                ));
            } else {
                meta.setLore(List.of(
                        MenuItems.miniToLegacy("<gray>Vendedor: <yellow>" + Bukkit.getOfflinePlayer(auc.seller).getName()),
                        MenuItems.miniToLegacy("<gray>Precio: <green>$" + auc.price),
                        "",
                        MenuItems.miniToLegacy("<yellow>Click: Opciones (Comprar)")
                ));
            }
        }

        MetaUtil.setTag(meta, "auction_index", String.valueOf(index));
        display.setItemMeta(meta);

        return display;
    }


    private static void loadGridSlots() {
        List<Integer> raw = AHConfig.get().getIntegerList("ah.grid_slots");
        GRID_SLOTS = raw.stream().mapToInt(i -> i).toArray();
    }
}
