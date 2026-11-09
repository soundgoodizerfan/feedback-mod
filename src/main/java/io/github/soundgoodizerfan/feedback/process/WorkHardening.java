// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.process;

import java.util.Map;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Cold working hardens a metal until it is annealed. A cold draw or roll step that finishes a stage adds one to the piece's
 * {@code strain}; a metal takes as many as its {@code ductility} before the next step will not move it (the blow is refused,
 * nothing lost, the piece needs the fire). Heating the piece to its anneal temperature clears the strain. A drawn filament of a
 * {@link Material.Trait#TEXTURED} material counts each anneal taken at the limit as one cycle of {@code texture}: a piece annealed
 * early gains nothing, so the order and spacing of draws and anneals is the path.
 *
 * <p>Only metals that state a work-hardening figure, a ductility and an anneal temperature take part, and below a ductility of
 * {@link #MIN_DUCTILITY} only a textured one does (tungsten, manganese: brittle metals that are not cold drawn). Only draws and
 * rolls count; a hot piece (at or over its anneal temperature) is worked without strain.
 */
public final class WorkHardening {

    /** Metals less ductile than this are left out unless they opt in with the textured trait. */
    public static final int MIN_DUCTILITY = 3;

    private WorkHardening() {
    }

    /** Whether cold work strains {@code m}. */
    public static boolean applies(Material m) {
        if (m == null || m.kind().orElse(null) != Material.Kind.METAL)
            return false;
        var mech = m.mechanical();
        if (mech.workHardening().isEmpty() || mech.ductility().isEmpty() || m.thermal().annealTemperature().isEmpty())
            return false;
        return mech.ductility().get() >= MIN_DUCTILITY || m.traits().contains(Material.Trait.TEXTURED);
    }

    /** Cold steps {@code m} takes before it needs an anneal. */
    public static int limit(Material m) {
        return m.mechanical().ductility().orElse(0);
    }

    public static int strain(ItemStack stack) {
        return stack.getOrDefault(FDataComponents.STRAIN.get(), 0);
    }

    public static int texture(ItemStack stack) {
        return stack.getOrDefault(FDataComponents.TEXTURE.get(), 0);
    }

    /** The material {@code stack} is, if cold work strains it. */
    public static Optional<Material> strained(Map<ResourceLocation, Material> materials, ItemStack stack) {
        return Material.find(materials, stack).map(Material.Located::material).filter(WorkHardening::applies);
    }

    /** A piece that is hot has recrystallised: its strain is gone, and for a textured filament an anneal at the limit is a cycle. */
    public static void annealIfHot(ItemStack stack, Tu tu, Level level) {
        if (level == null || !stack.has(FDataComponents.STRAIN.get()))
            return;
        var found = strained(io.github.soundgoodizerfan.feedback.process.ClientMaterials.on(level), stack);
        if (found.isEmpty()) {
            stack.remove(FDataComponents.STRAIN.get());
            return;
        }
        Material m = found.get();
        if (tu.value() < m.thermal().annealTemperature().get().value())
            return;
        if (m.traits().contains(Material.Trait.TEXTURED) && strain(stack) >= limit(m))
            stack.set(FDataComponents.TEXTURE.get(), texture(stack) + 1);
        stack.remove(FDataComponents.STRAIN.get());
    }
}
