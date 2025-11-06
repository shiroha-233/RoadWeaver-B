package net.shiroha233.roadweaver.persistence.fabric;

import net.shiroha233.roadweaver.helpers.Records;
import net.shiroha233.roadweaver.persistence.WorldDataProvider;
import net.shiroha233.roadweaver.persistence.attachments.WorldDataAttachment;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map;

public class FabricWorldDataProvider extends WorldDataProvider {

    @Override
    public Records.StructureLocationData getStructureLocations(ServerLevel level) {
        Records.StructureLocationData data = ((AttachmentTarget) level).getAttached(WorldDataAttachment.STRUCTURE_LOCATIONS);
        return data != null ? data : new Records.StructureLocationData(new ArrayList<>());
    }

    @Override
    public void setStructureLocations(ServerLevel level, Records.StructureLocationData data) {
        ((AttachmentTarget) level).setAttached(WorldDataAttachment.STRUCTURE_LOCATIONS, data);
    }

    @Override
    public List<Records.StructureConnection> getStructureConnections(ServerLevel level) {
        return ((AttachmentTarget) level).getAttachedOrCreate(WorldDataAttachment.CONNECTED_STRUCTURES, ArrayList::new);
    }

    @Override
    public void setStructureConnections(ServerLevel level, List<Records.StructureConnection> connections) {
        ((AttachmentTarget) level).setAttached(WorldDataAttachment.CONNECTED_STRUCTURES, connections);
    }

    @Override
    public Set<Long> getPlannedTileKeys(ServerLevel level) {
        return ((AttachmentTarget) level).getAttachedOrCreate(WorldDataAttachment.PLANNED_TILE_KEYS, java.util.HashSet::new);
    }

    @Override
    public void setPlannedTileKeys(ServerLevel level, Set<Long> keys) {
        ((AttachmentTarget) level).setAttached(WorldDataAttachment.PLANNED_TILE_KEYS, keys);
    }

    @Override
    public Map<Long, Long> getPlannedTileCenters(ServerLevel level) {
        return ((AttachmentTarget) level).getAttachedOrCreate(WorldDataAttachment.PLANNED_TILE_CENTERS, java.util.HashMap::new);
    }

    @Override
    public void setPlannedTileCenters(ServerLevel level, Map<Long, Long> centers) {
        ((AttachmentTarget) level).setAttached(WorldDataAttachment.PLANNED_TILE_CENTERS, centers);
    }
}
