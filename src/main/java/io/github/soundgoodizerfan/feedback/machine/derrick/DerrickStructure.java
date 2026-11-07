// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.derrick;

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.core.multiblock.Part;
import io.github.soundgoodizerfan.feedback.core.multiblock.RoleRule;
import io.github.soundgoodizerfan.feedback.core.multiblock.Structure;

import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * The derrick as a modular stack (spec 3.1, 6.2 mode B): one base at the bottom and a casing block
 * on top for every 8 m of hole it may bore. No controller; the stack is the machine. A base with
 * too little casing is not refused, it just cannot reach deeper than its casing allows.
 */
public class DerrickStructure extends Structure {

    public static final String BASE = "base", CASING = "casing";

    /** Parts join only under this one instance (see {@link RoleRule}). */
    public static final RoleRule RULE = new RoleRule(Direction.UP, DerrickStructure::new,
            RoleRule.Slot.one(BASE), RoleRule.Slot.many(CASING));

    @Override
    public void tick(Level level) {
        List<Part> bases = parts(BASE);
        if (!bases.isEmpty() && bases.get(0) instanceof DerrickBaseBlockEntity base)
            base.bore(level, new ArrayList<>(parts(CASING)));
    }
}
