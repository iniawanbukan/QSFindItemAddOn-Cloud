/**
 * QSFindItemAddOn: An Minecraft add-on plugin for the QuickShop Hikari
 * and Reremake Shop plugins for Spigot server platform.
 * Copyright (C) 2021  myzticbean
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.myzticbean.finditemaddon.models.enums;

import io.myzticbean.finditemaddon.models.FoundShopItemModel;
import org.bukkit.Material;
import org.bukkit.Location;

import java.util.Comparator;

/**
 * Ordering options offered by the /qss shop browser. Left-clicking the sort
 * button advances to the next mode, right-clicking goes to the previous one.
 *
 * @author myzticbean
 */
public enum QssSortMode {

    BEST_PRICE("Best Price", Material.EMERALD, null),
    PRICE_LOWEST("Price: Lowest", Material.IRON_INGOT,
            Comparator.comparingDouble(FoundShopItemModel::getShopPrice)),
    PRICE_HIGHEST("Price: Highest", Material.GOLD_INGOT,
            Comparator.comparingDouble(FoundShopItemModel::getShopPrice).reversed()),
    STOCK_HIGHEST("Stock: Highest", Material.CHEST,
            Comparator.comparingLong(QssSortMode::rankStock).reversed()),
    RECENTLY_LISTED("Recently Listed", Material.CLOCK,
            Comparator.comparingLong(FoundShopItemModel::getShopId).reversed());

    private final String displayName;
    private final Material icon;
    private final Comparator<FoundShopItemModel> comparator;

    QssSortMode(String displayName, Material icon, Comparator<FoundShopItemModel> comparator) {
        this.displayName = displayName;
        this.icon = icon;
        this.comparator = comparator;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }

    /**
     * Returns the comparator for the current shop side. "Best Price" flips
     * direction: TO_BUY shows shops that sell items to the player, so the
     * lowest price wins; TO_SELL shows shops that buy from the player, so the
     * highest payout wins.
     */
    public Comparator<FoundShopItemModel> getComparator(QssSide side) {
        Comparator<FoundShopItemModel> base = comparator != null ? comparator : bestPriceComparator(side);
        // Keep the list stable for shops that share a price, stock or id.
        return base.thenComparingLong(FoundShopItemModel::getShopId)
                .thenComparing(QssSortMode::locationKey);
    }

    private static Comparator<FoundShopItemModel> bestPriceComparator(QssSide side) {
        if (side == QssSide.TO_SELL) {
            return Comparator.comparingDouble(FoundShopItemModel::getShopPrice).reversed();
        }
        return Comparator.comparingDouble(FoundShopItemModel::getShopPrice);
    }

    /**
     * Converts a shop's remaining stock/space into a value that can be sorted
     * from highest to lowest. Unlimited shops always rank above finite ones and
     * unknown values ({@code -2}) rank last.
     */
    private static long rankStock(FoundShopItemModel shop) {
        int remaining = shop.getRemainingStockOrSpace();
        if (remaining == -1 || remaining == Integer.MAX_VALUE) {
            return Long.MAX_VALUE / 2;
        }
        if (remaining < 0) {
            return 0L;
        }
        return remaining;
    }

    private static String locationKey(FoundShopItemModel shop) {
        Location location = shop.getShopLocation();
        if (location == null || location.getWorld() == null) {
            return "";
        }
        return location.getWorld().getName()
                + ':' + location.getBlockX()
                + ':' + location.getBlockY()
                + ':' + location.getBlockZ();
    }

    public QssSortMode next() {
        QssSortMode[] modes = values();
        return modes[(ordinal() + 1) % modes.length];
    }

    public QssSortMode previous() {
        QssSortMode[] modes = values();
        return modes[(ordinal() - 1 + modes.length) % modes.length];
    }
}
