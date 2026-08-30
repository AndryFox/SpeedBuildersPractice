package it.andrea.speedbuilders;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.List;

public class Listeners implements Listener {

    private final Main plugin;

    public Listeners(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getGameManager().setState(player, "IDLE");
        player.getInventory().clear();
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setGameMode(org.bukkit.GameMode.SURVIVAL);

        if (plugin.getConfig().contains("locations.lobby")) {
            Bukkit.getScheduler().runTask(plugin, () -> player.teleport((Location) plugin.getConfig().get("locations.lobby")));
        }
    }

    @EventHandler
    public void onArenaFloorClick(PlayerInteractEvent event) {
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            Block clicked = event.getClickedBlock();
            Player player = event.getPlayer();

            if (clicked != null && clicked.getType() == Material.QUARTZ_BLOCK) {
                int x = clicked.getX(), y = clicked.getY(), z = clicked.getZ();
                if (y == 100 && x >= -4 && x <= 4 && z >= -4 && z <= 4) {
                    if (player.getWorld().getName().equals("practice")) {
                        event.setCancelled(true);
                        GameManager gm = plugin.getGameManager();
                        int buildId = gm.getCurrentBuild(player);
                        if (buildId != -1) {
                            // Se lo stato è IDLE è il primo click (8s), altrimenti è un riavvio veloce (6s)
                            boolean isRetry = !gm.getState(player).equals("IDLE");

                            gm.forceReset(player);
                            gm.loadBuild(player, buildId);
                            gm.startCountdown(player, isRetry);
                        } else {
                            player.sendMessage("§cDevi prima caricare una build con /map load <id>!");
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) return;
        GameManager gm = plugin.getGameManager();

        if (gm.isLobbyWorld(player.getWorld())) { event.setCancelled(true); return; }

        Block block = event.getBlock();
        if (!block.getWorld().getName().equals("practice")) return;

        if (!(block.getX() >= -3 && block.getX() <= 3 && block.getZ() >= -3 && block.getZ() <= 3 && block.getY() > 100)) {
            event.setCancelled(true); player.sendMessage("§cPuoi costruire solo nel riquadro nero!"); return;
        }

        String state = gm.getState(player);
        if (!state.equals("PLAYING")) {
            event.setCancelled(true);
            player.sendMessage(state.equals("COUNTDOWN") ? "§cAttendi la fine del countdown!" : "§cClicca sul pavimento nero per iniziare!");
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (gm.checkBuildPerfect(player)) gm.handlePerfect(player);
        });
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onBlockBreak(org.bukkit.event.block.BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) return;
        GameManager gm = plugin.getGameManager();

        if (gm.isLobbyWorld(player.getWorld())) { event.setCancelled(true); return; }

        Block block = event.getBlock();
        if (!block.getWorld().getName().equals("practice")) return;

        if (!(block.getX() >= -3 && block.getX() <= 3 && block.getZ() >= -3 && block.getZ() <= 3 && block.getY() > 100)) {
            event.setCancelled(true); player.sendMessage("§cNon puoi distruggere l'arena!"); return;
        }

        if (!gm.getState(player).equals("PLAYING")) { event.setCancelled(true); return; }

        event.setDropItems(false);

        // Usa il traduttore che abbiamo creato in GameManager
        Material dropMat = gm.getInventoryItemMaterial(block.getType());
        byte dropData = block.getData();
        boolean shouldDrop = true;

        // Normalizza il drop
        if (dropMat == Material.LOG || dropMat == Material.LOG_2) {
            dropData = (byte) (dropData % 4);
        } else if (dropMat.name().contains("STEP") || dropMat.name().contains("SLAB")) {
            dropData = (byte) (dropData % 8);
        } else if (dropMat.name().contains("DOOR") || dropMat == Material.BED) {
            dropData = 0;
            // Se sta rompendo la parte alta della porta non droppa un secondo oggetto
            if (block.getData() >= 8) shouldDrop = false;
        }

        if (shouldDrop) {
            player.getInventory().addItem(new ItemStack(dropMat, 1, dropData));
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (gm.checkBuildPerfect(player)) gm.handlePerfect(player);
        });
    }

    @EventHandler
    public void onBlockDamage(org.bukkit.event.block.BlockDamageEvent event) {
        org.bukkit.entity.Player player = event.getPlayer();
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) return;
        org.bukkit.block.Block block = event.getBlock();
        if (!block.getWorld().getName().equals("practice")) return;

        if (block.getX() >= -3 && block.getX() <= 3 && block.getZ() >= -3 && block.getZ() <= 3 && block.getY() > 100) {
            if (plugin.getGameManager().getState(player).equals("PLAYING")) {
                event.setInstaBreak(true);
            }
        }
    }

