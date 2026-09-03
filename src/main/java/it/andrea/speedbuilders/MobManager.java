package it.andrea.speedbuilders;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SpawnEggMeta;

public class MobManager implements Listener {
    private final Main plugin;

    public MobManager(Main plugin) {
        this.plugin = plugin;
    }

    // Pulisce il nome del cartello e lo converte in EntityType
    public EntityType getEntityType(String name) {
        name = name.toUpperCase().replace(" ", "_");
        if (name.contains("SNOWMAN")) return EntityType.SNOWMAN;
        if (name.contains("IRON_GOLEM")) return EntityType.IRON_GOLEM;
        if (name.contains("PIG_ZOMBIE")) return EntityType.PIG_ZOMBIE;
        if (name.contains("MOOSHROOM") || name.contains("MUSHROOM_COW")) return EntityType.MUSHROOM_COW;
        try {
            return EntityType.valueOf(name);
        } catch (Exception e) {
            return EntityType.PIG; // Fallback se il nome è scritto male
        }
    }

    // Genera l'uovo per l'inventario
    public ItemStack getMobEgg(String mobName) {
        ItemStack egg = new ItemStack(Material.MONSTER_EGG, 1);
        SpawnEggMeta meta = (SpawnEggMeta) egg.getItemMeta();
        meta.setSpawnedType(getEntityType(mobName));
        egg.setItemMeta(meta);
        return egg;
    }

    // Piazza il mob perfettamente centrato e senza IA
    @EventHandler
    public void onEggUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!player.getWorld().getName().equals("practice")) return;

        ItemStack item = event.getItem();
        if (item != null && item.getType() == Material.MONSTER_EGG) {
            event.setCancelled(true); // Annulla lo spawn vanilla sballato

            if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                if (!plugin.getGameManager().getState(player).equals("PLAYING")) return;

                org.bukkit.block.Block clicked = event.getClickedBlock();
                org.bukkit.block.Block target = clicked.getRelative(event.getBlockFace());

                Location spawnLoc = target.getLocation().add(0.5, 0, 0.5);
                spawnLoc.setYaw(player.getLocation().getYaw() + 180f); // Rivolto verso il giocatore

                SpawnEggMeta meta = (SpawnEggMeta) item.getItemMeta();
                Entity entity = player.getWorld().spawnEntity(spawnLoc, meta.getSpawnedType());
                entity.setCustomName("SpeedBuildersMob");
                entity.setCustomNameVisible(false);

                // Rende il mob una statuina
                if (entity instanceof LivingEntity) {
                    LivingEntity le = (LivingEntity) entity;
                    le.setAI(false);
                    le.setSilent(true);
                    le.setInvulnerable(true);
                    le.setCollidable(false);
                }

                if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
                    if (item.getAmount() > 1) item.setAmount(item.getAmount() - 1);
                    else player.getInventory().setItemInMainHand(null);
                }

                org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (plugin.getGameManager().checkBuildPerfect(player)) plugin.getGameManager().handlePerfect(player);
                }, 2L);
            }
        }
    }

    // Permette di "spaccare" il mob per rimuoverlo e riavere l'uovo
    @EventHandler
    public void onMobHit(EntityDamageByEntityEvent event) {
        if ("SpeedBuildersMob".equals(event.getEntity().getCustomName()) && event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            if (plugin.getGameManager().getState(player).equals("PLAYING")) {
                event.setCancelled(true);
                event.getEntity().remove();

                player.getInventory().addItem(getMobEgg(event.getEntity().getType().name()));

                org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (plugin.getGameManager().checkBuildPerfect(player)) plugin.getGameManager().handlePerfect(player);
                }, 2L);
            }
        }
    }

    // Rimuove tutti i mob dall'arena alla fine
    public void clearMobs(World world) {
        for (Entity ent : world.getEntities()) {
            if ("SpeedBuildersMob".equals(ent.getCustomName())) ent.remove();
        }
    }
}