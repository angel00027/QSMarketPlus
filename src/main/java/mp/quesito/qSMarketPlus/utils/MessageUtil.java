package mp.quesito.qSMarketPlus.utils;

import mp.quesito.qSMarketPlus.QSMarketPlus;
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
    //   Enviar mensajes
    // -------------------------
    public static void msg(Player player, String message, TagResolver... placeholders) {
        adventure.player(player).sendMessage(
                mm.deserialize(message, placeholders)
        );
    }
    public static void lang(Player player, String key, TagResolver... placeholders) {
        String raw = Lang.get(key);

        adventure.player(player).sendMessage(
                mm.deserialize(raw, placeholders)
        );
    }


    // Quitar códigos legacy para evitar crasheo con MiniMessage
    public static String stripLegacy(String text) {
        if (text == null) return "";

        // Eliminar códigos hex: §x§A§B§C§D§E§F
        text = text.replaceAll("§x(§[0-9A-Fa-f]){6}", "");

        // Eliminar todos los códigos legacy estándar §a §b §l §o §r ...
        text = text.replaceAll("§[0-9A-FK-ORa-fk-or]", "");

        return text;
    }



    public static String toLegacy(String mmText) {

        // Si ya contiene códigos § → NO procesar con MiniMessage
        if (mmText.contains("§")) {
            return mmText;
        }

        // Si contiene MiniMessage (<red>, <green>, etc)
        if (mmText.contains("<")) {
            return LegacyComponentSerializer.legacySection().serialize(
                    mm.deserialize(mmText)
            );
        }

        // Si no contiene ni § ni < > → se devuelve tal cual
        return mmText;
    }




    public static List<String> toLegacy(List<String> lines) {
        if (lines == null) return null;

        return lines.stream()
                .map(MessageUtil::toLegacy)
                .collect(Collectors.toList());
    }


    public static List<String> loreToLegacy(List<String> lines) {
        return lines.stream()
                .map(MessageUtil::toLegacy)
                .collect(Collectors.toList());
    }

    public static String placeholders(String text, double buy, double sell) {
        return text
                .replace("%buy%", String.valueOf(buy))
                .replace("%sell%", String.valueOf(sell));
    }

    public static String priceFormat(String key, double buy, double sell) {

        // Ocultar precio de compra si buy <= 0
        if (key.equals("buy") && buy <= 0)
            return "";

        // Ocultar precio de venta si sell <= 0
        if (key.equals("sell") && sell <= 0)
            return "";

        // Tomar formato de mensajes.yml
        String format = Lang.get("price-format." + key);
        if (format == null) return "";

        return toLegacy(placeholders(format, buy, sell));
    }



    // -------------------------
    //   TÍTULOS
    // -------------------------
    public static void title(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        adventure.player(player).showTitle(
                net.kyori.adventure.title.Title.title(
                        mm.deserialize(title),
                        mm.deserialize(subtitle)
                )
        );
    }

    public static void title(Player player, String title, String subtitle) {
        title(player, title, subtitle, 10, 40, 10);
    }

    // -------------------------
    //   ACTIONBAR
    // -------------------------
    public static void action(Player player, String msg) {
        adventure.player(player).sendActionBar(mm.deserialize(msg));
    }

    // -------------------------
    //   ITEM DISPLAY
    // -------------------------

    // Nombre de item
    public static Component itemName(String message) {
        return mm.deserialize(message);
    }

    // Lore de item
    public static List<Component> itemLore(List<String> lines) {
        return lines.stream()
                .map(mm::deserialize)
                .collect(Collectors.toList());
    }
}
