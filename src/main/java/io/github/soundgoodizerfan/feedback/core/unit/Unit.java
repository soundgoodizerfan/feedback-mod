// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.unit;

/**
 * A physical quantity with a unit. Implemented by every type in this package.
 *
 * <h2>What this interface is for, and what it deliberately is not</h2>
 * It exists so that machinery which must work over <em>any</em> unit can be written once instead
 * of once per unit: codecs, wire formats, and the display layer. That is the whole of it. It is
 * not a base class, it carries no arithmetic, and nothing physical should ever be written against
 * {@code Unit} rather than against a concrete type -- a method taking a {@code Unit} has thrown
 * away exactly the information this package exists to keep.
 *
 * <h2>{@link #raw()} is an escape hatch, and it costs something</h2>
 * This is the honest trade in the design, so it is recorded rather than buried.
 * <p>
 * The guard these types provide has two layers. The strong one is the <b>parameter type</b>: a
 * {@link Conductance} cannot be passed where a {@link ThermalMass} is wanted, and no accessor
 * naming is involved. The weaker one is <b>accessor naming</b>, which catches the case where a
 * value has already been unwrapped to a float inside one method -- writing
 * {@code getLeak().workPerTu()} fails only because {@code Conductance} has no method by that
 * name. That second layer is real: it is what the thermal pass was verified against.
 * <p>
 * {@code raw()} punches through the second layer, because every unit answers to it. Two reasons
 * it is worth having anyway:
 * <ul>
 *   <li>Every units library has one, because serialization and display genuinely need the number
 *       and genuinely do not care what it means. The alternative is a per-unit copy of every
 *       codec and every formatter, which is the fixed cost this interface was added to remove.</li>
 *   <li>The first layer -- the parameter type -- is untouched by it, and that is the layer doing
 *       almost all of the work.</li>
 * </ul>
 * So the rule is a convention, and it is short: <b>hand-written physics uses the named accessor;
 * {@code raw()} belongs to codecs and to the display layer.</b> A {@code raw()} inside a machine
 * or a process is a smell, and reviewing for it is cheap because the name is deliberately blunt.
 *
 * <h2>Why it returns a float even though {@link Fu} counts in integers</h2>
 * Fu is cumulative discrete work -- it is an {@code int} on the item, an {@code int} in the
 * deformation table and a VAR_INT on the wire, and widening it here would be lying about the
 * model. What {@code raw()} promises is narrower than "the value": it promises <em>a float good
 * enough to draw and to compare</em>. Anything that must round-trip a Fu exactly uses
 * {@link Fu#value()} and the integer codec, and {@link Units} provides both flavours for that
 * reason.
 *
 * <h2>Adding a unit</h2>
 * See {@link Units} -- the checklist lives next to the machinery it refers to.
 */
public interface Unit {

    /**
     * This quantity as a plain float, for codecs and for display.
     * <p>
     * Not for physics. See the class javadoc: the blunt name is the point, and a call to this
     * from a machine, process or {@code core} class is almost always a mistake.
     */
    float raw();

    /**
     * Translation key for this unit's symbol alone -- {@code "Tu"}, {@code "Su"}, {@code "RPM"}.
     * <p>
     * A key rather than a literal because §17's symbols are player-visible text, and the mod does
     * not hard-code player-visible text anywhere else either. Composite readouts that want
     * {@code "9 / 14 Fu"} keep their own keys; this is for the generic
     * {@code <figure> <symbol>} case.
     */
    String unitKey();
}
