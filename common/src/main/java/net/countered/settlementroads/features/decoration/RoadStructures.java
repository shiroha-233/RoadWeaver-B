package net.countered.settlementroads.features.decoration;

import net.countered.settlementroads.features.decoration.util.WoodSelector;

import java.util.Iterator;
import java.util.Set;

public class RoadStructures {

    public static void tryPlaceDecorations(Set<Decoration> roadDecorationPlacementPositions) {
        if (roadDecorationPlacementPositions.isEmpty()) {
            return;
        }
        Iterator<Decoration> iterator = roadDecorationPlacementPositions.iterator();
        while (iterator.hasNext()) {
            Decoration roadDecoration = iterator.next();
            if (roadDecoration != null) {
                if (roadDecoration instanceof LamppostDecoration lamppostDecoration) {
                    lamppostDecoration.setWoodType(WoodSelector.forBiome(lamppostDecoration.getWorld(), lamppostDecoration.getPos()));
                    lamppostDecoration.place();
                }
                if (roadDecoration instanceof DistanceSignDecoration distanceSignDecoration) {
                    distanceSignDecoration.setWoodType(WoodSelector.forBiome(distanceSignDecoration.getWorld(), distanceSignDecoration.getPos()));
                    distanceSignDecoration.place();
                }
                if (roadDecoration instanceof FenceWaypointDecoration fenceWaypointDecoration) {
                    fenceWaypointDecoration.setWoodType(WoodSelector.forBiome(fenceWaypointDecoration.getWorld(), fenceWaypointDecoration.getPos()));
                    fenceWaypointDecoration.place();
                }
                if (roadDecoration instanceof RoadFenceDecoration roadFenceDecoration) {
                    roadFenceDecoration.setWoodType(WoodSelector.forBiome(roadFenceDecoration.getWorld(), roadFenceDecoration.getPos()));
                    roadFenceDecoration.place();
                }
                if (roadDecoration instanceof SwingDecoration swingDecoration) {
                    swingDecoration.setWoodType(WoodSelector.forBiome(swingDecoration.getWorld(), swingDecoration.getPos()));
                    swingDecoration.place();
                }
                if (roadDecoration instanceof BenchDecoration benchDecoration) {
                    benchDecoration.setWoodType(WoodSelector.forBiome(benchDecoration.getWorld(), benchDecoration.getPos()));
                    benchDecoration.place();
                }
                if (roadDecoration instanceof GlorietteDecoration glorietteDecoration) {
                    glorietteDecoration.setWoodType(WoodSelector.forBiome(glorietteDecoration.getWorld(), glorietteDecoration.getPos()));
                    glorietteDecoration.place();
                }
                if (roadDecoration instanceof NbtStructureDecoration nbtStructureDecoration) {
                    nbtStructureDecoration.setWoodType(WoodSelector.forBiome(nbtStructureDecoration.getWorld(), nbtStructureDecoration.getPos()));
                    nbtStructureDecoration.place();
                }
                iterator.remove();
            }
        }
    }
}
