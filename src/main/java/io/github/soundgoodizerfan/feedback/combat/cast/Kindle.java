// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.combat.skill.Skills;
import io.github.soundgoodizerfan.feedback.core.hazard.HotContact;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.Vec3;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.near;

/** Kindle: a hot held item touched to what burns, on purpose. */
final class Kindle {

    private Kindle() {
    }

    /** STAND-INS: how far ahead the fire is laid, its radius, and how much heat the item gives up per thing lit. */
    private static final double REACH = 2.6, RADIUS = 2.2;
    private static final float COST_TU = 40f;

    /** Kindle: a held item above the burn line (a gun only through its Heat+ edge) lights every flammable block and living thing within two blocks of the spot ahead of you; the hotter the item the longer they burn (doubled by Scorch), and the fire passes on to bodies beside them. The item cools by what it lit. */
    static boolean act(ServerPlayer p) {
        ItemStack held = p.getMainHandItem();
        if (held.isEmpty())
            return false;
        Tu tu = ItemHeat.feltNoBarrel(held, p.level());
        if (tu == null)
            return false;
        ServerLevel lv = p.serverLevel();
        Vec3 spot = p.position().add(0, 0.3, 0).add(p.getLookAngle().multiply(1, 0, 1).normalize().scale(REACH));
        float seconds = (3 + tu.value() / 300f) * (Skills.unlocked(p, "scorch") ? 2f : 1f);
        int lit = 0;
        for (LivingEntity e : near(p, spot, RADIUS)) {
            Disorder.ignite(p, e, seconds);
            lit++;
        }
        BlockPos c = BlockPos.containing(spot);
        for (BlockPos pos : BlockPos.betweenClosed(c.offset(-2, -1, -2), c.offset(2, 1, 2))) {
            if (pos.distToCenterSqr(spot) > RADIUS * RADIUS || !lv.getBlockState(pos).isAir())
                continue;
            BlockPos below = pos.below();
            if (lv.getBlockState(below).isFlammable(lv, below, Direction.UP) && lv.mayInteract(p, pos)
                    && BaseFireBlock.canBePlacedAt(lv, pos, Direction.UP)) {
                lv.setBlockAndUpdate(pos, BaseFireBlock.getState(lv, pos));
                lit++;
            }
        }
        if (lit == 0)
            return false;
        if (!(held.getItem() instanceof GunItem))
            ItemHeat.drain(held, p.level(), lit * COST_TU);
        HotContact.burn(p, new Tu(Math.min(tu.value(), 500f)));
        p.swing(InteractionHand.MAIN_HAND, true);
        lv.sendParticles(ParticleTypes.FLAME, spot.x, spot.y + 0.2, spot.z, 30, RADIUS * 0.4, 0.2, RADIUS * 0.4, 0.04);
        CastFx.ring(lv, ParticleTypes.FLAME, spot, RADIUS, 24, 0.05);
        CastFx.spray(lv, ParticleTypes.LAVA, spot.add(0, 0.3, 0), 8, 0.3);
        CastFx.sound(lv, spot, SoundEvents.FIRECHARGE_USE, 1f, 0.7f);
        CastFx.sound(lv, spot, SoundEvents.LAVA_POP, 0.8f, 0.8f);
        return true;
    }
}
