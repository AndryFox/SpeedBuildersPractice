package it.andrea.speedbuilders;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class ItemUtils {

    public static ItemStack normalizeItem(Material mat, byte data, String category) {
        int amount = 1;

        switch (mat) {
            case QUARTZ_BLOCK:
            case LOG:
            case LOG_2:
                data = (byte) (data % 4);
                break;
            case LEAVES:
            case LEAVES_2:
                data = (byte) (data % 4);
                break;
            case CHEST:
            case TRAPPED_CHEST:
            case ENDER_CHEST:
            case FURNACE:
            case DISPENSER:
            case DROPPER:
            case HOPPER:
            case ENDER_PORTAL_FRAME:
            case ANVIL:
            case PUMPKIN:
            case JACK_O_LANTERN:
            case LADDER:
            case WOOD_BUTTON:
            case STONE_BUTTON:
            case RAILS:
            case ACTIVATOR_RAIL:
            case DETECTOR_RAIL:
            case POWERED_RAIL:
            case WOOD_STAIRS:
            case COBBLESTONE_STAIRS:
            case BRICK_STAIRS:
            case SMOOTH_STAIRS:
            case NETHER_BRICK_STAIRS:
            case SANDSTONE_STAIRS:
            case SPRUCE_WOOD_STAIRS:
            case BIRCH_WOOD_STAIRS:
            case JUNGLE_WOOD_STAIRS:
            case QUARTZ_STAIRS:
            case ACACIA_STAIRS:
            case DARK_OAK_STAIRS:
            case PURPUR_STAIRS:
                data = 0;
                break;
            case STEP:
            case WOOD_STEP:
            case STONE_SLAB2:
            case PURPUR_SLAB:
                data = (byte) (data % 8);
                break;
            case DOUBLE_STEP:
                mat = Material.STEP;
                data = (byte) (data % 8);
                amount = 2;
                break;
            case WOOD_DOUBLE_STEP:
                mat = Material.WOOD_STEP;
                data = (byte) (data % 8);
                amount = 2;
                break;
            case DOUBLE_STONE_SLAB2:
                mat = Material.STONE_SLAB2;
                data = 0;
                amount = 2;
                break;
            case DOUBLE_PLANT:
                if (data >= 8) return null;
                break;
            case SKULL: mat = Material.SKULL_ITEM; data = 0; break;
            case STANDING_BANNER:
            case WALL_BANNER: mat = Material.BANNER; data = 0; break;
            case CAKE_BLOCK: mat = Material.CAKE; data = 0; break;
            case CAULDRON: mat = Material.CAULDRON_ITEM; data = 0; break;
            case REDSTONE_WIRE: mat = Material.REDSTONE; data = 0; break;
            case GLOWING_REDSTONE_ORE: mat = Material.REDSTONE_ORE; data = 0; break;
            case DIODE_BLOCK_OFF:
            case DIODE_BLOCK_ON: mat = Material.DIODE; data = 0; break;
            case REDSTONE_COMPARATOR_OFF:
            case REDSTONE_COMPARATOR_ON: mat = Material.REDSTONE_COMPARATOR; data = 0; break;
            case DAYLIGHT_DETECTOR_INVERTED: mat = Material.DAYLIGHT_DETECTOR; data = 0; break;
            case SUGAR_CANE_BLOCK: mat = Material.SUGAR_CANE; data = 0; break;
            case BED_BLOCK: mat = Material.BED; data = 0; break;
            case FLOWER_POT: mat = Material.FLOWER_POT_ITEM; data = 0; break;
            case BREWING_STAND: mat = Material.BREWING_STAND_ITEM; data = 0; break;
            case SIGN_POST:
            case WALL_SIGN: mat = Material.SIGN; data = 0; break;
            case WOODEN_DOOR: mat = Material.WOOD_DOOR; data = 0; break;
            case IRON_DOOR_BLOCK: mat = Material.IRON_DOOR; data = 0; break;
            case SPRUCE_DOOR: mat = Material.SPRUCE_DOOR_ITEM; data = 0; break;
            case BIRCH_DOOR: mat = Material.BIRCH_DOOR_ITEM; data = 0; break;
            case JUNGLE_DOOR: mat = Material.JUNGLE_DOOR_ITEM; data = 0; break;
            case ACACIA_DOOR: mat = Material.ACACIA_DOOR_ITEM; data = 0; break;
            case DARK_OAK_DOOR: mat = Material.DARK_OAK_DOOR_ITEM; data = 0; break;
            default: break;
        }

        return new ItemStack(mat, amount, data);
    }
}