// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular;

import java.util.Map;
import java.util.Optional;

import net.minecraft.resources.ResourceLocation;

import io.github.soundgoodizerfan.feedback.process.Material;

/**
 * The physics of a spring-piston air gun, read from its design. A spring stores energy (its volume times its stock's yield
 * strength); the piston and barrel hand it to a pellet: the push reaching the pellet is what a barrel of that length and bore
 * keeps of it, and the pellet shares it with the piston's moving mass. The pellet is a plug of the bore, cut from a nugget of any
 * metal: its mass is its volume times that metal's density, never a named item. The receiver is the mount the spring is rated
 * against ({@link SpringSet}).
 *
 * @param energy     joules the spring stores, before any set.
 * @param pistonMass kilograms the piston moves.
 * @param boreMm     the bore, millimetres.
 * @param lengthM    the barrel's length, metres.
 * @param rating     joules the receiver holds before the spring takes a set.
 */
public record SpringGunSpec(float energy, float pistonMass, float boreMm, float lengthM, float rating) {

    public static final String SPRING = "spring", PISTON = "piston", BARREL = "barrel", RECEIVER = "receiver";

    /**
     * What one shot is.
     *
     * @param speed  blocks per tick at the muzzle.
     * @param joules the pellet's kinetic energy.
     * @param damage the hit it deals at the muzzle.
     * @param mass   the pellet's kilograms.
     */
    public record Shot(float speed, float joules, float damage, float mass) {
    }

    /** The spec of {@code design}, or empty if it lacks a spring, a piston, a barrel or a receiver, or a material is unknown. */
    public static Optional<SpringGunSpec> of(ModularDesign design, ModularType type, Map<ResourceLocation, Material> materials) {
        float energy = 0, rating = 0, piston = 0, bore = 0, length = 0;
        boolean spring = false, pist = false, barrel = false, receiver = false;
        for (ModularDesign.Part p : design.parts()) {
            ModularType.Role role = type.roles().get(p.role());
            Material m = materials.get(p.material());
            if (role == null || m == null)
                return Optional.empty();
            switch (role.name()) {
                case SPRING -> {
                    energy += ModularTuning.AIR_JOULES_PER_LS * role.volume(p) * ModularBake.strength(m);
                    spring = true;
                }
                case PISTON -> {
                    piston += ModularBake.mass(role, p, m);
                    pist = true;
                }
                case BARREL -> {
                    bore = role.axis(p, "bore");
                    length = role.axis(p, "barrel_length");
                    barrel = true;
                }
                case RECEIVER -> {
                    rating += ModularTuning.AIR_RATING_PER_LS * role.volume(p) * ModularBake.strength(m);
                    receiver = true;
                }
                default -> { }
            }
        }
        return spring && pist && barrel && receiver && bore > 0 && length > 0
                ? Optional.of(new SpringGunSpec(energy, piston, bore, length, rating)) : Optional.empty();
    }

    /** Share of the spring's energy that reaches the pellet and piston: a longer barrel keeps the push, a wider bore wastes it. */
    public float efficiency() {
        float len = lengthM / (lengthM + ModularTuning.AIR_LENGTH_HALF);
        float wide = boreMm / ModularTuning.AIR_BORE_HALF;
        return ModularTuning.AIR_ENVELOPE * len / (1f + wide * wide);
    }

    /** Litres of metal in one pellet of this bore. */
    public float pelletLitres() {
        float r = boreMm / 2f;
        return ModularTuning.AIR_PELLET_LENGTHS * (float) Math.PI * r * r * boreMm * 1e-6f;
    }

    /** How many pellets of this bore one nugget is worth. */
    public int perNugget() {
        return Math.max(1, (int) (ModularTuning.AIR_NUGGET_LITRES / pelletLitres()));
    }

    /** The shot of a pellet of {@code density} (kilograms per litre) from the spring with {@code set} of its energy lost. */
    public Shot shot(float density, float set) {
        float mass = pelletLitres() * density;
        float moving = mass + ModularTuning.AIR_PISTON_SHARE * pistonMass;
        float speed = (float) Math.sqrt(2f * energy * (1f - set) * efficiency() / Math.max(1e-6f, moving)) / 20f;
        speed = Math.min(speed, ModularTuning.AIR_SPEED_MAX);
        float joules = 0.5f * mass * (speed * 20f) * (speed * 20f);
        return new Shot(speed, joules, joules * ModularTuning.AIR_DAMAGE_PER_J, mass);
    }

    /** Ticks a wielder of strength 1 takes to cock the spring. */
    public float cockTicks() {
        return Math.min(ModularTuning.AIR_COCK_MAX_TICKS, Math.max(ModularTuning.AIR_COCK_MIN_TICKS,
                energy / ModularTuning.AIR_COCK_J_PER_TICK));
    }

    /** The spring's load over what the receiver holds: above 1 each cocking sets it, far above it snaps ({@link SpringSet}). */
    public float load(float set) {
        return rating > 0 ? energy * (1f - set) / rating : 0f;
    }
}
