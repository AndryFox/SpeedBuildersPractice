package it.andrea.speedbuilders;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
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

            if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
                String mat = clicked.getType().name();
                if (mat.contains("CHEST") || mat.contains("FURNACE") || mat.contains("WORKBENCH") ||
                        mat.contains("ANVIL") || mat.contains("BREWING") || mat.contains("HOPPER") ||
                        mat.contains("DISPENSER") || mat.contains("DROPPER") || mat.contains("ENCHANTMENT") ||
                        mat.contains("BED") || mat.contains("BUTTON") || mat.contains("LEVER") ||
                        mat.contains("DIODE") || mat.contains("COMPARATOR") || mat.contains("FENCE_GATE") ||
                        mat.contains("TRAP_DOOR") || mat.contains("NOTE_BLOCK") || mat.contains("JUKEBOX")) {

                    event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
                }
            }

            ItemStack item = event.getItem();
            if (item != null && clicked.getType().name().contains("GLASS")) {
                Block placeLoc = clicked.getRelative(event.getBlockFace());

                if (placeLoc.getType() == Material.AIR) {
                    if (item.getType() == Material.FLOWER_POT_ITEM) {
                        event.setCancelled(true);
                        placeLoc.setType(Material.FLOWER_POT);
                        consumeItem(event.getPlayer(), item);
                        triggerPerfectCheck(event.getPlayer());
                    }
                    else if (item.getType() == Material.WOOD_DOOR || item.getType() == Material.IRON_DOOR || item.getType().name().contains("DOOR_ITEM")) {
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

    // --- NUOVI FIX AGGIUNTI DA QUI IN POI ---

    // Blocca definitivamente l'apertura delle interfacce per poter piazzare blocchi (shiftando) su incudini, hopper ecc.
    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer().getWorld().getName().equals("practice") && !event.getPlayer().hasPermission("speedbuilders.admin")) {
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

    // Gestisce i blocchi rotti in modo personalizzato: blocca il drop vanilla e usa il Normalizzatore
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        org.bukkit.entity.Player player = event.getPlayer();
        if (player.getWorld().getName().equals("practice")) {
            GameManager gm = plugin.getGameManager();
            if (gm.getState(player).equals("PLAYING")) {
                event.setDropItems(false); // Niente drop a terra (fix per i doppi fiori, ecc.)

                Block b = event.getBlock();
                Material type = b.getType();

                // Restituisce l'oggetto perfetto per l'inventario tramite GameManager
                ItemStack toGive = gm.normalizeItem(type, b.getData(), gm.getCurrentCategory(player));
                if (toGive != null) {
                    player.getInventory().addItem(toGive);
                }

                triggerPerfectCheck(player);
            }
        }
    }
}