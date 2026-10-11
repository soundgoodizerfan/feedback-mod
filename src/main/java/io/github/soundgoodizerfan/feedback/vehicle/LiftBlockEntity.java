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
package io.github.soundgoodizerfan.feedback.vehicle;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.physics.constraint.FixedConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import io.github.soundgoodizerfan.feedback.registry.FVehicles;
import io.github.soundgoodizerfan.feedback.vehicle.frame.FrameBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import java.util.UUID;

/**
 * What a {@link LiftBlock} is holding, and how high. The hold is Sable's {@code Fixed} joint
 * between the body and the world itself -- the same thing Sable does for Create's Sticker -- so
 * the body is a live physics object the whole time, only pinned. Raising the lift moves the
 * joint's world end; the body follows it. The joint keeps the body upright, so a vehicle put back
 * on a lift is set straight.
 */
public class LiftBlockEntity extends BlockEntity {

    /** Highest the lift goes, in blocks above its rest. Placeholder. */
    public static final int MAX_HEIGHT = 10;
    /** Biggest vehicle one lift will take. */
    public static final int MAX_BLOCKS = 4096;

    private int height;
    /** The body held, by Sable's id for it; null when the lift is bare. */
    @Nullable
    private UUID body;
    /** The point of the body that sat on the lift's top face, in the body's own plot. */
    private final Vector3d anchor = new Vector3d();
    /** Live joint; rebuilt by the tick after any change or reload, so never saved. */
    @Nullable
    private PhysicsConstraintHandle hold;

    public LiftBlockEntity(BlockPos pos, BlockState state) {
        super(FVehicles.LIFT_ENTITY.get(), pos, state);
    }

    public boolean holding() {
        return body != null;
    }

    public int height() {
        return height;
    }

    /** The held body, for tests. */
    @Nullable
    public UUID body() {
        return body;
    }

    /**
     * Turns everything resting on the lift into one body and holds it. Returns the message key:
     * {@code lifted}, {@code empty}, or {@code too_big} -- which is also what touching the ground
     * looks like, since the floodfill then runs into the world.
     */
    public String lift(ServerLevel level) {
        BlockPos start = worldPosition.above();
        if (level.getBlockState(start).isAir())
            return "empty";
        var gathered = SubLevelAssemblyHelper.gatherConnectedBlocks(start, level, MAX_BLOCKS,
                (from, fromState, to, toState, direction) -> !to.equals(worldPosition));
        if (gathered.assemblyState() != SubLevelAssemblyHelper.GatherResult.State.SUCCESS || gathered.blocks() == null)
            return "too_big";
        for (BlockPos p : gathered.blocks())
            if (level.getBlockEntity(p) instanceof FrameBlockEntity frame)
                frame.setAssembled(true); // its plates and tubes become boxes on the body
        ServerSubLevel made = SubLevelAssemblyHelper.assembleBlocks(level, start, gathered.blocks(), gathered.boundingBox());
        body = made.getUniqueId();
        made.logicalPose().transformPositionInverse(top(), anchor);
        setChanged();
        return "lifted";
    }

    /** Raises or lowers the lift by {@code steps}. Returns {@code height}, or {@code limit} at either end. */
    public String step(ServerLevel level, int steps) {
        int next = Math.clamp(height + steps, 0, MAX_HEIGHT);
        if (next == height)
            return "limit";
        height = next;
        letGo(level);
        setChanged();
        return "height";
    }

    /** Drops the vehicle into physics: the lift is bare and back at rest. */
    public void release(ServerLevel level) {
        letGo(level);
        body = null;
        height = 0;
        setChanged();
    }

    private void letGo(ServerLevel level) {
        if (hold != null && hold.isValid())
            hold.remove();
        hold = null;
        if (held(level) instanceof ServerSubLevel s)
            SubLevelPhysicsSystem.get(level).getPipeline().wakeUp(s);
    }

    @Nullable
    private ServerSubLevel held(ServerLevel level) {
        return body != null && ServerSubLevelContainer.getContainer(level).getSubLevel(body) instanceof ServerSubLevel s ? s : null;
    }

    /** The middle of the lift's top face, where the vehicle sits, at the lift's current height. */
    private Vector3d top() {
        return new Vector3d(worldPosition.getX() + 0.5, worldPosition.getY() + 1 + height, worldPosition.getZ() + 0.5);
    }

    /** Makes the hold once the body is loaded, and again whenever it has been dropped. */
    static void serverTick(Level level, BlockPos pos, BlockState state, LiftBlockEntity be) {
        if (be.body == null || (be.hold != null && be.hold.isValid()) || !(level instanceof ServerLevel server))
            return;
        ServerSubLevel s = be.held(server);
        if (s == null)
            return; // not loaded yet
        if (s.isRemoved()) {
            be.body = null;
            be.setChanged();
            return;
        }
        var pipeline = SubLevelPhysicsSystem.get(server).getPipeline();
        be.hold = pipeline.addConstraint(s, null, new FixedConstraintConfiguration(be.anchor, be.top(), new Quaterniond()));
        pipeline.wakeUp(s);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (hold != null && hold.isValid())
            hold.remove();
        hold = null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Height", height);
        if (body != null) {
            tag.putUUID("Body", body);
            tag.putDouble("AnchorX", anchor.x);
            tag.putDouble("AnchorY", anchor.y);
            tag.putDouble("AnchorZ", anchor.z);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        height = tag.getInt("Height");
        body = tag.hasUUID("Body") ? tag.getUUID("Body") : null;
        anchor.set(tag.getDouble("AnchorX"), tag.getDouble("AnchorY"), tag.getDouble("AnchorZ"));
    }
}
