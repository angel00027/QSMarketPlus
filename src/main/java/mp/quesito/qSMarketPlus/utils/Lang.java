package mp.quesito.qSMarketPlus.utils;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Lang {

    private static QSMarketPlus plugin;
    private static YamlConfiguration langConfig;

    public static void init(QSMarketPlus pl) {
        plugin = pl;

        String selected = plugin.getConfig().getString("language", "es");

        // Archivo en plugins/QSMarketPlus/messages/messages_es.yml
        File file = new File(plugin.getDataFolder(), "messages/messages_" + selected + ".yml");

        // Si no existe → copiar desde resources/messages/
        if (!file.exists()) {
            plugin.saveResource("messages/messages_" + selected + ".yml", false);
        }

        langConfig = YamlConfiguration.loadConfiguration(file);
        plugin.getLogger().info("[QSMarketPlus] Archivo de idioma cargado: " + file.getName());
    }

    public static String get(String key) {
        return langConfig.getString(key, "<red>Missing message: " + key + "</red>");
    }

    public static void msg(Player p, String key, Object... placeholders) {

        String raw = get(key);

        List<TagResolver> resolvers = new ArrayList<>();

        // Recorrer placeholder1, valor1, placeholder2, valor2 ...
        for (int i = 0; i < placeholders.length - 1; i += 2) {
            String name = String.valueOf(placeholders[i]);
            String value = String.valueOf(placeholders[i + 1]);

            // ESTE es el correcto:
            resolvers.add(Placeholder.unparsed(name, value));
        }

        MessageUtil.msg(p, raw, resolvers.toArray(TagResolver[]::new));
    }

    public static void broadcast(String message) {
        MiniMessage mm = MiniMessage.miniMessage();
        Component comp = mm.deserialize(message);

        for (Player p : Bukkit.getOnlinePlayers()) {
            QSMarketPlus.getInstance().adventure().player(p).sendMessage(comp);
        }
    }

    public static void discord(String message) {

        if (!QSMarketPlus.getInstance().getConfig().getBoolean("discord.webhook_enabled"))
            return;

        String url = QSMarketPlus.getInstance().getConfig().getString("discord.webhook_url");

        if (url == null || url.isEmpty()) return;

        DiscordWebhook.send(url, message);
    }

}
