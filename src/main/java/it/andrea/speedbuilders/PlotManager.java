package it.andrea.speedbuilders;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.UUID;

public class PlotManager {

    private final Main plugin;
    private final HashMap<UUID, Integer> playerPlots = new HashMap<>();
    private final PriorityQueue<Integer> availablePlots = new PriorityQueue<>(); // Salva i plot liberati
    private int nextPlotId = 0;

    public PlotManager(Main plugin) {
        this.plugin = plugin;
    }

    public int getPlot(Player player) {
        UUID uuid = player.getUniqueId();
        if (!playerPlots.containsKey(uuid)) {
            // Se c'è un plot vuoto riciclato, prendi il più basso, altrimenti creane uno nuovo!
            int id = availablePlots.isEmpty() ? nextPlotId++ : availablePlots.poll();
            playerPlots.put(uuid, id);
        }
        return playerPlots.get(uuid);
    }

    // Metodo per liberare il plot quando il player quitta
    public void removePlot(Player player) {
        UUID uuid = player.getUniqueId();
        if (playerPlots.containsKey(uuid)) {
            int id = playerPlots.remove(uuid);
            availablePlots.add(id); // Rimette il plot in circolo per il prossimo /p!
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
}