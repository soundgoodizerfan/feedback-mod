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
package io.github.soundgoodizerfan.feedback.vehicle.drive;

import io.github.soundgoodizerfan.feedback.core.rotation.Rotatable;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.core.unit.Drag;
import io.github.soundgoodizerfan.feedback.core.unit.Inertia;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.vehicle.frame.Frame;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * The free-form drivetrain of one frame, {@code feedback_vehicles_spec.md} section 5: one member
 * of the block network at the shaft a tube is welded to, reporting its figures through
 * {@code RotationNetwork.driveExternally} every tick it couples.
 * <p>
 * The <b>coupling node</b> is a {@link RotationNode} that a tube is welded to
 * ({@link Frame#anchors()}) and that has a shaft on the face the tube leaves along -- the tube
 * runs straight out of the shaft, on its axis. Any shaft block; there is no flange block.
 * <p>
 * Step 2 of the spec's build order: no hubs and no tires yet, so what it reports is a
 * placeholder load. Derived from the frame and rebuilt when an edit marks it stale, like the
 * boxes; whether the welded block is a shaft is asked live, since a block can change without
 * the frame being edited.
 */
public final class Drivetrain {

    /**
     * What the drivetrain resists with until tires exist (spec section 5, step 4) -- a load
     * against the coupling node's turning, in that node's own frame. Placeholder.
     */
    public static final Su PLACEHOLDER_LOAD = new Su(10f);

    /** A welded point that couples if its block has a shaft on {@code face}. */
    private record Weld(BlockPos offset, Direction face) {}

    private final List<Weld> welds = new ArrayList<>();

    /**
     * Every welded point with a tube leaving it square to the grid: the face that tube leaves
     * along is the one the block needs a shaft on. A tube at an angle couples nothing.
     */
    public void rebuild(Frame frame) {
        welds.clear();
        frame.anchors().forEach((vertex, offset) -> {
            Frame.Vertex v = frame.vertices().get(vertex);
            for (long key : frame.tubesAt(vertex)) {
                Frame.Vertex o = frame.vertices().get(Frame.edgeA(key) == vertex ? Frame.edgeB(key) : Frame.edgeA(key));
                Direction face = Direction.fromDelta(Integer.signum(o.x() - v.x()),
                        Integer.signum(o.y() - v.y()), Integer.signum(o.z() - v.z()));
                if (face != null)
                    welds.add(new Weld(offset, face));
            }
        });
    }

    /**
     * Reports to the first welded point that couples. The figures are the node's own, referred
     * into its run's frame the way {@code RotationNetwork.recalculate} refers members: push by
     * 1/ratio, drag and inertia by ratio squared (both nothing until step 6).
     * <p>
     * Runs in the frame's block entity tick, which is before the network's
     * ({@code LevelTickEvent.Post}) -- inside a body's plot too, whose block entities tick with
     * the level's own.
     */
    public void tick(Level level, BlockPos framePos) {
        for (Weld w : welds) {
            BlockPos at = framePos.offset(w.offset);
            BlockState state = level.getBlockState(at);
            if (!(state.getBlock() instanceof Rotatable shaft) || !shaft.hasShaftTowards(level, at, state, w.face)
                    || !(level.getBlockEntity(at) instanceof RotationNode node) || node.getNetwork() == null)
                continue;
            node.getNetwork().driveExternally(
                    new Su(-Math.signum(node.getRpm().value()) * PLACEHOLDER_LOAD.value() / node.getRatio()),
                    new Drag(0), new Inertia(0));
            return;
        }
    }
}
