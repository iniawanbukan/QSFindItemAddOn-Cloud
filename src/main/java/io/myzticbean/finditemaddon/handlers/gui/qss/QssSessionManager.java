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

import io.myzticbean.finditemaddon.FindItemAddOn;
import io.myzticbean.finditemaddon.handlers.gui.PlayerMenuUtility;
import io.myzticbean.finditemaddon.models.enums.QssSide;
import io.myzticbean.finditemaddon.quickshop.QSApi;
import io.myzticbean.finditemaddon.utils.PlayerUtil;
import io.myzticbean.finditemaddon.utils.log.Logger;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads and keeps the /qss sessions for online players. Both /qss and the
 * /finditem command use this class, so every search result ends up in the same
 * shop browser.
 *
 * @author myzticbean
 */
public final class QssSessionManager {

    private static final Map<UUID, QssSession> SESSIONS = new ConcurrentHashMap<>();
    private static final String SEARCH_ERROR_MSG = "&cAn error occurred while loading shops. Please try again or contact an admin.";

    private QssSessionManager() {
    }

    public static QssSession get(Player player) {
        return SESSIONS.get(player.getUniqueId());
    }

    public static void remove(Player player) {
        SESSIONS.remove(player.getUniqueId());
    }

    /**
     * Opens the shop browser with its default filters.
     */
    public static void openDefault(Player player) {
        QssSession session = new QssSession(player.getUniqueId());
        SESSIONS.put(player.getUniqueId(), session);
        loadSide(player, session);
    }

    /**
     * Opens the shop browser from a /finditem-style search. A valid material is
     * filtered exactly; anything else is used as a free-text query.
     */
    public static void openFromFindItem(Player player, boolean toBuy, String itemArg) {
        QssSession session = new QssSession(player.getUniqueId());
        session.setSide(toBuy ? QssSide.TO_BUY : QssSide.TO_SELL);

        if (itemArg != null && !"*".equals(itemArg)) {
            Material material = Material.getMaterial(itemArg.toUpperCase(Locale.ROOT));
            if (material != null && material.isItem()) {
                session.setMaterialFilter(material);
            } else {
                session.setSearchQuery(itemArg);
            }
        }

        SESSIONS.put(player.getUniqueId(), session);
        loadSide(player, session);
    }

    /**
     * Opens the browser, loading the selected side first if it has not been
     * fetched yet (for example the first time a player switches sides).
     */
    public static void ensureLoadedAndOpen(Player player, QssSession session) {
        if (session == null) {
            return;
        }
        if (session.hasShops(session.getSide())) {
            open(player, session);
            return;
        }
        loadSide(player, session);
    }

    public static void open(Player player, QssSession session) {
        PlayerMenuUtility playerMenuUtility = FindItemAddOn.getPlayerMenuUtility(player);
        new Menu(playerMenuUtility, session).open();
    }

    private static void loadSide(Player player, QssSession session) {
        QSApi<?, ?> qsApi = FindItemAddOn.getQsApiInstance();
        if (qsApi == null) {
            PlayerUtil.sendMessage(player, getPluginPrefix() + "&cQuickShop is not ready yet. Please try again in a moment.");
            return;
        }

        if (!StringUtils.isEmpty(FindItemAddOn.getConfigProvider().SHOP_SEARCH_LOADING_MSG)) {
            PlayerUtil.sendMessage(player, getPluginPrefix() + FindItemAddOn.getConfigProvider().SHOP_SEARCH_LOADING_MSG);
        }

        QssSide side = session.getSide();
        qsApi.fetchAllItemsFromAllShops(side.isApiToBuy(), player)
                .thenAccept(results -> {
                    if (!player.isOnline() || SESSIONS.get(player.getUniqueId()) != session) {
                        SESSIONS.remove(player.getUniqueId(), session);
                        return;
                    }
                    // Keep one entry per shop; do not group identical items.
                    session.setShops(side, results);
                    if (session.getSide() == side || session.hasShops(session.getSide())) {
                        open(player, session);
                    } else {
                        loadSide(player, session);
                    }
                })
                .exceptionally(ex -> {
                    Throwable cause = ex;
                    while (cause instanceof CompletionException && cause.getCause() != null) {
                        cause = cause.getCause();
                    }
                    Logger.logError("QSS shop loading failed for player " + player.getName() + ": " + cause.getMessage());
                    PlayerUtil.sendMessage(player, getPluginPrefix() + SEARCH_ERROR_MSG);
                    return null;
                });
    }

    private static String getPluginPrefix() {
        return FindItemAddOn.getConfigProvider().PLUGIN_PREFIX;
    }
}
