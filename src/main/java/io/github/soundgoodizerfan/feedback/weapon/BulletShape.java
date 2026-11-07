// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.weapon;

import com.mojang.serialization.Codec;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/**
 * What a round's projectile is shaped like (guns spec section 6): a discrete choice inside the
 * projectile, read at the shot, never an item id. Every figure is a multiplier on what the
 * projectile's own materials already give, so the shape trades one thing for another and none is
 * best: a pointed bullet flies far and goes through armour but hits soft; a hollow point is the
 * reverse. Placeholders, retunable here.
 *
 * <p>A shape is the <em>form</em> of the projectile item ({@link #form}): a blank is cast in a mould
 * or forged to shape, and the press seats what it holds. A nugget is buckshot.
 *
 * @param form      the material form of the item that is this shape: the press reads the shape off the item in the bullet slot.
 * @param shot      thrown as pellets from a smooth bore.
 * @param drag      speed lost to the air, against a round nose's 1.
 * @param damage    share of the energy that becomes hurt; a hollow point opens up and gives more.
 * @param pierce    the hardness the projectile presents to armour and stone, against the metal's own.
 * @param ease      how readily it breaks a brittle block: the momentum it needs is divided by this.
 * @param glance    how readily it glances off a block: the angle it turns at is multiplied by this.
 * @param wear      bore erosion, against a round nose's 1.
 * @param pelletMm  shot: pellet diameter (0 reads {@link FTuning#GUN_SHOT_PELLET_MM}).
 * @param maxPellets shot: most pellets a shell throws.
 * @param spread    shot: width of the pattern, against a buckshot's 1.
 */
public enum BulletShape implements StringRepresentable {
    ROUND_NOSE("bullet", false, 1f, 1f, 1f, 1f, 1f, 1f, 0, 1, 1f),
    ROUND_BALL("round_ball", false, 1.35f, 1f, 0.85f, 1f, 1.4f, 0.7f, 0, 1, 1f),
    FLAT_NOSE("flat_nose_bullet", false, 1.15f, 1.1f, 0.9f, 1.5f, 0.6f, 1f, 0, 1, 1f),
    POINTED("pointed_bullet", false, 0.65f, 0.9f, 1.4f, 1f, 0.8f, 1.2f, 0, 1, 1f),
    HOLLOW_POINT("hollow_point_bullet", false, 1f, 1.45f, 0.55f, 0.8f, 1f, 1f, 0, 1, 1f),
    BIRDSHOT("birdshot", true, 1f, 1f, 1f, 1f, 1f, 0.5f, 2.5f, 24, 1.2f),
    BUCKSHOT("nugget", true, 1f, 1f, 1f, 1f, 1f, 0.8f, 0, FTuning.GUN_SHOT_MAX_PELLETS, 1f),
    LARGE_SHOT("large_shot", true, 1f, 1f, 1f, 1f, 1f, 1f, 12f, 6, 0.8f),
    FLECHETTE("flechette", true, 0.7f, 0.9f, 1.5f, 1f, 0.5f, 1.2f, 3.5f, 16, 0.5f);

    public static final Codec<BulletShape> CODEC = StringRepresentable.fromEnum(BulletShape::values);

    private final String form;
    private final boolean shot;
    private final float drag, damage, pierce, ease, glance, wear, pelletMm, spread;
    private final int maxPellets;

    BulletShape(String form, boolean shot, float drag, float damage, float pierce, float ease, float glance, float wear,
                float pelletMm, int maxPellets, float spread) {
        this.form = form;
        this.shot = shot;
        this.drag = drag;
        this.damage = damage;
        this.pierce = pierce;
        this.ease = ease;
        this.glance = glance;
        this.wear = wear;
        this.pelletMm = pelletMm;
        this.maxPellets = maxPellets;
        this.spread = spread;
    }

    public String form() { return form; }
    public boolean shot() { return shot; }
    public float drag() { return drag; }
    public float damage() { return damage; }
    public float pierce() { return pierce; }
    public float ease() { return ease; }
    public float glance() { return glance; }
    public float wear() { return wear; }
    public float spread() { return spread; }
    public int maxPellets() { return maxPellets; }

    /** Pellet diameter, mm; a bullet shape has none. */
    public float pelletMm() {
        return pelletMm > 0 ? pelletMm : FTuning.GUN_SHOT_PELLET_MM;
    }

    /** The shape a projectile form is, or empty if the form is no projectile's. */
    public static java.util.Optional<BulletShape> ofForm(String form) {
        for (BulletShape s : values())
            if (s.form.equals(form))
                return java.util.Optional.of(s);
        return java.util.Optional.empty();
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
