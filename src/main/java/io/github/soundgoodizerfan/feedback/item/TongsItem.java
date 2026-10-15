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
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A pair of tongs: hold one in a hand and the other hand can carry a hot piece, and the hand
 * with the tongs can reach into hot equipment.
 *
 * <h2>Two materials, two limits, and a longer reach than a glove</h2>
 * A glove sits on the palm; tongs put a forearm of arm and a grip between the jaws and the hand,
 * so only a tenth of a piece's excess reaches the grip ({@link FTuning#TONGS_EXPOSURE}). The
 * limit is therefore the lower of what the grip stands at that exposure and the temperature the
 * arms melt at -- a wooden grip and iron arms hold a yellow-hot piece, a bone grip holds whatever
 * the iron will, and copper arms give out before steel's window opens. Nothing is stamped on the
 * item: pair a better grip or a better metal and the limit moves.
 *
 * <p>They do nothing against a bare wire. Holding a piece away from the hand is not insulating
 * the hand from a conductor, and {@link Insulator#shockProofVolts} stays at its default zero.
 *
 * <p>They never wear: the three accepted exceptions are not a list that grows.
 */
public class TongsItem extends Item implements Insulator {

    public TongsItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public float handlesUpToTu(ItemStack stack, Level level) {
        Tongs tongs = stack.get(FDataComponents.TONGS.get());
        if (tongs == null)
            return FTuning.CONTACT_PAIN_TU.value();
        float grip = Grip.safeUpTo(level, tongs.grip(), FTuning.TONGS_EXPOSURE);
        float arms = Grip.material(level, tongs.arms())
                .flatMap(m -> m.thermal().meltingPoint())
                .map(tu -> tu.value())
                .orElse(Float.MAX_VALUE);
        return Math.min(grip, arms);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Tongs tongs = stack.get(FDataComponents.TONGS.get());
        if (tongs == null)
            return;
        tooltip.add(Component.translatable("item.feedback.tongs.arms", name(tongs.arms())));
        tooltip.add(Component.translatable("item.feedback.tongs.grip", name(tongs.grip())));
    }

    private static Component name(ResourceLocation material) {
        return Component.translatable("material." + material.getNamespace() + "." + material.getPath());
    }
}
