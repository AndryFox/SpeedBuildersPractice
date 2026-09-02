package it.andrea.speedbuilders;

import org.bukkit.*;
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
        player.setGameMode(org.bukkit.GameMode.SURVIVAL);

        player.setFlying(false);

        plugin.getConfig().set("players." + player.getUniqueId() + ".dj", true);
        plugin.saveConfig();

        player.setAllowFlight(true);

        if (plugin.getConfig().contains("locations.lobby")) {
            Bukkit.getScheduler().runTask(plugin, () -> player.teleport((Location) plugin.getConfig().get("locations.lobby")));
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                int wrCount = plugin.getDatabase().getPlayerWRCount(player.getName());

                String rankColor = "§e";
                String rankName = "Newbie";
                String tag = "Newbie";

                if (wrCount >= 100) { rankColor = "§6"; rankName = "Greatest of All Time"; tag = "GOAT"; }
                else if (wrCount >= 90) { rankColor = "§e"; rankName = "Legend"; tag = "Legend"; }
                else if (wrCount >= 80) { rankColor = "§6"; rankName = "Grandmaster"; tag = "G-Master"; }
                else if (wrCount >= 70) { rankColor = "§c"; rankName = "Master"; tag = "Master"; }
                else if (wrCount >= 60) { rankColor = "§4"; rankName = "Expert"; tag = "Expert"; }
                else if (wrCount >= 50) { rankColor = "§c"; rankName = "Imperial"; tag = "Imperial"; }
                else if (wrCount >= 45) { rankColor = "§d"; rankName = "Professional"; tag = "Pro"; }
                else if (wrCount >= 40) { rankColor = "§5"; rankName = "Talented"; tag = "Talented"; }
                else if (wrCount >= 35) { rankColor = "§9"; rankName = "Skilled"; tag = "Skilled"; }
                else if (wrCount >= 30) { rankColor = "§1"; rankName = "Seasoned"; tag = "Seasoned"; }
                else if (wrCount >= 25) { rankColor = "§3"; rankName = "Experienced"; tag = "Experienced"; }
                else if (wrCount >= 20) { rankColor = "§2"; rankName = "Trained"; tag = "Trained"; }
                else if (wrCount >= 15) { rankColor = "§a"; rankName = "Apprentice"; tag = "Apprentice"; }
                else if (wrCount >= 10) { rankColor = "§1"; rankName = "Amateur"; tag = "Amateur"; }
                else if (wrCount >= 6) { rankColor = "§8"; rankName = "Rookie"; tag = "Rookie"; }
                else if (wrCount >= 3) { rankColor = "§7"; rankName = "Novice"; tag = "Novice"; }
                else if (wrCount >= 1) { rankColor = "§f"; rankName = "Prospect"; tag = "Prospect"; }

                if (player.getName().equalsIgnoreCase("AndryFox_14")) {
                    rankColor = "§b";
                    rankName = "Elite Fox";
                    tag = "Elite Fox";
                }

                final String finalColor = rankColor;
                final String finalRankName = rankName;
                final String finalTag = tag;
                final int finalWrCount = wrCount;

                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        String message = "§6§lWR Totali: §f" + finalWrCount + " §8| " + finalColor + "§l" + finalRankName;
                        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, net.md_5.bungee.api.chat.TextComponent.fromLegacyText(message));

                        player.setLevel(finalWrCount);

                        org.bukkit.scoreboard.Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
                        org.bukkit.scoreboard.Team team = board.getTeam(player.getName());
                        if (team == null) {
                            team = board.registerNewTeam(player.getName());
                        }

                        String prefix = finalColor + "[" + finalTag + "] §f";

                        if (prefix.length() > 16) {
                            prefix = prefix.substring(0, 16);
                        }

                        team.setPrefix(prefix);
                        team.addEntry(player.getName());

                        try {
                            Class<?> chatSerializer = Class.forName("net.minecraft.server.v1_12_R1.IChatBaseComponent$ChatSerializer");
                            Object headerObj = chatSerializer.getMethod("a", String.class).invoke(null, "{\"text\": \"\\n§e§lSpeedbuilders Practice\\n§fMap by §bAndryFox_14\\n\"}");
                            Object footerObj = chatSerializer.getMethod("a", String.class).invoke(null, "{\"text\": \"\\n§7Usa §b/p §7per iniziare ad allenarti\\n\"}");

                            Object packet = Class.forName("net.minecraft.server.v1_12_R1.PacketPlayOutPlayerListHeaderFooter").newInstance();

                            java.lang.reflect.Field headerField = packet.getClass().getDeclaredField("a");
                            headerField.setAccessible(true);
                            headerField.set(packet, headerObj);

                            java.lang.reflect.Field footerField = packet.getClass().getDeclaredField("b");
                            footerField.setAccessible(true);
                            footerField.set(packet, footerObj);

                            Object handle = player.getClass().getMethod("getHandle").invoke(player);
                            Object connection = handle.getClass().getField("playerConnection").get(handle);
                            connection.getClass().getMethod("sendPacket", Class.forName("net.minecraft.server.v1_12_R1.Packet")).invoke(connection, packet);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }.runTaskAsynchronously(plugin);
    }

    @EventHandler
    public void onNPCInteract(org.bukkit.event.player.PlayerInteractEntityEvent event) {
        if (event.getRightClicked().getCustomName() != null && event.getRightClicked().getCustomName().equals("§c§lExit")) {
            event.setCancelled(true);
            event.getRightClicked().remove();
            event.getPlayer().sendMessage("§aNPC di prova eliminato!");
        }
    }

    @EventHandler
    public void onNPCDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        if (event.getEntity().getCustomName() != null && event.getEntity().getCustomName().equals("§c§lExit")) {
            event.setCancelled(true);
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
                            gm.forceReset(player);
                            // Recupera in modo dinamico la categoria in base all'ultima giocata
                            gm.loadBuild(player, buildId, gm.getCurrentCategory(player));
                            gm.startCountdown(player, 3);
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

        Material type = block.getType();
        if (type.name().contains("DOOR") && !type.name().contains("TRAP")) {
            event.setCancelled(true);

            byte data = block.getData();
            Block top = (data >= 8) ? block : block.getRelative(org.bukkit.block.BlockFace.UP);
            Block bottom = (data >= 8) ? block.getRelative(org.bukkit.block.BlockFace.DOWN) : block;

            if (top.getType() == type) top.setType(Material.AIR);
            if (bottom.getType() == type) bottom.setType(Material.AIR);

            Material dropMat = gm.getInventoryItemMaterial(type);
            player.getInventory().addItem(new ItemStack(dropMat, 1, (short)0));

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (gm.checkBuildPerfect(player)) gm.handlePerfect(player);
            });
            return;
        }

        Material dropMat = gm.getInventoryItemMaterial(block.getType());
        byte dropData = block.getData();
        boolean shouldDrop = true;

        if (dropMat == Material.LOG || dropMat == Material.LOG_2) {
            dropData = (byte) (dropData % 4);
        } else if (dropMat.name().contains("STEP") || dropMat.name().contains("SLAB")) {
            dropData = (byte) (dropData % 8);
        } else if (dropMat == Material.BED) {
            dropData = 0;
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
                plugin.getGameManager().openCategoryMenu(event.getPlayer());
            }
            else if (npcName.equalsIgnoreCase("Trova Errori")) {
                event.setCancelled(true);
                event.getPlayer().performCommand("p errors");
            }
            else if (npcName.equalsIgnoreCase("Guarda Build")) {
                event.setCancelled(true);
                event.getPlayer().performCommand("p view");
            }
            else if (npcName.equalsIgnoreCase("/leave")) {
                event.setCancelled(true);
                event.getPlayer().performCommand("p leave");
            }
        }
    }

    @EventHandler
    public void onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        String title = event.getView().getTitle();
        Player player = (Player) event.getWhoClicked();

        if (title.equals("§8Seleziona Server")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) return;

            String name = event.getCurrentItem().getItemMeta().getDisplayName();
            if (name.contains("feargames")) {
                plugin.getGameManager().openBuildMenu(player, 1, "FearGames");
            } else if (name.contains("mineplex")) {
                plugin.getGameManager().openBuildMenu(player, 1, "Mineplex");
            }
            return;
        }

        if (title.contains("- P. ")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
            if (!event.getCurrentItem().hasItemMeta() || !event.getCurrentItem().getItemMeta().hasDisplayName()) return;

            String name = event.getCurrentItem().getItemMeta().getDisplayName();
            GameManager gm = plugin.getGameManager();

            String category = title.contains("Mineplex") ? "Mineplex" : "FearGames";

            int currentPage = 1;
            try {
                String[] split = title.split("- P. ");
                if (split.length > 1) currentPage = Integer.parseInt(split[1].trim());
            } catch (Exception ignored) {}

            if (event.getCurrentItem().getType() == Material.NAME_TAG && name.equals("§e§lCerca Build")) {
                player.closeInventory();
                gm.setAwaitingSearch(player, true);
                player.sendMessage("§aScrivi in chat la parola da cercare!");
                return;
            }

            if (name.equals("§cPagina Precedente")) {
                gm.openBuildMenu(player, currentPage - 1, category);
                return;
            } else if (name.equals("§aPagina Successiva")) {
                gm.openBuildMenu(player, currentPage + 1, category);
                return;
            }

            List<String> lore = event.getCurrentItem().getItemMeta().getLore();
            if (lore != null && !lore.isEmpty()) {
                String rawId = org.bukkit.ChatColor.stripColor(lore.get(0)).replace("ID: ", "").trim();
                try {
                    int id = Integer.parseInt(rawId);
                    player.closeInventory();

                    gm.forceReset(player);
                    gm.loadBuild(player, id, category);
                    gm.startCountdown(player, 6);
                } catch (Exception ignored) {}
            }
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        GameManager gm = plugin.getGameManager();

        if (gm.isAwaitingSearch(player)) {
            event.setCancelled(true);
            gm.setAwaitingSearch(player, false);

            String msg = event.getMessage();

            if (msg.equalsIgnoreCase("annulla")) {
                player.sendMessage("§cRicerca annullata.");
                return;
            }

            gm.setActiveSearch(player, msg);
            Bukkit.getScheduler().runTask(plugin, () -> gm.openCategoryMenu(player));
        }
    }

    @EventHandler
    public void onDoubleJump(PlayerToggleFlightEvent event) {
        org.bukkit.entity.Player player = event.getPlayer();

        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) return;

        if (!plugin.getConfig().getBoolean("players." + player.getUniqueId() + ".dj", false)) return;

        if (player.getLocation().getX() < -30 || player.getLocation().getX() > 30) {
            player.sendMessage("§cSei troppo lontano dalla tua isola per usare il Double Jump. Usa /fly.");
            return;
        }

        event.setCancelled(true);
        player.setAllowFlight(false);
        player.setFlying(false);

        new BukkitRunnable() {
            @Override
            public void run() {
                player.setVelocity(new org.bukkit.util.Vector(0, 1.00, 0));
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_BLAZE_SHOOT, 1f, 1f);
            }
        }.runTask(plugin);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        org.bukkit.entity.Player player = event.getPlayer();
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) return;

        if (plugin.getConfig().getBoolean("players." + player.getUniqueId() + ".dj", false)) {
            if (player.getLocation().subtract(0, 0.1, 0).getBlock().getType().isSolid()) {
                player.setAllowFlight(true);
            }
        }
    }

}