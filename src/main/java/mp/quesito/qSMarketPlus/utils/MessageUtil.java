package mp.quesito.qSMarketPlus.utils;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.economia.EconomyProvider;
import mp.quesito.qSMarketPlus.shop.ShopItem;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class MessageUtil {

    private static BukkitAudiences adventure;
    private static QSMarketPlus plugin;

    private static final MiniMessage mm = MiniMessage.miniMessage();

    // Inicializar desde el plugin principal
    public static void init(QSMarketPlus pl) {
        plugin = pl;
        adventure = pl.adventure();
    }

    // -------------------------
    //   MiniMessage seguro
    // -------------------------

    /**
     * Deserializa MiniMessage.
     *
     * Si encuentra códigos legacy (§a, §f, §l, etc.),
     * los convierte automáticamente a MiniMessage.
     *
     * Si aun así falla, elimina los códigos legacy como
     * último recurso para evitar que el plugin genere una excepción.
     */
    public static Component safeDeserialize(String message, TagResolver... placeholders) {

        if (message == null || message.isEmpty()) {
            return Component.empty();
        }

        try {
            // Primer intento: MiniMessage normal
            return mm.deserialize(message, placeholders);

        } catch (Exception firstError) {

            // Segundo intento: convertir códigos legacy
            try {
                String converted = legacyToMiniMessage(message);

                return mm.deserialize(converted, placeholders);

            } catch (Exception secondError) {

                // Último recurso: eliminar códigos legacy
                try {
                    String stripped = stripLegacy(message);

                    return mm.deserialize(stripped, placeholders);

                } catch (Exception ignored) {

                    // Evita que un mensaje roto tumbe la tarea
                    return Component.text(stripLegacy(message));
                }
            }
        }
    }


    // -------------------------
    //   Enviar mensajes
    // -------------------------

    public static void msg(Player player, String message, TagResolver... placeholders) {

        adventure.player(player).sendMessage(
                safeDeserialize(message, placeholders)
        );
    }


    public static void lang(Player player, String key, TagResolver... placeholders) {

        String raw = Lang.get(key);

        adventure.player(player).sendMessage(
                safeDeserialize(raw, placeholders)
        );
    }


    // -------------------------
    //   Legacy -> MiniMessage
    // -------------------------

    /**
     * Convierte códigos legacy de Minecraft a MiniMessage.
     *
     * Ejemplo:
     *
     * §fPan
     * ->
     * <white>Pan</white>
     *
     * §aHola §cMundo
     * ->
     * <green>Hola <red>Mundo
     */
    public static String legacyToMiniMessage(String text) {

        if (text == null || text.isEmpty()) {
            return "";
        }

        // Colores normales
        text = text
                .replace("§0", "<black>")
                .replace("§1", "<dark_blue>")
                .replace("§2", "<dark_green>")
                .replace("§3", "<dark_aqua>")
                .replace("§4", "<dark_red>")
                .replace("§5", "<dark_purple>")
                .replace("§6", "<gold>")
                .replace("§7", "<gray>")
                .replace("§8", "<dark_gray>")
                .replace("§9", "<blue>")
                .replace("§a", "<green>")
                .replace("§b", "<aqua>")
                .replace("§c", "<red>")
                .replace("§d", "<light_purple>")
                .replace("§e", "<yellow>")
                .replace("§f", "<white>");

        // Formatos
        text = text
                .replace("§k", "<obfuscated>")
                .replace("§l", "<bold>")
                .replace("§m", "<strikethrough>")
                .replace("§n", "<underlined>")
                .replace("§o", "<italic>")
                .replace("§r", "<reset>");

        // También soportar &, por si algún nombre usa &f
        text = text
                .replace("&0", "<black>")
                .replace("&1", "<dark_blue>")
                .replace("&2", "<dark_green>")
                .replace("&3", "<dark_aqua>")
                .replace("&4", "<dark_red>")
                .replace("&5", "<dark_purple>")
                .replace("&6", "<gold>")
                .replace("&7", "<gray>")
                .replace("&8", "<dark_gray>")
                .replace("&9", "<blue>")
                .replace("&a", "<green>")
                .replace("&b", "<aqua>")
                .replace("&c", "<red>")
                .replace("&d", "<light_purple>")
                .replace("&e", "<yellow>")
                .replace("&f", "<white>");

        return text;
    }


    // -------------------------
    //   Quitar Legacy
    // -------------------------

    public static String stripLegacy(String text) {

        if (text == null) {
            return "";
        }

        // Hex legacy:
        // §x§A§B§C§D§E§F
        text = text.replaceAll(
                "§x(§[0-9A-Fa-f]){6}",
                ""
        );

        // Legacy normal:
        // §0-§9, §a-§f, §k-§o, §r
        text = text.replaceAll(
                "§[0-9A-FK-ORa-fk-or]",
                ""
        );

        return text;
    }


    // -------------------------
    //   MiniMessage -> Legacy
    // -------------------------

    public static String toLegacy(String mmText) {

        if (mmText == null) {
            return "";
        }

        // Si ya contiene códigos §, devolverlos tal cual
        if (mmText.contains("§")) {
            return mmText;
        }

        // Si contiene MiniMessage
        if (mmText.contains("<")) {

            try {

                return LegacyComponentSerializer
                        .legacySection()
                        .serialize(
                                safeDeserialize(mmText)
                        );

            } catch (Exception e) {

                // Último recurso
                return stripLegacy(mmText);
            }
        }

        return mmText;
    }


    public static List<String> toLegacy(List<String> lines) {

        if (lines == null) {
            return null;
        }

        return lines.stream()
                .map(MessageUtil::toLegacy)
                .collect(Collectors.toList());
    }


    public static List<String> loreToLegacy(List<String> lines) {

        if (lines == null) {
            return null;
        }

        return lines.stream()
                .map(MessageUtil::toLegacy)
                .collect(Collectors.toList());
    }


    // -------------------------
    //   Placeholders
    // -------------------------

    public static String placeholders(
            String text,
            double buy,
            double sell
    ) {

        if (text == null) {
            return "";
        }

        return text
                .replace("%buy%", String.valueOf(buy))
                .replace("%sell%", String.valueOf(sell));
    }


    // -------------------------
    //   Precio
    // -------------------------

    public static String priceFormat(String key, ShopItem item) {

        double buy = item.getBuy();
        double sell = item.getSell();

        if (key.equals("buy") && buy <= 0) {
            return "";
        }

        if (key.equals("sell") && sell <= 0) {
            return "";
        }

        String format = Lang.get("price-format." + key);

        if (format == null) {
            return "";
        }

        EconomyProvider eco = QSMarketPlus.getInstance()
                .getEconomyManager()
                .get(item.getEconomy());

        String symbol = "$";
        String currencyName = "Money";

        if (eco != null) {

            symbol = eco.getSymbol();

            String id = eco.getName();

            currencyName = QSMarketPlus.getInstance()
                    .getConfig()
                    .getString(
                            "economies." + id + ".display-name",
                            id
                    );
        }

        double price = key.equals("buy") ? buy : sell;

        return toLegacy(
                format
                        .replace("%symbol%", symbol)
                        .replace("%price%", String.valueOf(price))
                        .replace("%currency%", currencyName)
        );
    }


    // -------------------------
    //   Títulos
    // -------------------------

    public static void title(
            Player player,
            String title,
            String subtitle,
            int fadeIn,
            int stay,
            int fadeOut
    ) {

        adventure.player(player).showTitle(
                net.kyori.adventure.title.Title.title(
                        safeDeserialize(title),
                        safeDeserialize(subtitle)
                )
        );
    }


    public static void title(
            Player player,
            String title,
            String subtitle
    ) {

        title(
                player,
                title,
                subtitle,
                10,
                40,
                10
        );
    }
}

