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
package io.github.soundgoodizerfan.feedback.machine.bearing;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationPropagator;
import io.github.soundgoodizerfan.feedback.core.unit.Drag;
import io.github.soundgoodizerfan.feedback.core.unit.Inertia;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A full-size Rotary Bearing or Ring Gear on the network, and its hold on the body it turns
 * ({@link BearingJoint}; {@code docs/feedback_micro_spec.md}, <em>Bearings and ring gears</em>).
 * Assembles whole blocks as the Lift does; puts them back home, unturned.
 */
public class RotaryBearingBlockEntity extends RotationNode {

    @Nullable
    private BearingJoint bearing;

    public RotaryBearingBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.ROTARY_BEARING.get(), pos, state);
    }

    @Override
    public Drag getDragSuPerRpm() {
        return FTuning.ROTARY_BEARING_DRAG_SU_PER_RPM;
    }

    @Override
    public Inertia getInertia() {
        return new Inertia(FTuning.ROTARY_BEARING_INERTIA.suTicksPerRpm() + (bearing == null ? 0 : bearing.runInertia()));
    }

    /** True while it holds a body. */
    public boolean holding() {
        return bearing != null;
    }

    private Direction facing() {
        return getBlockState().getValue(RotaryBearingBlock.FACING);
    }

    /** Assembles, or puts back. Returns the message key. */
    String toggle(ServerLevel level) {
        return bearing == null ? assemble(level) : disassemble(level);
    }

    private String assemble(ServerLevel level) {
        Direction f = facing();
        BlockPos front = worldPosition.relative(f), host = worldPosition.relative(f.getOpposite());
        if (level.getBlockState(front).isAir())
            return "empty";
        var gathered = SubLevelAssemblyHelper.gatherConnectedBlocks(front, level, FTuning.ROTARY_BEARING_MAX_BLOCKS,
                (a, aState, to, toState, direction) -> !to.equals(worldPosition) && !to.equals(host));
        if (gathered.assemblyState() == SubLevelAssemblyHelper.GatherResult.State.NO_BLOCKS)
            return "empty";
        if (gathered.assemblyState() != SubLevelAssemblyHelper.GatherResult.State.SUCCESS || gathered.blocks() == null)
            return "too_big";
        List<BlockPos> blocks = new ArrayList<>(gathered.blocks());
        // From a body too: Sable starts the new body at its parent's pose and speed, so nothing moves.
        ServerSubLevel parent = BearingJoint.parentOf(level, worldPosition);
        ServerSubLevel made = SubLevelAssemblyHelper.assembleBlocks(level, front, blocks, BoundingBox3i.from(blocks));
        bearing = BearingJoint.of(made, worldAnchor(), f, front, parent);
        setChanged();
        return "assembled";
    }

    private String disassemble(ServerLevel level) {
        ServerSubLevel s = bearing.held(level);
        if (s == null)
            return "not_loaded";
        if (BearingJoint.carriesChild(level, s))
            return "has_child";
        var box = s.getPlot().getBoundingBox();
        List<BlockPos> blocks = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()))
            if (!level.getBlockState(p).isAir())
                blocks.add(p.immutable());
        // Home, unturned: Sable's block move turns only about Y, and the Lift puts back unturned too.
        var transform = new SubLevelAssemblyHelper.AssemblyTransform(bearing.homeFrom(), bearing.homeTo(), 0, Rotation.NONE, level);
        for (BlockPos p : blocks)
            if (!level.getBlockState(transform.apply(p)).canBeReplaced())
                return "blocked";
        bearing.letGo();
        if (!blocks.isEmpty())
            SubLevelAssemblyHelper.moveBlocks(level, transform, blocks);
        ServerSubLevelContainer.getContainer(level).removeSubLevel(s, SubLevelRemovalReason.REMOVED);
        forget(level);
        return "disassembled";
    }

    private void forget(ServerLevel level) {
        if (bearing != null)
            bearing.letGo();
        bearing = null;
        setChanged();
        RotationPropagator.rebuildFrom(level, worldPosition);
    }

    void tickServer(ServerLevel level) {
        if (bearing == null)
            return;
        if (bearing.gone(level)) {
            forget(level);
            return;
        }
        if (bearing.tick(level, worldPosition, worldAnchor(), facing(), getRpm().value()))
            RotationPropagator.rebuildFrom(level, worldPosition);
    }

    private Vector3d worldAnchor() {
        Direction f = facing();
        return new Vector3d(worldPosition.getX() + 0.5 + 0.5 * f.getStepX(), worldPosition.getY() + 0.5 + 0.5 * f.getStepY(),
                worldPosition.getZ() + 0.5 + 0.5 * f.getStepZ());
    }

    /** Broken or unloaded: the joint goes (an assembled body broken off falls free). */
    @Override
    public void setRemoved() {
        if (bearing != null)
            bearing.letGo();
        super.setRemoved();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (bearing != null)
            tag.put("Bearing", bearing.save());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        bearing = tag.contains("Bearing") ? BearingJoint.load(tag.getCompound("Bearing")) : null;
    }
}
