// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.phosphor;

import io.github.soundgoodizerfan.feedback.machine.ElectricMachineTuning;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.instrument.Instrument;
import io.github.soundgoodizerfan.feedback.instrument.Instruments;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import io.github.soundgoodizerfan.feedback.net.ScopeTracePayload;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * An oscilloscope: a block that watches the current in a cable beside it and shows
 * it on a phosphor screen beside it. It is an {@link Instrument} for {@link Quantity#CURRENT} at
 * {@link ElectricMachineTuning#SCOPE_RESOLUTION_AMPS}. It is not an {@code ElectricMember}, so it joins no run;
 * it only reads the node it touches, as a galvanometer does.
 *
 * <h2>Overrun</h2>
 * Current past {@link ElectricMachineTuning#SCOPE_FULL_SCALE_AMPS} pegs the trace at the top, exact and wrong,
 * like the mass probe; the screen then sits at full glow, which is what burns it in. A scope never
 * feeds back into the line.
 *
 * <h2>Sense and stop</h2>
 * Sense: the adjacent cable's current. Stop: passive; take it away from the cable.
 */
public class OscilloscopeBlockEntity extends BlockEntity implements Instrument {

    private final float[] history = new float[ElectricMachineTuning.SCOPE_HISTORY];
    private int head;
    /** The row as last sent, and where and when (never saved): what makes a send happen only on change. */
    private final float[] row = new float[ElectricMachineTuning.SCOPE_HISTORY];
    private BlockPos sentTo;
    private long sentAt;

    public OscilloscopeBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.OSCILLOSCOPE.get(), pos, state);
    }

    @Override
    public boolean canRead(Quantity quantity) {
        return quantity == Quantity.CURRENT;
    }

    @Override
    public float resolution(Quantity quantity) {
        return ElectricMachineTuning.SCOPE_RESOLUTION_AMPS;
    }

    @Override
    public Component label() {
        return Component.translatable("feedback.instrument.oscilloscope");
    }

    public void tickServer() {
        if (level == null || level.getGameTime() % ElectricMachineTuning.SCOPE_SAMPLE_TICKS != 0)
            return;
        ElectricNode line = null;
        PhosphorScreenBlockEntity screen = null;
        for (Direction side : Direction.values()) {
            BlockEntity next = level.getBlockEntity(worldPosition.relative(side));
            if (next instanceof ElectricNode node && line == null)
                line = node;
            else if (next instanceof PhosphorScreenBlockEntity s && screen == null)
                screen = s;
        }
        float amps = line == null ? 0f : Math.min(ElectricMachineTuning.SCOPE_FULL_SCALE_AMPS, Math.max(0f, line.getAmps()));
        history[head] = amps;
        head = (head + 1) % history.length;
        if (screen != null) {
            screen.land(Math.round(amps / ElectricMachineTuning.SCOPE_FULL_SCALE_AMPS * 3f));
            sendTrace(screen.getBlockPos());
        } else if (sentTo != null) {
            // The screen this scope drove is no longer beside it (or gone): blank its trace once so it does not keep a stale line.
            if (level instanceof ServerLevel server)
                PacketDistributor.sendToPlayersTrackingChunk(server, new ChunkPos(sentTo), new ScopeTracePayload(sentTo, new float[history.length]));
            sentTo = null;
        }
    }

    /**
     * Send the row to the screen it drives, to players tracking its chunk: only when the row changed (or the screen is a new one), and
     * otherwise as a slow keepalive so a viewer who arrives late gets it. No allocation unless something is sent.
     */
    private void sendTrace(BlockPos screen) {
        if (!(level instanceof ServerLevel server))
            return;
        boolean changed = !screen.equals(sentTo);
        for (int i = 0; i < history.length; i++) {
            float share = Instruments.quantise(history[(head + i) % history.length], ElectricMachineTuning.SCOPE_RESOLUTION_AMPS)
                    / ElectricMachineTuning.SCOPE_FULL_SCALE_AMPS;
            if (row[i] != share) {
                row[i] = share;
                changed = true;
            }
        }
        if (!changed && server.getGameTime() - sentAt < ElectricMachineTuning.SCOPE_TRACE_KEEPALIVE_TICKS)
            return;
        sentTo = screen.immutable();
        sentAt = server.getGameTime();
        PacketDistributor.sendToPlayersTrackingChunk(server, new ChunkPos(screen), new ScopeTracePayload(sentTo, row.clone()));
    }

    /** The history as a row of bars, oldest first, each quantised to this scope's step. */
    public Component trace() {
        String bars = "▁▂▃▄▅▆▇█";
        StringBuilder row = new StringBuilder();
        float last = 0f;
        for (int i = 0; i < history.length; i++) {
            last = Instruments.quantise(history[(head + i) % history.length], resolution(Quantity.CURRENT));
            row.append(bars.charAt(Math.round(Math.min(1f, last / ElectricMachineTuning.SCOPE_FULL_SCALE_AMPS) * 7f)));
        }
        return Instruments.signed(this, Component.translatable("feedback.scope.trace", row.toString(),
                Instruments.format(last, resolution(Quantity.CURRENT))));
    }
}
