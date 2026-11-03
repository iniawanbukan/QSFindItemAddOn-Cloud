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
import org.bukkit.Tag;

import java.util.Set;
import java.util.function.Predicate;

/**
 * Item categories offered by the /qss shop browser. Left-clicking the category
 * button advances to the next category, right-clicking goes to the previous
 * one.
 *
 * @author myzticbean
 */
public enum QssCategory {

    ALL("All", Material.CHEST, material -> true),
    BLOCKS("Blocks", Material.GRASS_BLOCK, Material::isBlock),
    TOOLS("Tools", Material.DIAMOND_PICKAXE, QssCategory::isTool),
    FOOD("Food", Material.COOKED_BEEF, Material::isEdible),
    COMBAT("Combat", Material.DIAMOND_SWORD, QssCategory::isCombat),
    POTIONS("Potions", Material.POTION, QssCategory::isPotion),
    BOOKS("Books", Material.BOOK, QssCategory::isBook),
    INGREDIENTS("Ingredients", Material.SUGAR, QssCategory::isIngredient),
    UTILITIES("Utilities", Material.COMPASS, QssCategory::isUtility);

    private static final Set<Material> EXTRA_TOOLS = Set.of(
            Material.FISHING_ROD,
            Material.SHEARS,
            Material.FLINT_AND_STEEL,
            Material.BRUSH,
            Material.SPYGLASS,
            Material.CARROT_ON_A_STICK,
            Material.WARPED_FUNGUS_ON_A_STICK,
            Material.LEAD
    );

    private static final Set<Material> POTION_MATERIALS = Set.of(
            Material.POTION,
            Material.SPLASH_POTION,
            Material.LINGERING_POTION,
            Material.TIPPED_ARROW,
            Material.DRAGON_BREATH,
            Material.GLASS_BOTTLE
    );

    private static final Set<Material> BOOK_MATERIALS = Set.of(
            Material.BOOK,
            Material.WRITTEN_BOOK,
            Material.WRITABLE_BOOK,
            Material.ENCHANTED_BOOK,
            Material.KNOWLEDGE_BOOK
    );

    private static final Set<Material> COMBAT_MATERIALS = Set.of(
            Material.BOW,
            Material.CROSSBOW,
            Material.ARROW,
            Material.SPECTRAL_ARROW,
            Material.TIPPED_ARROW,
            Material.TRIDENT,
            Material.SHIELD,
            Material.TOTEM_OF_UNDYING,
            Material.MACE,
            Material.WIND_CHARGE
    );

    private static final Set<Material> INGREDIENT_MATERIALS = Set.of(
            Material.COAL,
            Material.CHARCOAL,
            Material.RAW_IRON,
            Material.IRON_INGOT,
            Material.RAW_COPPER,
            Material.COPPER_INGOT,
            Material.RAW_GOLD,
            Material.GOLD_INGOT,
            Material.NETHERITE_SCRAP,
            Material.NETHERITE_INGOT,
            Material.DIAMOND,
            Material.EMERALD,
            Material.LAPIS_LAZULI,
            Material.REDSTONE,
            Material.QUARTZ,
            Material.AMETHYST_SHARD,
            Material.STICK,
            Material.STRING,
            Material.FEATHER,
            Material.LEATHER,
            Material.RABBIT_HIDE,
            Material.GUNPOWDER,
            Material.GLOWSTONE_DUST,
            Material.BLAZE_POWDER,
            Material.BLAZE_ROD,
            Material.SLIME_BALL,
            Material.MAGMA_CREAM,
            Material.GHAST_TEAR,
            Material.SUGAR,
            Material.WHEAT,
            Material.WHEAT_SEEDS,
            Material.COCOA_BEANS,
            Material.BONE_MEAL,
            Material.INK_SAC,
            Material.GLOW_INK_SAC,
            Material.CLAY_BALL,
            Material.BRICK,
            Material.NETHER_WART,
            Material.SPIDER_EYE,
            Material.FERMENTED_SPIDER_EYE,
            Material.PHANTOM_MEMBRANE,
            Material.ECHO_SHARD,
            Material.HONEYCOMB,
            Material.RABBIT_FOOT,
            Material.POPPED_CHORUS_FRUIT,
            Material.SHULKER_SHELL,
            Material.TURTLE_SCUTE,
            Material.ARMADILLO_SCUTE,
            Material.BREEZE_ROD,
            Material.NETHER_STAR,
            Material.HEART_OF_THE_SEA,
            Material.PRISMARINE_CRYSTALS,
            Material.PRISMARINE_SHARD,
            Material.NAUTILUS_SHELL
    );

