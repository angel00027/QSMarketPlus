package mp.quesito.qSMarketPlus.auction.menu;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.auction.AuctionItem;
import mp.quesito.qSMarketPlus.auction.AuctionManager;
import mp.quesito.qSMarketPlus.auction.holder.AHHistoryHolder;
import mp.quesito.qSMarketPlus.utils.MenuItems;
import mp.quesito.qSMarketPlus.utils.MetaUtil;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.*;

public class AHHistoryMenu {

    private static final int SIZE = 54;
    private static final String TITLE = MenuItems.miniToLegacy("<gold>Historial de Subastas");

    // 28 slots estilo AHMenu
    private static final int[] GRID = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private static final SimpleDateFormat DATE =
            new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public static void open(Player player, int page) {

        AuctionManager manager = QSMarketPlus.getInstance().getAuctionManager();
        List<AuctionItem> all = manager.getHistory(player.getUniqueId());

        int perPage = GRID.length;
        int maxPage = Math.max(1, (int) Math.ceil((double) all.size() / perPage));

        page = Math.max(1, Math.min(page, maxPage));

        int start = (page - 1) * perPage;
        int end = Math.min(start + perPage, all.size());

        List<AuctionItem> pageItems = new ArrayList<>(all.subList(start, end));

        // holder real
        AHHistoryHolder holder = new AHHistoryHolder(player, pageItems, page);
        Inventory inv = Bukkit.createInventory(holder, SIZE, TITLE);
        holder.setInventory(inv);

        fillBackground(inv);
        loadNavigation(inv, page, maxPage);

        for (int i = 0; i < pageItems.size(); i++) {
            AuctionItem auc = pageItems.get(i);
            ItemStack item = createHistoryDisplay(auc, i);
            inv.setItem(GRID[i], item);
        }

        player.openInventory(inv);
    }

    private static void fillBackground(Inventory inv) {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.setDisplayName(" ");
        glass.setItemMeta(meta);

        for (int i = 0; i < inv.getSize(); i++)
            inv.setItem(i, glass);
    }

    private static void loadNavigation(Inventory inv, int page, int maxPage) {

        // PREV
        if (page > 1) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta meta = prev.getItemMeta();
            meta.setDisplayName(MenuItems.miniToLegacy("<yellow>Página Anterior"));
            MetaUtil.setTag(meta, "btn", "prev");
            prev.setItemMeta(meta);
            inv.setItem(45, prev);
        }

        // CLOSE
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta cMeta = close.getItemMeta();
        cMeta.setDisplayName(MenuItems.miniToLegacy("<red>Cerrar"));
        MetaUtil.setTag(cMeta, "btn", "close");
        close.setItemMeta(cMeta);
        inv.setItem(49, close);

        // NEXT
        if (page < maxPage) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta meta = next.getItemMeta();
            meta.setDisplayName(MenuItems.miniToLegacy("<yellow>Página Siguiente"));
            MetaUtil.setTag(meta, "btn", "next");
            next.setItemMeta(meta);
            inv.setItem(53, next);
        }
    }

    private static ItemStack createHistoryDisplay(AuctionItem auc, int index) {

        // ITEM O BULK
        ItemStack item = auc.isBulk()
                ? new ItemStack(Material.CHEST)
                : auc.item.clone();

        ItemMeta meta = item.getItemMeta();
        List<String> lore = new ArrayList<>();

        // ========== ESTADO ==========
        lore.add(MenuItems.miniToLegacy("<gray>Estado: " + formatStatus(auc.status)));

        // ========== PRECIO ==========
        lore.add(MenuItems.miniToLegacy("<gray>Precio: <green>$" + auc.price));

        // ========== BULK COUNT ==========
        if (auc.isBulk()) {
            int count = 0;
            for (ItemStack it : auc.container)
                if (it != null && !it.getType().isAir()) count++;

            lore.add(MenuItems.miniToLegacy("<gray>Items en pack: <white>" + count));
        }

        // ========== COMPRADOR ==========
        if (auc.isSold() && auc.buyer != null) {
            String buyerName = Optional.ofNullable(Bukkit.getOfflinePlayer(auc.buyer).getName())
                    .orElse("Desconocido");
            lore.add(MenuItems.miniToLegacy("<gray>Comprador: <yellow>" + buyerName));
        }

        lore.add("");

        // ========== FECHA CREACIÓN ==========
        lore.add(MenuItems.miniToLegacy("<gray>Creado: <white>" + DATE.format(new Date(auc.created))));

        // ========== FECHA EXPIRACIÓN (solo si EXPIRED) ==========
        if (auc.status == AuctionItem.Status.EXPIRED) {
            lore.add(MenuItems.miniToLegacy("<gray>Expiró: <white>" + DATE.format(new Date(auc.expiresAt))));
        }

        meta.setLore(lore);

        // Hacerlo clickeable si quieres
        MetaUtil.setTag(meta, "history_index", String.valueOf(index));

        item.setItemMeta(meta);
        return item;
    }

    private static String formatStatus(AuctionItem.Status status) {

        return switch (status) {
            case SOLD -> "<green>Vendido";
            case CANCELLED -> "<red>Cancelado";
            case EXPIRED -> "<yellow>Expirado";
            case EXPIRED_TAKEN -> "<dark_gray>Retirado";
            default -> "<gray>Desconocido";
        };
    }
}
