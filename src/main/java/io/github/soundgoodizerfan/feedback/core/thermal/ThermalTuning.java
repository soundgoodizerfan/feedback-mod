// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.thermal;

import io.github.soundgoodizerfan.feedback.core.unit.Conductance;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.core.unit.TuRate;
import java.util.List;

/**
 * Tuning constants for the core/thermal code.
 * Every figure is a placeholder or invented unless its comment says it comes from the slice design.
 */
public final class ThermalTuning {

    private ThermalTuning() {
    }

    // --- thermal ---------------------------------------------------------------------------

    /**
     * Invented. The temperature everything decays towards, in Tu -- <em>by default</em>.
     *
     * <h3>A fallback, not the figure</h3>
     * The room a body sits in is {@code AmbientTable.at(level)}: a datapack table keyed by
     * dimension, so a Nether foundry sits in warmer air than an overworld one. This is what an
     * unlisted dimension reads, what a block entity not yet in a level starts at, and the floor of
     * the food-suitability ramp, which has no level to ask. Biomes are deliberately not a key --
     * nobody moves a factory two degrees; a dimension is a decision.
     */
    public static final Tu AMBIENT_TU = new Tu(20f);

    /**
     * How fast a loose workpiece sheds heat, in Tu per tick, cooling in a straight line.
     *
     * <h3>What this number actually sets</h3>
     * It sets the size of the player's workshop. A steel ingot leaves the crucible around 1450 Tu
     * and hot working stops below 900 Tu, so the walk from fire to anvil is
     * {@code 550 / rate} ticks -- about fourteen seconds here. Halve the rate and placement stops
     * mattering; double it and the hammer has to be bolted to the crucible.
     * <p>
     * Straight-line rather than exponential, which was the first version. See
     * {@link io.github.soundgoodizerfan.feedback.core.thermal.Heat#cooled} for the argument -- briefly, an
     * exponential never arrives, needs an arbitrary floor to stop it, and hides the whole working
     * window in a flat tail. A constant rate makes "you have fourteen seconds" literally true,
     * which is what the player is budgeting against.
     * <p>
     * Deliberately one figure for every item rather than per-material. Specific heat is real and
     * TerraFirmaCraft gives every item its own; the test here is whether it creates a choice, and
     * with one hot material in the slice it does not. It becomes worth having the moment a player
     * must decide <em>which</em> of two hot things to carry first.
     */
    public static final TuRate ITEM_COOLING_TU_PER_TICK = new TuRate(1.5f);

    /**
     * Invented. Below this, a workpiece is close enough to ambient to stop being described as hot,
     * and its heat components are dropped.
     *
     * <h3>Why a threshold survived the switch to linear cooling</h3>
     * It is no longer load-bearing -- straight-line cooling reaches ambient exactly, so nothing
     * would linger forever. It stays because components are part of a stack's identity: an ingot
     * at 21 Tu and one at exactly 20 Tu are different items and will not merge, so the player's
     * chest fills with piles of one. Clearing slightly early makes a cooled ingot byte-identical
     * to one that was never heated.
     */
    public static final Tu WARM_TU = new Tu(60f);

    /**
     * Invented. How readily heat crosses from a fire into the vessel above it, in Work per tick
     * per Tu of difference.
     *
     * <h3>Why heat flows on a difference rather than at a flat rate</h3>
     * A flat {@code Work/t} was the first thing tried and it is wrong in a way that matters: with
     * it, a vessel's final temperature is {@code fire output / leak}, so insulating a crucible
     * raises the temperature it settles at <em>without limit</em>. Insulation became a trap
     * rather than an upgrade, which contradicts the slice.
     * <p>
     * Driving the flow on {@code (fire - vessel)} fixes it the way the real relationship does: a
     * vessel approaches its fire's temperature and can never pass it. Insulation then does
     * exactly what it should -- it gets you closer to the flame and holds you there, and the only
     * way past the flame is a hotter flame.
     * <p>
     * It also pays for the slice's lava for free. Lava is a fire fixed at 1200 Tu, so a crucible
     * over lava sits just under 1200 Tu forever: perfectly stable, and permanently too cool for
     * steel. No rule had to be written for that.
     */
    public static final Conductance FIRE_CONDUCTANCE = new Conductance(0.6f);

    /**
     * Invented. How fast a vessel bleeds heat to the room, in Work per tick per Tu above ambient.
     * One figure for every vessel: what differs between a small crucible and a large one is mass,
     * not surface.
     */
    public static final Conductance VESSEL_LEAK = new Conductance(0.025f);

    /**
     * Invented. The share of the gap between the Hearth and a piece of stock beside it that closes
     * each tick: heat flows on the difference, so stock approaches the pan and never passes it.
     * Item cooling is {@link #ITEM_COOLING_TU_PER_TICK} (1.5 Tu/t), so stock beside a pan settles
     * 1.5 / 0.03 = 50 Tu under it.
     */
    public static final float HEARTH_STOCK_GRIP = 0.03f;

    /**
     * Fluid temperature. Invented. The share of the gap to its surroundings a tank's
     * contents close each tick (a tank, not a pipe: lines add no loss). A steam engine runs only on
     * steam at least {@code STEAM_ENGINE_MIN_TU} hot and sends it out {@code STEAM_ENGINE_EXHAUST_DROP_TU}
     * cooler (not below ambient), so a second engine can take the first's exhaust while it is still
     * above that line, and a condenser takes whatever is left.
     */
    public static final float FLUID_RELAX_PER_TICK = 0.002f;

