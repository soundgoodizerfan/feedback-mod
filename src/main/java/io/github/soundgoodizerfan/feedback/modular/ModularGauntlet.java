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
package io.github.soundgoodizerfan.feedback.modular;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.hazard.Grip;
import io.github.soundgoodizerfan.feedback.core.hazard.Insulator;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures.Fig;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

/**
 * A hand covering worn in the Curios {@code hands} slot: a gauntlet, or a glove when its plate is cloth. One
 * modular type ({@code family: gauntlet}); the plate decides everything, the way a tongs' grip does. It adds
 * {@link Fig#FIST} to a barehanded swing ({@code FistStances}) and keeps heat and a weak shock off the hand
 * as far as the plate's own file stands it ({@link Grip}), so a metal gauntlet punches and protects from nothing
 * and a cloth glove protects and punches almost not at all.
 */
public class ModularGauntlet extends ModularItem implements Insulator {

    /** The plate role's letter in the type file. */
    private static final String PLATE = "P";

    public ModularGauntlet(Properties properties) {
        super(properties);
    }

    /** Damage {@code stack} adds to a barehanded swing: 0 for anything else or a broken one. */
    public static float fist(ItemStack stack) {
        if (!(stack.getItem() instanceof ModularGauntlet) || ModularStacks.broken(stack))
            return 0f;
        ModularFigures f = ModularStacks.figures(stack);
        return f == null ? 0f : f.get(Fig.FIST);
    }

    @Nullable
    private static ResourceLocation plate(ItemStack stack) {
        ModularDesign d = ModularStacks.design(stack);
        ModularDesign.Part p = d == null ? null : d.part(PLATE);
        return p == null ? null : p.material();
    }

    @Override
    public float handlesUpToTu(ItemStack stack, Level level) {
        ResourceLocation plate = plate(stack);
        return plate == null || ModularStacks.broken(stack) ? 0f : Grip.safeUpTo(level, plate, FTuning.GLOVE_EXPOSURE);
    }

    @Override
    public float shockProofVolts(ItemStack stack, Level level) {
        ResourceLocation plate = plate(stack);
        return plate == null || ModularStacks.broken(stack) ? 0f
                : Grip.material(level, plate).filter(Grip::insulates).isPresent() ? FTuning.GLOVE_SHOCK_PROOF_VOLTS : 0f;
    }

    /** "Steel Gauntlet", "Wool Glove": a gauntlet is a glove when its plate is fibrous cloth. */
    @Override
    public Component getName(ItemStack stack) {
        ResourceLocation plate = plate(stack);
        var materials = ClientMaterials.get().isEmpty() ? MaterialTable.get().materials() : ClientMaterials.get();
        Material m = plate == null ? null : materials.get(plate);
        boolean glove = m != null && m.traits().contains(Material.Trait.FIBROUS);
        return ModularStacks.name(stack, glove ? Component.translatable("item.feedback.glove") : super.getName(stack));
    }
}
