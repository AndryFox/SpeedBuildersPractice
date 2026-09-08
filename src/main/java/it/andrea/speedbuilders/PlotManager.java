package it.andrea.speedbuilders;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;
import java.util.UUID;

public class PlotManager {

    private final Main plugin;
    // Mappe per la gestione Multiplayer
    private final HashMap<UUID, Integer> playerPlots = new HashMap<>(); // Proprietario -> ID Plot
    private final HashMap<UUID, UUID> visiting = new HashMap<>(); // Ospite -> Proprietario
    private final HashMap<UUID, List<UUID>> guests = new HashMap<>(); // Proprietario -> Lista Ospiti

    private final PriorityQueue<Integer> availablePlots = new PriorityQueue<>();
    private int nextPlotId = 0;

    public PlotManager(Main plugin) {
        this.plugin = plugin;
    }

    public int getPlot(Player player) {
        UUID uuid = player.getUniqueId();

        // Se il giocatore sta visitando un amico, restituisce il plot dell'amico
        if (visiting.containsKey(uuid)) {
            UUID owner = visiting.get(uuid);
            if (playerPlots.containsKey(owner)) {
                return playerPlots.get(owner);
            }
        }

        // Altrimenti gli assegna il suo plot personale (riciclato o nuovo)
        if (!playerPlots.containsKey(uuid)) {
            int id = availablePlots.isEmpty() ? nextPlotId++ : availablePlots.poll();
            playerPlots.put(uuid, id);
        }
        return playerPlots.get(uuid);
    }

    // Aggiunge un amico al plot
    public void addGuest(Player owner, Player guest) {
        UUID ownerId = owner.getUniqueId();
        UUID guestId = guest.getUniqueId();

        guests.putIfAbsent(ownerId, new ArrayList<>());
        if (!guests.get(ownerId).contains(guestId)) {
            guests.get(ownerId).add(guestId);
        }
        visiting.put(guestId, ownerId); // Segna il guest come "in visita"
    }

    // Controlla se il giocatore è il proprietario effettivo del plot in cui si trova
    public boolean isOwner(Player player) {
        return !visiting.containsKey(player.getUniqueId());
    }

    public void removePlot(Player player) {
        UUID uuid = player.getUniqueId();
        if (playerPlots.containsKey(uuid)) {
            int id = playerPlots.remove(uuid);
            availablePlots.add(id);
        }

        // Pulisce la memoria da visite e ospiti
        visiting.remove(uuid);
        if (guests.containsKey(uuid)) {
            for (UUID guest : guests.get(uuid)) {
                visiting.remove(guest); // Caccia gli ospiti se il proprietario quitta
            }
            guests.remove(uuid);
        }
    }

    public Location getPlotCenter(World world, int plotId) {
        int currentPlot = 0;
        int ring = 1;

        while (true) {
            int plotsInRing = (int) Math.round(2 * Math.PI * ring);

            if (plotId < currentPlot + plotsInRing) {
                int positionInRing = plotId - currentPlot;
                double angle = (2 * Math.PI / plotsInRing) * positionInRing;
                double radius = ring * 50.0;
                double x = radius * Math.cos(angle);
                double z = radius * Math.sin(angle);
                return new Location(world, Math.floor(x) + 0.5, 100, Math.floor(z) + 0.5);
            }
            currentPlot += plotsInRing;
            ring++;
        }
    }

    // Rileva in quale plot si trova fisicamente un giocatore (raggio di 15 blocchi)
    public int getPlotAt(Location loc) {
        if (loc.getWorld() == null || !loc.getWorld().getName().equals("practice")) return -1;

        // Controlla solo i plot attualmente occupati per le massime prestazioni
        for (Integer plotId : playerPlots.values()) {
            Location center = getPlotCenter(loc.getWorld(), plotId);
            if (loc.distance(center) <= 15) {
                return plotId;
            }
        }
        return -1; // Il giocatore è nel "vuoto" tra un'isola e l'altra
    }

}