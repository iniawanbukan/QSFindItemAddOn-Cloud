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
import io.myzticbean.finditemaddon.utils.PlayerUtil;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Opens a temporary sign editor for the /qss search input. The sign is placed
 * in an air block near the player and restored as soon as the player submits
 * the text, disconnects, or after a short timeout.
 *
 * @author myzticbean
 */
public final class QssSignInput {

    private static final Map<UUID, PendingSign> PENDING_SIGNS = new ConcurrentHashMap<>();
    private static final int[] SIGN_Y_OFFSETS = {3, 2, 4, 1, 5, 0, 6};
    private static final long RESTORE_AFTER_TICKS = 20L * 60;

    private QssSignInput() {
    }

    public static void open(Player player) {
        FindItemAddOn.getScheduler().runAtEntity(player, ignored -> openOnRegionThread(player));
    }

    private static void openOnRegionThread(Player player) {
        restore(player.getUniqueId());

        Location signLocation = findSignLocation(player);
        if (signLocation == null) {
            PlayerUtil.sendMessage(player, getPluginPrefix()
                    + "&cCould not find a free block to open the search sign. Please move to an open area and try again.");
            return;
        }

        Block block = signLocation.getBlock();
        BlockData originalData = block.getBlockData().clone();
        block.setBlockData(Material.OAK_SIGN.createBlockData(), false);

        if (!(block.getState() instanceof Sign sign)) {
            block.setBlockData(originalData, false);
            PlayerUtil.sendMessage(player, getPluginPrefix()
                    + "&cCould not open the search sign. Please try again.");
            return;
        }

        for (int line = 0; line < 4; line++) {
            sign.setLine(line, "");
        }
        sign.update(true, false);

        PENDING_SIGNS.put(player.getUniqueId(), new PendingSign(signLocation.clone(), originalData));
        player.closeInventory();
        player.openSign(sign, Side.FRONT);

        FindItemAddOn.getScheduler().runAtLocationLater(signLocation,
                () -> restore(player.getUniqueId()), RESTORE_AFTER_TICKS);

        PlayerUtil.sendMessage(player, getPluginPrefix()
                + "&7Type your search on the sign. Leave it blank to show every shop.");
    }

    /**
     * Reads and consumes the pending sign input for a player.
     *
     * @return an empty optional when the edited sign is not a QSS sign, or the
     *         entered query (possibly empty) when it is.
     */
    public static Optional<String> consumeSearch(Player player, Location location, String[] lines) {
        PendingSign pendingSign = PENDING_SIGNS.get(player.getUniqueId());
        if (pendingSign == null || !pendingSign.matches(location)) {
            return Optional.empty();
        }

        PENDING_SIGNS.remove(player.getUniqueId());
        FindItemAddOn.getScheduler().runAtLocation(pendingSign.location, ignored -> pendingSign.restore());

        String query = Arrays.stream(lines)
                .filter(line -> line != null && !line.isBlank())
                .map(line -> ChatColor.stripColor(line).trim())
                .filter(line -> !line.isEmpty())
                .collect(Collectors.joining(" "));
        return Optional.of(query);
    }

    public static void restore(UUID playerId) {
        PendingSign pendingSign = PENDING_SIGNS.remove(playerId);
        if (pendingSign != null) {
            FindItemAddOn.getScheduler().runAtLocation(pendingSign.location, ignored -> pendingSign.restore());
        }
    }

    private static Location findSignLocation(Player player) {
        Location base = player.getLocation();
        for (int offset : SIGN_Y_OFFSETS) {
            Location location = base.clone().add(0, offset, 0);
            Block block = location.getBlock();
            if (block.isEmpty() || block.isReplaceable()) {
                return location;
            }
        }
        return null;
    }

    private static String getPluginPrefix() {
        return FindItemAddOn.getConfigProvider().PLUGIN_PREFIX;
    }

    private record PendingSign(Location location, BlockData originalData) {

        private boolean matches(Location other) {
            return other != null
                    && other.getWorld() != null
                    && other.getWorld().equals(location.getWorld())
                    && other.getBlockX() == location.getBlockX()
                    && other.getBlockY() == location.getBlockY()
                    && other.getBlockZ() == location.getBlockZ();
        }

        private void restore() {
            location.getBlock().setBlockData(originalData.clone(), false);
        }
    }
}
