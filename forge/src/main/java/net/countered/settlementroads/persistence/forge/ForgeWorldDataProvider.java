package net.countered.settlementroads.persistence.forge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.countered.settlementroads.helpers.Records;
import net.countered.settlementroads.persistence.WorldDataProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Forge 端世界数据提供者实现，使用 SavedData 在 ServerLevel 持久化存储。
 */
public class ForgeWorldDataProvider extends WorldDataProvider {

    private static final String DATA_NAME = "roadweaver_world_data";

    /**
     * 实际持久化的数据容器。
     * 保存结构位置、结构连接、道路数据列表。
     */
    public static class Data extends SavedData {
        private Records.StructureLocationData structureLocations = new Records.StructureLocationData(new ArrayList<>());
        private List<Records.StructureConnection> connections = new ArrayList<>();
        private List<Records.RoadData> roadDataList = new ArrayList<>();

        // NBT 字段名
        private static final String KEY_LOCATIONS = "structure_locations";
        private static final String KEY_CONNECTIONS = "connections";
        private static final String KEY_ROAD_DATA = "road_data_list";

        public Data() {}

        public static Data load(CompoundTag tag) {
            Data data = new Data();
            DynamicOps<Tag> ops = NbtOps.INSTANCE;

            // 结构位置（从 CompoundTag 读取）
            if (tag.contains(KEY_LOCATIONS)) {
                Tag locTag = tag.get(KEY_LOCATIONS);
                DataResult<Records.StructureLocationData> res = Records.StructureLocationData.CODEC.parse(new Dynamic<>(ops, locTag));
                res.result().ifPresent(val -> data.structureLocations = val);
            }

            // 结构连接（从 ListTag 读取）
            if (tag.contains(KEY_CONNECTIONS)) {
                Tag conTag = tag.get(KEY_CONNECTIONS);
                DataResult<List<Records.StructureConnection>> res = Codec.list(Records.StructureConnection.CODEC).parse(new Dynamic<>(ops, conTag));
                res.result().ifPresent(val -> data.connections = val);
            }

            // 道路数据列表（从 ListTag 读取）
            if (tag.contains(KEY_ROAD_DATA)) {
                Tag roadsTag = tag.get(KEY_ROAD_DATA);
                DataResult<List<Records.RoadData>> res = Codec.list(Records.RoadData.CODEC).parse(new Dynamic<>(ops, roadsTag));
                res.result().ifPresent(val -> data.roadDataList = val);
            }

            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            DynamicOps<Tag> ops = NbtOps.INSTANCE;

            // 结构位置（Record 编码为 CompoundTag）
            Records.StructureLocationData.CODEC.encodeStart(ops, structureLocations)
                    .result()
                    .ifPresent(nbt -> tag.put(KEY_LOCATIONS, nbt));

            // 结构连接（List 编码为 ListTag）
            Codec.list(Records.StructureConnection.CODEC).encodeStart(ops, connections)
                    .result()
                    .ifPresent(nbt -> tag.put(KEY_CONNECTIONS, nbt));

            // 道路数据列表（List 编码为 ListTag）
            Codec.list(Records.RoadData.CODEC).encodeStart(ops, roadDataList)
                    .result()
                    .ifPresent(nbt -> tag.put(KEY_ROAD_DATA, nbt));

            return tag;
        }

        // getters/setters
        public Records.StructureLocationData getStructureLocations() {
            return structureLocations;
        }

        public void setStructureLocations(Records.StructureLocationData data) {
            this.structureLocations = Objects.requireNonNullElseGet(data, () -> new Records.StructureLocationData(new ArrayList<>()));
            setDirty();
        }

        public List<Records.StructureConnection> getConnections() {
            return connections;
        }

        public void setConnections(List<Records.StructureConnection> connections) {
            this.connections = Objects.requireNonNullElseGet(connections, ArrayList::new);
            setDirty();
        }

        public List<Records.RoadData> getRoadDataList() {
            return roadDataList;
        }

        public void setRoadDataList(List<Records.RoadData> roadDataList) {
            this.roadDataList = Objects.requireNonNullElseGet(roadDataList, ArrayList::new);
            setDirty();
        }
    }

    private Data getOrCreate(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(Data::load, Data::new, DATA_NAME);
    }

    @Override
    public Records.StructureLocationData getStructureLocations(ServerLevel level) {
        return getOrCreate(level).getStructureLocations();
    }

    @Override
    public void setStructureLocations(ServerLevel level, Records.StructureLocationData data) {
        getOrCreate(level).setStructureLocations(data);
    }

    @Override
    public List<Records.StructureConnection> getStructureConnections(ServerLevel level) {
        return getOrCreate(level).getConnections();
    }

    @Override
    public void setStructureConnections(ServerLevel level, List<Records.StructureConnection> connections) {
        getOrCreate(level).setConnections(connections);
    }

    @Override
    public List<Records.RoadData> getRoadDataList(ServerLevel level) {
        return getOrCreate(level).getRoadDataList();
    }

    @Override
    public void setRoadDataList(ServerLevel level, List<Records.RoadData> roadDataList) {
        getOrCreate(level).setRoadDataList(roadDataList);
    }
}