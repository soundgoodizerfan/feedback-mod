// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.column;

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.core.multiblock.Part;
import io.github.soundgoodizerfan.feedback.core.multiblock.RoleRule;
import io.github.soundgoodizerfan.feedback.core.multiblock.Structure;

import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * The fractionating column as a self-joining stack (spec 6.4, mode B): a reboiler at the bottom, one
 * or more trays, and optionally a condenser on top. No controller and no refusal: a column with one
 * tray is a still that smears its cuts, and one with no condenser runs warm at the top.
 */
public class ColumnStructure extends Structure {

    public static final String REBOILER = "reboiler", TRAY = "tray", CONDENSER = "condenser";

    public static final RoleRule RULE = new RoleRule(Direction.UP, ColumnStructure::new,
            RoleRule.Slot.one(REBOILER), RoleRule.Slot.many(TRAY), new RoleRule.Slot(CONDENSER, 0, 1));

    /** A part's role from the block's column tags, or {@code fallback} for a block not tagged. */
    public static String roleOf(net.minecraft.world.level.block.state.BlockState state, String fallback) {
        if (state.is(io.github.soundgoodizerfan.feedback.registry.FTags.COLUMN_REBOILER))
            return REBOILER;
        if (state.is(io.github.soundgoodizerfan.feedback.registry.FTags.COLUMN_TRAY))
            return TRAY;
        if (state.is(io.github.soundgoodizerfan.feedback.registry.FTags.COLUMN_CONDENSER))
            return CONDENSER;
        return fallback;
    }

    @Override
    public void tick(Level level) {
        List<Part> reboilers = parts(REBOILER);
        if (reboilers.isEmpty() || !(reboilers.get(0) instanceof ReboilerBlockEntity reboiler))
            return;
        List<TrayBlockEntity> trays = new ArrayList<>();
        for (Part p : parts(TRAY))
            if (p instanceof TrayBlockEntity tray)
                trays.add(tray);
        // The top is as cold as the condenser body actually is; with none it is NaN and the reboiler assumes a warm top.
        float top = Float.NaN;
        for (Part p : parts(CONDENSER))
            if (p instanceof io.github.soundgoodizerfan.feedback.machine.condenser.CondenserBlockEntity c)
                top = c.getTemperature().value();
        reboiler.run(level, trays, top);
    }
}
