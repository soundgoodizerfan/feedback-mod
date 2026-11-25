// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine;

import java.util.Optional;

import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A thread rod seated in a machine as its screw (the Lathe's carriage screw, the Vice's jaw screw). The thread form and the rod's metal
 * are read off the stack by property, never by id: a rolled thread ({@code rolled_thread_rod}) is strong but loose, a cut thread
 * ({@code cut_thread_rod}) is exact. A harder metal flexes less, so it holds its thread tighter.
 */
public final class ThreadScrew {

    private ThreadScrew() {
    }

    public static final String ROLLED = "rolled_thread_rod";
    public static final String CUT = "cut_thread_rod";

    /** A seated screw: its thread form and the metal it is cut in. */
    public record Screw(boolean cut, Material material) {
        /**
         * How loose this screw runs, as a fraction of travel: its thread form's base looseness scaled by the metal's hardness against the
         * reference (steel), within {@link MachineTuning#SCREW_METAL_SCALE_MIN} to {@link MachineTuning#SCREW_METAL_SCALE_MAX}.
         */
        public float looseness() {
            float base = cut ? MachineTuning.SCREW_CUT_LOOSENESS : MachineTuning.SCREW_ROLLED_LOOSENESS;
            float scale = MachineTuning.SCREW_REFERENCE_HARDNESS.value() / Math.max(0.01f, material.hardness().value());
            return base * Math.max(MachineTuning.SCREW_METAL_SCALE_MIN, Math.min(MachineTuning.SCREW_METAL_SCALE_MAX, scale));
        }
    }

    /** The screw {@code stack} is, or empty when it is not a thread rod. */
    public static Optional<Screw> of(ItemStack stack, Level level) {
        if (stack.isEmpty() || level == null)
            return Optional.empty();
        return Material.find(ClientMaterials.on(level), stack)
                .filter(l -> l.form().equals(ROLLED) || l.form().equals(CUT))
                .map(l -> new Screw(l.form().equals(CUT), l.material()));
    }
}
