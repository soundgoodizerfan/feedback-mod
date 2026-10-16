/*
 * Feedback -- a Minecraft technology mod.
 * Copyright (C) 2026 soundgoodizerfan
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.vehicle.frame;

import io.github.soundgoodizerfan.feedback.registry.FVehicles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * The frame inside one {@link ChassisCellBlock}, traced at 1/16: the shape players stand on,
 * walk into and see outlined, per position -- never the half-block octants physics uses.
 * Saved and synced, because the outline is drawn on the client.
 */
public class ChassisCellBlockEntity extends BlockEntity {

    private static final int G = Frame.GRID;

    /** Bit {@code x + 16y + 256z}: that 1/16 voxel holds plate or tube. */
    private BitSet voxels = new BitSet(G * G * G);
    @Nullable
    private VoxelShape shape;

    public ChassisCellBlockEntity(BlockPos pos, BlockState state) {
        super(FVehicles.CHASSIS_CELL_ENTITY.get(), pos, state);
    }

    public static int index(int x, int y, int z) {
        return x + G * y + G * G * z;
    }

    public void setVoxels(BitSet voxels) {
        this.voxels = (BitSet) voxels.clone();
        shape = null;
        setChanged();
    }

    boolean hasShape() {
        return !voxels.isEmpty();
    }

    VoxelShape shape() {
        if (shape == null)
            shape = merge(voxels);
        return shape;
    }

    /** Greedy-merges voxels into boxes: runs along x, widened along z, stacked along y. */
    static VoxelShape merge(BitSet in) {
        BitSet on = (BitSet) in.clone();
        List<VoxelShape> boxes = new ArrayList<>();
        for (int y = 0; y < G; y++)
            for (int z = 0; z < G; z++)
                for (int x = 0; x < G; x++) {
                    if (!on.get(index(x, y, z)))
                        continue;
                    int x1 = x;
                    while (x1 + 1 < G && on.get(index(x1 + 1, y, z)))
                        x1++;
                    int z1 = z;
                    while (z1 + 1 < G && run(on, x, x1, y, z1 + 1))
                        z1++;
                    int y1 = y;
                    while (y1 + 1 < G && slab(on, x, x1, y1 + 1, z, z1))
                        y1++;
                    for (int yy = y; yy <= y1; yy++)
                        for (int zz = z; zz <= z1; zz++)
                            for (int xx = x; xx <= x1; xx++)
                                on.clear(index(xx, yy, zz));
                    boxes.add(Block.box(x, y, z, x1 + 1, y1 + 1, z1 + 1));
                }
        if (boxes.isEmpty())
            return Shapes.empty();
        return Shapes.or(boxes.get(0), boxes.subList(1, boxes.size()).toArray(VoxelShape[]::new));
    }

    private static boolean run(BitSet on, int x0, int x1, int y, int z) {
        for (int x = x0; x <= x1; x++)
            if (!on.get(index(x, y, z)))
                return false;
        return true;
    }

    private static boolean slab(BitSet on, int x0, int x1, int y, int z0, int z1) {
        for (int z = z0; z <= z1; z++)
            if (!run(on, x0, x1, y, z))
                return false;
        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLongArray("Voxels", voxels.toLongArray());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        voxels = BitSet.valueOf(tag.getLongArray("Voxels"));
        shape = null;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
