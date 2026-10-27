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
package io.github.soundgoodizerfan.feedback.core.multiblock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * A formed (or degraded) assembly: the parts it found and the state they share. Runtime only; the
 * parts are the truth and {@link Multiblocks} rebuilds it whenever one is placed or broken.
 * <p>
 * Never refused: a stack or pattern that does not satisfy its rule still gets a structure, with
 * {@link #formed()} false and {@link #flaws()} saying why, so a machine can run at what its parts
 * can do. Subclass for machine state; {@link #tick} runs once per level tick, not once per part.
 */
public class Structure {

    /** One reason a structure is not whole. {@code pos} is the wrong block for a pattern, null for a stack. */
    public record Flaw(@Nullable BlockPos pos, String what) {
    }

    final List<Part> members = new ArrayList<>();
    boolean formed;
    List<Flaw> flaws = List.of();
    @Nullable
    Controller controller;

    /** Parts in order: bottom to top for a stack, discovery order for a pattern. */
    public List<Part> members() {
        return members;
    }

    public boolean formed() {
        return formed;
    }

    public List<Flaw> flaws() {
        return flaws;
    }

    @Nullable
    public Controller controller() {
        return controller;
    }

    public List<Part> parts(String role) {
        List<Part> out = new ArrayList<>();
        for (Part p : members)
            if (p.role().equals(role))
                out.add(p);
        return out;
    }

    public int count(String role) {
        return parts(role).size();
    }

    /** Called once per level tick, server side. */
    public void tick(Level level) {
    }

    /** A rebuilt structure inherits state from the ones it replaces (held pool, temperature). */
    public void merge(Collection<Structure> old) {
    }
}
