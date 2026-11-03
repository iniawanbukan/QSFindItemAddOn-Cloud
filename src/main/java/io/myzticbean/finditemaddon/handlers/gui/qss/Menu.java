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

import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.olziedev.playerwarps.api.warp.Warp;
import io.myzticbean.finditemaddon.FindItemAddOn;
import io.myzticbean.finditemaddon.config.ConfigProvider;
import io.myzticbean.finditemaddon.dependencies.EssentialsXPlugin;
import io.myzticbean.finditemaddon.dependencies.PlayerWarpsPlugin;
import io.myzticbean.finditemaddon.dependencies.ResidencePlugin;
import io.myzticbean.finditemaddon.dependencies.WGPlugin;
import io.myzticbean.finditemaddon.handlers.gui.PaginatedMenu;
import io.myzticbean.finditemaddon.handlers.gui.PlayerMenuUtility;
import io.myzticbean.finditemaddon.models.FoundShopItemModel;
import io.myzticbean.finditemaddon.models.enums.CustomCmdPlaceholdersEnum;
import io.myzticbean.finditemaddon.models.enums.PlayerPermsEnum;
import io.myzticbean.finditemaddon.models.enums.QssCategory;
import io.myzticbean.finditemaddon.models.enums.QssSide;
import io.myzticbean.finditemaddon.models.enums.QssSortMode;
import io.myzticbean.finditemaddon.models.enums.ShopLorePlaceholdersEnum;
import io.myzticbean.finditemaddon.utils.PlayerUtil;
import io.myzticbean.finditemaddon.utils.async.VirtualThreadScheduler;
import io.myzticbean.finditemaddon.utils.json.ShopSearchActivityStorageUtil;
import io.myzticbean.finditemaddon.utils.LocationUtils;
import io.myzticbean.finditemaddon.utils.log.Logger;
import io.myzticbean.finditemaddon.utils.warp.EssentialWarpsUtil;
import io.myzticbean.finditemaddon.utils.warp.PlayerWarpsUtil;
import io.myzticbean.finditemaddon.utils.warp.ResidenceUtils;
import io.myzticbean.finditemaddon.utils.warp.WGRegionUtils;
import me.kodysimpson.simpapi.colors.ColorTranslator;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Main GUI for the /qss command and /finditem searches. Every entry is one
 * QuickShop shop, so two shops selling the same item (even from the same owner)
 * are listed separately. Left-clicking an entry teleports the player directly
 * to that shop.
 * <p>
 * The top row holds the sort, side, category and search controls; the middle
 * rows hold the shops; the bottom row holds previous/next/close.
 *
 * @author myzticbean
 */
public class Menu extends PaginatedMenu {

    public static final String SHOP_STOCK_UNLIMITED = "Unlimited";
    public static final String SHOP_STOCK_UNKNOWN = "Unknown";

    private static final String NAMEDSPACE_KEY_LOCATION_DATA = "locationData";
    private static final int SHOPS_PER_PAGE = 36;
    private static final int SHOP_SLOT_START = 9;
    private static final int SHOP_SLOT_END = 44;

    private static final int SLOT_SORT = 0;
    private static final int SLOT_SIDE = 2;
    private static final int SLOT_CATEGORY = 4;
    private static final int SLOT_SEARCH = 8;

    private static final int SLOT_PREVIOUS = 45;
    private static final int SLOT_PAGE_INFO = 48;
    private static final int SLOT_CLOSE = 49;
    private static final int SLOT_NEXT = 53;

    private final ConfigProvider configProvider;
    private final QssSession session;

    public Menu(PlayerMenuUtility playerMenuUtility, QssSession session) {
        super(playerMenuUtility);
        this.configProvider = FindItemAddOn.getConfigProvider();
        this.session = session;
    }

    public void open() {
        super.open(Collections.emptyList());
    }

