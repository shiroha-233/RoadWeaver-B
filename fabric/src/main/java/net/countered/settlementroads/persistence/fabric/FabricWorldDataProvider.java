package net.countered.settlementroads.persistence.fabric;

import net.countered.settlementroads.helpers.Records;
import net.countered.settlementroads.persistence.WorldDataProvider;
import net.countered.settlementroads.persistence.attachments.WorldDataAttachment;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;

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
    public List<Records.RoadData> getRoadDataList(ServerLevel level) {
        return ((AttachmentTarget) level).getAttachedOrCreate(WorldDataAttachment.ROAD_DATA_LIST, ArrayList::new);
    }

    @Override
    public void setRoadDataList(ServerLevel level, List<Records.RoadData> roadDataList) {
        ((AttachmentTarget) level).setAttached(WorldDataAttachment.ROAD_DATA_LIST, roadDataList);
    }
}
