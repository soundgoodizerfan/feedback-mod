/*
 * Feedback -- a Minecraft technology mod.
 * Copyright (C) 2026 soundgoodizerfan
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */

/**
 * The units of philosophy 17, as types the compiler can tell apart.
 *
 * <h2>What this package is for</h2>
 * Every physical figure in the mod was a {@code float}, and once both the rotation and thermal
 * models existed that had become a hazard rather than a tidiness complaint. Ten distinct
 * quantities shared one spelling; several of them sat two lines apart in {@code FTuning}; and the
 * arithmetic that mixes them is four or five lines long in each system. The mistake would not
 * have been caught by a test, because a swapped coefficient still produces a plausible curve --
 * it would have been caught by a playtest, weeks later, as "the crucible feels wrong" or "the
 * water wheel is twitchy".
 *
 * <h2>What a unit type does and deliberately does not do</h2>
 * Each is a thin record with one component, <em>named for its dimension</em>. Two layers of guard
 * come out of that:
 * <ul>
 *   <li>The <b>parameter type</b>, which is the strong one. A {@link Conductance} cannot be passed
 *       where a {@link ThermalMass} is wanted, an {@link Su} cannot be passed where an {@link Rpm}
 *       is wanted, and no naming is involved.</li>
 *   <li>The <b>accessor name</b>, which catches the case where a value has already been unwrapped
 *       inside one method. {@code getLeak().workPerTu()} and
 *       {@code getDragSuPerRpm().suTicksPerRpm()} both fail to compile, and both were verified by
 *       being written on purpose.</li>
 * </ul>
 * Base units name their component {@code value}, because the type name is already the dimension.
 * Compounds name theirs for the dimension, because {@link ThermalMass} and {@link Conductance} --
 * or {@link Drag} and {@link Inertia} -- are indistinguishable floats until the accessor says
 * otherwise, and those are exactly the pairs that get swapped.
 * <p>
 * There is deliberately <b>no arithmetic on these types</b>. The alternative was considered and
 * rejected: an algebra of {@code Tu.minus} and {@code Work.over(ThermalMass)} would catch
 * dimensional errors in intermediate expressions too, but it costs a pile of invented operators
 * and extra delta types to express a handful of lines of physics, and it would stop
 * {@link Heat#tick} and {@code RotationNetwork.tick} looking like the equations they are. So the
 * types guard the <em>plumbing</em> -- interfaces, tuning constants, the seams between systems,
 * which is where a value gets handed to the wrong parameter -- and the arithmetic is done in
 * plain floats, unwrapped at the point of use.
 * <p>
 * The convention that follows, and it is a convention rather than something enforced:
 * <b>unwrap inline, never into a local.</b> {@code body.getLeak().workPerTickPerTu()} inside an
 * expression is safe; {@code float leak = ...; float mass = ...;} on two lines puts the two back
 * within swapping distance. Where a class must hold a figure across methods -- a block entity's
 * stored temperature, a network's live load -- it holds a <b>primitive field and wraps at the
 * accessor</b>. That is also the shape the JIT wants: a wrapper in a field outlives the method
 * that made it and is the one case escape analysis cannot eliminate.
 *
 * <h2>What is here</h2>
 * <table border="1">
 *   <caption>Live units</caption>
 *   <tr><th>Type</th><th>Dimension</th><th>What it measures</th></tr>
 *   <tr><td>{@link Tu}</td><td>Tu</td><td>temperature -- a state, never an amount</td></tr>
 *   <tr><td>{@link TuRate}</td><td>Tu/t</td><td>how fast a temperature is moving</td></tr>
 *   <tr><td>{@link ThermalMass}</td><td>Work/Tu</td><td>what it costs to heat a body by one Tu</td></tr>
 *   <tr><td>{@link Conductance}</td><td>Work/t/Tu</td><td>how readily heat crosses a boundary</td></tr>
 *   <tr><td>{@link Su}</td><td>Su</td><td>rotational load and supply</td></tr>
 *   <tr><td>{@link Rpm}</td><td>RPM</td><td>rotational speed, signed for direction</td></tr>
 *   <tr><td>{@link Fu}</td><td>Fu</td><td>cumulative work in a workpiece -- an integer</td></tr>
 *   <tr><td>{@link St}</td><td>St</td><td>the force behind one blow</td></tr>
 *   <tr><td>{@link Drag}</td><td>Su/RPM</td><td>bearing friction: a steady-state cost</td></tr>
 *   <tr><td>{@link Inertia}</td><td>Su&#183;t/RPM</td><td>resistance to changing speed: a transient one</td></tr>
 *   <tr><td>{@link Pu}</td><td>Pu</td><td>pressure -- stored air over a fixed volume</td></tr>
 *   <tr><td>{@link Vdrag}</td><td>block&#178;</td><td>a vehicle's aerodynamic drag area -- not {@link Drag}</td></tr>
 * </table>
 * {@link Unit} is what they have in common and {@link Units} is the machinery -- codecs, wire
 * formats, formatting -- that makes the next one cheap. <b>The checklist for adding a unit lives
 * in {@link Units}.</b>
 *
 * <h2>Three things this exercise found, which is the argument for having done it</h2>
 * <ol>
 *   <li><b>A vessel's leak and a fire's conductance are the same dimension.</b> Both are
 *       {@code Work/t/Tu}, and {@link Heat#equilibrium} has been adding them together correctly
 *       all along without anything in the code saying they were addable. One type now, in two
 *       roles. Naming a leak as a conductance to the room is also just true.</li>
 *   <li><b>The bellows answers a question about force with a volume of air.</b>
 *       {@code StrengthPair} is typed in {@link St}, and the bellows returns an air figure through
 *       it one-to-one. That was invisible while both were floats. Resolved by giving air a unit,
 *       {@link Mb}, and typing the pair per machine -- the linkage now hands over a stroke's throw
 *       and gearing, not a force. A force-to-air conversion was the other option and would have
 *       run backwards: the weaker long throw is the one that moves more air.</li>
 *   <li><b>{@link Tu} covers both a temperature and a difference between two.</b> Strictly a state
 *       and an interval. Not worth a separate type -- the only interval in the model is an
 *       instrument's resolution -- but it is the first place the scheme is knowingly loose, and
 *       the same looseness now applies to every base unit here.</li>
 * </ol>
 *
 * <h2>What has no type, and why</h2>
 * <ul>
 *   <li><b>{@code Work}</b> -- generic energy. Nothing stores a quantity of it; it appears only
 *       inside the dimensions of {@link ThermalMass} and {@link Conductance}, and as intermediate
 *       terms inside {@link Heat#tick} that never leave the method. A type with no variable to
 *       hold would be scaffolding.</li>
 *   <li><b>Condition</b> -- machine wear. Dimensionless on purpose: a fraction of as-built spec,
 *       so it multiplies the spec sheet directly. {@code CLAUDE.md} is explicit that wear is a
 *       percentage and not a unit, and minting one would be the mod arguing with itself.</li>
 *   <li><b>Ticks</b> -- durations, intervals, hold times. Minecraft's own unit, used raw
 *       everywhere in the ecosystem, and wrapping it would fight every vanilla signature we touch
 *       for no case where a tick count is confusable with anything here.</li>
 *   <li><b>Ratios and other bare multipliers</b> -- genuinely dimensionless. Hardness is <em>not</em>
 *       one of them: it is typed {@link St}, because it is the floor a blow's force must clear, so
 *       {@code Fu per blow = St / hardness} is St over St -- a dimensionless count of Fu, which is
 *       exactly what a whole-number {@link Fu} is.</li>
 *   <li><b>{@code mB}, {@code Mu}, {@code Qu}, {@code Eu}</b> -- named in philosophy 17
 *       and not yet in any code. They arrive with the systems that need them; the standing rule is
 *       to create a thing when there is something to put in it, and {@link Units} exists so that
 *       arriving is cheap.</li>
 * </ul>
 *
 * <h2>Scope of the retype</h2>
 * The simulation is fully typed: {@code core/thermal}, {@code core/rotation}, the machines, the
 * items and the display layer. So are the <b>datapack records</b> ({@code Deformation},
 * {@code Removal}, {@code ThermalProcess}, {@code Quench}, {@code Fuel}, {@code Casting}), through
 * {@link Units#codec} and {@link Units#streamCodec}, which keep the on-disk JSON a bare number.
 * {@code Work}, process conditions and tick counts stay untyped for the reasons above.
 */
package io.github.soundgoodizerfan.feedback.core.unit;

import io.github.soundgoodizerfan.feedback.core.thermal.Heat;
