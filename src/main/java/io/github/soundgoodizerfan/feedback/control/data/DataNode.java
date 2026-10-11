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
package io.github.soundgoodizerfan.feedback.control.data;

import java.util.Optional;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.fitting.Fittable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

import org.jetbrains.annotations.Nullable;

/**
 * Something that can be linked to another one, invisibly, with the data connector -- no cable
 * block, nothing in the world but the two endpoints. Philosophy's own §"how data gets to the
 * controller" already committed to no wire being a real thing in the world; this is that
 * paragraph, typed.
 *
 * <h2>Read from MrCrayfish's Furniture Mod: Refurbished (MIT), and what changed</h2>
 * {@code IElectricityNode} is the shape this is adapted from: a capability the holder implements,
 * a set of connections it owns, default connect/disconnect/search methods. Left behind
 * deliberately: {@code isSourceNode}/power/overload -- a data link never carries current, it just
 * lets a reading be asked for, so there is no "powered" state and nothing to overload. Also left
 * behind: the GPU frame-graph renderer that draws nodes and wires through walls. That mod targets
 * a newer Minecraft than Feedback's 1.21.1 and uses a render pipeline (a separate texture target
 * composited over the world) that doesn't exist on this toolchain -- the visible-with-tool-in-hand
 * overlay is real content but a different implementation, and isn't built yet (see
 * {@code TODO.md}).
 * <p>
 * Widened from the original: a node's identity is {@link #getNodePos()} <em>and</em>
 * {@link #getNodeSide()}, not position alone, because a {@link Fittable} holder can carry several
 * sensor fittings -- one per side -- each its own node. An unsided node (a controller) answers
 * {@code null} for its side.
 */
public interface DataNode {

    BlockPos getNodePos();

    @Nullable
    Direction getNodeSide();

    Level getNodeLevel();

    BlockEntity getNodeOwner();

    /** The other ends this node is linked to. Owned and persisted by the implementer. */
    Set<DataNodeRef> getNodeLinks();

    default DataNodeRef getNodeRef() {
        return new DataNodeRef(getNodePos(), getNodeSide());
    }

    /**
     * A small box, block-relative, marking where this node sits for the connector's visual --
     * centred for an unsided node, pulled toward the face for a sided one. Read from MrCrayfish's
     * Furniture Mod: Refurbished's {@code IModuleNode.DEFAULT_NODE_BOX}, the same small
     * center-cube shape.
     */
    default AABB getNodeBox() {
        return boxFor(getNodeSide());
    }

    /** Shared by {@link #getNodeBox()} and anything drawing a link to an unresolved reference. */
    static AABB boxFor(@Nullable Direction side) {
        AABB center = new AABB(0.375, 0.375, 0.375, 0.625, 0.625, 0.625);
        if (side == null)
            return center;
        double reach = 0.375;
        return center.move(side.getStepX() * reach, side.getStepY() * reach, side.getStepZ() * reach);
    }

    default boolean isNodeValid() {
        return !getNodeOwner().isRemoved();
    }

    /**
     * A small fixed cap on fan-out. Not tuned to anything yet -- move it to {@code FTuning} the
     * day a real number matters to a player.
     */
    default int getMaxNodeLinks() {
        return 4;
    }

    /**
     * Whether this node holds links at all. A pure actuator (a clutch, a damper, a valve) is only
     * ever <em>targeted</em> -- a card names it directly -- so a link to it would only burn a slot.
     */
    default boolean acceptsLinks() {
        return true;
    }

    default boolean canLinkTo(DataNode other) {
        return this != other && acceptsLinks() && other.acceptsLinks() && isNodeValid() && other.isNodeValid()
                && getNodeLinks().size() < getMaxNodeLinks()
                && other.getNodeLinks().size() < other.getMaxNodeLinks()
                && !getNodeLinks().contains(other.getNodeRef());
    }

    default boolean linkTo(DataNode other) {
        pruneNodeLinks();
        other.pruneNodeLinks();
        if (!canLinkTo(other))
            return false;
        getNodeLinks().add(other.getNodeRef());
        other.getNodeLinks().add(this.getNodeRef());
        getNodeOwner().setChanged();
        other.getNodeOwner().setChanged();
        return true;
    }

    default void unlink(DataNodeRef ref) {
        if (getNodeLinks().remove(ref))
            getNodeOwner().setChanged();
    }

    /** Forgets links whose far end is gone, so dead links stop counting against the cap.
     * An end in an unloaded chunk is kept -- it may well still exist. */
    default void pruneNodeLinks() {
        Level level = getNodeLevel();
        if (level != null && getNodeLinks().removeIf(ref -> level.isLoaded(ref.pos()) && resolve(level, ref).isEmpty()))
            getNodeOwner().setChanged();
    }

    /** This node is going away: take it off every loaded far end, then forget them all. */
    default void unlinkAll() {
        Level level = getNodeLevel();
        if (level != null)
            for (DataNodeRef ref : getNodeLinks())
                resolve(level, ref).ifPresent(other -> other.unlink(getNodeRef()));
        getNodeLinks().clear();
        getNodeOwner().setChanged();
    }

    /**
     * Resolves a reference back to a live node, the same re-derive-don't-cache trade
     * {@code ThermalBody} makes for a workpiece's temperature. An unsided reference must land on
     * a block entity that is itself a {@code DataNode}; a sided one must land on a
     * {@link Fittable} holder carrying a {@code DataNode}-shaped fitting (a {@code SensorFitting})
     * on that exact side.
     */
    static Optional<DataNode> resolve(Level level, DataNodeRef ref) {
        // Never load a chunk to answer: getBlockEntity on an unloaded position would.
        if (!level.isLoaded(ref.pos()))
            return Optional.empty();
        BlockEntity be = level.getBlockEntity(ref.pos());
        if (be == null)
            return Optional.empty();
        if (ref.side() == null)
            return be instanceof DataNode node ? Optional.of(node) : Optional.empty();
        if (be instanceof Fittable fittable && fittable.getSidedFitting(ref.side()) instanceof DataNode node)
            return Optional.of(node);
        return Optional.empty();
    }
}
