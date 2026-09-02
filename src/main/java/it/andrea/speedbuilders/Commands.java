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

        // NUOVO COMANDO: /fly
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
                // Attiva il volo
                player.setAllowFlight(true);
                player.setFlying(true);
                player.sendMessage("§aVolo attivato!");

                // Spegne il Double Jump in automatico per evitare conflitti
                if (plugin.getBuildsConfig().getBoolean("players." + player.getUniqueId() + ".dj", false)) {
                    plugin.getBuildsConfig().set("players." + player.getUniqueId() + ".dj", false);
                    plugin.saveConfig();
                    player.sendMessage("§8§o(Double Jump disattivato in automatico)");
                }
            }
            return true;
        }

        // NUOVO COMANDO: /dj (Attiva/Disattiva il Double Jump)
        if (cmdName.equals("dj")) {
            if (plugin.getGameManager().getState(player).equals("PLAYING")) {
                player.sendMessage("§cNon puoi usare il Double Jump mentre giochi!");
                return true;
            }

            boolean djState = !plugin.getBuildsConfig().getBoolean("players." + player.getUniqueId() + ".dj", false);
            plugin.getBuildsConfig().set("players." + player.getUniqueId() + ".dj", djState);
            plugin.saveConfig();
            player.sendMessage("§eDouble Jump " + (djState ? "§aattivato" : "§cdisattivato") + "§e.");

            if (djState) {
                player.setAllowFlight(true);
                // Spegne il Volo in automatico se si accende il Double Jump
                if (player.isFlying()) {
                    player.setFlying(false);
                    player.sendMessage("§8§o(Volo disattivato in automatico)");
                }
            } else {
                // Se disattivi il DJ e non stai volando, toglie l'allowFlight
                if (!player.isFlying()) {
                    player.setAllowFlight(false);
                }
            }
            return true;
        }

        if (cmdName.equals("practice") || cmdName.equals("p")) {

            if (args.length > 0) {
                String sub = args[0].toLowerCase();
                if (sub.equals("list")) {
                    gm.clearSearch(player);
                    gm.openCategoryMenu(player); // Apre la selezione invece del menu diretto
                    return true;
                } else if (sub.equals("errors")) {
                    gm.showErrors(player);
                    return true;
                } else if (sub.equals("view")) {
                    gm.viewBuild(player);
                    return true;
                } else if (sub.equals("leave")) { // <-- ECCO LA PARTE DA AGGIUNGERE
                    // Resetta il giocatore e lo manda alla lobby
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
            if (plugin.getBuildsConfig().contains("locations.lobby")) {
                player.teleport((Location) plugin.getBuildsConfig().get("locations.lobby"));
                player.sendMessage("§aTeletrasportato alla Lobby!");
            } else {
                player.sendMessage("§cLa lobby non è stata impostata. Usa /map setlobby");
            }
            return true;
        }

        // AGGIORNAMENTO: /map (Menu di aiuto migliorato e testo cliccabile)
        if (cmdName.equals("map")) {
            if (args.length == 0) {
                player.sendMessage("§8§m--------------------------------");
                player.sendMessage("§6§lGestione Mappe - SpeedBuilders");
                player.sendMessage("§e/map setup §7- Genera l'arena base.");
                player.sendMessage("§e/map create <nome> §7- Salva i blocchi che hai piazzato in una nuova build.");
                player.sendMessage("§e/map load <id> §7- Carica una build salvata nell'arena per testarla.");
                player.sendMessage("§e/map list §7- Mostra l'ID e il nome di tutte le build salvate.");
                player.sendMessage("§e/map delete <id> §7- Elimina una build dal database.");
                player.sendMessage("§8§m--------------------------------");
                return true;
            }

            if (args[0].equalsIgnoreCase("delete") && args.length == 2) {
                int buildId;
                try { buildId = Integer.parseInt(args[1]); } catch (Exception e) {
                    player.sendMessage("§cID non valido."); return true;
                }

                if (!plugin.getBuildsConfig().contains("builds." + buildId)) {
                    player.sendMessage("§cLa build con ID " + buildId + " non esiste.");
                    return true;
                }

                plugin.getGameManager().setPendingDelete(player, buildId);

                // Generazione del testo cliccabile
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
                    plugin.getBuildsConfig().set("locations.lobby", player.getLocation());
                    plugin.saveConfig();
                    player.sendMessage("§aPunto di spawn della §l/lobby§a impostato qui!");
                    break;
                case "setup":
                    gm.setupIsland(player);
                    break;
                case "confirm":
                    if (gm.hasPendingDelete(player)) {
                        int toDelete = gm.getPendingDelete(player);
                        plugin.getBuildsConfig().set("builds." + toDelete, null);
                        plugin.saveConfig();
                        player.sendMessage("§aBuild ID '" + toDelete + "' eliminata.");
                        gm.removePendingDelete(player);
                    } else player.sendMessage("§cNessuna eliminazione in sospeso.");
                    break;
                case "save":
                    if (args.length < 2) { player.sendMessage("§cUsa: /map save <Nome>"); break; }
                    StringBuilder nameBuilder = new StringBuilder();
                    for (int i = 1; i < args.length; i++) nameBuilder.append(args[i]).append(" ");
                    int nextId = 1;
                    if (plugin.getBuildsConfig().contains("builds")) {
                        for (String key : plugin.getBuildsConfig().getConfigurationSection("builds").getKeys(false)) {
                            try { if (Integer.parseInt(key) >= nextId) nextId = Integer.parseInt(key) + 1; } catch (Exception ignored) {}
                        }
                    }
                    gm.saveBuild(player, nextId, nameBuilder.toString().trim());
                    break;
                case "rename":
                    if (args.length < 3) { player.sendMessage("§cUsa: /map rename <id> <Nome>"); break; }
                    try {
                        int id = Integer.parseInt(args[1]);
                        if (!plugin.getBuildsConfig().contains("builds." + id)) { player.sendMessage("§cID non trovato."); break; }
                        StringBuilder renameBuilder = new StringBuilder();
                        for (int i = 2; i < args.length; i++) renameBuilder.append(args[i]).append(" ");
                        String newName = renameBuilder.toString().trim();
                        plugin.getBuildsConfig().set("builds." + id + ".name", newName);
                        plugin.saveConfig();
                        player.sendMessage("§aNome cambiato in '" + newName + "'!");
                    } catch (Exception e) { player.sendMessage("§cL'ID deve essere un numero!"); }
                    break;
                case "update":
                    if (args.length < 2) { player.sendMessage("§cUsa: /map update <id>"); break; }
                    try {
                        int id = Integer.parseInt(args[1]);
                        if (!plugin.getBuildsConfig().contains("builds." + id)) { player.sendMessage("§cID non trovato."); break; }
                        gm.saveBuild(player, id, plugin.getBuildsConfig().getString("builds." + id + ".name", "Sconosciuta"));
                        player.sendMessage("§eBlocchi aggiornati!");
                    } catch (Exception e) { player.sendMessage("§cL'ID deve essere un numero!"); }
                    break;
                case "load":
                    if (args.length < 2) { player.sendMessage("§cUsa: /map load <id>"); break; }
                    try { gm.loadBuild(player, Integer.parseInt(args[1])); } catch (Exception e) { player.sendMessage("§cID invalido!"); }
                    break;
                case "delete":
                    if (args.length < 2) { player.sendMessage("§cUsa: /map delete <id>"); break; }
                    try {
                        int id = Integer.parseInt(args[1]);
                        if (!plugin.getBuildsConfig().contains("builds." + id)) { player.sendMessage("§cID non trovato."); break; }
                        gm.setPendingDelete(player, id);
                        player.sendMessage("§cStai per eliminare l'ID " + id + ". Scrivi §l/map confirm");
                    } catch (Exception e) { player.sendMessage("§cID invalido!"); }
                    break;
                case "setholo":
                    plugin.getBuildsConfig().set("locations.hologram", player.getLocation().add(0, 2, 0));
                    plugin.saveConfig();
                    plugin.getHologramManager().spawnOrUpdate();
                    player.sendMessage("§aOlogramma della Top 10 posizionato in aria!");
                    break;
                case "setexit":
                    if (!player.isOp()) return true;
                    org.bukkit.entity.Villager npc = (org.bukkit.entity.Villager) player.getWorld().spawnEntity(player.getLocation(), org.bukkit.entity.EntityType.VILLAGER);
                    npc.setCustomName("§c§lExit");
                    npc.setCustomNameVisible(true);
                    npc.setAI(false); // Disattiva l'intelligenza artificiale (non si muove)
                    npc.setInvulnerable(true); // Lo rende immortale
                    npc.setCollidable(false); // Disattiva le collisioni (non può essere spinto)
                    player.sendMessage("§aNPC Exit creato alla tua posizione!");
                    break;
                default:
                    player.sendMessage("§cUsa: /map <save|load|update|rename|delete|setup|setlobby>");
            }
        }
        return true;
    }
}