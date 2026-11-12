// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.note;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;

/** The part of a note block that makes the sound: one per instrument, each made of its own material. A note block needs one fitted to sound at all. */
public class NoisemakerItem extends Item {

    /** The instruments a noisemaker can be: the sounded ones, never a mob head. */
    public static final java.util.List<NoteBlockInstrument> INSTRUMENTS = java.util.Arrays.stream(NoteBlockInstrument.values())
            .filter(NoteBlockInstrument::isTunable).toList();

    private static final Map<NoteBlockInstrument, NoisemakerItem> BY = new EnumMap<>(NoteBlockInstrument.class);
    private final NoteBlockInstrument instrument;

    public NoisemakerItem(NoteBlockInstrument instrument, Properties properties) {
        super(properties);
        this.instrument = instrument;
        BY.put(instrument, this);
    }

    public NoteBlockInstrument instrument() {
        return instrument;
    }

    public static Item of(NoteBlockInstrument instrument) {
        NoisemakerItem item = BY.get(instrument);
        return item == null ? io.github.soundgoodizerfan.feedback.registry.FItems.NOISEMAKERS.get(NoteBlockInstrument.HARP).get() : item;
    }
}