    /**
     * SLICE: lava is 1200 Tu and it does not cool. Not slowly. At all.
     *
     * <h3>No explanation is offered, deliberately</h3>
     * Philosophy 2 says the fantastical cannot be a footnote, and in this slice it lives in the
     * most mundane place available -- the fuel. A bucket of lava under a crucible is an infinite,
     * perfectly stable heat source that is <b>too cool for steel and impossible to turn off</b>,
     * and the player has walked past it since day one without asking. There is no lore entry and
     * no analysis machine. There is a player who now owns a thermometer and has started pointing
     * it at things, which is the correct first step of §2's observe-then-exploit and the whole of
     * somebody else's slice.
     * <p>
     * Sat deliberately between a bare charcoal fire and the steel window, so it is genuinely
     * useful and genuinely not enough.
     */
    public static final Tu FIRE_TU_LAVA = new Tu(1200f);

    // Left a bare float on purpose. Instrument#resolution answers for every Quantity -- Fu as
    // well as Tu -- so its return type cannot be a temperature. The unit lives in the name here,
    // which is the honest place for it when the seam it crosses is quantity-agnostic.
    // --- readouts, thermal ------------------------------------------------------------------
    /**
     * Above this, the eye stops distinguishing anything, in Tu.
     *
     * <h3>A free sense has a range, like any other instrument</h3>
     * Philosophy 10 gives instruments six properties and range is the first of them, so it would be
     * strange for the one apparatus every player owns to have none. Past brilliant white, hotter
     * simply looks the same -- which matters in beat 2, because iron's spoil point at 1540 Tu is
     * <em>above</em> this. The player cannot see themselves crossing the line that ruins the
     * batch. That is not a trick; it is why the thermometer exists, and it is the difference
     * between a mod that hides a number and one that models why you cannot have it.
     */
    public static final Tu MAX_VISIBLE_TU = new Tu(1600f);

    /**
     * Where the free, qualitative temperature bands fall, in Tu -- each entry is the <em>top</em>
     * of a band, and {@link io.github.soundgoodizerfan.feedback.client.Readout} names them in order.
     *
     * <h3>Why they read as colours, and why there are this many</h3>
     * These are the bands a blacksmith actually works to, and that is not decoration -- it is the
     * honest answer to what a player's own senses give them. You cannot feel 1450 Tu; you can see
     * that iron has gone from bright red to orange, and you can be badly wrong about which.
     * <p>
     * The first version had six wide bands and steel's entire 60 Tu window sat inside one of them,
     * which made the free sense useless rather than coarse. TerraFirmaCraft uses eleven, and the
     * reason is worth stating: the bands should be fine enough that a player can <em>get close</em>
     * by eye and never fine enough to land a 60 Tu window reliably. That is the gap the
     * thermometer is sold into, and a gap that is merely "hot or not" is not a gap, it is a wall.
     * So the bands crowd where the work happens and are wide where nothing does.
     */
    public static final List<Tu> HEAT_BAND_TOPS = List.of(
            new Tu(200f),    // warm
            new Tu(480f),    // hot
            new Tu(580f),    // faint red
            new Tu(730f),    // dark red
            new Tu(930f),    // bright red
            new Tu(1100f),   // orange
            new Tu(1300f),   // yellow
            new Tu(1400f),   // pale yellow
            new Tu(1500f),   // white
            MAX_VISIBLE_TU);

    /** Block light level a glowing body gives in each free-sense band, coldest first (placeholders, tune by play): none below faint red, brilliant white full. */
    public static final int[] INCANDESCENCE_LIGHT = {0, 0, 2, 4, 6, 8, 10, 12, 14, 15};


    // --- hazards: hot contact, scalding, shock ------------------------------------------------

    /**
     * Invented. Three named bands for what a hot thing does to bare skin, anchored on figures the
     * mod already has: water boils (pain), wood chars at the organics' oxidation point (burn), and
     * the first glow of the eye scale (sear). Named bands, never a damage-per-Tu dial -- philosophy
     * 3 -- so a player learns "that is a sear" and not a slope.
     */
    public static final Tu CONTACT_PAIN_TU = new Tu(100f);


    // --- offline catch-up --------------------------------------------------------------------

    /**
     * Invented. A block back from an unloaded chunk is brought up to date only if it was gone at
     * least this many ticks. A chunk that flickered out and in is not worth the arithmetic.
     */
    public static final int CATCHUP_MIN_TICKS = 20;


    // --- heat facing, the arc furnace and its electrodes -------------------------------------------

    /** Invented. Share of a source's heat (above ambient) that reaches a block on a side next to its facing; behind it is none. */
    public static final float HEAT_SIDE_SHARE = 0.4f;

    /** Invented. Ticks between pushes of a source's heat into the items and entities in the space it reaches. */
    public static final int HEAT_PUSH_INTERVAL = 5;

    /** Invented. Pushes between burns of a living thing standing in that space (5 x 4 = a second). */
    public static final int HEAT_SCORCH_EVERY = 4;

    /** Invented. Share of the gap an item in a pushed-to inventory closes per push: {@link #HEARTH_STOCK_GRIP} compounded over the interval. */
    public static final float HEAT_PUSH_ITEM_GAIN = 1f - (float) Math.pow(1f - HEARTH_STOCK_GRIP, HEAT_PUSH_INTERVAL);
}
