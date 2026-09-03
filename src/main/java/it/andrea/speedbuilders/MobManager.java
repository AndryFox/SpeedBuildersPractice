package it.andrea.speedbuilders;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SpawnEggMeta;
import org.bukkit.metadata.FixedMetadataValue;

public class MobManager implements Listener {
    private final Main plugin;

    public MobManager(Main plugin) {
        this.plugin = plugin;
    }

    public EntityType getEntityType(String name) {
        name = name.toUpperCase().replace(" ", "_");
        if (name.contains("SNOWMAN")) return EntityType.SNOWMAN;
        if (name.contains("IRON_GOLEM")) return EntityType.IRON_GOLEM;
        if (name.contains("PIG_ZOMBIE")) return EntityType.PIG_ZOMBIE;
        if (name.contains("MOOSHROOM") || name.contains("MUSHROOM_COW")) return EntityType.MUSHROOM_COW;
        try {
            return EntityType.valueOf(name);
        } catch (Exception e) {
            return EntityType.PIG;
        }
    }

    public ItemStack getMobEgg(String mobName) {
        ItemStack egg = new ItemStack(Material.MONSTER_EGG, 1);
        SpawnEggMeta meta = (SpawnEggMeta) egg.getItemMeta();
        meta.setSpawnedType(getEntityType(mobName));
        egg.setItemMeta(meta);
        return egg;
    }

    @EventHandler
    public void onEggUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!player.getWorld().getName().equals("practice")) return;

        ItemStack item = event.getItem();
        if (item != null && item.getType() == Material.MONSTER_EGG) {
            event.setCancelled(true);

            if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                if (!plugin.getGameManager().getState(player).equals("PLAYING")) return;

                org.bukkit.block.Block clicked = event.getClickedBlock();
                org.bukkit.block.Block target = clicked.getRelative(event.getBlockFace());

                if (!(target.getX() >= -3 && target.getX() <= 3 && target.getZ() >= -3 && target.getZ() <= 3 && target.getY() > 100)) {
                    player.sendMessage("§cPuoi piazzare i mob solo nel riquadro nero!");
                    return;
                }

                Location spawnLoc = target.getLocation().add(0.5, 0, 0.5);

                // Arrotonda la visuale ai 90 gradi più vicini per posizionarli dritti
                float yaw = player.getLocation().getYaw() + 180f;
                yaw = Math.round(yaw / 90.0f) * 90.0f;
                spawnLoc.setYaw(yaw);

                SpawnEggMeta meta = (SpawnEggMeta) item.getItemMeta();
                Entity entity = player.getWorld().spawnEntity(spawnLoc, meta.getSpawnedType());

                // Usa i Metadata invece del nome per renderli totalmente anonimi
                entity.setMetadata("SpeedBuildersMob", new FixedMetadataValue(plugin, true));

                if (entity instanceof LivingEntity) {
                    LivingEntity le = (LivingEntity) entity;
                    le.setAI(false);
                    le.setSilent(true);
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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMobHit(EntityDamageByEntityEvent event) {
        if (event.getEntity().hasMetadata("SpeedBuildersMob") && event.getDamager() instanceof Player) {
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

    public void clearMobs(World world) {
        for (Entity ent : world.getEntities()) {
            if (ent.hasMetadata("SpeedBuildersMob")) ent.remove();
        }
    }
}