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
package io.myzticbean.finditemaddon.handlers.gui.qss;

import io.myzticbean.finditemaddon.models.FoundShopItemModel;
import io.myzticbean.finditemaddon.models.enums.QssCategory;
import io.myzticbean.finditemaddon.models.enums.QssSide;
import io.myzticbean.finditemaddon.models.enums.QssSortMode;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player state for the /qss shop browser. It keeps the raw QuickShop
 * results for each side, so the GUI can be rebuilt for every filter change
 * without running the shop search again.
 * <p>
 * One {@link FoundShopItemModel} is kept per shop. Two shops that sell the same
 * item (even from the same owner) therefore stay as two separate entries and
 * can each be clicked to teleport to their own chest.
 *
 * @author myzticbean
 */
public class QssSession {

    @Getter
    private final UUID owner;
    private final Map<QssSide, List<FoundShopItemModel>> shopsBySide = new EnumMap<>(QssSide.class);
    @Getter
    private QssSide side = QssSide.TO_BUY;
    @Getter
    private QssSortMode sortMode = QssSortMode.BEST_PRICE;
    @Getter
    private QssCategory category = QssCategory.ALL;
    @Getter
    private String searchQuery = "";
    @Getter
    private Material materialFilter;
    @Getter
    @Setter
    private int page = 0;

    public QssSession(UUID owner) {
        this.owner = owner;
    }

    public boolean hasShops(QssSide side) {
        return shopsBySide.containsKey(side);
    }

    public void setShops(QssSide side, List<FoundShopItemModel> shops) {
        this.shopsBySide.put(side, List.copyOf(shops));
        this.page = 0;
    }

    public void setSide(QssSide side) {
        if (side == null || side == this.side) {
            return;
        }
        this.side = side;
        this.page = 0;
    }

    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery == null ? "" : searchQuery.trim();
        this.materialFilter = null;
        this.page = 0;
    }

    public void setMaterialFilter(Material materialFilter) {
        this.materialFilter = materialFilter;
        this.searchQuery = "";
        this.page = 0;
    }

    public void clearSearch() {
        this.searchQuery = "";
        this.materialFilter = null;
        this.page = 0;
    }

    public void cycleSort(boolean forward) {
        this.sortMode = forward ? sortMode.next() : sortMode.previous();
        this.page = 0;
    }

    public void cycleCategory(boolean forward) {
        this.category = forward ? category.next() : category.previous();
        this.page = 0;
    }

    public void cycleSide(boolean forward) {
        setSide(forward ? side.next() : side.previous());
    }

    /**
     * Returns every shop matching the current side, category, search and sort.
     * Duplicate items are intentionally not merged: each shop is its own entry.
     */
    public List<FoundShopItemModel> getVisibleShops() {
        String query = searchQuery.toLowerCase(Locale.ROOT);
        return shopsBySide.getOrDefault(side, List.of()).stream()
                .filter(shop -> shop != null && shop.getItem() != null)
                .filter(shop -> !shop.getItem().getType().isAir())
                .filter(shop -> materialFilter == null || shop.getItem().getType() == materialFilter)
                .filter(shop -> category.matches(shop.getItem().getType()))
                .filter(shop -> query.isEmpty() || matchesSearch(shop, query))
                .sorted(sortMode.getComparator(side))
                .toList();
    }

    private static boolean matchesSearch(FoundShopItemModel shop, String query) {
        String materialName = shop.getItem().getType().name().toLowerCase(Locale.ROOT).replace('_', ' ');
        if (materialName.contains(query)) {
            return true;
        }
        ItemStack item = shop.getItem();
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            String displayName = ChatColor.stripColor(meta.getDisplayName()).toLowerCase(Locale.ROOT);
            return displayName.contains(query);
        }
        return false;
    }
}
