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

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * One card in a printed program, controller spec §2.2's closed set of port shapes: a number
 * source ({@link SensorNode}), a comparator ({@link ComparatorNode}), a logic gate
 * ({@link AndNode}), an actuator terminal ({@link ActuatorNode}). A new shape is a design event,
 * not a card being added -- see the spec.
 * <p>
 * An input is stored as the {@code id} of the upstream node feeding it, per §2.4's "one wire per
 * input, always" -- there is no separate wire list, because every input already carries its own
 * single source (or, for a numeric input left unwired, its own literal).
 */
public sealed interface ProgramNode permits SensorNode, ComparatorNode, AndNode, ActuatorNode {

    /** Unique within one {@link ProgramGraph}. Assigned by the Programmer as cards are placed. */
    int id();

    String typeKey();

    /**
     * No node feeds this input -- either a numeric port using its own literal instead (spec
     * §2.3), or simply an incomplete card the player hasn't wired yet. Either way,
     * {@link ProgramGraph#findError} rejects a graph where a *mandatory* input is still this.
     */
    int UNWIRED = -1;

    /** Ids of the upstream nodes feeding this one's inputs, for topological ordering. */
    default List<Integer> inputs() {
        return switch (this) {
            case SensorNode ignored -> List.of();
            case ComparatorNode n -> n.rightInput() == UNWIRED
                    ? List.of(n.leftInput())
                    : List.of(n.leftInput(), n.rightInput());
            case AndNode n -> List.of(n.leftInput(), n.rightInput());
            case ActuatorNode n -> List.of(n.input());
        };
    }

    /**
     * Every input port in slot order, wired or not -- unlike {@link #inputs()}, which drops an
     * unwired literal port. What the editor draws a pin for, and what a commit is checked over.
     */
    default List<Integer> ports() {
        return switch (this) {
            case SensorNode ignored -> List.of();
            case ComparatorNode n -> List.of(n.leftInput(), n.rightInput());
            case AndNode n -> List.of(n.leftInput(), n.rightInput());
            case ActuatorNode n -> List.of(n.input());
        };
    }

    /** This node with input {@code slot} (a {@link #ports()} index) fed by {@code fromId}, or {@link #UNWIRED}. */
    default ProgramNode withInput(int slot, int fromId) {
        return switch (this) {
            case ComparatorNode n -> slot == 0
                    ? new ComparatorNode(n.id(), n.op(), fromId, n.rightInput(), n.literalDefault())
                    : new ComparatorNode(n.id(), n.op(), n.leftInput(), fromId, n.literalDefault());
            case AndNode n -> slot == 0
                    ? new AndNode(n.id(), fromId, n.rightInput())
                    : new AndNode(n.id(), n.leftInput(), fromId);
            case ActuatorNode n -> new ActuatorNode(n.id(), fromId, n.target());
            case SensorNode n -> n;
        };
    }

    /** This node renumbered, wiring untouched -- the editor's paste remaps inputs separately. */
    default ProgramNode withId(int id) {
        return switch (this) {
            case SensorNode n -> new SensorNode(id, n.target(), n.quantity());
            case ComparatorNode n -> new ComparatorNode(id, n.op(), n.leftInput(), n.rightInput(), n.literalDefault());
            case AndNode n -> new AndNode(id, n.leftInput(), n.rightInput());
            case ActuatorNode n -> new ActuatorNode(id, n.input(), n.target());
        };
    }

    Codec<ProgramNode> CODEC = Codec.STRING.dispatch(ProgramNode::typeKey, ProgramNode::codecFor);

    StreamCodec<ByteBuf, ProgramNode> STREAM_CODEC = StreamCodec.of(
            (buf, node) -> {
                switch (node) {
                    case SensorNode n -> {
                        buf.writeByte(0);
                        SensorNode.STREAM_CODEC.encode(buf, n);
                    }
                    case ComparatorNode n -> {
                        buf.writeByte(1);
                        ComparatorNode.STREAM_CODEC.encode(buf, n);
                    }
                    case AndNode n -> {
                        buf.writeByte(2);
                        AndNode.STREAM_CODEC.encode(buf, n);
                    }
                    case ActuatorNode n -> {
                        buf.writeByte(3);
                        ActuatorNode.STREAM_CODEC.encode(buf, n);
                    }
                }
            },
            buf -> {
                int kind = buf.readByte();
                return switch (kind) {
                    case 0 -> SensorNode.STREAM_CODEC.decode(buf);
                    case 1 -> ComparatorNode.STREAM_CODEC.decode(buf);
                    case 2 -> AndNode.STREAM_CODEC.decode(buf);
                    case 3 -> ActuatorNode.STREAM_CODEC.decode(buf);
                    default -> throw new IllegalArgumentException("Unknown program node kind: " + kind);
                };
            });

    private static MapCodec<? extends ProgramNode> codecFor(String type) {
        return switch (type) {
            case SensorNode.TYPE -> SensorNode.CODEC;
            case ComparatorNode.TYPE -> ComparatorNode.CODEC;
            case AndNode.TYPE -> AndNode.CODEC;
            case ActuatorNode.TYPE -> ActuatorNode.CODEC;
            default -> throw new IllegalArgumentException("Unknown program node type: " + type);
        };
    }
}