    @Override
    public String getMenuName() {
        String title = StringUtils.isEmpty(configProvider.SHOP_SEARCH_GUI_TITLE)
                ? "&l» &rQuickShop Shops"
                : configProvider.SHOP_SEARCH_GUI_TITLE;
        return ColorTranslator.translateColorCodes(title + " &7(" + session.getSide().getDisplayName() + ")");
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void setMenuItems(List<FoundShopItemModel> ignored) {
        List<FoundShopItemModel> visibleShops = session.getVisibleShops();
        int pageCount = getPageCount(visibleShops.size());
        int page = Math.max(0, Math.min(session.getPage(), pageCount - 1));
        session.setPage(page);

        addTopBar(visibleShops.size());
        addShopEntries(visibleShops, page);
        addBottomBar(visibleShops.size(), page, pageCount);
    }

    private void addTopBar(int visibleShopCount) {
        for (int slot = 0; slot < 9; slot++) {
            inventory.setItem(slot, GUI_FILLER_ITEM);
        }
        inventory.setItem(SLOT_SORT, buildSortItem());
        inventory.setItem(SLOT_SIDE, buildSideItem());
        inventory.setItem(SLOT_CATEGORY, buildCategoryItem());
        inventory.setItem(SLOT_SEARCH, buildSearchItem(visibleShopCount));
    }

    private void addShopEntries(List<FoundShopItemModel> visibleShops, int page) {
        int startIndex = page * SHOPS_PER_PAGE;
        for (int i = 0; i < SHOPS_PER_PAGE; i++) {
            int index = startIndex + i;
            if (index >= visibleShops.size()) {
                break;
            }

            FoundShopItemModel foundShop = visibleShops.get(index);
            if (foundShop == null) {
                continue;
            }

            // Use setItem so identical shop items never merge into one stack.
            // Every shop stays visible in its own slot with its own location.
            inventory.setItem(SHOP_SLOT_START + i, createShopItem(foundShop));
        }
    }

    private void addBottomBar(int visibleShopCount, int page, int pageCount) {
        for (int slot = 45; slot < 54; slot++) {
            inventory.setItem(slot, GUI_FILLER_ITEM);
        }

        inventory.setItem(SLOT_PREVIOUS, buildItem(Material.ARROW, "&c« Previous Page",
                List.of("&7Go to the previous page")));
        inventory.setItem(SLOT_PAGE_INFO, buildPageInfo(visibleShopCount, page, pageCount));
        inventory.setItem(SLOT_CLOSE, buildItem(Material.BARRIER, "&fClose",
                List.of("&7Close the shop browser")));
        inventory.setItem(SLOT_NEXT, buildItem(Material.ARROW, "&aNext Page »",
                List.of("&7Go to the next page")));
    }

    private ItemStack buildSortItem() {
        QssSortMode sortMode = session.getSortMode();
        List<String> lore = new ArrayList<>();
        lore.add("&7Current: &f" + sortMode.getDisplayName());
        if (sortMode == QssSortMode.BEST_PRICE) {
            lore.add(session.getSide() == QssSide.TO_SELL
                    ? "&7Highest price first &8(shops pay more)"
                    : "&7Lowest price first &8(you pay less)");
        }
        lore.add("");
        lore.add("&aLeft-click &7- next sort");
        lore.add("&cRight-click &7- previous sort");
        return buildItem(sortMode.getIcon(), "&6&lSort: &e" + sortMode.getDisplayName(), lore);
    }

    private ItemStack buildSideItem() {
        QssSide side = session.getSide();
        return buildItem(side.getIcon(), "&6&lShops: &e" + side.getDisplayName(),
                List.of(
                        "&7Current: &f" + side.getDisplayName(),
                        "&7" + side.getDescription(),
                        "",
                        "&aLeft-click &7- switch side",
                        "&cRight-click &7- switch side"
                ));
    }

    private ItemStack buildCategoryItem() {
        QssCategory category = session.getCategory();
        return buildItem(category.getIcon(), "&6&lCategory: &e" + category.getDisplayName(),
                List.of(
                        "&7Current: &f" + category.getDisplayName(),
                        "",
                        "&aLeft-click &7- next category",
                        "&cRight-click &7- previous category"
                ));
    }

    private ItemStack buildSearchItem(int visibleShopCount) {
        String query;
        if (session.getMaterialFilter() != null) {
            query = prettify(session.getMaterialFilter()) + " &7(exact item)";
        } else {
            query = StringUtils.isEmpty(session.getSearchQuery()) ? "None" : session.getSearchQuery();
        }
        return buildItem(Material.OAK_SIGN, "&6&lSearch",
                List.of(
                        "&7Current: &f" + query,
                        "&7Matching shops: &f" + visibleShopCount,
                        "",
                        "&aLeft-click &7- enter search on a sign",
                        "&cRight-click &7- clear search"
                ));
    }

    private ItemStack buildPageInfo(int visibleShopCount, int page, int pageCount) {
        String query;
        if (session.getMaterialFilter() != null) {
            query = prettify(session.getMaterialFilter()) + " (exact)";
        } else {
            query = StringUtils.isEmpty(session.getSearchQuery()) ? "None" : session.getSearchQuery();
        }
        return buildItem(Material.PAPER, "&ePage &f" + (page + 1) + " &7/ &f" + pageCount,
                List.of(
                        "&7Shops: &f" + visibleShopCount,
                        "&7Side: &f" + session.getSide().getDisplayName(),
                        "&7Sort: &f" + session.getSortMode().getDisplayName(),
                        "&7Category: &f" + session.getCategory().getDisplayName(),
                        "&7Search: &f" + query
                ));
    }

    @Override
    public void handleMenu(@NotNull InventoryClickEvent event) {
        int slot = event.getSlot();
        Player player = (Player) event.getWhoClicked();
        boolean leftClick = event.isLeftClick();
        boolean rightClick = event.isRightClick();

        if (slot == SLOT_SORT) {
            if (leftClick || rightClick) {
                session.cycleSort(leftClick);
                open();
            }
            return;
        }

        if (slot == SLOT_SIDE) {
            if (leftClick || rightClick) {
                session.cycleSide(leftClick);
                QssSessionManager.ensureLoadedAndOpen(player, session);
            }
            return;
        }

        if (slot == SLOT_CATEGORY) {
            if (leftClick || rightClick) {
                session.cycleCategory(leftClick);
                open();
            }
            return;
        }

        if (slot == SLOT_SEARCH) {
            if (leftClick) {
                QssSignInput.open(player);
            } else if (rightClick) {
                session.clearSearch();
                open();
            }
            return;
        }

        if (slot == SLOT_PREVIOUS) {
            if (session.getPage() > 0) {
                session.setPage(session.getPage() - 1);
                open();
            } else {
                sendFirstPageAlert(player);
            }
            return;
        }

        if (slot == SLOT_NEXT) {
            if (session.getPage() + 1 < getPageCount(session.getVisibleShops().size())) {
                session.setPage(session.getPage() + 1);
                open();
            } else {
                sendLastPageAlert(player);
            }
            return;
        }

        if (slot == SLOT_CLOSE) {
            player.closeInventory();
            return;
        }

        if (slot >= SHOP_SLOT_START && slot <= SHOP_SLOT_END) {
            ItemStack current = event.getCurrentItem();
            if (current == null || current.getType().isAir()) {
                return;
            }
            handleShopItemClick(event, player);
        }
    }

    /**
     * Handles clicks on shop items in the inventory.
     *
     * @param event  The InventoryClickEvent
     * @param player The player who clicked
     */
    private void handleShopItemClick(@NotNull InventoryClickEvent event, Player player) {
        ItemStack item = event.getCurrentItem();
        if (item == null) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        NamespacedKey key = new NamespacedKey(FindItemAddOn.getInstance(), NAMEDSPACE_KEY_LOCATION_DATA);

        // Check if the item has the required location data
        if (!meta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
            Logger.logError("PersistentDataContainer doesn't have the right kind of data!");
            return;
        }

        String locData = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        List<String> locDataList = Arrays.asList(locData.split("\\s*\\|\\|\\|\\s*"));

        // Parse location once - reused by TP and custom commands
        Location shopLocation = locDataList.size() >= 4 ? parseShopLocation(locDataList) : null;

        // Handle direct teleportation to shop
        if (configProvider.TP_PLAYER_DIRECTLY_TO_SHOP && shopLocation != null) {
            handleDirectShopTeleport(player, shopLocation);
        }
        // Handle teleportation to nearest warp
        else if (configProvider.TP_PLAYER_TO_NEAREST_WARP && locDataList.size() == 1
                && !locDataList.get(0).isEmpty()) {
            handleWarpTeleport(player, locDataList.get(0));
        }

        // Execute custom commands if enabled
        handleCustomCommands(player, shopLocation);
        player.closeInventory();
    }

    /**
     * Handles direct teleportation to a shop
     *
     * @param player       The player to teleport
     * @param shopLocation The parsed shop location
     */
    private void handleDirectShopTeleport(@NotNull Player player, @NotNull Location shopLocation) {
        VirtualThreadScheduler.runTaskAsync(() -> {
            // Check if player has permission to teleport
            if (!PlayerUtil.hasPermission(player, PlayerPermsEnum.FINDITEM_SHOPTP.value())) {
                sendNoPermissionMessage(player);
                return;
            }

            // Check if player is teleporting to their own shop
            UUID shopOwner = ShopSearchActivityStorageUtil.getShopOwnerUUID(shopLocation);
            if (player.getUniqueId().equals(shopOwner) && !PlayerPermsEnum.canPlayerTpToOwnShop(player)) {
                PlayerUtil.sendMessage(player, configProvider.PLUGIN_PREFIX + configProvider.SHOP_TP_NO_PERMISSION_MSG);
                return;
            }

            // Find a safe location around the shop
            LocationUtils.findSafeLocationAroundShop(shopLocation, player).thenAccept(locToTeleport -> {
                if (locToTeleport == null) {
                    sendUnsafeAreaMessage(player);
                    return;
                }

                // Record the visit and set last location for Essentials
                ShopSearchActivityStorageUtil.addPlayerVisitEntryAsync(shopLocation, player);
                if (EssentialsXPlugin.isEnabled())
                    EssentialsXPlugin.setLastLocation(player);

                // Apply the teleport countdown if necessary, otherwise teleport immediately
                if (shouldApplyTeleportCountdown(player)) {
                    applyTeleportCountdown(player, locToTeleport);
                } else {
                    PlayerUtil.teleport(player, locToTeleport);
                }
            });
        });
    }

    /**
     * Handles teleportation to the nearest warp
     *
     * @param player   The player to teleport
     * @param warpName The name of the warp
     */
    private void handleWarpTeleport(Player player, String warpName) {
        switch (configProvider.NEAREST_WARP_MODE) {
            case 1:
                EssentialWarpsUtil.warpPlayer(player, warpName);
                break;
            case 2:
                PlayerWarpsUtil.executeWarpPlayer(player, warpName);
                break;
            case 4:
                ResidenceUtils.residenceTp(player, warpName);
                break;
        }
    }

    /**
     * Executes custom commands if enabled
     *
     * @param player       The player who triggered the commands
     * @param shopLocation The parsed shop location, or null if unavailable
     */
    private void handleCustomCommands(Player player, @Nullable Location shopLocation) {
        if (configProvider.CUSTOM_CMDS_RUN_ENABLED && !configProvider.CUSTOM_CMDS_LIST.isEmpty()
                && shopLocation != null) {
            for (String cmd : configProvider.CUSTOM_CMDS_LIST) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), replaceCustomCmdPlaceholders(cmd, player, shopLocation));
            }
        }
    }

    /**
     * Parses location data into a Bukkit Location
     *
     * @param locDataList List containing location data
     * @return A Bukkit Location, or null if parsing fails
     */
    private @Nullable Location parseShopLocation(@NotNull List<String> locDataList) {
        if (locDataList.size() < 4) {
            Logger.logError("Malformed shop location data (expected 4 parts, got " + locDataList.size() + "): " + locDataList);
            return null;
        }
        World world = Bukkit.getWorld(locDataList.get(0));
        if (world == null) {
            Logger.logError("World not found for shop location: " + locDataList.get(0));
            return null;
        }
        try {
            int locX = Integer.parseInt(locDataList.get(1));
            int locY = Integer.parseInt(locDataList.get(2));
            int locZ = Integer.parseInt(locDataList.get(3));
            return new Location(world, locX, locY, locZ);
        } catch (NumberFormatException e) {
            Logger.logError("Invalid coordinates in shop location data: " + locDataList);
            return null;
        }
    }

    /**
     * Sends a no permission message to the player
     *
     * @param player The player to send the message to
     */
    private void sendNoPermissionMessage(Player player) {
        if (!StringUtils.isEmpty(configProvider.SHOP_TP_NO_PERMISSION_MSG)) {
            PlayerUtil.sendMessage(player, configProvider.PLUGIN_PREFIX + configProvider.SHOP_TP_NO_PERMISSION_MSG);
        }
    }

    /**
     * Sends an unsafe area message to the player
     *
     * @param player The player to send the message to
     */
    private void sendUnsafeAreaMessage(Player player) {
        if (!StringUtils.isEmpty(configProvider.UNSAFE_SHOP_AREA_MSG)) {
            PlayerUtil.sendMessage(player, configProvider.PLUGIN_PREFIX + configProvider.UNSAFE_SHOP_AREA_MSG);
        }
    }

    /**
     * Checks if a teleport countdown should be applied. Players with
     * {@code finditem.shoptp-delay.bypass} (or {@code finditem.admin}) teleport
     * immediately.
     *
     * @param player The player to check
     * @return true if a countdown should be applied, false otherwise
     */
    private boolean shouldApplyTeleportCountdown(Player player) {
        return configProvider.TP_DELAY_IN_SECONDS > 0
                && !PlayerPermsEnum.hasShopTpDelayBypassPermOrAdmin(player);
    }

    /**
     * Starts the teleport countdown, sending one message per remaining second.
     *
     * @param player        The player to apply the countdown to
     * @param locToTeleport The location to teleport to after the countdown
     */
    private void applyTeleportCountdown(Player player, Location locToTeleport) {
        int seconds = configProvider.TP_DELAY_IN_SECONDS;
        Logger.logDebugInfo("Teleport countdown is set to: " + seconds);
        scheduleTeleportCountdown(player, locToTeleport, seconds);
    }

    private void scheduleTeleportCountdown(Player player, Location locToTeleport, int secondsLeft) {
        if (!player.isOnline()) {
            return;
        }
        if (secondsLeft <= 0) {
            PlayerUtil.teleport(player, locToTeleport);
            return;
        }

        String tpDelayMsg = configProvider.TP_DELAY_MESSAGE;
        if (!StringUtils.isEmpty(tpDelayMsg)) {
            PlayerUtil.sendMessage(player, configProvider.PLUGIN_PREFIX
                    + replaceDelayPlaceholder(tpDelayMsg, secondsLeft));
        }

        FindItemAddOn.getScheduler().runLaterAsync(
                () -> scheduleTeleportCountdown(player, locToTeleport, secondsLeft - 1),
                20);
    }

    /**
     * Creates an ItemStack representing a shop
     *
     * @param foundShop The shop to create an item for
     * @return An ItemStack representing the shop
     */
    private @NotNull ItemStack createShopItem(@NotNull FoundShopItemModel foundShop) {
        // Create a new ItemStack based on the shop's item
        ItemStack item = new ItemStack(foundShop.getItem().getType(), foundShop.getItem().getAmount());
        ItemMeta meta = foundShop.getItem().getItemMeta();
        if (meta == null) {
            meta = Bukkit.getItemFactory().getItemMeta(item.getType());
        }
        if (meta == null) {
            return item;
        }

        List<String> lore = new ArrayList<>();
        // Add lore to the item
        addItemLore(lore, foundShop);

        meta.setLore(lore);
        // Set location data in the item's metadata
        setLocationData(meta, foundShop);

        // Preserve custom model data if it exists
        if (foundShop.getItem().getItemMeta() != null && foundShop.getItem().getItemMeta().hasCustomModelData()) {
            meta.setCustomModelData(foundShop.getItem().getItemMeta().getCustomModelData());
        }

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Adds lore to an item representing a shop
     *
     * @param lore      The list to add lore to
     * @param foundShop The shop to create lore for
     */
    private void addItemLore(List<String> lore, FoundShopItemModel foundShop) {
        // Add existing item lore
        ItemMeta shopItemMeta = foundShop.getItem().getItemMeta();
        if (shopItemMeta != null && shopItemMeta.hasLore()) {
            for (String line : shopItemMeta.getLore()) {
                lore.add(ColorTranslator.translateColorCodes(line));
            }
        }

        // Add shop info lore
        for (String loreLine : configProvider.SHOP_GUI_ITEM_LORE) {
            if (loreLine.contains(ShopLorePlaceholdersEnum.NEAREST_WARP.value())) {
                String nearestWarpInfo = getNearestWarpInfo(foundShop);
                lore.add(ColorTranslator.translateColorCodes(
                        loreLine.replace(ShopLorePlaceholdersEnum.NEAREST_WARP.value(), nearestWarpInfo)));
            } else {
                lore.add(ColorTranslator.translateColorCodes(replaceLorePlaceholders(loreLine, foundShop)));
            }
        }

        // Add teleport info if applicable
        if (configProvider.TP_PLAYER_DIRECTLY_TO_SHOP
                && playerMenuUtility.getOwner().hasPermission(PlayerPermsEnum.FINDITEM_SHOPTP.value())) {
            lore.add(ColorTranslator.translateColorCodes(configProvider.CLICK_TO_TELEPORT_MSG));
        }
    }

    /**
     * Finds the nearest warp or region to a shop based on the configuration
     *
     * @param foundShop The shop to find the nearest warp/region for
     * @return A string representing the nearest warp/region, or an error message if
     *         none found
     */
    private String getNearestWarpInfo(FoundShopItemModel foundShop) {
        int nearestWarpMode = configProvider.NEAREST_WARP_MODE;
        switch (nearestWarpMode) {
            case 1:
                // EssentialsX warps
                if (EssentialsXPlugin.isEnabled()) {
                    String nearestEWarp = EssentialWarpsUtil.findNearestWarp(foundShop.getShopLocation());
                    return (nearestEWarp != null && !StringUtils.isEmpty(nearestEWarp)) ? nearestEWarp
                            : configProvider.NO_WARP_NEAR_SHOP_ERROR_MSG;
                }
                break;
            case 2:
                // PlayerWarps
                if (PlayerWarpsPlugin.getIsEnabled()) {
                    Warp nearestPlayerWarp = PlayerWarpsUtil.findNearestWarp(foundShop.getShopLocation(),
                            foundShop.getShopOwner());
                    return (nearestPlayerWarp != null) ? nearestPlayerWarp.getWarpName()
                            : configProvider.NO_WARP_NEAR_SHOP_ERROR_MSG;
                }
                break;
            case 3:
                // WorldGuard regions
                if (WGPlugin.isEnabled()) {
                    String nearestWGRegion = new WGRegionUtils().findNearestWGRegion(foundShop.getShopLocation());
                    return (nearestWGRegion != null && !StringUtils.isEmpty(nearestWGRegion)) ? nearestWGRegion
                            : configProvider.NO_WG_REGION_NEAR_SHOP_ERROR_MSG;
                }
                break;
            case 4:
                // Residence plugin
                if (ResidencePlugin.isEnabled()) {
                    ClaimedResidence nearestResidence = ResidenceUtils
                            .findNearestResidence(foundShop.getShopLocation());
                    return (nearestResidence != null) ? ResidenceUtils.getResidenceName(nearestResidence)
                            : configProvider.NO_RESIDENCE_NEAR_SHOP_ERROR_MSG;
                }
                break;
            default:
                Logger.logDebugInfo("Invalid value in 'nearest-warp-mode' in config.yml!");
        }
        return configProvider.NO_WARP_NEAR_SHOP_ERROR_MSG;
    }

    /**
     * Sets location data in the item's metadata
     *
     * @param meta      The ItemMeta to set the data on
     * @param foundShop The shop to get location data from
     */
    private void setLocationData(ItemMeta meta, FoundShopItemModel foundShop) {
        NamespacedKey key = new NamespacedKey(FindItemAddOn.getInstance(), NAMEDSPACE_KEY_LOCATION_DATA);
        String locData = "";

        if (configProvider.TP_PLAYER_DIRECTLY_TO_SHOP) {
            // Store exact coordinates for direct teleportation
            Location shopLoc = foundShop.getShopLocation();
            locData = String.format("%s|||%d|||%d|||%d", shopLoc.getWorld().getName(), shopLoc.getBlockX(),
                    shopLoc.getBlockY(), shopLoc.getBlockZ());
        } else if (configProvider.TP_PLAYER_TO_NEAREST_WARP) {
            // Store nearest warp info for warp teleportation
            locData = getNearestWarpInfo(foundShop);
        }

        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, locData);
    }

    /**
     * Replaces all the placeholders in the Shop item lore in GUI
     *
     * @param text Line of lore
     * @param shop Shop instance
     * @return Line of lore replaced with placeholder values
     */
    private @NotNull String replaceLorePlaceholders(String text, @NotNull FoundShopItemModel shop) {
        text = text.replace(ShopLorePlaceholdersEnum.ITEM_PRICE.value(), formatNumber(shop.getShopPrice()));

        if (text.contains(ShopLorePlaceholdersEnum.SHOP_STOCK.value())) {
            int stock = shop.getRemainingStockOrSpace();
            String stockText;
            if (stock == -2) {
                // if -2 (cache doesn't have value) -> try to fetch from MAIN thread
                int stockOrSpace = processUnknownStockSpace(shop);
                stockText = (stockOrSpace == -2) ? SHOP_STOCK_UNKNOWN
                        : (stockOrSpace == -1 ? SHOP_STOCK_UNLIMITED : String.valueOf(stockOrSpace));
            } else {
                stockText = (stock == Integer.MAX_VALUE) ? SHOP_STOCK_UNLIMITED : String.valueOf(stock);
            }
            text = text.replace(ShopLorePlaceholdersEnum.SHOP_STOCK.value(), stockText);
        }

        text = text.replace(ShopLorePlaceholdersEnum.SHOP_PER_ITEM_QTY.value(),
                String.valueOf(shop.getItem().getAmount()));

        if (text.contains(ShopLorePlaceholdersEnum.SHOP_OWNER.value())) {
            OfflinePlayer shopOwner = Bukkit.getOfflinePlayer(shop.getShopOwner());
            // set a generic name for shops with no owner name ('Admin')
            String ownerName = shopOwner.getName() != null ? shopOwner.getName() : "Admin";
            text = text.replace(ShopLorePlaceholdersEnum.SHOP_OWNER.value(), ownerName);
        }

        if (text.contains(ShopLorePlaceholdersEnum.SHOP_LOCATION.value())) {
            Location loc = shop.getShopLocation();
            String locText = loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ();
            text = text.replace(ShopLorePlaceholdersEnum.SHOP_LOCATION.value(), locText);
        }

        text = text.replace(ShopLorePlaceholdersEnum.SHOP_WORLD.value(),
                Objects.requireNonNull(shop.getShopLocation().getWorld()).getName());

        // Added in v2.0
        text = text.replace(ShopLorePlaceholdersEnum.SHOP_VISITS.value(),
                String.valueOf(ShopSearchActivityStorageUtil.getPlayerVisitCount(shop.getShopLocation())));

        return text;
    }

    private int processUnknownStockSpace(FoundShopItemModel shop) {
        return FindItemAddOn.getQsApiInstance().processUnknownStockSpace(shop.getShopLocation(), shop.isToBuy());
    }

    private ItemStack buildItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            meta = Bukkit.getItemFactory().getItemMeta(material);
        }
        if (meta == null) {
            return item;
        }

        meta.setDisplayName(translate(name));
        if (lore != null && !lore.isEmpty()) {
            meta.setLore(lore.stream().map(this::translate).toList());
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private int getPageCount(int shopCount) {
        return Math.max(1, (shopCount + SHOPS_PER_PAGE - 1) / SHOPS_PER_PAGE);
    }

    private void sendFirstPageAlert(Player player) {
        sendAlert(player, configProvider.SHOP_NAV_FIRST_PAGE_ALERT_MSG);
    }

    private void sendLastPageAlert(Player player) {
        sendAlert(player, configProvider.SHOP_NAV_LAST_PAGE_ALERT_MSG);
    }

    private void sendAlert(Player player, String message) {
        if (!StringUtils.isEmpty(message)) {
            PlayerUtil.sendMessage(player, configProvider.PLUGIN_PREFIX + message);
        }
    }

    private String prettify(Material material) {
        if (material == null) {
            return "Unknown";
        }
        String[] words = material.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return builder.toString();
    }

    private String replaceDelayPlaceholder(String tpDelayMsg, long delay) {
        return tpDelayMsg.replace("{DELAY}", String.valueOf(delay));
    }

    private String formatNumber(double number) {
        if (configProvider.SHOP_GUI_USE_SHORTER_CURRENCY_FORMAT) {
            if (number < 100_000) {
                return String.format("%,.2f", number);
            } else if (number < 1_000_000) {
                return String.format("%.2fK", number / 1_000.0);
            } else if (number < 1_000_000_000) {
                return String.format("%.2fM", number / 1_000_000.0);
            } else if (number < 1_000_000_000_000L) {
                return String.format("%.2fB", number / 1_000_000_000.0);
            } else {
                return String.format("%.2fT", number / 1_000_000_000_000.0);
            }
        } else {
            return String.format("%,.2f", number);
        }
    }

    private String replaceCustomCmdPlaceholders(String cmd, Player player, Location shopLoc) {
        return cmd
                .replace(CustomCmdPlaceholdersEnum.PLAYER_NAME.value(), player.getName())
                .replace(CustomCmdPlaceholdersEnum.SHOP_LOC_X.value(), Double.toString(shopLoc.getX()))
                .replace(CustomCmdPlaceholdersEnum.SHOP_LOC_Y.value(), Double.toString(shopLoc.getY()))
                .replace(CustomCmdPlaceholdersEnum.SHOP_LOC_Z.value(), Double.toString(shopLoc.getZ()))
                .replace(CustomCmdPlaceholdersEnum.SHOP_WORLD.value(), shopLoc.getWorld().getName());
    }

    private String translate(String text) {
        return ColorTranslator.translateColorCodes(text);
    }
}
