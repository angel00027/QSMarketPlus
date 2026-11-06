package mp.quesito.qSMarketPlus.shop.browser;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.shop.PlayerShop;
import mp.quesito.qSMarketPlus.utils.MessageUtil;
import mp.quesito.qSMarketPlus.utils.MetaUtil;
import mp.quesito.qSMarketPlus.utils.NexoGui;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

/**
 * Buscador de tiendas de jugadores (/tiendas).
 *
 * Vista principal: un icono por objeto a la venta, con cuantas tiendas lo venden y
 * el mejor precio por unidad. Al elegir uno se ven todas sus tiendas, de la mas
 * barata a la mas cara, y un clic lleva a la tienda a un sitio seguro.
 *
 * Todo el aspecto sale de config.yml -> player-shops-browser.
 */
public final class ShopBrowser {

    public static final int[] GRID = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private static final long CACHE_MS = 20_000L;
    private static final DecimalFormat MONEY =
            new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(Locale.US));

    /** Una tienda que vende un objeto, con stock y precio por unidad. */
    public record Offer(PlayerShop shop, ItemStack item, int stock, double unitPrice) {
    }

    /** Un objeto a la venta y todas las tiendas que lo venden (de la mas barata a la mas cara). */
    public record Product(ItemStack icon, List<Offer> offers) {
        double bestPrice() {
            return offers.get(0).unitPrice();
        }

        int totalStock() {
            return offers.stream().mapToInt(Offer::stock).sum();
        }
    }

    private static List<Product> cache = List.of();
    private static long cachedAt;

    private ShopBrowser() {
    }

    // =====================================================
    //                    DATOS
    // =====================================================

    /** Lee los cofres de las tiendas; se guarda unos segundos para no recargar chunks en cada clic. */
    public static synchronized List<Product> products() {
        if (System.currentTimeMillis() - cachedAt < CACHE_MS) return cache;

        List<Product> products = new ArrayList<>();

        for (PlayerShop shop : QSMarketPlus.getInstance().getShopManager().getAllShops()) {

            if (!shop.isActive() || shop.getChestLocation().getWorld() == null) continue;

            Inventory inv = shop.getChestInventory();
            if (inv == null) continue;

            ItemStack first = null;
            int stock = 0;
            boolean mixed = false;

            for (ItemStack it : inv.getContents()) {
                if (it == null || it.getType().isAir()) continue;
                if (first == null) first = it;
                if (it.getType() != first.getType()) mixed = true;
                else stock += it.getAmount();
            }

            // Igual que la tienda: vacia, mezclada o sin stock para una venta no vende
            if (first == null || mixed || stock < shop.getAmountPerSale() || shop.getAmountPerSale() <= 0) continue;

            ItemStack key = first.clone();
            key.setAmount(1);
            Offer offer = new Offer(shop, key, stock, shop.getPrice() / shop.getAmountPerSale());

            Product product = products.stream().filter(p -> p.icon().isSimilar(key)).findFirst().orElse(null);
            if (product == null) {
                product = new Product(key, new ArrayList<>());
                products.add(product);
            }
            product.offers().add(offer);
        }

        for (Product p : products) {
            p.offers().sort(Comparator.comparingDouble(Offer::unitPrice));
        }
        products.sort(Comparator.comparing((Product p) -> -p.offers().size()).thenComparing(p -> itemName(p.icon())));

        cache = products;
        cachedAt = System.currentTimeMillis();
        return cache;
    }

    public static void invalidate() {
        cachedAt = 0;
    }

    // =====================================================
    //                    MENUS
    // =====================================================
    public static void openMain(Player player, int page) {

        List<Product> products = products();
        int maxPage = Math.max(1, (int) Math.ceil(products.size() / (double) GRID.length));
        page = Math.max(1, Math.min(page, maxPage));

        ShopBrowserHolder holder = new ShopBrowserHolder(products, -1, page);
        Inventory inv = Bukkit.createInventory(holder, 54, title("main", "<dark_green>Tiendas de jugadores", page, maxPage));
        holder.setInventory(inv);

        inv.setItem(4, button("info", Material.BOOK, "<yellow>Tiendas de jugadores", null, null,
                lines("info-lore", List.of("<gray>Cada objeto muestra su mejor precio.",
                        "<gray>Elige uno para ver quién lo vende."))));

        int start = (page - 1) * GRID.length;
        for (int i = 0; i < GRID.length && start + i < products.size(); i++) {
            Product p = products.get(start + i);
            ItemStack icon = p.icon().clone();
            ItemMeta meta = icon.getItemMeta();
            List<String> lore = new ArrayList<>(meta.hasLore() ? meta.getLore() : List.of());
            if (!lore.isEmpty()) lore.add("");
            lore.addAll(lines("product-lore", List.of(
                    "<gray>Tiendas: <white><shops>",
                    "<gray>Desde: <green>$<best> <gray>c/u",
                    "<gray>Stock total: <white><stock>",
                    "",
                    "<yellow>Clic para ver las tiendas"),
                    "shops", String.valueOf(p.offers().size()),
                    "best", money(p.bestPrice()),
                    "stock", String.valueOf(p.totalStock())));
            meta.setLore(lore);
            MetaUtil.setTag(meta, "product", String.valueOf(start + i));
            icon.setItemMeta(meta);
            inv.setItem(GRID[i], icon);
        }

        if (products.isEmpty()) {
            inv.setItem(22, button("empty", Material.BARRIER, "<red>Aún no hay tiendas con stock", null, null, null));
        }

        navigation(inv, page, maxPage, "close");
        fill(inv);
        player.openInventory(inv);
    }

    public static void openProduct(Player player, List<Product> products, int index, int page) {

        if (index < 0 || index >= products.size()) {
            openMain(player, 1);
            return;
        }

        Product product = products.get(index);
        List<Offer> offers = product.offers();
        int maxPage = Math.max(1, (int) Math.ceil(offers.size() / (double) GRID.length));
        page = Math.max(1, Math.min(page, maxPage));

        ShopBrowserHolder holder = new ShopBrowserHolder(products, index, page);
        Inventory inv = Bukkit.createInventory(holder, 54,
                title("product", "<dark_green><item>", page, maxPage).replace("<item>", stripName(product.icon())));
        holder.setInventory(inv);

        ItemStack header = product.icon().clone();
        ItemMeta hm = header.getItemMeta();
        hm.setLore(lines("product-lore", List.of("<gray>Desde: <green>$<best> <gray>c/u"),
                "shops", String.valueOf(offers.size()), "best", money(product.bestPrice()),
                "stock", String.valueOf(product.totalStock())));
        header.setItemMeta(hm);
        inv.setItem(4, header);

        int start = (page - 1) * GRID.length;
        for (int i = 0; i < GRID.length && start + i < offers.size(); i++) {
            Offer offer = offers.get(start + i);
            inv.setItem(GRID[i], offerIcon(player, offer, start + i == 0));
        }

        navigation(inv, page, maxPage, "back");
        fill(inv);
        player.openInventory(inv);
    }

    private static ItemStack offerIcon(Player viewer, Offer offer, boolean best) {

        PlayerShop shop = offer.shop();
        Location loc = shop.getChestLocation();

        ItemStack icon = offer.item().clone();
        icon.setAmount(Math.max(1, Math.min(shop.getAmountPerSale(), icon.getMaxStackSize())));

        String distance = viewer.getWorld().equals(loc.getWorld())
                ? String.valueOf((int) viewer.getLocation().distance(loc)) + " bloques"
                : "otro mundo";

        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(MessageUtil.toLegacy(cfg().getString("player-shops-browser.offer-name",
                "<yellow>Tienda de <white><owner>").replace("<owner>", owner(shop))));

        List<String> lore = new ArrayList<>();
        if (best) lore.addAll(lines("best-line", List.of("<gold>★ Mejor precio")));
        lore.addAll(lines("offer-lore", List.of(
                "<gray>Vende <white><amount></white> por <green>$<price>",
                "<gray>Precio c/u: <green>$<unit>",
                "<gray>Stock: <white><stock>",
                "<gray>Lugar: <white><world> <x>, <y>, <z>",
                "<gray>Distancia: <white><distance>",
                "",
                "<yellow>Clic para ir a la tienda"),
                "amount", String.valueOf(shop.getAmountPerSale()),
                "price", money(shop.getPrice()),
                "unit", money(offer.unitPrice()),
                "stock", String.valueOf(offer.stock()),
                "world", loc.getWorld().getName(),
                "x", String.valueOf(loc.getBlockX()), "y", String.valueOf(loc.getBlockY()), "z", String.valueOf(loc.getBlockZ()),
                "distance", distance));
        meta.setLore(lore);
        MetaUtil.setTag(meta, "shop_loc", key(loc));
        icon.setItemMeta(meta);
        return icon;
    }

    private static void navigation(Inventory inv, int page, int maxPage, String backKey) {
        if (page > 1) inv.setItem(45, button("prev", Material.ARROW, "<yellow>Anterior", "btn", "prev", null));
        if (page < maxPage) inv.setItem(53, button("next", Material.ARROW, "<yellow>Siguiente", "btn", "next", null));
        if (backKey.equals("back")) {
            inv.setItem(49, button("back", Material.ARROW, "<yellow>Volver", "btn", "back", null));
        } else {
            inv.setItem(49, button("close", Material.BARRIER, "<red>Cerrar", "btn", "close", null));
        }
    }

    // =====================================================
    //                   UTILIDADES
    // =====================================================
    static FileConfiguration cfg() {
        return QSMarketPlus.getInstance().getConfig();
    }

    public static String key(Location loc) {
        return loc.getWorld().getName() + ";" + loc.getBlockX() + ";" + loc.getBlockY() + ";" + loc.getBlockZ();
    }

    private static String title(String key, String fallback, int page, int maxPage) {
        String raw = cfg().getString("player-shops-browser.titles." + key, fallback)
                .replace("{page}", String.valueOf(page)).replace("{max}", String.valueOf(maxPage));
        return MessageUtil.toLegacy(NexoGui.title(raw));
    }

    private static List<String> lines(String key, List<String> fallback, String... vars) {
        String path = "player-shops-browser.lore." + key;
        List<String> raw = cfg().isList(path) ? cfg().getStringList(path) : fallback;
        List<String> out = new ArrayList<>();
        for (String line : raw) {
            for (int i = 0; i + 1 < vars.length; i += 2) {
                line = line.replace("<" + vars[i] + ">", vars[i + 1]);
            }
            out.add(MessageUtil.toLegacy(line));
        }
        return out;
    }

    private static ItemStack button(String key, Material material, String name, String tag, String value, List<String> lore) {
        ConfigurationSection sec = cfg().getConfigurationSection("player-shops-browser.buttons." + key);

        ItemStack item = sec != null ? NexoGui.item(sec.getString("nexo", null)) : null;
        if (item == null && sec != null && sec.contains("material")) {
            Material m = Material.matchMaterial(sec.getString("material"));
            if (m != null) item = new ItemStack(m);
        }
        if (item == null) item = new ItemStack(material);

        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtil.toLegacy(sec != null ? sec.getString("name", name) : name));
        if (lore != null) meta.setLore(lore);
        if (tag != null) MetaUtil.setTag(meta, tag, value);
        item.setItemMeta(meta);
        return item;
    }

    private static void fill(Inventory inv) {
        Material material = Material.matchMaterial(cfg().getString("player-shops-browser.filler", "GRAY_STAINED_GLASS_PANE"));
        if (material == null || material.isAir()) return;

        ItemStack filler = new ItemStack(material);
        ItemMeta meta = filler.getItemMeta();
        meta.setDisplayName(" ");
        filler.setItemMeta(meta);
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) inv.setItem(i, filler);
        }
    }

    static String money(double value) {
        return MONEY.format(value);
    }

    private static String owner(PlayerShop shop) {
        return shop.getOwnerName() != null ? shop.getOwnerName() : "Desconocido";
    }

    private static String itemName(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) return MessageUtil.stripLegacy(meta.getDisplayName());
        String raw = item.getType().name().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }

    private static String stripName(ItemStack item) {
        return itemName(item);
    }
}
