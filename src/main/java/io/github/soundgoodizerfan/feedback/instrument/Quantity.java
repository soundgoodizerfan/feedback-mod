// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.instrument;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.ByIdMap;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/**
 * Something a player might want a number for.
 *
 * <h2>Why measurement is never general</h2>
 * A thermometer says nothing about how flattened an ingot is, and calipers say nothing about how
 * fast a shaft turns. Precision is not a property a player accumulates — it is bought one
 * quantity at a time, and owning one instrument must never quietly sharpen every readout in the
 * game.
 * <p>
 * {@link StringRepresentable} plus the two codecs below exist for
 * {@code control.program.SensorNode} -- a controller program has to know which quantity a wired
 * sensor reads, so a comparator can refuse to compare Su against Tu instead of silently mixing
 * them (controller spec §2.3's typed ports, one level more specific than Number/Boolean).
 */
public enum Quantity implements StringRepresentable {

    /** Mechanical work beaten into a workpiece, in Fu. */
    WORK("work"),
    /** How fast a run is turning, in RPM. */
    SPEED("speed"),
    /** What a network is carrying, in Su. */
    LOAD("load"),
    /** How hot something is, in Tu. */
    TEMPERATURE("temperature"),
    /**
     * How sound a machine still is, as a fraction of as-built.
     *
     * <h3>The one quantity here with no unit</h3>
     * Every other entry names a unit from philosophy 17. Condition is a ratio of a machine to its
     * own former self, which is dimensionless on purpose -- so a resolution for it is a fraction
     * too, and 0.05 means "to the nearest five per cent" rather than to the nearest anything.
     * Nobody should mint {@code Wu} for it.
     */
    CONDITION("condition"),
    /**
     * Which voltage band a line is in: a word (LV, MV...), never a number. The wire form is the ordinal;
     * the resolution is a whole number of bands.
     */
    BAND("band"),
    /** Current, in amps. */
    CURRENT("current"),
    /** A part's dimensions, in mm: a machined barrel's bore and wall (spec 12). */
    LENGTH("length"),
    /**
     * Whether a switch is on: an actuator's own state, read back. The one boolean here, and the
     * one no instrument reads -- it exists so a controller can see what it last switched, which a
     * deadband needs (philosophy 14). A program reads it as a boolean, never a number.
     */
    STATE("state"),
    /**
     * How far a control is pushed, 0 to 1: a Driver's Seat key, ramped while held. A fraction, like
     * condition, and no instrument reads it -- the driver is the instrument.
     */
    CONTROL("control"),
    /** A made thing's mass, in Mu: a balance scale reads a modular item's. Appended, never inserted. */
    MASS("mass"),
    /** How hard an edge or face is: a scratch file set reads it as a ladder of files, never a number finer than its files. */
    HARDNESS("hardness"),
    /**
     * Any figure no quantity above covers yet: a modular item's balance, swing and bite, a coat's
     * thickness. No instrument reads it, so its only tier is the truth tier -- which is the point:
     * every measurable figure has one even before anything a player can make reaches it. When an
     * instrument for one of these arrives, that one gets a quantity of its own. Appended.
     */
    FIGURE("figure"),
    /** Air or steam pressure, in Pu. Appended, never inserted. */
    PRESSURE("pressure"),
    /** Ore blocks within a survey meter's reach, a count. Appended, never inserted. */
    ORE("ore"),
    /** Stored experience, in XPu: what an enchanting session costs. No instrument reads it yet; truth tier only. Appended, never inserted. */
    EXPERIENCE("experience"),
    /** Metal deposited on a mass probe, in Qu: its crystal's resonance falls as the coat grows. Appended, never inserted. */
    DEPOSIT("deposit"),
    /** How strongly a part answers its stimulus, 0 to 1: a whisker spot's sensitivity. A component tester reads it. Appended, never inserted. */
    RESPONSE("response"),
    /** A food's water, 0 to 1 of its mass: a fraction like condition, so its resolution is a fraction too. Appended, never inserted. */
    MOISTURE("moisture"),
    /** A food's live-organism load, 0 to 1 as {@code FoodState} holds it (a probability when eaten). No unit minted. Appended, never inserted. */
    PATHOGEN("pathogen"),
    /** A food's stable-toxin concentration, in Qu/mB (a mole stand-in per litre of the food); a test strip reads it. Appended, never inserted. */
    TOXIN("toxin"),
    /** Dose rate, in Ru: a Geiger counter counts it as clicks, so its reading is as noisy as the clicks. Appended, never inserted. */
    RADIATION("radiation"),
    /** Dose a body has taken, in Ru x s: a dosimeter reads it. Appended, never inserted. */
    DOSE("dose"),
    /** Height above the world's floor, in blocks: a flight sensor reads it. Appended, never inserted. */
    ALTITUDE("altitude"),
    /** Speed through the air, in blocks per second (no wind is modelled, so the body's own speed). Appended, never inserted. */
    AIRSPEED("airspeed"),
    /** Tilt of a body from level, in degrees, signed: a flight sensor's pitch or roll. Appended, never inserted. */
    ATTITUDE("attitude"),
    /** The level an XP container stands at, fractional: the potential its points are held at. An XP meter reads it. Appended, never inserted. */
    XP_LEVEL("xp_level"),
    /** A secondary cell's state of charge, 0 to 1 of its capacity: a fraction like condition. A hydrometer reads it. Appended, never inserted. */
    CHARGE("charge"),
    /** A reactor core's neutron flux, as a fraction of full power. A flux meter reads it. Appended, never inserted. */
    FLUX("flux");

    private final String key;

    Quantity(String key) {
        this.key = key;
    }

    @Override
    public String getSerializedName() {
        return key;
    }

    public static final Codec<Quantity> CODEC = StringRepresentable.fromEnum(Quantity::values);

    public static final StreamCodec<ByteBuf, Quantity> STREAM_CODEC =
            ByteBufCodecs.idMapper(ByIdMap.continuous(Quantity::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO), Quantity::ordinal);
}
