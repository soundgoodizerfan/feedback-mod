// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular;

import java.util.Map;
import java.util.Optional;

import net.minecraft.world.item.ItemStack;

import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;

/**
 * The spring a weapon carries: the addon role that names the {@code spring} trait (maul, spear, sword). What it stores is its
 * volume times its material's stiffness ({@link ModularBake#strength}), so a bigger or stiffer spring punches and throws harder;
 * what it can take is the mount's, not its own ({@link SpringSet}): a strong spring in a weak head loads it past its rating.
 *
 * @param volume    litres of spring stock.
 * @param stiffness the spring material's yield strength.
 * @param params    the role's numbers: {@code reset} (ticks to recock), {@code rating} (the mount's strike rating at a toughness of 20) and
 *                  {@code throw_rating} (the same for a throw).
 */
public record SpringPart(float volume, float stiffness, Map<String, Float> params) {

    /** Id of the trait that marks the role. */
    public static final String TRAIT = "spring";

    /** The spring on {@code stack}, if its design fills a role that names the spring trait. */
    public static Optional<SpringPart> of(ItemStack stack) {
        ModularDesign design = ModularStacks.design(stack);
        ModularType type = design == null ? null : ModularTypes.known(design.type());
        if (type == null)
            return Optional.empty();
        for (Map.Entry<String, ModularType.Role> e : type.roles().entrySet()) {
            ModularType.Role role = e.getValue();
            ModularDesign.Part part = design.part(e.getKey());
            if (part == null || !TRAIT.equals(role.trait()))
                continue;
            Material m = MaterialTable.get().materials().get(part.material());
            if (m != null)
                return Optional.of(new SpringPart(role.volume(part), ModularBake.strength(m), role.params()));
        }
        return Optional.empty();
    }

    /** Extra damage the cocked spring adds to the next blow, before any set. */
    public float stored() {
        return ModularTuning.SPRING_STORED_PER_LS * volume * stiffness;
    }

    /** Multiple the spring adds to a throw's speed, before any set (1 for none). */
    public float throwMult() {
        return 1f + ModularTuning.SPRING_THROW_PER_LS * volume * stiffness;
    }

    public float param(String name, float fallback) {
        return params.getOrDefault(name, fallback);
    }
}
