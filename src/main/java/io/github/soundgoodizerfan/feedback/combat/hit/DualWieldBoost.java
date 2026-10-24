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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.combat.hit;

import io.github.soundgoodizerfan.feedback.Feedback;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The attack speed bonus for {@link Wielding#dualWielding}, as a transient attribute modifier the server
 * adds and removes (the attribute syncs to the client by itself, so both sides' cooldowns agree).
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class DualWieldBoost {

    private static final ResourceLocation ID = Feedback.id("dual_wield");

    private DualWieldBoost() {
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        var speed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (speed == null)
            return;
        boolean dual = Wielding.dualWielding(player), has = speed.getModifier(ID) != null;
        if (dual && !has)
            speed.addTransientModifier(new AttributeModifier(ID, Wielding.DUAL_SPEED - 1.0,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        else if (!dual && has)
            speed.removeModifier(ID);
    }
}
