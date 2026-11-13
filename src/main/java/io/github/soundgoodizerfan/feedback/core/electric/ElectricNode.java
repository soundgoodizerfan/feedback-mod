// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.electric;

import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.Catchup;
import io.github.soundgoodizerfan.feedback.core.thermal.FireSpan;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalBody;
import java.util.HashSet;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.control.data.DataNode;
import io.github.soundgoodizerfan.feedback.control.data.DataNodeRef;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Base block entity for anything on an electrical run: wires, sources, sinks.
 *
 * <h2>What a node knows</h2>
 * Its role and, once per tick, what the run decided for it: the band and packet it sees and the current it
 * is delivered against the current it asked for. It does not know where the supply is, who else
 * is on the line or why it is starved -- that is a whole-run question, answered by {@link
 * ElectricRun} and pushed down, exactly as a rotation node cannot tell on its own that it is
 * overloaded.
 *
 * <h2>Every node is a {@link DataNode}</h2>
 * A failure of supply must show on the thing that failed, so every wire and
 * sink carries a reading the data link and the instruments can reach. The reading is held here as
 * three floats and is not synced to clients -- an instrument composes its figure on the server,
 * where it is true, and a client copy would be stale by construction.
 */
public abstract class ElectricNode extends BlockEntity implements ElectricMember, DataNode {

    private static final String LINKS = "Links";

    @Nullable
    private ElectricRun run;

    private VoltageBand band = VoltageBand.LV;
    private float seenEu;
    private float amps;
    private float wanted;
    private boolean blocked;

    private final Set<DataNodeRef> links = new HashSet<>();

    /** When this was last saved, so a load can say how long it was away. See {@link Catchup}. */
    private final Catchup catchup = new Catchup();

    protected ElectricNode(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // --- the reading --------------------------------------------------------------------------

    /** The run's band at this node; LV when nothing arrives (read with {@link #getPacketEu}). */
    public VoltageBand getBand() {
        return band;
    }

    /** Eu in the packet at this node: on a wire, what is left after the loss behind it; at a sink, what it receives; at a source, what it sends. */
    public float getPacketEu() {
        return seenEu;
    }

    /** Whether any packet reaches this node. */
    public boolean isLive() {
        return seenEu > 0f;
    }

    /**
     * What an electrochemical device (battery, jar, charger) sees here as a potential; see {@link ElectricRules#seenPotential}.
     * Never shown to a player.
     */
    public float seenPotential() {
        return ElectricRules.seenPotential(band, seenEu);
    }

    /**
     * The band a bare hand would meet on this block, or null where a casing or insulation covers it,
     * which is almost everywhere. A conductor whose insulation has gone, and a vessel
     * whose terminals are exposed, say what they hold. {@code Shock} turns it into a hurt, and a
     * glove or its absence decides who pays.
     */
    @Nullable
    public VoltageBand touchBand() {
        return null;
    }

    /** Current through a wire, delivered to a sink, or drawn from a source. */
    public float getAmps() {
        return amps;
    }

    /** SINK: the current it asked for. A reading below this is a starved sink. */
    public float getWantedAmps() {
        return wanted;
    }

    /** SOURCE: active but outbid by a higher voltage on its run, so supplying nothing. */
    public boolean isBlocked() {
        return blocked;
    }

    @Override
    public void onResolved(VoltageBand band, float eu, float amps, float wanted, boolean blocked) {
        // The first resolve after a load: if this node holds a temperature, it was not ticked while
        // its chunk was away, and nothing was driving it either -- so it has been cooling. Every
        // subclass calls this first, so its own step starts from the temperature it should have.
        if (level != null && this instanceof ThermalBody body) {
            long gap = catchup.take(level);
            if (gap > 0)
                Catchup.settle(body, FireSpan.NONE, AmbientTable.at(level), gap);
        }
        this.band = band;
        this.seenEu = eu;
        this.amps = amps;
        this.wanted = wanted;
        this.blocked = blocked;
    }

    @Nullable
    public ElectricRun getRun() {
        return run;
    }

    void setRun(@Nullable ElectricRun run) {
        this.run = run;
    }

    // --- lifecycle ----------------------------------------------------------------------------

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide)
            ElectricPropagator.onAdded(level, worldPosition);
    }

    private boolean unloaded;

    boolean isGone() {
        return unloaded || isRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        unloaded = true;
    }

    @Override
    public void setRemoved() {
        // Unload and break both rebuild the neighbours: the run cannot keep a member that is gone.
        // The rebuild never loads a chunk (it checks isLoaded first), which is what made the
        // rotation propagator avoid it on unload.
        if (level != null && !level.isClientSide)
            ElectricPropagator.onRemoved(level, worldPosition, this);
        super.setRemoved();
    }

    // --- DataNode -----------------------------------------------------------------------------

    @Override
    public BlockPos getNodePos() {
        return getBlockPos();
    }

    @Override
    public Direction getNodeSide() {
        return null;
    }

    @Override
    public Level getNodeLevel() {
        return level;
    }

    @Override
    public BlockEntity getNodeOwner() {
        return this;
    }

    @Override
    public Set<DataNodeRef> getNodeLinks() {
        return links;
    }

    // --- persistence --------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (DataNodeRef ref : links)
            list.add(ref.toTag());
        tag.put(LINKS, list);
        catchup.save(tag, level);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        catchup.load(tag);
        links.clear();
        for (Tag entry : tag.getList(LINKS, Tag.TAG_COMPOUND))
            links.add(DataNodeRef.fromTag((CompoundTag) entry));
    }
}
