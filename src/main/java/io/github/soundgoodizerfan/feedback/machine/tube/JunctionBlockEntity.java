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
package io.github.soundgoodizerfan.feedback.machine.tube;

import java.util.List;

import io.github.soundgoodizerfan.feedback.core.tube.Capsule;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A tube block that routes by address alone: each face carries an optional named colour, and a
 * capsule leaves through the face whose colour matches its own. It never looks at the load
 * (spec 4.3). An unaddressed or unmatched capsule is the undefined case: a plain junction sends it
 * out of the first open face; a programmed junction sends it to its return face and, with none open,
 * holds it until the return line has room.
 */
public class JunctionBlockEntity extends TubeBlockEntity {

    private final DyeColor[] faces = new DyeColor[6];
    private int returnFace = -1;

    public JunctionBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.TUBE_JUNCTION.get(), pos, state);
    }

    public boolean programmed() {
        return getBlockState().getBlock() instanceof JunctionBlock j && j.programmed();
    }

    @Nullable
    public DyeColor face(Direction d) {
        return faces[d.get3DDataValue()];
    }

    public int returnFace() {
        return returnFace;
    }

    public void setFace(Direction d, @Nullable DyeColor colour) {
        faces[d.get3DDataValue()] = colour;
        setChanged();
    }

    public void toggleReturn(Direction d) {
        returnFace = returnFace == d.get3DDataValue() ? -1 : d.get3DDataValue();
        setChanged();
    }

    public void clear() {
        java.util.Arrays.fill(faces, null);
        returnFace = -1;
        setChanged();
    }

    @Nullable
    @Override
    protected Direction route(Capsule c, List<Direction> open) {
        DyeColor address = CarrierItem.address(c.stack).orElse(null);
        if (address != null)
            for (Direction d : open)
                if (faces[d.get3DDataValue()] == address)
                    return d;
        if (!programmed())
            return open.get(0);
        for (Direction d : open)
            if (d.get3DDataValue() == returnFace)
                return d;
        return null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        int[] colours = new int[6];
        for (int i = 0; i < 6; i++)
            colours[i] = faces[i] == null ? -1 : faces[i].getId();
        tag.putIntArray("Faces", colours);
        tag.putInt("Return", returnFace);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        int[] colours = tag.getIntArray("Faces");
        for (int i = 0; i < 6; i++)
            faces[i] = i < colours.length && colours[i] >= 0 ? DyeColor.byId(colours[i]) : null;
        returnFace = tag.contains("Return") ? tag.getInt("Return") : -1;
    }
}
