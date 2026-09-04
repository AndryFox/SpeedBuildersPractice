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
                        if (prefix.length() > 16) { prefix = prefix.substring(0, 16); }

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
    public void onNPCInteract(PlayerInteractEntityEvent event) {
        if (event.getRightClicked().hasMetadata("NPC")) return; // Ignora se è di Citizens

        if (event.getRightClicked().getCustomName() != null) {
            String npcName = event.getRightClicked().getCustomName();
            if (npcName.equals("§c§lExit")) {
                event.setCancelled(true);
                event.getRightClicked().remove();
                event.getPlayer().sendMessage("§aNPC Exit eliminato!");
            }
        }
    }

    @EventHandler
    public void onNPCDamage(EntityDamageEvent event) {
        if (event.getEntity().getCustomName() != null) {
            if (event.getEntity().getCustomName().equals("§c§lExit")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onArenaFloorClick(PlayerInteractEvent event) {
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            Block clicked = event.getClickedBlock();
            Player player = event.getPlayer();

            if (clicked != null && clicked.getType() == Material.QUARTZ_BLOCK) {
                int x = clicked.getX(), y = clicked.getY(), z = clicked.getZ();

                // Controlla che il blocco si trovi nell'area globale dell'isola
                if (y == 100 && x >= -4 && x <= 4 && z >= -4 && z <= 4) {

                    // Controlla se il blocco si trova all'interno del 7x7 dove c'è il pavimento della build
                    boolean isInnerPlot = (x >= -3 && x <= 3 && z >= -3 && z <= 3);

                    // Fa partire il timer SOLO se il quarzo cliccato NON fa parte dell'area interna
                    if (!isInnerPlot) {
                        if (player.getWorld().getName().equals("practice")) {
                            event.setCancelled(true);
                            GameManager gm = plugin.getGameManager();
                            int buildId = gm.getCurrentBuild(player);
                            if (buildId != -1) {
                                gm.forceReset(player);
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
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) return;

        GameManager gm = plugin.getGameManager();
        if (gm.isLobbyWorld(player.getWorld())) { event.setCancelled(true); return; }
        if (!event.getBlock().getWorld().getName().equals("practice")) return;

        String state = gm.getState(player);
        if (!state.equals("PLAYING")) {
            event.setCancelled(true);
            player.sendMessage(state.equals("COUNTDOWN") ? "§cAttendi la fine del countdown!" : "§cClicca sul pavimento nero per iniziare!");
            return;
        }

        // Il controllo dei bordi lo fa già BlockFixes. Qui controlliamo solo la vittoria.
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (gm.checkBuildPerfect(player)) gm.handlePerfect(player);
        });
    }

    @EventHandler
    public void onBlockDamage(BlockDamageEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) return;
        Block block = event.getBlock();
        if (!block.getWorld().getName().equals("practice")) return;

        if (block.getX() >= -3 && block.getX() <= 3 && block.getZ() >= -3 && block.getZ() <= 3 && block.getY() > 100) {
            if (plugin.getGameManager().getState(player).equals("PLAYING")) {
                event.setInstaBreak(true);
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        // Rende i mob delle build totalmente invincibili a qualsiasi danno
        if (event.getEntity().hasMetadata("SpeedBuildersMob")) {
            event.setCancelled(true);
            return;
        }

        if (event.getEntity().getWorld().getName().equals("practice")) { event.setCancelled(true); return; }
        if (event.getEntity() instanceof Player && plugin.getGameManager().isLobbyWorld(event.getEntity().getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onMobInteract(PlayerInteractEntityEvent event) {
        // Impedisce l'interazione (es. far nascere baby mob con l'uovo)
        if (event.getRightClicked().hasMetadata("SpeedBuildersMob")) {
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

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onCitizensNpcClick(PlayerInteractEntityEvent event) {
        if (event.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND) return;

        org.bukkit.entity.Entity clicked = event.getRightClicked();

        if (clicked.hasMetadata("NPC")) {

            // 1. Forza lo sblocco dell'evento se WorldGuard o altri plugin lo avevano negato ai Non-OP
            if (event.isCancelled()) {
                event.setCancelled(false);
            }

            // 2. Legge il nome in modo infallibile (sia Custom Name che Name base)
            String npcName = clicked.getCustomName();
            if (npcName == null) npcName = clicked.getName();
            if (npcName == null) return;

            npcName = org.bukkit.ChatColor.stripColor(npcName).toLowerCase();
            org.bukkit.entity.Player player = event.getPlayer();

            // 3. Esegue l'apertura delle GUI con 1 Tick di ritardo
            // Questo impedisce al client di chiudere istantaneamente il menu se l'evento originale era stato bloccato.
            if (npcName.contains("andryfox")) {
                event.setCancelled(true);
                Bukkit.getScheduler().runTaskLater(plugin, () -> player.performCommand("p"), 1L);
            } else if (npcName.contains("lista build")) {
                event.setCancelled(true);
                Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.getGameManager().openCategoryMenu(player), 1L);
            } else if (npcName.contains("trova errori")) {
                event.setCancelled(true);
                Bukkit.getScheduler().runTaskLater(plugin, () -> player.performCommand("p errors"), 1L);
            } else if (npcName.contains("guarda build")) {
                event.setCancelled(true);
                Bukkit.getScheduler().runTaskLater(plugin, () -> player.performCommand("p view"), 1L);
            } else if (npcName.contains("/leave")) {
                event.setCancelled(true);
                Bukkit.getScheduler().runTaskLater(plugin, () -> player.performCommand("p leave"), 1L);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        String title = event.getView().getTitle();
        Player player = (Player) event.getWhoClicked();

        // 1. MENU: SELEZIONA SERVER
        if (title.equals("§8Seleziona Server")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) return;

            String rawName = org.bukkit.ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());

            // Se clicca una qualsiasi categoria (Base, Custom speciale o Custom creata dal config)
            if (rawName.equals("FearGames") || rawName.equals("Mineplex") || rawName.equals("Custom") || plugin.getConfig().contains("custom_categories." + rawName)) {
                plugin.getGameManager().openBuildMenu(player, 1, rawName);
            }
        }

        // 2. MENU: SCEGLI LA MODALITÀ (Nuovo Menu di Ingresso)
        if (title.equals("§8Scegli la Modalità")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) return;

            String itemName = org.bukkit.ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());

            org.bukkit.World practiceWorld = Bukkit.getWorld("practice");
            Location arenaLoc = new Location(practiceWorld, 0.5, 101, 5.5, 180f, 35f);

            // Chiude l'inventario prima del teletrasporto per evitare bug visivi
            player.closeInventory();

            if (itemName.contains("Costruttore")) {
                if (practiceWorld != null) player.teleport(arenaLoc);
                player.getInventory().clear();

                // Ritardo di 2 tick per evitare che i plugin dei mondi forzino la Survival ai non-OP
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.setGameMode(org.bukkit.GameMode.CREATIVE);
                    player.sendMessage("§aSei entrato nell'arena in modalità Costruttore!");
                }, 2L);
            }
            else if (itemName.contains("Giocatore")) {
                if (practiceWorld != null) player.teleport(arenaLoc);
                player.getInventory().clear();

                // Ritardo di 2 tick anche qui per sicurezza
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.setGameMode(org.bukkit.GameMode.SURVIVAL);
                    player.setAllowFlight(true);
                    player.setFlying(false);
                    player.sendMessage("§eSei entrato nell'arena in modalità Giocatore! Clicca sul bordo in quarzo per ricominciare.");
                }, 2L);
            }
            return;
        }

        // 3. MENU: LISTA DELLE MAPPE (- P. )
        if (title.contains("- P. ")) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
            if (!event.getCurrentItem().hasItemMeta() || !event.getCurrentItem().getItemMeta().hasDisplayName()) return;

            String name = event.getCurrentItem().getItemMeta().getDisplayName();
            GameManager gm = plugin.getGameManager();

            String titleStripped = org.bukkit.ChatColor.stripColor(title);
            String category = "FearGames"; // Fallback di sicurezza
            String leftPart = titleStripped.split(" - P. ")[0];
            if (leftPart.startsWith("Cerca (") && leftPart.endsWith(")")) {
                category = leftPart.substring(7, leftPart.length() - 1); // Rimuove "Cerca (" e ")"
            } else {
                category = leftPart;
            }

            int currentPage = 1;
            try {
                String[] split = title.split("- P. ");
                if (split.length > 1) currentPage = Integer.parseInt(split[1].trim());
            } catch (Exception ignored) {}

            // Gestione del Tasto Indietro
            if (name.equals("§c§lTorna ai Server")) {
                player.closeInventory();
                gm.openCategoryMenu(player);
                return;
            }

            // Gestione del Tasto Cerca (Reset con tasto destro)
            if (event.getCurrentItem().getType() == Material.NAME_TAG && name.equals("§e§lCerca Build")) {
                if (event.isRightClick() && gm.hasActiveSearch(player)) {
                    gm.clearSearch(player);
                    gm.openBuildMenu(player, currentPage, category);
                } else {
                    player.closeInventory();
                    gm.setAwaitingSearch(player, true);
                    player.sendMessage("§aScrivi in chat la parola da cercare! (Scrivi 'annulla' per uscire)");
                }
                return;
            }

            // Gestione del Tasto Random
            if (event.getCurrentItem().getType() == Material.ENDER_PEARL && name.equals("§d§lBuild Casuale")) {
                int randomId = gm.getRandomBuildId(category);
                if (randomId == -1) {
                    player.sendMessage("§cNessuna build trovata in questa categoria!");
                    return;
                }

                player.closeInventory();

                if (event.isLeftClick()) {
                    gm.setContinuousRandom(player, true);
                    player.sendMessage("§aModalità Random Continua attivata!");
                } else {
                    gm.setContinuousRandom(player, false);
                    player.sendMessage("§eModalità Random Singola attivata!");
                }

                gm.forceReset(player);
                gm.loadBuild(player, randomId, category);
                gm.startCountdown(player, 6);
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
            if (lore != null && !lore.isEmpty() && lore.get(0).contains("ID: ")) {
                String rawId = org.bukkit.ChatColor.stripColor(lore.get(0)).replace("ID: ", "").trim();
                try {
                    int id = Integer.parseInt(rawId);
                    player.closeInventory();

                    gm.setContinuousRandom(player, false);

                    gm.forceReset(player);
                    gm.loadBuild(player, id, category);
                    gm.startCountdown(player, 6);
                } catch (Exception ignored) {}
            }
        }
    }

    @EventHandler
    public void onDoubleJump(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();

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
                player.setVelocity(new Vector(0, 1.00, 0));
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_BLAZE_SHOOT, 1f, 1f);
            }
        }.runTask(plugin);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) return;

        if (plugin.getConfig().getBoolean("players." + player.getUniqueId() + ".dj", false)) {
            if (player.getLocation().subtract(0, 0.1, 0).getBlock().getType().isSolid()) {
                player.setAllowFlight(true);
            }
        }
    }

    // 1. Permette di creare i cartelli speciali scrivendo semplicemente "Floor" o "Building" nella prima riga
    @EventHandler
    public void onSignChange(org.bukkit.event.block.SignChangeEvent event) {
        org.bukkit.entity.Player player = event.getPlayer();
        String line0 = event.getLine(0);

        if (line0 == null) return;

        if (line0.equalsIgnoreCase("Floor") || line0.equalsIgnoreCase("[Floor]")) {
            if (player.hasPermission("speedbuilders.admin") || player.isOp()) {
                event.setLine(0, "§8[§bSpeedBuilders§8]");
                event.setLine(1, "§9§lFloor");
                event.setLine(2, "§7Clicca per");
                event.setLine(3, "§7salvare");
                player.sendMessage("§aCartello Floor creato con successo!");
            }
        }
        else if (line0.equalsIgnoreCase("Building") || line0.equalsIgnoreCase("[Building]")) {
            if (player.hasPermission("speedbuilders.admin") || player.isOp()) {
                event.setLine(0, "§8[§bSpeedBuilders§8]");
                event.setLine(1, "§e§lBuilding");
                event.setLine(2, "§7Clicca per");
                event.setLine(3, "§7testare");
                player.sendMessage("§aCartello Building creato con successo!");
            }
        }
    }

    // 2. Legge il Click destro sui cartelli ed esegue le azioni di salvataggio/test
    @EventHandler
    public void onSignClick(org.bukkit.event.player.PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;

        org.bukkit.block.Block b = event.getClickedBlock();
        if (b == null) return;

        if (b.getType() == org.bukkit.Material.SIGN_POST || b.getType() == org.bukkit.Material.WALL_SIGN) {
            org.bukkit.block.Sign sign = (org.bukkit.block.Sign) b.getState();
            String line1 = org.bukkit.ChatColor.stripColor(sign.getLine(1)).trim();

            org.bukkit.entity.Player player = event.getPlayer();

            if (line1.equalsIgnoreCase("Floor")) {
                event.setCancelled(true);

                // Salva il floor senza rimuovere o resettare i blocchi, li copia e basta
                plugin.getGameManager().saveAndApplyCustomFloor(player);
                player.sendMessage("§a§l[!] §aPavimento custom salvato! §7I blocchi sono rimasti al loro posto.");
            }
            else if (line1.equalsIgnoreCase("Building")) {
                event.setCancelled(true);

                // 1. Salva la build solo nella memoria RAM usando la categoria fittizia "TempTest"
                plugin.getGameManager().saveBuild(player, 1, "Test in RAM", "TempTest");

                // 2. Resetta l'area visiva (rimuove i blocchi dalla visuale)
                plugin.getGameManager().forceReset(player);

                // 3. Mette in survival e carica la build dalla memoria
                player.setGameMode(org.bukkit.GameMode.SURVIVAL);
                plugin.getGameManager().loadBuild(player, 1, "TempTest");

                player.sendMessage("§e§l[!] §eInizio del test... (Nessun file modificato)");
            }
        }
    }

}