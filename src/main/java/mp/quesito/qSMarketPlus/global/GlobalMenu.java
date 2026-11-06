package mp.quesito.qSMarketPlus.global;

import mp.quesito.qSMarketPlus.global.GlobalQuery.AuctionLogRow;
import mp.quesito.qSMarketPlus.global.GlobalQuery.ShopRow;
import mp.quesito.qSMarketPlus.global.GlobalQuery.TradeRow;
import mp.quesito.qSMarketPlus.utils.MessageUtil;
import mp.quesito.qSMarketPlus.utils.MetaUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class GlobalMenu {

    private static final int SIZE = 54;
    private static final int PER_PAGE = 36;
    private static final String[] TAB_NAMES = {"<green>Tienda", "<gold>Tradeos", "<aqua>Subastas"};
    private static final Material[] TAB_ICONS = {Material.EMERALD, Material.PAPER, Material.GOLD_NUGGET};
    private static final String[] TIME_NAMES = {"<white>Todos", "<yellow>24 horas", "<yellow>7 días", "<yellow>30 días"};

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public static void open(Player viewer, int tab, UUID playerFilter, String playerFilterName, int timeIndex, int page) {
        if (page < 1) page = 1;

        GlobalHolder holder = new GlobalHolder(tab, playerFilter, playerFilterName, timeIndex, page);
        Inventory inv = Bukkit.createInventory(holder, SIZE, MessageUtil.toLegacy("<dark_gray>Historial » Global"));
        holder.setInventory(inv);

        fillBackground(inv);

        for (int t = 0; t < 3; t++) {
            inv.setItem(t, tabItem(t, tab == t));
        }
        inv.setItem(3, playerFilterItem(playerFilter, playerFilterName));
        inv.setItem(4, pageInfoItem(tab, playerFilterName, timeIndex, page));
        inv.setItem(5, timeItem(timeIndex));
        inv.setItem(8, navItem(Material.BARRIER, "<red>Cerrar", "close"));

        boolean hasNext = false;
        int offset = (page - 1) * PER_PAGE;

        switch (tab) {
            case 1 -> {
                List<TradeRow> rows = GlobalQuery.trades(offset, PER_PAGE + 1, playerFilter, timeIndex);
                hasNext = rows.size() > PER_PAGE;
                List<TradeRow> pageRows = new ArrayList<>(rows.subList(0, Math.min(rows.size(), PER_PAGE)));
                for (int i = 0; i < pageRows.size(); i++) {
                    inv.setItem(9 + i, tradeEntry(pageRows.get(i)));
                }
            }
            case 2 -> {
                List<AuctionLogRow> rows = GlobalQuery.auctionLog(offset, PER_PAGE + 1, playerFilter, timeIndex);
                hasNext = rows.size() > PER_PAGE;
                List<AuctionLogRow> pageRows = new ArrayList<>(rows.subList(0, Math.min(rows.size(), PER_PAGE)));
                for (int i = 0; i < pageRows.size(); i++) {
                    inv.setItem(9 + i, auctionEntry(pageRows.get(i)));
                }
            }
            default -> {
                List<ShopRow> rows = GlobalQuery.shop(offset, PER_PAGE + 1, playerFilter, timeIndex);
                hasNext = rows.size() > PER_PAGE;
                List<ShopRow> pageRows = new ArrayList<>(rows.subList(0, Math.min(rows.size(), PER_PAGE)));
                for (int i = 0; i < pageRows.size(); i++) {
                    inv.setItem(9 + i, shopEntry(pageRows.get(i)));
                }
            }
        }

        if (page > 1) inv.setItem(45, navItem(Material.ARROW, "<yellow>Página Anterior", "prev"));
        if (hasNext) inv.setItem(53, navItem(Material.ARROW, "<yellow>Página Siguiente", "next"));

        viewer.openInventory(inv);
    }

    // ==============================
    //   FILTROS
    // ==============================
    private static ItemStack tabItem(int tab, boolean selected) {
        ItemStack item = new ItemStack(TAB_ICONS[tab]);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtil.toLegacy(
                (selected ? "<green>▶ " : "<gray>") + TAB_NAMES[tab]));
        MetaUtil.setTag(meta, "tab", String.valueOf(tab));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack playerFilterItem(UUID filter, String filterName) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtil.toLegacy("<yellow>Filtro de jugador"));
        List<String> lore = new ArrayList<>();
        lore.add(MessageUtil.toLegacy("<white>" + (filter != null ? filterName : "<gray>Todos los jugadores")));
        lore.add(MessageUtil.toLegacy("<dark_gray>Click: escribir un nombre"));
        lore.add(MessageUtil.toLegacy("<dark_gray>Escribe 'off' para quitar el filtro"));
        meta.setLore(lore);
        MetaUtil.setTag(meta, "filter", filter != null ? filter.toString() : "none");
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack timeItem(int timeIndex) {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtil.toLegacy("<yellow>Rango de tiempo"));
        List<String> lore = new ArrayList<>();
        for (int i = 0; i < TIME_NAMES.length; i++) {
            lore.add(MessageUtil.toLegacy((i == timeIndex ? "<green>▶ " : "<gray>  ") + TIME_NAMES[i]));
        }
        lore.add(MessageUtil.toLegacy("<dark_gray>Click: cambiar rango"));
        meta.setLore(lore);
        MetaUtil.setTag(meta, "time", String.valueOf(timeIndex));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack pageInfoItem(int tab, String filterName, int timeIndex, int page) {
        ItemStack item = new ItemStack(Material.OAK_SIGN);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtil.toLegacy(TAB_NAMES[tab]));
        List<String> lore = new ArrayList<>();
        lore.add(MessageUtil.toLegacy("<gray>Página: <white>" + page));
        lore.add(MessageUtil.toLegacy("<gray>Jugador: <white>" + (filterName != null ? filterName : "Todos")));
        lore.add(MessageUtil.toLegacy("<gray>Rango: <white>" + TIME_NAMES[timeIndex]));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // ==============================
    //   ENTRIES
    // ==============================
    private static ItemStack shopEntry(ShopRow row) {
        boolean buy = "BUY".equals(row.type());
        ItemStack item = new ItemStack(buy ? Material.EMERALD : Material.GOLD_INGOT);
        String display = row.itemName();
        if (display == null || display.isEmpty()) display = "?";

        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtil.toLegacy(display));

        List<String> lore = new ArrayList<>();
        lore.add(MessageUtil.toLegacy(buy ? "<green>Compra</green>" : "<red>Venta</red>"));
        lore.add(MessageUtil.toLegacy("<gray>Jugador: <white>" + or(row.playerName(), "Desconocido")));
        lore.add(MessageUtil.toLegacy("<gray>Cantidad: <white>" + row.amount()));
        lore.add(MessageUtil.toLegacy("<gray>Total: <white>$" + formatMoney(row.total())));
        lore.add(MessageUtil.toLegacy("<dark_gray>" + formatDate(row.timestamp())));
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack tradeEntry(TradeRow row) {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtil.toLegacy("<gold>Tradeo</gold>"));

        String nameA = resolveName(row.playerA());
        String nameB = resolveName(row.playerB());

        List<String> lore = new ArrayList<>();
        lore.add(MessageUtil.toLegacy("<white>" + nameA + " <dark_gray>→ <white>" + nameB));
        lore.add(MessageUtil.toLegacy("<green>  Dio: <white>$" + formatMoney(row.moneyA())
                + " <green>| <white>" + fmtXp(row.xpA()) + " XP"));
        lore.add(MessageUtil.toLegacy("<gold>  Dio: <white>$" + formatMoney(row.moneyB())
                + " <gold>| <white>" + fmtXp(row.xpB()) + " XP"));
        lore.add(MessageUtil.toLegacy("<dark_gray>" + formatDate(row.timestamp())));
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack auctionEntry(AuctionLogRow row) {
        String type = row.type() == null ? "?" : row.type();
        Material icon;
        String label;
        switch (type) {
            case "CREATE" -> { icon = Material.EMERALD; label = "<green>Subastado"; }
            case "SOLD" -> { icon = Material.GOLD_INGOT; label = "<gold>Vendido"; }
            case "EXPIRED" -> { icon = Material.BARRIER; label = "<red>Expirado"; }
            case "CANCEL" -> { icon = Material.BARRIER; label = "<red>Cancelado"; }
            case "TAKEN" -> { icon = Material.CHEST; label = "<aqua>Reclamado"; }
            default -> { icon = Material.PAPER; label = "<gray>" + type; }
        }

        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtil.toLegacy(label + " · " + row.itemName()));

        List<String> lore = new ArrayList<>();
        lore.add(MessageUtil.toLegacy("<gray>Vendedor: <white>" + or(row.sellerName(), "Desconocido")));
        if ("SOLD".equals(type) && row.buyerName() != null) {
            lore.add(MessageUtil.toLegacy("<gray>Comprador: <white>" + row.buyerName()));
        }
        lore.add(MessageUtil.toLegacy("<gray>Precio: <white>$" + formatMoney(row.price())));
        lore.add(MessageUtil.toLegacy("<dark_gray>" + formatDate(row.timestamp())));
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    // ==============================
    //   HELPERS
    // ==============================
    private static String or(String s, String fallback) {
        return (s == null || s.isEmpty()) ? fallback : s;
    }

    private static String resolveName(UUID uuid) {
        OfflinePlayer off = Bukkit.getOfflinePlayer(uuid);
        if (off.getName() != null) return off.getName();
        return uuid.toString().substring(0, 8);
    }

    private static String fmtXp(double xp) {
        return Math.round(xp) == xp ? String.valueOf((long) xp) : String.format("%.1f", xp);
    }

    private static String formatMoney(double amount) {
        return Math.abs(amount - Math.round(amount)) < 0.005
                ? String.valueOf((long) Math.round(amount))
                : String.format("%.2f", amount);
    }

    private static String formatDate(long millis) {
        if (millis <= 0) return "?";
        return DATE_FMT.format(new Date(millis));
    }

    private static ItemStack navItem(Material material, String name, String btn) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtil.toLegacy(name));
        MetaUtil.setTag(meta, "btn", btn);
        item.setItemMeta(meta);
        return item;
    }

    private static void fillBackground(Inventory inv) {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.setDisplayName(" ");
        glass.setItemMeta(meta);
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, glass);
        }
    }
}