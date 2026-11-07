// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.trait;

import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.Smash;
import io.github.soundgoodizerfan.feedback.modular.ModularTrait;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

/**
 * Sabatons (spec 5.4, armour): the mace's smash, landed on the wearer's own feet. Landing from a
 * fall of {@code min_fall} blocks or more crushes the hostile mobs within {@code radius} for the
 * mace's fall damage ({@link Smash#fallDamage}, times {@code per_block}, at most {@code max}) and
 * throws the mace's ring of shove, sound and dust {@code shove} times as hard around the wearer.
 * Landing on a soft block (wool, snow or leaves) costs the wearer no fall damage at all. The
 * crushing wears the piece by {@code wear}; the cost beyond that is mass, the heavy six-cell
 * shell. Worn: counts once.
 */
public final class Trample implements ModularTrait {

    @Override
    public void onWornFall(LivingFallEvent event, LivingEntity wearer, ItemStack piece, Params params) {
        var below = wearer.getBlockStateOn();
        if (below.is(BlockTags.WOOL) || below.is(BlockTags.SNOW) || below.is(BlockTags.LEAVES))
            event.setDamageMultiplier(0f);
        float fall = event.getDistance();
        if (fall < params.get("min_fall", 3f))
            return;
        float damage = Math.min(params.get("max", 12f), Smash.fallDamage(fall) * params.get("per_block", 1f));
        DamageSource source = wearer instanceof Player p ? wearer.damageSources().playerAttack(p) : wearer.damageSources().mobAttack(wearer);
        for (LivingEntity target : wearer.level().getEntitiesOfClass(LivingEntity.class,
                wearer.getBoundingBox().inflate(params.get("radius", 1.5f), 0.5, params.get("radius", 1.5f)),
                e -> e != wearer && e instanceof Enemy))
            target.hurt(source, damage);
        Smash.shockwave(wearer.level(), wearer, wearer.position(), wearer.getOnPos(), Smash.RADIUS, params.get("shove", 1f),
                fall > Smash.HEAVY_FALL);
        piece.hurtAndBreak(ModularStacks.cap(piece, (int) params.get("wear", 1f)), wearer, ((ArmorItem) piece.getItem()).getEquipmentSlot());
    }
}
