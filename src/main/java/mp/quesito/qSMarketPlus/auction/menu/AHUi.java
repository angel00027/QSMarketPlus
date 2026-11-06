package mp.quesito.qSMarketPlus.auction.menu;

import mp.quesito.qSMarketPlus.auction.AuctionItem;
import mp.quesito.qSMarketPlus.manager.AHConfig;
import mp.quesito.qSMarketPlus.utils.MenuItems;
import mp.quesito.qSMarketPlus.utils.MessageUtil;
import mp.quesito.qSMarketPlus.utils.MetaUtil;
import mp.quesito.qSMarketPlus.utils.NexoGui;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Aspecto de los menus de subasta, leido de la seccion "ui" de ah_menu.yml.
 *
 * ui.titles.<menu>      titulo (acepta <glyph:id> y <shift:N> para fondos de Nexo)
 * ui.filler             material de relleno; AIR deja ver el fondo
 * ui.buttons.<boton>    nexo / head / material / name / lore de cada boton
 *
 * Sin la seccion "ui" todo se ve como antes: cristal gris y botones vanilla.
 */
public final class AHUi {

    private static final DecimalFormat MONEY =
            new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(Locale.US));

    private AHUi() {
    }

    private static FileConfiguration cfg() {
        return AHConfig.get();
    }

    public static String title(String menu, String fallback) {
        return MessageUtil.toLegacy(NexoGui.title(cfg().getString("ui.titles." + menu, fallback)));
    }

    /** Rellena solo los huecos vacios. */
    public static void fill(Inventory inv) {
        Material material = Material.matchMaterial(cfg().getString("ui.filler", "GRAY_STAINED_GLASS_PANE"));
        if (material == null || material.isAir()) return;

        ItemStack filler = new ItemStack(material);
        ItemMeta meta = filler.getItemMeta();
        meta.setDisplayName(" ");
        filler.setItemMeta(meta);

        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) inv.setItem(i, filler);
        }
    }

    /** Boton configurable con valores por defecto; tag = clave y valor de su etiqueta. */
    public static ItemStack button(String key, Material material, String name, String tag, String value) {

        ConfigurationSection sec = cfg().getConfigurationSection("ui.buttons." + key);

        ItemStack item = null;
        if (sec != null) {
            item = NexoGui.item(sec.getString("nexo", null));
            if (item == null && sec.contains("head")) {
                item = MenuItems.customHead(sec.getString("name", name), sec.getString("head"));
            }
            if (item == null && sec.contains("material")) {
                Material m = Material.matchMaterial(sec.getString("material"));
                if (m != null) item = new ItemStack(m);
            }
        }
        if (item == null) item = new ItemStack(material);

        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtil.toLegacy(sec != null ? sec.getString("name", name) : name));
        if (sec != null && sec.isList("lore")) {
            meta.setLore(MessageUtil.loreToLegacy(sec.getStringList("lore")));
        }
        if (tag != null) MetaUtil.setTag(meta, tag, value);
        item.setItemMeta(meta);
        return item;
    }

    public static String money(double value) {
        return MONEY.format(value);
    }

    public static String seller(AuctionItem auc) {
        String name = Bukkit.getOfflinePlayer(auc.seller).getName();
        return name != null ? name : "Desconocido";
    }

    /** "2d 4h", "3h 15m", "12m" o "menos de 1m". */
    public static String timeLeft(long expiresAt) {
        long ms = expiresAt - System.currentTimeMillis();
        if (ms <= 60_000L) return "menos de 1m";

        long minutes = ms / 60_000L;
        long days = minutes / 1440;
        long hours = (minutes % 1440) / 60;
        long mins = minutes % 60;

        if (days > 0) return days + "d " + hours + "h";
        if (hours > 0) return hours + "h " + mins + "m";
        return mins + "m";
    }

    /** Lineas de ui.lore.<key> con variables <var>; si no hay, usa las dadas. */
    public static List<String> lore(String key, List<String> fallback, String... vars) {

        List<String> raw = cfg().isList("ui.lore." + key) ? cfg().getStringList("ui.lore." + key) : fallback;
        List<String> out = new ArrayList<>();

        for (String line : raw) {
            for (int i = 0; i + 1 < vars.length; i += 2) {
                line = line.replace("<" + vars[i] + ">", vars[i + 1]);
            }
            out.add(MessageUtil.toLegacy(line));
        }
        return out;
    }

    public static int countItems(ItemStack[] container) {
        int count = 0;
        if (container == null) return 0;
        for (ItemStack it : container) {
            if (it != null && !it.getType().isAir()) count++;
        }
        return count;
    }
}