    private static final Set<Material> UTILITY_MATERIALS = Set.of(
            Material.BUCKET,
            Material.WATER_BUCKET,
            Material.LAVA_BUCKET,
            Material.MILK_BUCKET,
            Material.POWDER_SNOW_BUCKET,
            Material.AXOLOTL_BUCKET,
            Material.COD_BUCKET,
            Material.SALMON_BUCKET,
            Material.TROPICAL_FISH_BUCKET,
            Material.PUFFERFISH_BUCKET,
            Material.TADPOLE_BUCKET,
            Material.COMPASS,
            Material.RECOVERY_COMPASS,
            Material.CLOCK,
            Material.MAP,
            Material.FILLED_MAP,
            Material.LEAD,
            Material.NAME_TAG,
            Material.SADDLE,
            Material.ELYTRA,
            Material.FIREWORK_ROCKET,
            Material.FIREWORK_STAR,
            Material.TORCH,
            Material.SOUL_TORCH,
            Material.LANTERN,
            Material.SOUL_LANTERN,
            Material.CAMPFIRE,
            Material.SOUL_CAMPFIRE,
            Material.ENDER_PEARL,
            Material.ENDER_EYE,
            Material.SNOWBALL,
            Material.EGG,
            Material.TNT,
            Material.MINECART,
            Material.CHEST_MINECART,
            Material.FURNACE_MINECART,
            Material.HOPPER_MINECART,
            Material.TNT_MINECART,
            Material.RAIL,
            Material.POWERED_RAIL,
            Material.DETECTOR_RAIL,
            Material.ACTIVATOR_RAIL
    );

    private final String displayName;
    private final Material icon;
    private final Predicate<Material> matcher;

    QssCategory(String displayName, Material icon, Predicate<Material> matcher) {
        this.displayName = displayName;
        this.icon = icon;
        this.matcher = matcher;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }

    public boolean matches(Material material) {
        return matcher.test(material);
    }

    public QssCategory next() {
        QssCategory[] categories = values();
        return categories[(ordinal() + 1) % categories.length];
    }

    public QssCategory previous() {
        QssCategory[] categories = values();
        return categories[(ordinal() - 1 + categories.length) % categories.length];
    }

    private static boolean isTool(Material material) {
        return Tag.ITEMS_TOOLS.isTagged(material)
                || Tag.ITEMS_PICKAXES.isTagged(material)
                || Tag.ITEMS_AXES.isTagged(material)
                || Tag.ITEMS_SHOVELS.isTagged(material)
                || Tag.ITEMS_HOES.isTagged(material)
                || EXTRA_TOOLS.contains(material);
    }

    private static boolean isCombat(Material material) {
        return Tag.ITEMS_SWORDS.isTagged(material)
                || Tag.ITEMS_HEAD_ARMOR.isTagged(material)
                || Tag.ITEMS_CHEST_ARMOR.isTagged(material)
                || Tag.ITEMS_LEG_ARMOR.isTagged(material)
                || Tag.ITEMS_FOOT_ARMOR.isTagged(material)
                || COMBAT_MATERIALS.contains(material);
    }

    private static boolean isPotion(Material material) {
        return POTION_MATERIALS.contains(material);
    }

    private static boolean isBook(Material material) {
        return BOOK_MATERIALS.contains(material);
    }

    private static boolean isIngredient(Material material) {
        return INGREDIENT_MATERIALS.contains(material);
    }

    private static boolean isUtility(Material material) {
        if (UTILITY_MATERIALS.contains(material)) {
            return true;
        }
        String name = material.name();
        return name.endsWith("_BED")
                || name.endsWith("_BOAT")
                || name.endsWith("_MINECART")
                || name.endsWith("_SIGN");
    }
}
