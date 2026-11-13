// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.combat.skill.Skills;
import io.github.soundgoodizerfan.feedback.core.hazard.HotContact;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Cauterize (Proficiency): press a hot piece from either hand to the wound. Health comes back in a lump, read off the
 * piece's contact band (a burn heals less than a sear), and the skin pays it in {@link HotContact}: the band's own
 * damage and fire. The heat is spent too: the piece comes away {@link #SPENT} cooler. So it is the heal that works
 * mid-fight, priced in a forge you had to run and a fire you have to put out (fire resistance or Shake Off answers it).
 * Main hand first. Refused at full health or with nothing hot enough to burn. With Sear Shut the hand is practised: the
 * burn it pays is smaller, the piece loses less heat, and the next one comes sooner ({@link CastTuning#SEAR_SHUT_REARM}).
 */
final class Cauterize {

    /** STAND-IN: health restored at the burn and sear bands, before the burn is paid; to be felt in play. */
    static final float HEAL_BURN = 6f, HEAL_SEAR = 12f;
    /** STAND-IN: Tu the piece loses into the wound. */
    static final float SPENT = 200f;

    private Cauterize() {
    }

    static boolean act(ServerPlayer p) {
        if (p.getHealth() >= p.getMaxHealth())
            return false;
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = p.getItemInHand(hand);
            if (!ItemHeat.isHotFelt(stack, p.level()))
                continue;
            Tu tu = ItemHeat.felt(stack, p.level());
            HotContact.Band band = HotContact.band(tu);
            if (band != HotContact.Band.BURN && band != HotContact.Band.SEAR)
                continue;
            boolean practised = Skills.unlocked(p, "sear_shut");
            HotContact.burn(p, tu, practised ? CastTuning.SEAR_SHUT_BURN : 1f);
            p.heal(band == HotContact.Band.SEAR ? HEAL_SEAR : HEAL_BURN);
            ItemHeat.drain(stack, p.level(), practised ? SPENT * CastTuning.SEAR_SHUT_SPENT : SPENT);
            Vec3 at = p.position().add(0, 1, 0);
            p.serverLevel().sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y, at.z, 12, 0.25, 0.3, 0.25, 0.02);
            p.serverLevel().sendParticles(ParticleTypes.LAVA, at.x, at.y, at.z, 4, 0.2, 0.2, 0.2, 0);
            CastFx.sound(p.serverLevel(), at, SoundEvents.FIRE_EXTINGUISH, 1f, 0.7f);
            return true;
        }
        return false;
    }
}
