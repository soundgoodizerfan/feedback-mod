// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular;

import java.util.Map;
import java.util.Optional;

import net.minecraft.resources.ResourceLocation;

import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
import io.github.soundgoodizerfan.feedback.process.Material;

/**
 * The physics of an electric gun, read from its design: a barrel of one of three kinds hands a charge from a swappable battery to a
 * slug (coil, rail) or to a vaporised nugget (plasma). Nothing ignites, and nothing here is a dial: what a shot takes from the cell and
 * what it gives back are fixed by the build, bounded by the cell's charge and the receiver's rating.
 *
 * <ul>
 *   <li><b>Coil:</b> {@code stages} windings along the bore each add {@code EGUN_STAGE_J}; stages past what the barrel's length can fit
 *       are mostly wasted (switching losses). The slug must be magnetic ({@code separation.magnetic}).</li>
 *   <li><b>Rail:</b> the rails take what their conductor is rated for; the slug is any conductor. Longer rails keep the push.</li>
 *   <li><b>Plasma:</b> an arc channel vaporises a conductor nugget: the vaporisation energy of its mass is the floor, and what comes
 *       out is a short ray, not a slug.</li>
 * </ul>
 *
 * @param kind       which barrel.
 * @param boreMm     the bore, millimetres.
 * @param lengthM    the barrel's length, metres.
 * @param stages     coil stages (1 for the others).
 * @param energyIn   joules a shot takes from the cell at full draw.
 * @param ampsRating amps the barrel's conductor carries before it wears.
 * @param band       the voltage band the receiver's insulation holds off.
 * @param barrelMass kilograms of the barrel.
 */
