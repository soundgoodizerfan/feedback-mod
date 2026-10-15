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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import io.github.soundgoodizerfan.feedback.control.data.DataNodeRef;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;

/**
 * Pure graph functions -- no Minecraft world access beyond the {@link DataNodeRef}s a caller
 * hands in through {@code sensorReader}/{@code actuatorWriter}. Spec §2.4: no cycles, rejected
 * before a program is usable rather than at runtime; §2's whole point, a node's output is a pure
 * function of its inputs.
 */
public final class ProgramEvaluator {

    private ProgramEvaluator() {
    }

    /**
     * Node ids in dependency order, or empty if the graph contains a cycle. DFS with a
     * "currently visiting" set is enough to detect one -- a graph this small never needs
     * anything smarter.
     */
    public static Optional<List<Integer>> topologicalOrder(ProgramGraph graph) {
        Map<Integer, ProgramNode> byId = new HashMap<>();
        for (ProgramNode node : graph.nodes())
            byId.put(node.id(), node);

        List<Integer> order = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        Set<Integer> visiting = new HashSet<>();

        for (ProgramNode node : graph.nodes())
            if (!visited.contains(node.id())
                    && !visit(node.id(), byId, visited, visiting, order))
                return Optional.empty();

        return Optional.of(order);
    }

    private static boolean visit(int id, Map<Integer, ProgramNode> byId, Set<Integer> visited,
                                  Set<Integer> visiting, List<Integer> order) {
        if (visiting.contains(id))
            return false;
        if (visited.contains(id))
            return true;

        ProgramNode node = byId.get(id);
        if (node == null)
            return true; // a dangling wire is ProgramGraph#findError's problem, not a cycle

        visiting.add(id);
        for (int input : node.inputs())
            if (!visit(input, byId, visited, visiting, order))
                return false;
        visiting.remove(id);

        visited.add(id);
        order.add(id);
        return true;
    }

    /** Where an actuator card's value goes: its target, its channel, and a Boolean or a Float. */
    @FunctionalInterface
    public interface ActuatorWriter {
        void write(@org.jetbrains.annotations.Nullable DataNodeRef target, int channel, Object value);
    }

    /**
     * Runs every node once, in dependency order, and calls {@code actuatorWriter} for each
     * actuator card. {@code sensorReader} and {@code actuatorWriter} are the only places this
     * touches the world -- everything else is arithmetic over already-read numbers.
     */
    public static void evaluate(ProgramGraph graph, List<Integer> order,
                                 Function<SensorNode, Float> sensorReader, ActuatorWriter actuatorWriter) {
        Map<Integer, ProgramNode> byId = new HashMap<>();
        for (ProgramNode node : graph.nodes())
            byId.put(node.id(), node);
        Map<Integer, Object> values = new HashMap<>();
        for (int id : order) {
            ProgramNode node = byId.get(id);
            if (node == null)
                continue;
            Object value = switch (node) {
                case SensorNode n -> n.quantity() == Quantity.STATE
                        ? (Object) (sensorReader.apply(n) > 0.5f)
                        : sensorReader.apply(n);
                case ComparatorNode n -> {
                    float left = asFloat(values.get(n.leftInput()));
                    float right = n.rightInput() == ProgramNode.UNWIRED
                            ? n.literalDefault()
                            : asFloat(values.get(n.rightInput()));
                    yield n.op() == ComparatorNode.Compare.GREATER ? left > right : left < right;
                }
                case LogicNode n -> n.op() == LogicNode.Gate.AND
                        ? asBoolean(values.get(n.leftInput())) && asBoolean(values.get(n.rightInput()))
                        : asBoolean(values.get(n.leftInput())) || asBoolean(values.get(n.rightInput()));
                case MathNode n -> n.op().apply(asFloat(values.get(n.leftInput())),
                        n.rightInput() == ProgramNode.UNWIRED ? n.literalDefault() : asFloat(values.get(n.rightInput())));
                case ActuatorNode n -> {
                    Object in = values.get(n.input());
                    actuatorWriter.write(n.target(), n.channel(), in instanceof Float ? in : (Object) asBoolean(in));
                    yield null;
                }
            };
            values.put(id, value);
        }
    }

    private static float asFloat(Object value) {
        return value instanceof Float f ? f : 0f;
    }

    private static boolean asBoolean(Object value) {
        return value instanceof Boolean b && b;
    }
}
