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

    /** Files a Computer's internal disk holds. Invented. */
    public static final int INTERNAL_DISK_FILES = 16;

    /** The most bytes (of a file's compressed saved form) one file may take on a medium: removable media carry files in an item component. Invented. */
    public static final int FILE_MAX_BYTES = 16384;

    /** The longest name a file keeps. */
    public static final int FILE_NAME_MAX = 32;

    /** How long a placed internal disk may go unclaimed in a loaded chunk before its Computer is taken to be gone, in ticks. */
    public static final int COMPUTER_ORPHAN_TICKS = 200;

    /** Data nodes a bare Controller binds. Invented; each Card Rack adds {@link #CARD_RACK_LINKS}. */
    public static final int CONTROLLER_BASE_LINKS = 8;

    /** Data nodes one Card Rack lets a Controller bind. Invented. */
    public static final int CARD_RACK_LINKS = 4;

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

    /**
     * Placeholder. A host with a circuit slot reads its sensors through the circuit: a quantity (not a two-state reading) is multiplied by
     * {@code 1 + u x noise x this}, u uniform in -1 to 1 and noise the circuit's (a host with none reads full noise). The heater of a tubed
     * circuit is topped up every {@link #CIRCUIT_HEAT_EVERY_TICKS} ticks.
     */
    public static final float CONTROLLER_READ_JITTER = 0.04f;

    public static final int CIRCUIT_HEAT_EVERY_TICKS = 5;

    /**
     * Placeholder. A Controller or Computer riding in a vehicle (a Sable body) is an autopilot host: its program holds {@code VEHICLE_CARDS_BASE} cards
     * with no circuit and {@code VEHICLE_CARDS_PER_ELEMENT} more for each element on the circuit seated in it, as a robot's brain does, never over the
     * Controller ceiling; a Card Rack adds nothing there.
     */
    public static final int VEHICLE_CARDS_BASE = 6, VEHICLE_CARDS_PER_ELEMENT = 5;

    // --- endernet, layer 1 (entangled pairs); placeholders ---

    /** Amps (at MV) a Data Port, and an endernet upgrade on an Electric Controller or Computer, draw while bonded: standing power at each end. */
    public static final float ENDERNET_PORT_AMPS = 0.1f, ENDERNET_UPGRADE_AMPS = 0.1f;

    /** What a Data Port carries: signals (one path search that crosses the pair) a minute of bandwidth, and teleportant mB a signal at no distance (scaled by the pair's distance as ender ports are). */
    public static final float ENDERNET_SIGNALS_PER_MIN = 3600f, ENDERNET_MB_PER_SIGNAL = 0.01f;

    /** mB of teleportant a Data Port's tank holds. */
    public static final int ENDERNET_PORT_TANK_MB = 500;

    /**
     * Endernet layers 2 and 3 (placeholders). A channel is a number the player tunes: 1 with no circuit, more for the selectivity of a resonator
     * on the host's or port's circuit, never over {@code ENDERNET_MAX_CHANNELS}. A channel's bandwidth is one cap shared by everything on it:
     * {@code ENDERNET_CHANNEL_SIGNALS_PER_MIN} signals a minute. An address is a number from 1 to {@code ENDERNET_ADDRESS_MAX} (0 is none).
     * A packet costs one signal per {@code PACKET_BYTES_PER_SIGNAL} bytes (at least one), each paid as any crossing is. A message is cut to
     * {@code MESSAGE_MAX} characters and a file may hold {@code FILE_MAX_NODES} nodes ({@code FILE_BYTES_PER_NODE} bytes each). An inbox holds
     * {@code INBOX_MAX} mails: a packet to a full one is lost. The directory lists {@code DIRECTORY_MAX} addresses. A bus slot value holds
     * {@code BUS_STALE_TICKS} and is rewritten when it is half that old.
     */
    public static final int ENDERNET_CHANNELS = 1, ENDERNET_MAX_CHANNELS = 16, ENDERNET_ADDRESS_MAX = 99, BUS_SLOTS = 4, BUS_STALE_TICKS = 1200,
            PACKET_BYTES_PER_SIGNAL = 16, MESSAGE_MAX = 120, FILE_MAX_NODES = 64, FILE_BYTES_PER_NODE = 16, INBOX_MAX = 16, DIRECTORY_MAX = 12;
    public static final float ENDERNET_CHANNEL_SIGNALS_PER_MIN = 1200f;

    /** The Network app: hops walked past the far end and entries listed, and how often (ticks) a watching Computer re-lists. */
    public static final int NETWORK_MAX_HOPS = 6, NETWORK_MAX_ENTRIES = 24, NETWORK_REFRESH_TICKS = 20;
    /** Most sensors a Computer shows from its own links, with no program reading them; placeholder. */
    public static final int ATTACHED_MAX = 8;

    /** Invented. Amps an Electric Controller and a Computer want from the line. */
    public static final float CONTROLLER_ELECTRIC_AMPS = 0.3f, COMPUTER_AMPS = 0.6f;

    /**
     * Placeholder. How far from a frame block, in blocks, a vehicle part may stand and still be
     * named as an offset from that frame by a card (vehicles spec section 5); farther, it is a
     * plain position.
     */
    public static final int CARD_FRAME_REACH = 48;

    /** Invented. Data wire (control/data/DataWires): the blocks a link may span on poor (copper) and good (redstone-chromium) wire, the links an end may carry before good wire quenches, and the ticks a quenched line stays dark. */
    /** Links per end a wire carries per unit of its material's ampacity (the redstone filament's 1.74 gives about 3, the superconductor's 6.25 about 11). */
    public static final float WIRE_LINKS_PER_AMPACITY = 1.75f;
    public static final int WIRE_RANGE_POOR = 8, WIRE_RANGE_GOOD = 32, WIRE_CRITICAL_LINKS = 3, WIRE_QUENCH_TICKS = 200;
    /** Placeholder. Loss in dB a fibre link may carry before its reading starts to degrade, with a perfect crystal at both ends. */
    public static final float FIBRE_LOSS_BUDGET_DB = 20f;
    /** Placeholder. The most a clean fibre run reaches, however pure the glass (blocks): silica glass would otherwise reach for ever. */
    public static final float FIBRE_CAP_BLOCKS = 512f;
    /** Placeholder. A terminal's loss budget scales by {@code FLOOR + SLOPE x rectifying} of its crystal (the weaker end of the pair counts). */
    public static final float FIBRE_CRYSTAL_FLOOR = 0.4f, FIBRE_CRYSTAL_SLOPE = 0.6f;
    /** Past this many times its clean reach a fibre reading is lost; between 1 and 2 it rounds to two figures, after that to one. */
    public static final float FIBRE_DEAD_RATIO = 8f, FIBRE_ONE_FIGURE_RATIO = 2f;
    /** Placeholder. Standing current of an optical terminal with a crystal seated (amps, at its LV band). */
    public static final float OPTICAL_TERMINAL_AMPS = 0.05f;
}
