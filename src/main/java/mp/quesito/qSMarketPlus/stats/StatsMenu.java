package mp.quesito.qSMarketPlus.stats;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.stats.StatsManager.ItemStat;
import mp.quesito.qSMarketPlus.stats.StatsManager.StatsSummary;
import mp.quesito.qSMarketPlus.utils.MessageUtil;
import mp.quesito.qSMarketPlus.utils.MetaUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StatsMenu {

    private static final int SIZE = 54;
    private static final int PER_PAGE = 36;
    private static final String TITLE = MessageUtil.toLegacy("<dark_gray>Estadísticas » <yellow>Tienda");

    public static void open(Player viewer, UUID subject, int page) {
        List<ItemStat> all = QSMarketPlus.getInstance().getStatsManager().getItemStats(subject);
        int offset = (page - 1) * PER_PAGE;
        boolean hasNext = all.size() > offset + PER_PAGE;
        List<ItemStat> pageItems = new ArrayList<>(
                all.subList(offset, Math.min(all.size(), offset + PER_PAGE)));

        if (pageItems.isEmpty() && page > 1) {
            open(viewer, subject, page - 1);
            return;
        }

        StatsHolder holder = new StatsHolder(subject, page);
        Inventory inv = Bukkit.createInventory(holder, SIZE, TITLE);
        holder.setInventory(inv);

        fillBackground(inv);
        StatsSummary summary = QSMarketPlus.getInstance().getStatsManager().getSummary(subject);
        inv.setItem(0, headerItem(subject, summary));
        inv.setItem(4, pageInfoItem(page, pageItems.isEmpty()));

        for (int i = 0; i < pageItems.size(); i++) {
            inv.setItem(9 + i, entryItem(pageItems.get(i)));
        }

        if (page > 1) inv.setItem(45, navItem(Material.ARROW, "<yellow>Página Anterior", "prev"));
        inv.setItem(49, navItem(Material.BARRIER, "<red>Cerrar", "close"));
        if (hasNext) inv.setItem(53, navItem(Material.ARROW, "<yellow>Página Siguiente", "next"));

        viewer.openInventory(inv);
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

    private static ItemStack headerItem(UUID subject, StatsSummary s) {
        String name = QSMarketPlus.getInstance().getStatsManager().lastKnownName(subject);
        if (name == null) {
            name = Bukkit.getOfflinePlayer(subject).getName();
        }
        if (name == null) name = "?";

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = head.getItemMeta();
        meta.setDisplayName(MessageUtil.toLegacy("<yellow>Estadísticas de <white>" + name));
        List<String> lore = new ArrayList<>();
        lore.add(MessageUtil.toLegacy("  "));
        lore.add(MessageUtil.toLegacy("<green>■ <white>Compras: <green>" + s.buyCount));
        lore.add(MessageUtil.toLegacy("<green>  Total gastado: <white>$" + formatMoney(s.buyTotal)));
        lore.add(MessageUtil.toLegacy("<green>  Promedio compra: <white>$" + formatMoney(s.buyAverage)));
        lore.add(MessageUtil.toLegacy("  "));
        lore.add(MessageUtil.toLegacy("<red>■ <white>Ventas: <red>" + s.sellCount));
        lore.add(MessageUtil.toLegacy("<red>  Total ganado: <white>$" + formatMoney(s.sellTotal)));
        lore.add(MessageUtil.toLegacy("<red>  Promedio venta: <white>$" + formatMoney(s.sellAverage)));
        meta.setLore(lore);

        if (meta instanceof SkullMeta skull && name.matches("[A-Za-z0-9_]{1,16}")) {
            skull.setOwner(name);
        }
        head.setItemMeta(meta);
        return head;
    }

    private static ItemStack pageInfoItem(int page, boolean empty) {
        ItemStack info = new ItemStack(Material.OAK_SIGN);
        ItemMeta meta = info.getItemMeta();
        meta.setDisplayName(MessageUtil.toLegacy("<yellow>Página <white>" + page));
        if (empty) {
            List<String> lore = new ArrayList<>();
            lore.add(MessageUtil.toLegacy("<red>No hay movimientos registrados.</red>"));
            meta.setLore(lore);
        }
        info.setItemMeta(meta);
        return info;
    }

    private static ItemStack navItem(Material material, String name, String btn) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtil.toLegacy(name));
        MetaUtil.setTag(meta, "btn", btn);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack entryItem(ItemStat stat) {
        boolean buy = "BUY".equals(stat.type);
        ItemStack item = new ItemStack(buy ? Material.EMERALD : Material.GOLD_INGOT);
        String display = stat.itemName;
        if (display == null || display.isEmpty() || display.equals("-")) {
            display = stat.itemId;
        }

        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtil.toLegacy(display));

        List<String> lore = new ArrayList<>();
        lore.add(MessageUtil.toLegacy(buy ? "<green>Compras</green>" : "<red>Ventas</red>"));
        lore.add(MessageUtil.toLegacy("<gray>Operaciones: <white>" + stat.count));
        lore.add(MessageUtil.toLegacy("<gray>Cantidad total: <white>" + stat.totalAmount + " ítems"));
        lore.add(MessageUtil.toLegacy(buy
                ? "<gray>Total gastado: <white>$" + formatMoney(stat.totalMoney)
                : "<gray>Total ganado: <white>$" + formatMoney(stat.totalMoney)));
        lore.add(MessageUtil.toLegacy("<gray>Promedio por operación: <white>$" + formatMoney(stat.averageMoney)));
        if (stat.categoryId != null && !stat.categoryId.isEmpty() && !stat.categoryId.equals("-")) {
            lore.add(MessageUtil.toLegacy("<dark_gray>Categoría: " + stat.categoryId));
        }
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private static String formatMoney(double amount) {
        return Math.abs(amount - Math.round(amount)) < 0.005
                ? String.valueOf((long) Math.round(amount))
                : String.format("%.2f", amount);
    }
}