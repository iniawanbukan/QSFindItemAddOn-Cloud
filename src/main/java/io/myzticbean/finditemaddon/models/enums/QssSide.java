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

import org.bukkit.Material;

/**
 * Which side of the shops the /qss browser shows. The names match the
 * /finditem subcommands, so {@code TO_BUY} shows exactly the same shops as
 * {@code /finditem TO_BUY ...} and {@code TO_SELL} matches
 * {@code /finditem TO_SELL ...}.
 *
 * @author myzticbean
 */
public enum QssSide {

    TO_SELL("TO_SELL", Material.EMERALD, false),
    TO_BUY("TO_BUY", Material.GOLD_INGOT, true);

    private final String displayName;
    private final Material icon;
    private final boolean apiToBuy;

    QssSide(String displayName, Material icon, boolean apiToBuy) {
        this.displayName = displayName;
        this.icon = icon;
        this.apiToBuy = apiToBuy;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }

    /**
     * Value passed to {@code QSApi#fetchAllItemsFromAllShops}. It uses the
     * same mapping as the /finditem subcommand, keeping both entry points
     * consistent.
     */
    public boolean isApiToBuy() {
        return apiToBuy;
    }

    public String getDescription() {
        return "Same shops as /finditem " + displayName;
    }

    public QssSide next() {
        QssSide[] sides = values();
        return sides[(ordinal() + 1) % sides.length];
    }

    public QssSide previous() {
        QssSide[] sides = values();
        return sides[(ordinal() - 1 + sides.length) % sides.length];
    }
}
