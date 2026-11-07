// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.weapon;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.process.Material;

/**
 * What a packed charge does when it goes up, from its blend and its casing: one rule for the placed
 * block ({@link DemolitionChargeBlockEntity}) and for the figures a charge is designed against
 * (the Blueprint Table, {@code ModularBake}), so the plan and the blast cannot drift.
 */
public final class Blast {

    private Blast() {
    }

    /** How much the casing holds the gas in until it lets go: 1 bare, more for a stronger metal, to a cap. */
    public static float confinement(Material casing) {
        return confinement(casing, 1f);
    }

    /** As {@link #confinement(Material)}, the casing's own bonus scaled by its shape ({@code confine}: a tight casing 1.4, a scored one 0.7). */
    public static float confinement(Material casing, float shape) {
        if (casing == null)
            return 1f;
        return 1f + shape * Math.min(FTuning.DEMO_CONFINE_MAX, casing.mechanical().yieldStrength().map(y -> y.value()).orElse(0f)
                * FTuning.DEMO_CONFINE_PER_ST);
    }

    /** Explosion power: the square root of the burned potential, times the casing's confinement, capped. */
    public static float power(Cartridge.Blend blend, Material casing) {
        if (blend.coil())
            return 0f;
        return Math.min(FTuning.DEMO_POWER_MAX,
                FTuning.DEMO_POWER_PER_SQRT * (float) Math.sqrt(blend.potential() * blend.burned()) * confinement(casing));
    }

    /** Whether what did not burn goes up as fire: a fuel-rich charge. */
    public static boolean fire(Cartridge.Blend blend) {
        return blend.burned() < FTuning.DEMO_FIRE_BELOW_BURNED;
    }

    /** The role letters of a charge's optional parts; the casing is the type's wear role. */
    public static final String FUSE_ROLE = "W", PRIMER_ROLE = "R", PAYLOAD_ROLE = "P";

    /** Whether {@code role} of a charge holds propellant filler (not the casing, fuse, primer or payload). */
    public static boolean isFiller(io.github.soundgoodizerfan.feedback.modular.ModularType type, String role) {
        return !role.equals(type.wearRole()) && !role.equals(FUSE_ROLE) && !role.equals(PRIMER_ROLE) && !role.equals(PAYLOAD_ROLE);
    }

    /**
     * What a charge will do when it goes up: its power, whether it throws fire, the casing (null if bare), the fuse, the
     * damage its fragments do (casing hardness times the casing's {@code frag}, plus the payload's), and whether its
     * payload is luminous and blinds (a flash).
     */
    public record Packed(float power, boolean fire, Material casing, int fuseTicks, boolean impact, float fragDamage, boolean flash) {
    }

    /** A charge item read as the placed block reads its contents. Empty for an item that is not a charge. */
    public static java.util.Optional<Packed> packed(net.minecraft.world.item.ItemStack made) {
        var design = io.github.soundgoodizerfan.feedback.modular.ModularStacks.design(made);
        var type = design == null ? null : io.github.soundgoodizerfan.feedback.modular.ModularTypes.known().get(design.type());
        if (type == null)
            return java.util.Optional.empty();
        return read(design, type, io.github.soundgoodizerfan.feedback.process.MaterialTable.get().materials());
    }

