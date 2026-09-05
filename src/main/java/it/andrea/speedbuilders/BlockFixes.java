package it.andrea.speedbuilders;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockFadeEvent;

public class BlockFixes implements Listener {

    private final Main plugin;

    public BlockFixes(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockPhysics(BlockPhysicsEvent event) {
        if (event.getBlock().getWorld().getName().equals("practice")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockInteract(PlayerInteractEvent event) {
        if (!event.getPlayer().getWorld().getName().equals("practice")) return;

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block clicked = event.getClickedBlock();
            if (clicked == null) return;

            if (clicked.getType() == Material.DAYLIGHT_DETECTOR || clicked.getType() == Material.DAYLIGHT_DETECTOR_INVERTED) {
                triggerPerfectCheck(event.getPlayer());
            }

            if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
                String mat = clicked.getType().name();
                if (mat.contains("CHEST") || mat.contains("FURNACE") || mat.contains("WORKBENCH") ||
                        mat.contains("ANVIL") || mat.contains("BREWING") || mat.contains("HOPPER") ||
                        mat.contains("DISPENSER") || mat.contains("DROPPER") || mat.contains("ENCHANTMENT") ||
                        mat.contains("BED") || mat.contains("BUTTON") || mat.contains("LEVER") ||
                        mat.contains("DIODE") || mat.contains("COMPARATOR") || mat.contains("FENCE_GATE") ||
                        mat.contains("TRAP_DOOR") || mat.contains("NOTE_BLOCK") || mat.contains("JUKEBOX") ||
                        mat.contains("OBSIDIAN")) {

                    event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
                    event.setUseItemInHand(org.bukkit.event.Event.Result.ALLOW);
                }
            }

            ItemStack item = event.getItem();
            if (item != null) {
                Block placeLoc = clicked.getRelative(event.getBlockFace());

                // MULTIPLAYER FIX: Calculate relative coordinates
                int plotId = plugin.getPlotManager().getPlot(event.getPlayer());
                Location centerLoc = plugin.getPlotManager().getPlotCenter(event.getPlayer().getWorld(), plotId);
                int cX = centerLoc.getBlockX(), cZ = centerLoc.getBlockZ();

                if (item.getType() == Material.FLOWER_POT_ITEM || item.getType().name().contains("DOOR")) {

                    if (!(Math.abs(placeLoc.getX() - cX) <= 3 && Math.abs(placeLoc.getZ() - cZ) <= 3 && placeLoc.getY() > 100)) {
                        if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
                            event.setCancelled(true);
                            event.getPlayer().sendMessage("§cPuoi costruire solo nel riquadro nero!");
                        }
                        return;
                    }

                    if (placeLoc.getType() == Material.AIR) {
                        if (item.getType() == Material.FLOWER_POT_ITEM) {
                            event.setCancelled(true);
                            placeLoc.setType(Material.FLOWER_POT);
                            consumeItem(event.getPlayer(), item);
                            triggerPerfectCheck(event.getPlayer());
                        } else {
                            Block topLoc = placeLoc.getRelative(BlockFace.UP);
                            if (topLoc.getType() == Material.AIR) {
                                event.setCancelled(true);
                                Material doorMat = (item.getType() == Material.IRON_DOOR) ? Material.IRON_DOOR_BLOCK : Material.WOODEN_DOOR;
                                if (item.getType().name().contains("SPRUCE")) doorMat = Material.SPRUCE_DOOR;
                                if (item.getType().name().contains("BIRCH")) doorMat = Material.BIRCH_DOOR;
                                if (item.getType().name().contains("JUNGLE")) doorMat = Material.JUNGLE_DOOR;
                                if (item.getType().name().contains("ACACIA")) doorMat = Material.ACACIA_DOOR;
                                if (item.getType().name().contains("DARK_OAK")) doorMat = Material.DARK_OAK_DOOR;

                                placeLoc.setType(doorMat);
                                placeLoc.setData((byte) 0);
                                topLoc.setType(doorMat);
                                topLoc.setData((byte) 8);

                                consumeItem(event.getPlayer(), item);
                                triggerPerfectCheck(event.getPlayer());
                            }
                        }
                    }
                }
            }
        }
    }