public record ElectricGunSpec(Kind kind, float boreMm, float lengthM, int stages, float energyIn, float ampsRating, VoltageBand band,
                              float barrelMass) {

    public enum Kind {
        COIL, RAIL, PLASMA
    }

    public static final String BARREL = "barrel", RECEIVER = "receiver";

    /**
     * What one shot is.
     *
     * @param speed  blocks per tick at the muzzle (0 for a plasma ray).
     * @param joules energy delivered.
     * @param damage the hit at the muzzle.
     * @param mass   the slug's kilograms (the vaporised nugget's for a plasma shot).
     */
    public record Shot(float speed, float joules, float damage, float mass) {
    }

    /** The spec of {@code design}, or empty if it lacks a barrel or a receiver or a material is unknown. */
    public static Optional<ElectricGunSpec> of(ModularDesign design, ModularType type, Map<ResourceLocation, Material> materials) {
        Kind kind = Kind.COIL;
        float bore = 0, length = 0, amps = 0, mass = 0;
        int stages = 1;
        VoltageBand band = VoltageBand.LV;
        boolean barrel = false, receiver = false;
        float conductivity = 0;
        for (ModularDesign.Part p : design.parts()) {
            ModularType.Role role = type.roles().get(p.role());
            Material m = materials.get(p.material());
            if (role == null || m == null)
                return Optional.empty();
            switch (role.name()) {
                case BARREL -> {
                    String name = role.variant(p).map(ModularType.Variant::name).orElse("coil_barrel");
                    kind = name.startsWith("rail") ? Kind.RAIL : name.startsWith("plasma") ? Kind.PLASMA : Kind.COIL;
                    bore = role.axis(p, "bore");
                    length = role.axis(p, "barrel_length");
                    if (kind == Kind.COIL)
                        stages = Math.max(1, Math.round(role.axis(p, "stages")));
                    conductivity = m.ampacity().orElse(0f);
                    amps = conductivity * role.volume(p) * ModularTuning.EGUN_AMPS_PER_LS;
                    mass = ModularBake.mass(role, p, m);
                    barrel = true;
                }
                case RECEIVER -> {
                    band = m.electrical().insulation().orElse(VoltageBand.LV);
                    receiver = true;
                }
                default -> { }
            }
        }
        if (!barrel || !receiver || bore <= 0 || length <= 0 || conductivity <= 0)
            return Optional.empty();
        float in = switch (kind) {
            case COIL -> stages * ModularTuning.EGUN_STAGE_J;
            case RAIL -> amps * ModularTuning.EGUN_RAIL_J_PER_AMP;
            case PLASMA -> ModularTuning.EGUN_ARC_J * (bore / ModularTuning.EGUN_BORE_HALF) * (bore / ModularTuning.EGUN_BORE_HALF);
        };
        return Optional.of(new ElectricGunSpec(kind, bore, length, stages, in, amps, band, mass));
    }

    /** Share of the energy taken that reaches the projectile. */
    public float efficiency() {
        float len = lengthM / (lengthM + ModularTuning.EGUN_LENGTH_HALF);
        float wide = boreMm / ModularTuning.EGUN_BORE_HALF;
        float bore = 1f / (1f + wide * wide);
        return switch (kind) {
            case COIL -> {
                // Stages the barrel can fit work; the rest cost switching losses and add little.
                float fit = Math.max(1f, lengthM / ModularTuning.EGUN_STAGE_SPACING_M);
                float useful = Math.min(stages, fit) + ModularTuning.EGUN_STAGE_WASTE * Math.max(0f, stages - fit);
                yield ModularTuning.EGUN_COIL_EFF * (useful / stages) * len * bore;
            }
            case RAIL -> ModularTuning.EGUN_RAIL_EFF * len * bore;
            case PLASMA -> ModularTuning.EGUN_PLASMA_EFF;
        };
    }

    /** Litres of metal in one slug of this bore. */
    public float slugLitres() {
        float r = boreMm / 2f;
        return ModularTuning.EGUN_SLUG_LENGTHS * (float) Math.PI * r * r * boreMm * 1e-6f;
    }

    /** How many slugs one nugget is worth; a plasma shot takes the whole nugget. */
    public int perNugget() {
        return kind == Kind.PLASMA ? 1 : Math.max(1, (int) (ModularTuning.EGUN_NUGGET_LITRES / slugLitres()));
    }

    /** Joules a shot needs at least: a plasma bolt's vaporisation floor for a nugget of {@code density} kg per litre. */
    public float floorJ(float density) {
        return kind == Kind.PLASMA ? ModularTuning.EGUN_NUGGET_LITRES * density * ModularTuning.EGUN_VAPOR_J_PER_KG : 0f;
    }

    /** The shot of a slug (or nugget) of {@code density} kilograms per litre when the cell gave {@code fraction} of the full draw. */
    public Shot shot(float density, float fraction) {
        float out = energyIn * Math.max(0f, fraction) * efficiency();
        if (kind == Kind.PLASMA) {
            float mass = ModularTuning.EGUN_NUGGET_LITRES * density;
            float joules = Math.max(0f, out - floorJ(density));
            return new Shot(0f, joules, joules * ModularTuning.EGUN_PLASMA_DAMAGE_PER_J, mass);
        }
        float mass = slugLitres() * density;
        float speed = (float) Math.sqrt(2f * out / Math.max(1e-6f, mass)) / 20f;
        speed = Math.min(speed, ModularTuning.EGUN_SPEED_MAX);
        float joules = 0.5f * mass * (speed * 20f) * (speed * 20f);
        return new Shot(speed, joules, joules * ModularTuning.EGUN_DAMAGE_PER_J, mass);
    }

    /** The current a shot pulls from a cell at {@code volts}: the energy over the volts and the pulse length. */
    public float amps(float volts) {
        return energyIn / Math.max(0.1f, volts * ModularTuning.EGUN_PULSE_S);
    }

    /** The current over what the barrel's conductor is rated for: above 1 each shot wears it, far above it fuses. */
    public float load(float volts) {
        return ampsRating > 0f ? amps(volts) / ampsRating : 0f;
    }
}
