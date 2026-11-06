package mp.quesito.qSMarketPlus.trade;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.utils.MessageUtil;
import mp.quesito.qSMarketPlus.utils.MetaUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class TradeHistoryMenu {

    private static final int SIZE = 54;
    private static final String TITLE = MessageUtil.toLegacy("<gold>Historial de Tradeos");

    private static final int[] GRID = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private static final SimpleDateFormat DATE =
            new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public static void open(Player player, int page) {
        open(player, player.getUniqueId(), page);
    }

    public static void open(Player viewer, UUID subject, int page) {

        int perPage = GRID.length;
        int offset = (page - 1) * perPage;

        List<TradeRecord> loaded = QSMarketPlus.getInstance().getTradeManager()
                .getDao().loadHistory(subject, perPage + 1, offset);

        boolean hasNext = loaded.size() > perPage;
        List<TradeRecord> pageRecords = new ArrayList<>(
                loaded.subList(0, Math.min(perPage, loaded.size())));

        if (pageRecords.isEmpty() && page > 1) {
            open(viewer, subject, page - 1);
            return;
        }

        TradeHistoryHolder holder = new TradeHistoryHolder(viewer, pageRecords, page);
        Inventory inv = Bukkit.createInventory(holder, SIZE, TITLE);
        holder.setInventory(inv);

        fillBackground(inv);
        loadNavigation(inv, page, hasNext, pageRecords.isEmpty());

        for (int i = 0; i < pageRecords.size(); i++) {
            inv.setItem(GRID[i], displayItem(subject, pageRecords.get(i)));
        }

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

    private static void loadNavigation(Inventory inv, int page, boolean hasNext, boolean empty) {

        ItemStack info = new ItemStack(Material.OAK_SIGN);
        ItemMeta iMeta = info.getItemMeta();
        iMeta.setDisplayName(MessageUtil.toLegacy("<yellow>Página <white>" + page));
        if (empty) {
            List<String> lore = new ArrayList<>();
            lore.add(MessageUtil.toLegacy("<red>No hay trades registrados.</red>"));
            iMeta.setLore(lore);
        }
        info.setItemMeta(iMeta);
        inv.setItem(4, info);

        if (page > 1) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta meta = prev.getItemMeta();
            meta.setDisplayName(MessageUtil.toLegacy("<yellow>Página Anterior"));
            MetaUtil.setTag(meta, "btn", "prev");
            prev.setItemMeta(meta);
            inv.setItem(45, prev);
        }

        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta cMeta = close.getItemMeta();
        cMeta.setDisplayName(MessageUtil.toLegacy("<red>Cerrar"));
        MetaUtil.setTag(cMeta, "btn", "close");
        close.setItemMeta(cMeta);
        inv.setItem(49, close);

        if (hasNext) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta meta = next.getItemMeta();
            meta.setDisplayName(MessageUtil.toLegacy("<yellow>Página Siguiente"));
            MetaUtil.setTag(meta, "btn", "next");
            next.setItemMeta(meta);
            inv.setItem(53, next);
        }
    }

    private static ItemStack displayItem(UUID subject, TradeRecord record) {

        boolean isA = record.playerA.equals(subject);

        List<ItemStack> received = isA ? record.itemsB : record.itemsA;
        List<ItemStack> gave = isA ? record.itemsA : record.itemsB;

        double receivedMoney = isA ? record.moneyB : record.moneyA;
        double gaveMoney = isA ? record.moneyA : record.moneyB;
        int receivedXp = isA ? record.xpB : record.xpA;
        int gaveXp = isA ? record.xpA : record.xpB;

        UUID partnerUuid = isA ? record.playerB : record.playerA;
        String partner = partnerName(partnerUuid);

        ItemStack item;
        if (!received.isEmpty()) {
            item = received.get(0).clone();
        } else if (receivedMoney > 0) {
            item = new ItemStack(Material.GOLD_INGOT);
        } else if (receivedXp > 0) {
            item = new ItemStack(Material.EXPERIENCE_BOTTLE);
        } else {
            item = new ItemStack(Material.BARRIER);
        }

        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtil.toLegacy("<yellow>Tradeo con <white>" + partner));

        List<String> lore = new ArrayList<>();
        lore.add(MessageUtil.toLegacy("<gray>Recibiste: " + itemSummary(received)
                + moneyPart(receivedMoney) + xpPart(receivedXp)));
        lore.add(MessageUtil.toLegacy("<gray>Entregaste: " + itemSummary(gave)
                + moneyPart(gaveMoney) + xpPart(gaveXp)));
        lore.add("");
        lore.add(MessageUtil.toLegacy("<dark_gray>" + DATE.format(new Date(record.timestamp))));
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private static String itemSummary(List<ItemStack> items) {
        if (items.isEmpty()) return "<white>nada";

        if (items.size() == 1) {
            ItemStack it = items.get(0);
            if (it.getItemMeta() != null && it.getItemMeta().hasDisplayName()) {
                return "<white>" + it.getItemMeta().getDisplayName() + "</white> <dark_gray>x" + it.getAmount();
            }
            return "<white>" + prettyName(it.getType().name()) + "</white> <dark_gray>x" + it.getAmount();
        }

        return "<white>" + items.size() + " ítems</white>";
    }

    private static String moneyPart(double money) {
        if (money <= 0) return "";
        return " <green>$" + formatMoney(money) + "</green>";
    }

    private static String xpPart(int xp) {
        if (xp <= 0) return "";
        return " <aqua>" + xp + " niveles</aqua>";
    }

    private static String formatMoney(double amount) {
        return amount == Math.floor(amount)
                ? String.valueOf((long) amount)
                : String.format("%.2f", amount);
    }

    private static String prettyName(String raw) {
        StringBuilder sb = new StringBuilder();
        for (String part : raw.toLowerCase().split("_")) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(' ');
            }
        }
        return sb.toString().trim();
    }

    private static String partnerName(UUID uuid) {
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        return name != null ? name : "?";
    }
}