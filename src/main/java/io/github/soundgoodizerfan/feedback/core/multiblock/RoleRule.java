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
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.core.Direction;

/**
 * Mode B's rule: an ordered list of role slots along {@code up}, each with a count range, e.g.
 * reboiler 1..1, tray 1..max, condenser 1..1. A line that breaks the rule is not refused; the
 * breaks come back as flaws and the structure runs degraded. Parts join a stack only if they
 * carry the same rule instance.
 */
public final class RoleRule {

    public record Slot(String role, int min, int max) {
        public static Slot one(String role) {
            return new Slot(role, 1, 1);
        }

        public static Slot many(String role) {
            return new Slot(role, 1, Integer.MAX_VALUE);
        }
    }

    public final Direction up;
    public final List<Slot> slots;
    final Supplier<Structure> factory;

    public RoleRule(Direction up, Supplier<Structure> factory, Slot... slots) {
        this.up = up;
        this.factory = factory;
        this.slots = List.of(slots);
    }

    /**
     * Checks roles listed bottom to top. Order is enforced by only ever moving forward through the
     * slots; a role with no slot ahead of it is stray. Empty result means formed.
     */
    public List<Structure.Flaw> check(List<String> roles) {
        List<Structure.Flaw> flaws = new ArrayList<>();
        int[] counts = new int[slots.size()];
        int s = 0;
        for (int i = 0; i < roles.size(); i++) {
            String role = roles.get(i);
            int t = s;
            while (t < slots.size() && !(slots.get(t).role().equals(role) && counts[t] < slots.get(t).max()))
                t++;
            if (t == slots.size()) {
                flaws.add(new Structure.Flaw(null, "stray " + role + " at position " + i));
                continue;
            }
            s = t;
            counts[t]++;
        }
        for (int k = 0; k < counts.length; k++)
            if (counts[k] < slots.get(k).min())
                flaws.add(new Structure.Flaw(null,
                        "needs " + slots.get(k).min() + " " + slots.get(k).role() + ", has " + counts[k]));
        return flaws;
    }
}