    private void consumeItem(org.bukkit.entity.Player player, ItemStack item) {
        if (player.getGameMode() == GameMode.CREATIVE) return;
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }
    }

    private void triggerPerfectCheck(org.bukkit.entity.Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (plugin.getGameManager().checkBuildPerfect(player)) {
                plugin.getGameManager().handlePerfect(player);
            }
        });
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent event) {
        if (event.getLocation().getWorld().getName().equals("practice")) event.setCancelled(true);
    }

    @EventHandler
    public void onPrime(ExplosionPrimeEvent event) {
        if (event.getEntity().getWorld().getName().equals("practice")) event.setCancelled(true);
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getLocation().getWorld().getName().equals("practice")) {
            if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.BUILD_IRONGOLEM ||
                    event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.BUILD_SNOWMAN ||
                    event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.BUILD_WITHER) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockSpread(BlockSpreadEvent event) {
        if (event.getBlock().getWorld().getName().equals("practice")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockGrow(BlockGrowEvent event) {
        if (event.getBlock().getWorld().getName().equals("practice")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockFade(BlockFadeEvent event) {
        if (event.getBlock().getWorld().getName().equals("practice")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer().getWorld().getName().equals("practice") && !event.getPlayer().hasPermission("speedbuilders.admin")) {

            if (event.getInventory().getHolder() == null) {
                return;
            }

            org.bukkit.event.inventory.InventoryType type = event.getInventory().getType();
            if (type == org.bukkit.event.inventory.InventoryType.ANVIL ||
                    type == org.bukkit.event.inventory.InventoryType.HOPPER ||
                    type == org.bukkit.event.inventory.InventoryType.CHEST ||
                    type == org.bukkit.event.inventory.InventoryType.DISPENSER ||
                    type == org.bukkit.event.inventory.InventoryType.DROPPER ||
                    type == org.bukkit.event.inventory.InventoryType.FURNACE) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        org.bukkit.entity.Player player = event.getPlayer();
        if (!player.getWorld().getName().equals("practice")) return;

        Block b = event.getBlock();
        int x = b.getX();
        int y = b.getY();
        int z = b.getZ();

        // MULTIPLAYER FIX: Calculate relative coordinates
        int plotId = plugin.getPlotManager().getPlot(player);
        Location centerLoc = plugin.getPlotManager().getPlotCenter(player.getWorld(), plotId);
        int cX = centerLoc.getBlockX(), cZ = centerLoc.getBlockZ();

        boolean isBuildArea = (Math.abs(x - cX) <= 3) && (Math.abs(z - cZ) <= 3) && (y > 100);

        GameManager gm = plugin.getGameManager();
        String state = gm.getState(player);

        if (player.getGameMode() == GameMode.SURVIVAL) {
            if (!isBuildArea) {
                event.setCancelled(true);
                return;
            }

            if (!state.equals("PLAYING")) {
                event.setCancelled(true);
                player.sendMessage("§cNon puoi rompere i blocchi in questa fase!");
                return;
            }

            event.setDropItems(false);
            Material type = b.getType();
            byte data = b.getData();

            if (type == Material.DOUBLE_PLANT || type.name().contains("DOOR")) {
                Block top = (data >= 8) ? b : b.getRelative(BlockFace.UP);
                Block bottom = (data >= 8) ? b.getRelative(BlockFace.DOWN) : b;
                byte bottomData = bottom.getData();

                if (top.getType() == type) top.setType(Material.AIR);
                if (bottom.getType() == type) bottom.setType(Material.AIR);

                ItemStack toGive = ItemUtils.normalizeItem(type, bottomData, gm.getCurrentCategory(player));
                if (toGive != null) player.getInventory().addItem(toGive);
                triggerPerfectCheck(player);
                return;
            }

            ItemStack toGive = ItemUtils.normalizeItem(type, data, gm.getCurrentCategory(player));
            if (toGive != null) player.getInventory().addItem(toGive);
            triggerPerfectCheck(player);
        }
        else if (player.getGameMode() == GameMode.CREATIVE) {
            if (!isBuildArea) {
                if (!player.isOp()) {
                    event.setCancelled(true);
                } else if (!player.isSneaking()) {
                    event.setCancelled(true);
                }
            }

            if (state.equals("PLAYING")) {
                triggerPerfectCheck(player);
            }
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onBlockPlace(org.bukkit.event.block.BlockPlaceEvent event) {
        org.bukkit.entity.Player player = event.getPlayer();
        if (!player.getWorld().getName().equals("practice")) return;

        Block b = event.getBlock();
        int x = b.getX();
        int y = b.getY();
        int z = b.getZ();

        // MULTIPLAYER FIX: Calculate relative coordinates
        int plotId = plugin.getPlotManager().getPlot(player);
        Location centerLoc = plugin.getPlotManager().getPlotCenter(player.getWorld(), plotId);
        int cX = centerLoc.getBlockX(), cZ = centerLoc.getBlockZ();

        boolean isBuildArea = (Math.abs(x - cX) <= 3) && (Math.abs(z - cZ) <= 3) && (y > 100);

        if (player.getGameMode() == GameMode.SURVIVAL) {
            if (!isBuildArea) {
                event.setCancelled(true);
            }
        }
        else if (player.getGameMode() == GameMode.CREATIVE) {
            if (!isBuildArea) {
                if (!player.isOp()) {
                    event.setCancelled(true);
                } else if (!player.isSneaking()) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onBannedItemsUse(PlayerInteractEvent event) {
        if (!event.getPlayer().getWorld().getName().equals("practice")) return;

        ItemStack item = event.getItem();
        if (item == null) return;

        Material t = item.getType();

        if (t == Material.FLINT_AND_STEEL ||
                t == Material.EXPLOSIVE_MINECART ||
                t == Material.MONSTER_EGG ||
                t.name().contains("POTION") ||
                t == Material.LAVA_BUCKET ||
                t == Material.WATER_BUCKET ||
                t == Material.ENDER_PEARL ||
                t == Material.EYE_OF_ENDER ||
                t == Material.FIREWORK) {

            event.setCancelled(true);
            event.getPlayer().sendMessage("§cQuesto oggetto è disabilitato per motivi di sicurezza!");
            return;
        }

        if (t == Material.INK_SACK && item.getDurability() == 15) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cLa farina d'ossa è disabilitata!");
        }
    }

    @EventHandler
    public void onPortalCreate(org.bukkit.event.world.PortalCreateEvent event) {
        if (event.getWorld().getName().equals("practice")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFireIgnite(org.bukkit.event.block.BlockIgniteEvent event) {
        if (event.getBlock().getWorld().getName().equals("practice")) {
            event.setCancelled(true);
        }
    }

}