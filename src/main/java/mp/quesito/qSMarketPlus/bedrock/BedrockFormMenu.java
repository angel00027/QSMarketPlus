package mp.quesito.qSMarketPlus.bedrock;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.economia.EconomyProvider;
import mp.quesito.qSMarketPlus.manager.CategoryManager;
import mp.quesito.qSMarketPlus.manager.UniquePurchaseManager;
import mp.quesito.qSMarketPlus.shop.ShopCategory;
import mp.quesito.qSMarketPlus.shop.ShopItem;
import mp.quesito.qSMarketPlus.utils.MessageUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BedrockFormMenu {

    /**
     * Procesa el texto dependiendo de la configuración.
     * Si 'strip' es true, remueve códigos de color y tags de MiniMessage.
     * Si es false, mantiene el formato original o lo pasa a legacy según prefieras.
     */
    private static String formatText(String text, boolean strip) {
        if (text == null) return "";

        // 1. Si se deben limpiar los colores, borramos tanto códigos legacy como tags <...>
        if (strip) {
            return text.replaceAll("(?i)[§&][0-9a-fk-orx]", "")
                    .replaceAll("<[^>]*>", "");
        }

        // 2. Si NO se deben limpiar, convertimos MiniMessage (<red>) a formato Legacy (§c)
        // para que la interfaz nativa de Geyser/Bedrock pueda renderizar el color.
        return MessageUtil.toLegacy(text);
    }

    public static void openCategories(Player player, CategoryManager manager) {
        FileConfiguration config = QSMarketPlus.getInstance().getConfig();
        // 📌 LEER DE LA CONFIGURACIÓN SI SE LIMPIAN LOS COLORES O NO
        boolean strip = config.getBoolean("bedrock-menus.strip-colors", true);

        String title = formatText(config.getString("bedrock-menus.categories.title", "Tienda"), strip);
        String content = formatText(config.getString("bedrock-menus.categories.content", "Selecciona una categoría:"), strip);

        SimpleForm.Builder form = SimpleForm.builder()
                .title(title)
                .content(content);

        List<ShopCategory> activeCategories = new ArrayList<>();

        for (ShopCategory cat : manager.getCategories().values()) {
            if (!cat.canAccess(player)) continue;

            activeCategories.add(cat);
            // Formateo dinámico del botón de categoría
            String catNameClean = formatText(cat.getName(), strip);
            form.button(catNameClean);
        }

        UUID playerUuid = player.getUniqueId();

        form.validResultHandler(response -> {
            int buttonId = response.clickedButtonId();
            if (buttonId >= 0 && buttonId < activeCategories.size()) {
                ShopCategory selectedCategory = activeCategories.get(buttonId);

                Bukkit.getScheduler().runTask(QSMarketPlus.getInstance(), () -> {
                    Player targetPlayer = Bukkit.getPlayer(playerUuid);
                    if (targetPlayer != null && targetPlayer.isOnline()) {
                        openItems(targetPlayer, selectedCategory);
                    }
                });
            }
        });

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form.build());
    }

    public static void openItems(Player player, ShopCategory category) {
        FileConfiguration config = QSMarketPlus.getInstance().getConfig();
        boolean strip = config.getBoolean("bedrock-menus.strip-colors", true);

        String title = formatText(category.getName(), strip);
        String content = formatText(config.getString("bedrock-menus.items.content", "Selecciona un artículo:"), strip);
        String backButtonText = formatText(config.getString("bedrock-menus.items.back-button", "« Volver Atrás"), strip);

        SimpleForm.Builder form = SimpleForm.builder()
                .title(title)
                .content(content);

        var itemManager = QSMarketPlus.getInstance().getItemManager();
        List<ShopItem> items = new ArrayList<>(itemManager.getOrderedItems(category.getId()));
        List<ShopItem> availableItems = new ArrayList<>();

        for (ShopItem item : items) {
            if (!item.canAccess(player)) continue;

            availableItems.add(item);

            // Formateo dinámico del botón del artículo
            String itemNameClean = formatText(item.getName(), strip);
            form.button(itemNameClean);
        }

        form.button(backButtonText);

        UUID playerUuid = player.getUniqueId();

        form.validResultHandler(response -> {
            int buttonId = response.clickedButtonId();

            if (buttonId == availableItems.size()) {
                Bukkit.getScheduler().runTask(QSMarketPlus.getInstance(), () -> {
                    Player targetPlayer = Bukkit.getPlayer(playerUuid);
                    if (targetPlayer != null && targetPlayer.isOnline()) {
                        openCategories(targetPlayer, QSMarketPlus.getInstance().getCategoryManager());
                    }
                });
                return;
            }

            if (buttonId >= 0 && buttonId < availableItems.size()) {
                ShopItem selectedItem = availableItems.get(buttonId);

                Bukkit.getScheduler().runTask(QSMarketPlus.getInstance(), () -> {
                    Player targetPlayer = Bukkit.getPlayer(playerUuid);
                    if (targetPlayer != null && targetPlayer.isOnline()) {
                        openTransactionConfirm(targetPlayer, selectedItem, category);
                    }
                });
            }
        });

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form.build());
    }

    public static void openTransactionConfirm(Player player, ShopItem item, ShopCategory category) {
        FileConfiguration config = QSMarketPlus.getInstance().getConfig();
        boolean strip = config.getBoolean("bedrock-menus.strip-colors", true);

        EconomyProvider ecoProvider = QSMarketPlus.getInstance().getEconomyManager().get(item.getEconomy());

        if (ecoProvider == null) {
            MessageUtil.msg(player, "<red>Error interno: El sistema de economía '" + item.getEconomy() + "' no está registrado.</red>");
            return;
        }

        ItemStack template = item.getRealItem();

        String buyPriceFormatted = MessageUtil.priceFormat("buy", item);
        String sellPriceFormatted = MessageUtil.priceFormat("sell", item);

        boolean canBuy = !buyPriceFormatted.isEmpty();
        boolean canSell = !sellPriceFormatted.isEmpty();

        if (!canBuy && !canSell) {
            MessageUtil.msg(player, "<red>Este artículo no está configurado para transacciones actualmente.</red>");
            return;
        }

        String rawTitle = config.getString("bedrock-menus.confirm.title", "Transacción: %item%");
        String title = formatText(rawTitle.replace("%item%", item.getName()), strip);

        // =========================================================================
        // 🛠️ CONSTRUCCIÓN DINÁMICA DEL LABEL DESDE EL CONFIG.YML
        // =========================================================================
        StringBuilder labelBuilder = new StringBuilder();

        String labelHeader = config.getString("bedrock-menus.confirm.label-header", "Detalles del Artículo:");
        labelBuilder.append(formatText(labelHeader, strip)).append("\n");

        if (canBuy) {
            labelBuilder.append("• ").append(formatText(buyPriceFormatted, strip)).append("\n");
        }
        if (canSell) {
            labelBuilder.append("• ").append(formatText(sellPriceFormatted, strip)).append("\n");
        }

        double currentBalance = ecoProvider.getBalance(player);
        String rawBalanceStr = config.getString("bedrock-menus.confirm.label-balance", "Tu Saldo actual: %balance%");
        String balanceStr = rawBalanceStr.replace("%balance%", String.format("%.2f", currentBalance));

        labelBuilder.append("\n").append(formatText(balanceStr, strip));
        String label = labelBuilder.toString();

        // =========================================================================
        // 🎛️ TEXTOS DE LOS COMPONENTES DESDE EL CONFIG.YML
        // =========================================================================
        String sliderText = formatText(config.getString("bedrock-menus.confirm.slider", "Cantidad"), strip);
        String toggleText = formatText(config.getString("bedrock-menus.confirm.toggle-mode", "Modo: COMPRA [Apagado] / VENTA [Encendido]"), strip);
        String backToggleText = formatText(config.getString("bedrock-menus.confirm.toggle-back", "¿Volver a la lista de artículos?"), strip);

        CustomForm.Builder form = CustomForm.builder()
                .title(title)
                .label(label)
                .slider(sliderText, 1, 64, 1, 1)
                .toggle(toggleText, false)
                .toggle(backToggleText, false);

        UUID playerUuid = player.getUniqueId();

        form.validResultHandler(response -> {
            float cantidadFloat = response.asSlider(1);
            int cantidad = Math.round(cantidadFloat);
            boolean esVenta = response.asToggle(2);
            boolean volverAtras = response.asToggle(3);

            Bukkit.getScheduler().runTask(QSMarketPlus.getInstance(), () -> {
                Player targetPlayer = Bukkit.getPlayer(playerUuid);
                if (targetPlayer == null || !targetPlayer.isOnline()) return;

                if (volverAtras) {
                    openItems(targetPlayer, category);
                    return;
                }

                String currencyName = config.getString("economies." + ecoProvider.getName() + ".display-name", ecoProvider.getName());
                // Para las respuestas del chat/lang se suele preferir limpio, pero usamos la variable también aquí si se desea.
                String cleanedItemName = formatText(item.getName(), strip);

                // =========================================================================
                // 🟢 SECCIÓN DE VENTA
                // =========================================================================
                if (esVenta) {
                    if (!canSell) {
                        MessageUtil.lang(targetPlayer, "cant_sell");
                        return;
                    }

                    double totalEarnings = item.getSell() * cantidad;

                    int totalEncontrado = 0;
                    for (ItemStack invItem : targetPlayer.getInventory().getContents()) {
                        if (invItem != null && invItem.getType() == template.getType()) {
                            if (!template.hasItemMeta() || invItem.isSimilar(template)) {
                                totalEncontrado += invItem.getAmount();
                            }
                        }
                    }

                    if (totalEncontrado < cantidad) {
                        MessageUtil.lang(targetPlayer, "no_items",
                                Placeholder.parsed("amount", String.valueOf(cantidad)),
                                Placeholder.parsed("item", cleanedItemName)
                        );
                        return;
                    }

                    int cantidadPorRemover = cantidad;
                    ItemStack[] contents = targetPlayer.getInventory().getContents();

                    for (int i = 0; i < contents.length; i++) {
                        ItemStack invItem = contents[i];
                        if (invItem != null && invItem.getType() == template.getType()) {
                            if (!template.hasItemMeta() || invItem.isSimilar(template)) {
                                if (invItem.getAmount() <= cantidadPorRemover) {
                                    cantidadPorRemover -= invItem.getAmount();
                                    contents[i] = null;
                                } else {
                                    invItem.setAmount(invItem.getAmount() - cantidadPorRemover);
                                    cantidadPorRemover = 0;
                                }
                            }
                        }
                        if (cantidadPorRemover == 0) break;
                    }

                    targetPlayer.getInventory().setContents(contents);

                    boolean depositSuccess = ecoProvider.deposit(targetPlayer, totalEarnings);
                    if (!depositSuccess) {
                        MessageUtil.msg(targetPlayer, "<red>Error al procesar el depósito en tu cuenta de " + ecoProvider.getName() + ".</red>");
                        return;
                    }

                    MessageUtil.lang(targetPlayer, "sell_success",
                            Placeholder.parsed("amount", String.valueOf(cantidad)),
                            Placeholder.parsed("item", cleanedItemName),
                            Placeholder.parsed("price", String.format("%.2f", totalEarnings)),
                            Placeholder.parsed("currency", currencyName)
                    );

                    // =========================================================================
                    // 🔴 SECCIÓN DE COMPRA
                    // =========================================================================
                } else {
                    if (!canBuy) {
                        MessageUtil.msg(targetPlayer, "<red>Este artículo no se encuentra disponible para la compra.</red>");
                        return;
                    }

                    UniquePurchaseManager upManager = QSMarketPlus.getInstance().getUniquePurchaseManager();
                    if (item.isOnlyOnce() && upManager.hasPurchased(targetPlayer, item)) {
                        MessageUtil.lang(targetPlayer, "already_bought");
                        return;
                    }

                    if (item.getCommands().isEmpty() && targetPlayer.getInventory().firstEmpty() == -1) {
                        MessageUtil.lang(targetPlayer, "not_enough_space");
                        return;
                    }

                    double totalCost = item.getBuy() * cantidad;

                    if (ecoProvider.getBalance(targetPlayer) < totalCost) {
                        MessageUtil.lang(targetPlayer, "no_money",
                                Placeholder.parsed("currency", currencyName)
                        );
                        return;
                    }

                    boolean transactionSuccess = ecoProvider.withdraw(targetPlayer, totalCost);
                    if (!transactionSuccess) {
                        MessageUtil.msg(targetPlayer, "<red>Error al procesar el cobro con el sistema de economía: " + ecoProvider.getName() + ".</red>");
                        return;
                    }

                    if (!item.getCommands().isEmpty()) {
                        for (int i = 0; i < cantidad; i++) {
                            item.executeBuyCommands(targetPlayer);
                        }
                    } else {
                        ItemStack reward = template.clone();
                        reward.setAmount(cantidad);

                        var leftovers = targetPlayer.getInventory().addItem(reward);
                        if (!leftovers.isEmpty()) {
                            for (ItemStack remaining : leftovers.values()) {
                                targetPlayer.getWorld().dropItemNaturally(targetPlayer.getLocation(), remaining);
                            }
                        }
                    }

                    if (item.isOnlyOnce()) {
                        upManager.markPurchased(targetPlayer, item);
                    }

                    MessageUtil.lang(targetPlayer, "buy_success",
                            Placeholder.parsed("amount", String.valueOf(cantidad)),
                            Placeholder.parsed("item", cleanedItemName),
                            Placeholder.parsed("price", String.format("%.2f", totalCost)),
                            Placeholder.parsed("currency", currencyName)
                    );
                }
            });
        });

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form.build());
    }
}