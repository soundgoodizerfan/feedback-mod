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
package io.github.soundgoodizerfan.feedback.modular.attunement;

import io.github.soundgoodizerfan.feedback.modular.AttunementKinds;
import io.github.soundgoodizerfan.feedback.modular.MaterialPerks;
import io.github.soundgoodizerfan.feedback.modular.ModularTuning;
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.process.Material;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import static io.github.soundgoodizerfan.feedback.modular.AttunementKinds.onFlame;
import static io.github.soundgoodizerfan.feedback.modular.AttunementKinds.onStrike;
import static io.github.soundgoodizerfan.feedback.modular.AttunementKinds.onStruck;

/**
 * The runtime kinds of the Heat effect ({@code docs/feedback_enchant_roster.md}). Registers its handlers with
 * {@link AttunementKinds} for the three shared moments, and subscribes to any other event itself, reading a part's
 * attunements through {@link MaterialPerks#attunements}.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class HeatKinds {

    /** Fire damage a level of Heat- lining turns away at best, before the lining's conductivity weighs in. [OPEN] until played */
    private static final float INSULATE_PER_LEVEL = 0.2f;
    /** The most fire damage the whole outfit can turn away. [OPEN] until played */
    private static final float INSULATE_MAX = 0.8f;
    /** Conductivity at which a lining insulates half as well (copper is far above it, wool far below). [OPEN] until played */
    private static final float INSULATE_HALF_CONDUCTIVITY = 0.01f;
    /** Conductivity assumed for a lining material that states none. [OPEN] until played */
    private static final float INSULATE_FALLBACK_CONDUCTIVITY = 0.01f;

    /** Frozen ticks a level of Heat- edge adds per blow, on top of what the target already carries. [OPEN] until played */
    private static final int FREEZE_TICKS_PER_LEVEL = 40;
    /** Frozen ticks past the point where freezing starts to hurt, the most blows can stack. [OPEN] until played */
    private static final int FREEZE_OVERSTACK = 40;

    private HeatKinds() {
    }

    public static void register() {
        // Heat- on an edge: each blow stacks freezing as powder snow does, so the target slows and then takes damage.
        onStrike("freeze", (event, a) -> {
            LivingEntity target = event.getEntity();
            if (target.canFreeze())
                target.setTicksFrozen(Math.min(target.getTicksFrozen() + a.level() * FREEZE_TICKS_PER_LEVEL,
                        target.getTicksRequiredToFreeze() + FREEZE_OVERSTACK));
        });
        onStrike("ignite_target", (event, a) ->
                event.getEntity().igniteForSeconds(a.level() * ModularTuning.PERK_IGNITE_SECONDS_PER_LEVEL));
        onStruck("ignite_attacker", (attacker, a) ->
                attacker.igniteForSeconds(a.level() * ModularTuning.PERK_IGNITE_SECONDS_PER_LEVEL));
        // A part that burns the wielder (fire aspect on a handle) burns at its flame temperature, else a fixed heat.
        onFlame("burn_wielder", a -> a.material().combustion().flatMap(Material.Combustion::flameTemperature)
                .orElse(new Tu(ModularTuning.ENCHANT_BURN_FALLBACK_TU)));
    }

    /** Fire on a wearer of Heat- lining: each lining turns away a share, weighted by how poor a conductor its material is. */
    @SubscribeEvent
    public static void onFire(LivingIncomingDamageEvent event) {
        if (!event.getSource().is(DamageTypeTags.IS_FIRE))
            return;
        float kept = 1f;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR)
                continue;
            ItemStack worn = event.getEntity().getItemBySlot(slot);
            for (MaterialPerks.Attunement a : MaterialPerks.attunements(worn)) {
                if (!a.kind().equals("insulate"))
                    continue;
                float k = a.material().thermal().conductivity().map(c -> c.workPerTickPerTu())
                        .orElse(INSULATE_FALLBACK_CONDUCTIVITY);
                kept *= 1f - INSULATE_PER_LEVEL * a.level() * INSULATE_HALF_CONDUCTIVITY / (INSULATE_HALF_CONDUCTIVITY + k);
            }
        }
        event.setAmount(event.getAmount() * Math.max(kept, 1f - INSULATE_MAX));
    }
}
