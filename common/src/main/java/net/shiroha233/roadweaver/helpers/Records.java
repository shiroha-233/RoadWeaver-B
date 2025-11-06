package net.shiroha233.roadweaver.helpers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * 公共数据记录与编解码器（Common）
 * 注意：保持与现有代码的字段访问兼容；允许在 Codec 字段名上与历史数据不完全一致。
 */
public final class Records {

    private Records() {}

    public record WoodAssets(Block fence, Block hangingSign, Block planks) {}

    /**
     * 单个结构位置与类型
     */
    public record StructureInfo(BlockPos pos, String structureId) {
        public static final Codec<StructureInfo> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        BlockPos.CODEC.fieldOf("pos").forGetter(StructureInfo::pos),
                        Codec.STRING.optionalFieldOf("structure_id", "unknown").forGetter(StructureInfo::structureId)
                ).apply(instance, StructureInfo::new)
        );
    }

    /**
     * 结构位置集合（升级版，包含类型信息）
     */
    public record StructureLocationData(List<BlockPos> structureLocations, List<StructureInfo> structureInfos) {
        public StructureLocationData(List<BlockPos> structureLocations, List<StructureInfo> structureInfos) {
            this.structureLocations = new ArrayList<>(structureLocations != null ? structureLocations : new ArrayList<>());
            this.structureInfos = new ArrayList<>(structureInfos != null ? structureInfos : new ArrayList<>());
        }
        
        // 兼容旧版本：只有位置列表
        public StructureLocationData(List<BlockPos> structureLocations) {
            this(structureLocations, new ArrayList<>());
        }

        public void addStructure(BlockPos pos) {
            structureLocations.add(pos);
        }
        
        public void addStructureInfo(StructureInfo info) {
            structureInfos.add(info);
            if (!structureLocations.contains(info.pos())) {
                structureLocations.add(info.pos());
            }
        }

        // 兼容历史数据：支持旧格式（只有 BlockPos 列表）和新格式（包含 StructureInfo）
        public static final Codec<StructureLocationData> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        BlockPos.CODEC.listOf().optionalFieldOf("structure_locations", new ArrayList<>()).forGetter(StructureLocationData::structureLocations),
                        StructureInfo.CODEC.listOf().optionalFieldOf("structure_infos", new ArrayList<>()).forGetter(StructureLocationData::structureInfos)
                ).apply(instance, StructureLocationData::new)
        );
    }

    /**
     * 结构连接状态
     */
    public enum ConnectionStatus {
        PLANNED,
        GENERATING,
        COMPLETED,
        FAILED
    }

    /**
     * 结构连接（from -> to + 状态）
     */
    public record StructureConnection(BlockPos from, BlockPos to, ConnectionStatus status) {
        public static final Codec<StructureConnection> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        BlockPos.CODEC.fieldOf("from").forGetter(StructureConnection::from),
                        BlockPos.CODEC.fieldOf("to").forGetter(StructureConnection::to),
                        Codec.STRING.optionalFieldOf("status", "PLANNED").xmap(ConnectionStatus::valueOf, Enum::name).forGetter(StructureConnection::status)
                ).apply(instance, StructureConnection::new)
        );

        public StructureConnection(BlockPos from, BlockPos to) {
            this(from, to, ConnectionStatus.PLANNED);
        }
    }

    public record RoadSegmentPlacement(BlockPos middlePos, List<BlockPos> positions) {
        public static final Codec<RoadSegmentPlacement> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        BlockPos.CODEC.fieldOf("middle_pos").forGetter(RoadSegmentPlacement::middlePos),
                        BlockPos.CODEC.listOf().fieldOf("positions").forGetter(RoadSegmentPlacement::positions)
                ).apply(instance, RoadSegmentPlacement::new)
        );
    }

    public enum SpanType {
        BRIDGE,
        TUNNEL
    }

    public record RoadSpan(BlockPos start, BlockPos end, SpanType type) {
        public static final Codec<RoadSpan> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        BlockPos.CODEC.fieldOf("start").forGetter(RoadSpan::start),
                        BlockPos.CODEC.fieldOf("end").forGetter(RoadSpan::end),
                        Codec.STRING.fieldOf("type").xmap(SpanType::valueOf, Enum::name).forGetter(RoadSpan::type)
                ).apply(instance, RoadSpan::new)
        );
    }

    public record RoadData(int width, int roadType, List<BlockState> materials, List<RoadSegmentPlacement> roadSegmentList, List<RoadSpan> spans, List<Integer> targetY) {
        public static final Codec<RoadData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("width").forGetter(RoadData::width),
                Codec.INT.fieldOf("road_type").forGetter(RoadData::roadType),
                BlockState.CODEC.listOf().fieldOf("materials").forGetter(RoadData::materials),
                RoadSegmentPlacement.CODEC.listOf().fieldOf("placements").forGetter(RoadData::roadSegmentList),
                RoadSpan.CODEC.listOf().optionalFieldOf("spans", new ArrayList<>()).forGetter(RoadData::spans),
                Codec.INT.listOf().optionalFieldOf("target_y", new ArrayList<>()).forGetter(RoadData::targetY)
        ).apply(instance, RoadData::new));
    }
}