    @EventHandler
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE || !player.getWorld().getName().equals("practice")) return;

        event.setCancelled(true);
        player.setAllowFlight(false);
        player.setFlying(false);
        new BukkitRunnable() {
            @Override
            public void run() {
                player.setVelocity(new Vector(0, 1.00, 0));
                player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 1f);
            }
        }.runTask(plugin);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE || !player.getWorld().getName().equals("practice")) return;

        if (!player.getAllowFlight()) {
            if (player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() != Material.AIR) {
                player.setAllowFlight(true);
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity().getWorld().getName().equals("practice")) { event.setCancelled(true); return; }
        if (event.getEntity() instanceof Player && plugin.getGameManager().isLobbyWorld(event.getEntity().getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity().getWorld().getName().equals("practice") || plugin.getGameManager().isLobbyWorld(event.getEntity().getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event) {
        if (event.getWorld().getName().equals("practice") && event.toWeatherState()) event.setCancelled(true);
    }

    @EventHandler
    public void onCitizensNpcClick(PlayerInteractEntityEvent event) {
        if (event.getRightClicked().hasMetadata("NPC")) {
            String npcName = org.bukkit.ChatColor.stripColor(event.getRightClicked().getName());

            if (npcName.equalsIgnoreCase("AndryFox_14")) {
                event.setCancelled(true);
                event.getPlayer().performCommand("p");
            }
            else if (npcName.equalsIgnoreCase("Lista Build")) {
                event.setCancelled(true);
                event.getPlayer().performCommand("p list");
            }
            else if (npcName.equalsIgnoreCase("Trova Errori")) {
                event.setCancelled(true);
                event.getPlayer().performCommand("p errors");
            }
            else if (npcName.equalsIgnoreCase("Guarda Build")) {
                event.setCancelled(true);
                event.getPlayer().performCommand("p view");
            }
        }
    }

    @EventHandler
    public void onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        String title = event.getView().getTitle();

        // Riconosce sia il menu normale che quello in modalità ricerca
        if (title.contains("Lista Build - P. ") || title.contains("Ricerca - P. ")) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

            Player player = (Player) event.getWhoClicked();
            ItemStack item = event.getCurrentItem();
            if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;

            String name = item.getItemMeta().getDisplayName();
            GameManager gm = plugin.getGameManager();

            // Calcola in che pagina ci troviamo leggendo il titolo
            int currentPage = 1;
            try {
                String[] split = title.split("- P. ");
                if (split.length > 1) currentPage = Integer.parseInt(split[1].trim());
            } catch (Exception ignored) {}

            // Se clicca il bottone per cercare
            if (item.getType() == Material.NAME_TAG && name.equals("§e§lCerca Build")) {
                player.closeInventory();
                gm.setAwaitingSearch(player, true); // Attiva la modalità ascolto chat
                player.sendMessage("§aScrivi in chat la parola da cercare!");
                player.sendMessage("§7(Oppure scrivi §cannulla§7 per annullare)");
                return;
            }

            // Frecce per cambiare pagina
            if (name.equals("§cPagina Precedente")) {
                gm.openBuildMenu(player, currentPage - 1);
                return;
            } else if (name.equals("§aPagina Successiva")) {
                gm.openBuildMenu(player, currentPage + 1);
                return;
            }

            // Se ha cliccato su una Build da giocare
            List<String> lore = item.getItemMeta().getLore();
            if (lore != null && !lore.isEmpty()) {
                String rawId = org.bukkit.ChatColor.stripColor(lore.get(0)).replace("ID: ", "").trim();
                try {
                    int id = Integer.parseInt(rawId);
                    player.closeInventory();

                    gm.forceReset(player);
                    gm.loadBuild(player, id);
                    gm.startCountdown(player, false);
                } catch (Exception ignored) {}
            }
        }
    }

    // Ascolta la chat per intercettare la ricerca
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        GameManager gm = plugin.getGameManager();

        if (gm.isAwaitingSearch(player)) {
            event.setCancelled(true); // Evita che il messaggio venga visto dagli altri
            gm.setAwaitingSearch(player, false); // Spegne la modalità ascolto

            String msg = event.getMessage();

            if (msg.equalsIgnoreCase("annulla")) {
                player.sendMessage("§cRicerca annullata.");
                return;
            }

            // Salva la parola ricercata e riapre il menu (necessario aprirlo in modo sincrono)
            gm.setActiveSearch(player, msg);
            Bukkit.getScheduler().runTask(plugin, () -> gm.openBuildMenu(player, 1));
        }
    }

}