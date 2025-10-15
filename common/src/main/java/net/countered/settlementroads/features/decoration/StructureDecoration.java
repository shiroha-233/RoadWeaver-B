package net.countered.settlementroads.features.decoration;

import net.countered.settlementroads.features.decoration.util.BiomeWoodAware;
import net.countered.settlementroads.helpers.Records;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;

public abstract class StructureDecoration extends OrientedDecoration implements BiomeWoodAware {
    private static final Logger LOGGER = LoggerFactory.getLogger("roadweaver");

    protected Records.WoodAssets wood;
    private final String structureName;
    private final Vec3i structureSize;

    public StructureDecoration(BlockPos pos, Vec3i direction, WorldGenLevel world, String structureName, Vec3i structureSize) {
        super(pos, direction, world);
        this.structureName = structureName;
        this.structureSize = structureSize;
    }

    @Override
    public void place() {
        if (!placeAllowed()) return;
        if (!hasEnoughSpace()) {
            return;
        }
        StructureTemplate template = loadStructureTemplate();
        if (template != null) {
            placeStructure(template);
        } else {
            placeFallbackStructure();
        }
    }

    protected boolean hasEnoughSpace() {
        BlockPos basePos = getPos();
        WorldGenLevel world = getWorld();
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (int x = 0; x < structureSize.getX(); x++) {
            for (int z = 0; z < structureSize.getZ(); z++) {
                BlockPos checkPos = basePos.offset(x, 0, z);
                int groundY = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, checkPos.getX(), checkPos.getZ());
                minY = Math.min(minY, groundY);
                maxY = Math.max(maxY, groundY);
            }
        }
        return (maxY - minY) <= 2;
    }

    protected StructureTemplate loadStructureTemplate() {
        try {
            ResourceLocation structureId = new ResourceLocation("roadweaver", "structures/" + structureName);
            StructureTemplate template = new StructureTemplate();
            InputStream inputStream = getClass().getResourceAsStream("/data/roadweaver/structures/" + structureName + ".nbt");
            if (inputStream != null) {
                CompoundTag nbt = NbtIo.readCompressed(inputStream);
                template.load(getWorld().getLevel().holderLookup(net.minecraft.core.registries.Registries.BLOCK), nbt);
                return template;
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load structure: {} using fallback. Error: {}", structureName, e.getMessage());
        }
        return null;
    }

    protected void placeStructure(StructureTemplate template) {
        BlockPos placePos = getPos();
        WorldGenLevel world = getWorld();
        BlockPos groundLevel = findGroundLevel(placePos, world);
        StructurePlaceSettings placementData = new StructurePlaceSettings()
                .setMirror(Mirror.NONE)
                .setRotation(getRotationFromDirection())
                .setIgnoreEntities(true);
        template.placeInWorld(world, groundLevel, groundLevel, placementData, world.getRandom(), 2);
        cleanupAirBlocks(template, groundLevel, placementData);
    }

    protected BlockPos findGroundLevel(BlockPos basePos, WorldGenLevel world) {
        int minY = Integer.MAX_VALUE;
        Vec3i size = structureSize;
        for (int x = 0; x < size.getX(); x++) {
            for (int z = 0; z < size.getZ(); z++) {
                BlockPos checkPos = basePos.offset(x - size.getX() / 2, 0, z - size.getZ() / 2);
                int groundY = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, checkPos.getX(), checkPos.getZ());
                minY = Math.min(minY, groundY);
            }
        }
        return new BlockPos(basePos.getX(), minY - 1, basePos.getZ());
    }

    protected void cleanupAirBlocks(StructureTemplate template, BlockPos basePos, StructurePlaceSettings placementData) {
        List<StructureTemplate.StructureBlockInfo> blocks = template.filterBlocks(basePos, placementData, Blocks.AIR);
        for (StructureTemplate.StructureBlockInfo blockInfo : blocks) {
            BlockPos airPos = blockInfo.pos();
            BlockState currentState = getWorld().getBlockState(airPos);
            if (shouldReplaceWithAir(currentState)) {
                getWorld().setBlock(airPos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    protected boolean shouldReplaceWithAir(BlockState state) {
        return state.getBlock().equals(Blocks.GRASS)
                || state.getBlock().equals(Blocks.TALL_GRASS)
                || state.is(BlockTags.FLOWERS)
                || state.is(BlockTags.SAPLINGS)
                || state.is(BlockTags.LEAVES);
    }

    protected Rotation getRotationFromDirection() {
        Vec3i roadDirection = getRoadDirection();
        if (Math.abs(roadDirection.getX()) > Math.abs(roadDirection.getZ())) {
            return roadDirection.getX() > 0 ? Rotation.NONE : Rotation.CLOCKWISE_180;
        } else {
            return roadDirection.getZ() > 0 ? Rotation.CLOCKWISE_90 : Rotation.COUNTERCLOCKWISE_90;
        }
    }

    protected Vec3i getRoadDirection() {
        Vec3i orthogonal = getOrthogonalVector();
        return new Vec3i(-orthogonal.getZ(), 0, orthogonal.getX());
    }

    protected abstract void placeFallbackStructure();

    @Override
    public void setWoodType(Records.WoodAssets assets) {
        this.wood = assets;
    }

    public Vec3i getStructureSize() {
        return structureSize;
    }
}
