package net.shiroha233.roadweaver.features.decoration;

import net.shiroha233.roadweaver.features.decoration.util.WoodSelector;

import java.util.Iterator;
import java.util.Set;

public final class RoadStructures {
    private RoadStructures() {}

    public static void tryPlaceDecorations(Set<Decoration> positions) {
        if (positions.isEmpty()) return;
        Iterator<Decoration> it = positions.iterator();
        while (it.hasNext()) {
            Decoration dec = it.next();
            if (dec == null) continue;
            if (dec instanceof LamppostDecoration lamp) {
                lamp.setWoodType(WoodSelector.forBiome(lamp.getWorld(), lamp.getPos()));
                lamp.place();
            } else if (dec instanceof DistanceSignDecoration sign) {
                sign.setWoodType(WoodSelector.forBiome(sign.getWorld(), sign.getPos()));
                sign.place();
            } else if (dec instanceof LanternPostDecoration natLamp) {
                natLamp.setWoodType(WoodSelector.forBiome(natLamp.getWorld(), natLamp.getPos()));
                natLamp.place();
            } else if (dec instanceof FenceWaypointDecoration wp) {
                wp.setWoodType(WoodSelector.forBiome(wp.getWorld(), wp.getPos()));
                wp.place();
            }
            it.remove();
        }
    }

    
}
