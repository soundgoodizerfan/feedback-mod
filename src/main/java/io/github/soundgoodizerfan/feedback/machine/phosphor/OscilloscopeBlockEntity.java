// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.phosphor;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.instrument.Instrument;
import io.github.soundgoodizerfan.feedback.instrument.Instruments;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * An oscilloscope (mechanics 6.20): a block that watches the current in a cable beside it and shows
 * it on a phosphor screen beside it. It is an {@link Instrument} for {@link Quantity#CURRENT} at
 * {@link FTuning#SCOPE_RESOLUTION_AMPS}. It is not an {@code ElectricMember}, so it joins no run;
 * it only reads the node it touches, as a galvanometer does.
 *
 * <h2>Overrun</h2>
 * Current past {@link FTuning#SCOPE_FULL_SCALE_AMPS} pegs the trace at the top, exact and wrong,
 * like the mass probe; the screen then sits at full glow, which is what burns it in. A scope never
 * feeds back into the line.
 *
 * <h2>Sense and stop</h2>
 * Sense: the adjacent cable's current. Stop: passive; take it away from the cable.
 */
public class OscilloscopeBlockEntity extends BlockEntity implements Instrument {

    private final float[] history = new float[FTuning.SCOPE_HISTORY];
    private int head;

    public OscilloscopeBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.OSCILLOSCOPE.get(), pos, state);
    }

    @Override
    public boolean canRead(Quantity quantity) {
        return quantity == Quantity.CURRENT;
    }

    @Override
    public float resolution(Quantity quantity) {
        return FTuning.SCOPE_RESOLUTION_AMPS;
    }

    @Override
    public Component label() {
        return Component.translatable("feedback.instrument.oscilloscope");
    }

    public void tickServer() {
        if (level == null || level.getGameTime() % FTuning.SCOPE_SAMPLE_TICKS != 0)
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
        float amps = line == null ? 0f : Math.min(FTuning.SCOPE_FULL_SCALE_AMPS, Math.max(0f, line.getAmps()));
        history[head] = amps;
        head = (head + 1) % history.length;
        if (screen != null)
            screen.land(Math.round(amps / FTuning.SCOPE_FULL_SCALE_AMPS * 3f));
    }

    /** The history as a row of bars, oldest first, each quantised to this scope's step. */
    public Component trace() {
        String bars = "▁▂▃▄▅▆▇█";
        StringBuilder row = new StringBuilder();
        float last = 0f;
        for (int i = 0; i < history.length; i++) {
            last = Instruments.quantise(history[(head + i) % history.length], resolution(Quantity.CURRENT));
            row.append(bars.charAt(Math.round(Math.min(1f, last / FTuning.SCOPE_FULL_SCALE_AMPS) * 7f)));
        }
        return Instruments.signed(this, Component.translatable("feedback.scope.trace", row.toString(),
                Instruments.format(last, resolution(Quantity.CURRENT))));
    }
}
