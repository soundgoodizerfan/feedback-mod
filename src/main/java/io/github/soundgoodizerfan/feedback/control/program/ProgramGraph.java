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
package io.github.soundgoodizerfan.feedback.control.program;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A whole printed program -- what a Punch Card actually carries. Persisted and network-synced as
 * one {@code FDataComponents.PROGRAM_GRAPH} component; the working copy edited in a Programmer is
 * the same type, just not yet written back to an item. {@code variables} are the named memory
 * cells its {@link ReadNode}/{@link WriteNode} cards refer to.
 */
public record ProgramGraph(List<ProgramNode> nodes, List<ProgramVariable> variables) {

    public static final ProgramGraph EMPTY = new ProgramGraph(List.of(), List.of());

    public ProgramGraph(List<ProgramNode> nodes) {
        this(nodes, List.of());
    }

    public static final Codec<ProgramGraph> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ProgramNode.CODEC.listOf().fieldOf("nodes").forGetter(ProgramGraph::nodes),
            ProgramVariable.CODEC.listOf().optionalFieldOf("variables", List.of()).forGetter(ProgramGraph::variables)
    ).apply(instance, ProgramGraph::new));

    public static final StreamCodec<ByteBuf, ProgramGraph> STREAM_CODEC = StreamCodec.composite(
            ProgramNode.STREAM_CODEC.apply(ByteBufCodecs.list()), ProgramGraph::nodes,
            ProgramVariable.STREAM_CODEC.apply(ByteBufCodecs.list()), ProgramGraph::variables,
            ProgramGraph::new);

    public Optional<ProgramNode> find(int id) {
        return nodes.stream().filter(n -> n.id() == id).findFirst();
    }

    /** The declared type of variable {@code name}, or empty if the graph declares none of that name. */
    public Optional<ValueType> variableType(String name) {
        return variables.stream().filter(v -> v.name().equals(name)).map(ProgramVariable::type).findFirst();
    }

    /** This graph with {@code nodes} in place of its own, variables kept. */
    public ProgramGraph withNodes(List<ProgramNode> nodes) {
        return new ProgramGraph(nodes, variables);
    }

    /**
     * Why a card would be rejected at print time (spec's "rejected at print time, not
     * runtime"), or empty if the graph is fine to burn onto a card. The translated message lives
     * under {@code feedback.programmer.error.<key>}.
     */
    public Optional<String> findError(ControllerLimits limits) {
        Optional<String> editError = findEditError(limits);
        if (editError.isPresent())
            return editError;

        Set<Integer> ids = new HashSet<>();
        for (ProgramNode node : nodes)
            ids.add(node.id());

        for (ProgramNode node : nodes)
            if (node instanceof SensorNode n && n.target() == null
                    || node instanceof ActuatorNode a && a.target() == null)
                return Optional.of("unlinked_card");

        for (ProgramNode node : nodes)
            for (int input : node.inputs()) {
                if (input == ProgramNode.UNWIRED)
                    return Optional.of("unwired_input");
                if (!ids.contains(input))
                    return Optional.of("dangling_wire");
            }

        // A cycle is legal only through a DelayNode (its output is history); see topologicalOrder.
        if (ProgramEvaluator.topologicalOrder(this).isEmpty())
            return Optional.of("cycle");

        return Optional.empty();
    }

    /**
     * What a half-built graph must already satisfy for a Programmer to accept it as its working
     * copy -- the subset of {@link #findError} that is never merely "not finished yet": card and
     * variable counts against {@code limits}, unique ids, valid variable names and references,
     * delay lengths, and every wired port carrying the right kind of signal.
     */
    public Optional<String> findEditError(ControllerLimits limits) {
        if (nodes.size() > limits.nodes())
            return Optional.of("too_many_nodes");
        if (variables.size() > limits.variables())
            return Optional.of("too_many_variables");

        Set<String> names = new HashSet<>();
        for (ProgramVariable variable : variables) {
            if (!ProgramVariable.validName(variable.name()))
                return Optional.of("bad_variable_name");
            if (!names.add(variable.name()))
                return Optional.of("duplicate_variable");
        }

        Set<Integer> ids = new HashSet<>();
        for (ProgramNode node : nodes) {
            // UNWIRED is the "no wire" marker, so no card may carry it as its own id.
            if (node.id() < 0)
                return Optional.of("bad_id");
            if (!ids.add(node.id()))
                return Optional.of("duplicate_id");
            if (node instanceof ComparatorNode c && !Float.isFinite(c.literalDefault())
                    || node instanceof MathNode m && !Float.isFinite(m.literalDefault()))
                return Optional.of("bad_literal");
            if (node instanceof ReadNode r && !names.contains(r.variable())
                    || node instanceof WriteNode w && !names.contains(w.variable()))
                return Optional.of("unknown_variable");
            if (node instanceof DelayNode d) {
                if (d.ticks() < FTuning.DELAY_TYPED_MIN_TICKS || d.ticks() > FTuning.DELAY_MAX_TICKS)
                    return Optional.of("bad_delay");
                if (!d.named() && !limits.typedDelays())
                    return Optional.of("typed_delay_locked");
            }
        }

        for (ProgramNode node : nodes) {
            List<Integer> ports = node.ports();
            for (int slot = 0; slot < ports.size(); slot++) {
                Optional<ProgramNode> from = find(ports.get(slot));
                if (from.isPresent()) {
                    Optional<String> error = wireError(node, slot, from.get());
                    if (error.isPresent())
                        return error;
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Why {@code from} may not feed {@code target}'s input {@code slot}, or empty if it may --
     * spec's port types. A Number comes from a reading {@link SensorNode}, a {@link MathNode}, or a
     * Number-typed {@link DelayNode}/{@link ReadNode}; a Boolean from a {@link ComparatorNode}, a
     * {@link LogicNode}, a switch's own state read back, or a Boolean-typed delay/read. Comparator
     * and math ports take Numbers, logic ports and a write's trigger Booleans, a delay its own type, a
     * write's value port its variable's type, and an actuator either -- a number only moves an
     * analog vehicle part, at run time. Two {@link SensorNode}s feeding the same comparator must
     * also share a {@link io.github.soundgoodizerfan.feedback.instrument.Quantity} once both are linked
     * (an unlinked side is simply not checked yet); a math card's output carries no quantity.
     */
    public Optional<String> wireError(ProgramNode target, int slot, ProgramNode from) {
        boolean number = isNumber(from), bool = isBoolean(from);
        if (target instanceof ComparatorNode comparator) {
            if (!number)
                return Optional.of("type_mismatch");
            int otherId = slot == 0 ? comparator.rightInput() : comparator.leftInput();
            if (from instanceof SensorNode fromSensor && find(otherId).orElse(null) instanceof SensorNode other
                    && other.quantity() != null && fromSensor.quantity() != null
                    && other.quantity() != fromSensor.quantity())
                return Optional.of("quantity_mismatch");
            return Optional.empty();
        }
        if (target instanceof MathNode)
            return number ? Optional.empty() : Optional.of("type_mismatch");
        if (target instanceof ActuatorNode)
            return number || bool ? Optional.empty() : Optional.of("type_mismatch");
        if (target instanceof DelayNode delay)
            return (delay.type() == ValueType.NUMBER ? number : bool) ? Optional.empty() : Optional.of("type_mismatch");
        if (target instanceof WriteNode write) {
            if (slot == 1)
                return bool ? Optional.empty() : Optional.of("type_mismatch");
            Optional<ValueType> type = variableType(write.variable());
            if (type.isEmpty())
                return Optional.of("unknown_variable");
            return (type.get() == ValueType.NUMBER ? number : bool) ? Optional.empty() : Optional.of("type_mismatch");
        }
        return target.ports().isEmpty() || !bool ? Optional.of("type_mismatch") : Optional.empty();
    }

    private boolean isNumber(ProgramNode node) {
        return node instanceof MathNode || node instanceof SensorNode s && s.quantity() != Quantity.STATE
                || node instanceof DelayNode d && d.type() == ValueType.NUMBER
                || node instanceof ReadNode r && variableType(r.variable()).orElse(null) == ValueType.NUMBER;
    }

    private boolean isBoolean(ProgramNode node) {
        return node instanceof ComparatorNode || node instanceof LogicNode
                || node instanceof SensorNode s && s.quantity() == Quantity.STATE
                || node instanceof DelayNode d && d.type() == ValueType.BOOLEAN
                || node instanceof ReadNode r && variableType(r.variable()).orElse(null) == ValueType.BOOLEAN;
    }
}
