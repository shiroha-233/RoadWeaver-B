package net.shiroha233.roadweaver.features.decoration;

import net.shiroha233.roadweaver.features.decoration.util.WoodSelector;
import net.shiroha233.roadweaver.features.decoration.util.BiomeWoodAware;

import java.util.Iterator;
import java.util.Set;

public final class RoadStructures {
    private RoadStructures() {}

    public static void tryPlaceDecorations(Set<Decoration> positions) {
        if (positions.isEmpty()) return;
        Iterator<Decoration> it = positions.iterator();
        while (it.hasNext()) {
            Decoration dec = it.next();
            if (dec == null) { it.remove(); continue; }
            if (dec instanceof BiomeWoodAware aware) {
                aware.setWoodType(WoodSelector.forBiome(dec.getWorld(), dec.getPos()));
            }
            dec.place();
            it.remove();
        }
    }

    
}
