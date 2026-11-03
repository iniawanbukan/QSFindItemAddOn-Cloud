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
package io.myzticbean.finditemaddon.models;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Model for the Shop Item lore that will be shown in the search GUI
 * @author myzticbean
 */
@Getter
public class FoundShopItemModel {
    private final double shopPrice;
    private final int remainingStockOrSpace;
    private final UUID shopOwner;
    private final Location shopLocation;
    private final ItemStack item;
    private final boolean toBuy;
    /**
     * QuickShop's internal shop id. Higher ids are created later, which lets the
     * QSS item browser offer a "recently listed" ordering. {@code 0} means the
     * id is unknown (for example when a shop implementation cannot expose it).
     */
    private final long shopId;

    public FoundShopItemModel(double shopPrice, int remainingStockOrSpace, UUID shopOwner,
                              Location shopLocation, ItemStack item, boolean toBuy) {
        this(shopPrice, remainingStockOrSpace, shopOwner, shopLocation, item, toBuy, 0L);
    }

    public FoundShopItemModel(double shopPrice, int remainingStockOrSpace, UUID shopOwner,
                              Location shopLocation, ItemStack item, boolean toBuy, long shopId) {
        this.shopPrice = shopPrice;
        this.remainingStockOrSpace = remainingStockOrSpace;
        this.shopOwner = shopOwner;
        this.shopLocation = shopLocation;
        this.item = item;
        this.toBuy = toBuy;
        this.shopId = shopId;
    }
}
