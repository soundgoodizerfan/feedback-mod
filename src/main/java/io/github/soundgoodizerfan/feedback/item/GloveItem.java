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
package io.github.soundgoodizerfan.feedback.item;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.hazard.Grip;
import io.github.soundgoodizerfan.feedback.core.hazard.Insulator;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Gloves, worn in the Curios {@code hands} slot.
 *
 * <h2>Made of a material, so rated by that material</h2>
 * There is no figure on this class. It holds the id of the material it is made of and
 * {@link Grip} reads that material's own file, so a pair of gloves is as good as the stuff in
 * it and a second material is one registration line. It is the same rule the cable and the
 * heating coil already follow: the item says what it is made of; the material says what that can
 * do.
 *
 * <h2>Thick enough to hold something warm, not something glowing</h2>
 * A glove's exposure is a quarter of a piece's excess over the room, which puts leather at the
 * warm end of the eye scale and well short of red heat. Red-hot work wants tongs (the other
 * physical answer, and one that costs a hand), not a better glove -- two answers that cost
 * different things, philosophy 5.
 *
 * <h2>It does not wear</h2>
 * A glove above its rating is simply not protecting, which is a threshold and not a decay. The
 * accepted wear-by-correct-use exceptions are three and a glove is not a fourth; this one is
 * consumed by a mistake or not at all.
 */
public class GloveItem extends Item implements Insulator {

    private final ResourceLocation material;

    public GloveItem(Properties properties, ResourceLocation material) {
        super(properties.stacksTo(1));
        this.material = material;
    }

    public ResourceLocation material() {
        return material;
    }

    @Override
    public float handlesUpToTu(ItemStack stack, Level level) {
        return Grip.safeUpTo(level, material, FTuning.GLOVE_EXPOSURE);
    }

    @Override
    public float shockProofVolts(ItemStack stack, Level level) {
        return Grip.material(level, material).filter(Grip::insulates).isPresent()
                ? FTuning.GLOVE_SHOCK_PROOF_VOLTS : 0f;
    }
}
