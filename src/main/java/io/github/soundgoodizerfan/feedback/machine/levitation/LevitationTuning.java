// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.levitation;

/** Figures for the levitation plate and sleeve. */
public final class LevitationTuning {

    private LevitationTuning() {
    }

    /** Invented. Sable mass of one plate block (its data file {@code physics_block_properties/levitation_plate.json} says the same). */
    public static final double PLATE_SABLE_MASS = 0.04;

    /** Invented. Lift at full field and full diamagnetism with the plate touching the magnet, in plate weights: itself and as much again. */
    public static final double LIFT_AT_TOUCH_WEIGHTS = 2.0;

    /** Invented. Damping ratio of the plate's hover (1 is critical): high enough that a dropped stage settles in a bounce or two. */
    public static final double HOVER_DAMPING_RATIO = 0.7;

    /** Surface friction of a plate over no magnet: a block's ordinary 0.6. Over a full field it reaches {@link #SLAB_FRICTION_LIFTED}. */
    public static final float SLAB_FRICTION_RESTING = 0.6f;
    /** Ice's slipperiness: the nearest a floating surface comes to no friction in a block world. */
    public static final float SLAB_FRICTION_LIFTED = 0.98f;

    /** Ticks between sparks from a floating plate. */
    public static final int SPARK_INTERVAL_TICKS = 6;

    /** Invented. Share of a shaft, bearing or flywheel's drag a full-strength sleeve removes. */
    public static final float SLEEVE_DRAG_CUT = 0.9f;
}