    /**
     * Read a charge design: the one rule behind the placed block, the thrown charge and the Blueprint Table's figures.
     * Options are variants of the casing ({@code confine}, {@code frag}), the fuse ({@code ticks}, {@code impact}) and
     * the primer ({@code burn_add}, {@code fire}); the payload is raw stock counted by item.
     */
    public static java.util.Optional<Packed> read(io.github.soundgoodizerfan.feedback.modular.ModularDesign design,
                                                  io.github.soundgoodizerfan.feedback.modular.ModularType type,
                                                  java.util.Map<net.minecraft.resources.ResourceLocation, Material> materials) {
        java.util.List<Cartridge.Charge> filler = new java.util.ArrayList<>();
        Material casing = null;
        int ticks = FTuning.DEMO_FUSE_TICKS;
        boolean impact = false, forceFire = false, flash = false;
        float confine = 1f, frag = 1f, burnAdd = 0f, payloadFrag = 0f;
        for (var part : design.parts()) {
            var role = type.roles().get(part.role());
            var v = role == null ? java.util.Optional.<io.github.soundgoodizerfan.feedback.modular.ModularType.Variant>empty() : role.variant(part);
            Material m = materials.get(part.material());
            if (part.role().equals(type.wearRole())) {
                casing = m;
                confine = v.map(x -> x.params().getOrDefault("confine", 1f)).orElse(1f);
                frag = v.map(x -> x.params().getOrDefault("frag", 1f)).orElse(1f);
            } else if (part.role().equals(FUSE_ROLE)) {
                ticks = v.map(x -> Math.round(x.params().getOrDefault("ticks", (float) FTuning.DEMO_FUSE_TICKS))).orElse(ticks);
                impact = v.map(x -> x.params().getOrDefault("impact", 0f) > 0f).orElse(false);
            } else if (part.role().equals(PRIMER_ROLE)) {
                burnAdd = v.map(x -> x.params().getOrDefault("burn_add", 0f)).orElse(0f);
                forceFire = v.map(x -> x.params().getOrDefault("fire", 0f) > 0f).orElse(false);
            } else if (part.role().equals(PAYLOAD_ROLE)) {
                if (m != null) {
                    payloadFrag += m.hardness().value() * part.count() * FTuning.GRENADE_PAYLOAD_FRAG;
                    flash |= m.traits().contains(Material.Trait.LUMINOUS);
                }
            } else
                filler.add(new Cartridge.Charge(part.material(), part.count()));
        }
        Material cased = casing;
        float c = confine, f = frag, add = burnAdd, pf = payloadFrag;
        boolean fireAlways = forceFire, flashed = flash, hit = impact;
        int fuse = ticks;
        return Cartridge.Blend.of(materials, filler).filter(b -> !b.coil()).map(b -> {
            Cartridge.Blend primed = new Cartridge.Blend(b.potential(), Math.min(1f, b.burned() + add), b.burnRate(), b.ignitionTu(), b.total(), b.coil(), b.chemical());
            float damage = Math.min(FTuning.GRENADE_FRAG_DAMAGE_MAX, (cased == null ? 0f
                    : cased.hardness().value() * FTuning.GRENADE_FRAG_DAMAGE_PER_HARDNESS * f) + pf);
            float power = Math.min(FTuning.DEMO_POWER_MAX, FTuning.DEMO_POWER_PER_SQRT
                    * (float) Math.sqrt(primed.potential() * primed.burned()) * confinement(cased, c));
            return new Packed(power, fireAlways || fire(primed), cased, fuse, hit, damage, flashed);
        });
    }

    /**
     * What the shrapnel and the flash of a charge going up at {@code at} do: everything alive within reach is hurt by
     * the fragments (more the nearer) and a luminous payload blinds those who can see it.
     */
    public static void scatter(net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.Entity source,
                               @org.jetbrains.annotations.Nullable net.minecraft.world.entity.Entity owner,
                               net.minecraft.world.phys.Vec3 at, float power, Packed p) {
        if (p.fragDamage() <= 0f && !p.flash())
            return;
        float reach = Math.max(power * FTuning.GRENADE_FRAG_REACH_PER_POWER, p.flash() ? FTuning.GRENADE_FLASH_REACH : 0f);
        for (var body : level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                new net.minecraft.world.phys.AABB(at, at).inflate(reach))) {
            float near = 1f - (float) Math.sqrt(body.distanceToSqr(at)) / reach;
            if (near <= 0f)
                continue;
            if (p.fragDamage() > 0f)
                body.hurt(level.damageSources().explosion(source, owner), p.fragDamage() * near);
            if (p.flash())
                body.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.BLINDNESS,
                        Math.round(FTuning.GRENADE_FLASH_TICKS * near)));
        }
    }
}
