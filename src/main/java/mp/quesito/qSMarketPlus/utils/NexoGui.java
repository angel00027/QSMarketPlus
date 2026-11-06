package mp.quesito.qSMarketPlus.utils;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Iconos y fondos de Nexo para los menus, sin depender de Nexo al compilar.
 *
 * Botones: en el YAML se pone  nexo: "qsm_confirmar"  y el material (o la cabeza)
 * queda de respaldo si Nexo no esta o el item no existe.
 *
 * Titulos: aceptan %shift:N% (N pixeles, negativo = izquierda), que se cambia por
 * los caracteres de la fuente nexo:shift. Nexo la referencia dentro de
 * minecraft:default, asi que sirve en titulos de texto normal.
 */
public final class NexoGui {

    private static final Pattern SHIFT = Pattern.compile("%shift:(-?\\d+)%|<shift:(-?\\d+)>");
    private static final Pattern GLYPH = Pattern.compile("<(?:glyph|g):([a-zA-Z0-9_\\-]+)(?::[a-z]+)?>");

    /*
     * QSMarketPlus lleva su propia copia de Adventure (reubicada), asi que no puede usar
     * los tags de Nexo. Los caracteres de cada glifo se leen de plugins/Nexo/glyphs.
     */
    private static final long GLYPH_CACHE_MS = 30_000L;
    private static Map<String, String> glyphs = new HashMap<>();
    private static long glyphsLoadedAt;

    // nexo:shift: .. = -1, -2, -4 ... -512; .. = +1 ... +512
    private static final char[] NEGATIVE = {
            '', '', '', '', '',
            '', '', '', '', ''
    };

    private static final char[] POSITIVE = {
            '', '', '', '', '',
            '', '', '', '', ''
    };

    private NexoGui() {
    }

    /** Item de Nexo sin sus datos internos, para que Nexo no lo trate como item real. */
    public static ItemStack item(String id) {

        if (id == null || id.isBlank() || !Bukkit.getPluginManager().isPluginEnabled("Nexo")) {
            return null;
        }

        try {
            Class<?> nexoItems = Class.forName("com.nexomc.nexo.api.NexoItems");
            Object builder = nexoItems.getMethod("itemFromId", String.class).invoke(null, id.trim());
            if (builder == null) return null;

            Object built = builder.getClass().getMethod("build").invoke(builder);
            if (!(built instanceof ItemStack item)) return null;

            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                for (NamespacedKey key : meta.getPersistentDataContainer().getKeys()) {
                    if (key.getNamespace().equals("nexo")) {
                        meta.getPersistentDataContainer().remove(key);
                    }
                }
                item.setItemMeta(meta);
            }
            return item;

        } catch (Throwable ignored) {
            return null;
        }
    }

    public static String shift(int pixels) {

        if (pixels == 0) return "";

        char[] table = pixels < 0 ? NEGATIVE : POSITIVE;
        int rest = Math.abs(pixels);

        StringBuilder sb = new StringBuilder();
        for (int i = table.length - 1; i >= 0; i--) {
            int value = 1 << i;
            while (rest >= value) {
                sb.append(table[i]);
                rest -= value;
            }
        }
        return sb.toString();
    }

    /** Cambia <glyph:id>, <g:id>, <shift:N> y %shift:N% por sus caracteres. */
    public static String title(String text) {

        if (text == null) return null;

        if (text.contains("<glyph:") || text.contains("<g:")) {
            Map<String, String> chars = glyphs();
            Matcher g = GLYPH.matcher(text);
            StringBuilder sb = new StringBuilder();
            while (g.find()) {
                String glyph = chars.getOrDefault(g.group(1).toLowerCase(), "");
                m_append(g, sb, glyph);
            }
            g.appendTail(sb);
            text = sb.toString();
        }

        if (!text.contains("shift:")) return text;

        Matcher m = SHIFT.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String value = m.group(1) != null ? m.group(1) : m.group(2);
            m_append(m, sb, shift(Integer.parseInt(value)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static void m_append(Matcher m, StringBuilder sb, String replacement) {
        m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
    }

    private static synchronized Map<String, String> glyphs() {

        long now = System.currentTimeMillis();
        if (now - glyphsLoadedAt < GLYPH_CACHE_MS) return glyphs;

        Map<String, String> found = new HashMap<>();
        File folder = new File(Bukkit.getWorldContainer(), "plugins/Nexo/glyphs");
        readGlyphs(folder, found);

        glyphs = found;
        glyphsLoadedAt = now;
        return glyphs;
    }

    private static void readGlyphs(File folder, Map<String, String> out) {

        File[] files = folder.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                readGlyphs(file, out);
                continue;
            }
            if (!file.getName().endsWith(".yml")) continue;

            YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
            for (String id : yml.getKeys(false)) {
                ConfigurationSection sec = yml.getConfigurationSection(id);
                if (sec == null) continue;

                Object chars = sec.get("char");
                if (chars instanceof String single) {
                    out.put(id.toLowerCase(), single);
                } else if (chars instanceof List<?> rows && !rows.isEmpty()) {
                    out.put(id.toLowerCase(), String.valueOf(rows.get(0)));
                }
            }
        }
    }
}
