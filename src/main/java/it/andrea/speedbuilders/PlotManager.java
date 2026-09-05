package it.andrea.speedbuilders;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

public class PlotManager {

    private final Main plugin;
    private final HashMap<UUID, Integer> playerPlots = new HashMap<>();
    private int nextPlotId = 0; // Il Plot 0 è il centro esatto. Plot 1-6 sono il primo anello, 7-18 il secondo, ecc.

    public PlotManager(Main plugin) {
        this.plugin = plugin;
    }

    // Assegna un plot univoco al giocatore se non ne ha uno
    public int getPlot(Player player) {
        UUID uuid = player.getUniqueId();
        if (!playerPlots.containsKey(uuid)) {
            playerPlots.put(uuid, nextPlotId);
            nextPlotId++;
        }
        return playerPlots.get(uuid);
    }

    // Calcola il centro esatto di un plot usando la matematica degli anelli radiali
    public Location getPlotCenter(World world, int plotId) {
        // Plot 0 al centro assoluto
        if (plotId == 0) {
            return new Location(world, 0.5, 100, 0.5);
        }

        int currentPlot = 1;
        int ring = 1;

        while (true) {
            // Calcola quanti plot entrano in questo anello mantenendo 50 blocchi di distanza
            int plotsInRing = (int) Math.round(2 * Math.PI * ring);

            if (plotId < currentPlot + plotsInRing) {
                // Abbiamo trovato in quale anello si trova il plot!
                int positionInRing = plotId - currentPlot;

                // Calcola l'angolo e la distanza dal centro
                double angle = (2 * Math.PI / plotsInRing) * positionInRing;
                double radius = ring * 50.0; // Distanza fissa di 50 blocchi per ogni anello

                double x = radius * Math.cos(angle);
                double z = radius * Math.sin(angle);

                return new Location(world, Math.floor(x) + 0.5, 100, Math.floor(z) + 0.5);
            }
            currentPlot += plotsInRing;
            ring++;
        }
    }
}