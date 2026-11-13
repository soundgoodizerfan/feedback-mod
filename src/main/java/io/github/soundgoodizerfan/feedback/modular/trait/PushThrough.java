// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import java.util.UUID;

import io.github.soundgoodizerfan.feedback.combat.tether.BodyMass;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Greaves (spec 5.4, armour): plate on the shins tears through what holds the legs. Cobweb, powder snow and
 * berry bushes all slow a body by setting its stuck multiplier; a wearer takes back a share of that slow, their
 * mass (body plus armour) over {@code ref_mu}, at most {@code max}. A heavy body in plate walks through a web that
 * stops a light one. Each tear costs {@code exhaustion} hunger and {@code wear} on the piece, then waits
 * {@code cooldown_ticks} before costing again (the slow stays eased meanwhile). Per-wearer cooldown is dropped on
 * death or logout. Worn: counts once.
 */
public final class PushThrough implements ModularTrait {

    private static final WielderState<Long> READY = new WielderState<>();

    @Override
    public void onWornTick(Player wearer, ItemStack piece, Params params) {
        Vec3 stuck = wearer.stuckSpeedMultiplier;
        if (stuck.lengthSqr() < 1.0e-9)
            return;
        float share = Math.min(params.get("max", 0.8f),
                (BodyMass.of(wearer) + BodyMass.armour(wearer)) / params.get("ref_mu", 200f));
        wearer.stuckSpeedMultiplier = new Vec3(stuck.x + (1 - stuck.x) * share, stuck.y + (1 - stuck.y) * share, stuck.z + (1 - stuck.z) * share);
        UUID id = wearer.getUUID();
        long now = wearer.level().getGameTime();
        Long ready = READY.get(id);
        if (ready != null && now < ready)
            return;
        wearer.causeFoodExhaustion(params.get("exhaustion", 0.3f));
        piece.hurtAndBreak(ModularStacks.cap(piece, (int) params.get("wear", 1f)), wearer, ((ArmorItem) piece.getItem()).getEquipmentSlot());
        READY.put(id, now + (long) params.get("cooldown_ticks", 30f));
    }
}
