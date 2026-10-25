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
package io.github.soundgoodizerfan.feedback.client;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Says how hot the thing in your hand is -- as a word, or as a figure if you own a thermometer.
 *
 * <h2>It needs no packet, which is the stamp design paying out</h2>
 * A workpiece stores the temperature it was stamped at and the tick that happened on, both
 * network-synchronised components, and the client's game time is the server's. So the client can
 * compute the current figure itself, exactly, with no round trip and no per-item sync. A design
 * that stored a live temperature would have needed one of those and would have been stale anyway.
 *
 * <h2>Why this is a separate listener from {@link WorkpieceTooltip}</h2>
 * The two are independent facts about a stack and one does not imply the other -- a hot ingot
 * that has never been struck is the normal case at the crucible, and a half-worked plate is cold.
 * Folding them together would mean one early return deciding whether the other line prints, which
 * is how a tooltip quietly stops mentioning heat.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public class HeatTooltip {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!stack.has(FDataComponents.TEMPERATURE.get())
                && !(stack.getItem() instanceof ItemHeat.Cooling hold && hold.holdsAt(stack) != null))
            return;

        Level level = Minecraft.getInstance().level;
        if (level == null)
            return;

        Tu tu = ItemHeat.get(stack, level);
        if (tu.value() <= FTuning.WARM_TU.value())
            return;

        event.getToolTip().add(Readout.temperatureReading(tu));
    }
}
