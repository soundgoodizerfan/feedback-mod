// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.robot;

/** Placeholder figures for robots (spec {@code FTuning.ROBOT_*}); none is tuned yet. */
public final class RobotTuning {

    private RobotTuning() {
    }

    /** Ticks between program steps: a robot's Controller runs at a reduced rate, its movement every tick. */
    public static final int STEP_TICKS = 4;
    /** Blocks per tick of a massless robot on its drive, and the mass (Mu) at which that halves. */
    public static final float SPEED_MAX = 0.14f, SPEED_HALF_MASS = 40f;
    /** Eu drawn per tick while moving, and per tick idle. */
    public static final float EU_MOVE_PER_TICK = 0.6f, EU_IDLE_PER_TICK = 0.02f;
    /** Eu to break a block, per unit of its hardness. */
    public static final float EU_PER_HARDNESS = 8f;
    /** Ticks to break a block, per unit of hardness, at a head strength of {@link #HEAD_ST_REF}. */
    public static final float DIG_TICKS_PER_HARDNESS = 20f;
    /** The head's yield strength that digs at the base rate, and the strength per unit of hardness below which it cannot break a block at all. */
    public static final float HEAD_ST_REF = 250f, HEAD_ST_PER_HARDNESS = 6f;
    /** Ticks a quarter turn, a place, a pick-up and a drop take. */
    public static final int TURN_TICKS = 8, PLACE_TICKS = 6, HANDLE_TICKS = 6;
    /** Blocks of position error a wheeled robot gathers per block rolled; an inertial unit keeps {@link #INERTIAL_DRIFT_SHARE} of it. */
    public static final float DRIFT_PER_BLOCK = 0.06f, INERTIAL_DRIFT_SHARE = 0.25f;
    /** Blocks within which a beacon corrects a robot's position; how far a rangefinder reaches. */
    public static final int BEACON_RANGE = 3, RANGE_MAX = 8;
    /** Speed of a legged and of a rotor robot against a wheeled one; the mass (Mu) above which a rotor cannot lift; Eu a tick to hover and to hold a chunk loaded. */
    public static final float LEGS_SPEED = 0.8f, ROTOR_SPEED = 0.7f, ROTOR_LIFT_MU = 60f, EU_HOVER_PER_TICK = 0.3f, EU_LOADER_PER_TICK = 1f;
    /**
     * Placeholder. What the brain circuit decides. Program size: {@code CARDS_BASE} cards with no brain, {@code CARDS_PER_ELEMENT} more for each
     * element on it (never over the Controller ceiling). Sense reach (a rangefinder's range, a beacon's) grows by {@code SENSE_GAIN x gain}; the
     * numeric senses (range, hardness) wobble by the brain's noise times the Controller's read jitter, a robot with no brain at full noise.
     * A leash (a fitted endernet upgrade bonded to a Data Port) carries the first {@code LEASH_CHANNELS_BASE x max(1, selectivity)} senses and
     * actions, and costs {@code EU_LEASH_PER_TICK} from the pack, as the brain's own heater does ({@code Circuit.power}). Places the robot
     * remembers: the lattice's memory, a robot within {@code PLACE_NEAR} blocks of the chosen one is at it.
     */
    public static final int CARDS_BASE = 6, CARDS_PER_ELEMENT = 5, LEASH_CHANNELS_BASE = 6;

    public static final float SENSE_GAIN = 1f, EU_LEASH_PER_TICK = 0.02f, PLACE_NEAR = 1.5f;

    /** A gas-cell robot: the mass (Mu) it can float, its speed against a wheeled one. It holds its height with no power at all, and drifts nowhere when its power dies. */
    public static final float GAS_CELL_LIFT_MU = 150f, GAS_CELL_SPEED = 0.4f;

    /** Blocks a tethered robot may roam from its reel. */
    public static final float TETHER_LENGTH = 12f;
    /** The fixed tool head strength of an arm, and the amps it asks of its line while it has a program. */
    public static final float ARM_HEAD_ST = 300f, ARM_AMPS = 0.5f;
    /** The amps a radio mast asks of its line, the blocks it reaches per amp delivered, the most it reaches per band step, and how far from either end noise is heard. */
    public static final float MAST_AMPS = 1f, RADIO_BLOCKS_PER_AMP = 20f, RADIO_BAND_CEILING = 48f;
    public static final int RADIO_NOISE_RANGE = 4;
    /**
     * Placeholder. A job board: the amps it asks of its line, the amps it needs to hear one robot (its bandwidth), the game ticks a claim
     * lives without a refresh, and the slots it holds.
     */
    public static final float BOARD_AMPS = 0.5f, BOARD_AMPS_PER_ROBOT = 0.5f;
    public static final int BOARD_CLAIM_TICKS = 600, BOARD_SLOTS = 16;
    /** Slots of cargo a robot carries. */
    public static final int CARGO = 9;
}
