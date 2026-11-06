package mp.quesito.qSMarketPlus.pwarp.menu;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.pwarp.Pwarp;
import mp.quesito.qSMarketPlus.pwarp.PwarpManager;
import mp.quesito.qSMarketPlus.pwarp.holder.PwarpHolder;
import mp.quesito.qSMarketPlus.utils.MenuItems;
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
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class PwarpMenu {

    private static final int SIZE = 54;
    private static final int[] GRID = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    public static void open(Player player) {
        open(player, 1, PwarpHolder.Filter.ALL);
    }

    public static void open(Player player, int page, PwarpHolder.Filter filter) {
        PwarpManager manager = QSMarketPlus.getInstance().getPwarpManager();

        List<Pwarp> warps = new ArrayList<>(manager.getAll());
        if (filter == PwarpHolder.Filter.FAVORITES) {
            Set<String> favs = manager.getFavoriteNames(player.getUniqueId());
            warps.removeIf(w -> !favs.contains(w.getName()));
        }
        warps.sort(Comparator.comparing(Pwarp::getOwnerName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Pwarp::getDisplayName, String.CASE_INSENSITIVE_ORDER));

        int maxPage = Math.max(1, (int) Math.ceil((double) warps.size() / GRID.length));
        page = Math.max(1, Math.min(page, maxPage));

        int start = (page - 1) * GRID.length;
        int end = Math.min(start + GRID.length, warps.size());
        List<Pwarp> pageItems = start >= warps.size()
                ? List.of()
                : new ArrayList<>(warps.subList(start, end));

        PwarpHolder holder = new PwarpHolder(player, page, filter, pageItems);
        Inventory inv = Bukkit.createInventory(holder, SIZE, MessageUtil.toLegacy("<dark_gray>» PWarps de Jugadores «"));

        fillBackground(inv);
        loadHeader(inv, holder, manager, warps.size());
        loadNavigation(inv, page, maxPage);
        loadItems(inv, pageItems, player, manager);

        player.openInventory(inv);
    }

    private static void fillBackground(Inventory inv) {
        ItemStack filler = MenuItems.glass();
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler.clone());
        }
    }

    private static void loadHeader(Inventory inv, PwarpHolder holder, PwarpManager manager, int total) {
        Player player = holder.getPlayer();

        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName(MessageUtil.toLegacy("<gold>PWarps del servidor</gold>"));
        infoMeta.setLore(MessageUtil.loreToLegacy(List.of(
                "",
                "<gray>Total de warps: <white>" + total,
                "<gray>Tus favoritos: <white>" + manager.getFavoriteNames(player.getUniqueId()).size(),
                "",
                "<yellow>Click en un warp:</yellow> <gray>Abre sus opciones (ir / favorito).</gray>"
        )));
        info.setItemMeta(infoMeta);
        inv.setItem(4, info);

        boolean favsOnly = holder.getFilter() == PwarpHolder.Filter.FAVORITES;
        ItemStack filter = new ItemStack(Material.ENDER_EYE);
        ItemMeta fMeta = filter.getItemMeta();
        fMeta.setDisplayName(MessageUtil.toLegacy(
                favsOnly ? "<yellow>Favoritos</yellow> <green>(activo)</green>" : "<aqua>Favoritos</aqua>"
        ));
        fMeta.setLore(MessageUtil.loreToLegacy(List.of(
                "",
                "<gray>Click: ver solo tus favoritos.</gray>",
                favsOnly ? "<green>« Haz click para volver a todos »</green>" : "<yellow>« Puedes usar /pwarp fav <nombre> »</yellow>"
        )));
        MetaUtil.setTag(fMeta, "btn", "filter");
        filter.setItemMeta(fMeta);
        inv.setItem(49, filter);

        ItemStack close = MenuItems.red("close", "<red>» Cerrar");
        inv.setItem(50, close);
    }

    private static void loadNavigation(Inventory inv, int page, int maxPage) {
        if (page > 1) inv.setItem(45, MenuItems.next("prev"));
        if (page < maxPage) inv.setItem(53, MenuItems.next("next"));
    }

    private static void loadItems(Inventory inv, List<Pwarp> items, Player viewer, PwarpManager manager) {
        for (int i = 0; i < items.size(); i++) {
            inv.setItem(GRID[i], warpItem(items.get(i), viewer, manager));
        }
    }

    private static ItemStack warpItem(Pwarp warp, Player viewer, PwarpManager manager) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        try {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(warp.getOwner()));
        } catch (Exception ignored) {
        }

        meta.setDisplayName(MessageUtil.toLegacy("<yellow>" + warp.getDisplayName()));

        List<String> lore = new ArrayList<>();
        lore.add(MessageUtil.toLegacy("<gray>Dueño: <white>" + warp.getOwnerName()));

        if (warp.getDescription() != null && !warp.getDescription().isEmpty()) {
            lore.addAll(MessageUtil.loreToLegacy(wrapDescription(warp.getDescription())));
        }

        lore.add("");
        boolean fav = manager.isFavorite(viewer.getUniqueId(), warp.getName());
        lore.add(MessageUtil.toLegacy(fav ? "<red>❤ Favorito</red>" : "<gray>Sin favorito</gray>"));
        lore.add(MessageUtil.toLegacy("<yellow>Click: Ver opciones</yellow>"));

        meta.setLore(lore);

        MetaUtil.setTag(meta, "btn", "warp");
        MetaUtil.setTag(meta, "pwarp", warp.getName());
        head.setItemMeta(meta);
        return head;
    }

    public static List<String> wrapDescription(String desc) {
        List<String> lines = new ArrayList<>();
        String rest = desc;
        while (rest.length() > 24) {
            int cut = rest.lastIndexOf(' ', 24);
            if (cut <= 0) cut = 24;
            String part = "<gray>" + rest.substring(0, cut);
            lines.add(part);
            rest = rest.substring(cut).trim();
        }
        if (!rest.isEmpty()) {
            lines.add("<gray>" + rest);
        }
        return lines;
    }
}