package net.shiroha233.roadweaver.structures.api;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public final class StructureConnector {
    private final BlockPos relativePos;
    private final Direction facing;
    private final int width;
    private final String role;

    public StructureConnector(BlockPos relativePos, Direction facing, int width, String role) {
        this.relativePos = relativePos;
        this.facing = facing;
        this.width = Math.max(1, width);
        this.role = role;
    }

    public BlockPos relativePos() { return relativePos; }
    public Direction facing() { return facing; }
    public int width() { return width; }
    public String role() { return role; }
}
