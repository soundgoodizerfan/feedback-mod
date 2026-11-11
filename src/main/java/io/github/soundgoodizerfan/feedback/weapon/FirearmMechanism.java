// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.weapon;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import io.github.soundgoodizerfan.feedback.core.Haste;
import io.github.soundgoodizerfan.feedback.modular.ModularTraits;
import io.github.soundgoodizerfan.feedback.process.Material;

/**
 * A firearm's mechanism: a powder charge in a case throws the projectile, read off the gun as a {@link GunSpec} and off the round
 * chambered as a {@link Cartridge}. The shot itself is {@link GunShots#triggerFirearm}; this class is the seam that lets the shared
 * code reach it without knowing it is a firearm.
 */
public final class FirearmMechanism implements GunMechanism {

    public static final FirearmMechanism INSTANCE = new FirearmMechanism();

    private FirearmMechanism() {
    }

    /**
     * The part of a firearm's figures that only its design and its next round decide: the base bake, the load and the trait list.
     * Redone only when one of those changes; heat, wear and haste are applied on top each call.
     */
    private record Baked(GunSpec design, Map<ResourceLocation, Material> materials, Cartridge round, boolean lastRound,
                         Optional<GunFigures> base, Optional<GunSpec.Load> load, List<ModularTraits.Active> traits) {
    }

    private static final Baked[] BAKED = new Baked[2];

    @Override
    public boolean built(ItemStack gun) {
        return GunSpec.of(gun).isPresent();
    }

    @Override
    public boolean trigger(ServerLevel level, Player shooter, ItemStack gun, float yaw, float pitch, long nowMs, boolean aimed, InteractionHand hand) {
        return GunShots.triggerFirearm(level, shooter, gun, yaw, pitch, nowMs, aimed, hand);
    }

    @Override
    public boolean triggerMob(ServerLevel level, LivingEntity mob, ItemStack gun, long nowGameTick, float yaw, float pitch) {
        return GunShots.triggerMobFirearm(level, mob, gun, nowGameTick, yaw, pitch);
    }

    @Override
    public GunFigures predict(Player player, ItemStack gun, Level level, Map<ResourceLocation, Material> materials, int hand) {
        GunSpec design = GunSpec.of(gun).orElse(null);
        if (design == null || GunHazard.isSpent(gun))
            return null;
        var next = GunAmmo.next(gun);
        if (next.isEmpty())
            return null;
        boolean last = GunAmmo.count(gun) == 1;
        Baked b = BAKED[hand];
        if (b == null || b.design != design || b.materials != materials || b.lastRound != last || !b.round.equals(next.get())) {
            var r = next.get();
            b = new Baked(design, materials, r, last, design.firing(GunAmmo.count(gun)).bake(materials, r),
                    r.load(materials), ModularTraits.allOf(gun));
            BAKED[hand] = b;
        }
        if (b.base.isPresent() && b.load.isPresent()) {
            var aimed = ModularTraits.gunAimed(player, gun, GunCondition.effective(b.base.get(), gun, level, b.load.get().ignitionTu()), b.traits);
            if (GunItem.dualWielding(player))
                aimed = aimed.oneHanded();
            return aimed.withCycle(1f / Haste.rate(player));
        }
        return null;
    }

    @Override
    public float sightZoom(ItemStack gun) {
        return GunSpec.of(gun).map(g -> g.sight().fov()).orElse(1f);
    }

    @Override
    public int raiseMs(ItemStack gun) {
        return GunSpec.of(gun).map(g -> g.sight().raiseMs()).orElse(1);
    }

    @Override
    public FireMode allow(ItemStack gun, FireMode set) {
        return GunSpec.of(gun).map(g -> g.layout().action().allow(set)).orElse(set);
    }

    @Override
    public FireMode next(ItemStack gun, FireMode current) {
        return GunSpec.of(gun).map(g -> g.layout().action().next(current)).orElse(current);
    }

    @Override
    public List<GunSound.Layer> sound(ItemStack gun, GunFigures figures, RandomSource random) {
        return GunSound.layers(figures, random);
    }

    @Override
    public SimpleParticleType muzzleParticle(ItemStack gun, GunFigures figures) {
        return ParticleTypes.SMOKE;
    }
}
