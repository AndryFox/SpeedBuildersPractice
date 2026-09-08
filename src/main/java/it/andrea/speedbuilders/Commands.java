package it.andrea.speedbuilders;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Commands implements CommandExecutor {

    private final Main plugin;

    public Commands(Main plugin) {
        this.plugin = plugin;
    }

    private int getPing(Player player) {
        try {
            Object entityPlayer = player.getClass().getMethod("getHandle").invoke(player);
            return entityPlayer.getClass().getField("ping").getInt(entityPlayer);
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;
        GameManager gm = plugin.getGameManager();
        String cmdName = command.getName().toLowerCase();

        if (cmdName.equalsIgnoreCase("gmc")) {
            if (player.hasPermission("speedbuilders.admin") || player.isOp()) {
                player.setGameMode(org.bukkit.GameMode.CREATIVE);
                player.sendMessage("§aModalità Creativa attivata!");
            }
            return true;
        }
        if (cmdName.equalsIgnoreCase("gms")) {
            if (player.hasPermission("speedbuilders.admin") || player.isOp()) {
                player.setGameMode(org.bukkit.GameMode.SURVIVAL);
                player.sendMessage("§eModalità Sopravvivenza attivata!");
            }
            return true;
        }

        if (cmdName.equals("fly")) {
            if (player.isFlying()) {
                player.setAllowFlight(false);
                player.setFlying(false);
                player.sendMessage("§cVolo disattivato.");
            } else {
                player.setAllowFlight(true);
                player.setFlying(true);
                player.sendMessage("§aVolo attivato!");

                if (plugin.getConfig().getBoolean("players." + player.getUniqueId() + ".dj", false)) {
                    plugin.getConfig().set("players." + player.getUniqueId() + ".dj", false);
                    plugin.saveConfig();
                    player.sendMessage("§8§o(Double Jump disattivato in automatico)");
                }
            }

            if (player.getWorld().getName().equals("practice")) {
                int buildId = gm.getCurrentBuild(player);
                if (buildId != -1) {
                    gm.forceReset(player);
                    gm.loadBuild(player, buildId, gm.getCurrentCategory(player));
                    gm.readyBuild(player);
                }
            }
            return true;
        }

        if (cmdName.equals("dj")) {
            boolean djState = !plugin.getConfig().getBoolean("players." + player.getUniqueId() + ".dj", false);
            plugin.getConfig().set("players." + player.getUniqueId() + ".dj", djState);
            plugin.saveConfig();
            player.sendMessage("§eDouble Jump " + (djState ? "§aattivato" : "§cdisattivato") + "§e.");

            if (djState) {
                player.setAllowFlight(true);
                if (player.isFlying()) {
                    player.setFlying(false);
                    player.sendMessage("§8§o(Volo disattivato in automatico)");
                }
            } else {
                if (!player.isFlying()) {
                    player.setAllowFlight(false);
                }
            }

            if (player.getWorld().getName().equals("practice")) {
                int buildId = gm.getCurrentBuild(player);
                if (buildId != -1) {
                    gm.forceReset(player);
                    gm.loadBuild(player, buildId, gm.getCurrentCategory(player));
                    gm.readyBuild(player);
                }
            }
            return true;
        }

        if (cmdName.equals("tpworld")) {
            if (args.length == 0) {
                player.sendMessage("§cUsa: /tpworld <nome>");
                return true;
            }
            World targetWorld = Bukkit.getWorld(args[0]);
            if (targetWorld != null) {
                player.teleport(targetWorld.getSpawnLocation());
                player.sendMessage("§aTeletrasportato nel mondo: " + targetWorld.getName());
            } else {
                player.sendMessage("§cIl mondo '" + args[0] + "' non è caricato sul server!");
            }
            return true;
        }

        if (cmdName.equals("practice") || cmdName.equals("p")) {
            if (args.length > 0) {
                String sub = args[0].toLowerCase();
                if (sub.equals("list")) {
                    gm.clearSearch(player);
                    gm.openCategoryMenu(player);
                    return true;
                } else if (sub.equals("errors")) {
                    gm.showErrors(player);
                    return true;
                } else if (sub.equals("view")) {
                    gm.viewBuild(player);
                    return true;
                } else if (sub.equals("leave")) {
                    gm.resetPlayer(player);
                    player.performCommand("lobby");
                    return true;
                }
            }

            org.bukkit.World practiceWorld = Bukkit.getWorld("practice");
            if (practiceWorld == null) {
                player.sendMessage("§cErrore: Il mondo 'practice' non esiste.");
                return true;
            }

            gm.resetPlayer(player);
            gm.resetCustomFloor(player);
            gm.clearCurrentCategory(player);

            // MULTIPLAYER FIX: Calcola il teleport nel plot personale
            int plotId = plugin.getPlotManager().getPlot(player);
            Location centerLoc = plugin.getPlotManager().getPlotCenter(practiceWorld, plotId);
            int cX = centerLoc.getBlockX();
            int cZ = centerLoc.getBlockZ();

            // --- AUTO-GENERAZIONE DELL'ISOLA ---
            // Se al centro del pavimento (Y=100) non c'è niente, significa che l'isola non esiste!
            if (practiceWorld.getBlockAt(cX, 100, cZ).getType() == Material.AIR) {
                gm.setupIsland(player);
            } else {
                player.teleport(new Location(practiceWorld, cX + 0.5, 101, cZ + 7.5, 180f, 0f));
            }

            // Mettilo in Creativa invece che Survival
            player.setGameMode(org.bukkit.GameMode.CREATIVE);
            player.getInventory().clear();

            plugin.getConfig().set("players." + player.getUniqueId() + ".dj", true);
            plugin.saveConfig();
            player.setAllowFlight(true);
            player.setFlying(false);

            player.sendMessage("§aSei entrato nell'arena! Usa l'NPC per scegliere una mappa.");

            // AGGIORNA LA SCOREBOARD SUBITO ALL'INGRESSO
            plugin.getUIManager().updateScoreboard(player);

            return true;
        }

        if (cmdName.equals("lobby") || cmdName.equals("l") || cmdName.equals("leave")) {
            // Se si trova nell'arena, distrugge tutto
            if (player.getWorld().getName().equals("practice")) {
                plugin.getArenaManager().clearIsland(player);
                plugin.getGameManager().resetPlayer(player);
                plugin.getHologramManager().deleteArenaHologram(); // Rimuove l'ologramma!
            }

            // Teletrasporto
            if (plugin.getConfig().contains("locations.lobby")) {
                player.teleport((Location) plugin.getConfig().get("locations.lobby"));
                player.sendMessage("§aTeletrasportato alla Lobby!");
            } else {
                player.sendMessage("§cLa lobby non è stata impostata. Usa /map setlobby");
            }

            // Attiva la Scoreboard della Lobby
            plugin.getUIManager().updateLobbyScoreboard(player);
            return true;
        }

        if (cmdName.equals("category")) {
            if (!player.isOp() && !player.hasPermission("speedbuilders.admin")) {
                player.sendMessage("§cNon hai i permessi.");
                return true;
            }
            if (args.length == 0) {
                player.sendMessage("§cUsa: /category create <IP> <Nome Server>");
                player.sendMessage("§cUsa: /category seticon <Nome Server> §7(con l'oggetto in mano)");
                return true;
            }

            if (args[0].equalsIgnoreCase("create") && args.length >= 3) {
                String ip = args[1];
                StringBuilder nameBuilder = new StringBuilder();
                for (int i = 2; i < args.length; i++) nameBuilder.append(args[i]).append(" ");
                String name = nameBuilder.toString().trim();

                plugin.getConfig().set("custom_categories." + name + ".ip", ip);
                plugin.getConfig().set("custom_categories." + name + ".name", name);
                plugin.getConfig().set("custom_categories." + name + ".icon", "STAINED_CLAY;3");
                plugin.saveConfig();

                plugin.getGameManager().getBuildConfig(name);
                player.sendMessage("§aCategoria §l" + name + " §acreata! IP: §f" + ip);
            }
            else if (args[0].equalsIgnoreCase("seticon") && args.length >= 2) {
                StringBuilder nameBuilder = new StringBuilder();
                for (int i = 1; i < args.length; i++) nameBuilder.append(args[i]).append(" ");
                String name = nameBuilder.toString().trim();

                if (!name.equalsIgnoreCase("FearGames") && !name.equalsIgnoreCase("Mineplex") && !plugin.getConfig().contains("custom_categories." + name)) {
                    player.sendMessage("§cLa categoria '" + name + "' non esiste!");
                    return true;
                }

                @SuppressWarnings("deprecation")
                org.bukkit.inventory.ItemStack inHand = player.getItemInHand();
                if (inHand == null || inHand.getType() == org.bukkit.Material.AIR) {
                    player.sendMessage("§cDevi avere un blocco o oggetto in mano per impostare l'icona!");
                    return true;
                }

                String iconData = inHand.getType().name() + ";" + inHand.getDurability();
                plugin.getConfig().set("custom_categories." + name + ".icon", iconData);

                plugin.getConfig().set("custom_categories." + name + ".name", name);
                plugin.saveConfig();
                player.sendMessage("§aIcona di §l" + name + " §aaggiornata con successo!");
            } else {
                player.sendMessage("§cUsa: /category create <IP> <Nome> oppure /category seticon <Nome>");
            }
            return true;
        }

        // COMANDO PER INVITARE NEL PLOT
        if (cmdName.equals("add")) {
            if (args.length != 1) {
                player.sendMessage("§cUsa: /add <giocatore>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                player.sendMessage("§cGiocatore non trovato o offline.");
                return true;
            }
            if (target.equals(player)) {
                player.sendMessage("§cNon puoi invitare te stesso.");
                return true;
            }
            if (!player.getWorld().getName().equals("practice")) {
                player.sendMessage("§cDevi essere in un'arena per invitare qualcuno.");
                return true;
            }

            // Registra l'amico nel PlotManager
            plugin.getPlotManager().addGuest(player, target);
            player.sendMessage("§aHai invitato §e" + target.getName() + " §anel tuo plot!");
            target.sendMessage("§aSei stato invitato nel plot di §e" + player.getName() + "§a!");
            target.sendMessage("§7Fai §b/p §7per unirti a lui!");
            return true;
        }

        // SISTEMA AMICI COLLEGATO AL DATABASE SUPABASE
        if (cmdName.equals("friends") || cmdName.equals("f")) {
            if (args.length == 0) {
                player.sendMessage("§8§m--------------------------------");
                player.sendMessage("§6§lLista Amici");
                player.sendMessage("§e/f add <nome> §7- Aggiungi un amico");
                player.sendMessage("§e/f remove <nome> §7- Rimuovi un amico");
                player.sendMessage("§e/f list §7- Lista amici");
                player.sendMessage("§8§m--------------------------------");
                return true;
            }

            String sub = args[0].toLowerCase();

            if (sub.equals("add") && args.length == 2) {
                String targetName = args[1];
                // Esecuzione asincrona per non bloccare il server durante la query al DB
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    boolean success = plugin.getDatabase().addFriend(player.getUniqueId(), targetName);
                    if (success) player.sendMessage("§aHai aggiunto §e" + targetName + " §aai tuoi amici!");
                    else player.sendMessage("§cImpossibile aggiungere. L'utente esiste già o non è stato trovato.");
                });
            } else if (sub.equals("remove") && args.length == 2) {
                String targetName = args[1];
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    boolean success = plugin.getDatabase().removeFriend(player.getUniqueId(), targetName);
                    if (success) player.sendMessage("§cHai rimosso §e" + targetName + " §cdagli amici.");
                    else player.sendMessage("§cAmico non trovato nella tua lista.");
                });
            } else if (sub.equals("list")) {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    java.util.List<String> friends = plugin.getDatabase().getFriends(player.getUniqueId());
                    player.sendMessage("§8§m--------------------------------");
                    player.sendMessage("§6§lI tuoi Amici (" + friends.size() + ")");
                    for (String f : friends) {
                        player.sendMessage("§8- §a" + f);
                    }
                    player.sendMessage("§8§m--------------------------------");
                });
            } else {
                player.sendMessage("§cUsa /f per vedere i comandi.");
            }
            return true;
        }

        if (cmdName.equals("ping")) {
            if (args.length == 0) {
                player.sendMessage("§8[§bPractice§8] §7Il tuo ping: §9" + getPing(player) + "ms");
            } else {
                Player target = Bukkit.getPlayer(args[0]);
                if (target != null) {
                    player.sendMessage("§8[§bPractice§8] §7Ping di §e" + target.getName() + "§7: §9" + getPing(target) + "ms");
                } else {
                    player.sendMessage("§cGiocatore non trovato.");
                }
            }
            return true;
        }

        if (cmdName.equals("map")) {
            if (args.length == 0) {
                player.sendMessage("§8§m--------------------------------");
                player.sendMessage("§6§lGestione Mappe - SpeedBuilders");
                player.sendMessage("§e/map setup §7- Genera l'arena base.");
                player.sendMessage("§e/map save <Categoria> <Nome> §7- Salva una nuova build.");
                player.sendMessage("§e/map update <Categoria> <id> §7- Aggiorna i blocchi.");
                player.sendMessage("§e/map rename <Categoria> <id> <Nome> §7- Rinomina build.");
                player.sendMessage("§e/map delete <Categoria> <id> §7- Elimina una build.");
                player.sendMessage("§e/map load <id> <Categoria> §7- Carica una build.");
                player.sendMessage("§8§m--------------------------------");
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "setlobby":
                    plugin.getConfig().set("locations.lobby", player.getLocation());
                    plugin.saveConfig();
                    player.sendMessage("§aPunto di spawn della §l/lobby§a impostato qui!");
                    break;
                case "setup":
                    gm.setupIsland(player);
                    break;
                case "save":
                    if (args.length < 3) { player.sendMessage("§cUsa: /map save <Categoria> <Nome>"); break; }
                    String catSave = args[1];
                    StringBuilder nameBuilder = new StringBuilder();
                    for (int i = 2; i < args.length; i++) nameBuilder.append(args[i]).append(" ");
                    int nextId = 1;
                    org.bukkit.configuration.file.FileConfiguration cfgSave = gm.getBuildConfig(catSave);
                    if (cfgSave.contains("builds")) {
                        for (String key : cfgSave.getConfigurationSection("builds").getKeys(false)) {
                            try { if (Integer.parseInt(key) >= nextId) nextId = Integer.parseInt(key) + 1; } catch (Exception ignored) {}
                        }
                    }
                    gm.saveBuild(player, nextId, nameBuilder.toString().trim(), catSave);
                    break;
                case "rename":
                    if (args.length < 4) { player.sendMessage("§cUsa: /map rename <Categoria> <id> <Nome>"); break; }
                    try {
                        String catRen = args[1];
                        int id = Integer.parseInt(args[2]);
                        StringBuilder renameBuilder = new StringBuilder();
                        for (int i = 3; i < args.length; i++) renameBuilder.append(args[i]).append(" ");
                        String newName = renameBuilder.toString().trim();

                        org.bukkit.configuration.file.FileConfiguration cfgRen = gm.getBuildConfig(catRen);
                        if (cfgRen.contains("builds." + id)) {
                            cfgRen.set("builds." + id + ".name", newName);
                            cfgRen.save(new java.io.File(plugin.getDataFolder(), catRen.toLowerCase() + "_builds.yml"));
                            player.sendMessage("§aNome cambiato in '" + newName + "'!");
                        } else {
                            player.sendMessage("§cID non trovato in " + catRen + ".");
                        }
                    } catch (Exception e) { player.sendMessage("§cL'ID deve essere un numero!"); }
                    break;
                case "update":
                    if (args.length < 3) { player.sendMessage("§cUsa: /map update <Categoria> <id>"); break; }
                    try {
                        String catUp = args[1];
                        int id = Integer.parseInt(args[2]);
                        org.bukkit.configuration.file.FileConfiguration cfgUp = gm.getBuildConfig(catUp);
                        if (cfgUp.contains("builds." + id)) {
                            gm.saveBuild(player, id, cfgUp.getString("builds." + id + ".name", "Sconosciuta"), catUp);
                            player.sendMessage("§eBlocchi aggiornati!");
                        } else {
                            player.sendMessage("§cID non trovato in " + catUp + ".");
                        }
                    } catch (Exception e) { player.sendMessage("§cL'ID deve essere un numero!"); }
                    break;
                case "delete":
                    if (args.length < 3) { player.sendMessage("§cUsa: /map delete <Categoria> <id>"); break; }
                    try {
                        String catDel = args[1];
                        int id = Integer.parseInt(args[2]);
                        org.bukkit.configuration.file.FileConfiguration cfgDel = gm.getBuildConfig(catDel);
                        if (!cfgDel.contains("builds." + id)) {
                            player.sendMessage("§cLa build con ID " + id + " non esiste in " + catDel + ".");
                            break;
                        }
                        cfgDel.set("builds." + id, null);
                        cfgDel.save(new java.io.File(plugin.getDataFolder(), catDel.toLowerCase() + "_builds.yml"));
                        player.sendMessage("§aBuild eliminata definitivamente da " + catDel + "!");
                    } catch (Exception e) { player.sendMessage("§cID non valido."); }
                    break;
                case "load":
                    if (args.length < 3) { player.sendMessage("§cUsa: /map load <id> <Categoria>"); break; }
                    try { gm.loadBuild(player, Integer.parseInt(args[1]), args[2]); } catch (Exception e) { player.sendMessage("§cID invalido!"); }
                    break;
                case "setholo":
                    plugin.getConfig().set("locations.hologram", player.getLocation().add(0, 2, 0));
                    plugin.saveConfig();
                    plugin.getHologramManager().spawnOrUpdate();
                    player.sendMessage("§aOlogramma della Top 10 posizionato in aria!");
                    break;
                case "setexit":
                    if (!player.isOp()) return true;
                    org.bukkit.entity.Villager npc = (org.bukkit.entity.Villager) player.getWorld().spawnEntity(player.getLocation(), org.bukkit.entity.EntityType.VILLAGER);
                    npc.setCustomName("§c§lExit");
                    npc.setCustomNameVisible(true);
                    npc.setAI(false);
                    npc.setInvulnerable(true);
                    npc.setCollidable(false);
                    player.sendMessage("§aNPC Exit creato alla tua posizione!");
                    break;
                case "resetfloor":
                    if (!player.isOp()) return true;
                    plugin.getGameManager().resetCustomFloor(player);
                    player.sendMessage("§aPavimento ripristinato a quello di default!");
                    break;
                case "create":
                    if (args.length < 3) { player.sendMessage("§cUsa: /map create <Categoria> <Nome>"); break; }
                    String targetCat = args[1];
                    StringBuilder sb = new StringBuilder();
                    for (int i = 2; i < args.length; i++) sb.append(args[i]).append(" ");
                    String buildName = sb.toString().trim();

                    int reviewId = 1;
                    org.bukkit.configuration.file.FileConfiguration reviewCfg = gm.getBuildConfig("Review");
                    if (reviewCfg.contains("builds")) {
                        for (String key : reviewCfg.getConfigurationSection("builds").getKeys(false)) {
                            try { if (Integer.parseInt(key) >= reviewId) reviewId = Integer.parseInt(key) + 1; } catch (Exception ignored) {}
                        }
                    }
                    gm.saveBuild(player, reviewId, buildName + " [" + targetCat + "]", "Review");
                    player.sendMessage("§aBuild '" + buildName + "' inviata con successo agli Admin per l'approvazione!");
                    break;
                default:
                    player.sendMessage("§cUsa: /map per vedere la lista dei comandi");
            }
        }
        return true;
    }
}