package it.andrea.speedbuilders;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class Commands implements CommandExecutor {

    private final Main plugin;

    public Commands(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;
        GameManager gm = plugin.getGameManager();
        String cmdName = command.getName().toLowerCase();

        if (cmdName.equals("fly")) {
            if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
                player.sendMessage("§cSei in Creativa, il volo è già forzato dal gioco!");
                return true;
            }

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
            return true;
        }

        if (cmdName.equals("dj")) {
            if (plugin.getGameManager().getState(player).equals("PLAYING")) {
                player.sendMessage("§cNon puoi usare il Double Jump mentre giochi!");
                return true;
            }

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

            World practiceWorld = Bukkit.getWorld("practice");
            if (practiceWorld == null) {
                player.sendMessage("§cErrore: Il mondo 'practice' non esiste.");
                return true;
            }
            gm.resetPlayer(player);
            player.teleport(new Location(practiceWorld, 0.5, 101, 5.5, 180f, 35f));
            if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
                player.setAllowFlight(true);
                player.setFlying(false);
            }
            player.sendMessage("§aArea pulita! Clicca sul bordo in quarzo per ricominciare.");
            return true;
        }

        if (cmdName.equals("lobby")) {
            if (plugin.getConfig().contains("locations.lobby")) {
                player.teleport((Location) plugin.getConfig().get("locations.lobby"));
                player.sendMessage("§aTeletrasportato alla Lobby!");
            } else {
                player.sendMessage("§cLa lobby non è stata impostata. Usa /map setlobby");
            }
            return true;
        }

        if (cmdName.equals("map")) {
            if (args.length == 0) {
                player.sendMessage("§8§m--------------------------------");
                player.sendMessage("§6§lGestione Mappe - SpeedBuilders");
                player.sendMessage("§e/map setup §7- Genera l'arena base.");
                player.sendMessage("§e/map create <nome> §7- Salva i blocchi che hai piazzato.");
                player.sendMessage("§e/map load <id> <categoria> §7- Carica una build.");
                player.sendMessage("§e/map list §7- Mostra tutte le build salvate.");
                player.sendMessage("§e/map delete <id> §7- Elimina una build.");
                player.sendMessage("§e/map setfloornpc §7- Genera l'NPC per lo stile pavimento.");
                player.sendMessage("§8§m--------------------------------");
                return true;
            }

            if (args[0].equalsIgnoreCase("delete") && args.length == 2) {
                int buildId;
                try { buildId = Integer.parseInt(args[1]); } catch (Exception e) {
                    player.sendMessage("§cID non valido."); return true;
                }

                if (!plugin.getFearConfig().contains("builds." + buildId) && !plugin.getMineplexConfig().contains("builds." + buildId)) {
                    player.sendMessage("§cLa build con ID " + buildId + " non esiste.");
                    return true;
                }

                plugin.getGameManager().setPendingDelete(player, buildId);

                TextComponent warning = new TextComponent("§cStai per eliminare la build ID " + buildId + ". ");
                TextComponent clickBtn = new TextComponent("§4§l[CLICCA QUI PER CONFERMARE]");
                clickBtn.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/map confirm"));
                clickBtn.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§7Clicca per eliminare definitivamente").create()));

                warning.addExtra(clickBtn);
                player.spigot().sendMessage(warning);
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
                case "confirm":
                    if (gm.hasPendingDelete(player)) {
                        int toDelete = gm.getPendingDelete(player);

                        if(plugin.getFearConfig().contains("builds." + toDelete)) {
                            plugin.getFearConfig().set("builds." + toDelete, null);
                            try { plugin.getFearConfig().save(new java.io.File(plugin.getDataFolder(), "feargames_builds.yml")); } catch (Exception ignored){}
                        }
                        if(plugin.getMineplexConfig().contains("builds." + toDelete)) {
                            plugin.getMineplexConfig().set("builds." + toDelete, null);
                            try { plugin.getMineplexConfig().save(new java.io.File(plugin.getDataFolder(), "mineplex_builds.yml")); } catch (Exception ignored){}
                        }

                        player.sendMessage("§aBuild ID '" + toDelete + "' eliminata.");
                        gm.removePendingDelete(player);
                    } else player.sendMessage("§cNessuna eliminazione in sospeso.");
                    break;
                case "save":
                    if (args.length < 2) { player.sendMessage("§cUsa: /map save <Nome>"); break; }
                    StringBuilder nameBuilder = new StringBuilder();
                    for (int i = 1; i < args.length; i++) nameBuilder.append(args[i]).append(" ");
                    int nextId = 1;
                    if (plugin.getFearConfig().contains("builds")) {
                        for (String key : plugin.getFearConfig().getConfigurationSection("builds").getKeys(false)) {
                            try { if (Integer.parseInt(key) >= nextId) nextId = Integer.parseInt(key) + 1; } catch (Exception ignored) {}
                        }
                    }
                    gm.saveBuild(player, nextId, nameBuilder.toString().trim());
                    break;
                case "rename":
                    if (args.length < 3) { player.sendMessage("§cUsa: /map rename <id> <Nome>"); break; }
                    try {
                        int id = Integer.parseInt(args[1]);
                        String newName = "";
                        StringBuilder renameBuilder = new StringBuilder();
                        for (int i = 2; i < args.length; i++) renameBuilder.append(args[i]).append(" ");
                        newName = renameBuilder.toString().trim();

                        if (plugin.getFearConfig().contains("builds." + id)) {
                            plugin.getFearConfig().set("builds." + id + ".name", newName);
                            try { plugin.getFearConfig().save(new java.io.File(plugin.getDataFolder(), "feargames_builds.yml")); } catch (Exception ignored){}
                            player.sendMessage("§aNome cambiato in '" + newName + "'!");
                        } else if (plugin.getMineplexConfig().contains("builds." + id)) {
                            plugin.getMineplexConfig().set("builds." + id + ".name", newName);
                            try { plugin.getMineplexConfig().save(new java.io.File(plugin.getDataFolder(), "mineplex_builds.yml")); } catch (Exception ignored){}
                            player.sendMessage("§aNome cambiato in '" + newName + "'!");
                        } else {
                            player.sendMessage("§cID non trovato.");
                        }
                    } catch (Exception e) { player.sendMessage("§cL'ID deve essere un numero!"); }
                    break;
                case "update":
                    if (args.length < 2) { player.sendMessage("§cUsa: /map update <id>"); break; }
                    try {
                        int id = Integer.parseInt(args[1]);
                        if (plugin.getFearConfig().contains("builds." + id)) {
                            gm.saveBuild(player, id, plugin.getFearConfig().getString("builds." + id + ".name", "Sconosciuta"));
                            player.sendMessage("§eBlocchi aggiornati!");
                        } else {
                            player.sendMessage("§cID non trovato in FearGames (Supportato solo per build salvate manualmente).");
                        }
                    } catch (Exception e) { player.sendMessage("§cL'ID deve essere un numero!"); }
                    break;
                case "load":
                    if (args.length < 3) { player.sendMessage("§cUsa: /map load <id> <FearGames|Mineplex>"); break; }
                    try { gm.loadBuild(player, Integer.parseInt(args[1]), args[2]); } catch (Exception e) { player.sendMessage("§cID invalido!"); }
                    break;
                case "setholo":
                    plugin.getConfig().set("locations.hologram", player.getLocation().add(0, 2, 0));
                    plugin.saveConfig();
                    plugin.getHologramManager().spawnOrUpdate();
                    player.sendMessage("§aOlogramma della Top 10 posizionato in aria!");
                    break;
                case "setfloornpc":
                    if (!player.isOp()) return true;
                    org.bukkit.entity.Villager npcFloor = (org.bukkit.entity.Villager) player.getWorld().spawnEntity(player.getLocation(), org.bukkit.entity.EntityType.VILLAGER);
                    npcFloor.setCustomName("§e§lBuild Floor");
                    npcFloor.setCustomNameVisible(true);
                    npcFloor.setAI(false);
                    npcFloor.setInvulnerable(true);
                    npcFloor.setCollidable(false);
                    player.sendMessage("§aNPC stile pavimento creato alla tua posizione!");
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
                default:
                    player.sendMessage("§cUsa: /map <save|load|update|rename|delete|setup|setlobby>");
            }
        }
        return true;
    }
}