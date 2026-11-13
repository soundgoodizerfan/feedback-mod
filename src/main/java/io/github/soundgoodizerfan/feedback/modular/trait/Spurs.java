// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import java.util.UUID;


import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Riding boots (spec 5.4, armour): starting to sprint gives a speed boost of {@code boost_ticks},
 * once every {@code cooldown_ticks}, so a dash from standing is quick and a long run is not. The
 * cost is cover: the light boot is laid in fewer cells, so it protects less. Per-wearer state is
 * dropped on death or logout (not on taking the boots off, so swapping cannot reset the wait). Worn: counts once.
 */
public final class Spurs implements ModularTrait {

    /** Tick the boost is ready again, and whether the wearer sprinted last tick. */
    private record Last(long ready, boolean sprinting) {}

    private static final WielderState<Last> STATE = new WielderState<>();

    @Override
    public void onWornTick(Player wearer, ItemStack piece, Params params) {
        UUID id = wearer.getUUID();
        long now = wearer.level().getGameTime();
        Last last = STATE.get(id);
        boolean sprinting = wearer.isSprinting();
        long ready = last == null ? 0 : last.ready();
        if (sprinting && (last == null || !last.sprinting()) && now >= ready) {
            wearer.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, (int) params.get("boost_ticks", 60f), (int) params.get("level", 1f) - 1, false, false));
            ready = now + (long) params.get("cooldown_ticks", 200f);
        }
        STATE.put(id, new Last(ready, sprinting));
    }
}
