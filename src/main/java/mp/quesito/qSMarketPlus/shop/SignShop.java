package mp.quesito.qSMarketPlus.shop;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.utils.MessageUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SignShop {

    private final Location signLocation;
    private final ShopItem item;
    private final String ownerUUID;

    private double buyPrice;
    private double sellPrice;
    private int amount;
    private boolean active = true;

    public SignShop(Location signLocation, ShopItem item, double buyPrice, double sellPrice, int amount, String ownerUUID) {
        this.signLocation = signLocation;
        this.item = item;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
        this.amount = amount;
        this.ownerUUID = ownerUUID;
    }

    // ===================== GETTERS =====================
    public ShopItem getItem() { return item; }
    public double getBuyPrice() { return buyPrice; }
    public double getSellPrice() { return sellPrice; }
    public int getAmount() { return amount; }
    public Location getSignLocation() { return signLocation; }
    public String getOwnerUUID() { return ownerUUID; }
    public boolean isActive() { return active; }

    public void setActive(boolean active) {
        this.active = active;
        updateSign();
    }

    // ===================== CARTEL =====================
    public void updateSign() {
        Block block = signLocation.getBlock();
        if (!(block.getState() instanceof Sign sign)) return;

        if (!active) {
            sign.setLine(0, "§cTIENDA");
            sign.setLine(1, "§7Inactiva");
            sign.setLine(2, "");
            sign.setLine(3, "");
            sign.update();
            return;
        }

        String itemName = MessageUtil.toLegacy(item.getName());
        if (itemName.length() > 15) itemName = itemName.substring(0, 15);

        sign.setLine(0, "§aTIENDA");
        sign.setLine(1, itemName);
        sign.setLine(2, MessageUtil.priceFormat("buy", item));
        sign.setLine(3, MessageUtil.priceFormat("sell", item));
        sign.update();
    }

    // ===================== COMPRAR =====================
    public void buy(Player player) {
        if (!active) {
            MessageUtil.lang(player, "shop.inactive");
            return;
        }

        if (buyPrice <= 0) {
            MessageUtil.lang(player, "shop.buy-disabled");
            return;
        }

        double total = buyPrice * amount;

        if (QSMarketPlus.economy.getBalance(player) < total) {
            MessageUtil.lang(player, "shop.no-money");
            return;
        }

        ItemStack toGive = item.getRealItem();
        toGive.setAmount(amount);

        if (player.getInventory().firstEmpty() == -1) {
            MessageUtil.lang(player, "shop.full-inventory");
            return;
        }

        EconomyResponse response = QSMarketPlus.economy.withdrawPlayer(player, total);
        if (!response.transactionSuccess()) {
            MessageUtil.lang(player, "shop.transaction-error");
            return;
        }

        player.getInventory().addItem(toGive);

        MessageUtil.lang(player, "shop.buy-success",
                Placeholder.unparsed("amount", String.valueOf(amount)),
                Placeholder.parsed("item", item.getName()), // ¡Aquí solo item.getName() sin toLegacy!
                Placeholder.unparsed("price", String.valueOf(total))
        );


        if (item.getCommands() != null) {
            item.getCommands().forEach(cmd ->
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", player.getName()))
            );
        }
    }

    // ===================== VENDER =====================
    public void sell(Player player) {
        if (!active) {
            MessageUtil.lang(player, "shop.inactive");
            return;
        }

        if (sellPrice <= 0) {
            MessageUtil.lang(player, "shop.sell-disabled");
            return;
        }

        ItemStack required = item.getRealItem();
        required.setAmount(amount);

        if (!player.getInventory().containsAtLeast(required, amount)) {
            MessageUtil.lang(player, "shop.not-enough-items");
            return;
        }

        player.getInventory().removeItem(required);

        double total = sellPrice * amount;
        EconomyResponse response = QSMarketPlus.economy.depositPlayer(player, total);
        if (!response.transactionSuccess()) {
            MessageUtil.lang(player, "shop.transaction-error");
            return;
        }

        MessageUtil.lang(player, "shop.sell-success",
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed("amount", String.valueOf(amount)),
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("item", item.getName()), // <--- directamente, sin toLegacy
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed("price", String.valueOf(total))
        );


        if (item.getSellCommands() != null) {
            item.getSellCommands().forEach(cmd ->
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", player.getName()))
            );
        }
    }

    // ===================== VENDER TODO =====================
    public void sellAll(Player player) {
        if (!active) {
            MessageUtil.lang(player, "shop.inactive");
            return;
        }

        if (sellPrice <= 0) {
            MessageUtil.lang(player, "shop.sell-disabled");
            return;
        }

        ItemStack required = item.getRealItem();
        int playerAmount = 0;

        // Contar cuántos ítems tiene el jugador
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.isSimilar(required)) {
                playerAmount += stack.getAmount();
            }
        }

        if (playerAmount <= 0) {
            MessageUtil.lang(player, "shop.not-enough-items");
            return;
        }

        // Crear ItemStack a remover
        ItemStack toRemove = required.clone();
        toRemove.setAmount(playerAmount);
        player.getInventory().removeItem(toRemove);

        double total = playerAmount * sellPrice;
        QSMarketPlus.economy.depositPlayer(player, total);

        MessageUtil.lang(player, "shop.sell-success",
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed("amount", String.valueOf(playerAmount)),
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("item", item.getName()),
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed("price", String.valueOf(total))
        );

        item.executeSellCommands(player);
    }

}
