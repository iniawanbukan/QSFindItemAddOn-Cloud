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
package io.myzticbean.finditemaddon.listeners;

import io.myzticbean.finditemaddon.handlers.gui.qss.QssSession;
import io.myzticbean.finditemaddon.handlers.gui.qss.QssSessionManager;
import io.myzticbean.finditemaddon.handlers.gui.qss.QssSignInput;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Optional;

/**
 * Captures the search text entered in the temporary /qss sign.
 *
 * @author myzticbean
 */
public class QssSignListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        Optional<String> query = QssSignInput.consumeSearch(player, event.getBlock().getLocation(), event.getLines());
        if (query.isEmpty()) {
            return;
        }

        event.setCancelled(true);

        QssSession session = QssSessionManager.get(player);
        if (session == null) {
            return;
        }

        session.setSearchQuery(query.get());
        QssSessionManager.open(player, session);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        QssSignInput.restore(player.getUniqueId());
        QssSessionManager.remove(player);
    }
}
