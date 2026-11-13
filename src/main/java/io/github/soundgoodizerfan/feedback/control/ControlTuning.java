// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control;

import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import java.util.List;

/**
 * Tuning constants for the control code.
 * Every figure is a placeholder or invented unless its comment says it comes from the slice design.
 */
public final class ControlTuning {

    private ControlTuning() {
    }
    /**
     * Invented. The temperature at which the slice's bimetallic strip snaps over, in Tu.
     *
     * <h3>One figure, on the part, not on a dial</h3>
     * Philosophy 3 and 13: a strip is a piece of bent metal that trips where its two metals say
     * it trips. It has no setting, and wanting a different trip point means crafting a different
     * strip -- which is how this family grows and why the controller in slice 2 is a genuine
     * upgrade rather than a convenience.
     * <p>
     * Sat a third of the way up steel's 1420-1480 window rather than at either end, and the
     * figure was arrived at by simulating the loop rather than by taste. A large crucible under
     * full air climbs at 0.79 Tu/t and the strip reads every 20 ticks, so the loop oscillates
     * about 16 Tu either side of its trip point: 1440 gives 1424 to 1456, which fits. At 1425 the
     * bottom of the swing falls out of the window and the hold stalls for half of every cycle; at
     * 1470 the top of it does, and on a small crucible the top of the swing is 1599 and burns the
     * batch outright.
     */
    public static final Tu BIMETALLIC_TRIP_TU = new Tu(1440f);


    // --- control --------------------------------------------------------------------------

    /**
     * How long one wind of the Timer lasts, in ticks.
     * <p>
     * A short list of positions rather than a free number (§3). There is no correct value hiding
     * in here to be ground out -- the run being timed is not repeatable enough for one to exist
     * -- but a device with a scalar on it teaches players to hunt for one anyway, and that is a
     * habit worth not teaching.
     * <p>
     * The range is set against beat 1: a water wheel lands roughly a blow a second, and a plate
     * is five blows on the gentle crank. So the useful settings sit either side of that, and the
     * long ones exist mostly to demonstrate what overrun looks like.
     */
    public static final List<Integer> TIMER_SETTINGS = List.of(40, 100, 200, 400);


    // --- controller -------------------------------------------------------------------------

    /** A Controller's flat draw while turning -- reading and switching costs a little Su. */
    public static final Su CONTROLLER_LOAD_SU = new Su(2f);

    /**
     * A printed card's node cap. Not tuned to anything yet -- same honesty as
     * {@code DataNode.getMaxNodeLinks()}'s own comment.
     */
    public static final int CONTROLLER_MAX_NODES = 16;

    /** Variables a bare Controller can declare. Invented; a Memory Drum adds {@link #MEMORY_DRUM_VARIABLES}. */
    public static final int CONTROLLER_BASE_VARIABLES = 4;

    /** Variables one Memory Drum adds. Invented. */
    public static final int MEMORY_DRUM_VARIABLES = 4;

    /** Memory Drums one Controller takes. Invented. */
    public static final int MEMORY_DRUM_MAX = 3;

    /** Cards one Card Rack adds to the node cap. Invented. */
    public static final int CARD_RACK_NODES = 8;

    /** Card Racks one Controller takes. Invented. */
    public static final int CARD_RACK_MAX = 3;

    /** The named delay lengths in ticks: 1 s, 5 s, 30 s, 2 min, 10 min. Invented; any Controller offers these. */
    public static final int[] DELAY_NAMED_TICKS = {20, 100, 600, 2400, 12000};

    /** Shortest typed delay, in ticks (1 s); needs an Escapement. Invented. */
    public static final int DELAY_TYPED_MIN_TICKS = 20;

    /** Longest delay a card may carry, in ticks (1 h). Invented. */
    public static final int DELAY_MAX_TICKS = 72000;

    /** Change events a Delay card remembers; a fuller ring forgets the oldest. Invented. */
    public static final int DELAY_HISTORY_EVENTS = 32;

    /** Longest variable name, in characters. Invented. */
    public static final int VARIABLE_NAME_MAX = 12;

    /** Invented. Amps an Electric Controller and a Computer want from the line. */
    public static final float CONTROLLER_ELECTRIC_AMPS = 0.3f, COMPUTER_AMPS = 0.6f;

    /**
     * Placeholder. How far from a frame block, in blocks, a vehicle part may stand and still be
     * named as an offset from that frame by a card (vehicles spec section 5); farther, it is a
     * plain position.
     */
    public static final int CARD_FRAME_REACH = 48;

    /** Invented. Data wire (control/data/DataWires): the blocks a link may span on poor (copper) and good (redstone-chromium) wire, the links an end may carry before good wire quenches, and the ticks a quenched line stays dark. */
    public static final int WIRE_RANGE_POOR = 8, WIRE_RANGE_GOOD = 32, WIRE_CRITICAL_LINKS = 3, WIRE_QUENCH_TICKS = 200;
}
