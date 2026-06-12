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
     * Utilidad para remover de raíz cualquier formato de color o tag de MiniMessage
     * y asegurar legibilidad absoluta en los botones nativos de Bedrock.
     */
    private static String stripColor(String text) {
        if (text == null) return "";
        // Convierte el texto usando el método de tu plugin
        String legacy = MessageUtil.toLegacy(text);
        // Remueve secuencias de colores tradicionales (§a, &e) y tags de MiniMessage (<red>, </green>)
        return legacy.replaceAll("(?i)[§&][0-9a-fk-orx]", "")
                .replaceAll("<[^>]*>", "");
    }

    public static void openCategories(Player player, CategoryManager manager) {
        FileConfiguration config = QSMarketPlus.getInstance().getConfig();

        String title = stripColor(config.getString("bedrock-menus.categories.title", "Tienda"));
        String content = stripColor(config.getString("bedrock-menus.categories.content", "Selecciona una categoría:"));

        SimpleForm.Builder form = SimpleForm.builder()
                .title(title)
                .content(content);

        List<ShopCategory> activeCategories = new ArrayList<>();

        for (ShopCategory cat : manager.getCategories().values()) {
            if (!cat.canAccess(player)) continue;

            activeCategories.add(cat);
            // Botón limpio de colores para Bedrock
            String catNameClean = stripColor(cat.getName());
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

        String title = stripColor(category.getName());
        String content = stripColor(config.getString("bedrock-menus.items.content", "Selecciona un artículo:"));
        String backButtonText = stripColor(config.getString("bedrock-menus.items.back-button", "« Volver Atrás"));

        SimpleForm.Builder form = SimpleForm.builder()
                .title(title)
                .content(content);

        var itemManager = QSMarketPlus.getInstance().getItemManager();
        List<ShopItem> items = new ArrayList<>(itemManager.getOrderedItems(category.getId()));
        List<ShopItem> availableItems = new ArrayList<>();

        for (ShopItem item : items) {
            if (!item.canAccess(player)) continue;

            availableItems.add(item);

            // Nombre del artículo completamente limpio para evitar fallos de renderizado
            String itemNameClean = stripColor(item.getName());
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
        String title = stripColor(rawTitle.replace("%item%", item.getName()));

        // =========================================================================
        // 🛠️ CONSTRUCCIÓN DINÁMICA DEL LABEL DESDE EL CONFIG.YML
        // =========================================================================
        StringBuilder labelBuilder = new StringBuilder();

        String labelHeader = config.getString("bedrock-menus.confirm.label-header", "Detalles del Artículo:");
        labelBuilder.append(stripColor(labelHeader)).append("\n");

        if (canBuy) {
            labelBuilder.append("• ").append(stripColor(buyPriceFormatted)).append("\n");
        }
        if (canSell) {
            labelBuilder.append("• ").append(stripColor(sellPriceFormatted)).append("\n");
        }

        double currentBalance = ecoProvider.getBalance(player);
        String rawBalanceStr = config.getString("bedrock-menus.confirm.label-balance", "Tu Saldo actual: %balance%");
        String balanceStr = rawBalanceStr.replace("%balance%", String.format("%.2f", currentBalance));

        labelBuilder.append("\n").append(stripColor(balanceStr));
        String label = labelBuilder.toString();

        // =========================================================================
        // 🎛️ TEXTOS DE LOS COMPONENTES DESDE EL CONFIG.YML
        // =========================================================================
        String sliderText = stripColor(config.getString("bedrock-menus.confirm.slider", "Cantidad"));
        String toggleText = stripColor(config.getString("bedrock-menus.confirm.toggle-mode", "Modo: COMPRA [Apagado] / VENTA [Encendido]"));
        String backToggleText = stripColor(config.getString("bedrock-menus.confirm.toggle-back", "¿Volver a la lista de artículos?"));

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
                String cleanedItemName = stripColor(item.getName());

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
                        // 🔥 FIJADO: Uso de Placeholders individuales separados por comas
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

                    // 🔥 FIJADO: Uso de Placeholders individuales separados por comas
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
                        // 🔥 FIJADO: Uso de Placeholder individual
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

                    // 🔥 FIJADO: Uso de Placeholders individuales separados por comas
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