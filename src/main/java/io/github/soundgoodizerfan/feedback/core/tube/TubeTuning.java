// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.tube;

import io.github.soundgoodizerfan.feedback.core.unit.Pu;
import io.github.soundgoodizerfan.feedback.core.unit.Su;

/**
 * Tuning constants for the core/tube code.
 * Every figure is a placeholder or invented unless its comment says it comes from the slice design.
 */
public final class TubeTuning {

    private TubeTuning() {
    }

    // --- pneumatic tubes, pods, late tube upgrades, ender trickle (item logistics spec 4, 6) -----------

    /** Ticks a capsule takes to cross one tube block at the full-drive pressure, empty, straight, in a plain tube; invented. */
    public static final int TUBE_BASE_HOP_TICKS = 12;

    /** Run pressure (Pu) at which a capsule moves at the base speed; more is faster up to {@link #TUBE_MAX_DRIVE_FACTOR}; invented. */
    public static final float TUBE_FULL_DRIVE_PU = 2f, TUBE_MAX_DRIVE_FACTOR = 2f;

    /** Run pressure (Pu) under which a plain or lined tube cannot move a capsule (it sits pinned until pressure returns); invented. */
    public static final float TUBE_MIN_DRIVE_PU = 0.3f;

    /** Air (mB) a hop spends from the run's ledger: a capsule is pushed by air that then has to be replaced; invented. */
    public static final int TUBE_AIR_PER_HOP_MB = 15;

    /** The heaviest capsule (Mu) a narrow bore and a wide bore will pass; half of it doubles the hop time (the mass cost that keeps tubes honest against belts); invented. */
    public static final float TUBE_BORE_MAX_MU = 40f, TUBE_WIDE_BORE_MAX_MU = 400f;

    /** Speed multiplier of a lined tube (lower wall friction), of an evacuated one (no air drag) and of a powered linear-motor ring; invented. */
    public static final float TUBE_LINER_FACTOR = 1.5f, TUBE_EVAC_FACTOR = 3f, TUBE_LINEAR_FACTOR = 2f;

    /** Run pressure (Pu) at or under which an evacuated tube holds its vacuum; invented. */
    public static final float TUBE_EVAC_MAX_PU = 0.05f;

    /** Air (mB) a second leaks into an evacuated tube block from outside; invented. */
    public static final int TUBE_EVAC_LEAK_MB_PER_SECOND = 2;

    /** Inrush when an evacuated run loses its vacuum: reach (blocks), pull speed, damage, capsule stall (ticks). */
    public static final float TUBE_INRUSH_REACH = 3f, TUBE_INRUSH_PULL = 0.7f, TUBE_INRUSH_DAMAGE = 3f;

    public static final int TUBE_INRUSH_STALL_TICKS = 60;

    /** Time multiplier of a bend (any capsule) and, for a pod, of a bend and of a vertical run: corners and drops cost speed, not damage; invented. */
    public static final float TUBE_CORNER_FACTOR = 1.5f, POD_CORNER_FACTOR = 3f, POD_DROP_FACTOR = 2f;

    /** Ticks a blocked or stalled capsule waits before looking again; invented. */
    public static final int TUBE_STALL_RETRY_TICKS = 10;

    /** Mass (Mu) of an empty capsule, an empty pod and a rider; invented. */
    public static final float CARRIER_EMPTY_MU = 1f, POD_EMPTY_MU = 120f, POD_RIDER_MU = 75f;

    /** Slots in a carrier; invented. */
    public static final int CARRIER_SLOTS = 4;

    /** Ticks a loading station waits for a part-full buffer before it ships it; invented. */
    public static final int STATION_LOAD_IDLE_TICKS = 20;

    /** A vacuum pump removes this many mB of air a tick per RPM from the run in front of it; invented. */
    public static final float VACUUM_MB_PER_RPM = 0.4f;

    public static final Su VACUUM_LOAD_SU = new Su(25f);

    /** Current (A) a linear-motor ring asks at MV; invented. */
    public static final float LINEAR_MOTOR_AMPS = 2f;

    /** Mass (Mu) per minute a bonded ender pair may move, teleportant (mB) per Mu moved, the distance (blocks) that doubles the cost, and the distance counted for a pair in two dimensions; invented. */
    public static final float PAIR_CAP_MU_PER_MIN = 640f, PAIR_TELEPORTANT_MB_PER_MU = 0.5f, PAIR_DISTANCE_SCALE = 256f,
            PAIR_CROSS_DIMENSION_BLOCKS = 10000f;

    /** Teleportant (mB) an ender port can hold; invented. */
    public static final int PAIR_TANK_MB = 2000;

    /** Invented, ender spec 2.1. A link that carried something within this many ticks is running; if its feed then runs dry it destabilises: the stack in transit is lost and a hop of this many mB of unstable fluid goes off at the port. */
    public static final int PAIR_LIVE_TICKS = 200, PAIR_DRY_HOP_MB = 60;

    /** Stages a multi-stage sorting station chains, and the ticks each kind of head needs per load (the station runs at its slowest stage); invented. */
    public static final int SORT_STATION_STAGES = 3, SORT_STAGE_TICKS_MAGNETIC = 4, SORT_STAGE_TICKS_OPTICAL = 8,
            SORT_STAGE_TICKS_WEIGHER = 16;
}
