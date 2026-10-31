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
import java.util.Arrays;
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
 * hands in through {@code sensorReader}/{@code actuatorWriter}. Spec §2.4: no cycles except through a
 * {@link DelayNode}, rejected before a program is usable rather than at runtime; §2's whole point, a node's output is a pure
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
        // A delay's output is history, not a function of this step's input: its input edge is not
        // a dependency, which is what lets a wire cycle pass through one.
        if (!(node instanceof DelayNode))
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
     * The memory a program runs against: variables and delay history. The evaluator never keeps
     * any itself. {@link #read} must answer with the value from before this step's writes
     * ({@link #write} may be buffered until {@link #endStep}), so a Read sees a Write one step later.
     */
    public interface State {
        /** The variable's value at the start of this step, a Float or a Boolean. */
        Object read(String variable);

        /** Sets the variable; takes effect for reads from the next step. */
        void write(String variable, Object value);

        /** The delay card's output this step: its input as it was {@code ticks} ago. */
        Object delayed(DelayNode node);

        /** This step's input of the delay card, to remember. */
        void record(DelayNode node, Object input);

        void endStep();

        /** No memory: reads give zero or false, writes and history go nowhere. */
        State NONE = new State() {
            @Override
            public Object read(String variable) {
                return 0f;
            }

            @Override
            public void write(String variable, Object value) {
            }

            @Override
            public Object delayed(DelayNode node) {
                return node.type().zero();
            }

            @Override
            public void record(DelayNode node, Object input) {
            }

            @Override
            public void endStep() {
            }
        };
    }

    /** One sensor's reading for a step; {@code index} is its place in {@link Plan#sensors()}. */
    @FunctionalInterface
    public interface SensorReader {
        float read(SensorNode node, int index);
    }

    public static void evaluate(ProgramGraph graph, List<Integer> order,
                                 Function<SensorNode, Float> sensorReader, ActuatorWriter actuatorWriter) {
        evaluate(graph, order, sensorReader, actuatorWriter, State.NONE);
    }

    /** Convenience over {@link Plan}: compiles the plan afresh, so callers that run often hold a {@link Plan} instead. */
    public static void evaluate(ProgramGraph graph, List<Integer> order,
                                 Function<SensorNode, Float> sensorReader, ActuatorWriter actuatorWriter, State state) {
        new Plan(graph, order).evaluate((node, i) -> sensorReader.apply(node), actuatorWriter, state);
    }

    /**
     * A graph compiled for repeated evaluation: node positions, input positions and scratch space
     * are built once, so {@link #evaluate} allocates nothing of its own but the boxed values a
     * step passes to {@link State} and {@link ActuatorWriter}. Not thread-safe (one scratch array).
     */
    public static final class Plan {
        private final ProgramGraph graph;
        private final ProgramNode[] nodes;
        private final int[] order;
        /** Per node position: the position feeding port 0 and port 1, or -1 (unwired or dangling). */
        private final int[] left, right;
        /** Per node position: the declared type of a Read/Write's variable. */
        private final ValueType[] variableType;
        private final SensorNode[] sensors;
        private final int[] sensorIndex;
        private final int[] delayPositions;
        private final Object[] values;

        /** {@code order} is {@link #topologicalOrder}'s result for {@code graph}. */
        public Plan(ProgramGraph graph, List<Integer> order) {
            this.graph = graph;
            nodes = graph.nodes().toArray(new ProgramNode[0]);
            int n = nodes.length;
            Map<Integer, Integer> position = new HashMap<>();
            for (int i = 0; i < n; i++)
                position.put(nodes[i].id(), i);
            this.order = new int[order.size()];
            for (int i = 0; i < this.order.length; i++)
                this.order[i] = position.getOrDefault(order.get(i), -1);
            left = new int[n];
            right = new int[n];
            variableType = new ValueType[n];
            sensorIndex = new int[n];
            List<SensorNode> sensorList = new ArrayList<>();
            List<Integer> delayList = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                List<Integer> ports = nodes[i].ports();
                left[i] = ports.size() > 0 ? position.getOrDefault(ports.get(0), -1) : -1;
                right[i] = ports.size() > 1 ? position.getOrDefault(ports.get(1), -1) : -1;
                variableType[i] = nodes[i] instanceof ReadNode r ? graph.variableType(r.variable()).orElse(ValueType.NUMBER)
                        : nodes[i] instanceof WriteNode w ? graph.variableType(w.variable()).orElse(ValueType.NUMBER) : null;
                sensorIndex[i] = -1;
                if (nodes[i] instanceof SensorNode s) {
                    sensorIndex[i] = sensorList.size();
                    sensorList.add(s);
                } else if (nodes[i] instanceof DelayNode)
                    delayList.add(i);
            }
            sensors = sensorList.toArray(new SensorNode[0]);
            delayPositions = delayList.stream().mapToInt(Integer::intValue).toArray();
            values = new Object[n];
        }

        /** The graph's sensor cards in graph order; the {@code index} {@link SensorReader} is called with. */
        public SensorNode[] sensors() {
            return sensors;
        }

        public ProgramGraph graph() {
            return graph;
        }

        private Object value(int position) {
            return position < 0 ? null : values[position];
        }

        /**
         * Runs every node once, in dependency order, and calls {@code actuatorWriter} for each
         * actuator card. {@code sensorReader} and {@code actuatorWriter} are the only places this
         * touches the world, and {@code state} the only memory -- everything else is arithmetic over
         * already-read values. Delay inputs are recorded and writes committed after the pass.
         */
        public void evaluate(SensorReader sensorReader, ActuatorWriter actuatorWriter, State state) {
            Arrays.fill(values, null);
            for (int p : order) {
                if (p < 0)
                    continue;
                Object value = switch (nodes[p]) {
                    case SensorNode n -> n.quantity() == Quantity.STATE
                            ? (Object) (sensorReader.read(n, sensorIndex[p]) > 0.5f)
                            : (Object) sensorReader.read(n, sensorIndex[p]);
                    case ComparatorNode n -> {
                        float l = asFloat(value(left[p]));
                        float r = n.rightInput() == ProgramNode.UNWIRED ? n.literalDefault() : asFloat(value(right[p]));
                        yield n.op() == ComparatorNode.Compare.GREATER ? l > r : l < r;
                    }
                    case LogicNode n -> switch (n.op()) {
                        case AND -> asBoolean(value(left[p])) && asBoolean(value(right[p]));
                        case OR -> asBoolean(value(left[p])) || asBoolean(value(right[p]));
                        case NOT -> !asBoolean(value(left[p]));
                    };
                    case MathNode n -> n.op().apply(asFloat(value(left[p])),
                            n.rightInput() == ProgramNode.UNWIRED ? n.literalDefault() : asFloat(value(right[p])));
                    case ActuatorNode n -> {
                        Object in = value(left[p]);
                        actuatorWriter.write(n.target(), n.channel(), in instanceof Float ? in : (Object) asBoolean(in));
                        yield null;
                    }
                    case DelayNode n -> coerce(state.delayed(n), n.type());
                    case ReadNode n -> coerce(state.read(n.variable()), variableType[p]);
                    case WriteNode n -> {
                        if (asBoolean(value(right[p])))
                            state.write(n.variable(), coerce(value(left[p]), variableType[p]));
                        yield null;
                    }
                };
                values[p] = value;
            }
            for (int p : delayPositions) {
                DelayNode n = (DelayNode) nodes[p];
                state.record(n, coerce(value(left[p]), n.type()));
            }
            state.endStep();
        }
    }

    private static Object coerce(Object value, ValueType type) {
        return type == ValueType.NUMBER ? (Object) asFloat(value) : (Object) asBoolean(value);
    }

    private static float asFloat(Object value) {
        return value instanceof Float f ? f : 0f;
    }

    private static boolean asBoolean(Object value) {
        return value instanceof Boolean b && b;
    }
}
