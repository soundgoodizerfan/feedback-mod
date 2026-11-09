// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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
 * The runtime kinds of the Heat effect. Registers its handlers with
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

    /** Gun barrel cooling: share added per level of {@code vent_cold}, and the most the barrel may cool at (a trade, not a freeze). [OPEN] until played */
    private static final float VENT_COLD_PER_LEVEL = 0.3f, VENT_COLD_MAX = 3f;
    /** Gun barrel heat: share of extra heat per shot per level of {@code vent_hot}, and its ceiling. [OPEN] until played */
    private static final float VENT_HOT_PER_LEVEL = 0.25f, VENT_HOT_MAX = 2.5f;

    private HeatKinds() {
    }

    private static float gunMult(ItemStack gun, String kind, float perLevel, float max) {
        int n = TideKinds.level(gun, kind);
        return n == 0 ? 1f : Math.min(max, 1f + perLevel * n);
    }

    /** Multiplier on a gun's cooling rate: 1 without a {@code vent_cold} part. */
    public static float gunCooling(ItemStack gun) {
        return gunMult(gun, "vent_cold", VENT_COLD_PER_LEVEL, VENT_COLD_MAX);
    }

    /** Multiplier on the heat a shot puts in the barrel: 1 without a {@code vent_hot} part. */
    public static float gunShotHeat(ItemStack gun) {
        return gunMult(gun, "vent_hot", VENT_HOT_PER_LEVEL, VENT_HOT_MAX);
    }

    /**
     * The temperature a stack's Heat+ edge (an {@code ignite_target} part: blade, head, tip or barrel) holds it at, or null
     * for none: the hottest part's flame temperature, else the fixed enchant heat. A permanent figure, never spent.
     */
    public static Tu edgeTu(ItemStack stack) {
        Tu hottest = null;
        for (MaterialPerks.Attunement a : MaterialPerks.attunements(stack)) {
            if (!a.kind().equals("ignite_target"))
                continue;
            Tu t = a.material().combustion().flatMap(Material.Combustion::flameTemperature)
                    .orElse(new Tu(ModularTuning.ENCHANT_BURN_FALLBACK_TU));
            if (hottest == null || t.value() > hottest.value())
                hottest = t;
        }
        return hottest;
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
